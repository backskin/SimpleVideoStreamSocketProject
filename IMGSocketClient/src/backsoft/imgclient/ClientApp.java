package backsoft.imgclient;

import backsoft.utils.Loader;
import backsoft.utils.Pair;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.stage.Stage;

public class ClientApp extends Application {

    @Override
    public void start(Stage primaryStage) {

        primaryStage.setTitle("IMGSocketClient");
        primaryStage.setOnCloseRequest(event->System.exit(0));
        Pair<Parent, Controller> fxmlData = Loader.loadFXML(this.getClass().getResource("client.fxml"));
        fxmlData.getTwo().setStage(primaryStage);
        Loader.openInAWindow(primaryStage, fxmlData.getOne(), false);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
