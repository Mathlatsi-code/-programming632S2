package pims;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

/** Admin: create, delete and manage cashier accounts. */
public class UserPanel extends JPanel implements Refreshable {
    private final JTable table = new JTable();
    private final JTextField tfUser = UI.field(18), tfName = UI.field(18);
    private final JPasswordField pfPass = UI.passwordField(18);
    private int selectedId = -1;
    private String selectedRole = "";

    public UserPanel() {
        super(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        setBackground(Color.WHITE);

        UI.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) loadSelected();
        });
        add(UI.scroll(table), BorderLayout.CENTER);

        JPanel form = UI.card(new GridBagLayout());
        form.setPreferredSize(new Dimension(340, 0));
        UI.formRow(form, 0, "Username", tfUser);
        UI.formRow(form, 1, "Full name", tfName);
        UI.formRow(form, 2, "Password", pfPass);
        JLabel hint = new JLabel("<html><i>New accounts are created as Cashier.<br>To reset a password: select the user, type a new password, click Reset Password.</i></html>");
        hint.setForeground(UI.GREY);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = 3; g.gridwidth = 2; g.fill = GridBagConstraints.HORIZONTAL; g.insets = new Insets(6, 0, 6, 0);
        form.add(hint, g);

        JPanel btns = new JPanel(new GridLayout(3, 2, 8, 8));
        btns.setOpaque(false);
        JButton add = UI.button("Add Cashier", UI.PRIMARY);
        JButton upd = UI.button("Update Name", UI.INFO);
        JButton rst = UI.button("Reset Password", UI.ACCENT);
        JButton del = UI.button("Delete", UI.DANGER);
        JButton clr = UI.button("Clear", UI.GREY);
        btns.add(add); btns.add(upd); btns.add(rst); btns.add(del); btns.add(clr);
        g.gridy = 4; g.insets = new Insets(10, 0, 0, 0);
        form.add(btns, g);
        g.gridy = 5; g.weighty = 1;
        form.add(Box.createVerticalGlue(), g);
        add(form, BorderLayout.EAST);

        add.addActionListener(e -> addCashier());
        upd.addActionListener(e -> updateName());
        rst.addActionListener(e -> resetPassword());
        del.addActionListener(e -> delete());
        clr.addActionListener(e -> clearForm());
        refresh();
    }

    @Override public void refresh() {
        try {
            table.setModel(Db.query("SELECT user_id AS ID, username AS Username, full_name AS `Full Name`, "
                    + "role AS Role FROM users ORDER BY role, username"));
            table.getColumnModel().getColumn(0).setMaxWidth(50);
            selectedId = -1;
        } catch (SQLException ex) { UI.error(this, ex.getMessage()); }
    }

    private void loadSelected() {
        int r = table.getSelectedRow();
        selectedId = Integer.parseInt(table.getValueAt(r, 0).toString());
        tfUser.setText(table.getValueAt(r, 1).toString());
        tfName.setText(table.getValueAt(r, 2).toString());
        selectedRole = table.getValueAt(r, 3).toString();
        pfPass.setText("");
    }

    private void clearForm() {
        selectedId = -1;
        selectedRole = "";
        table.clearSelection();
        tfUser.setText(""); tfName.setText(""); pfPass.setText("");
    }

    private boolean needCashierSelected() {
        if (selectedId < 0) { UI.error(this, "Select a cashier in the table first."); return false; }
        if (!"Cashier".equals(selectedRole)) { UI.error(this, "Only Cashier accounts can be managed here."); return false; }
        return true;
    }

    private void addCashier() {
        String u = tfUser.getText().trim(), n = tfName.getText().trim(), p = new String(pfPass.getPassword());
        if (u.isEmpty() || n.isEmpty() || p.isEmpty()) { UI.error(this, "Username, full name and password are required."); return; }
        if (p.length() < 6) { UI.error(this, "Password must be at least 6 characters."); return; }
        try {
            Db.update("INSERT INTO users (username, password, role, full_name) VALUES (?,?, 'Cashier', ?)",
                    u, PasswordUtil.sha256(p), n);
            refresh();
            clearForm();
            UI.info(this, "Cashier account '" + u + "' created.");
        } catch (SQLIntegrityConstraintViolationException ex) {
            UI.error(this, "That username already exists. Choose another.");
        } catch (SQLException ex) { UI.error(this, "Database error: " + ex.getMessage()); }
    }

    private void updateName() {
        if (!needCashierSelected()) return;
        String n = tfName.getText().trim();
        if (n.isEmpty()) { UI.error(this, "Full name cannot be empty."); return; }
        try {
            Db.update("UPDATE users SET full_name=? WHERE user_id=?", n, selectedId);
            refresh();
            clearForm();
        } catch (SQLException ex) { UI.error(this, ex.getMessage()); }
    }

    private void resetPassword() {
        if (!needCashierSelected()) return;
        String p = new String(pfPass.getPassword());
        if (p.length() < 6) { UI.error(this, "Type a new password of at least 6 characters first."); return; }
        try {
            Db.update("UPDATE users SET password=? WHERE user_id=?", PasswordUtil.sha256(p), selectedId);
            pfPass.setText("");
            UI.info(this, "Password updated.");
        } catch (SQLException ex) { UI.error(this, ex.getMessage()); }
    }

    private void delete() {
        if (!needCashierSelected()) return;
        if (!UI.confirm(this, "Delete cashier '" + tfUser.getText() + "'?")) return;
        try {
            Db.update("DELETE FROM users WHERE user_id=?", selectedId);
            refresh();
            clearForm();
        } catch (SQLIntegrityConstraintViolationException ex) {
            UI.error(this, "This cashier has processed sales, so the account cannot be deleted\n"
                    + "(it would break the sales history). Reset their password instead.");
        } catch (SQLException ex) { UI.error(this, ex.getMessage()); }
    }
}
