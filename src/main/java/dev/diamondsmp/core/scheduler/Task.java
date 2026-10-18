package dev.diamondsmp.core.scheduler;

@FunctionalInterface
public interface Task {

    Task NONE = () -> {
    };

    void cancel();
}
