package pims;

import javax.swing.*;
import java.awt.*;

/** Cashier dashboard: Point of Sale and Stock Check only. */
public class CashierDashboard extends JFrame {
    public CashierDashboard() {
        super("HealthFirst PIMS - Cashier");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1240, 740);
        setMinimumSize(new Dimension(1050, 620));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(UI.headerBar("HealthFirst Pharmacy  |  Cashier",
                Session.fullName + "  (" + Session.role + ")", e -> logout()), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("  Point of Sale  ", new POSPanel());
        tabs.addTab("  Stock Check  ", new StockCheckPanel());
        tabs.addChangeListener(e -> {
            Component c = tabs.getSelectedComponent();
            if (c instanceof Refreshable) ((Refreshable) c).refresh();
        });
        JPanel body = new JPanel(new BorderLayout());
        body.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        body.add(tabs);
        add(body, BorderLayout.CENTER);
    }

    private void logout() {
        Session.clear();
        dispose();
        new LoginFrame().setVisible(true);
    }
}
