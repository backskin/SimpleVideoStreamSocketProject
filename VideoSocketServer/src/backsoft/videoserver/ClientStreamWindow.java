package backsoft.videoserver;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class ClientStreamWindow {

    @FXML
    private StackPane framePane;
    @FXML
    private Label extraStatsLabel;
    @FXML
    private ImageView streamImageView;
    @FXML
    private Label statsLabel;

    public void setExtraStatsLabel(String content) {
        extraStatsLabel.setText(content);
    }

    public void setStreamFrame(Image frame) {
        streamImageView.setImage(frame);
        streamImageView.setFitHeight(Math.min(frame.getHeight(), framePane.getHeight()));
        streamImageView.setFitWidth(Math.min(frame.getWidth(), framePane.getWidth()));
    }

    public void setStatsLabel(String content) {
        Platform.runLater(()-> statsLabel.setText(content));
    }
}
