package backsoft.imgserver;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public class AlertHandler {

    static void makeError(String content){
        Alert alert = new Alert(Alert.AlertType.ERROR,content, ButtonType.OK);
        alert.initOwner(Loader.getStage());
        alert.showAndWait();
    }

    static void makeInfo(String content){
        Alert alert = new Alert(Alert.AlertType.INFORMATION, content, ButtonType.OK);
        alert.initOwner(Loader.getStage());
        alert.showAndWait();
    }
}
