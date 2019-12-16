package backsoft.videoserver;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

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

    @FXML
    private void initialize(){
        clientsTable.getColumns().get(0).setCellValueFactory(new PropertyValueFactory<>("inetAddress"));
    }

    void putClientToTable(Socket client){
        Platform.runLater(()-> clientsTable.getItems().add(client));
    }

    void removeClientFromTable(Socket client){
        Platform.runLater(()-> clientsTable.getItems().remove(client));
    }

    private void startServer(int port){

        runnable = new ServerRunnable(port,this);
        serverThread = new Thread(runnable);
        serverThread.start();
    }

    synchronized void writeToConsole(String data){
        Platform.runLater(()->consoleArea.appendText(data+"\n"));
    }

    public synchronized void handleServerStatus(boolean enable){
        Platform.runLater(()->{
            writeToConsole("Сервер " + (enable ? "запущен!" : "остановлен"));
            closeButton.setDisable(!enable);
            openButton.setDisable(enable);});
        if (serverThread!= null && serverThread.isAlive())
            serverThread.interrupt();
    }

    @FXML
    public void handleCloseButton() {

        writeToConsole("Попытка завершения работы сервера");
        if (serverThread != null){
            runnable.stop();
            serverThread.interrupt();
        }
    }

    @FXML
    private void handleOpenButton() {

        try {
            int port = Integer.parseInt(portField.getText());
            writeToConsole("Попытка открыть порт для прослушивания");
            startServer(port);

        } catch (NumberFormatException e){
            makeError("Ошибка ввода (номер порта)!", stage);
        }
    }
}
