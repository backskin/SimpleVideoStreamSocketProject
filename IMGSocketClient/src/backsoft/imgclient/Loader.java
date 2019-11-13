package backsoft.imgclient;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.io.*;

class Loader {

    private static Stage primaryStage;
    static Stage getStage(){
        return primaryStage;
    }

    static <T> Pair<Parent, T> loadFXML(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(Loader.class.getResource(fxml + ".fxml"));
            return new Pair<>(loader.load(), loader.getController());
        } catch (IOException e) {
            AlertHandler.makeError("Системная ошибка при загрузке FXML");
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    static void openInAWindow(Stage stage, Parent parent){
        if (primaryStage == null) primaryStage = stage;
        stage.setResizable(false);
        stage.setScene(new Scene(parent));
        stage.show();
        stage.setMinHeight(stage.getHeight());
        stage.setMinWidth(stage.getWidth());
    }

    static ImageView convertToFxImage(BufferedImage image) {
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
}
