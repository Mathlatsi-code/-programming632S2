package pims;

import javax.swing.table.DefaultTableModel;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/** Small JDBC helpers used by all panels. */
public final class Db {
    private Db() { }

    private static void bind(PreparedStatement ps, Object[] p) throws SQLException {
        for (int i = 0; i < p.length; i++) ps.setObject(i + 1, p[i]);
    }

    /** Runs a SELECT and returns a read-only table model. Money columns are shown as R0.00. */
    public static DefaultTableModel query(String sql, Object... p) throws SQLException {
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, p);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                int n = md.getColumnCount();
                Vector<String> cols = new Vector<>();
                for (int i = 1; i <= n; i++) cols.add(md.getColumnLabel(i));
                Vector<Vector<Object>> rows = new Vector<>();
                while (rs.next()) {
                    Vector<Object> r = new Vector<>();
                    for (int i = 1; i <= n; i++) {
                        Object o = rs.getObject(i);
                        if (o instanceof BigDecimal) o = UI.money((BigDecimal) o);
                        else if (o instanceof Timestamp) o = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format((Timestamp) o);
                        else if (o instanceof java.util.Date) o = o.toString();
                        else if (o == null) o = "";
                        r.add(o);
                    }
                    rows.add(r);
                }
                return new DefaultTableModel(rows, cols) {
                    @Override public boolean isCellEditable(int r, int c) { return false; }
                };
            }
        }
    }

    public static Object scalar(String sql, Object... p) throws SQLException {
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, p);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getObject(1) : null;
            }
        }
    }

    public static int update(String sql, Object... p) throws SQLException {
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, p);
            return ps.executeUpdate();
        }
    }

    public static List<Item> suppliers() throws SQLException {
        List<Item> list = new ArrayList<>();
        try (Connection c = DBConnection.get(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT supplier_id, name FROM suppliers ORDER BY name")) {
            while (rs.next()) list.add(new Item(rs.getInt(1), rs.getString(2)));
        }
        return list;
    }
}
