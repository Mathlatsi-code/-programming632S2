package pims;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;

/** Authentication module: secure login with role-based redirection. */
public class LoginFrame extends JFrame {
    private final JTextField tfUser = UI.field(18);
    private final JPasswordField pfPass = UI.passwordField(18);
    private final JLabel lblError = new JLabel(" ");
    private final JButton btnLogin = UI.button("LOGIN", UI.PRIMARY);
    private int failed = 0;

    public LoginFrame() {
        super("HealthFirst Pharmacy - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(820, 480);
        setResizable(false);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(1, 2));

        // Left brand panel
        JPanel left = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, UI.PRIMARY_DARK, getWidth(), getHeight(), UI.PRIMARY));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        JLabel cross = new JLabel("\u271A");
        cross.setFont(new Font("Segoe UI", Font.BOLD, 72));
        cross.setForeground(Color.WHITE);
        JLabel name = new JLabel("HealthFirst");
        name.setFont(new Font("Segoe UI", Font.BOLD, 34));
        name.setForeground(Color.WHITE);
        JLabel sub = new JLabel("Pharmacy Inventory Management");
        sub.setFont(UI.BASE);
        sub.setForeground(new Color(0xCCFBF1));
        cross.setAlignmentX(CENTER_ALIGNMENT);
        name.setAlignmentX(CENTER_ALIGNMENT);
        sub.setAlignmentX(CENTER_ALIGNMENT);
        brand.add(cross);
        brand.add(name);
        brand.add(Box.createVerticalStrut(6));
        brand.add(sub);
        left.add(brand);

        // Right form panel
        JPanel right = new JPanel(new GridBagLayout());
        right.setBackground(Color.WHITE);
        right.setBorder(new EmptyBorder(30, 40, 30, 40));
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1; g.insets = new Insets(4, 0, 4, 0);

        JLabel h = new JLabel("Welcome back");
        h.setFont(UI.TITLE);
        g.gridy = 0; right.add(h, g);
        JLabel hs = new JLabel("Sign in to continue");
        hs.setForeground(UI.GREY);
        g.gridy = 1; g.insets = new Insets(0, 0, 18, 0); right.add(hs, g);
        g.insets = new Insets(4, 0, 4, 0);
        g.gridy = 2; right.add(UI.boldLabel("Username"), g);
        g.gridy = 3; right.add(tfUser, g);
        g.gridy = 4; right.add(UI.boldLabel("Password"), g);
        g.gridy = 5; right.add(pfPass, g);
        JCheckBox show = new JCheckBox("Show password");
        show.setOpaque(false);
        char echo = pfPass.getEchoChar();
        show.addActionListener(e -> pfPass.setEchoChar(show.isSelected() ? (char) 0 : echo));
        g.gridy = 6; right.add(show, g);
        lblError.setForeground(UI.DANGER);
        lblError.setFont(UI.BOLD);
        g.gridy = 7; right.add(lblError, g);
        g.gridy = 8; g.insets = new Insets(8, 0, 4, 0); right.add(btnLogin, g);

        add(left);
        add(right);
        getRootPane().setDefaultButton(btnLogin);
        btnLogin.addActionListener(e -> doLogin());
    }

    private void doLogin() {
        String u = tfUser.getText().trim();
        String p = new String(pfPass.getPassword());
        if (u.isEmpty() || p.isEmpty()) {
            lblError.setText("Please enter your username and password.");
            return;
        }
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT user_id, username, role, full_name FROM users WHERE username=? AND password=?")) {
            ps.setString(1, u);
            ps.setString(2, PasswordUtil.sha256(p));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Session.userId = rs.getInt(1);
                    Session.username = rs.getString(2);
                    Session.role = rs.getString(3);
                    Session.fullName = rs.getString(4);
                    dispose();
                    // Role-based redirection
                    if (Session.isAdmin()) new AdminDashboard().setVisible(true);
                    else new CashierDashboard().setVisible(true);
                    return;
                }
            }
            failed++;
            pfPass.setText("");
            if (failed >= 5) {
                lblError.setText("Too many failed attempts. Wait 10 seconds.");
                btnLogin.setEnabled(false);
                Timer t = new Timer(10000, e -> { btnLogin.setEnabled(true); failed = 0; lblError.setText(" "); });
                t.setRepeats(false);
                t.start();
            } else {
                lblError.setText("Invalid username or password (" + (5 - failed) + " attempts left).");
            }
        } catch (SQLException ex) {
            UI.error(this, "Cannot reach the database:\n" + ex.getMessage());
        }
    }
}
