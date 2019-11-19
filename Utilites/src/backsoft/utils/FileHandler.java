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

    public static byte[] readBytesFromBase64(String stopWord, DataInputStream in, IntegerProperty chunks) throws IOException {

        StringBuilder imgAsString = new StringBuilder();
        String utf = in.readUTF();
        while (!utf.equals(stopWord)){
            if (chunks != null)
                Platform.runLater(()->chunks.setValue(chunks.getValue()+1));
            imgAsString.append(utf);
            utf = in.readUTF();
        }
        if (chunks != null) Platform.runLater(()->chunks.setValue(0));
        return Base64.getDecoder().decode(imgAsString.toString());
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

    public static void sendBytesByBase64(String stopWord, byte[] bytes, DataOutputStream out) throws IOException{

        String imgAsString = Base64.getEncoder().encodeToString(bytes);

        int chunkSize = 128;
        while (!"".equals(imgAsString)){
            int endIndex = Math.min(chunkSize, imgAsString.length());
            out.writeUTF(imgAsString.substring(0,endIndex));
            imgAsString = imgAsString.substring(endIndex);
            out.flush();
        }
        out.writeUTF(stopWord);
        out.flush();
    }
}
