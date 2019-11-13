package backsoft.imgclient;

import javafx.application.Application;
import javafx.stage.Stage;

public class ClientApp extends Application {

    @Override
    public void start(Stage primaryStage) {

        primaryStage.setTitle("IMGSocketClient");
        Loader.openInAWindow(primaryStage, Loader.loadFXML("client").getOne());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
