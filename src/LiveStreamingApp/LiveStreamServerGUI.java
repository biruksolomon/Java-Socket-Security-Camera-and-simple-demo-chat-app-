package LiveStreamingApp;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.highgui.HighGui;
import org.opencv.videoio.VideoCapture;

import javax.swing.*;
import java.awt.*;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class LiveStreamServerGUI {
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    private JFrame frame;
    private JTextArea statusArea;
    private JButton startButton;
    private JButton stopButton;

    private ServerSocket serverSocket;
    private Socket socket;
    private VideoCapture capture;
    private boolean isRunning = false;

    public LiveStreamServerGUI() {
        setupGUI();
    }

    private void setupGUI() {
        frame = new JFrame("Live Streaming Server");
        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        statusArea = new JTextArea();
        statusArea.setEditable(false);
        JScrollPane statusScroll = new JScrollPane(statusArea);

        startButton = new JButton("Start Server");
        stopButton = new JButton("Stop Server");
        stopButton.setEnabled(false);

        startButton.addActionListener(e -> startServer());
        stopButton.addActionListener(e -> stopServer());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);

        frame.setLayout(new BorderLayout());
        frame.add(statusScroll, BorderLayout.CENTER);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private void startServer() {
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        appendStatus("Starting server...");

        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(1234);
                appendStatus("Server is listening on port 1234...");

                socket = serverSocket.accept();
                appendStatus("Client connected!");

                capture = new VideoCapture(0);
                if (!capture.isOpened()) {
                    appendStatus("ERROR: Cannot access camera.");
                    stopServer();
                    return;
                }

                isRunning = true;

                ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());

                Mat frame = new Mat();

                new Thread(() -> receiveFrames(inputStream)).start();

                while (isRunning) {
                    capture.read(frame);
                    if (frame.empty()) {
                        appendStatus("ERROR: Empty frame from camera.");
                        break;
                    }

                    sendFrame(outputStream, frame);
                }

                stopServer();
            } catch (Exception e) {
                appendStatus("Error: " + e.getMessage());
                e.printStackTrace();
                stopServer();
            }
        }).start();
    }

    private void receiveFrames(ObjectInputStream inputStream) {
        try {
            while (isRunning) {
                int rows = inputStream.readInt();
                int cols = inputStream.readInt();
                int type = inputStream.readInt();
                byte[] frameData = (byte[]) inputStream.readObject();

                Mat receivedFrame = new Mat(rows, cols, type);
                receivedFrame.put(0, 0, frameData);

                HighGui.imshow("Client Camera Feed", receivedFrame);
                if (HighGui.waitKey(1) >= 0) break;
            }
        } catch (Exception e) {
            appendStatus("Error receiving client frames: " + e.getMessage());
        }
    }

    private void sendFrame(ObjectOutputStream outputStream, Mat frame) throws Exception {
        int rows = frame.rows();
        int cols = frame.cols();
        int type = frame.type();
        byte[] buffer = new byte[(int) frame.total() * frame.channels()];
        frame.get(0, 0, buffer);

        outputStream.writeInt(rows);
        outputStream.writeInt(cols);
        outputStream.writeInt(type);
        outputStream.writeObject(buffer);
        outputStream.flush();

        HighGui.imshow("Server Camera Feed", frame);
    }

    private void stopServer() {
        appendStatus("Stopping server...");
        isRunning = false;

        try {
            if (capture != null) capture.release();
            if (socket != null) socket.close();
            if (serverSocket != null) serverSocket.close();
            appendStatus("Server stopped.");
        } catch (Exception e) {
            appendStatus("Error stopping server: " + e.getMessage());
        }

        startButton.setEnabled(true);
        stopButton.setEnabled(false);
    }

    private void appendStatus(String message) {
        SwingUtilities.invokeLater(() -> statusArea.append(message + "\n"));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LiveStreamServerGUI::new);
    }
}

//import org.opencv.core.Core;
//import org.opencv.core.Mat;
//import org.opencv.videoio.VideoCapture;
//
//import java.io.ObjectInputStream;
//import java.io.ObjectOutputStream;
//import java.net.ServerSocket;
//import java.net.Socket;
//
//public class LiveStreamServer {
//    static {
//        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
//    }
//
//    private ServerSocket serverSocket;
//    private Socket socket;
//    private VideoCapture capture;
//    private boolean isRunning = false;
//
//    public void startServer() {
//        try {
//            System.out.println("Starting server...");
//            serverSocket = new ServerSocket(1234);
//            System.out.println("Server is listening on port 1234...");
//
//            socket = serverSocket.accept();
//            System.out.println("Client connected!");
//
//            capture = new VideoCapture(0); // Open server's camera
//            if (!capture.isOpened()) {
//                System.out.println("ERROR: Cannot access camera.");
//                stopServer();
//                return;
//            }
//
//            isRunning = true;
//
//            ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
//            ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
//
//            Mat frame = new Mat();
//
//            // Thread for receiving frames
//            new Thread(() -> receiveFrames(inputStream)).start();
//
//            // Continuously capture and send frames
//            while (isRunning) {
//                capture.read(frame);
//                if (frame.empty()) {
//                    System.out.println("ERROR: Empty frame from camera.");
//                    break;
//                }
//
//                sendFrame(outputStream, frame);
//            }
//
//            stopServer();
//        } catch (Exception e) {
//            System.out.println("Error: " + e.getMessage());
//            stopServer();
//        }
//    }
//
//    private void receiveFrames(ObjectInputStream inputStream) {
//        try {
//            while (isRunning) {
//                int rows = inputStream.readInt();
//                int cols = inputStream.readInt();
//                int type = inputStream.readInt();
//                byte[] frameData = (byte[]) inputStream.readObject();
//
//                Mat receivedFrame = new Mat(rows, cols, type);
//                receivedFrame.put(0, 0, frameData);
//
//                // Display the received frame (optional)
//                // HighGui.imshow("Client Camera Feed", receivedFrame);
//                // if (HighGui.waitKey(1) >= 0) break;
//            }
//        } catch (Exception e) {
//            System.out.println("Error receiving client frames: " + e.getMessage());
//        }
//    }
//
//    private void sendFrame(ObjectOutputStream outputStream, Mat frame) throws Exception {
//        int rows = frame.rows();
//        int cols = frame.cols();
//        int type = frame.type();
//        byte[] buffer = new byte[(int) frame.total() * frame.channels()];
//        frame.get(0, 0, buffer);
//
//        outputStream.writeInt(rows);
//        outputStream.writeInt(cols);
//        outputStream.writeInt(type);
//        outputStream.writeObject(buffer);
//        outputStream.flush();
//
//        // Display the server camera feed (optional)
//        // HighGui.imshow("Server Camera Feed", frame);
//    }
//
//    private void stopServer() {
//        System.out.println("Stopping server...");
//        isRunning = false;
//
//        try {
//            if (capture != null) capture.release();
//            if (socket != null) socket.close();
//            if (serverSocket != null) serverSocket.close();
//            System.out.println("Server stopped.");
//        } catch (Exception e) {
//            System.out.println("Error stopping server: " + e.getMessage());
//        }
//    }
//
//    public static void main(String[] args) {
//        new LiveStreamServer().startServer();
//    }
//}
