package backsoft.imgclient;

import backsoft.utils.AlertHandler;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.rmi.UnknownHostException;
import backsoft.utils.Pair;
import javafx.stage.Stage;

import static backsoft.utils.FileHandler.*;
import static backsoft.utils.Loader.*;

public class Controller {

    private Socket socket;
    private Pair<byte[], File> imageFile;
    private Thread clientThread;
    private Stage stage;
    void setStage(Stage stage){
        this.stage = stage;
    }

    @FXML
    private Button sendButton;
    @FXML
    private TextField pathField;
    @FXML
    private TextField ipField;
    @FXML
    private TextField portField;
    @FXML
    private TextArea consoleArea;
    @FXML
    private ImageView imageView;
    @FXML
    private Button connectButton;
    @FXML
    private Button disconnectButton;

    private void connectToServer(String address, int port) {

        writeToConsole("Подключение к серверу\n" + address + ":" + port);
        Runnable r = () -> {
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(address, port), 3500);
                if (socket.isConnected()) {
                        writeToConsole("Соединение установлено!");
                        disconnectButton.setDisable(false);
                        if (imageFile != null) sendButton.setDisable(false);
                        startServerListener();
                } else {
                    socket.close();
                        connectButton.setDisable(false);
                        writeToConsole("Подключение не удалось.");
                }

            } catch (UnknownHostException e) {
                connectButton.setDisable(false);
                writeToConsole("Неизвестный хост!\n" + e.getLocalizedMessage());
            } catch (IOException e) {
                connectButton.setDisable(false);
                writeToConsole("Ошибка при подключении к серверу\n" + e.getLocalizedMessage());
            }

        };
        Thread th = new Thread(r);
        th.start();
    }

    private void sendToServer() {

        try {
            DataOutputStream dout = new DataOutputStream(socket.getOutputStream());

            dout.writeUTF("image");
            dout.flush();
            Thread.sleep(10);

            dout.writeUTF(imageFile.getTwo().getName());
            dout.flush();

            sendBytesByBase64("image-end", imageFile.getOne(), dout);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private synchronized void writeToConsole(String data){
        Platform.runLater(()->consoleArea.appendText("\n"+data));
    }

    private void startServerListener(){
        Runnable serverListenerRunnable = () -> {
            try {
                while (!socket.isClosed()) {
                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    String respond = in.readUTF();
                    if (respond.equals("close")) {
                        writeToConsole("Сервер отключил вас :(");
                        in.close();
                        handleDisconnect();
                    }
                    if (respond.equals("gotit")) AlertHandler.makeInfo(
                            "Изображение успешно доставлено!", stage);
                }
            } catch (IOException e) {
                Controller.this.writeToConsole("Связь с сервером прервана");
            }
        };

        clientThread = new Thread(serverListenerRunnable);
        clientThread.start();
    }

    @FXML
    private void handleConnect() {

        try {
            String address = ipField.getText();
            int port = Integer.parseInt(portField.getText());
            connectButton.setDisable(true);
            connectToServer(address, port);

        } catch (NumberFormatException e){
            AlertHandler.makeError("Ошибка ввода порта!", stage);
        }
    }

    @FXML
    private void handleDisconnect() {

        try {
            if (!socket.isClosed()) {
                clientThread.interrupt();
                DataOutputStream dout = new DataOutputStream(socket.getOutputStream());
                dout.writeUTF("quit");
                dout.flush();
                socket.getOutputStream().close();
                socket.close();
            }
            writeToConsole("Отключен от сервера");
            connectButton.setDisable(false);
            disconnectButton.setDisable(true);
            sendButton.setDisable(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleChooseImage() {
       imageFile = openFile(stage);
       if (imageFile != null) {
           pathField.setText(imageFile.getTwo().getAbsolutePath());
           imageView.setImage(convertToFxImage(convertToBuffImage(imageFile.getOne())));
           if (socket.isConnected()) sendButton.setDisable(false);
       }
    }

    @FXML
    private void handleSendButton() {

        Thread sendThread = new Thread(this::sendToServer);
        sendThread.start();
    }
}
