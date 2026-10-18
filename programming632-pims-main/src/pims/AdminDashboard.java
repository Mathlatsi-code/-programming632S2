package pims;

import javax.swing.*;
import java.awt.*;

/** Administrator dashboard: Overview, Medicines, Suppliers, Users, Reports. */
public class AdminDashboard extends JFrame {
    public AdminDashboard() {
        super("HealthFirst PIMS - Administrator");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1320, 760);
        setMinimumSize(new Dimension(1100, 650));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(UI.headerBar("HealthFirst Pharmacy  |  Admin Dashboard",
                Session.fullName + "  (" + Session.role + ")", e -> logout()), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("  Overview  ", new OverviewPanel());
        tabs.addTab("  Manage Medicines  ", new MedicinePanel());
        tabs.addTab("  Manage Suppliers  ", new SupplierPanel());
        tabs.addTab("  Manage Users  ", new UserPanel());
        tabs.addTab("  Reports  ", new ReportsPanel());
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
