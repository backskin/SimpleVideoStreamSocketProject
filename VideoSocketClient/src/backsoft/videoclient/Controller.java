package backsoft.videoclient;

import backsoft.utils.AlertHandler;
import backsoft.utils.Loader;
import backsoft.utils.Streamer;
import backsoft.utils.Streamer.StreamerBuilder;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.rmi.UnknownHostException;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;

import static backsoft.utils.CommonPhrases.SIGNAL.*;
import static backsoft.utils.CommonPhrases.byteFileSignal;
import static backsoft.utils.CommonPhrases.system;
import static backsoft.utils.FileHandler.*;
import static backsoft.utils.Streamer.convertToBuffImage;
import static backsoft.utils.Streamer.convertToFxImage;

public class Controller {

    private Socket socket;
    private File imageFile;
    private Thread clientThread;
    private Streamer streamer;

    private Stage stage;
    private Scene mainScene;
    private Scene videoScene;
    private Scene imageScene;

    private File videoFile;

    void setStageAndScene(Stage stage, Scene scene){
        this.stage = stage;
        mainScene = scene;
        videoScene = new Scene(Loader.loadChildrenFXML(
                this.getClass().getResource("videoWindow.fxml"), this));

        imageScene = new Scene(Loader.loadChildrenFXML(
                this.getClass().getResource("imageWindow.fxml"), this));
        offlineMode(true);
    }

    @FXML
    private Button sendButton;
    @FXML
    private Button videoButton;
    @FXML
    private Button photoButton;
    @FXML
    private TextField imagePathField;
    @FXML
    private TextField videoPathField;
    @FXML
    private TextField ipField;
    @FXML
    private TextField portField;
    @FXML
    private TextArea consoleArea;
    @FXML
    private ImageView imageView;
    @FXML
    private MediaView videoView;
    @FXML
    private Button connectButton;
    @FXML
    private Button disconnectButton;
    @FXML
    private Button chooseImageButton;
    @FXML
    private Button chooseVideoButton;
    @FXML
    private Button playPauseButton;
    @FXML
    private Button stopVideoButton;
    @FXML
    private Slider volumeSlider;

    private void connectToServer(String address, int port) {

        writeToConsole("Подключение к серверу\n" + address + ":" + port);
        Runnable r = () -> {
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(address, port), 3500);
                if (socket.isConnected()) {
                        writeToConsole("Соединение установлено!");
                        offlineMode(false);
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

    private void offlineMode(boolean status){
        connectButton.setDisable(!status);
        disconnectButton.setDisable(status);
        sendButton.setDisable(status);
        chooseImageButton.setDisable(status);
        chooseVideoButton.setDisable(status);
        imagePathField.setDisable(status);
        videoPathField.setDisable(status);
        playPauseButton.setDisable(status);
        stopVideoButton.setDisable(status);

        imageView.setImage(null);
    }

    private void blockImageScreenWhileSending(boolean block){
        sendButton.setDisable(block);
        chooseImageButton.setDisable(block);
        imagePathField.setDisable(block);
    }

    private void sendImageToServer() {

        Platform.runLater(()-> blockImageScreenWhileSending(true));

        writeToConsole("Отправление изображения на сервер...");
        StreamerBuilder builder = new StreamerBuilder();
        try {
            builder.setOutputStream(socket.getOutputStream())
                    .setFileToStream(imageFile)
                    .build()
                    .startFileStreaming();

        } catch (IOException e) {
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
                    if (response.equals(byteFileSignal.get(CORRECT))) {
                        blockImageScreenWhileSending(false);
                        writeToConsole("Сервер успешно получил изображение без потерь");
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

        streamer.stopVideoStreaming();

        try {
            if (clientThread != null && clientThread.isAlive())
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
        offlineMode(true);
    }

    @FXML
    private void handleChooseImage() {
       imageFile = openFile(stage);
       if (imageFile != null) {
           imagePathField.setText(imageFile.getAbsolutePath());
           if (socket != null && socket.isConnected()) sendButton.setDisable(false);
           Image preview = convertToFxImage(convertToBuffImage(readBytes(imageFile)));
           if (preview != null){ imageView.setImage(preview);}
       }
    }

    @FXML
    private void handleChooseVideo(){
        videoFile = openVideoLink(stage);
        if (videoFile != null){
            try {
                videoView.setMediaPlayer(
                        new MediaPlayer(
                                new Media(
                                        videoFile.toURI().toURL().toExternalForm()
                                )
                        )
                );
                videoView.getMediaPlayer().volumeProperty().bind(volumeSlider.valueProperty());
                videoPathField.setText(videoFile.getAbsolutePath());
                streamer = (new StreamerBuilder())
                        .setOutputStream(socket.getOutputStream())
                        .setVideoToStream(videoFile)
                        .build();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleSendButton() {

        Thread sendThread = new Thread(this::sendImageToServer);
        sendThread.start();
    }

    @FXML
    private void handlePlayPause(){
        MediaPlayer.Status playerStatus = videoView.getMediaPlayer().getStatus();
        if (playerStatus != MediaPlayer.Status.PLAYING) {
            videoView.getMediaPlayer().play();
            streamer.startVideoStreaming();
        }
        else {
            videoView.getMediaPlayer().pause();
            streamer.pauseVideoStreaming();
        }
    }

    @FXML
    private void handleStopVideo(){
        if (videoView.getMediaPlayer().getStatus() != MediaPlayer.Status.STOPPED) {
            videoView.getMediaPlayer().stop();
            streamer.stopVideoStreaming();
        }
    }

    @FXML
    public void handlePhotoButton() {

        stage.setScene(imageScene);
        stage.sizeToScene();
    }

    @FXML
    public void handleVideoButton() {

        stage.setScene(videoScene);
        stage.sizeToScene();
    }

    @FXML
    public void handleBackToMain() {

        stage.setScene(mainScene);
        stage.sizeToScene();
    }
}
