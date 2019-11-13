package backsoft.imgclient;

import javafx.application.Platform;
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

public class Controller {

    private Socket socket;
    private BufferedImage bufferedImage;
    private String ext;

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

    private boolean sendToServer(BufferedImage image, String extension){

//        Thread microThread = new Thread(()->{});
//        microThread.start();
        try {
            DataOutputStream dous = new DataOutputStream(socket.getOutputStream());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, extension, bos);
            byte[] imageAsByte = bos.toByteArray();
            dous.writeUTF("image");
            dous.flush();
            Thread.sleep(100);
            dous.write(imageAsByte.length);
            dous.flush();
            Thread.sleep(100);
            dous.write(imageAsByte);
            dous.flush();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return true;
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

            if (connectToServer(address,port)){
                writeToConsole("Соединение установлено!");

                connectButton.setDisable(true);
                disconnectButton.setDisable(false);
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
            DataOutputStream dous = new DataOutputStream(socket.getOutputStream());
            dous.writeUTF("quit");
            socket.close();
            connectButton.setDisable(false);
            disconnectButton.setDisable(true);
            sendButton.setDisable(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static BufferedImage convertToBuffImage(byte[] imageInByte){

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
       Pair<byte[], String> imageAndPath = FileHandler.openFileAsByteArray();
       if (imageAndPath != null) {
           pathField.setText(imageAndPath.getTwo());
           bufferedImage = convertToBuffImage(imageAndPath.getOne());
           ext = imageAndPath.getTwo()
                   .subSequence(imageAndPath.getTwo().lastIndexOf('.'),
                           imageAndPath.getTwo().length() - 1).toString();
           imageView.setImage(Loader.convertToFxImage(bufferedImage).getImage());
           sendButton.setDisable(false);
       }
    }

    @FXML
    private void handleSendButton() {

        try {
           if (sendToServer(bufferedImage, ext)){
               AlertHandler.makeInfo("Изображение успешно доставлено!");
           }
           else AlertHandler.makeInfo("Изображение не было доставлено :(");
        } catch (Exception e){
           AlertHandler.makeError("Ошибка при отправке!");
        }
    }
}
