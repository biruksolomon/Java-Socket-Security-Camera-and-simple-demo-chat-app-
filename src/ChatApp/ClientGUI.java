package ChatApp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class ClientGUI {
    private JFrame frame;
    private JTextArea messageArea;
    private JTextField inputField;
    private JButton sendButton, exitButton;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;

    public ClientGUI() {
        setupGUI();
        startClient();
    }

    private void setupGUI() {
        frame = new JFrame("Client");
        frame.setSize(500, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        messageArea = new JTextArea();
        messageArea.setEditable(false);
        messageArea.setFont(new Font("Arial", Font.PLAIN, 16));
        messageArea.setBackground(new Color(245, 245, 245));
        JScrollPane scrollPane = new JScrollPane(messageArea);

        inputField = new JTextField();
        inputField.setFont(new Font("Arial", Font.PLAIN, 16));
        sendButton = new JButton("Send");
        sendButton.setFont(new Font("Arial", Font.BOLD, 14));
        sendButton.setBackground(new Color(0, 123, 255));
        sendButton.setForeground(Color.WHITE);
        exitButton = new JButton("Exit");
        exitButton.setFont(new Font("Arial", Font.BOLD, 14));
        exitButton.setBackground(new Color(220, 53, 69));
        exitButton.setForeground(Color.WHITE);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(inputField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        buttonPanel.add(sendButton);
        buttonPanel.add(exitButton);
        inputPanel.add(buttonPanel, BorderLayout.EAST);

        frame.setLayout(new BorderLayout(10, 10));
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(inputPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exitClient();
            }
        });

        frame.setVisible(true);
    }

    private void sendMessage() {
        try {
            String message = inputField.getText().trim();
            if (!message.isEmpty()) {
                dataOutputStream.writeUTF(message);
                messageArea.append("You: " + message + "\n");
                inputField.setText("");
            }
        } catch (Exception e) {
            messageArea.append("Error sending message.\n");
        }
    }

    private void exitClient() {
        try {
            dataOutputStream.writeUTF("stop");
            frame.dispose();
            System.exit(0);
        } catch (Exception e) {
            System.exit(0);
        }
    }

    private void startClient() {
        try {
            Socket socket = new Socket("localhost", 1234);
            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());

            Thread receiveThread = new Thread(() -> {
                try {
                    String message;
                    while (!(message = dataInputStream.readUTF()).equals("stop")) {
                        messageArea.append("Server: " + message + "\n");
                    }
                    messageArea.append("Server has disconnected.\n");
                } catch (Exception e) {
                    messageArea.append("Connection closed.\n");
                }
            });
            receiveThread.start();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Unable to connect to server.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientGUI::new);
    }
}

//import java.io.DataInputStream;
//import java.io.DataOutputStream;
//import java.net.Socket;
//
//public class Client {
//    private DataInputStream dataInputStream;
//    private DataOutputStream dataOutputStream;
//
//    public void startClient() {
//        try {
//            System.out.println("Connecting to server...");
//            Socket socket = new Socket("localhost", 1234);
//            System.out.println("Connected to server.");
//
//            dataInputStream = new DataInputStream(socket.getInputStream());
//            dataOutputStream = new DataOutputStream(socket.getOutputStream());
//
//            Thread receiveThread = new Thread(() -> {
//                try {
//                    String message;
//                    while (!(message = dataInputStream.readUTF()).equals("stop")) {
//                        System.out.println("Server: " + message);
//                    }
//                    System.out.println("Server has disconnected.");
//                } catch (Exception e) {
//                    System.out.println("Connection closed.");
//                }
//            });
//            receiveThread.start();
//
//            // Simulating a send loop (replace with proper input mechanism as needed)
//            java.util.Scanner scanner = new java.util.Scanner(System.in);
//            String message;
//            while (!(message = scanner.nextLine()).equalsIgnoreCase("stop")) {
//                dataOutputStream.writeUTF(message);
//                System.out.println("You: " + message);
//            }
//
//            scanner.close();
//            dataOutputStream.writeUTF("stop");
//            socket.close();
//            System.out.println("Disconnected from server.");
//        } catch (Exception e) {
//            System.out.println("Unable to connect to server: " + e.getMessage());
//        }
//    }
//
//    public static void main(String[] args) {
//        new Client().startClient();
//    }
//}
