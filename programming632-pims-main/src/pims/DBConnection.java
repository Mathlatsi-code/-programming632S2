package pims;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Central place for JDBC connections. Settings can be changed in db.properties (optional). */
public final class DBConnection {
    private static final Properties P = new Properties();

    static {
        P.setProperty("host", "localhost");
        P.setProperty("port", "3306");
        P.setProperty("db", "healthfirst_pims");
        P.setProperty("user", "root");
        P.setProperty("password", "");
        // Use MySQL Connector/J if it is on the classpath, otherwise the bundled MariaDB driver (works with XAMPP/MySQL)
        String scheme = "mariadb";
        try { Class.forName("com.mysql.cj.jdbc.Driver"); scheme = "mysql"; } catch (ClassNotFoundException ignored) { }
        P.setProperty("scheme", scheme);
        File f = new File("db.properties");
        if (f.exists()) {
            try (FileInputStream in = new FileInputStream(f)) {
                P.load(in);
            } catch (Exception ignored) { }
        }
        for (String k : new String[]{"host", "port", "db", "user", "password", "scheme"}) {
            String v = System.getProperty("pims." + k);
            if (v != null) P.setProperty(k, v);
        }
    }

    private DBConnection() { }

    public static String dbName() { return P.getProperty("db"); }

    private static String url(boolean withDb) {
        return "jdbc:" + P.getProperty("scheme") + "://" + P.getProperty("host") + ":" + P.getProperty("port")
                + "/" + (withDb ? P.getProperty("db") : "")
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Africa/Johannesburg";
    }

    /** Connection to the pharmacy database. */
    public static Connection get() throws SQLException {
        return DriverManager.getConnection(url(true), P.getProperty("user"), P.getProperty("password"));
    }

    /** Connection to the MySQL server only (used to create the database on first run). */
    public static Connection getServer() throws SQLException {
        return DriverManager.getConnection(url(false), P.getProperty("user"), P.getProperty("password"));
    }
}
