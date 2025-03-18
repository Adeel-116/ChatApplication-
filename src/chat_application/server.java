package chat_application;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class server extends Application {

    private TextArea chatArea; // To display messages and logs
    private TextField inputField; // To type messages
    private Button sendButton; // To send messages
    private Button fileButton; // To send files
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private DataOutputStream dataOutput;
    private DataInputStream dataInput;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Server UI");

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

        // Start the server
        startServer();

        // Event Handlers
        sendButton.setOnAction(e -> sendMessage());
        fileButton.setOnAction(e -> sendFile());
        inputField.setOnAction(e -> sendMessage());
    }

    private void startServer() {
        int port = 1265;
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                Platform.runLater(() -> chatArea.appendText("Server is listening on port " + port + "\n"));

                while (true) {
                    clientSocket = serverSocket.accept();
                    Platform.runLater(() -> chatArea.appendText("New client connected.\n"));

                    dataInput = new DataInputStream(clientSocket.getInputStream());
                    dataOutput = new DataOutputStream(clientSocket.getOutputStream());

                    // Handle client communication in a separate thread
                    new Thread(this::handleClient).start();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                Platform.runLater(() -> chatArea.appendText("Server error: " + ex.getMessage() + "\n"));
            }
        }).start();
    }

    private void handleClient() {
        try {
            while (true) {
                String requestType = dataInput.readUTF();

                if (requestType.startsWith("MESSAGE:")) {
                    // Handle message
                    String message = requestType.substring(8);
                    Platform.runLater(() -> chatArea.appendText("Client: " + message + "\n"));

                    // Send a response back to the client
                    dataOutput.writeUTF("Server: Message received - " + message);
                } else if (requestType.startsWith("FILE:")) {
                    // Handle file
                    String fileName = requestType.substring(5);
                    long fileSize = dataInput.readLong();

                    // Save the file to the server
                    FileOutputStream fileOutput = new FileOutputStream("server_" + fileName);
                    byte[] buffer = new byte[4096];
                    int bytesRead;

                    while (fileSize > 0 && (bytesRead = dataInput.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {
                        fileOutput.write(buffer, 0, bytesRead);
                        fileSize -= bytesRead;
                    }

                    fileOutput.close();
                    Platform.runLater(() -> chatArea.appendText("File received: " + fileName + "\n"));

                    // Send a response back to the client
                    dataOutput.writeUTF("Server: File received - " + fileName);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            Platform.runLater(() -> chatArea.appendText("Client disconnected.\n"));
        }
    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            try {
                dataOutput.writeUTF("MESSAGE:" + message);
                dataOutput.flush();
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

                // Send file data
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileInput.read(buffer)) != -1) {
                    dataOutput.write(buffer, 0, bytesRead);
                }

                Platform.runLater(() -> chatArea.appendText("You sent a file: " + selectedFile.getName() + "\n"));
            } catch (IOException ex) {
                ex.printStackTrace();
                Platform.runLater(() -> chatArea.appendText("Failed to send file.\n"));
            }
        }
    }

    @Override
    public void stop() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            if (clientSocket != null) {
                clientSocket.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}