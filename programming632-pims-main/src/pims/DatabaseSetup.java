package pims;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;

/** On first run, creates the database from database.sql so the app works out of the box. */
public final class DatabaseSetup {
    private DatabaseSetup() { }

    public static void ensure() throws Exception {
        try (Connection c = DBConnection.getServer(); Statement st = c.createStatement()) {
            try (ResultSet rs = st.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='"
                            + DBConnection.dbName() + "' AND table_name='users'")) {
                rs.next();
                if (rs.getInt(1) > 0) return;      // already installed
            }
            StringBuilder sql = new StringBuilder();
            for (String line : readScript().split("\\r?\\n")) {
                String t = line.trim();
                if (t.isEmpty() || t.startsWith("--")) continue;
                sql.append(line).append('\n');
                if (t.endsWith(";")) {
                    String stmt = sql.toString().trim();
                    st.execute(stmt.substring(0, stmt.length() - 1));
                    sql.setLength(0);
                }
            }
        }
    }

    private static String readScript() throws IOException {
        File f = new File("database.sql");
        InputStream in = f.exists() ? new FileInputStream(f) : DatabaseSetup.class.getResourceAsStream("/database.sql");
        if (in == null) throw new FileNotFoundException("database.sql was not found next to the application.");
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(); InputStream i = in) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = i.read(buf)) > 0) out.write(buf, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
