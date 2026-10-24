import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class BillDAO {

    public static List<MenuItem> loadMenu() throws SQLException {
        List<MenuItem> menu = new ArrayList<>();

        String sql = "SELECT name, price FROM menu_items";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                menu.add(new MenuItem(
                    rs.getString("name"),
                    rs.getDouble("price")
                ));
            }
        }

        return menu;
    }

    public static int saveBill(List<OrderItem> order, double total, String cashierName) throws SQLException {
        String insertBill =
            "INSERT INTO bills (cashier_name, total_amount) VALUES (?, ?)";

        String insertItem =
            "INSERT INTO bill_items " +
            "(bill_id, item_name, quantity, price, subtotal) " +
            "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                int billId;

                try (PreparedStatement billStmt = conn.prepareStatement(
                        insertBill,
                        Statement.RETURN_GENERATED_KEYS)) {

                    billStmt.setString(1, cashierName);
                    billStmt.setDouble(2, total);
                    billStmt.executeUpdate();

                    try (ResultSet keys = billStmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            billId = keys.getInt(1);
                        } else {
                            throw new SQLException("Creating bill failed; no ID returned.");
                        }
                    }
                }

                try (PreparedStatement itemStmt = conn.prepareStatement(insertItem)) {
                    for (OrderItem item : order) {
                        itemStmt.setInt(1, billId);
                        itemStmt.setString(2, item.getMenuItem().getName());
                        itemStmt.setInt(3, item.getQuantity());
                        itemStmt.setDouble(4, item.getMenuItem().getPrice());
                        itemStmt.setDouble(5, item.getSubtotal());
                        itemStmt.addBatch();
                    }

                    itemStmt.executeBatch();
                }

                conn.commit();
                return billId;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public static List<String> loadAllBillsFormatted() throws SQLException {
        List<String> bills = new ArrayList<>();

        String billSql =
            "SELECT bill_id, cashier_name, bill_date, total_amount " +
            "FROM bills ORDER BY bill_id DESC";

        String itemSql =
            "SELECT item_name, quantity, subtotal " +
            "FROM bill_items WHERE bill_id = ?";

        try (Connection conn = DBConnection.getConnection();
             Statement billStmt = conn.createStatement();
             ResultSet billRs = billStmt.executeQuery(billSql)) {

            while (billRs.next()) {
                int billId = billRs.getInt("bill_id");

                StringBuilder sb = new StringBuilder();

                sb.append("Bill #").append(billId)
                  .append("    |   ")
                  .append(billRs.getTimestamp("bill_date"))
                  .append("    |   Served by: ")
                  .append(billRs.getString("cashier_name"))
                  .append("\n");

                try (PreparedStatement itemStmt = conn.prepareStatement(itemSql)) {
                    itemStmt.setInt(1, billId);

                    try (ResultSet itemRs = itemStmt.executeQuery()) {
                        while (itemRs.next()) {
                            sb.append(String.format(
                                "   %-18s x%-4d Rs.%.2f%n",
                                itemRs.getString("item_name"),
                                itemRs.getInt("quantity"),
                                itemRs.getDouble("subtotal")
                            ));
                        }
                    }
                }

                sb.append(String.format(
                    "   TOTAL: Rs.%.2f",
                    billRs.getDouble("total_amount")
                ));

                bills.add(sb.toString());
            }
        }

        return bills;
    }
}
