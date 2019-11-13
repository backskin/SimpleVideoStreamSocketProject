package backsoft.imgclient;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.rmi.UnknownHostException;
import java.util.Base64;

public class Controller {

    private Socket socket;
    private Pair<byte[], File> imageFile;

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


    private boolean connectToServer(String address, int port){

        writeToConsole("Подключение к серверу\nIP: "+address+"\nport: " + port);

        try {
            socket = new Socket(address, port);

            return socket.isConnected();

        }catch (UnknownHostException e){
            writeToConsole("Неизвестный хост!");
            writeToConsole(e.getLocalizedMessage());
        }
        catch (IOException e) {
            writeToConsole("Ошибка при подключении к серверу");
            writeToConsole(e.getLocalizedMessage());
        }
        return false;
    }

    private void sendToServer(byte[] content, String name) {

        try {
            DataOutputStream dout = new DataOutputStream(socket.getOutputStream());

            dout.writeUTF("image");
            dout.flush();
            Thread.sleep(10);

            dout.writeUTF(name);
            dout.flush();

            String imgAsString = Base64.getEncoder().encodeToString(content);

            int chunkSize = 4096;
            int rounds =  (int)Math.ceil(imgAsString.length() / (double)chunkSize);
            for (int i = 0; i < rounds; i++) {
                dout.writeUTF(imgAsString.substring(chunkSize * i, Math.min(chunkSize*(i+1), imgAsString.length())));
                dout.flush();
            }
            dout.writeUTF("image-end");
            dout.flush();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void writeToConsole(String data){
        if (!consoleArea.getText().isEmpty())
            consoleArea.appendText("\n");
        consoleArea.appendText(data);
    }

    @FXML
    private void handleConnect() {

        try {

            String address = ipField.getText();
            int port = Integer.parseInt(portField.getText());

            if (connectToServer(address,port)) {
                writeToConsole("Соединение установлено!");
                connectButton.setDisable(true);
                disconnectButton.setDisable(false);
                if (imageFile != null) sendButton.setDisable(false);

                Task connectTask = new Task() {
                    @Override
                    protected Object call() throws Exception {

                        DataInputStream in = new DataInputStream(socket.getInputStream());
                        String respond = in.readUTF();
                        if (respond.equals("close")){
                            socket.close();
                            Platform.runLater(()->{
                                writeToConsole("Связь с сервером прервана");
                                handleDisconnect();
                            });
                        }
                        if (respond.equals("gotit"))
                            Platform.runLater(()->AlertHandler.makeInfo(
                                    "Изображение успешно доставлено!"));
                        return respond;
                    }
                };

                Thread clientThread = new Thread(() -> {
                    connectTask.run();
                    while (!socket.isClosed()) {
                        if (connectTask.isDone()) connectTask.run();
                    }
                    connectTask.cancel();
                });
                clientThread.start();
            }
            else {
                socket.close();
                writeToConsole("Подключение не удалось.");
            }
        } catch (NumberFormatException e){
            AlertHandler.makeError("Ошибка ввода!");
        } catch (Exception e){
            AlertHandler.makeError("Ошибка подключения!");
        }
    }



    @FXML
    private void handleDisconnect() {

        try {
            if (!socket.isClosed()) {
                DataOutputStream dout = new DataOutputStream(socket.getOutputStream());
                dout.writeUTF("quit");
                dout.flush();
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

    private static BufferedImage convertToBuffImage(byte[] imageInByte){

        InputStream in = new ByteArrayInputStream(imageInByte);
        BufferedImage bImageFromConvert = null;
        try {
            bImageFromConvert = ImageIO.read(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bImageFromConvert;
    }

    @FXML
    private void handleChooseImage() {
       imageFile = FileHandler.openFileAsByteArray();
       if (imageFile != null) {
           pathField.setText(imageFile.getTwo().getAbsolutePath());
           imageView.setImage(Loader.convertToFxImage(convertToBuffImage(imageFile.getOne())).getImage());
           if (socket.isConnected()) sendButton.setDisable(false);
       }
    }

    @FXML
    private void handleSendButton() {

        Task sendTask = new Task() {
            @Override
            protected Object call() {
                sendToServer(imageFile.getOne(), imageFile.getTwo().getName());
                return null;
            }
        };
        sendTask.run();
    }
}
