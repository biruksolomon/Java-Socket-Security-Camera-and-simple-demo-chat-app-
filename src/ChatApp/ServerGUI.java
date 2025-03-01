package ChatApp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerGUI {
    private JFrame frame;
    private JTextArea messageArea;
    private JTextField inputField;
    private JButton sendButton, exitButton;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;

    public ServerGUI() {
        setupGUI();
        startServer();
    }

    private void setupGUI() {
        frame = new JFrame("Server");
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
                exitServer();
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

    private void exitServer() {
        try {
            dataOutputStream.writeUTF("stop");
            frame.dispose();
            System.exit(0);
        } catch (Exception e) {
            System.exit(0);
        }
    }

    private void startServer() {
        try {
            ServerSocket serverSocket = new ServerSocket(1234);
            messageArea.append("Waiting for a client...\n");

            Socket client = serverSocket.accept();
            messageArea.append("Client connected.\n");

            dataInputStream = new DataInputStream(client.getInputStream());
            dataOutputStream = new DataOutputStream(client.getOutputStream());

            Thread receiveThread = new Thread(() -> {
                try {
                    String message;
                    while (!(message = dataInputStream.readUTF()).equals("stop")) {
                        messageArea.append("Client: " + message + "\n");
                    }
                    messageArea.append("Client has disconnected.\n");
                } catch (Exception e) {
                    messageArea.append("Connection closed.\n");
                }
            });
            receiveThread.start();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Error starting server.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ServerGUI::new);
    }
}
//import java.io.DataInputStream;
//import java.io.DataOutputStream;
//import java.net.ServerSocket;
//import java.net.Socket;

//public class Server {
//    private DataInputStream dataInputStream;
//    private DataOutputStream dataOutputStream;
//
//    public void startServer() {
//        try {
//            ServerSocket serverSocket = new ServerSocket(1234);
//            System.out.println("Waiting for a client...");
//
//            Socket client = serverSocket.accept();
//            System.out.println("Client connected.");
//
//            dataInputStream = new DataInputStream(client.getInputStream());
//            dataOutputStream = new DataOutputStream(client.getOutputStream());
//
//            String message;
//            while (!(message = dataInputStream.readUTF()).equals("stop")) {
//                System.out.println("Client: " + message);
//            }
//            System.out.println("Client has disconnected.");
//
//            client.close();
//            serverSocket.close();
//        } catch (Exception e) {
//            System.err.println("Error starting server: " + e.getMessage());
//        }
//    }
//
//    public static void main(String[] args) {
//        Server server = new Server();
//        server.startServer();
//    }
//}
