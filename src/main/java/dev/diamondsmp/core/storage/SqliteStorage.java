package dev.diamondsmp.core.storage;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class SqliteStorage implements Storage {

    private static final int SCHEMA_VERSION = 3;
    private static final String PROFILE_COLUMNS =
            "uuid, name, kills, deaths, streak, best_streak, kill_effect, social_spy, ignored, blocked, first_join, last_seen, playtime";

    private final File file;
    private Connection connection;

    public SqliteStorage(File file) {
        this.file = file;
    }

    @Override
    public void open() {
        try {
            Class.forName("org.sqlite.JDBC");
            File parent = file.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                throw new SQLException("Could not create " + parent);
            }
            connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA journal_mode=WAL");
                statement.execute("PRAGMA synchronous=NORMAL");
                statement.execute("PRAGMA foreign_keys=ON");
                statement.execute("CREATE TABLE IF NOT EXISTS schema_info (version INTEGER NOT NULL)");
                statement.execute("CREATE TABLE IF NOT EXISTS players ("
                        + "uuid TEXT PRIMARY KEY, name TEXT NOT NULL, name_lower TEXT NOT NULL, "
                        + "kills INTEGER NOT NULL DEFAULT 0, deaths INTEGER NOT NULL DEFAULT 0, "
                        + "streak INTEGER NOT NULL DEFAULT 0, best_streak INTEGER NOT NULL DEFAULT 0, "
                        + "kill_effect TEXT, social_spy INTEGER NOT NULL DEFAULT 0, "
                        + "ignored TEXT NOT NULL DEFAULT '', blocked TEXT NOT NULL DEFAULT '', "
                        + "first_join INTEGER NOT NULL, last_seen INTEGER NOT NULL, "
                        + "playtime INTEGER NOT NULL DEFAULT 0)");
                statement.execute("CREATE INDEX IF NOT EXISTS players_name ON players (name_lower)");
                statement.execute("CREATE TABLE IF NOT EXISTS points ("
                        + "kind TEXT NOT NULL, name TEXT NOT NULL, world TEXT NOT NULL, "
                        + "x REAL NOT NULL, y REAL NOT NULL, z REAL NOT NULL, yaw REAL NOT NULL, pitch REAL NOT NULL, "
                        + "PRIMARY KEY (kind, name))");
                statement.execute("CREATE TABLE IF NOT EXISTS placed_blocks ("
                        + "world TEXT NOT NULL, x INTEGER NOT NULL, y INTEGER NOT NULL, z INTEGER NOT NULL, "
                        + "material TEXT NOT NULL, PRIMARY KEY (world, x, y, z))");
            }
            migrate();
        } catch (ClassNotFoundException | SQLException ex) {
            throw new StorageException("Could not open " + file.getName(), ex);
        }
    }

    private void migrate() throws SQLException {
        int current = 0;
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT version FROM schema_info LIMIT 1")) {
            if (result.next()) {
                current = result.getInt(1);
            }
        }
        if (current >= SCHEMA_VERSION) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            if (playersLack("blocked")) {
                statement.execute("ALTER TABLE players ADD COLUMN blocked TEXT NOT NULL DEFAULT ''");
            }
            if (playersLack("playtime")) {
                statement.execute("ALTER TABLE players ADD COLUMN playtime INTEGER NOT NULL DEFAULT 0");
            }
            statement.execute("DELETE FROM schema_info");
            statement.execute("INSERT INTO schema_info (version) VALUES (" + SCHEMA_VERSION + ")");
        }
    }

    private boolean playersLack(String column) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("PRAGMA table_info(players)")) {
            while (result.next()) {
                if (column.equalsIgnoreCase(result.getString("name"))) {
                    return false;
                }
            }
            return true;
        }
    }

    @Override
    public void close() {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException ex) {
            throw new StorageException("Could not close " + file.getName(), ex);
        } finally {
            connection = null;
        }
    }

    @Override
    public Optional<ProfileSnapshot> loadProfile(UUID uuid) {
        return queryProfile("SELECT " + PROFILE_COLUMNS + " FROM players WHERE uuid = ?", uuid.toString());
    }

    @Override
    public Optional<ProfileSnapshot> findProfile(String name) {
        return queryProfile("SELECT " + PROFILE_COLUMNS
                + " FROM players WHERE name_lower = ? ORDER BY last_seen DESC LIMIT 1", name.toLowerCase(Locale.ROOT));
    }

    private Optional<ProfileSnapshot> queryProfile(String sql, String parameter) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, parameter);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(readProfile(result)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new StorageException("Could not load a player profile", ex);
        }
    }

    @Override
    public void saveProfile(ProfileSnapshot profile) {
        String sql = "INSERT OR REPLACE INTO players (" + PROFILE_COLUMNS + ", name_lower) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, profile.uuid().toString());
            statement.setString(2, profile.name());
            statement.setInt(3, profile.kills());
            statement.setInt(4, profile.deaths());
            statement.setInt(5, profile.streak());
            statement.setInt(6, profile.bestStreak());
            statement.setString(7, profile.killEffect());
            statement.setInt(8, profile.socialSpy() ? 1 : 0);
            statement.setString(9, joinIds(profile.ignored()));
            statement.setString(10, joinIds(profile.blocked()));
            statement.setLong(11, profile.firstJoin());
            statement.setLong(12, profile.lastSeen());
            statement.setLong(13, profile.playtime());
            statement.setString(14, profile.name().toLowerCase(Locale.ROOT));
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new StorageException("Could not save the profile of " + profile.name(), ex);
        }
    }

    @Override
    public List<ProfileSnapshot> top(LeaderboardType type, int offset, int limit) {
        String sql = "SELECT " + PROFILE_COLUMNS + " FROM players ORDER BY " + type.ordering()
                + ", name ASC LIMIT ? OFFSET ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, Math.max(1, limit));
            statement.setInt(2, Math.max(0, offset));
            try (ResultSet result = statement.executeQuery()) {
                List<ProfileSnapshot> entries = new ArrayList<>();
                while (result.next()) {
                    entries.add(readProfile(result));
                }
                return entries;
            }
        } catch (SQLException ex) {
            throw new StorageException("Could not read the " + type.id() + " leaderboard", ex);
        }
    }

    @Override
    public int countProfiles() {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM players")) {
            return result.next() ? result.getInt(1) : 0;
        } catch (SQLException ex) {
            throw new StorageException("Could not count the players", ex);
        }
    }

    @Override
    public Map<String, StoredLocation> loadPoints(String kind) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT name, world, x, y, z, yaw, pitch FROM points WHERE kind = ?")) {
            statement.setString(1, kind);
            try (ResultSet result = statement.executeQuery()) {
                Map<String, StoredLocation> locations = new LinkedHashMap<>();
                while (result.next()) {
                    locations.put(result.getString(1), new StoredLocation(result.getString(2),
                            result.getDouble(3), result.getDouble(4), result.getDouble(5),
                            result.getFloat(6), result.getFloat(7)));
                }
                return locations;
            }
        } catch (SQLException ex) {
            throw new StorageException("Could not load the saved " + kind + " locations", ex);
        }
    }

    @Override
    public void savePoint(String kind, String name, StoredLocation location) {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT OR REPLACE INTO points (kind, name, world, x, y, z, yaw, pitch) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, kind);
            statement.setString(2, name);
            statement.setString(3, location.world());
            statement.setDouble(4, location.x());
            statement.setDouble(5, location.y());
            statement.setDouble(6, location.z());
            statement.setFloat(7, location.yaw());
            statement.setFloat(8, location.pitch());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new StorageException("Could not save the " + kind + " " + name, ex);
        }
    }

    @Override
    public void deletePoint(String kind, String name) {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM points WHERE kind = ? AND name = ?")) {
            statement.setString(1, kind);
            statement.setString(2, name);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new StorageException("Could not delete the " + kind + " " + name, ex);
        }
    }

    @Override
    public List<PlacedBlock> loadPlacedBlocks() {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT world, x, y, z, material FROM placed_blocks")) {
            List<PlacedBlock> blocks = new ArrayList<>();
            while (result.next()) {
                blocks.add(new PlacedBlock(result.getString(1), result.getInt(2), result.getInt(3),
                        result.getInt(4), result.getString(5)));
            }
            return blocks;
        } catch (SQLException ex) {
            throw new StorageException("Could not load the tracked blocks", ex);
        }
    }

    @Override
    public void replacePlacedBlocks(Collection<PlacedBlock> blocks) {
        try {
            connection.setAutoCommit(false);
            try (Statement clear = connection.createStatement();
                 PreparedStatement insert = connection.prepareStatement(
                         "INSERT OR REPLACE INTO placed_blocks (world, x, y, z, material) VALUES (?, ?, ?, ?, ?)")) {
                clear.execute("DELETE FROM placed_blocks");
                for (PlacedBlock block : blocks) {
                    insert.setString(1, block.world());
                    insert.setInt(2, block.x());
                    insert.setInt(3, block.y());
                    insert.setInt(4, block.z());
                    insert.setString(5, block.material());
                    insert.addBatch();
                }
                insert.executeBatch();
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new StorageException("Could not save the tracked blocks", ex);
        }
    }

    private static ProfileSnapshot readProfile(ResultSet result) throws SQLException {
        return new ProfileSnapshot(
                UUID.fromString(result.getString(1)),
                result.getString(2),
                result.getInt(3),
                result.getInt(4),
                result.getInt(5),
                result.getInt(6),
                result.getString(7),
                result.getInt(8) == 1,
                splitIds(result.getString(9)),
                splitIds(result.getString(10)),
                result.getLong(11),
                result.getLong(12),
                result.getLong(13));
    }

    private static String joinIds(Set<UUID> ids) {
        StringBuilder builder = new StringBuilder(ids.size() * 37);
        for (UUID id : ids) {
            if (!builder.isEmpty()) {
                builder.append(',');
            }
            builder.append(id);
        }
        return builder.toString();
    }

    private static Set<UUID> splitIds(String raw) {
        Set<UUID> ids = new HashSet<>();
        if (raw == null || raw.isBlank()) {
            return ids;
        }
        for (String part : raw.split(",")) {
            try {
                ids.add(UUID.fromString(part.trim()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return ids;
    }
}
