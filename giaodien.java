package guiClient;

import dao.*;
import dao.impl.*;
import model.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.*;
import java.rmi.RemoteException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import javax.swing.Timer;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.util.stream.Collectors;

/**
 * Train Ticket Booking System GUI
 * Allows users to search for trains, view cars, select seats, and book tickets
 * @author luongtan204
 */
public class TrainTicketBookingSystem extends JFrame {
    // UI Components
    private JPanel seatsPanel;
    private JPanel carsPanel;
    private JPanel trainsPanel;
    private JScrollPane trainsScrollPane;
    private JScrollPane carsScrollPane;
    private JLabel seatingSectionLabel;
    private JLabel totalLabel;

    // Form fields
    private JTextField departureField;
    private JTextField arrivalField;
    private JTextField departureDateField;
    private JTextField returnDateField;
    private JRadioButton oneWayRadio;
    private JRadioButton roundTripRadio;

    // State variables
    private String selectedTrainId = "";   // Compound ID for selection (train code + time)
    private String currentTrainId = "";    // Just the train code
    private String currentToaId = "";      // Car ID
    private String currentMaLich = "";     // Schedule ID for seat loading
    private ArrayList<TicketItem> cartItems = new ArrayList<>();
    private double totalAmount = 0.0;

    // Seat status constants
    private static final String STATUS_AVAILABLE = "Trống";
    private static final String STATUS_BOOKED = "Đã đặt";
    private static final String STATUS_PENDING = "Chờ xác nhận";

    // Seat colors
    private static final Color SEAT_AVAILABLE_COLOR = Color.WHITE;
    private static final Color SEAT_BOOKED_COLOR = Color.LIGHT_GRAY;
    private static final Color SEAT_PENDING_COLOR = Color.YELLOW;

    private Map<String, Timer> reservationTimers = new HashMap<>(); // Store timers for each reserved seat
    private Map<String, Integer> remainingTimes = new HashMap<>();  // Store remaining seconds for each reservation
    private static final int RESERVATION_TIMEOUT = 300; // 300 seconds (5 minutes)

    // Visual effects
    private Color activeColor = new Color(0, 136, 204);
    private Color inactiveColor = new Color(153, 153, 153);
    private Color hoverColor = new Color(51, 153, 255);
    private Border activeBorder = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 215, 0), 2), // Gold border
            BorderFactory.createEmptyBorder(1, 1, 1, 1)
    );
    private Border normalBorder = BorderFactory.createEmptyBorder(3, 3, 3, 3);

    // DAOs
    private LichTrinhTauDAO lichTrinhTauDAO;
    private TauDAO tauDAO;
    private ToaTauDAO toaTauDAO;
    private ChoNgoiDAO choNgoiDAO;

    /**
     * Constructor - initializes the application
     */
    public TrainTicketBookingSystem() throws RemoteException {
        setTitle("Hệ thống đặt vé tàu");
        setSize(1200, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Initialize DAOs
        lichTrinhTauDAO = new LichTrinhTauDAOImpl();
        tauDAO = new TauDAOImpl();
        toaTauDAO = new ToaTauDAOImpl();
        choNgoiDAO = new ChoNgoiDAOImpl();

        // Main container panel with BorderLayout
        JPanel containerPanel = new JPanel(new BorderLayout(10, 10));

        // Main panel (center content)
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Add components to main panel
        mainPanel.add(createHeaderPanel(), BorderLayout.NORTH);
        mainPanel.add(createTrainsPanel(), BorderLayout.CENTER);

        // Left panel - Trip Information
        JPanel leftPanel = createTripInfoPanel();

        // Right panel - Ticket Cart
        JPanel rightPanel = createTicketCartPanel();

        // Add all panels to the container
        containerPanel.add(leftPanel, BorderLayout.WEST);
        containerPanel.add(mainPanel, BorderLayout.CENTER);
        containerPanel.add(rightPanel, BorderLayout.EAST);

        // Add container panel to frame
        add(containerPanel);
    }

    /**
     * Creates the header panel with route information
     */
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BorderLayout());

        JLabel headerLabel = new JLabel("Chiều đi: ngày 20/04/2025 từ Sài Gòn đến Hà Nội");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 18));
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setBorder(new EmptyBorder(10, 20, 10, 10));

        JPanel bluePanel = new JPanel();
        bluePanel.setLayout(new BorderLayout());
        bluePanel.setBackground(activeColor);
        bluePanel.add(headerLabel, BorderLayout.CENTER);

        // Add "arrow" shape to the right
        JPanel arrowPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(activeColor);
                int[] xPoints = {0, getWidth() - 20, getWidth(), getWidth() - 20, 0};
                int[] yPoints = {0, 0, getHeight()/2, getHeight(), getHeight()};
                g2d.fillPolygon(xPoints, yPoints, 5);
            }
        };
        arrowPanel.setOpaque(false);
        arrowPanel.setPreferredSize(new Dimension(30, 50));

        bluePanel.add(arrowPanel, BorderLayout.EAST);
        headerPanel.add(bluePanel, BorderLayout.NORTH);

        return headerPanel;
    }

    /**
     * Creates the panel that displays trains and their details
     */
    private JPanel createTrainsPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        // Train options cards panel (initially empty)
        trainsPanel = new JPanel();
        trainsPanel.setLayout(new BoxLayout(trainsPanel, BoxLayout.X_AXIS));

        // Add an empty placeholder panel with instructions
        JPanel placeholderPanel = new JPanel(new BorderLayout());
        placeholderPanel.setPreferredSize(new Dimension(400, 150));
        JLabel placeholderLabel = new JLabel("Vui lòng tìm kiếm chuyến tàu", SwingConstants.CENTER);
        placeholderLabel.setFont(new Font("Arial", Font.ITALIC, 16));
        placeholderLabel.setForeground(Color.GRAY);
        placeholderPanel.add(placeholderLabel, BorderLayout.CENTER);
        trainsPanel.add(placeholderPanel);

        // Add ScrollPane for trains panel
        trainsScrollPane = new JScrollPane(trainsPanel);
        trainsScrollPane.setBorder(BorderFactory.createEmptyBorder());
        trainsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        trainsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        // Set explicit height for train panel
        trainsScrollPane.setPreferredSize(new Dimension(mainPanel.getWidth(), 180));

        mainPanel.add(trainsScrollPane, BorderLayout.NORTH);

        // Train cars diagram - initially empty
        carsPanel = new JPanel();
        carsPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

        // Add ScrollPane for cars panel
        carsScrollPane = new JScrollPane(carsPanel);
        carsScrollPane.setBorder(BorderFactory.createEmptyBorder());
        carsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        carsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        carsScrollPane.setPreferredSize(new Dimension(mainPanel.getWidth(), 80));

        // Wrap the ScrollPane in a panel with padding
        JPanel carsContainer = new JPanel(new BorderLayout());
        carsContainer.setBorder(new EmptyBorder(20, 0, 20, 0));
        carsContainer.add(carsScrollPane, BorderLayout.CENTER);

        mainPanel.add(carsContainer, BorderLayout.CENTER);

        // Seating chart panel - simplified for now
        JPanel seatingSectionPanel = new JPanel(new BorderLayout());
        seatingSectionLabel = new JLabel("Toa: Chưa chọn", SwingConstants.CENTER);
        seatingSectionLabel.setFont(new Font("Arial", Font.BOLD, 18));
        seatingSectionLabel.setForeground(activeColor);
        seatingSectionLabel.setBorder(new EmptyBorder(10, 0, 10, 0));
        seatingSectionPanel.add(seatingSectionLabel, BorderLayout.NORTH);

        // Navigation and seats panel - simplified
        JPanel navigationPanel = new JPanel(new BorderLayout());

        // Left navigation button
        JButton leftButton = new JButton("<");
        leftButton.setPreferredSize(new Dimension(30, 200));
        leftButton.setFont(new Font("Arial", Font.BOLD, 24));
        navigationPanel.add(leftButton, BorderLayout.WEST);

        // Create placeholder seating chart
        seatsPanel = createPlaceholderSeatsPanel();
        seatsPanel.setBorder(new LineBorder(Color.GRAY, 1));
        navigationPanel.add(seatsPanel, BorderLayout.CENTER);

        // Right navigation button
        JButton rightButton = new JButton(">");
        rightButton.setPreferredSize(new Dimension(30, 200));
        rightButton.setFont(new Font("Arial", Font.BOLD, 24));
        navigationPanel.add(rightButton, BorderLayout.EAST);

        seatingSectionPanel.add(navigationPanel, BorderLayout.CENTER);
        mainPanel.add(seatingSectionPanel, BorderLayout.SOUTH);

        return mainPanel;
    }

    /**
     * Creates a placeholder panel for the seating chart
     */
    private JPanel createPlaceholderSeatsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel placeholder = new JLabel("Vui lòng chọn toa để xem sơ đồ ghế", SwingConstants.CENTER);
        placeholder.setFont(new Font("Arial", Font.ITALIC, 14));
        placeholder.setForeground(Color.GRAY);
        panel.add(placeholder, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Creates a train card panel with the given details
     */
    private JPanel createTrainCard(String trainCode, String departTime, long availableSeats,
                                   boolean isSelected, TrangThai status, String maLich) {
        JPanel cardPanel = new JPanel();
        cardPanel.setLayout(new BorderLayout());
        cardPanel.setBorder(isSelected ? activeBorder : normalBorder);

        // Create a unique identifier using train code and departure time
        String uniqueId = trainCode + "_" + departTime.replace("/", "").replace(":", "").replace(" ", "");

        // Store important data as client properties
        cardPanel.putClientProperty("trainId", uniqueId);
        cardPanel.putClientProperty("trainCode", trainCode);
        cardPanel.putClientProperty("maLich", maLich);

        // Set background color based on selection
        Color bgColor = isSelected ? activeColor : inactiveColor;
        cardPanel.setBackground(bgColor);

        // Set minimum size and margin
        cardPanel.setPreferredSize(new Dimension(180, 150));
        cardPanel.setMinimumSize(new Dimension(180, 150));
        cardPanel.setMaximumSize(new Dimension(180, 150));

        // Header with train code
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);

        JLabel codeLabel = new JLabel(trainCode);
        codeLabel.setFont(new Font("Arial", Font.BOLD, 14));
        codeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        headerPanel.add(codeLabel, BorderLayout.CENTER);

        // Add status indicator if provided
        if (status != null) {
            JLabel statusLabel = new JLabel();
            statusLabel.setFont(new Font("Arial", Font.ITALIC, 10));

            if (status == TrangThai.CHUA_KHOI_HANH) {
                statusLabel.setText("Chưa khởi hành");
                statusLabel.setForeground(new Color(0, 128, 0));  // Green
            } else if (status == TrangThai.HOAT_DONG) {
                statusLabel.setText("Đang hoạt động");
                statusLabel.setForeground(new Color(0, 0, 255));  // Blue
            }

            statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
            headerPanel.add(statusLabel, BorderLayout.SOUTH);
        }

        cardPanel.add(headerPanel, BorderLayout.NORTH);

        // Center panel with times and seats info
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new GridLayout(4, 2));
        centerPanel.setBackground(bgColor);

        Color textColor = Color.WHITE;

        JLabel departLabel = new JLabel("TG đi");
        departLabel.setForeground(textColor);
        centerPanel.add(departLabel);

        JLabel departTimeLabel = new JLabel(departTime);
        departTimeLabel.setForeground(textColor);
        centerPanel.add(departTimeLabel);

        JLabel bookedLabel = new JLabel("SL chỗ đặt");
        bookedLabel.setForeground(textColor);
        centerPanel.add(bookedLabel);

        JLabel availableLabel = new JLabel("SL chỗ trống");
        availableLabel.setForeground(textColor);
        centerPanel.add(availableLabel);

        JLabel bookedCountLabel = new JLabel("0");
        bookedCountLabel.setForeground(textColor);
        centerPanel.add(bookedCountLabel);

        JLabel availableCountLabel = new JLabel(String.valueOf(availableSeats));
        availableCountLabel.setForeground(textColor);
        centerPanel.add(availableCountLabel);

        cardPanel.add(centerPanel, BorderLayout.CENTER);

        // Train icon at bottom
        JPanel iconPanel = createTrainIconPanel(bgColor);
        cardPanel.add(iconPanel, BorderLayout.SOUTH);

        // Add tooltip for hover information
        String statusText = (status != null) ?
                "<br>Trạng thái: " + status.getValue() : "";

        cardPanel.setToolTipText(
                "<html>" +
                        "<b>Tàu: " + trainCode + "</b><br>" +
                        "Thời gian đi: " + departTime + "<br>" +
                        "Số chỗ trống: " + availableSeats +
                        statusText +
                        "<br>Nhấp để xem chi tiết toa tàu." +
                        "</html>"
        );

        // Add selection listener with clear event handling
        cardPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    // Store the unique ID, train code and schedule ID
                    selectedTrainId = uniqueId;
                    currentMaLich = maLich;

                    // Update selection in UI and load the corresponding cars
                    updateTrainSelection(uniqueId, trainCode);
                } catch (RemoteException ex) {
                    JOptionPane.showMessageDialog(cardPanel,
                            "Lỗi khi tải thông tin tàu: " + ex.getMessage(),
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }

            // Add hover effect
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!uniqueId.equals(selectedTrainId)) {
                    cardPanel.setBackground(hoverColor);
                    centerPanel.setBackground(hoverColor);
                    iconPanel.setBackground(hoverColor);

                    cardPanel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(173, 216, 230), 2),
                            BorderFactory.createEmptyBorder(1, 1, 1, 1)
                    ));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!uniqueId.equals(selectedTrainId)) {
                    cardPanel.setBackground(inactiveColor);
                    centerPanel.setBackground(inactiveColor);
                    iconPanel.setBackground(inactiveColor);

                    cardPanel.setBorder(normalBorder);
                }
            }
        });

        return cardPanel;
    }

    /**
     * Creates a train icon with wheels
     */
    private JPanel createTrainIconPanel(Color bgColor) {
        JPanel iconPanel = new JPanel();
        iconPanel.setLayout(new BorderLayout());
        iconPanel.setBackground(bgColor);

        // Create train wheels
        JPanel wheelsPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(Color.WHITE);
                g.fillOval(30, 0, 20, 20);
                g.fillOval(90, 0, 20, 20);
            }
        };
        wheelsPanel.setOpaque(false);
        wheelsPanel.setPreferredSize(new Dimension(140, 20));

        // Create train base
        JPanel basePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(Color.BLACK);
                g.fillRect(20, 0, 100, 5);
            }
        };
        basePanel.setOpaque(false);
        basePanel.setPreferredSize(new Dimension(140, 5));

        iconPanel.add(wheelsPanel, BorderLayout.CENTER);
        iconPanel.add(basePanel, BorderLayout.SOUTH);

        return iconPanel;
    }

    /**
     * Load train cars for the selected train
     */
    private void loadTrainCars(String trainId) throws RemoteException {
        // Clear existing cars
        carsPanel.removeAll();

        if (trainId == null || trainId.isEmpty()) {
            // No train selected, show placeholder
            JLabel placeholder = new JLabel("Vui lòng chọn tàu để xem toa", SwingConstants.CENTER);
            placeholder.setFont(new Font("Arial", Font.ITALIC, 14));
            placeholder.setForeground(Color.GRAY);
            carsPanel.add(placeholder);
            carsPanel.revalidate();
            carsPanel.repaint();
            return;
        }

        // Get train cars from DAO
        List<ToaTau> toaTauList = toaTauDAO.getToaByTau(trainId);

        // Sort cars by order (thuTu) - still reversed for display order
        Collections.sort(toaTauList, Comparator.comparingInt(ToaTau::getThuTu).reversed());

        // Create car panels
        for (ToaTau toaTau : toaTauList) {
            // Extract car number from maToa rather than using thuTu field
            String toaNumber;
            String maToa = toaTau.getMaToa();

            if (maToa != null && maToa.contains("-")) {
                // Extract number from the first part (before the dash)
                String firstPart = maToa.split("-")[0];
                // Remove non-numeric characters to get just the number
                toaNumber = firstPart.replaceAll("\\D+", "");
            } else {
                // Fallback to thuTu if maToa format is different
                toaNumber = String.valueOf(toaTau.getThuTu());
            }

            JPanel carPanel = new JPanel();
            carPanel.setLayout(new BorderLayout());
            carPanel.setPreferredSize(new Dimension(50, 50));

            // IMPORTANT: Set the name property to store the toa ID for selection
            carPanel.setName(maToa);

            // Determine background color based on car type (loaiToa)
            boolean isVIP = isVipCar(toaTau);

            // Check if this car is currently selected
            boolean isSelected = maToa.equals(currentToaId);

            Color bgColor = isVIP ?
                    new Color(255, 69, 0) : // Red-orange for Vietnam flag (VIP)
                    new Color(100, 181, 246); // Light blue (standard)

            // Add border for active state
            if (isSelected) {
                carPanel.setBorder(activeBorder);
            }

            JPanel colorPanel = new JPanel();
            colorPanel.setBackground(bgColor);
            colorPanel.setPreferredSize(new Dimension(50, 40));

            // Add car number label
            JLabel carLabel = new JLabel(toaNumber, SwingConstants.CENTER);
            carLabel.setPreferredSize(new Dimension(50, 10));
            carLabel.setForeground(Color.BLACK);
            carLabel.setFont(new Font("Arial", Font.BOLD, 12));

            carPanel.add(colorPanel, BorderLayout.CENTER);
            carPanel.add(carLabel, BorderLayout.SOUTH);

            // Add star for VIP cars
            if (isVIP) {
                JLabel starLabel = new JLabel("★", SwingConstants.CENTER);
                starLabel.setForeground(Color.YELLOW);
                starLabel.setFont(new Font("Arial", Font.BOLD, 16));
                colorPanel.setLayout(new BorderLayout());
                colorPanel.add(starLabel, BorderLayout.CENTER);
            }

            // Add tooltip for hovering
            String tooltip = "<html>" +
                    "<b>Toa số " + toaNumber + "</b><br>" +
                    "Loại toa: " + (toaTau.getLoaiToa() != null ? toaTau.getLoaiToa().getTenLoai() : "Thường") + "<br>" +
                    "Số ghế: " + toaTau.getSoGhe() + "<br>" +
                    (isVIP ? "<i>Toa VIP</i>" : "") +
                    "</html>";

            carPanel.setToolTipText(tooltip);

            // Add click listener for car selection
            final String toaId = toaTau.getMaToa();
            final String toaName = toaTau.getTenToa();
            final String finalToaNumber = toaNumber;

            carPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    try {
                        // Update current toa ID
                        currentToaId = toaId;

                        // Update the seating section label
                        seatingSectionLabel.setText("Toa số " + finalToaNumber + ": " + toaName);

                        // Update visual selection for all cars
                        updateCarSelection(toaId);

                        // Load and display the seat chart
                        loadSeatChart(currentTrainId, currentToaId, currentMaLich);

                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(carPanel,
                                "Lỗi khi chọn toa: " + ex.getMessage(),
                                "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }

                // Add hover effect
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!toaId.equals(currentToaId)) {
                        colorPanel.setBorder(BorderFactory.createLineBorder(Color.YELLOW, 2));
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (!toaId.equals(currentToaId)) {
                        colorPanel.setBorder(null);
                    }
                }
            });

            carsPanel.add(carPanel);
        }

        // Add locomotive
        JPanel locomotivePanel = new JPanel();
        locomotivePanel.setLayout(new BorderLayout());
        locomotivePanel.setPreferredSize(new Dimension(70, 50));

        JPanel colorPanel = new JPanel();
        colorPanel.setBackground(activeColor);
        colorPanel.setPreferredSize(new Dimension(70, 40));

        JLabel carLabel = new JLabel(trainId, SwingConstants.CENTER);
        carLabel.setPreferredSize(new Dimension(70, 10));
        carLabel.setForeground(Color.BLACK);
        carLabel.setFont(new Font("Arial", Font.BOLD, 12));

        // Add tooltip for locomotive
        locomotivePanel.setToolTipText("<html><b>Đầu máy tàu " + trainId + "</b></html>");

        locomotivePanel.add(colorPanel, BorderLayout.CENTER);
        locomotivePanel.add(carLabel, BorderLayout.SOUTH);

        carsPanel.add(locomotivePanel);

        // Refresh UI
        carsPanel.revalidate();
        carsPanel.repaint();
    }

    /**
     * Load and display the seat chart
     */
    private void loadSeatChart(String trainId, String toaId, String maLich) {
        try {
            if (trainId.isEmpty() || toaId.isEmpty() || maLich.isEmpty()) {
                seatsPanel.removeAll();
                seatsPanel = createPlaceholderSeatsPanel();
                return;
            }

            // Get seats with their status
            Map<String, String> seatsMap = choNgoiDAO.getAvailableSeatsMapByScheduleAndToa(maLich, toaId);

            // If no seats returned, show message
            if (seatsMap == null || seatsMap.isEmpty()) {
                seatsPanel.removeAll();
                JLabel noSeatsLabel = new JLabel("Không tìm thấy thông tin ghế cho toa này", SwingConstants.CENTER);
                noSeatsLabel.setFont(new Font("Arial", Font.ITALIC, 14));
                noSeatsLabel.setForeground(Color.GRAY);
                seatsPanel.add(noSeatsLabel, BorderLayout.CENTER);
                seatsPanel.revalidate();
                seatsPanel.repaint();
                return;
            }

            // Create a new panel for the seat layout
            seatsPanel.removeAll();
            seatsPanel.setLayout(new BorderLayout());

            // Create seat visualization panel
            JPanel seatVisualizationPanel = createSeatVisualizationPanel(seatsMap);

            // Add a legend panel at the bottom
            JPanel legendPanel = createSeatLegendPanel();

            // Add both panels to the seats panel
            seatsPanel.add(seatVisualizationPanel, BorderLayout.CENTER);
            seatsPanel.add(legendPanel, BorderLayout.SOUTH);
            System.out.println("Loading seat chart for train: " + trainId + ", car: " + toaId);
            System.out.println("Schedule ID: " + maLich);
            if (seatsMap != null) {
                System.out.println("Received " + seatsMap.size() + " seats from DAO");
                if (seatsMap.size() > 100) {
                    System.out.println("WARNING: Unusually high seat count detected!");
                    // Print first 10 seats for inspection
                    int count = 0;
                    for (Map.Entry<String, String> entry : seatsMap.entrySet()) {
                        System.out.println("Seat: " + entry.getKey() + ", Status: " + entry.getValue());
                        if (++count >= 10) break;
                    }
                }
            }
            seatsPanel.revalidate();
            seatsPanel.repaint();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Lỗi khi tải sơ đồ ghế: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    /**
     * Create the visualization panel for seats
     */
    /**
     * Create the visualization panel for seats with exactly 4 rows as requested
     * IMPROVED: Uses ChoNgoi.getById() to get proper seat names
     */
    private JPanel createSeatVisualizationPanel(Map<String, String> seatsMap) {
        // Main panel with border layout
        JPanel containerPanel = new JPanel(new BorderLayout());
        containerPanel.setBackground(Color.WHITE);

        // Create the seat grid panel with fixed 4-row layout
        JPanel seatGridPanel = new JPanel();
        seatGridPanel.setBackground(Color.WHITE);
        seatGridPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Get all seat IDs and sort them
        List<String> seatIds = new ArrayList<>(seatsMap.keySet());
        Collections.sort(seatIds, (s1, s2) -> {
            // Extract numeric parts for comparison
            String num1 = s1.replaceAll("[^0-9]", "");
            String num2 = s2.replaceAll("[^0-9]", "");

            try {
                return Integer.parseInt(num1) - Integer.parseInt(num2);
            } catch (Exception e) {
                return s1.compareTo(s2);
            }
        });

        System.out.println("Total seats: " + seatIds.size());

        // Calculate columns based on total seats (ensuring 4 rows)
        int totalRows = 4;
        int totalSeatsPerSide = (int)Math.ceil(seatIds.size() / (double)totalRows / 2);
        int totalColumns = totalSeatsPerSide * 2 * 2 + 1; // 2 sides, 2 seats per block, +1 for aisle
        int aislePosition = totalColumns / 2;

        // Create a grid layout with 4 rows
        GridLayout gridLayout = new GridLayout(totalRows, totalColumns);
        gridLayout.setHgap(2);
        gridLayout.setVgap(2);
        seatGridPanel.setLayout(gridLayout);

        // First, create empty panels for all positions in the grid
        JPanel[][] seatPanels = new JPanel[totalRows][totalColumns];

        for (int row = 0; row < totalRows; row++) {
            for (int col = 0; col < totalColumns; col++) {
                // Create aisle at middle position
                if (col == aislePosition) {
                    JPanel aislePanel = createAislePanel();
                    seatPanels[row][col] = aislePanel;
                    seatGridPanel.add(aislePanel);
                }
                // Create divider after each pair of seats
                else if ((col % 2 == 0) && col != 0 && col != aislePosition + 1) {
                    JPanel dividerPanel = createVerticalDivider();
                    seatPanels[row][col] = dividerPanel;
                    seatGridPanel.add(dividerPanel);
                }
                else {
                    // Create empty panel initially
                    JPanel emptyPanel = new JPanel();
                    emptyPanel.setBackground(Color.WHITE);
                    seatPanels[row][col] = emptyPanel;
                    seatGridPanel.add(emptyPanel);
                }
            }
        }

        // Now place actual seats according to the zigzag pattern
        int currentSeat = 0;

        // This tracks actual seat positions (skipping dividers and aisles)
        int actualCol = 0;

        // Place seats into appropriate positions
        while (currentSeat < seatIds.size()) {
            // Calculate the column group (0, 1, 2...) and position within group (0 or 1)
            int colGroup = actualCol / 2;
            int posInGroup = actualCol % 2;

            // Calculate the actual column in the grid (accounting for dividers)
            int gridCol;
            if (colGroup < totalSeatsPerSide) {
                // Left side of aisle
                gridCol = colGroup * 3 + posInGroup;
            } else {
                // Right side of aisle (after aisle)
                gridCol = aislePosition + 1 + (colGroup - totalSeatsPerSide) * 3 + posInGroup;
            }

            // Skip if we're out of grid bounds
            if (gridCol >= totalColumns) break;

            // Apply zigzag pattern
            if (posInGroup == 0) {
                // Top to bottom
                for (int row = 0; row < totalRows && currentSeat < seatIds.size(); row++) {
                    placeSeatInGrid(seatGridPanel, seatPanels, seatIds, seatsMap, currentSeat, row, gridCol);
                    currentSeat++;
                }
            } else {
                // Bottom to top
                for (int row = totalRows - 1; row >= 0 && currentSeat < seatIds.size(); row--) {
                    placeSeatInGrid(seatGridPanel, seatPanels, seatIds, seatsMap, currentSeat, row, gridCol);
                    currentSeat++;
                }
            }

            actualCol++;
        }

        // Wrap in scroll pane
        JScrollPane scrollPane = new JScrollPane(seatGridPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER); // Only horizontal scrolling
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);

        containerPanel.add(scrollPane, BorderLayout.CENTER);
        return containerPanel;
    }

    /**
     * Helper method to place a seat in the grid
     */
    private void placeSeatInGrid(JPanel seatGridPanel, JPanel[][] seatPanels, List<String> seatIds,
                                 Map<String, String> seatsMap, int currentSeat, int row, int col) {
        String seatId = seatIds.get(currentSeat);

        // Get the actual seat name from ChoNgoi object
        ChoNgoi choNgoi = null;
        choNgoi = choNgoiDAO.getById(seatId);

        // Use friendly display name if available, otherwise use ID
        String displayName = (choNgoi != null && choNgoi.getTenCho() != null) ?
                choNgoi.getTenCho() : seatId;

        JPanel seatPanel = createSeatPanelWithFriendlyName(seatId, displayName, seatsMap.get(seatId));
        seatGridPanel.remove(seatPanels[row][col]);
        seatPanels[row][col] = seatPanel;
        seatGridPanel.add(seatPanel, row * seatPanels[0].length + col);
    }

    /**
     * Create a vertical divider
     */
    private JPanel createVerticalDivider() {
        JPanel dividerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(new Color(184, 134, 11)); // Gold color
                g.fillRect(getWidth() / 2 - 1, 0, 3, getHeight());
            }
        };
        dividerPanel.setPreferredSize(new Dimension(8, 30));
        dividerPanel.setOpaque(false);
        return dividerPanel;
    }

    /**
     * Create a seat panel with friendly name display
     */
    private JPanel createSeatPanelWithFriendlyName(String seatId, String displayName, String status) {
        JPanel outerPanel = new JPanel(new BorderLayout());
        outerPanel.setBackground(Color.WHITE);

        // Create gold vertical divider on left
        JPanel leftDivider = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(new Color(184, 134, 11)); // Gold color
                g.fillRect(0, 0, 3, getHeight());
            }
        };
        leftDivider.setPreferredSize(new Dimension(3, 30));
        leftDivider.setOpaque(false);

        // Create the actual seat panel
        JPanel seatPanel = new JPanel(new BorderLayout());
        seatPanel.setPreferredSize(new Dimension(60, 30));
        seatPanel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));

        // Check if this seat is in reservation process
        boolean isReserved = reservationTimers.containsKey(seatId);
        if (isReserved) {
            // Override status for reserved seats
            status = STATUS_PENDING;
        }

        // Set background color based on status
        Color bgColor;
        switch (status) {
            case STATUS_BOOKED:
                bgColor = SEAT_BOOKED_COLOR;
                break;
            case STATUS_PENDING:
                bgColor = SEAT_PENDING_COLOR;
                break;
            case STATUS_AVAILABLE:
            default:
                bgColor = SEAT_AVAILABLE_COLOR;
                break;
        }
        seatPanel.setBackground(bgColor);

        // Add seat name label
        JLabel nameLabel = new JLabel(displayName, SwingConstants.CENTER);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 10));
        seatPanel.add(nameLabel, BorderLayout.CENTER);

        try {
            // Get the seat price for tooltip
            ChoNgoi choNgoi = choNgoiDAO.getById(seatId);
            double price = getSeatPrice(choNgoi);
            String formattedPrice = formatCurrency(price);

            // Add tooltip with price
            String tooltipStatus = status;
            if (isReserved) {
                int remaining = remainingTimes.get(seatId);
                int minutes = remaining / 60;
                int seconds = remaining % 60;
                String timeString = String.format("%02d:%02d", minutes, seconds);
                tooltipStatus = "Chờ xác nhận (còn " + timeString + ")";
            }

            seatPanel.setToolTipText("<html>" +
                    "Ghế: <b>" + displayName + "</b><br>" +
                    "Trạng thái: " + tooltipStatus + "<br>" +
                    "Giá vé: <b>" + formattedPrice + "</b>" +
                    "</html>");
        } catch (Exception e) {
            // Fallback tooltip
            seatPanel.setToolTipText("Ghế " + displayName + ": " + status);
        }

        // Add click listener for available seats
        if (status.equals(STATUS_AVAILABLE)) {
            seatPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            seatPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    selectSeat(seatId);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    seatPanel.setBackground(new Color(230, 255, 230));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    seatPanel.setBackground(bgColor);
                }
            });
        }

        // Assemble the complete seat with divider
        outerPanel.add(leftDivider, BorderLayout.WEST);
        outerPanel.add(seatPanel, BorderLayout.CENTER);

        return outerPanel;
    }
    /**
     * Create an aisle panel
     */
    private JPanel createAislePanel() {
        JPanel aislePanel = new JPanel();
        aislePanel.setBackground(new Color(100, 100, 100)); // Gray color
        aislePanel.setPreferredSize(new Dimension(15, 30));
        return aislePanel;
    }





    /**
     * Create an individual seat panel
     */

    /**
     * Handle seat selection
     */
    private void selectSeat(String seatId) {
        try {
            // Check if seat is already in pending status
            if (reservationTimers.containsKey(seatId)) {
                JOptionPane.showMessageDialog(this,
                        "Ghế này đã được giữ chỗ và đang chờ thanh toán.",
                        "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Get the seat info from DAO
            ChoNgoi choNgoi = choNgoiDAO.getById(seatId);

            if (choNgoi == null) {
                JOptionPane.showMessageDialog(this,
                        "Không tìm thấy thông tin ghế: " + seatId,
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Get seat price based on route and seat class
            double price = getSeatPrice(choNgoi);
            String displayName = (choNgoi.getTenCho() != null) ? choNgoi.getTenCho() : seatId;

                // Update UI to reflect pending status
                updateSeatStatus(seatId, STATUS_PENDING);
                // rerender
                reloadSeatUI(seatId);

                // Add to cart
                addToCartWithTimeout(seatId, displayName, price);

                // Start countdown timer for this seat
                startReservationCountdown(seatId, displayName, price);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Lỗi khi giữ chỗ: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
    private void reloadSeatUI(String seatId) {
        // Use SwingUtilities.invokeLater to ensure UI updates happen on EDT
        SwingUtilities.invokeLater(() -> {
            // Find the seating visualization panel (first component of seatsPanel)
            if (seatsPanel.getComponentCount() > 0) {
                Component visualPanel = seatsPanel.getComponent(0);
                if (visualPanel instanceof JPanel) {
                    // Get scroll pane
                    Component scrollComp = ((JPanel)visualPanel).getComponent(0);
                    if (scrollComp instanceof JScrollPane) {
                        // Force repaint of the viewport and all its children
                        JViewport viewport = ((JScrollPane)scrollComp).getViewport();
                        viewport.repaint();

                        // Queue another repaint after a short delay to ensure UI updates
                        Timer repaintTimer = new Timer(50, e -> viewport.repaint());
                        repaintTimer.setRepeats(false);
                        repaintTimer.start();
                    }
                }
            }
        });
    }
    private boolean findAndUpdateComponentRecursively(Container container, String seatId) {
        // Check all components in this container
        for (Component comp : container.getComponents()) {
            // If it's the outer seat panel that contains a panel with BorderLayout
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;

                // Check if this panel has a BorderLayout and contains our seat panel
                if (panel.getLayout() instanceof BorderLayout) {
                    Component centerComp = ((BorderLayout)panel.getLayout()).getLayoutComponent(BorderLayout.CENTER);
                    if (centerComp instanceof JPanel) {
                        JPanel seatPanel = (JPanel) centerComp;

                        // Check for tooltip text containing the seat ID (most reliable way)
                        String tooltip = seatPanel.getToolTipText();
                        if (tooltip != null && tooltip.contains("Ghế: <b>" + seatId + "</b>")) {
                            // Found it! Update color based on status
                            String status = reservationTimers.containsKey(seatId) ? STATUS_PENDING : STATUS_AVAILABLE;
                            Color bgColor = switch (status) {
                                case STATUS_PENDING -> SEAT_PENDING_COLOR;
                                case STATUS_BOOKED -> SEAT_BOOKED_COLOR;
                                default -> SEAT_AVAILABLE_COLOR;
                            };

                            seatPanel.setBackground(bgColor);
                            seatPanel.repaint();
                            return true;
                        }
                    }
                }

                // Recursively search children
                if (findAndUpdateComponentRecursively(panel, seatId)) {
                    return true;
                }
            } else if (comp instanceof Container) {
                // Check other containers (like scroll panes)
                if (findAndUpdateComponentRecursively((Container) comp, seatId)) {
                    return true;
                }
            }
        }
        return false;
    }
    /**
     * Start countdown timer for seat reservation
     */
    private void startReservationCountdown(String seatId, String displayName, double price) {
        // Initialize the remaining time
        remainingTimes.put(seatId, RESERVATION_TIMEOUT);

        // Create and start the timer
        Timer timer = new Timer(1000, null); // 1 second intervals
        timer.addActionListener(e -> {
            int remaining = remainingTimes.get(seatId) - 1;
            remainingTimes.put(seatId, remaining);

            // Update the countdown in the cart
            updateCartItemCountdown(seatId, remaining);

            // If time is up, release the seat
            if (remaining <= 0) {
                timer.stop();
                releaseReservation(seatId);
            }
        });

        // Store the timer and start it
        reservationTimers.put(seatId, timer);
        timer.start();
    }



    /**
     * Release the reservation when timer expires
     */
    private void releaseReservation(String seatId) {
        // Run on EDT to avoid Swing thread issues
        SwingUtilities.invokeLater(() -> {
            try {
                // Update UI to show seat is available again
                updateSeatStatus(seatId, STATUS_AVAILABLE);

                // Remove from cart
                removeFromCart(seatId);

                // Clean up timer references
                reservationTimers.remove(seatId);
                remainingTimes.remove(seatId);

                System.out.println("Reservation for seat " + seatId + " has expired");
            } catch (Exception ex) {
                System.err.println("Error releasing reservation: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
    }


    private double getSeatPrice(ChoNgoi choNgoi) {
        // In a real app, you would get this from your pricing service or database
        // For this example, we'll use a base price based on car type
        double basePrice = 250000.0; // Base price for regular seats

        try {
            // Get the car info to determine if it's a VIP or regular seat
            ToaTau toaTau = toaTauDAO.getToaTauById(currentToaId);
            if (toaTau != null && toaTau.getLoaiToa() != null) {
                // Apply multiplier for VIP cars
                if (isVipCar(toaTau)) {
                    basePrice *= 1.5; // 50% premium for VIP seats
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting seat price: " + e.getMessage());
        }

        return basePrice;
    }

    /**
     * Format currency in Vietnamese format
     */
    private String formatCurrency(double amount) {
        NumberFormat currencyFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        return currencyFormat.format(amount) + " đ";
    }

    /**
     * Add to cart with timeout feature
     */
    private void addToCartWithTimeout(String seatId, String displayName, double price) {
        String from = departureField.getText();
        String to = arrivalField.getText();
        String departureDate = departureDateField.getText();

        // Create and add ticket item to cart with seatId for later reference
        TicketItem item = new TicketItem(currentTrainId, seatId, displayName, from, to, departureDate, price);
        cartItems.add(item);

        // Update cart display
        updateCartDisplay();

        // Immediately update the seat status in UI to show as pending (yellow)
        updateSeatStatus(seatId, STATUS_PENDING);

    }


    /**
     * Update cart item to show countdown
     */


    /**
     * Helper to update a panel with countdown
     */

    /**
     * Remove an item from cart by seat ID
     */
    private void removeFromCart(String seatId) {
        Iterator<TicketItem> iterator = cartItems.iterator();
        while (iterator.hasNext()) {
            TicketItem item = iterator.next();
            if (item.seatId.equals(seatId)) {
                iterator.remove();
                break;
            }
        }

        // Update the cart display
        updateCartDisplay();
    }

    /**
     * Update seat status in UI
     */
    private void updateSeatStatus(String seatId, String status) {
        try {
            // Re-load the seat chart to reflect changes
            loadSeatChart(currentTrainId, currentToaId, currentMaLich);
        } catch (Exception ex) {
            System.err.println("Error updating seat status: " + ex.getMessage());
        }
    }

    /**

    /**
     * Create legend panel
     */
    private JPanel createSeatLegendPanel() {
        JPanel legendPanel = new JPanel();
        legendPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 5));
        legendPanel.setBackground(Color.WHITE);

        // Available seat legend
        addLegendItem(legendPanel, SEAT_AVAILABLE_COLOR, "Trống");

        // Booked seat legend
        addLegendItem(legendPanel, SEAT_BOOKED_COLOR, "Đã đặt");

        // Pending seat legend
        addLegendItem(legendPanel, SEAT_PENDING_COLOR, "Chờ xác nhận");

        return legendPanel;
    }

    /**
     * Helper to add legend items
     */
    private void addLegendItem(JPanel legendPanel, Color color, String text) {
        JPanel colorBox = new JPanel();
        colorBox.setPreferredSize(new Dimension(20, 20));
        colorBox.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        colorBox.setBackground(color);

        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.PLAIN, 12));

        JPanel itemPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        itemPanel.setOpaque(false);
        itemPanel.add(colorBox);
        itemPanel.add(label);

        legendPanel.add(itemPanel);
    }

    /**
     * Update car selection visual state
     */
    private void updateCarSelection(String selectedToaId) {
        Component[] components = carsPanel.getComponents();

        for (Component comp : components) {
            if (comp instanceof JPanel) {
                JPanel carPanel = (JPanel) comp;

                // Skip the locomotive panel which doesn't have toa ID
                if (carPanel.getPreferredSize().width > 50) continue;

                // Clear any existing border
                carPanel.setBorder(null);

                // If this panel has the name matching the selected toa ID
                if (carPanel.getName() != null && carPanel.getName().equals(selectedToaId)) {
                    carPanel.setBorder(activeBorder);
                }
            }
        }

        carsPanel.revalidate();
        carsPanel.repaint();
    }

    /**
     * Determine if a car is VIP based on loaiToa
     */
    private boolean isVipCar(ToaTau toaTau) {
        if (toaTau.getLoaiToa() == null) return false;

        LoaiToa loaiToa = toaTau.getLoaiToa();
        String tenLoai = loaiToa.getTenLoai();

        // Check if the type name contains VIP or premium indicators
        return tenLoai != null && (
                tenLoai.contains("VIP") ||
                        tenLoai.contains("Cao cấp") ||
                        tenLoai.contains("Đặc biệt")
        );
    }

    /**
     * Update train selection and load its cars
     */
    private void updateTrainSelection(String selectedId, String trainCode) throws RemoteException {
        System.out.println("Selecting train with ID: " + selectedId + ", code: " + trainCode);

        // Update selection in train cards
        for (Component component : trainsPanel.getComponents()) {
            if (component instanceof JPanel) {
                JPanel panel = (JPanel) component;

                // Get the unique train ID from client property
                Object trainIdObj = panel.getClientProperty("trainId");

                // Skip if this panel doesn't have a train ID
                if (trainIdObj == null) {
                    continue;
                }

                String trainId = (String) trainIdObj;
                boolean isSelected = trainId.equals(selectedId);

                System.out.println("Train card: " + trainId + ", Selected: " + isSelected);

                // Update the card appearance based on selection state
                updateCardAppearance(panel, isSelected);
            }
        }

        // Update current train ID and load its cars
        currentTrainId = trainCode;
        currentToaId = ""; // Reset current toa
        loadTrainCars(currentTrainId);
    }

    /**
     * Helper method to update a train card's appearance
     */
    private void updateCardAppearance(JPanel cardPanel, boolean isSelected) {
        // Set the card border
        cardPanel.setBorder(isSelected ? activeBorder : normalBorder);

        // Set the card background
        cardPanel.setBackground(isSelected ? activeColor : inactiveColor);

        // Update each contained panel except the header (which is at NORTH)
        for (Component comp : cardPanel.getComponents()) {
            if (comp instanceof JPanel) {
                // Don't change the header panel's background
                if (((BorderLayout)cardPanel.getLayout()).getConstraints(comp) == BorderLayout.NORTH) {
                    continue;
                }

                // Update background of other panels
                comp.setBackground(isSelected ? activeColor : inactiveColor);
            }
        }
    }

    /**
     * Create the trip information panel (left panel)
     */
    private JPanel createTripInfoPanel() {
        JPanel tripInfoPanel = new JPanel();
        tripInfoPanel.setLayout(new BorderLayout());
        tripInfoPanel.setPreferredSize(new Dimension(250, 0));
        tripInfoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Title panel
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(activeColor);

        // Header with icon
        JLabel headerLabel = new JLabel("≡ Thông tin hành trình", JLabel.LEFT);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 14));
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setBorder(BorderFactory.createEmptyBorder(8, 5, 8, 5));
        titlePanel.add(headerLabel, BorderLayout.CENTER);

        // Form panel
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        // Departure station
        formPanel.add(createFormLabel("Ga đi"));
        departureField = new JTextField("Sài Gòn");
        formPanel.add(departureField);
        formPanel.add(Box.createVerticalStrut(10));

        // Arrival station
        formPanel.add(createFormLabel("Ga đến"));
        arrivalField = new JTextField("Hà Nội");
        formPanel.add(arrivalField);
        formPanel.add(Box.createVerticalStrut(10));

        // Trip type
        JPanel tripTypePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        oneWayRadio = new JRadioButton("Một chiều");
        oneWayRadio.setSelected(true);
        roundTripRadio = new JRadioButton("Khứ hồi");
        ButtonGroup tripTypeGroup = new ButtonGroup();
        tripTypeGroup.add(oneWayRadio);
        tripTypeGroup.add(roundTripRadio);
        tripTypePanel.add(oneWayRadio);
        tripTypePanel.add(Box.createHorizontalStrut(10));
        tripTypePanel.add(roundTripRadio);
        formPanel.add(tripTypePanel);
        formPanel.add(Box.createVerticalStrut(10));

        // Departure date
        formPanel.add(createFormLabel("Ngày đi"));
        JPanel departureDatePanel = new JPanel(new BorderLayout(5, 0));
        departureDateField = new JTextField("20/04/2025");
        JButton departureDateButton = createCalendarButton();
        departureDatePanel.add(departureDateField, BorderLayout.CENTER);
        departureDatePanel.add(departureDateButton, BorderLayout.EAST);
        formPanel.add(departureDatePanel);
        formPanel.add(Box.createVerticalStrut(10));

        // Return date
        formPanel.add(createFormLabel("Ngày về"));
        JPanel returnDatePanel = new JPanel(new BorderLayout(5, 0));
        returnDateField = new JTextField("20/04/2025");
        JButton returnDateButton = createCalendarButton();
        returnDatePanel.add(returnDateField, BorderLayout.CENTER);
        returnDatePanel.add(returnDateButton, BorderLayout.EAST);
        formPanel.add(returnDatePanel);
        formPanel.add(Box.createVerticalStrut(20));

        // Search button
        JButton searchButton = new JButton("Tìm kiếm");
        searchButton.setBackground(activeColor);
        searchButton.setForeground(Color.WHITE);
        searchButton.setFocusPainted(false);
        searchButton.addActionListener(e -> searchTrains());

        // Add hover effect to search button
        searchButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                searchButton.setBackground(hoverColor);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                searchButton.setBackground(activeColor);
            }
        });

        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.add(searchButton, BorderLayout.EAST);
        formPanel.add(searchPanel);

        // Add components to trip info panel
        tripInfoPanel.add(titlePanel, BorderLayout.NORTH);
        tripInfoPanel.add(formPanel, BorderLayout.CENTER);

        return tripInfoPanel;
    }

    /**
     * Create a form field label
     */
    private JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.PLAIN, 13));
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        return label;
    }

    /**
     * Create a calendar button for date fields
     */
    private JButton createCalendarButton() {
        JButton calendarButton = new JButton();
        calendarButton.setBackground(activeColor);
        calendarButton.setPreferredSize(new Dimension(30, 30));
        calendarButton.setFocusPainted(false);

        // Calendar icon fallback
        calendarButton.setIcon(new ImageIcon(getClass().getResource("/Anh_HeThong/calendar.png")) {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(Color.WHITE);
                g2.fillRect(x + 3, y + 3, 14, 14);
                g2.setColor(Color.WHITE);
                g2.fillRect(x + 5, y, 2, 4);
                g2.fillRect(x + 13, y, 2, 4);
                g2.setColor(activeColor);
                g2.drawRect(x + 3, y + 3, 14, 14);
                g2.drawLine(x + 3, y + 8, x + 17, y + 8);
                g2.dispose();
            }
            public int getIconWidth() { return 20; }
            public int getIconHeight() { return 20; }
        });

        // Add hover effect
        calendarButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                calendarButton.setBackground(hoverColor);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                calendarButton.setBackground(activeColor);
            }
        });

        return calendarButton;
    }

    /**
     * Search for trains based on form data
     */
    private void searchTrains() {
        try {
            // Get input data
            String gaDi = departureField.getText().trim();
            String gaDen = arrivalField.getText().trim();

            // Parse date
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate departureDate = LocalDate.parse(departureDateField.getText().trim(), formatter);

            // Call DAO to get train schedules
            List<LichTrinhTau> allLichTrinhList = lichTrinhTauDAO.getListLichTrinhTauByDateAndGaDiGaDen(departureDate, gaDi, gaDen);

            // Filter to only include trains that haven't departed or are in operation
            List<LichTrinhTau> filteredLichTrinhList = allLichTrinhList.stream()
                    .filter(lichTrinh -> {
                        TrangThai trangThai = lichTrinh.getTrangThai();
                        return trangThai == TrangThai.CHUA_KHOI_HANH ||
                                trangThai == TrangThai.HOAT_DONG;
                    })
                    .collect(Collectors.toList());

            if (filteredLichTrinhList.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Không tìm thấy lịch trình tàu phù hợp đang hoạt động hoặc chưa khởi hành vào ngày " +
                                departureDateField.getText(),
                        "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Clear the train panel and reset global state
            trainsPanel.removeAll();
            selectedTrainId = "";
            currentTrainId = "";
            currentToaId = "";
            currentMaLich = "";

            trainsPanel.setLayout(new BoxLayout(trainsPanel, BoxLayout.X_AXIS));
            trainsPanel.add(Box.createHorizontalStrut(10));

            // Create train cards only for filtered schedules
            for (LichTrinhTau lichTrinh : filteredLichTrinhList) {
                // Get train object
                Tau tau = tauDAO.getTauByLichTrinhTau(lichTrinh);

                // Format departure time
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
                String departTime = lichTrinh.getGioDi().format(timeFormatter);

                // Format date
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM");
                String departDate = lichTrinh.getNgayDi().format(dateFormatter);

                // Create formatted times for display
                String departTimeFormatted = departDate + " " + departTime;

                // Get available seats
                long availableSeats = lichTrinhTauDAO.getAvailableSeatsBySchedule(lichTrinh.getMaLich());

                // Create train card with status indicator
                JPanel trainCard = createTrainCard(
                        tau.getMaTau(),
                        departTimeFormatted,
                        availableSeats,
                        false, // Initially not selected
                        lichTrinh.getTrangThai(),  // Pass the status to possibly display it
                        lichTrinh.getMaLich()      // Store schedule ID
                );

                trainsPanel.add(trainCard);
                trainsPanel.add(Box.createHorizontalStrut(10));
            }

            // Clear car panel and update header
            loadTrainCars(""); // Empty train ID to show placeholder
            seatingSectionLabel.setText("Toa: Chưa chọn");

            // Update header
            updateHeaderInfo(gaDi, gaDen, departureDateField.getText());

            // Refresh UI
            trainsPanel.revalidate();
            trainsPanel.repaint();

            System.out.println("Search completed, " + filteredLichTrinhList.size() + " trains found");

        } catch (RemoteException ex) {
            JOptionPane.showMessageDialog(this,
                    "Lỗi kết nối đến máy chủ: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Lỗi tìm kiếm: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    /**
     * Update header information with route details
     */
    private void updateHeaderInfo(String from, String to, String date) {
        // Find the header label and update it
        Container contentPane = getContentPane();
        if (contentPane instanceof JPanel) {
            Component[] components = ((JPanel) contentPane).getComponents();
            for (Component comp : components) {
                if (comp instanceof JPanel) {
                    updateHeaderInPanel((JPanel)comp, from, to, date);
                }
            }
        }
    }

    /**
     * Update header text in panel recursively
     */
    private void updateHeaderInPanel(JPanel panel, String from, String to, String date) {
        Component[] components = panel.getComponents();
        for (Component comp : components) {
            if (comp instanceof JPanel) {
                Component[] innerComps = ((JPanel) comp).getComponents();
                for (Component innerComp : innerComps) {
                    if (innerComp instanceof JLabel) {
                        JLabel label = (JLabel) innerComp;
                        if (label.getText().contains("Chiều đi")) {
                            label.setText("Chiều đi: ngày " + date + " từ " + from + " đến " + to);
                            return;
                        }
                    }
                    if (innerComp instanceof JPanel) {
                        updateHeaderInPanel((JPanel)innerComp, from, to, date);
                    }
                }
            }
        }
    }

    /**
     * Create the ticket cart panel (right side)
     */
    private JPanel createTicketItemPanel(TicketItem item) {
        // Main panel with border layout for compact display
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230)),
                BorderFactory.createEmptyBorder(8, 5, 8, 5)
        ));
        panel.setBackground(Color.WHITE);

        // Left section - Train info
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(Color.WHITE);
        infoPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));

        // Train and route info
        JLabel trainLabel = new JLabel(item.trainCode + " " + item.from + "-" + item.to);
        trainLabel.setFont(new Font("Arial", Font.BOLD, 12));
        trainLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(trainLabel);

        // Date and car/seat info
        JLabel detailsLabel = new JLabel(item.departureDate + " " + item.seatNumber);
        detailsLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        detailsLabel.setForeground(Color.GRAY);
        detailsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(detailsLabel);

        // Add countdown if applicable
        if (remainingTimes.containsKey(item.seatId)) {
            int remaining = remainingTimes.get(item.seatId);
            int minutes = remaining / 60;
            int seconds = remaining % 60;
            String timeString = String.format("Còn %02d:%02d", minutes, seconds);

            JLabel timerLabel = new JLabel(timeString);
            timerLabel.setFont(new Font("Arial", Font.ITALIC, 11));
            timerLabel.setForeground(new Color(255, 153, 0));
            timerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            // Set the seatId property on the label itself
            timerLabel.putClientProperty("seatId", item.seatId);

            // Add a name to the component for easier debugging
            timerLabel.setName("timerLabel_" + item.seatId);

            infoPanel.add(timerLabel);
        }

        panel.add(infoPanel, BorderLayout.CENTER);

        // Right section - Price and delete button
        JPanel rightPanel = new JPanel(new BorderLayout(5, 0));
        rightPanel.setBackground(Color.WHITE);

        // Price in red
        JLabel priceLabel = new JLabel(formatNumber(item.price));
        priceLabel.setFont(new Font("Arial", Font.BOLD, 12));
        priceLabel.setForeground(new Color(192, 0, 0)); // Dark red
        rightPanel.add(priceLabel, BorderLayout.CENTER);

        // Delete button with trash icon
        JButton deleteButton = createTrashButton();
        deleteButton.addActionListener(e -> {
            // Stop the timer if it exists
            if (reservationTimers.containsKey(item.seatId)) {
                reservationTimers.get(item.seatId).stop();
                reservationTimers.remove(item.seatId);
                remainingTimes.remove(item.seatId);
            }

            // Update seat status to available
            updateSeatStatus(item.seatId, STATUS_AVAILABLE);

            // Remove from cart
            cartItems.remove(item);
            updateCartDisplay();
        });
        rightPanel.add(deleteButton, BorderLayout.EAST);

        panel.add(rightPanel, BorderLayout.EAST);

        return panel;
    }

    private String formatNumber(double number) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
        return format.format(number);
    }
    private JButton createTrashButton() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(24, 24));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Create trash icon
        button.setIcon(new ImageIcon(getClass().getResource("/Anh_HeThong/trash.png")) {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();

                // Draw simple trash icon if image not found
                g2.setColor(new Color(65, 130, 180)); // Steel blue

                // Draw trash can
                g2.fillRect(x + 5, y + 6, 12, 14);
                g2.setColor(Color.WHITE);
                g2.fillRect(x + 7, y + 8, 8, 10);

                // Draw trash lid
                g2.setColor(new Color(65, 130, 180));
                g2.fillRect(x + 3, y + 3, 16, 3);

                // Draw handle
                g2.drawLine(x + 9, y + 8, x + 9, y + 16);
                g2.drawLine(x + 13, y + 8, x + 13, y + 16);

                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return 24;
            }

            @Override
            public int getIconHeight() {
                return 24;
            }
        });

        return button;
    }
    private void updateCartItemCountdown(String seatId, int remainingSeconds) {
        try {
            // Format the time string
            int minutes = remainingSeconds / 60;
            int seconds = remainingSeconds % 60;
            String timeString = String.format("Còn %02d:%02d", minutes, seconds);

            // Use SwingUtilities.invokeLater for thread safety
            SwingUtilities.invokeLater(() -> {
                try {
                    // Find the label through component tree
                    Container contentPane = getContentPane();
                    if (contentPane == null) return;

                    // Add a debug log
                    System.out.println("Updating timer for seat: " + seatId + " with time: " + timeString);

                    JPanel containerPanel = findMainContainerPanel(contentPane);
                    if (containerPanel == null) return;

                    JPanel rightPanel = findRightPanel(containerPanel);
                    if (rightPanel == null) return;

                    JScrollPane scrollPane = findScrollPane(rightPanel);
                    if (scrollPane == null) return;

                    // Get the viewport and items panel
                    JViewport viewport = scrollPane.getViewport();
                    if (viewport == null) return;

                    JPanel itemsPanel = (JPanel) viewport.getView();
                    if (itemsPanel == null) return;

                    // Now find our specific timer label
                    boolean found = updateTimerInPanel(itemsPanel, seatId, timeString);

                    if (!found) {
                        System.out.println("Timer label for seat " + seatId + " not found");
                    }
                } catch (Exception ex) {
                    System.err.println("Error updating countdown: " + ex.getMessage());
                    ex.printStackTrace();
                }
            });
        } catch (Exception e) {
            System.err.println("Error preparing countdown update: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private JPanel findMainContainerPanel(Container contentPane) {
        for (Component comp : contentPane.getComponents()) {
            if (comp instanceof JPanel) {
                return (JPanel) comp;
            }
        }
        return null;
    }

    /**
     * Helper method to find right panel
     */
    private JPanel findRightPanel(JPanel containerPanel) {
        Component[] components = containerPanel.getComponents();
        // Right panel is typically the third component (index 2)
        if (components.length > 2 && components[2] instanceof JPanel) {
            return (JPanel) components[2];
        }
        return null;
    }

    /**
     * Helper method to find scroll pane
     */
    private JScrollPane findScrollPane(JPanel rightPanel) {
        for (Component comp : rightPanel.getComponents()) {
            if (comp instanceof JScrollPane) {
                return (JScrollPane) comp;
            }
        }
        return null;
    }
    private boolean updateTimerInPanel(JPanel panel, String seatId, String timeString) {
        // Look at each cart item
        for (Component comp : panel.getComponents()) {
            if (comp instanceof JPanel) {
                JPanel itemPanel = (JPanel) comp;

                // Look for the center component (info panel)
                Component centerComp = ((BorderLayout)itemPanel.getLayout()).getLayoutComponent(BorderLayout.CENTER);
                if (centerComp instanceof JPanel) {
                    JPanel infoPanel = (JPanel) centerComp;

                    // Check each component in the info panel
                    for (Component infoComp : infoPanel.getComponents()) {
                        if (infoComp instanceof JLabel) {
                            JLabel label = (JLabel) infoComp;

                            // Print some debug information
                            Object property = label.getClientProperty("seatId");
                            if (property != null) {
                                System.out.println("Found label with seatId: " + property);
                            }

                            // Check if this is our timer label
                            if (property != null && property.equals(seatId)) {
                                // Found it, update the text
                                label.setText(timeString);
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
    private void updateCartDisplay() {
        // Get the cart container
        Container contentPane = getContentPane();
        JPanel containerPanel = (JPanel) contentPane.getComponent(0);
        JPanel rightPanel = (JPanel) containerPanel.getComponent(2);

        // Get scroll pane from right panel
        JScrollPane scrollPane = null;
        for (Component comp : rightPanel.getComponents()) {
            if (comp instanceof JScrollPane) {
                scrollPane = (JScrollPane) comp;
                break;
            }
        }

        if (scrollPane != null) {
            JViewport viewport = scrollPane.getViewport();
            JPanel itemsPanel = (JPanel) viewport.getView();

            // Clear items panel
            itemsPanel.removeAll();

            // Update layout to vertical BoxLayout
            itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));

            if (cartItems.isEmpty()) {
                // Empty cart message
                JPanel emptyPanel = new JPanel(new BorderLayout());
                emptyPanel.setBorder(BorderFactory.createEmptyBorder(15, 5, 5, 5));
                emptyPanel.setOpaque(false);

                JLabel emptyLabel = new JLabel("Giỏ vé trống", JLabel.CENTER);
                emptyLabel.setFont(new Font("Arial", Font.ITALIC, 12));
                emptyLabel.setForeground(Color.GRAY);
                emptyPanel.add(emptyLabel, BorderLayout.CENTER);
                itemsPanel.add(emptyPanel);
            } else {
                // Add each ticket item
                for (TicketItem item : cartItems) {
                    JPanel ticketPanel = createTicketItemPanel(item);
                    itemsPanel.add(ticketPanel);
                }
            }

            // Update total
            double total = cartItems.stream().mapToDouble(item -> item.price).sum();
            totalLabel.setText("Tổng cộng: " + formatCurrency(total));

            // Make sure to revalidate and repaint both the items panel and the scrollpane
            itemsPanel.revalidate();
            itemsPanel.repaint();
            scrollPane.revalidate();
            scrollPane.repaint();

            // Scroll to the bottom to show the newly added item
            if (!cartItems.isEmpty()) {
                JScrollPane finalScrollPane = scrollPane;
                SwingUtilities.invokeLater(() -> {
                    JScrollBar verticalBar = finalScrollPane.getVerticalScrollBar();
                    verticalBar.setValue(verticalBar.getMaximum());
                });
            }
        }
    }

    private JPanel createTicketCartPanel() {
        JPanel cartPanel = new JPanel();
        cartPanel.setLayout(new BorderLayout());
        cartPanel.setPreferredSize(new Dimension(220, 0)); // Fixed width
        cartPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Title panel
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(activeColor);

        // Cart title with icon
        JLabel cartTitle = new JLabel(" Giỏ vé", JLabel.LEFT);
        cartTitle.setIcon(new ImageIcon(getClass().getResource("/Anh_HeThong/cart.png")) {
            // Fallback icon if resource is not found
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(Color.WHITE);
                g2.fillOval(x, y, 18, 18);
                g2.setColor(activeColor);
                g2.fillOval(x + 3, y + 3, 12, 12);
                g2.setColor(Color.WHITE);
                g2.fillRect(x + 7, y + 3, 4, 7);
                g2.fillRect(x + 3, y + 7, 12, 4);
                g2.dispose();
            }
            public int getIconWidth() { return 20; }
            public int getIconHeight() { return 20; }
        });
        cartTitle.setFont(new Font("Arial", Font.BOLD, 14));
        cartTitle.setForeground(Color.WHITE);
        cartTitle.setBorder(BorderFactory.createEmptyBorder(8, 5, 8, 5));
        titlePanel.add(cartTitle, BorderLayout.CENTER);

        // Cart items panel
        JPanel itemsPanel = new JPanel();
        itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));
        itemsPanel.setBackground(Color.WHITE);

        // Empty cart message
        JPanel emptyPanel = new JPanel(new BorderLayout());
        emptyPanel.setBorder(BorderFactory.createEmptyBorder(15, 5, 5, 5));
        emptyPanel.setOpaque(false);

        JLabel emptyLabel = new JLabel("Giỏ vé trống", JLabel.CENTER);
        emptyLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        emptyLabel.setForeground(Color.GRAY);
        emptyPanel.add(emptyLabel, BorderLayout.CENTER);
        itemsPanel.add(emptyPanel);

        // Scrollable cart with enhanced scrolling functionality
        JScrollPane scrollPane = new JScrollPane(itemsPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(Color.WHITE);
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16); // For smoother scrolling
        scrollPane.getVerticalScrollBar().setBlockIncrement(64); // Faster page scrolling

        // Use preferred size but don't set minimum size to allow dynamic resizing
        scrollPane.setPreferredSize(new Dimension(220, 250));

        // Make sure the viewport tracks the view's size changes
        scrollPane.getViewport().setViewSize(itemsPanel.getPreferredSize());

        // Make the scrollbar look better with custom UI
        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(180, 180, 180);
                this.trackColor = Color.WHITE;
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }

            private JButton createZeroButton() {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                button.setMinimumSize(new Dimension(0, 0));
                button.setMaximumSize(new Dimension(0, 0));
                return button;
            }
        });

        // Total panel
        JPanel totalPanel = new JPanel(new BorderLayout());
        totalPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        totalPanel.setBackground(new Color(245, 245, 245));
        totalLabel = new JLabel("Tổng cộng: 0 đ", JLabel.RIGHT);
        totalLabel.setFont(new Font("Arial", Font.BOLD, 14));
        totalLabel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));

        JButton checkoutButton = new JButton("Thanh toán");
        checkoutButton.setBackground(activeColor);
        checkoutButton.setForeground(Color.WHITE);
        checkoutButton.setFocusPainted(false);

        // Add hover effect to checkout button
        checkoutButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                checkoutButton.setBackground(hoverColor);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                checkoutButton.setBackground(activeColor);
            }
        });

        totalPanel.add(totalLabel, BorderLayout.NORTH);
        totalPanel.add(checkoutButton, BorderLayout.SOUTH);

        // Add components to cart panel
        cartPanel.add(titlePanel, BorderLayout.NORTH);
        cartPanel.add(scrollPane, BorderLayout.CENTER);
        cartPanel.add(totalPanel, BorderLayout.SOUTH);

        return cartPanel;
    }



    /**
     * Class to represent a ticket item in the cart
     */
    private class TicketItem {
        String trainCode;
        String seatId;      // The actual seat ID used by the system
        String seatNumber;  // The display name shown to user
        String from;
        String to;
        String departureDate;
        double price;

        public TicketItem(String trainCode, String seatId, String seatNumber,
                          String from, String to, String departureDate, double price) {
            this.trainCode = trainCode;
            this.seatId = seatId;
            this.seatNumber = seatNumber;
            this.from = from;
            this.to = to;
            this.departureDate = departureDate;
            this.price = price;
        }
    }




    /**
     * Main method
     */
    public static void main(String[] args) {
        // Set system time for debugging - in production this would be removed
        System.out.println("System time: " + new Date());
        System.out.println("User: luongtan204viet");

        SwingUtilities.invokeLater(() -> {
            try {
                TrainTicketBookingSystem app = new TrainTicketBookingSystem();
                app.setVisible(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Lỗi khởi động ứng dụng: " + e.getMessage(),
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        });
    }
}