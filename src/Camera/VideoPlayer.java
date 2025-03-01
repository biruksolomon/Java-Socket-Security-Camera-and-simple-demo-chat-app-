package Camera;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.sql.*;
import org.opencv.core.*;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

public class VideoPlayer {
    private JFrame frame;
    private JTable videoTable;
    private DefaultTableModel tableModel;
    private Connection dbConnection;

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME); // Load OpenCV library
        SwingUtilities.invokeLater(() -> new VideoPlayer().createAndShowGUI());
    }

    private void createAndShowGUI() {
        // Setup Database Connection
        setupDatabase();

        // Frame Setup
        frame = new JFrame("Video Player");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout());

        // Table to Display Recordings
        String[] columnNames = {"ID", "Date", "Camera Address", "Video Path"};
        tableModel = new DefaultTableModel(columnNames, 0);
        videoTable = new JTable(tableModel);
        JScrollPane tableScrollPane = new JScrollPane(videoTable);

        // Load Videos into the Table
        loadVideoList();

        // Play Button
        JButton playButton = new JButton("Play Selected Video");
        playButton.addActionListener(e -> playSelectedVideo());

        // Refresh Button
        JButton refreshButton = new JButton("Refresh List");
        refreshButton.addActionListener(e -> loadVideoList());

        // Buttons Panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(playButton);
        buttonPanel.add(refreshButton);

        // Add Components to Frame
        frame.add(new JLabel("Recorded Videos", SwingConstants.CENTER), BorderLayout.NORTH);
        frame.add(tableScrollPane, BorderLayout.CENTER);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private void setupDatabase() {
        try {
            // Replace with your database details
            String url = "jdbc:mysql://localhost:3306/cameradb";
            String user = "root";
            String password = "837535Abc@";

            dbConnection = DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Database connection error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void loadVideoList() {
        tableModel.setRowCount(0); // Clear existing rows

        try (Statement stmt = dbConnection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, timestamp, camera_address, recording_path FROM Recordings")) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String date = rs.getString("timestamp");
                String cameraAddress = rs.getString("camera_address");
                String videoPath = rs.getString("recording_path");

                tableModel.addRow(new Object[]{id, date, cameraAddress, videoPath});
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error loading videos: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void playSelectedVideo() {
        int selectedRow = videoTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(frame, "Please select a video to play!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String filePath = (String) tableModel.getValueAt(selectedRow, 3);
        playVideo(filePath);
    }

    private void playVideo(String filePath) {
        VideoCapture videoCapture = new VideoCapture(filePath);

        if (!videoCapture.isOpened()) {
            JOptionPane.showMessageDialog(frame, "Unable to open video file: " + filePath, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Mat frame = new Mat();

        double fps = videoCapture.get(Videoio.CAP_PROP_FPS); // Get frames per second
        long frameDelay = (long) (1000 / (fps > 0 ? fps : 30)); // Delay between frames in milliseconds

        while (videoCapture.read(frame)) {
            if (frame.empty()) {
                break;
            }

            HighGui.imshow("Playing Video: " + filePath, frame); // Display the video frame
            int key = HighGui.waitKey((int) frameDelay); // Wait for the specified delay or a key press
            if (key == 27) { // Exit on 'Esc' key
                break;
            }
        }

        videoCapture.release();
        HighGui.destroyAllWindows();
    }


    public static BufferedImage Mat2BufferedImage(Mat mat) {
        int width = mat.cols();
        int height = mat.rows();
        int channels = mat.channels();

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);

        byte[] data = new byte[width * height * channels];
        mat.get(0, 0, data);

        image.getRaster().setDataElements(0, 0, width, height, data);

        return image;
    }
}
