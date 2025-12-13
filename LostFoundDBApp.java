
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.sql.*;
import java.time.LocalDate;

/**
 * LostFoundDBApp.java
 * Single-file Lost & Found Management System (Swing + MySQL)
 * All buttons use a BlueButton style (gradient blue, white text, rounded).
 *
 * BEFORE RUNNING:
 * 1) Run the SQL script to create lostfound_db with tables/procedures/triggers.
 * 2) Add MySQL Connector/J to project libraries.
 * 3) Update DB_USER / DB_PASSWORD if needed.
 */

public class LostFoundDBApp extends JFrame {

    // === DATABASE CONFIG ===
    private static final String DB_URL = "jdbc:mysql://localhost:3306/lostfound_db?useSSL=false&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = ""; // <- set your MySQL password here

    // === THEME ===
    private final Color ROYAL_BLUE = new Color(30, 75, 216); // #1E4BD8
    private final Color LIGHT_BG = Color.WHITE;
    private final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 26);
    private final Font SUB_FONT = new Font("Segoe UI", Font.BOLD, 18);
    private final Font NORMAL_FONT = new Font("Segoe UI", Font.PLAIN, 14);

    // === DB connection ===
    private Connection conn;

    // === Shared UI ===
    private CardLayout cardLayout = new CardLayout();
    private JPanel cards = new JPanel(cardLayout);

    // Login components
    private JTextField loginEmail;
    private JPasswordField loginPassword;

    // Add item components
    private JTextField aiName, aiCategory, aiLocation, aiDate;
    private JTextArea aiDescription;

    // Tables and models
    private DefaultTableModel itemsModel;
    private JTable itemsTable;

    private DefaultTableModel claimsModel;
    private JTable claimsTable;

    private DefaultTableModel logsModel;
    private JTable logsTable;

    // Logged-in user
    private int currentUserId = -1;
    private String currentUserName = "Guest";
    private String currentUserRole = "guest";

    // ---------------- Constructor ----------------
    public LostFoundDBApp() {
        setTitle("Lost & Found Management");
        setSize(1150, 760);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(LIGHT_BG);

        // Connect to DB
        connectDatabase();

        // Header + side nav + cards
        add(createHeader(), BorderLayout.NORTH);
        add(createSideNav(), BorderLayout.WEST);

        // Create screens (cards)
        cards.add(createLoginPanel(), "LOGIN");
        cards.add(createDashboardPanel(), "DASH");
        cards.add(createAddItemPanel(), "ADD_ITEM");
        cards.add(createViewItemsPanel(), "VIEW_ITEMS");
        cards.add(createClaimsPanel(), "CLAIMS");
        cards.add(createLogsPanel(), "LOGS");

        add(cards, BorderLayout.CENTER);

        // Start on login screen
        cardLayout.show(cards, "LOGIN");
        setVisible(true);
    }

    // ---------------- Database ----------------
    private void connectDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("Connected to database.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "DB Connection failed: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void closeConnection() {
        try { if (conn != null && !conn.isClosed()) conn.close(); } catch (Exception ignored) {}
    }

    // ---------------- Header ----------------
    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ROYAL_BLUE);
        header.setPreferredSize(new Dimension(0, 72));
        header.setBorder(new EmptyBorder(8, 16, 8, 16));

        JLabel title = new JLabel("Lost & Found Management System");
        title.setFont(TITLE_FONT);
        title.setForeground(Color.WHITE);

        JLabel userLabel = new JLabel("Not logged in");
        userLabel.setFont(NORMAL_FONT);
        userLabel.setForeground(Color.WHITE);

        // Update logged in user label periodically
        Timer t = new Timer(400, e -> {
            if (currentUserId > 0) userLabel.setText(currentUserName + " (" + currentUserRole + ")");
            else userLabel.setText("Not logged in");
        });
        t.start();

        header.add(title, BorderLayout.WEST);
        header.add(userLabel, BorderLayout.EAST);
        return header;
    }

    // ---------------- Side Nav ----------------
    private JPanel createSideNav() {
        JPanel nav = new JPanel();
        nav.setPreferredSize(new Dimension(230, getHeight()));
        nav.setBackground(new Color(245, 247, 255));
        nav.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10, 10, 10, 10);
        c.gridx = 0; c.fill = GridBagConstraints.HORIZONTAL;

        JLabel logo = new JLabel("LOST & FOUND", SwingConstants.CENTER);
        logo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        logo.setForeground(ROYAL_BLUE);
        logo.setBorder(new EmptyBorder(12, 8, 12, 8));
        c.gridy = 0; nav.add(logo, c);

        BlueButton btnLogin = new BlueButton("Login");
        c.gridy = 1; nav.add(btnLogin, c);

        BlueButton btnDash = new BlueButton("Dashboard");
        c.gridy = 2; nav.add(btnDash, c);

        BlueButton btnAdd = new BlueButton("Add Item");
        c.gridy = 3; nav.add(btnAdd, c);

        BlueButton btnView = new BlueButton("View Items");
        c.gridy = 4; nav.add(btnView, c);

        BlueButton btnClaims = new BlueButton("Claims");
        c.gridy = 5; nav.add(btnClaims, c);

        BlueButton btnLogs = new BlueButton("Logs");
        c.gridy = 6; nav.add(btnLogs, c);

        BlueButton btnLogout = new BlueButton("Logout");
        c.gridy = 7; nav.add(btnLogout, c);

        BlueButton btnExit = new BlueButton("Exit");
        c.gridy = 8; nav.add(btnExit, c);

        // Actions (all use BlueButton style)
        btnLogin.addActionListener(e -> cardLayout.show(cards, "LOGIN"));
        btnDash.addActionListener(e -> { refreshDashboard(); cardLayout.show(cards, "DASH"); });
        btnAdd.addActionListener(e -> { resetAddForm(); cardLayout.show(cards, "ADD_ITEM"); });
        btnView.addActionListener(e -> { loadItems(); cardLayout.show(cards, "VIEW_ITEMS"); });
        btnClaims.addActionListener(e -> { loadClaims(); cardLayout.show(cards, "CLAIMS"); });
        btnLogs.addActionListener(e -> { loadLogs(); cardLayout.show(cards, "LOGS"); });
        btnLogout.addActionListener(e -> doLogout());
        btnExit.addActionListener(e -> { closeConnection(); System.exit(0); });

        return nav;
    }

    // ---------------- Login Panel ----------------
    private JPanel createLoginPanel() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(LIGHT_BG);

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(new EmptyBorder(20, 20, 20, 20));
        card.setPreferredSize(new Dimension(560, 420));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(12, 12, 12, 12);
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel labelTitle = new JLabel("Welcome back");
        labelTitle.setFont(SUB_FONT);
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2; card.add(labelTitle, c);
        c.gridwidth = 1;

        c.gridy++; c.gridx = 0; card.add(new JLabel("Email"), c);
        loginEmail = new JTextField();
        loginEmail.setFont(NORMAL_FONT);
        c.gridx = 1; card.add(loginEmail, c);

        c.gridy++; c.gridx = 0; card.add(new JLabel("Password"), c);
        loginPassword = new JPasswordField();
        loginPassword.setFont(NORMAL_FONT);
        c.gridx = 1; card.add(loginPassword, c);

        c.gridy++; c.gridx = 1;
        BlueButton btnLogin = new BlueButton("Login");
        btnLogin.addActionListener(e -> doLogin());
        card.add(btnLogin, c);

        c.gridy++; c.gridx = 0; c.gridwidth = 2;
        JLabel hint = new JLabel("<html><font color='#666666'>Use admin@gmail.com / admin123 (admin) or staff@gmail.com / staff123 (staff)</font></html>");
        card.add(hint, c);

        root.add(card);
        return root;
    }

    private void doLogin() {
        String email = loginEmail.getText().trim();
        String pass = new String(loginPassword.getPassword()).trim();
        if (email.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter email and password.", "Input required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql = "SELECT user_id, full_name, role FROM users WHERE email = ? AND password = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, pass);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    currentUserId = rs.getInt("user_id");
                    currentUserName = rs.getString("full_name");
                    currentUserRole = rs.getString("role");
                    JOptionPane.showMessageDialog(this, "Welcome " + currentUserName + " (" + currentUserRole + ")");
                    cardLayout.show(cards, "DASH");
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid credentials.", "Login failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Login error: " + ex.getMessage(), "DB error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---------------- Dashboard ----------------
    private JPanel createDashboardPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(LIGHT_BG);
        JLabel h = new JLabel("Dashboard");
        h.setFont(SUB_FONT);
        h.setBorder(new EmptyBorder(12, 12, 12, 12));
        p.add(h, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(1, 3, 16, 16));
        center.setBorder(new EmptyBorder(16, 16, 16, 16));
        center.setBackground(LIGHT_BG);

        center.add(statCard("Total Items", "0"));
        center.add(statCard("Pending Claims", "0"));
        center.add(statCard("Returned Items", "0"));

        p.add(center, BorderLayout.CENTER);
        // refresh numbers
        refreshDashboardCounts(center);
        return p;
    }

    private JPanel statCard(String title, String value) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(new EmptyBorder(12, 12, 12, 12));
        JLabel t = new JLabel(title); t.setFont(NORMAL_FONT);
        JLabel v = new JLabel(value, SwingConstants.CENTER); v.setFont(new Font("Segoe UI", Font.BOLD, 28)); v.setForeground(ROYAL_BLUE);
        card.add(t, BorderLayout.NORTH);
        card.add(v, BorderLayout.CENTER);
        return card;
    }

    private void refreshDashboardCounts(JPanel centerPanel) {
        try (Statement st = conn.createStatement()) {
            ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM items");
            String total = rs.next() ? rs.getString(1) : "0";
            rs = st.executeQuery("SELECT COUNT(*) FROM claims WHERE claim_status='Pending'");
            String pending = rs.next() ? rs.getString(1) : "0";
            rs = st.executeQuery("SELECT COUNT(*) FROM items WHERE status='Returned'");
            String returned = rs.next() ? rs.getString(1) : "0";

            centerPanel.removeAll();
            centerPanel.add(statCard("Total Items", total));
            centerPanel.add(statCard("Pending Claims", pending));
            centerPanel.add(statCard("Returned Items", returned));
            centerPanel.revalidate();
            centerPanel.repaint();
        } catch (SQLException ignored) {}
    }

    // ---------------- Add Item ----------------
    private JPanel createAddItemPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(LIGHT_BG);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10, 10, 10, 10); c.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Add Found Item");
        title.setFont(SUB_FONT);
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2; p.add(title, c);
        c.gridwidth = 1;

        c.gridy++; c.gridx = 0; p.add(new JLabel("Item Name *"), c);
        aiName = new JTextField(); aiName.setFont(NORMAL_FONT); c.gridx = 1; p.add(aiName, c);

        c.gridy++; c.gridx = 0; p.add(new JLabel("Category"), c);
        aiCategory = new JTextField(); aiCategory.setFont(NORMAL_FONT); c.gridx = 1; p.add(aiCategory, c);

        c.gridy++; c.gridx = 0; p.add(new JLabel("Found Location"), c);
        aiLocation = new JTextField(); aiLocation.setFont(NORMAL_FONT); c.gridx = 1; p.add(aiLocation, c);

        c.gridy++; c.gridx = 0; p.add(new JLabel("Found Date (YYYY-MM-DD)"), c);
        aiDate = new JTextField(LocalDate.now().toString()); aiDate.setFont(NORMAL_FONT); c.gridx = 1; p.add(aiDate, c);

        c.gridy++; c.gridx = 0; p.add(new JLabel("Description"), c);
        aiDescription = new JTextArea(5, 30); aiDescription.setFont(NORMAL_FONT); c.gridx = 1; p.add(new JScrollPane(aiDescription), c);

        c.gridy++; c.gridx = 1;
        BlueButton save = new BlueButton("Save Item");
        save.addActionListener(e -> addItem());
        p.add(save, c);

        return p;
    }

    private void resetAddForm() {
        aiName.setText(""); aiCategory.setText(""); aiLocation.setText(""); aiDate.setText(LocalDate.now().toString()); aiDescription.setText("");
    }

    private void addItem() {
        String name = aiName.getText().trim();
        if (name.isEmpty()) { JOptionPane.showMessageDialog(this, "Item name is required."); return; }
        String cat = aiCategory.getText().trim();
        String loc = aiLocation.getText().trim();
        String date = aiDate.getText().trim();
        String desc = aiDescription.getText().trim();

        try (CallableStatement cs = conn.prepareCall("{CALL add_item(?,?,?,?,?,?)}")) {
            cs.setString(1, name);
            cs.setString(2, desc.isEmpty() ? null : desc);
            cs.setString(3, cat.isEmpty() ? null : cat);
            if (loc.isEmpty()) cs.setNull(4, Types.VARCHAR); else cs.setString(4, loc);
            if (date.isEmpty()) cs.setNull(5, Types.DATE); else cs.setDate(5, Date.valueOf(date));
            if (currentUserId <= 0) cs.setNull(6, Types.INTEGER); else cs.setInt(6, currentUserId);

            cs.execute();
            JOptionPane.showMessageDialog(this, "Item added successfully.");
            resetAddForm();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Add item failed: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---------------- View Items ----------------
    private JPanel createViewItemsPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(LIGHT_BG);

        JLabel title = new JLabel("All Items"); title.setFont(SUB_FONT); title.setBorder(new EmptyBorder(8,8,8,8));
        p.add(title, BorderLayout.NORTH);

        itemsModel = new DefaultTableModel(new String[]{"ID","Name","Category","Description","Found Date","Location","Status"}, 0);
        itemsTable = new JTable(itemsModel);
        itemsTable.setRowHeight(26);

        p.add(new JScrollPane(itemsTable), BorderLayout.CENTER);

        JPanel ctrl = new JPanel();

        BlueButton refresh = new BlueButton("Refresh");
        refresh.addActionListener(e -> loadItems());

        BlueButton search = new BlueButton("Search");
        search.addActionListener(e -> searchItemsDialog());

        BlueButton delete = new BlueButton("Delete");
        delete.addActionListener(e -> deleteSelectedItem());

        ctrl.add(refresh);
        ctrl.add(search);
        ctrl.add(delete);

p.add(ctrl, BorderLayout.SOUTH);


        loadItems();
        return p;
    }

    private void deleteSelectedItem() {
    int row = itemsTable.getSelectedRow();
    if (row < 0) {
        JOptionPane.showMessageDialog(this, "Please select an item to delete.");
        return;
    }

    int itemId = Integer.parseInt(itemsModel.getValueAt(row, 0).toString());

    int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete this item?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION
    );

    if (confirm != JOptionPane.YES_OPTION) return;

    try (PreparedStatement ps = conn.prepareStatement(
            "DELETE FROM items WHERE item_id = ?"
    )) {
        ps.setInt(1, itemId);
        ps.executeUpdate();

        JOptionPane.showMessageDialog(this, "Item deleted successfully.");
        loadItems();      // refresh table
        loadLogs();       // refresh logs if needed
    } catch (SQLException ex) {
        JOptionPane.showMessageDialog(
                this,
                "Delete failed: " + ex.getMessage(),
                "DB Error",
                JOptionPane.ERROR_MESSAGE
        );
    }
}

    
    private void loadItems() {
        itemsModel.setRowCount(0);
        String q = "SELECT * FROM items ORDER BY created_at DESC";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(q)) {
            while (rs.next()) {
                itemsModel.addRow(new Object[]{
                        rs.getInt("item_id"),
                        rs.getString("item_name"),
                        rs.getString("category"),
                        rs.getString("description"),
                        rs.getDate("date_found"),
                        rs.getString("location_found"),
                        rs.getString("status")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Load items failed: " + ex.getMessage(), "DB error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void searchItemsDialog() {
        String term = JOptionPane.showInputDialog(this, "Enter keyword to search (name or description):");
        if (term == null || term.trim().isEmpty()) return;
        itemsModel.setRowCount(0);
        String q = "SELECT * FROM items WHERE item_name LIKE ? OR description LIKE ? ORDER BY created_at DESC";
        try (PreparedStatement ps = conn.prepareStatement(q)) {
            String like = "%" + term + "%";
            ps.setString(1, like); ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    itemsModel.addRow(new Object[]{
                            rs.getInt("item_id"),
                            rs.getString("item_name"),
                            rs.getString("category"),
                            rs.getString("description"),
                            rs.getDate("date_found"),
                            rs.getString("location_found"),
                            rs.getString("status")
                    });
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Search failed: " + ex.getMessage(), "DB error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---------------- Claims ----------------
    private JPanel createClaimsPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(LIGHT_BG);

        JLabel title = new JLabel("Claims"); title.setFont(SUB_FONT); title.setBorder(new EmptyBorder(8,8,8,8));
        p.add(title, BorderLayout.NORTH);

        claimsModel = new DefaultTableModel(new String[]{"ClaimID","ItemID","Claimer","Contact","Reason","Status","Date"}, 0);
        claimsTable = new JTable(claimsModel);
        claimsTable.setRowHeight(26);

        p.add(new JScrollPane(claimsTable), BorderLayout.CENTER);

        JPanel ctrl = new JPanel();
        BlueButton refresh = new BlueButton("Refresh"); refresh.addActionListener(e -> loadClaims());
        BlueButton raise = new BlueButton("Raise Claim"); raise.addActionListener(e -> raiseClaimDialog());
        BlueButton approve = new BlueButton("Approve"); approve.addActionListener(e -> approveSelectedClaim());
        BlueButton reject = new BlueButton("Reject"); reject.addActionListener(e -> rejectSelectedClaim());
        ctrl.add(refresh); ctrl.add(raise); ctrl.add(approve); ctrl.add(reject);
        p.add(ctrl, BorderLayout.SOUTH);

        loadClaims();
        return p;
    }

    private void loadClaims() {
        claimsModel.setRowCount(0);
        String q = "SELECT * FROM claims ORDER BY claim_date DESC";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(q)) {
            while (rs.next()) {
                claimsModel.addRow(new Object[]{
                        rs.getInt("claim_id"),
                        rs.getInt("item_id"),
                        rs.getString("claimer_name"),
                        rs.getString("claimer_contact"),
                        rs.getString("claim_reason"),
                        rs.getString("claim_status"),
                        rs.getTimestamp("claim_date")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Load claims failed: " + ex.getMessage(), "DB error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void raiseClaimDialog() {
        JPanel panel = new JPanel(new GridLayout(0,1,6,6));
        JTextField itemIdField = new JTextField();
        JTextField nameField = new JTextField();
        JTextField contactField = new JTextField();
        JTextArea reasonArea = new JTextArea(4, 20);

        panel.add(new JLabel("Item ID:")); panel.add(itemIdField);
        panel.add(new JLabel("Your Name:")); panel.add(nameField);
        panel.add(new JLabel("Contact:")); panel.add(contactField);
        panel.add(new JLabel("Reason/Details:")); panel.add(new JScrollPane(reasonArea));

        int res = JOptionPane.showConfirmDialog(this, panel, "Raise Claim", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;

        String sItem = itemIdField.getText().trim();
        String claimer = nameField.getText().trim();
        String contact = contactField.getText().trim();
        String reason = reasonArea.getText().trim();

        if (sItem.isEmpty() || claimer.isEmpty() || contact.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Item ID, name and contact are required.", "Input required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int itemId = Integer.parseInt(sItem);
            String sql = "INSERT INTO claims(item_id, claimer_name, claimer_contact, claim_reason) VALUES (?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, itemId);
                ps.setString(2, claimer);
                ps.setString(3, contact);
                ps.setString(4, reason);
                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "Claim submitted (Pending).");
                loadClaims();
                loadItems();
            }
        } catch (NumberFormatException nf) {
            JOptionPane.showMessageDialog(this, "Item ID must be a number.", "Input error", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Claim failed: " + ex.getMessage(), "DB error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void approveSelectedClaim() {
        int row = claimsTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a claim to approve."); return; }
        int claimId = Integer.parseInt(claimsModel.getValueAt(row, 0).toString());
        try (CallableStatement cs = conn.prepareCall("{CALL approve_claim(?)}")) {
            cs.setInt(1, claimId);
            cs.execute();
            JOptionPane.showMessageDialog(this, "Claim approved. Item marked Returned.");
            loadClaims();
            loadItems();
            loadLogs();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Approve failed: " + ex.getMessage(), "DB error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void rejectSelectedClaim() {
        int row = claimsTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a claim to reject."); return; }
        int claimId = Integer.parseInt(claimsModel.getValueAt(row, 0).toString());
        try (PreparedStatement ps = conn.prepareStatement("UPDATE claims SET claim_status='Rejected' WHERE claim_id=?")) {
            ps.setInt(1, claimId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Claim rejected.");
            loadClaims();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Reject failed: " + ex.getMessage(), "DB error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---------------- Logs ----------------
    private JPanel createLogsPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(LIGHT_BG);

        JLabel title = new JLabel("Item Status Change Logs");
        title.setFont(SUB_FONT);
        title.setBorder(new EmptyBorder(8,8,8,8));
        p.add(title, BorderLayout.NORTH);

        logsModel = new DefaultTableModel(new String[]{"LogID","ItemID","Old Status","New Status","Changed At"}, 0);
        logsTable = new JTable(logsModel);
        logsTable.setRowHeight(26);

        p.add(new JScrollPane(logsTable), BorderLayout.CENTER);

        JPanel ctrl = new JPanel();
        BlueButton refresh = new BlueButton("Refresh");
        refresh.addActionListener(e -> loadLogs());
        ctrl.add(refresh);
        p.add(ctrl, BorderLayout.SOUTH);

        loadLogs();
        return p;
    }

    private void loadLogs() {
        logsModel.setRowCount(0);
        String q = "SELECT * FROM item_logs ORDER BY changed_at DESC";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(q)) {
            while (rs.next()) {
                logsModel.addRow(new Object[]{
                        rs.getInt("log_id"),
                        rs.getInt("item_id"),
                        rs.getString("old_status"),
                        rs.getString("new_status"),
                        rs.getTimestamp("changed_at")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Load logs failed: " + ex.getMessage(), "DB error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---------------- Utilities ----------------
    private void refreshDashboard() {
        // left for simplicity; dashboard refresh occurs when navigating
    }

    // ---------------- Main ----------------
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new LostFoundDBApp());
    }

    private void doLogout() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    // ---------------- BlueButton class (all buttons use this) ----------------
    static class BlueButton extends JButton {
        private static final Color TOP = new Color(30, 110, 255);
        private static final Color BOTTOM = new Color(8, 63, 190);

        public BlueButton(String text) {
            super(text);
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(140, 38));
            setMargin(new Insets(6, 12, 6, 12));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            int w = getWidth();
            int h = getHeight();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Gradient
            GradientPaint gp = new GradientPaint(0, 0, TOP, 0, h, BOTTOM);
            g2.setPaint(gp);
            g2.fillRoundRect(0, 0, w, h, 12, 12);

            // Inner brighter stroke
            g2.setColor(new Color(255, 255, 255, 30));
            g2.drawRoundRect(1, 1, w - 3, h - 3, 12, 12);

            // Drop shadow (subtle)
            g2.setColor(new Color(0, 0, 0, 35));
            g2.fillRoundRect(0, h - 4, w, 6, 12, 12);

            g2.dispose();

            super.paintComponent(g);
        }

        @Override
        public void setContentAreaFilled(boolean b) {
            // prevent JButton default fill
        }

        @Override
        public boolean contains(int x, int y) {
            // make click area rounded
            Shape r = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12);
            return r.contains(x, y);
        }
    }
}