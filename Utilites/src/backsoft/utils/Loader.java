package backsoft.utils;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;

public class Loader {

    private static Stage mainStage;

    public static Stage getMainStage() {
        return mainStage;
    }

    public static void setMainStage(Stage mainStage) {
        Loader.mainStage = mainStage;
    }

    public static <T> Pair<Parent, T> loadFXML(URL url) {
        try {
            FXMLLoader loader = new FXMLLoader(url);
            return new Pair<>(loader.load(), loader.getController());
        } catch (IOException e) {
            AlertHandler.makeError("Системная ошибка при загрузке FXML", null);
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    public static<T> Parent loadChildrenFXML(URL url, T superController) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setController(superController);
            loader.setLocation(url);
            return loader.load();
        } catch (IOException e) {
            AlertHandler.makeError("Системная ошибка при загрузке FXML", null);
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    public static void openInAWindow(Stage stage, Scene scene, boolean resizable){

        stage.setResizable(resizable);
        stage.setScene(scene);
        stage.show();
        stage.setMinHeight(stage.getHeight());
        stage.setMinWidth(stage.getWidth());
    }

    public static synchronized void showImageInAWindow(String imgName, BufferedImage bImage, EventHandler<ActionEvent> saveEvent){

        ImageView imageView = new ImageView(SwingFXUtils.toFXImage(bImage, null));
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(800);
        imageView.setFitHeight(540);
        Button closeButton = new Button("Закрыть");
        Button saveButton = new Button("Сохранить как");

        HBox buttonsPane = new HBox();
        buttonsPane.setSpacing(20);
        buttonsPane.getChildren().addAll(saveButton, closeButton);
        buttonsPane.setAlignment(Pos.CENTER);
        VBox vboxPane = new VBox();
        vboxPane.getChildren().addAll(imageView, buttonsPane);
        VBox.setMargin(imageView, new Insets(16,16,16,16));

        Stage imageStage = new Stage();
        imageStage.setTitle(imgName);
        saveButton.setOnAction(saveEvent);
        closeButton.setOnAction(event -> imageStage.close());

        openInAWindow(imageStage, new Scene(vboxPane), false);
    }
}
