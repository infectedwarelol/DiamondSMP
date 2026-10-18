package dev.diamondsmp.core.storage;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Database {

    private static final long SHUTDOWN_TIMEOUT_SECONDS = 30L;

    private final Logger logger;
    private final Storage storage;
    private final ExecutorService executor;

    public Database(Logger logger, Storage storage) {
        this.logger = logger;
        this.storage = storage;
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "DiamondSMP-Storage");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void open() {
        await(run("open the database", Storage::open), SHUTDOWN_TIMEOUT_SECONDS);
    }

    public <T> CompletableFuture<T> supply(String action, Function<Storage, T> call) {
        try {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return call.apply(storage);
                } catch (RuntimeException ex) {
                    logger.log(Level.SEVERE, "Could not " + action, ex);
                    throw new CompletionException(ex);
                }
            }, executor);
        } catch (RuntimeException ex) {
            logger.log(Level.SEVERE, "Could not queue: " + action, ex);
            return CompletableFuture.failedFuture(ex);
        }
    }

    public CompletableFuture<Void> run(String action, Consumer<Storage> call) {
        return supply(action, target -> {
            call.accept(target);
            return null;
        });
    }

    public <T> T await(CompletableFuture<T> future, long timeoutSeconds) {
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new StorageException("Interrupted while waiting for the database", ex);
        } catch (ExecutionException ex) {
            throw new StorageException("A database task failed", ex.getCause());
        } catch (TimeoutException ex) {
            throw new StorageException("The database did not answer in time", ex);
        }
    }

    public void close() {
        CompletableFuture<Void> closing = run("close the database", Storage::close);
        executor.shutdown();
        try {
            closing.get(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                logger.warning("The storage thread did not stop in time; pending writes may be lost.");
                executor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        } catch (ExecutionException | TimeoutException ex) {
            logger.log(Level.WARNING, "The database did not close cleanly", ex);
        }
    }
}
