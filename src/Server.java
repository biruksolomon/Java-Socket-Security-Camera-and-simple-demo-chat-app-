//import org.opencv.core.Core;
//import org.opencv.core.Mat;
//import org.opencv.highgui.HighGui;
//import org.opencv.videoio.VideoCapture;
//
//import java.io.ObjectInputStream;
//import java.io.ObjectOutputSt ream;
//import java.net.ServerSocket;
//import java.net.Socket;
//
//public class Server {
//    static {
//        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
//    }
//
//    public static void main(String[] args) throws Exception {
//        ServerSocket serverSocket = new ServerSocket(1234);
//        System.out.println("Server is listening...");
//        Socket socket = serverSocket.accept();
//        System.out.println("Client connected.");
//
//        VideoCapture capture = new VideoCapture(0);  // Server's camera
//        if (!capture.isOpened()) {
//            System.out.println("ERROR: Cannot access camera.");
//            return;
//        }
//
//        ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
//        ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
//
//        Mat frame = new Mat();
//
//        // Start separate thread for receiving frames
//        new Thread(() -> {
//            try {
//                while (true) {
//                    // These variables will be used directly in the thread, no need for final/ effectively final
//                    int rows = objectInputStream.readInt();
//                    int cols = objectInputStream.readInt();
//                    int type = objectInputStream.readInt();
//                    byte[] frameData = (byte[]) objectInputStream.readObject();
//
//                    // Create a new Mat object inside the thread
//                    Mat receivedFrame = new Mat(rows, cols, type);
//                    receivedFrame.put(0, 0, frameData);
//                    HighGui.imshow("Client Camera Feed", receivedFrame);
//                    if (HighGui.waitKey(1) >= 0) break;
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }).start();
//
//        // Continuously capture and send frames
//        while (true) {
//            capture.read(frame);
//            if (frame.empty()) {
//                System.out.println("ERROR: Empty frame.");
//                break;
//            }
//
//            int rows = frame.rows();
//            int cols = frame.cols();
//            int type = frame.type();
//            byte[] buffer = new byte[(int) frame.total() * frame.channels()];
//            frame.get(0, 0, buffer);
//
//            objectOutputStream.writeInt(rows);
//            objectOutputStream.writeInt(cols);
//            objectOutputStream.writeInt(type);
//            objectOutputStream.writeObject(buffer);
//            objectOutputStream.flush();
//
//            HighGui.imshow("Server Camera Feed", frame);
//            if (HighGui.waitKey(1) >= 0) break;
//        }
//
//        capture.release();
//        socket.close();
//        serverSocket.close();
//    }
//}