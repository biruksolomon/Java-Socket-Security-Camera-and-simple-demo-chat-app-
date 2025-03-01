package LiveStreamingApp;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.highgui.HighGui;
import org.opencv.videoio.VideoCapture;

import javax.swing.*;
import java.awt.*;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class LiveStreamClientGUI {
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    private JFrame frame;
    private JTextArea statusArea;
    private JButton connectButton;
    private JButton disconnectButton;

    private Socket socket;
    private VideoCapture capture;
    private boolean isRunning = false;

    public LiveStreamClientGUI() {
        setupGUI();
    }

    private void setupGUI() {
        frame = new JFrame("Live Streaming Client");
        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        statusArea = new JTextArea();
        statusArea.setEditable(false);
        JScrollPane statusScroll = new JScrollPane(statusArea);

        connectButton = new JButton("Connect to Server");
        disconnectButton = new JButton("Disconnect");
        disconnectButton.setEnabled(false);

        connectButton.addActionListener(e -> connectToServer());
        disconnectButton.addActionListener(e -> disconnectFromServer());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(connectButton);
        buttonPanel.add(disconnectButton);

        frame.setLayout(new BorderLayout());
        frame.add(statusScroll, BorderLayout.CENTER);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private void connectToServer() {
        connectButton.setEnabled(false);
        disconnectButton.setEnabled(true);
        appendStatus("Connecting to server...");

        new Thread(() -> {
            try {
                socket = new Socket("192.168.1.7", 1234);
                appendStatus("Connected to server.");

                capture = new VideoCapture(0);
                if (!capture.isOpened()) {
                    appendStatus("ERROR: Cannot access camera.");
                    disconnectFromServer();
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

                disconnectFromServer();
            } catch (Exception e) {
                appendStatus("Error: " + e.getMessage());
                e.printStackTrace();
                disconnectFromServer();
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

                HighGui.imshow("Server Camera Feed", receivedFrame);
                if (HighGui.waitKey(1) >= 0) break;
            }
        } catch (Exception e) {
            appendStatus("Error receiving server frames: " + e.getMessage());
            e.printStackTrace();
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

        HighGui.imshow("Client Camera Feed", frame);
    }

    private void disconnectFromServer() {
        appendStatus("Disconnecting...");
        isRunning = false;

        try {
            if (capture != null) capture.release();
            if (socket != null) socket.close();
            appendStatus("Disconnected from server.");
        } catch (Exception e) {
            appendStatus("Error disconnecting: " + e.getMessage());
        }

        connectButton.setEnabled(true);
        disconnectButton.setEnabled(false);
    }

    private void appendStatus(String message) {
        SwingUtilities.invokeLater(() -> statusArea.append(message + "\n"));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LiveStreamClientGUI::new);
    }
}

//import org.opencv.core.Core;
//import org.opencv.core.Mat;
//import org.opencv.highgui.HighGui;
//import org.opencv.videoio.VideoCapture;
//
//import java.io.ObjectInputStream;
//import java.io.ObjectOutputStream;
//import java.net.Socket;
//
//public class LiveStreamClient {
//    static {
//        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
//    }
//
//    private Socket socket;
//    private VideoCapture capture;
//    private boolean isRunning = false;
//
//    public void startClient() {
//        try {
//            System.out.println("Connecting to server...");
//            socket = new Socket("192.168.1.7", 1234);
//            System.out.println("Connected to server.");
//
//            capture = new VideoCapture(0);
//            if (!capture.isOpened()) {
//                System.out.println("ERROR: Cannot access camera.");
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
//            new Thread(() -> receiveFrames(inputStream)).start();
//
//            while (isRunning) {
//                capture.read(frame);
//                if (frame.empty()) {
//                    System.out.println("ERROR: Empty frame from camera.");
//                    break;
//                }
//                sendFrame(outputStream, frame);
//            }
//
//            disconnect();
//        } catch (Exception e) {
//            System.err.println("Error: " + e.getMessage());
//            e.printStackTrace();
//            disconnect();
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
//                HighGui.imshow("Server Camera Feed", receivedFrame);
//                if (HighGui.waitKey(1) >= 0) break;
//            }
//        } catch (Exception e) {
//            System.err.println("Error receiving server frames: " + e.getMessage());
//            e.printStackTrace();
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
//        HighGui.imshow("Client Camera Feed", frame);
//    }
//
//    private void disconnect() {
//        try {
//            System.out.println("Disconnecting...");
//            isRunning = false;
//            if (capture != null) capture.release();
//            if (socket != null) socket.close();
//            System.out.println("Disconnected from server.");
//        } catch (Exception e) {
//            System.err.println("Error disconnecting: " + e.getMessage());
//        }
//    }
//
//    public static void main(String[] args) {
//        new LiveStreamClient().startClient();
//    }
//}

