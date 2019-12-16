package backsoft.videoclient;

import backsoft.utils.Loader;
import backsoft.utils.Pair;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientApp extends Application {

    @Override
    public void start(Stage primaryStage) {

        primaryStage.setTitle("Клиент стримера");

        Pair<Parent, Controller> fxmlData = Loader.loadFXML(this.getClass().getResource("mainWindow.fxml"));
        Scene mainScene = new Scene(fxmlData.getOne());
        fxmlData.getTwo().setStageAndScene(primaryStage, mainScene);
        primaryStage.setOnShowing(event -> primaryStage.sizeToScene());
        primaryStage.setOnCloseRequest(event->{
            fxmlData.getTwo().handleDisconnect();
            System.exit(0);
        });
        Loader.openInAWindow(primaryStage, mainScene, false);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
