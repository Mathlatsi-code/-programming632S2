package pims;

import javax.swing.*;

/** Application entry point. */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UI.install();
            try {
                DatabaseSetup.ensure();
            } catch (Exception ex) {
                UI.error(null, "Could not connect to / set up the MySQL database.\n\n"
                        + "1. Make sure MySQL (e.g. XAMPP) is running on localhost:3306.\n"
                        + "2. Check the user/password in db.properties (default: root, empty password).\n"
                        + "3. Or run database.sql manually in MySQL Workbench.\n\nDetails: " + ex.getMessage());
                System.exit(1);
            }
            new LoginFrame().setVisible(true);
        });
    }
}
