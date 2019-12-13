package backsoft.utils;

import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Base64;

public class Streamer {

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
