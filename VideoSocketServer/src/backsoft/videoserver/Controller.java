package backsoft.videoserver;

import backsoft.utils.FileHandler;
import backsoft.utils.Loader;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.beans.value.ObservableStringValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import javax.naming.ldap.SortKey;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.Socket;

import static backsoft.utils.AlertHandler.*;

public class Controller {

    @FXML
    private TableView<Socket> clientsTable;
    @FXML
    private TextField portField;
    @FXML
    private Button openButton;
    @FXML
    private TextArea consoleArea;
    @FXML
    private Button closeButton;
    private IntegerProperty chunks = new SimpleIntegerProperty(0);
    private Stage stage;

    public IntegerProperty chunksProperty() {
        return chunks;
    }


    void setStage(Stage stage){
        this.stage = stage;
    }

    private ServerRunnable runnable;
    private Thread serverThread;
    private BooleanProperty serverWorking = new SimpleBooleanProperty();

    @FXML
    private void initialize(){
        clientsTable.getColumns().get(0).setCellValueFactory(param -> new ObservableStringValue());
    }

    void putClientToTable(Socket client){
        Platform.runLater(()->{
            clientsTable.getItems().add(client);
        });
    }

    void removeClientFromTable(Socket client){
        Platform.runLater(()->{});
    }

    void setServerWorking(boolean serverWorking) {
        writeToConsole("Сервер " + (serverWorking ? "запущен!" : "остановлен"));
        handleServerStatus(serverWorking);
    }

    private void startServer(int port){

        runnable = new ServerRunnable(port,this);
        serverThread = new Thread(runnable);
        serverThread.start();
    }

    synchronized void writeToConsole(String data){
        Platform.runLater(()->consoleArea.appendText(data+"\n"));
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
