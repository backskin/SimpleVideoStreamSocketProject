package backsoft.imgserver;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;

public class Loader {

    private static Stage primaryStage;
    public static Stage getStage(){
        return primaryStage;
    }

    public static <T> Pair<Parent, T> loadFXML(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(Loader.class.getResource(fxml + ".fxml"));
            return new Pair<>(loader.load(), loader.getController());
        } catch (IOException e) {
            AlertHandler.makeError("Системная ошибка при загрузке FXML");
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    public static void openInAWindow(Stage stage, Parent parent, boolean resizable){
        if (primaryStage == null) primaryStage = stage;
        stage.setResizable(resizable);
        stage.setScene(new Scene(parent));
        stage.show();
        stage.setMinHeight(stage.getHeight());
        stage.setMinWidth(stage.getWidth());
    }

    public static ImageView convertToFxImage(BufferedImage image) {

        WritableImage wr = null;
        if (image != null) {
            wr = new WritableImage(image.getWidth(), image.getHeight());
            PixelWriter pixelWriter = wr.getPixelWriter();
            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    pixelWriter.setArgb(x, y, image.getRGB(x, y));
                }
            }
        }
        return new ImageView(wr);
    }

    public static synchronized void showImageInAWindow(BufferedImage bImage){

        ImageView imageView = convertToFxImage(bImage);

        Button closeButton = new Button("Закрыть");
        Button saveButton = new Button("Сохранить как");
        saveButton.setOnAction(event->{});
        closeButton.setOnAction(event -> {});
        HBox buttonsPane = new HBox();
        buttonsPane.getChildren().addAll(saveButton, closeButton);
        buttonsPane.setAlignment(Pos.CENTER);
        VBox vboxPane = new VBox();
        vboxPane.getChildren().addAll(imageView, buttonsPane);
        VBox.setMargin(imageView, new Insets(16,16,16,16));

        Stage imageStage = new Stage();
        imageStage.initModality(Modality.WINDOW_MODAL);
        imageStage.initOwner(primaryStage);

        openInAWindow(imageStage, vboxPane, false);
    }

    public static void saveImageTo(BufferedImage image, String imgname){

        try {
            ImageIO.write(image, "jpg", new File(imgname + ".jpg"));
        } catch (IOException e) {
            AlertHandler.makeError("Cистемная ошибка при загрузке изображения");
            e.printStackTrace();
            throw new RuntimeException();
        }
    }
}
