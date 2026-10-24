import javax.swing.*;
import java.awt.*;
import java.awt.print.PrinterException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RestaurantBillingApp extends JFrame {

    private MenuItem[] menu;
    private final String cashierName;

    private final List<OrderItem> currentOrder = new ArrayList<>();

    private JComboBox<MenuItem> menuBox;
    private JTextField qtyField;
    private JTextArea orderArea;
    private JLabel totalLabel;

    public RestaurantBillingApp() {
        setTitle("ABC Restaurant - Billing System");
        setSize(520, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        cashierName = promptLogin();
        setTitle("ABC Restaurant - Billing System (Cashier: " + cashierName + ")");

        loadMenuFromDatabase();

        add(buildTopPanel(), BorderLayout.NORTH);
        add(buildCenterPanel(), BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);
    }

    private String promptLogin() {
        String name = JOptionPane.showInputDialog(
                this,
                "Enter cashier name to begin:",
                "Login",
                JOptionPane.PLAIN_MESSAGE
        );

        if (name == null || name.trim().isEmpty()) {
            name = "Guest";
        }

        return name.trim();
    }

    private void loadMenuFromDatabase() {
        try {
            List<MenuItem> items = BillDAO.loadMenu();

            if (items.isEmpty()) {
                throw new SQLException("menu_items table returned no rows.");
            }

            menu = items.toArray(MenuItem[]::new);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Could not load the menu from the database:\n"
                    + e.getMessage()
                    + "\n\nCheck that the embedded database is initialized correctly.",
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );

            System.exit(1);
        }
    }

    private JPanel buildTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));

        menuBox = new JComboBox<>(menu);
        qtyField = new JTextField(5);

        JButton addButton = new JButton("Add to Order");
        addButton.addActionListener(e -> addItem());

        panel.add(new JLabel("Item:"));
        panel.add(menuBox);
        panel.add(new JLabel("Qty:"));
        panel.add(qtyField);
        panel.add(addButton);

        return panel;
    }

    private JPanel buildCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        orderArea = new JTextArea();
        orderArea.setEditable(false);
        orderArea.setFont(new Font("Monospaced", Font.PLAIN, 13));

        panel.add(new JScrollPane(orderArea), BorderLayout.CENTER);

        totalLabel = new JLabel("Total: Rs. 0.00");
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

        panel.add(totalLabel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout());

        JButton generateButton = new JButton("Generate Bill");
        JButton viewPreviousButton = new JButton("View Previous Bills");
        JButton newOrderButton = new JButton("New Order");

        generateButton.addActionListener(e -> generateBill());
        viewPreviousButton.addActionListener(e -> viewPreviousBills());

        newOrderButton.addActionListener(e -> {
            currentOrder.clear();
            refreshOrderDisplay();
        });

        panel.add(generateButton);
        panel.add(viewPreviousButton);
        panel.add(newOrderButton);

        return panel;
    }

    private void addItem() {
        MenuItem selected = (MenuItem) menuBox.getSelectedItem();
        String qtyText = qtyField.getText().trim();

        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select a menu item.");
            return;
        }

        try {
            int qty = Integer.parseInt(qtyText);

            if (qty <= 0) {
                JOptionPane.showMessageDialog(
                        this,
                        "Quantity must be greater than 0."
                );
                return;
            }

            currentOrder.add(new OrderItem(selected, qty));
            qtyField.setText("");
            refreshOrderDisplay();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please enter a valid whole number for quantity."
            );
        }
    }

    private void refreshOrderDisplay() {
        StringBuilder sb = new StringBuilder();
        double total = 0;

        for (OrderItem item : currentOrder) {
            sb.append(String.format(
                    "%-20s x%-5d Rs.%.2f%n",
                    item.getMenuItem().getName(),
                    item.getQuantity(),
                    item.getSubtotal()
            ));

            total += item.getSubtotal();
        }

        orderArea.setText(sb.toString());
        totalLabel.setText(String.format("Total: Rs. %.2f", total));
    }

    private void generateBill() {
        if (currentOrder.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Add at least one item before generating a bill."
            );
            return;
        }

        double total = 0;

        for (OrderItem item : currentOrder) {
            total += item.getSubtotal();
        }

        try {
            BillDAO.saveBill(currentOrder, total, cashierName);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Could not save the bill to the database:\n"
                    + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        showReceiptDialog(ReceiptGenerator.generate(currentOrder));

        currentOrder.clear();
        refreshOrderDisplay();
    }

    private void showReceiptDialog(String receipt) {
        JTextArea receiptArea = new JTextArea(receipt);
        receiptArea.setEditable(false);
        receiptArea.setFont(new Font("Monospaced", Font.PLAIN, 13));

        JButton printButton = new JButton("Print");

        printButton.addActionListener(e -> {
            try {
                receiptArea.print();
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "Printing failed: " + ex.getMessage()
                );
            }
        });

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(new JScrollPane(receiptArea), BorderLayout.CENTER);
        panel.add(printButton, BorderLayout.SOUTH);

        JOptionPane.showMessageDialog(
                this,
                panel,
                "Receipt",
                JOptionPane.PLAIN_MESSAGE
        );
    }

    private void viewPreviousBills() {
        List<String> bills;

        try {
            bills = BillDAO.loadAllBillsFormatted();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Could not load previous bills:\n"
                    + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        if (bills.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No previous bills yet.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (String bill : bills) {
            sb.append(bill).append("\n\n");
        }

        JTextArea area = new JTextArea(sb.toString());
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setPreferredSize(new Dimension(480, 420));

        JOptionPane.showMessageDialog(
                this,
                scrollPane,
                "Previous Bills",
                JOptionPane.PLAIN_MESSAGE
        );
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            RestaurantBillingApp app = new RestaurantBillingApp();
            app.setVisible(true);
        });
    }
}
