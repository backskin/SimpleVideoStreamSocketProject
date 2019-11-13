package backsoft.imgserver;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

public class AlertHandler {

    static void makeError(String content, Stage owner){
        Alert alert = new Alert(Alert.AlertType.ERROR,content, ButtonType.OK);
        alert.initOwner(owner);
        alert.showAndWait();
    }

    static void makeInfo(String content, Stage owner){
        Alert alert = new Alert(Alert.AlertType.INFORMATION, content, ButtonType.OK);
        alert.initOwner(owner);
        alert.showAndWait();
    }
}
