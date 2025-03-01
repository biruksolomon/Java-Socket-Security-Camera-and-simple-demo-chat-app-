//
//
//import org.opencv.core.Core;
//import org.opencv.core.Mat;
//import org.opencv.highgui.HighGui;
//import org.opencv.videoio.VideoCapture;
//
//import java.io.ObjectInputStream;
//import java.io.ObjectOutputStream;
//import java.net.Socket;
//
//public class Client {
//    static {
//        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
//    }
//
//    public static void main(String[] args) throws Exception {
//        Socket socket = new Socket("192.168.1.7", 1234);  // Replace with your server's IP address
//        VideoCapture capture = new VideoCapture(0); // Client's camera
//        if (!capture.isOpened()) {
//            System.out.println("ERROR: Cannot access camera.");
//            return;
//        }
//
//        ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
//        ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
//
//        Mat frame = new Mat();
//
//        // Start separate thread for receiving frames
//        new Thread(() -> {
//            try {
//                while (true) {
//                    int rows = objectInputStream.readInt();
//                    int cols = objectInputStream.readInt();
//                    int type = objectInputStream.readInt();
//                    byte[] frameData = (byte[]) objectInputStream.readObject();
//
//                    // Create a new Mat inside the thread to avoid modifying external state
//                    Mat receivedFrame = new Mat(rows, cols, type);
//                    receivedFrame.put(0, 0, frameData);
//                    HighGui.imshow("Server Camera Feed", receivedFrame);
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
//            HighGui.imshow("Client Camera Feed", frame);
//            if (HighGui.waitKey(1) >= 0) break;
//        }
//
//        capture.release();
//        socket.close();
//    }
//}