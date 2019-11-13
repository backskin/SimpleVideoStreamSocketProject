package backsoft.imgserver;

import javafx.application.Application;
import javafx.stage.Stage;

public class ServerApp extends Application {

    @Override
    public void start(Stage primaryStage) {

        primaryStage.setTitle("IMGSocketServer");
        Loader.openInAWindow(primaryStage, Loader.loadFXML("server").getOne(), false);
    }


    public static void main(String[] args) {
        launch(args);
    }
}
