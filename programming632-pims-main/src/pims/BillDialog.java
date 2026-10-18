package pims;

import javax.swing.*;
import java.awt.*;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.*;
import java.text.SimpleDateFormat;

/** The bill window: shows the receipt for a sale and lets the cashier print or save it. */
public class BillDialog extends JDialog {
    private final JTextArea area = new JTextArea();
    private final int saleId;

    public static void show(Window owner, int saleId) {
        new BillDialog(owner, saleId).setVisible(true);
    }

    private BillDialog(Window owner, int saleId) {
        super(owner, "Bill #" + saleId, ModalityType.APPLICATION_MODAL);
        this.saleId = saleId;
        setSize(480, 620);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(0, 10));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        getContentPane().setBackground(UI.BG);

        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setEditable(false);
        area.setMargin(new Insets(14, 14, 14, 14));
        area.setText(buildBill());
        area.setCaretPosition(0);
        add(UI.scroll(area), BorderLayout.CENTER);

        JPanel btns = new JPanel(new GridLayout(1, 3, 8, 0));
        btns.setOpaque(false);
        JButton print = UI.button("Print", UI.INFO);
        JButton save = UI.button("Save to file", UI.PRIMARY);
        JButton close = UI.button("Close", UI.GREY);
        btns.add(print); btns.add(save); btns.add(close);
        add(btns, BorderLayout.SOUTH);

        print.addActionListener(e -> {
            try { area.print(); } catch (PrinterException ex) { UI.error(this, "Printing failed: " + ex.getMessage()); }
        });
        save.addActionListener(e -> saveFile());
        close.addActionListener(e -> dispose());
    }

    private String buildBill() {
        StringBuilder sb = new StringBuilder();
        String line = "------------------------------------------\n";
        try (Connection c = DBConnection.get()) {
            String cashier; Timestamp when; BigDecimal total;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT s.sale_date, s.total_amount, u.full_name FROM sales s JOIN users u ON u.user_id=s.user_id WHERE s.sale_id=?")) {
                ps.setInt(1, saleId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    when = rs.getTimestamp(1); total = rs.getBigDecimal(2); cashier = rs.getString(3);
                }
            }
            sb.append("          HEALTHFIRST PHARMACY\n");
            sb.append("        Your health comes first\n");
            sb.append(line);
            sb.append(String.format("Bill No : %06d%n", saleId));
            sb.append("Date    : ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(when)).append('\n');
            sb.append("Cashier : ").append(cashier).append('\n');
            sb.append(line);
            sb.append(String.format("%-20s %4s %8s %9s%n", "Item", "Qty", "Price", "Amount"));
            sb.append(line);
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT m.name, si.quantity_sold, si.price_at_sale FROM sale_items si "
                            + "JOIN medicines m ON m.medicine_id=si.medicine_id WHERE si.sale_id=? ORDER BY si.sale_item_id")) {
                ps.setInt(1, saleId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int q = rs.getInt(2);
                        BigDecimal p = rs.getBigDecimal(3);
                        sb.append(String.format("%-20.20s %4d %8s %9s%n", rs.getString(1), q,
                                UI.money(p), UI.money(p.multiply(BigDecimal.valueOf(q)))));
                    }
                }
            }
            sb.append(line);
            BigDecimal vat = total.multiply(new BigDecimal("15")).divide(new BigDecimal("115"), 2, RoundingMode.HALF_UP);
            sb.append(String.format("%-32s %9s%n", "VAT (15%) included:", UI.money(vat)));
            sb.append(String.format("%-32s %9s%n", "TOTAL DUE:", UI.money(total)));
            sb.append(line);
            sb.append("     Thank you for shopping with us!\n");
            sb.append("  Keep your slip for returns & queries\n");
        } catch (SQLException ex) {
            sb.append("Could not load bill: ").append(ex.getMessage());
        }
        return sb.toString();
    }

    private void saveFile() {
        try {
            File dir = new File("bills");
            dir.mkdirs();
            File f = new File(dir, String.format("Bill_%06d.txt", saleId));
            Files.write(f.toPath(), area.getText().getBytes(StandardCharsets.UTF_8));
            UI.info(this, "Bill saved to:\n" + f.getAbsolutePath());
        } catch (IOException ex) { UI.error(this, "Could not save bill: " + ex.getMessage()); }
    }
}
