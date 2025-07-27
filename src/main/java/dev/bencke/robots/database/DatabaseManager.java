package dev.bencke.robots.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.bencke.robots.RobotPlugin;
import dev.bencke.robots.models.Robot;
import dev.bencke.robots.utils.Logger;
import lombok.Getter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {

    private final RobotPlugin plugin;
    private HikariDataSource dataSource;

    @Getter
    private JedisPool jedisPool;
    private boolean redisEnabled;

    public DatabaseManager(RobotPlugin plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        setupMySQL();

        if (plugin.getConfigManager().getMainConfig().getBoolean("redis.enabled", false)) {
            setupRedis();
        }
    }

    private void setupMySQL() {
        HikariConfig config = new HikariConfig();

        String host = plugin.getConfigManager().getMainConfig().getString("mysql.host");
        int port = plugin.getConfigManager().getMainConfig().getInt("mysql.port");
        String database = plugin.getConfigManager().getMainConfig().getString("mysql.database");
        String username = plugin.getConfigManager().getMainConfig().getString("mysql.username");
        String password = plugin.getConfigManager().getMainConfig().getString("mysql.password");

        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false");
        config.setUsername(username);
        config.setPassword(password);

        // Performance settings
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);
        config.setIdleTimeout(300000);
        config.setConnectionTimeout(10000);
        config.setLeakDetectionThreshold(60000);

        // Optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");

        try {
            dataSource = new HikariDataSource(config);
            createTables();
            Logger.info("MySQL connection established!");
        } catch (Exception e) {
            Logger.severe("Failed to connect to MySQL: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupRedis() {
        try {
            String host = plugin.getConfigManager()
                    .getMainConfig()
                    .getString("redis.host", "localhost");
            int port = plugin.getConfigManager()
                    .getMainConfig()
                    .getInt("redis.port", 6379);
            String user = plugin.getConfigManager()
                    .getMainConfig()
                    .getString("redis.user", "default");
            String password = plugin.getConfigManager()
                    .getMainConfig()
                    .getString("redis.password", "");

            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(10);
            poolConfig.setMaxIdle(5);
            poolConfig.setMinIdle(1);
            poolConfig.setTestOnBorrow(true);
            poolConfig.setTestOnReturn(true);
            poolConfig.setTestWhileIdle(true);
            poolConfig.setMinEvictableIdleTimeMillis(60_000);
            poolConfig.setTimeBetweenEvictionRunsMillis(30_000);
            poolConfig.setNumTestsPerEvictionRun(-1);
            poolConfig.setBlockWhenExhausted(false);

            // Escolha o construtor adequado para incluir usuário + senha
            jedisPool = new JedisPool(
                    poolConfig,
                    host,
                    port,
                    5000,        // conexão timeout (ms)
                    5000,        // soTimeout (ms)
                    user,
                    password,
                    0,           // database index
                    "brobots-plugin",
                    false        // ssl desabilitado (true se usar TLS)
            );

            // Teste de conexão
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.ping();
                redisEnabled = true;
                Logger.info("Redis (ACL) conectado como usuário '" + user + "'!");
            }
        } catch (Exception e) {
            Logger.warning("Falha ao conectar ao Redis: " + e.getMessage());
            e.printStackTrace();
            redisEnabled = false;

            if (jedisPool != null && !jedisPool.isClosed()) {
                jedisPool.close();
                jedisPool = null;
            }
        }
    }


    private void createTables() {
        String robotsTable = "CREATE TABLE IF NOT EXISTS robots (" +
                "id VARCHAR(36) PRIMARY KEY," +
                "owner_uuid VARCHAR(36) NOT NULL," +
                "owner_name VARCHAR(16) NOT NULL," +
                "type VARCHAR(64) NOT NULL," +
                "location TEXT NOT NULL," +
                "level INT DEFAULT 1," +
                "fuel BIGINT DEFAULT 0," +
                "storage LONGTEXT," +
                "upgrades TEXT," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "INDEX idx_owner (owner_uuid)," +
                "INDEX idx_type (type)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(robotsTable);
        } catch (SQLException e) {
            Logger.severe("Failed to create tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public CompletableFuture<Void> saveRobot(Robot robot) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO robots (id, owner_uuid, owner_name, type, location, level, fuel, storage, upgrades) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "location = VALUES(location), level = VALUES(level), fuel = VALUES(fuel), " +
                    "storage = VALUES(storage), upgrades = VALUES(upgrades)";

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                Map<String, Object> data = robot.serialize();

                stmt.setString(1, robot.getId().toString());
                stmt.setString(2, robot.getOwnerId().toString());
                stmt.setString(3, robot.getOwnerName());
                stmt.setString(4, robot.getType().getId());
                stmt.setString(5, (String) data.get("location"));
                stmt.setInt(6, robot.getLevel());
                stmt.setLong(7, robot.getFuel());
                stmt.setString(8, serializeMap((Map<?, ?>) data.get("storage")));
                stmt.setString(9, String.join(",", robot.getUpgrades()));

                stmt.executeUpdate();

                // Cache in Redis if enabled
                if (redisEnabled) {
                    cacheRobot(robot);
                }

            } catch (SQLException e) {
                Logger.severe("Failed to save robot: " + e.getMessage());
                e.printStackTrace();
            }
        }, plugin.getExecutorService());
    }

    public CompletableFuture<List<Robot>> loadRobots(UUID ownerId) {
        return CompletableFuture.supplyAsync(() -> {
            // Try Redis cache first
            if (redisEnabled) {
                List<Robot> cached = getCachedRobots(ownerId);
                if (!cached.isEmpty()) {
                    return cached;
                }
            }

            List<Robot> robots = new ArrayList<>();
            String sql = "SELECT * FROM robots WHERE owner_uuid = ?";

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, ownerId.toString());
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", rs.getString("id"));
                    data.put("owner", rs.getString("owner_uuid"));
                    data.put("ownerName", rs.getString("owner_name"));
                    data.put("type", rs.getString("type"));
                    data.put("location", rs.getString("location"));
                    data.put("level", rs.getInt("level"));
                    data.put("fuel", rs.getLong("fuel"));
                    data.put("storage", deserializeMap(rs.getString("storage")));

                    String upgradesStr = rs.getString("upgrades");
                    List<String> upgrades = upgradesStr != null && !upgradesStr.isEmpty()
                            ? Arrays.asList(upgradesStr.split(","))
                            : new ArrayList<>();
                    data.put("upgrades", upgrades);

                    Robot robot = Robot.deserialize(data);
                    if (robot != null) {
                        robots.add(robot);
                    }
                }

            } catch (SQLException e) {
                Logger.severe("Failed to load robots: " + e.getMessage());
                e.printStackTrace();
            }

            return robots;
        }, plugin.getExecutorService());
    }

    public CompletableFuture<Void> deleteRobot(UUID robotId) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM robots WHERE id = ?";

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, robotId.toString());
                stmt.executeUpdate();

                // Remove from Redis cache
                if (redisEnabled) {
                    uncacheRobot(robotId);
                }

            } catch (SQLException e) {
                Logger.severe("Failed to delete robot: " + e.getMessage());
                e.printStackTrace();
            }
        }, plugin.getExecutorService());
    }

    private void cacheRobot(Robot robot) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "robot:" + robot.getId();
            Map<String, String> data = new HashMap<>();

            robot.serialize().forEach((k, v) -> {
                data.put(k, v.toString());
            });

            jedis.hmset(key, data);
            jedis.expire(key, 3600); // 1 hour cache

            // Add to owner's robot set
            jedis.sadd("owner:" + robot.getOwnerId(), robot.getId().toString());
            jedis.expire("owner:" + robot.getOwnerId(), 3600);
        }
    }

    private List<Robot> getCachedRobots(UUID ownerId) {
        List<Robot> robots = new ArrayList<>();

        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> robotIds = jedis.smembers("owner:" + ownerId);

            for (String robotId : robotIds) {
                Map<String, String> data = jedis.hgetAll("robot:" + robotId);
                if (!data.isEmpty()) {
                    // Convert back to proper types
                    Map<String, Object> objectData = new HashMap<>(data);
                    Robot robot = Robot.deserialize(objectData);
                    if (robot != null) {
                        robots.add(robot);
                    }
                }
            }
        }

        return robots;
    }

    private void uncacheRobot(UUID robotId) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del("robot:" + robotId);
        }
    }

    private String serializeMap(Map<?, ?> map) {
        if (map == null || map.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        map.forEach((k, v) -> {
            if (sb.length() > 0) sb.append(";");
            sb.append(k).append("=").append(v);
        });

        return sb.toString();
    }

    private Map<String, Integer> deserializeMap(String str) {
        Map<String, Integer> map = new HashMap<>();

        if (str == null || str.isEmpty()) {
            return map;
        }

        String[] entries = str.split(";");
        for (String entry : entries) {
            String[] parts = entry.split("=", 2);
            if (parts.length == 2) {
                try {
                    map.put(parts[0], Integer.parseInt(parts[1]));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return map;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }

        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }
}