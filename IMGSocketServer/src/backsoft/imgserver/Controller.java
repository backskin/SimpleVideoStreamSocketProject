package backsoft.imgserver;

import backsoft.utils.FileHandler;
import backsoft.utils.Loader;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static backsoft.utils.AlertHandler.*;

public class Controller {

    @FXML
    private TextField portField;
    @FXML
    private Button openButton;
    @FXML
    private TextArea consoleArea;
    @FXML
    private Button closeButton;
    @FXML
    private TextField chunksField;
    private IntegerProperty chunks = new SimpleIntegerProperty(0);
    private Stage stage;

    public IntegerProperty chunksProperty() {
        return chunks;
    }

    public void visualiseChunksField(boolean visible){
        Platform.runLater(()->chunksField.setVisible(visible));
    }

    void setStage(Stage stage){
        this.stage = stage;
    }

    private ServerRunnable runnable;
    private Thread serverThread;
    private BooleanProperty serverWorking = new SimpleBooleanProperty();

    @FXML
    private void initialize(){
        chunksField.textProperty().bind(chunks.asString());
        serverWorking.addListener((o, old, newVal) -> handleServerStatus(newVal));
    }

    void setServerWorking(boolean serverWorking) {
        writeToConsole("Сервер " + (serverWorking ? "запущен!" : "остановлен"));
        this.serverWorking.set(serverWorking);
    }

    private void startServer(int port){

        runnable = new ServerRunnable(port,this);
        serverThread = new Thread(runnable);
        serverThread.start();
    }

    synchronized void writeToConsole(String data){
        Platform.runLater(()->consoleArea.appendText("\n"+data));
    }

    private synchronized void handleServerStatus(boolean enable){
        Platform.runLater(()->{
            closeButton.setDisable(!enable);
            openButton.setDisable(enable);});
    }

    @FXML
    public void handleCloseButton() {

        writeToConsole("Попытка завершения работы сервера");
        if (serverThread != null){
            runnable.stop();
            serverThread.interrupt();
        }
    }

    void showImage(String imageName, BufferedImage image, byte[] source){
        Platform.runLater(()->{
            String path = "images"+File.separator+imageName;
            EventHandler<ActionEvent> handler = event -> {
                try {
                    FileHandler.saveFileAs(path, source, stage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            };
            Loader.showImageInAWindow(imageName, image, handler);
        });
    }

    @FXML
    private void handleOpenButton() {

        try {
            int port = Integer.parseInt(portField.getText());
            writeToConsole("Попытка открыть порт для прослушивания");
            startServer(port);
            handleServerStatus(true);

        } catch (NumberFormatException e){
            makeError("Ошибка ввода (номер порта)!", stage);
        }
    }
}
