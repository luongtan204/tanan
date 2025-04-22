package guiClient;

import javax.swing.*;
import java.awt.*;

/**
 * Test class for KhuyenMaiPanel
 * This class creates a JFrame and adds the KhuyenMaiPanel to it
 */
public class KhuyenMaiPanelTest {
    public static void main(String[] args) {
        // Set look and feel to system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Create and show the GUI on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            // Create a JFrame
            JFrame frame = new JFrame("Quản Lý Khuyến Mãi");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1200, 700);
            frame.setLocationRelativeTo(null);
            
            // Create the KhuyenMaiPanel
            KhuyenMaiPanel khuyenMaiPanel = new KhuyenMaiPanel();
            
            // Add the panel to the frame
            frame.getContentPane().add(khuyenMaiPanel, BorderLayout.CENTER);
            
            // Show the frame
            frame.setVisible(true);
        });
    }
}