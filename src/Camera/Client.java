package Camera;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoWriter;
import org.opencv.highgui.HighGui;

import java.io.*;
import java.net.*;
import java.sql.*;

public class Client {
    private JLabel statusLabel;
    private JTextField ipField, portField;
    private JPanel[] videoPanels; // Panels for 4 partitions
    private JFrame frame;
    private ArrayList<String> cameraList = new ArrayList<>(); // Store camera IPs and ports
    private ConcurrentHashMap<Integer, Thread> cameraThreads = new ConcurrentHashMap<>();
    private int connectedCameras = 0;

    private Connection dbConnection;

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME); // Load OpenCV native library
        SwingUtilities.invokeLater(() -> new Client().createAndShowGUI());
    }

    private void createAndShowGUI() {
        frame = new JFrame("Video Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        // Main Layout
        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel controls = new JPanel(new GridLayout(3, 2, 10, 10));
        JPanel videoGrid = new JPanel(new GridLayout(2, 2, 10, 10)); // 2x2 grid for 4 partitions

        // IP Address Input
        JLabel ipLabel = new JLabel("Server IP:");
        ipField = new JTextField("localhost");
        JLabel portLabel = new JLabel("Port:");
        portField = new JTextField("1234");

        // Buttons
        JButton addCameraButton = new JButton("Add Camera");
        JButton connectButton = new JButton("Connect");
        statusLabel = new JLabel("No camera connected", SwingConstants.CENTER);

        // Initialize 4 video panels (black by default)
        videoPanels = new JPanel[4];
        for (int i = 0; i < 4; i++) {
            videoPanels[i] = new JPanel();
            videoPanels[i].setBackground(Color.BLACK);
            videoGrid.add(videoPanels[i]);
        }

        // Add Components
        controls.add(ipLabel);
        controls.add(ipField);
        controls.add(portLabel);
        controls.add(portField);
        controls.add(addCameraButton);
        controls.add(connectButton);

        mainPanel.add(statusLabel, BorderLayout.NORTH);
        mainPanel.add(videoGrid, BorderLayout.CENTER);
        mainPanel.add(controls, BorderLayout.SOUTH);

        frame.add(mainPanel);
        frame.setVisible(true);

        // Initialize Database
        initDatabase();

        // Action Listeners
        addCameraButton.addActionListener(e -> addCamera());
        connectButton.addActionListener(e -> connectToCameras());
    }

    private void initDatabase() {
        try {
            dbConnection = DriverManager.getConnection("jdbc:mysql://localhost:3306/", "root", "837535Abc@");
            Statement statement = dbConnection.createStatement();
            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS CameraDB");
            statement.executeUpdate("USE CameraDB");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS Recordings (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        camera_address VARCHAR(255),
                        recording_path VARCHAR(255),
                        timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
                    )
                """);
            log("Database initialized successfully.");
        } catch (SQLException e) {
            log("Database error: " + e.getMessage());
        }
    }

    private void addCamera() {
        if (cameraList.size() >= 4) {
            JOptionPane.showMessageDialog(frame, "Maximum 4 cameras can be added!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String serverAddress = ipField.getText();
        String port = portField.getText();

        if (serverAddress.isEmpty() || port.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter both IP address and port!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String cameraDetails = serverAddress + ":" + port;
        if (cameraList.contains(cameraDetails)) {
            JOptionPane.showMessageDialog(frame, "This camera is already added!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        cameraList.add(cameraDetails);
        updateStatus("Camera added: " + cameraDetails);
    }

    private void connectToCameras() {
        if (cameraList.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "No cameras added to connect!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        for (String cameraDetails : cameraList) {
            if (connectedCameras >= 4) {
                JOptionPane.showMessageDialog(frame, "Maximum 4 cameras can be connected!", "Warning", JOptionPane.WARNING_MESSAGE);
                break;
            }

            String[] details = cameraDetails.split(":");
            String serverAddress = details[0];
            int port = Integer.parseInt(details[1]);

            int partitionIndex = connectedCameras; // Assign next available partition
            connectedCameras++;

            // Start a new thread for this camera stream
            Thread cameraThread = new Thread(() -> startClient(serverAddress, port, partitionIndex));
            cameraThreads.put(partitionIndex, cameraThread);
            cameraThread.start();
        }

        cameraList.clear(); // Clear the list after connecting
    }

    private void startClient(String serverAddress, int port, int partitionIndex) {
        try (Socket socket = new Socket(serverAddress, port)) {
            updateStatus("Connected to: " + serverAddress + ":" + port);

            InputStream input = socket.getInputStream();
            ObjectInputStream objectInputStream = new ObjectInputStream(input);

            JPanel videoPanel = videoPanels[partitionIndex]; // Get assigned partition
            int panelWidth = videoPanel.getWidth();
            int panelHeight = videoPanel.getHeight();

            // Initialize VideoWriter for recording
            String outputPath = "recordings/camera_" + serverAddress + "_" + port + ".avi";
            new File("recordings").mkdirs(); // Ensure the recordings directory exists
            VideoWriter videoWriter = new VideoWriter(outputPath,VideoWriter.fourcc('M', 'J', 'P', 'G'), 20.0, new Size(640, 480));
            if (!videoWriter.isOpened()) {
                log("Error initializing video writer for " + serverAddress + ":" + port);
                return;
            }

            // Save recording metadata to database
            saveRecordingMetadata(serverAddress, port, outputPath);

            while (true) {
                byte[] frameData = (byte[]) objectInputStream.readObject();

                // Convert byte array to Mat
                Mat frame = new Mat(480, 640, CvType.CV_8UC3);
                frame.put(0, 0, frameData);

                // Write frame to video file
                videoWriter.write(frame);

                // Resize the frame to match the panel size
                Mat resizedFrame = new Mat();
                Imgproc.resize(frame, resizedFrame, new Size(panelWidth, panelHeight));

                // Convert frame from BGR to RGB
                Imgproc.cvtColor(resizedFrame, resizedFrame, Imgproc.COLOR_BGR2RGB);

                // Convert Mat to ImageIcon
                ImageIcon image = new ImageIcon(HighGui.toBufferedImage(resizedFrame));

                SwingUtilities.invokeLater(() -> {
                    videoPanel.removeAll();
                    videoPanel.add(new JLabel(image));
                    videoPanel.revalidate();
                    videoPanel.repaint();
                });
            }
        } catch (IOException | ClassNotFoundException ex) {
            updateStatus("Error with " + serverAddress + ":" + port + " - " + ex.getMessage());
            connectedCameras--;
            videoPanels[partitionIndex].setBackground(Color.BLACK); // Reset to black
        }
    }

    private void saveRecordingMetadata(String serverAddress, int port, String path) {
        try (PreparedStatement stmt = dbConnection.prepareStatement(
                "INSERT INTO Recordings (camera_address, recording_path) VALUES (?, ?)")) {
            stmt.setString(1, serverAddress + ":" + port);
            stmt.setString(2, path);
            stmt.executeUpdate();
            log("Recording metadata saved for " + serverAddress + ":" + port);
        } catch (SQLException e) {
            log("Database error: " + e.getMessage());
        }
    }

    private void updateStatus(String message) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(message));
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            System.out.println(message);
            statusLabel.setText(message);
        });
    }
}
