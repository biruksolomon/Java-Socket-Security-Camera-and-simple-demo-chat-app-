package Camera;

import javax.swing.*;
import java.awt.*;
import org.opencv.core.*;
import org.opencv.videoio.VideoCapture;

import java.io.*;
import java.io.*;
import java.net.*;

public class camera {
    private JTextArea logArea;
    private JTextField portField;
    private JLabel statusLabel;
    private volatile boolean running = true;

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME); // Load OpenCV native library
        SwingUtilities.invokeLater(() -> new camera().createAndShowGUI());
    }

    private void createAndShowGUI() {
        JFrame frame = new JFrame("Video Server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);

        // Main Layout
        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel controls = new JPanel();
        controls.setLayout(new GridLayout(2, 2, 10, 10));

        // Input Field for Port
        JLabel portLabel = new JLabel("Port:");
        portField = new JTextField("1234");

        // Buttons
        JButton startButton = new JButton("Start Server");
        statusLabel = new JLabel("Server Not Started", SwingConstants.CENTER);

        // Custom Font for Status Label
        Font statusFont = new Font("Arial", Font.BOLD, 20);  // Set the font style and size for the status label
        statusLabel.setFont(statusFont);
        statusLabel.setForeground(Color.RED);  // Make the status text red for better visibility

        // Log Area
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 14));  // Set a monospaced font for the log area
        JScrollPane scrollPane = new JScrollPane(logArea);

        // Add Components
        controls.add(portLabel);
        controls.add(portField);
        controls.add(startButton);

        mainPanel.add(statusLabel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(controls, BorderLayout.SOUTH);

        frame.add(mainPanel);
        frame.setVisible(true);

        // Action Listener for Start Button
        startButton.addActionListener(e -> new Thread(this::startServer).start());
    }

    private void startServer() {
        int port = Integer.parseInt(portField.getText());

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            updateStatus("Server Listening...");
            log("Server is listening on port " + port);

            Socket socket = serverSocket.accept();
            updateStatus("Camera is Active");
            log("Client connected");

            OutputStream output = socket.getOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(output);

            VideoCapture capture = new VideoCapture(0); // Default webcam
            if (!capture.isOpened()) {
                log("Error: Cannot open the webcam.");
                return;
            }

            Mat frame = new Mat();
            while (running) {
                capture.read(frame);
                if (frame.empty()) break;

                // Send Frame to Client
                byte[] buffer = new byte[(int) frame.total() * frame.channels()];
                frame.get(0, 0, buffer);
                objectOutputStream.writeObject(buffer);
                objectOutputStream.flush();
            }

            capture.release();
            socket.close();
        } catch (IOException ex) {
            log("Server error: " + ex.getMessage());
        }
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }

    private void updateStatus(String message) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(message));
    }
}
