package pims;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.util.Locale;

/** Shared look-and-feel helpers so every screen has the same clean design. */
public final class UI {
    public static final Color PRIMARY = new Color(0x0F766E);
    public static final Color PRIMARY_DARK = new Color(0x134E4A);
    public static final Color PRIMARY_LIGHT = new Color(0xCCFBF1);
    public static final Color ACCENT = new Color(0xF59E0B);
    public static final Color DANGER = new Color(0xDC2626);
    public static final Color INFO = new Color(0x2563EB);
    public static final Color GREY = new Color(0x64748B);
    public static final Color BG = new Color(0xF1F5F9);
    public static final Color LINE = new Color(0xE2E8F0);
    public static final Color TEXT = new Color(0x0F172A);

    public static final Font BASE = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font BOLD = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font TITLE = new Font("Segoe UI", Font.BOLD, 22);

    private UI() { }

    public static String money(BigDecimal v) {
        return String.format(Locale.US, "R%,.2f", v);
    }

    public static void install() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) { }
        for (String k : new String[]{"Label.font", "TextField.font", "PasswordField.font", "ComboBox.font",
                "Table.font", "Spinner.font", "OptionPane.messageFont", "OptionPane.buttonFont",
                "CheckBox.font", "TextArea.font", "ToolTip.font"}) {
            UIManager.put(k, BASE);
        }
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Panel.background", BG);
        UIManager.put("OptionPane.background", Color.WHITE);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("TabbedPane.font", BOLD);
        UIManager.put("TabbedPane.selected", Color.WHITE);
        UIManager.put("TabbedPane.contentAreaColor", Color.WHITE);
        UIManager.put("TabbedPane.focus", PRIMARY);
        UIManager.put("TabbedPane.borderHightlightColor", PRIMARY);
    }

    public static JButton button(String text, Color bg) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c = bg;
                if (!isEnabled()) c = new Color(0xCBD5E1);
                else if (getModel().isPressed()) c = bg.darker();
                else if (getModel().isRollover()) c = bg.brighter();
                g2.setColor(c);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setForeground(Color.WHITE);
        b.setFont(BOLD);
        b.setBorder(new EmptyBorder(9, 18, 9, 18));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setRolloverEnabled(true);
        return b;
    }

    public static JTextField field(int cols) {
        JTextField t = new JTextField(cols);
        t.setBorder(inputBorder());
        return t;
    }

    public static JPasswordField passwordField(int cols) {
        JPasswordField t = new JPasswordField(cols);
        t.setBorder(inputBorder());
        return t;
    }

    private static Border inputBorder() {
        return new CompoundBorder(new LineBorder(new Color(0xCBD5E1), 1, true), new EmptyBorder(6, 8, 6, 8));
    }

    public static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(BASE);
        return l;
    }

    public static JLabel boldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(BOLD);
        return l;
    }

    public static JPanel card(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setBackground(Color.WHITE);
        p.setBorder(new CompoundBorder(new LineBorder(LINE, 1, true), new EmptyBorder(14, 14, 14, 14)));
        return p;
    }

    public static void formRow(JPanel p, int r, String label, JComponent c) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = r; g.anchor = GridBagConstraints.WEST; g.insets = new Insets(5, 2, 5, 10);
        p.add(label(label), g);
        g.gridx = 1; g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1;
        p.add(c, g);
    }

    public static JPanel headerBar(String title, String userText, ActionListener onLogout) {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, PRIMARY_DARK, getWidth(), 0, PRIMARY));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(12, 20, 12, 20));
        JLabel t = new JLabel("\u271A  " + title);
        t.setFont(TITLE);
        t.setForeground(Color.WHITE);
        bar.add(t, BorderLayout.WEST);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        JLabel u = new JLabel(userText);
        u.setForeground(new Color(0xCCFBF1));
        u.setFont(BASE);
        JButton out = button("Logout", ACCENT);
        out.addActionListener(onLogout);
        right.add(u);
        right.add(out);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    public static void styleTable(JTable t) {
        t.setFont(BASE);
        t.setRowHeight(28);
        t.setGridColor(LINE);
        t.setShowVerticalLines(false);
        t.setSelectionBackground(PRIMARY_LIGHT);
        t.setSelectionForeground(TEXT);
        t.setFillsViewportHeight(true);
        t.setAutoCreateRowSorter(false);
        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean s, boolean f, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tb, v, false, false, r, c);
                l.setBackground(PRIMARY_DARK);
                l.setForeground(Color.WHITE);
                l.setFont(BOLD);
                l.setBorder(new EmptyBorder(8, 8, 8, 8));
                l.setHorizontalAlignment(SwingConstants.LEFT);
                return l;
            }
        });
        h.setPreferredSize(new Dimension(100, 36));
    }

    /** Sets preferred column widths (0 = leave flexible). */
    public static void widths(JTable t, int... w) {
        for (int i = 0; i < w.length && i < t.getColumnCount(); i++) {
            if (w[i] > 0) t.getColumnModel().getColumn(i).setPreferredWidth(w[i]);
        }
    }

    public static JScrollPane scroll(JComponent c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(new LineBorder(LINE, 1));
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    public static void error(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void info(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "HealthFirst PIMS", JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirm(Component parent, String msg) {
        return JOptionPane.showConfirmDialog(parent, msg, "Please confirm", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }
}
