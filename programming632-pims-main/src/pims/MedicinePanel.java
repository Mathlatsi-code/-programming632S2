package pims;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;

/** Admin: full CRUD for medicines. Expired rows are red, low-stock rows are amber. */
public class MedicinePanel extends JPanel implements Refreshable {
    private final JTable table = new JTable();
    private final JTextField tfSearch = UI.field(20), tfName = UI.field(14), tfCompany = UI.field(14),
            tfPrice = UI.field(14), tfQty = UI.field(14), tfReorder = UI.field(14), tfExpiry = UI.field(14);
    private final JComboBox<String> cbType = new JComboBox<>(
            new String[]{"Tablet", "Capsule", "Syrup", "Injection", "Cream", "Sachet", "Drops", "Other"});
    private final JComboBox<Item> cbSupplier = new JComboBox<>();
    private int selectedId = -1;

    public MedicinePanel() {
        super(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        setBackground(Color.WHITE);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.setOpaque(false);
        top.add(UI.boldLabel("Search:"));
        top.add(tfSearch);
        JButton go = UI.button("Search", UI.PRIMARY);
        JButton all = UI.button("Show All", UI.GREY);
        top.add(go);
        top.add(all);
        JLabel legend = new JLabel("<html>&nbsp;&nbsp;<font color='#DC2626'>\u25A0</font> Expired &nbsp;&nbsp;<font color='#F59E0B'>\u25A0</font> Low stock</html>");
        legend.setForeground(UI.GREY);
        top.add(legend);
        add(top, BorderLayout.NORTH);

        UI.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean f, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, sel, f, r, c);
                if (!sel) {
                    comp.setBackground(Color.WHITE);
                    try {
                        int stock = Integer.parseInt(t.getValueAt(r, 5).toString());
                        int reorder = Integer.parseInt(t.getValueAt(r, 6).toString());
                        LocalDate exp = LocalDate.parse(t.getValueAt(r, 7).toString());
                        if (exp.isBefore(LocalDate.now())) comp.setBackground(new Color(0xFEE2E2));
                        else if (stock <= reorder) comp.setBackground(new Color(0xFEF3C7));
                    } catch (Exception ignored) { }
                }
                return comp;
            }
        });
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) loadSelected();
        });
        add(UI.scroll(table), BorderLayout.CENTER);

        // Form
        JPanel form = UI.card(new GridBagLayout());
        form.setPreferredSize(new Dimension(320, 0));
        UI.formRow(form, 0, "Name", tfName);
        UI.formRow(form, 1, "Company", tfCompany);
        UI.formRow(form, 2, "Type", cbType);
        UI.formRow(form, 3, "Price (R)", tfPrice);
        UI.formRow(form, 4, "In stock", tfQty);
        UI.formRow(form, 5, "Reorder level", tfReorder);
        UI.formRow(form, 6, "<html>Expiry date<br><small>yyyy-mm-dd</small></html>", tfExpiry);
        UI.formRow(form, 7, "Supplier", cbSupplier);
        cbType.setEditable(true);

        JPanel btns = new JPanel(new GridLayout(2, 2, 8, 8));
        btns.setOpaque(false);
        JButton add = UI.button("Add", UI.PRIMARY);
        JButton upd = UI.button("Update", UI.INFO);
        JButton del = UI.button("Delete", UI.DANGER);
        JButton clr = UI.button("Clear", UI.GREY);
        btns.add(add); btns.add(upd); btns.add(del); btns.add(clr);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = 8; g.gridwidth = 2; g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(14, 0, 0, 0);
        form.add(btns, g);
        g.gridy = 9; g.weighty = 1;
        form.add(Box.createVerticalGlue(), g);
        add(form, BorderLayout.EAST);

        go.addActionListener(e -> loadTable());
        tfSearch.addActionListener(e -> loadTable());
        all.addActionListener(e -> { tfSearch.setText(""); loadTable(); });
        add.addActionListener(e -> save(true));
        upd.addActionListener(e -> save(false));
        del.addActionListener(e -> delete());
        clr.addActionListener(e -> clearForm());
        refresh();
    }

    @Override public void refresh() {
        try {
            Item keep = (Item) cbSupplier.getSelectedItem();
            cbSupplier.removeAllItems();
            for (Item i : Db.suppliers()) cbSupplier.addItem(i);
            if (keep != null) selectSupplier(keep.id);
        } catch (SQLException ex) { UI.error(this, ex.getMessage()); }
        loadTable();
    }

    private void selectSupplier(int id) {
        for (int i = 0; i < cbSupplier.getItemCount(); i++)
            if (cbSupplier.getItemAt(i).id == id) { cbSupplier.setSelectedIndex(i); return; }
    }

    private void loadTable() {
        try {
            String like = "%" + tfSearch.getText().trim() + "%";
            table.setModel(Db.query(
                    "SELECT m.medicine_id AS ID, m.name AS Name, m.company AS Company, m.medicine_type AS Type, "
                            + "m.price AS Price, m.quantity_in_stock AS Stock, m.reorder_level AS Reorder, "
                            + "m.expiry_date AS Expiry, s.name AS Supplier "
                            + "FROM medicines m LEFT JOIN suppliers s ON s.supplier_id = m.supplier_id "
                            + "WHERE m.name LIKE ? OR m.company LIKE ? OR m.medicine_type LIKE ? ORDER BY m.name",
                    like, like, like));
            UI.widths(table, 40, 200, 120, 80, 85, 55, 80, 100, 140);
            selectedId = -1;
        } catch (SQLException ex) { UI.error(this, ex.getMessage()); }
    }

    private void loadSelected() {
        int row = table.getSelectedRow();
        selectedId = Integer.parseInt(table.getValueAt(row, 0).toString());
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM medicines WHERE medicine_id=?")) {
            ps.setInt(1, selectedId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    tfName.setText(rs.getString("name"));
                    tfCompany.setText(rs.getString("company"));
                    cbType.setSelectedItem(rs.getString("medicine_type"));
                    tfPrice.setText(rs.getBigDecimal("price").toPlainString());
                    tfQty.setText(String.valueOf(rs.getInt("quantity_in_stock")));
                    tfReorder.setText(String.valueOf(rs.getInt("reorder_level")));
                    tfExpiry.setText(rs.getDate("expiry_date").toString());
                    selectSupplier(rs.getInt("supplier_id"));
                }
            }
        } catch (SQLException ex) { UI.error(this, ex.getMessage()); }
    }

    private void clearForm() {
        selectedId = -1;
        table.clearSelection();
        tfName.setText(""); tfCompany.setText(""); tfPrice.setText(""); tfQty.setText("");
        tfReorder.setText(""); tfExpiry.setText("");
        cbType.setSelectedIndex(0);
    }

    private void save(boolean isNew) {
        if (!isNew && selectedId < 0) { UI.error(this, "Select a medicine in the table first."); return; }
        try {
            String name = tfName.getText().trim();
            if (name.isEmpty()) throw new IllegalArgumentException("Medicine name is required.");
            BigDecimal price = new BigDecimal(tfPrice.getText().trim());
            if (price.signum() < 0) throw new IllegalArgumentException("Price cannot be negative.");
            int qty = Integer.parseInt(tfQty.getText().trim());
            int reorder = Integer.parseInt(tfReorder.getText().trim());
            if (qty < 0 || reorder < 0) throw new IllegalArgumentException("Stock and reorder level cannot be negative.");
            LocalDate expiry = LocalDate.parse(tfExpiry.getText().trim());
            Item sup = (Item) cbSupplier.getSelectedItem();
            Object supId = sup == null ? null : sup.id;
            String type = String.valueOf(cbType.getSelectedItem());
            if (isNew) {
                Db.update("INSERT INTO medicines (name, company, medicine_type, price, quantity_in_stock, "
                                + "reorder_level, expiry_date, supplier_id) VALUES (?,?,?,?,?,?,?,?)",
                        name, tfCompany.getText().trim(), type, price, qty, reorder, java.sql.Date.valueOf(expiry), supId);
            } else {
                Db.update("UPDATE medicines SET name=?, company=?, medicine_type=?, price=?, quantity_in_stock=?, "
                                + "reorder_level=?, expiry_date=?, supplier_id=? WHERE medicine_id=?",
                        name, tfCompany.getText().trim(), type, price, qty, reorder, java.sql.Date.valueOf(expiry),
                        supId, selectedId);
            }
            loadTable();
            clearForm();
        } catch (NumberFormatException ex) {
            UI.error(this, "Price, stock and reorder level must be valid numbers.");
        } catch (java.time.format.DateTimeParseException ex) {
            UI.error(this, "Expiry date must be in the format yyyy-mm-dd (e.g. 2027-03-31).");
        } catch (IllegalArgumentException ex) {
            UI.error(this, ex.getMessage());
        } catch (SQLException ex) {
            UI.error(this, "Database error: " + ex.getMessage());
        }
    }

    private void delete() {
        if (selectedId < 0) { UI.error(this, "Select a medicine in the table first."); return; }
        if (!UI.confirm(this, "Delete '" + tfName.getText() + "' permanently?")) return;
        try {
            Db.update("DELETE FROM medicines WHERE medicine_id=?", selectedId);
            loadTable();
            clearForm();
        } catch (SQLIntegrityConstraintViolationException ex) {
            UI.error(this, "This medicine appears in past sales and cannot be deleted.\n"
                    + "Set its stock to 0 instead to keep your sales history intact.");
        } catch (SQLException ex) {
            UI.error(this, "Database error: " + ex.getMessage());
        }
    }
}
