package chat_application;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;

public class ClientSide extends Application {

    private TextArea chatArea; // To display messages
    private TextField inputField; // To type messages
    private Button sendButton; // To send messages
    private Button fileButton; // To send files
    private Socket socket;
    private DataOutputStream dataOutput;
    private DataInputStream dataInput;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("This is the Clinet Side");

        // UI Components
        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);

        inputField = new TextField();
        inputField.setPromptText("Type a message...");

        sendButton = new Button("Send");
        fileButton = new Button("Send File");

        // Layout
        HBox inputBox = new HBox(10, inputField, sendButton, fileButton);
        inputBox.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setCenter(chatArea);
        root.setBottom(inputBox);

        Scene scene = new Scene(root, 500, 400);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Connect to the server
        connectToServer();

        // Event Handlers
        sendButton.setOnAction(e -> sendMessage());
        fileButton.setOnAction(e -> sendFile());
        inputField.setOnAction(e -> sendMessage());
    }

    private void connectToServer() {
        String serverAddress = "192.168.43.183";
        int port = 3000;

        try {
            socket = new Socket(serverAddress, port);
            dataOutput = new DataOutputStream(socket.getOutputStream());
            dataInput = new DataInputStream(socket.getInputStream());

            // Start a thread to listen for server messages and files
            new Thread(this::listenForServerMessages).start();

            Platform.runLater(() -> chatArea.appendText("Connected to server.\n"));
        } catch (IOException ex) {
            ex.printStackTrace();
            Platform.runLater(() -> chatArea.appendText("Failed to connect to server.\n"));
        }
    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            try {
                dataOutput.writeUTF("MESSAGE:" + message);
                dataOutput.flush(); // Ensure the message is sent immediately
                Platform.runLater(() -> chatArea.appendText("You: " + message + "\n"));
                inputField.clear();
            } catch (IOException ex) {
                ex.printStackTrace();
                Platform.runLater(() -> chatArea.appendText("Failed to send message.\n"));
            }
        }
    }

    private void sendFile() {
        FileChooser fileChooser = new FileChooser();
        File selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile != null) {
            try (FileInputStream fileInput = new FileInputStream(selectedFile)) {
                // Send file metadata
                dataOutput.writeUTF("FILE:" + selectedFile.getName());
                dataOutput.writeLong(selectedFile.length());
                dataOutput.flush(); // Ensure metadata is sent immediately

                // Send file data
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileInput.read(buffer)) != -1) {
                    dataOutput.write(buffer, 0, bytesRead);
                }
                dataOutput.flush(); // Ensure file data is sent immediately

                Platform.runLater(() -> chatArea.appendText("You sent a file: " + selectedFile.getName() + "\n"));
            } catch (IOException ex) {
                ex.printStackTrace();
                Platform.runLater(() -> chatArea.appendText("Failed send.\n"));
            }
        }
    }

    private void listenForServerMessages() {
        try {
            while (true) {
                String requestType = dataInput.readUTF();

                if (requestType.startsWith("MESSAGE:")) {
                    // Handle incoming message
                    String message = requestType.substring(8);
                    Platform.runLater(() -> chatArea.appendText("Server: " + message + "\n"));
                } else if (requestType.startsWith("FILE:")) {
                    // Handle incoming file
                    String fileName = requestType.substring(5);
                    long fileSize = dataInput.readLong();

                    // Save the file to the client's machine
                    FileOutputStream fileOutput = new FileOutputStream("client_received_" + fileName);
                    byte[] buffer = new byte[4096];
                    int bytesRead;

                    while (fileSize > 0 && (bytesRead = dataInput.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {
                        fileOutput.write(buffer, 0, bytesRead);
                        fileSize -= bytesRead;
                    }

                    fileOutput.close();
                    Platform.runLater(() -> chatArea.appendText("File received from server: " + fileName + "\n"));
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            Platform.runLater(() -> chatArea.appendText("Disconnected from server.\n"));
        }
    }

    @Override
    public void stop() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
    
    
    
    
    
    
    
//    public static void main(String[] args) {
//        String serverAddress = "localhost"; // Server ka address (Agar same PC par ho to "localhost" ya "127.0.0.1" use karo)
//        int port = 4000; // Server ka port
//
//        try (Socket socket = new Socket(serverAddress, port);
//             PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
//             BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))) {
//
//            System.out.println("Connected to server. Type your messages:");
//
//            String message;
//            while (true) {
//                System.out.print("You: ");
//                message = userInput.readLine(); // User se input lega
//                
//                if (message.equalsIgnoreCase("exit")) { // Agar user "exit" likhe to connection close ho jayega
//                    break;
//                }
//
//                output.println(message); // Message server ko bhejega
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }










































//import java.io.*;
//import java.net.*;
//
//public class ClientSide {
//    public static void main(String[] args) {
//        String serverAddress = "localhost"; // Server ka address (Agar same PC par ho to "localhost" ya "127.0.0.1" use karo)
//        int port = 4000; // Server ka port
//
//        try (Socket socket = new Socket(serverAddress, port);
//             PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
//             BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))) {
//
//            System.out.println("Connected to server. Type your messages:");
//
//            String message;
//            while (true) {
//                System.out.print("You: ");
//                message = userInput.readLine(); // User se input lega
//                
//                if (message.equalsIgnoreCase("exit")) { // Agar user "exit" likhe to connection close ho jayega
//                    break;
//                }
//
//                output.println(message); // Message server ko bhejega
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//}
