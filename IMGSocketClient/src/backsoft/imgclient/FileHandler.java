package backsoft.imgclient;

import javafx.stage.FileChooser;

import java.io.*;
import java.nio.file.Files;

class FileHandler {

    static Pair<byte[], String> openFileAsByteArray(){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose a file");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File file = fileChooser.showOpenDialog(Loader.getStage());
        if (file == null) return null;
        try {
            return new Pair<>(Files.readAllBytes(file.toPath()), file.getAbsolutePath());
        } catch (IOException e){
            AlertHandler.makeError("Системная ошибка при чтении с файла");
            e.printStackTrace();
        }
        return null;
    }
}
