package guiClient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoginGUI {


    public static void main(String[] args) {
        // Chạy ứng dụng trên Event Dispatch Thread (luồng quản lý giao diện)
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Loading loading = new Loading();
                loading.setVisible(true);

            }
        });
    }
}
