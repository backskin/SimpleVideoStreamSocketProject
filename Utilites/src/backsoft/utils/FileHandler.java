package backsoft.utils;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.nio.file.Files;
import java.util.Base64;

public class FileHandler {

    public static Pair<byte[], File> openFile(Stage stage){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose a file");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File file = fileChooser.showOpenDialog(stage);
        try {
            return file == null ? null : new Pair<>(Files.readAllBytes(file.toPath()), file);
        } catch (IOException e){
            AlertHandler.makeError("Системная ошибка при чтении с файла", stage);
        }
        return null;
    }

    public static void saveToFile(String filepath, byte[] fileByte) throws IOException {

        File file = new File(filepath);
        if (!file.exists()) file.getParentFile().mkdir();

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(fileByte);
        }
    }

    public static void saveFileAs(String origPath, byte[] fileByte, Stage stage) throws IOException {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохраныт какъ...");
        File oldDir = new File(origPath);
        fileChooser.setInitialDirectory(oldDir.getParentFile());
        fileChooser.setInitialFileName(oldDir.getName());
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Файл ",
                origPath.substring(origPath.lastIndexOf("."))));
        File file = fileChooser.showSaveDialog(stage);

        saveToFile(file.getAbsolutePath(), fileByte);
    }
}
