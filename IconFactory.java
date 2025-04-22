package guiClient;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * Lớp tiện ích để tạo các biểu tượng cho ứng dụng
 * thay vì sử dụng tệp hình ảnh
 */
public class IconFactory {

    /**
     * Tạo biểu tượng tìm kiếm
     * @param width Chiều rộng
     * @param height Chiều cao
     * @param color Màu sắc
     * @return ImageIcon
     */
    public static ImageIcon createSearchIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);

        // Vẽ vòng tròn kính lúp
        int circleSize = Math.min(width, height) - 8;
        g2.setStroke(new BasicStroke(2f));
        g2.drawOval(2, 2, circleSize - 4, circleSize - 4);

        // Vẽ tay cầm kính lúp
        g2.setStroke(new BasicStroke(3f));
        g2.drawLine(circleSize - 1, circleSize - 1, width - 4, height - 4);

        g2.dispose();
        return new ImageIcon(image);
    }

    /**
     * Tạo biểu tượng vé
     * @param width Chiều rộng
     * @param height Chiều cao
     * @param color Màu sắc
     * @return ImageIcon
     */
    public static ImageIcon createTicketIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);

        // Vẽ biểu tượng vé
        GeneralPath path = new GeneralPath();
        path.moveTo(3, 5);
        path.lineTo(width - 3, 5);
        path.lineTo(width - 3, height - 5);
        path.lineTo(3, height - 5);
        path.closePath();

        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(path);

        // Vẽ đường gấp khúc
        float dashLength = 2f;
        float[] dash = {dashLength, dashLength};
        g2.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
        g2.drawLine(width/2, 5, width/2, height - 5);

        // Vẽ các chi tiết trên vé
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawLine(6, height/3, width/2 - 3, height/3);
        g2.drawLine(6, height*2/3, width/2 - 3, height*2/3);
        g2.drawLine(width/2 + 3, height/2, width - 6, height/2);

        g2.dispose();
        return new ImageIcon(image);
    }

    /**
     * Tạo biểu tượng đổi/trao đổi
     * @param width Chiều rộng
     * @param height Chiều cao
     * @param color Màu sắc
     * @return ImageIcon
     */
    public static ImageIcon createExchangeIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(2f));

        // Vẽ mũi tên trái sang phải
        g2.drawLine(5, height/3, width - 8, height/3);
        int[] xPoints1 = {width - 8, width - 8 - 5, width - 8 - 5};
        int[] yPoints1 = {height/3, height/3 - 4, height/3 + 4};
        g2.fillPolygon(xPoints1, yPoints1, 3);

        // Vẽ mũi tên phải sang trái
        g2.drawLine(width - 5, height*2/3, 8, height*2/3);
        int[] xPoints2 = {8, 8 + 5, 8 + 5};
        int[] yPoints2 = {height*2/3, height*2/3 - 4, height*2/3 + 4};
        g2.fillPolygon(xPoints2, yPoints2, 3);

        g2.dispose();
        return new ImageIcon(image);
    }

    /**
     * Tạo biểu tượng làm mới
     * @param width Chiều rộng
     * @param height Chiều cao
     * @param color Màu sắc
     * @return ImageIcon
     */
    public static ImageIcon createRefreshIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(2f));

        // Vẽ vòng tròn làm mới
        int arcSize = Math.min(width, height) - 6;
        g2.drawArc(3, 3, arcSize, arcSize, 30, 300);

        // Vẽ mũi tên làm mới
        int[] xPoints = {arcSize - 2, arcSize + 3, arcSize + 6};
        int[] yPoints = {3, 3, 10};
        g2.fillPolygon(xPoints, yPoints, 3);

        g2.dispose();
        return new ImageIcon(image);
    }

    /**
     * Tạo biểu tượng thoát
     * @param width Chiều rộng
     * @param height Chiều cao
     * @param color Màu sắc
     * @return ImageIcon
     */
    public static ImageIcon createExitIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(2f));

        // Vẽ hình chữ nhật
        g2.drawRect(2, 2, width - 10, height - 4);

        // Vẽ mũi tên ra khỏi
        g2.drawLine(width - 8, height/2, width - 3, height/2);
        int[] xPoints = {width - 7, width - 3, width - 7};
        int[] yPoints = {height/2 - 4, height/2, height/2 + 4};
        g2.fillPolygon(xPoints, yPoints, 3);

        g2.dispose();
        return new ImageIcon(image);
    }

    public static Icon getIcon(String iconName, int width, int height, Color color) {
        switch (iconName.toLowerCase()) {
            case "search":
                return createSearchIcon(width, height, color);
            // Thêm các case khác nếu cần
            default:
                return null;
        }
    }
}
