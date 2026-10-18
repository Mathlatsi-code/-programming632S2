package pims;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Admin: Sales, Item-Wise, Low Stock and Expiry reports with CSV export. */
public class ReportsPanel extends JPanel implements Refreshable {
    public static final String[] REPORTS = {"Sales Report", "Item-Wise Sales Report", "Low Stock Report", "Expiry Report"};

    private final JComboBox<String> cbReport = new JComboBox<>(REPORTS);
    private final JTextField tfFrom = UI.field(9), tfTo = UI.field(9);
    private final JTable table = new JTable();
    private final JLabel lblSummary = new JLabel(" ");
    private final JLabel lblTitle = new JLabel(" ");

    public ReportsPanel() {
        super(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        setBackground(Color.WHITE);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.setOpaque(false);
        top.add(UI.boldLabel("Report:"));
        top.add(cbReport);
        top.add(UI.boldLabel("From:"));
        tfFrom.setText(LocalDate.now().withDayOfMonth(1).toString());
        top.add(tfFrom);
        top.add(UI.boldLabel("To:"));
        tfTo.setText(LocalDate.now().toString());
        top.add(tfTo);
        JButton gen = UI.button("Generate", UI.PRIMARY);
        JButton exp = UI.button("Export CSV", UI.INFO);
        top.add(gen);
        top.add(exp);
        add(top, BorderLayout.NORTH);

        UI.styleTable(table);
        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        center.add(lblTitle, BorderLayout.NORTH);
        center.add(UI.scroll(table), BorderLayout.CENTER);
        lblSummary.setFont(UI.BOLD);
        lblSummary.setForeground(UI.PRIMARY_DARK);
        center.add(lblSummary, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        cbReport.addActionListener(e -> {
            boolean dated = cbReport.getSelectedIndex() <= 1;
            tfFrom.setEnabled(dated);
            tfTo.setEnabled(dated);
            generate();
        });
        gen.addActionListener(e -> generate());
        exp.addActionListener(e -> exportCsv());
        generate();
    }

    @Override public void refresh() { generate(); }

    private void generate() {
        try {
            int idx = cbReport.getSelectedIndex();
            lblTitle.setText(REPORTS[idx]);
            Date from = null, to = null;
            if (idx <= 1) {
                try {
                    from = Date.valueOf(LocalDate.parse(tfFrom.getText().trim()));
                    to = Date.valueOf(LocalDate.parse(tfTo.getText().trim()));
                } catch (DateTimeParseException ex) {
                    UI.error(this, "Dates must be in the format yyyy-mm-dd.");
                    return;
                }
                if (from.after(to)) { UI.error(this, "'From' date cannot be after 'To' date."); return; }
                lblTitle.setText(REPORTS[idx] + "  (" + from + " to " + to + ")");
            }
            DefaultTableModel m;
            String summary;
            switch (idx) {
                case 0:
                    m = Db.query("SELECT s.sale_id AS `Bill No`, s.sale_date AS `Date & Time`, u.full_name AS Cashier, "
                            + "SUM(si.quantity_sold) AS `Items Sold`, s.total_amount AS Total "
                            + "FROM sales s JOIN users u ON u.user_id = s.user_id "
                            + "JOIN sale_items si ON si.sale_id = s.sale_id "
                            + "WHERE s.sale_date >= ? AND s.sale_date < DATE_ADD(?, INTERVAL 1 DAY) "
                            + "GROUP BY s.sale_id, s.sale_date, u.full_name, s.total_amount ORDER BY s.sale_date DESC", from, to);
                    Object cnt = Db.scalar("SELECT COUNT(*) FROM sales WHERE sale_date >= ? AND sale_date < DATE_ADD(?, INTERVAL 1 DAY)", from, to);
                    Object sum = Db.scalar("SELECT COALESCE(SUM(total_amount),0) FROM sales WHERE sale_date >= ? AND sale_date < DATE_ADD(?, INTERVAL 1 DAY)", from, to);
                    summary = "Transactions: " + cnt + "     Total revenue: " + UI.money(new java.math.BigDecimal(sum.toString()));
                    break;
                case 1:
                    m = Db.query("SELECT m.name AS Medicine, m.company AS Company, SUM(si.quantity_sold) AS `Units Sold`, "
                            + "SUM(si.quantity_sold * si.price_at_sale) AS Revenue "
                            + "FROM sale_items si JOIN sales s ON s.sale_id = si.sale_id "
                            + "JOIN medicines m ON m.medicine_id = si.medicine_id "
                            + "WHERE s.sale_date >= ? AND s.sale_date < DATE_ADD(?, INTERVAL 1 DAY) "
                            + "GROUP BY m.medicine_id, m.name, m.company ORDER BY Revenue DESC", from, to);
                    Object units = Db.scalar("SELECT COALESCE(SUM(si.quantity_sold),0) FROM sale_items si JOIN sales s ON s.sale_id=si.sale_id "
                            + "WHERE s.sale_date >= ? AND s.sale_date < DATE_ADD(?, INTERVAL 1 DAY)", from, to);
                    summary = "Different medicines sold: " + m.getRowCount() + "     Total units sold: " + units;
                    break;
                case 2:
                    m = Db.query("SELECT m.name AS Medicine, m.company AS Company, m.quantity_in_stock AS `In Stock`, "
                            + "m.reorder_level AS `Reorder Level`, (m.reorder_level - m.quantity_in_stock) AS Shortfall, "
                            + "COALESCE(s.name,'-') AS Supplier, COALESCE(s.phone,'-') AS `Supplier Phone` "
                            + "FROM medicines m LEFT JOIN suppliers s ON s.supplier_id = m.supplier_id "
                            + "WHERE m.quantity_in_stock <= m.reorder_level ORDER BY Shortfall DESC");
                    summary = "Medicines at or below reorder level: " + m.getRowCount();
                    break;
                default:
                    m = Db.query("SELECT m.name AS Medicine, m.company AS Company, m.quantity_in_stock AS `In Stock`, "
                            + "m.expiry_date AS `Expiry Date`, DATEDIFF(m.expiry_date, CURDATE()) AS `Days Left`, "
                            + "CASE WHEN m.expiry_date < CURDATE() THEN 'EXPIRED' ELSE 'Expiring soon' END AS Status "
                            + "FROM medicines m WHERE m.expiry_date <= DATE_ADD(CURDATE(), INTERVAL 1 MONTH) "
                            + "ORDER BY m.expiry_date");
                    summary = "Medicines expired or expiring within the next month: " + m.getRowCount();
            }
            table.setModel(m);
            lblSummary.setText(summary);
        } catch (SQLException ex) {
            UI.error(this, "Could not generate report: " + ex.getMessage());
        }
    }

    private void exportCsv() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(cbReport.getSelectedItem().toString().replace(' ', '_') + ".csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try (Writer w = new OutputStreamWriter(new FileOutputStream(fc.getSelectedFile()), StandardCharsets.UTF_8)) {
            DefaultTableModel m = (DefaultTableModel) table.getModel();
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < m.getColumnCount(); c++) sb.append(c > 0 ? "," : "").append(q(m.getColumnName(c)));
            sb.append("\r\n");
            for (int r = 0; r < m.getRowCount(); r++) {
                for (int c = 0; c < m.getColumnCount(); c++) sb.append(c > 0 ? "," : "").append(q(String.valueOf(m.getValueAt(r, c))));
                sb.append("\r\n");
            }
            w.write(sb.toString());
            UI.info(this, "Report saved to:\n" + fc.getSelectedFile().getAbsolutePath());
        } catch (IOException ex) { UI.error(this, "Could not save file: " + ex.getMessage()); }
    }

    private static String q(String s) { return "\"" + s.replace("\"", "\"\"") + "\""; }
}
