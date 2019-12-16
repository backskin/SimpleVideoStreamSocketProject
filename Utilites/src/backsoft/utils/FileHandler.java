package backsoft.utils;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.nio.file.Files;

public class FileHandler {

    public static File lastDirectory = new File(System.getProperty("user.home"));

    public static File openVideoLink(Stage stage){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите видеофайл");
        fileChooser.setInitialDirectory(lastDirectory);
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("MP4 video", "*.mp4"),
                new FileChooser.ExtensionFilter("3GP video", "*.3gp"),
                new FileChooser.ExtensionFilter("MKV video", "*.mkv")
        );
        File out = fileChooser.showOpenDialog(stage);
        if (out != null) {
            lastDirectory = out.getParentFile();
            return out;
        }
        return null;
    }

    public static File openFile(Stage stage){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите файл для отправки");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        return fileChooser.showOpenDialog(stage);
    }

    public static byte[] readBytes(File file){
        try {
           return Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
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
