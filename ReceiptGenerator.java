import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ReceiptGenerator {

    private static final String LINE =
            "========================================";
    private static final String DIVIDER =
            "----------------------------------------";
    private static final int WIDTH = 40;

    public static String generate(List<OrderItem> order) {
        StringBuilder sb = new StringBuilder();

        sb.append(LINE).append("\n");
        sb.append(center("ABC RESTAURANT")).append("\n");
        sb.append(LINE).append("\n");

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        sb.append("Date: ")
          .append(sdf.format(new Date()))
          .append("\n\n");

        sb.append(String.format(
                "%-20s%-8s%s%n",
                "Item", "Qty", "Price"
        ));
        sb.append(DIVIDER).append("\n");

        double total = 0;

        for (OrderItem item : order) {
            sb.append(String.format(
                    "%-20s%-8d%.2f%n",
                    item.getMenuItem().getName(),
                    item.getQuantity(),
                    item.getSubtotal()
            ));

            total += item.getSubtotal();
        }

        sb.append(DIVIDER).append("\n");
        sb.append(String.format(
                "%-28s%.2f%n", "TOTAL:", total
        ));
        sb.append(LINE).append("\n");
        sb.append(center("Thank You! Visit Again")).append("\n");
        sb.append(LINE);

        return sb.toString();
    }

    private static String center(String text) {
        int pad = Math.max(0, (WIDTH - text.length()) / 2);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pad; i++) {
            sb.append(" ");
        }
        sb.append(text);
        return sb.toString();
    }
}
