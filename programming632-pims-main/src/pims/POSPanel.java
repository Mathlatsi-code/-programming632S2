package pims;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Cashier: Point of Sale. Search medicines, build a cart, checkout and print a bill. */
public class POSPanel extends JPanel implements Refreshable {
    /** One line in the cart. */
    private static final class Line {
        int id; String name; BigDecimal price; int qty; int stock;
        BigDecimal subtotal() { return price.multiply(BigDecimal.valueOf(qty)); }
    }

    private final List<Line> cart = new ArrayList<>();
    private final JTextField tfSearch = UI.field(18);
    private final JTable tblMeds = new JTable();
    private final JTable tblCart = new JTable();
    private final JSpinner spQty = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
    private final JLabel lblTotal = new JLabel("R0.00");
    private final JLabel lblItems = new JLabel("0 items");

    public POSPanel() {
        super(new GridLayout(1, 2, 14, 0));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        setBackground(Color.WHITE);

        // ---------- left: medicine picker ----------
        JPanel left = new JPanel(new BorderLayout(0, 10));
        left.setOpaque(false);
        JPanel search = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        search.setOpaque(false);
        search.add(UI.boldLabel("Find medicine:"));
        search.add(tfSearch);
        JButton go = UI.button("Search", UI.PRIMARY);
        search.add(go);
        left.add(search, BorderLayout.NORTH);

        UI.styleTable(tblMeds);
        tblMeds.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        left.add(UI.scroll(tblMeds), BorderLayout.CENTER);

        JPanel addBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        addBar.setOpaque(false);
        addBar.add(UI.boldLabel("Quantity:"));
        spQty.setPreferredSize(new Dimension(80, 34));
        addBar.add(spQty);
        JButton add = UI.button("Add to Cart  \u2192", UI.PRIMARY);
        addBar.add(add);
        left.add(addBar, BorderLayout.SOUTH);

        // ---------- right: cart ----------
        JPanel right = UI.card(new BorderLayout(0, 10));
        JLabel cartTitle = new JLabel("Current Sale");
        cartTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        right.add(cartTitle, BorderLayout.NORTH);
        UI.styleTable(tblCart);
        tblCart.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        right.add(UI.scroll(tblCart), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(0, 10));
        bottom.setOpaque(false);
        JPanel totals = new JPanel(new BorderLayout());
        totals.setOpaque(false);
        lblItems.setForeground(UI.GREY);
        JLabel tl = new JLabel("TOTAL");
        tl.setFont(UI.BOLD);
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 30));
        lblTotal.setForeground(UI.PRIMARY_DARK);
        JPanel tleft = new JPanel(new GridLayout(2, 1));
        tleft.setOpaque(false);
        tleft.add(tl);
        tleft.add(lblItems);
        totals.add(tleft, BorderLayout.WEST);
        totals.add(lblTotal, BorderLayout.EAST);
        bottom.add(totals, BorderLayout.NORTH);

        JPanel btns = new JPanel(new GridLayout(1, 3, 8, 0));
        btns.setOpaque(false);
        JButton rem = UI.button("Remove Item", UI.ACCENT);
        JButton clr = UI.button("Clear Cart", UI.DANGER);
        JButton pay = UI.button("CHECKOUT", UI.PRIMARY);
        btns.add(rem); btns.add(clr); btns.add(pay);
        bottom.add(btns, BorderLayout.SOUTH);
        right.add(bottom, BorderLayout.SOUTH);

        add(left);
        add(right);

        go.addActionListener(e -> loadMedicines());
        tfSearch.addActionListener(e -> loadMedicines());
        add.addActionListener(e -> addToCart());
        tblMeds.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2) addToCart(); }
        });
        rem.addActionListener(e -> removeSelected());
        clr.addActionListener(e -> clearCart(true));
        pay.addActionListener(e -> checkout());
        refreshCart();
        loadMedicines();
    }

    @Override public void refresh() { loadMedicines(); }

    private void loadMedicines() {
        try {
            String like = "%" + tfSearch.getText().trim() + "%";
            tblMeds.setModel(Db.query(
                    "SELECT medicine_id AS ID, name AS Medicine, medicine_type AS Type, price AS Price, "
                            + "quantity_in_stock AS `In Stock`, expiry_date AS Expiry FROM medicines "
                            + "WHERE quantity_in_stock > 0 AND expiry_date >= CURDATE() "
                            + "AND (name LIKE ? OR company LIKE ? OR medicine_type LIKE ?) ORDER BY name", like, like, like));
            UI.widths(tblMeds, 35, 190, 70, 70, 65, 95);
        } catch (SQLException ex) { UI.error(this, ex.getMessage()); }
    }

    private void addToCart() {
        int row = tblMeds.getSelectedRow();
        if (row < 0) { UI.error(this, "Select a medicine from the list first."); return; }
        int id = Integer.parseInt(tblMeds.getValueAt(row, 0).toString());
        int qty = (Integer) spQty.getValue();
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT name, price, quantity_in_stock, expiry_date FROM medicines WHERE medicine_id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) { UI.error(this, "That medicine no longer exists."); return; }
                if (rs.getDate(4).toLocalDate().isBefore(LocalDate.now())) {
                    UI.error(this, "This medicine has expired and cannot be sold.");
                    return;
                }
                Line existing = null;
                for (Line l : cart) if (l.id == id) existing = l;
                int already = existing == null ? 0 : existing.qty;
                int stock = rs.getInt(3);
                if (already + qty > stock) {
                    UI.error(this, "Only " + stock + " in stock" + (already > 0 ? " (" + already + " already in cart)." : "."));
                    return;
                }
                if (existing != null) existing.qty += qty;
                else {
                    Line l = new Line();
                    l.id = id; l.name = rs.getString(1); l.price = rs.getBigDecimal(2); l.qty = qty; l.stock = stock;
                    cart.add(l);
                }
            }
        } catch (SQLException ex) { UI.error(this, ex.getMessage()); return; }
        spQty.setValue(1);
        refreshCart();
    }

    private void removeSelected() {
        int r = tblCart.getSelectedRow();
        if (r < 0) { UI.error(this, "Select a cart item to remove."); return; }
        cart.remove(r);
        refreshCart();
    }

    private void clearCart(boolean ask) {
        if (cart.isEmpty()) return;
        if (ask && !UI.confirm(this, "Remove all items from the cart?")) return;
        cart.clear();
        refreshCart();
    }

    private void refreshCart() {
        DefaultTableModel m = new DefaultTableModel(new Object[]{"Medicine", "Price", "Qty", "Subtotal"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        BigDecimal total = BigDecimal.ZERO;
        int items = 0;
        for (Line l : cart) {
            m.addRow(new Object[]{l.name, UI.money(l.price), l.qty, UI.money(l.subtotal())});
            total = total.add(l.subtotal());
            items += l.qty;
        }
        tblCart.setModel(m);
        tblCart.getColumnModel().getColumn(0).setPreferredWidth(190);
        tblCart.getColumnModel().getColumn(2).setMaxWidth(50);
        lblTotal.setText(UI.money(total));
        lblItems.setText(items + (items == 1 ? " item" : " items"));
    }

    private void checkout() {
        if (cart.isEmpty()) { UI.error(this, "The cart is empty."); return; }
        if (!UI.confirm(this, "Complete this sale for " + lblTotal.getText() + "?")) return;
        Connection c = null;
        try {
            c = DBConnection.get();
            c.setAutoCommit(false);
            BigDecimal total = BigDecimal.ZERO;
            // lock the rows, validate stock/expiry and use the current database price
            for (Line l : cart) {
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT price, quantity_in_stock, expiry_date FROM medicines WHERE medicine_id=? FOR UPDATE")) {
                    ps.setInt(1, l.id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException(l.name + " no longer exists.");
                        if (rs.getInt(2) < l.qty)
                            throw new SQLException("Not enough stock for " + l.name + " (only " + rs.getInt(2) + " left).");
                        if (rs.getDate(3).toLocalDate().isBefore(LocalDate.now()))
                            throw new SQLException(l.name + " has expired.");
                        l.price = rs.getBigDecimal(1);
                        total = total.add(l.subtotal());
                    }
                }
            }
            int saleId;
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO sales (total_amount, user_id) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setBigDecimal(1, total);
                ps.setInt(2, Session.userId);
                ps.executeUpdate();
                try (ResultSet k = ps.getGeneratedKeys()) { k.next(); saleId = k.getInt(1); }
            }
            for (Line l : cart) {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO sale_items (sale_id, medicine_id, quantity_sold, price_at_sale) VALUES (?,?,?,?)")) {
                    ps.setInt(1, saleId); ps.setInt(2, l.id); ps.setInt(3, l.qty); ps.setBigDecimal(4, l.price);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE medicines SET quantity_in_stock = quantity_in_stock - ? WHERE medicine_id=?")) {
                    ps.setInt(1, l.qty); ps.setInt(2, l.id);
                    ps.executeUpdate();
                }
            }
            c.commit();
            cart.clear();
            refreshCart();
            loadMedicines();
            BillDialog.show(SwingUtilities.getWindowAncestor(this), saleId);
        } catch (SQLException ex) {
            try { if (c != null) c.rollback(); } catch (SQLException ignored) { }
            UI.error(this, "Sale cancelled: " + ex.getMessage());
            loadMedicines();
        } finally {
            try { if (c != null) c.close(); } catch (SQLException ignored) { }
        }
    }
}
