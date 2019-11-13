package backsoft.imgserver;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.IOException;


public class Controller {

    @FXML
    private TextField portField;
    @FXML
    private Button openButton;
    @FXML
    private TextArea consoleArea;
    @FXML
    private Button closeButton;

    private ServerRunnable runnable;
    private StringProperty newMessageToConsole = new SimpleStringProperty();
    private BooleanProperty serverWorking = new SimpleBooleanProperty();

    @FXML
    private void initialize(){
        newMessageToConsole.addListener((o, old, newVal) -> writeToConsole(newVal));
        serverWorking.addListener((o, old, newVal) -> handleServerStatus(newVal));
    }

    void setServerWorking(boolean serverWorking) {
        setNewMessageToConsole("Сервер отключен");
        this.serverWorking.set(serverWorking);
    }

    void setNewMessageToConsole(String newMessageToConsole) {
        this.newMessageToConsole.set(newMessageToConsole);
    }

    private void startServer(int port){

        runnable = new ServerRunnable(port,this);
        Thread serverThread = new Thread(runnable);
        serverThread.start();
    }


    private synchronized void writeToConsole(String data){
        if (!consoleArea.getText().isEmpty()) consoleArea.appendText("\n");
        consoleArea.appendText(data);
    }

    private synchronized void handleServerStatus(boolean disable){
        closeButton.setDisable(!disable);
        openButton.setDisable(disable);
    }

    @FXML
    private void handleCloseButton() {

        writeToConsole("Попытка завершения работы сервера");
        Platform.runLater(()-> {
            if (runnable.stop()) {
                Platform.runLater(()->{
                    openButton.setDisable(false);
                    closeButton.setDisable(true);
                    writeToConsole("Сервер успешно остановлен");
                });

            }
        });

    }
    @FXML
    private void handleOpenButton() {

        try {
            int port = Integer.parseInt(portField.getText());
            writeToConsole("Попытка открыть порт для прослушивания");

            Platform.runLater(()->startServer(port));
            handleServerStatus(true);

        } catch (NumberFormatException e){
            AlertHandler.makeError("Ошибка ввода (номер порта)!", Loader.getStage());
        }
    }
}
