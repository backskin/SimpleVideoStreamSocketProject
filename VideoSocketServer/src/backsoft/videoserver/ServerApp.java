package backsoft.videoserver;

import backsoft.utils.Loader;
import backsoft.utils.Pair;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.stage.Stage;

public class ServerApp extends Application {

    @Override
    public void start(Stage primaryStage) {

        primaryStage.setTitle("VideoSocketServer");
        Pair<Parent, Controller> fxmlData = Loader.loadFXML(ServerApp.class.getResource("server.fxml"));
        fxmlData.getTwo().setStage(primaryStage);
        Loader.openInAWindow(primaryStage, fxmlData.getOne(), false);
        primaryStage.setOnCloseRequest(event -> {
            fxmlData.getTwo().handleCloseButton();
            System.exit(0);
        });
    }


    public static void main(String[] args) {
        launch(args);
    }
}
