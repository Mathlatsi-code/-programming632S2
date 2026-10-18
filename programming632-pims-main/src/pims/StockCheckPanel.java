package pims;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.sql.SQLException;

/** Cashier: read-only price and availability lookup. */
public class StockCheckPanel extends JPanel implements Refreshable {
    private final JTextField tfSearch = UI.field(24);
    private final JTable table = new JTable();

    public StockCheckPanel() {
        super(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        setBackground(Color.WHITE);
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.setOpaque(false);
        top.add(UI.boldLabel("Check price / availability:"));
        top.add(tfSearch);
        JButton go = UI.button("Check", UI.PRIMARY);
        JButton all = UI.button("Show All", UI.GREY);
        top.add(go);
        top.add(all);
        add(top, BorderLayout.NORTH);
        UI.styleTable(table);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, s, f, r, c);
                if (c == 5 && v != null) {
                    String st = v.toString();
                    comp.setForeground(st.equals("Available") ? new Color(0x15803D)
                            : st.equals("Low stock") ? new Color(0xB45309) : UI.DANGER);
                    comp.setFont(UI.BOLD);
                } else comp.setForeground(UI.TEXT);
                return comp;
            }
        });
        add(UI.scroll(table), BorderLayout.CENTER);
        go.addActionListener(e -> refresh());
        tfSearch.addActionListener(e -> refresh());
        all.addActionListener(e -> { tfSearch.setText(""); refresh(); });
        refresh();
    }

    @Override public void refresh() {
        try {
            String like = "%" + tfSearch.getText().trim() + "%";
            table.setModel(Db.query(
                    "SELECT name AS Medicine, company AS Company, medicine_type AS Type, price AS Price, "
                            + "quantity_in_stock AS `In Stock`, "
                            + "CASE WHEN expiry_date < CURDATE() THEN 'Expired - do not sell' "
                            + "WHEN quantity_in_stock = 0 THEN 'Out of stock' "
                            + "WHEN quantity_in_stock <= reorder_level THEN 'Low stock' ELSE 'Available' END AS Status "
                            + "FROM medicines WHERE name LIKE ? OR company LIKE ? OR medicine_type LIKE ? ORDER BY name",
                    like, like, like));
            table.getColumnModel().getColumn(0).setPreferredWidth(240);
            table.getColumnModel().getColumn(5).setPreferredWidth(160);
        } catch (SQLException ex) { UI.error(this, ex.getMessage()); }
    }
}
