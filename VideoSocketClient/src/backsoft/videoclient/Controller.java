package backsoft.videoclient;

import backsoft.utils.AlertHandler;
import backsoft.utils.Streamer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.rmi.UnknownHostException;
import java.util.Arrays;

import backsoft.utils.Pair;
import javafx.stage.Stage;

import static backsoft.utils.CommonPhrases.SIGNAL.*;
import static backsoft.utils.CommonPhrases.imageSignal;
import static backsoft.utils.CommonPhrases.system;
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
    @FXML
    private Button chooseImageButton;

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

    private void blockRightSide(boolean block){
        sendButton.setDisable(block);
        chooseImageButton.setDisable(block);
        pathField.setDisable(block);
    }

    private void sendImageToServer() {

        Platform.runLater(()->blockRightSide(true));

        try {
            DataOutputStream dout = new DataOutputStream(socket.getOutputStream());

            dout.writeUTF(imageSignal.get(START));
            dout.flush();
            Thread.sleep(10);

            dout.writeUTF(imageFile.getTwo().getName());
            dout.flush();
            Thread.sleep(50);

            int imageHASH = Arrays.hashCode(imageFile.getOne());
            String hash = Integer.toString(imageHASH);
            dout.writeUTF(hash);
            dout.flush();
            Thread.sleep(50);

            Streamer.sendBytesByBase64(imageSignal.get(STOP), imageFile.getOne(), dout);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private synchronized void writeToConsole(String data){
        Platform.runLater(()->consoleArea.appendText(data+"\n"));
    }

    private void startServerListener(){
        Runnable serverListenerRunnable = () -> {
            try {
                while (!socket.isClosed()) {
                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    String response = in.readUTF();
                    if (response.equals(system.get(BYEBYE))) {
                        writeToConsole("Сервер отключил вас :(");
                        in.close();
                        handleDisconnect();
                    }
                    if (response.equals(imageSignal.get(CORRECT))) {
                        blockRightSide(false);
                        AlertHandler.makeInfo(
                                "Изображение успешно доставлено!", stage);
                    }
                }
            } catch (IOException e) {
                Controller.this.writeToConsole("Связь с сервером прервана");
                handleDisconnect();
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
    public void handleDisconnect() {

        try {

            if (clientThread.isAlive())
                clientThread.interrupt();

            if (socket != null && !socket.isClosed() && socket.isConnected()) {

                DataOutputStream dout = new DataOutputStream(socket.getOutputStream());
                dout.writeUTF(system.get(BYEBYE));
                dout.flush();
                socket.getOutputStream().close();
                socket.close();
            }
            writeToConsole("Отключен от сервера");
        } catch (IOException e) {
            writeToConsole("Ошибка отсоединения:");
            writeToConsole(e.getLocalizedMessage());
        }
        connectButton.setDisable(false);
        disconnectButton.setDisable(true);
        sendButton.setDisable(true);
        chooseImageButton.setDisable(false);
    }

    @FXML
    private void handleChooseImage() {
       imageFile = openFile(stage);
       if (imageFile != null) {
           pathField.setText(imageFile.getTwo().getAbsolutePath());
           if (socket.isConnected()) sendButton.setDisable(false);
           Image preview = convertToFxImage(convertToBuffImage(imageFile.getOne()));
           if (preview != null){ imageView.setImage(preview);}
       }
    }

    @FXML
    private void handleSendButton() {

        Thread sendThread = new Thread(this::sendImageToServer);
        sendThread.start();
    }
}
