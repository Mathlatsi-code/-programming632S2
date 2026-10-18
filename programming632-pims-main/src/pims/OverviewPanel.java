package pims;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;

/** Admin landing tab with headline numbers. */
public class OverviewPanel extends JPanel implements Refreshable {
    private final JLabel[] values = new JLabel[6];
    private static final String[] TITLES = {"Medicines in catalogue", "Low-stock medicines", "Expiring within 1 month",
            "Already expired", "Today's sales", "Total revenue (all time)"};
    private static final Color[] COLORS = {UI.PRIMARY, UI.ACCENT, UI.INFO, UI.DANGER, UI.PRIMARY_DARK, UI.GREY};

    public OverviewPanel() {
        super(new BorderLayout(0, 14));
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        setBackground(Color.WHITE);
        JLabel hello = new JLabel("Welcome, " + Session.fullName);
        hello.setFont(UI.TITLE);
        JLabel sub = new JLabel("Here is how HealthFirst is doing right now.");
        sub.setForeground(UI.GREY);
        JPanel head = new JPanel(new GridLayout(2, 1));
        head.setOpaque(false);
        head.add(hello);
        head.add(sub);
        add(head, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 3, 16, 16));
        grid.setOpaque(false);
        for (int i = 0; i < 6; i++) {
            final Color col = COLORS[i];
            JPanel card = new JPanel(new GridLayout(2, 1)) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(UI.BG);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                    g2.setColor(col);
                    g2.fillRoundRect(0, 0, 8, getHeight(), 8, 8);
                    g2.dispose();
                }
            };
            card.setOpaque(false);
            card.setBorder(BorderFactory.createEmptyBorder(14, 24, 14, 14));
            values[i] = new JLabel("-");
            values[i].setFont(new Font("Segoe UI", Font.BOLD, 30));
            values[i].setForeground(col);
            JLabel t = new JLabel(TITLES[i]);
            t.setForeground(UI.GREY);
            card.add(values[i]);
            card.add(t);
            grid.add(card);
        }
        add(grid, BorderLayout.CENTER);
        refresh();
    }

    @Override public void refresh() {
        try {
            values[0].setText(String.valueOf(Db.scalar("SELECT COUNT(*) FROM medicines")));
            values[1].setText(String.valueOf(Db.scalar("SELECT COUNT(*) FROM medicines WHERE quantity_in_stock <= reorder_level")));
            values[2].setText(String.valueOf(Db.scalar("SELECT COUNT(*) FROM medicines WHERE expiry_date >= CURDATE() AND expiry_date <= DATE_ADD(CURDATE(), INTERVAL 1 MONTH)")));
            values[3].setText(String.valueOf(Db.scalar("SELECT COUNT(*) FROM medicines WHERE expiry_date < CURDATE()")));
            values[4].setText(UI.money(new BigDecimal(Db.scalar("SELECT COALESCE(SUM(total_amount),0) FROM sales WHERE DATE(sale_date)=CURDATE()").toString())));
            values[5].setText(UI.money(new BigDecimal(Db.scalar("SELECT COALESCE(SUM(total_amount),0) FROM sales").toString())));
        } catch (SQLException ex) { UI.error(this, ex.getMessage()); }
    }
}
