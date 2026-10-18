package pims;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

/** Admin: CRUD for suppliers. */
public class SupplierPanel extends JPanel implements Refreshable {
    private final JTable table = new JTable();
    private final JTextField tfName = UI.field(18), tfContact = UI.field(18), tfPhone = UI.field(18), tfEmail = UI.field(18);
    private final JTextArea taAddress = new JTextArea(3, 18);
    private int selectedId = -1;

    public SupplierPanel() {
        super(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        setBackground(Color.WHITE);

        UI.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) loadSelected();
        });
        add(UI.scroll(table), BorderLayout.CENTER);

        taAddress.setLineWrap(true);
        taAddress.setWrapStyleWord(true);
        JScrollPane sp = new JScrollPane(taAddress);
        sp.setBorder(BorderFactory.createLineBorder(new Color(0xCBD5E1)));

        JPanel form = UI.card(new GridBagLayout());
        form.setPreferredSize(new Dimension(340, 0));
        UI.formRow(form, 0, "Name", tfName);
        UI.formRow(form, 1, "Contact person", tfContact);
        UI.formRow(form, 2, "Phone", tfPhone);
        UI.formRow(form, 3, "Email", tfEmail);
        UI.formRow(form, 4, "Address", sp);

        JPanel btns = new JPanel(new GridLayout(2, 2, 8, 8));
        btns.setOpaque(false);
        JButton add = UI.button("Add", UI.PRIMARY);
        JButton upd = UI.button("Update", UI.INFO);
        JButton del = UI.button("Delete", UI.DANGER);
        JButton clr = UI.button("Clear", UI.GREY);
        btns.add(add); btns.add(upd); btns.add(del); btns.add(clr);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = 5; g.gridwidth = 2; g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(14, 0, 0, 0);
        form.add(btns, g);
        g.gridy = 6; g.weighty = 1;
        form.add(Box.createVerticalGlue(), g);
        add(form, BorderLayout.EAST);

        add.addActionListener(e -> save(true));
        upd.addActionListener(e -> save(false));
        del.addActionListener(e -> delete());
        clr.addActionListener(e -> clearForm());
        refresh();
    }

    @Override public void refresh() {
        try {
            table.setModel(Db.query("SELECT supplier_id AS ID, name AS Name, contact_person AS Contact, "
                    + "phone AS Phone, email AS Email, address AS Address FROM suppliers ORDER BY name"));
            UI.widths(table, 40, 170, 120, 100, 200, 220);
            selectedId = -1;
        } catch (SQLException ex) { UI.error(this, ex.getMessage()); }
    }

    private void loadSelected() {
        int r = table.getSelectedRow();
        selectedId = Integer.parseInt(table.getValueAt(r, 0).toString());
        tfName.setText(table.getValueAt(r, 1).toString());
        tfContact.setText(table.getValueAt(r, 2).toString());
        tfPhone.setText(table.getValueAt(r, 3).toString());
        tfEmail.setText(table.getValueAt(r, 4).toString());
        taAddress.setText(table.getValueAt(r, 5).toString());
    }

    private void clearForm() {
        selectedId = -1;
        table.clearSelection();
        tfName.setText(""); tfContact.setText(""); tfPhone.setText(""); tfEmail.setText(""); taAddress.setText("");
    }

    private void save(boolean isNew) {
        if (!isNew && selectedId < 0) { UI.error(this, "Select a supplier in the table first."); return; }
        String name = tfName.getText().trim();
        if (name.isEmpty()) { UI.error(this, "Supplier name is required."); return; }
        String email = tfEmail.getText().trim();
        if (!email.isEmpty() && !email.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$")) {
            UI.error(this, "Please enter a valid email address.");
            return;
        }
        try {
            if (isNew) {
                Db.update("INSERT INTO suppliers (name, contact_person, phone, email, address) VALUES (?,?,?,?,?)",
                        name, tfContact.getText().trim(), tfPhone.getText().trim(), email, taAddress.getText().trim());
            } else {
                Db.update("UPDATE suppliers SET name=?, contact_person=?, phone=?, email=?, address=? WHERE supplier_id=?",
                        name, tfContact.getText().trim(), tfPhone.getText().trim(), email,
                        taAddress.getText().trim(), selectedId);
            }
            refresh();
            clearForm();
        } catch (SQLException ex) { UI.error(this, "Database error: " + ex.getMessage()); }
    }

    private void delete() {
        if (selectedId < 0) { UI.error(this, "Select a supplier in the table first."); return; }
        if (!UI.confirm(this, "Delete supplier '" + tfName.getText() + "'?\nTheir medicines will be kept but show no supplier.")) return;
        try {
            Db.update("DELETE FROM suppliers WHERE supplier_id=?", selectedId);
            refresh();
            clearForm();
        } catch (SQLException ex) { UI.error(this, "Database error: " + ex.getMessage()); }
    }
}
