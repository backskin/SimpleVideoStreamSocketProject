package backsoft.videoserver;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;

public class ClientStreamWindow {

    public Label extraStatsLabel;
    @FXML
    private ImageView streamImageView;
    @FXML
    private Label statsLabel;

    public void showImage(BufferedImage image, String title){
        Platform.runLater(()->{
            streamImageView.setOpacity(1.0);
            streamImageView.setImage(SwingFXUtils.toFXImage(image, null));
            streamImageView.setFitHeight(300.0);
            streamImageView.setFitWidth(475.0);
            statsLabel.setText(title);
        });
    }
}
