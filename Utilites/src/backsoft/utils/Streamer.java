package backsoft.utils;

import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;

import static backsoft.utils.CommonPhrases.SIGNAL.START;
import static backsoft.utils.CommonPhrases.SIGNAL.STOP;
import static backsoft.utils.CommonPhrases.byteFileSignal;
import static org.opencv.videoio.Videoio.CAP_PROP_FPS;

public class Streamer {

    static{ System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    public static class StreamerBuilder{

        private Streamer streamer;

        public StreamerBuilder(){
            streamer = new Streamer();
        }

        public StreamerBuilder setVideoToStream(File videoToStream){
            streamer.capture = new VideoCapture(videoToStream.getAbsolutePath());
            streamer.frameRate = streamer.capture.get(CAP_PROP_FPS);
            streamer.dataName = videoToStream.getName();
            return this;
        }

        public StreamerBuilder setOutputStream(OutputStream outputStream){
            streamer.outputStream = new DataOutputStream(outputStream);
            return this;
        }

        public StreamerBuilder setFileToStream(File fileToStream){
            streamer.dataName = fileToStream.getName();
            streamer.filePath = fileToStream.toPath();
            return this;
        }

        public Streamer build(){
            return streamer;
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

    public void sendBytesByBase64(String stopWord, byte[] bytes, DataOutputStream out) throws IOException{

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

    private static BufferedImage matToBufferedImage(Mat frame) {
        int type = 0;
        if (frame.channels() == 1) {
            type = BufferedImage.TYPE_BYTE_GRAY;
        } else if (frame.channels() == 3) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        BufferedImage image = new BufferedImage(frame.width(), frame.height(), type);
        WritableRaster raster = image.getRaster();
        DataBufferByte dataBuffer = (DataBufferByte) raster.getDataBuffer();
        byte[] data = dataBuffer.getData();
        frame.get(0, 0, data);

        return image;
    }

    private VideoCapture capture = null;
    private double frameRate = 25;
    private DataOutputStream outputStream = null;
    private String dataName;
    private Path filePath;

    private Streamer(){}

    private Mat getNextFrame(){
        Mat mat = new Mat();
        capture.read(mat);
        return mat;
    }

    public void startFileStream(){

        try {

            byte[] data = Files.readAllBytes(filePath);

            outputStream.writeUTF(byteFileSignal.get(START));
            outputStream.flush();
            Thread.sleep(10);

            outputStream.writeUTF(dataName);
            outputStream.flush();
            Thread.sleep(50);

            int fileHASH = Arrays.hashCode(data);
            outputStream.writeUTF(Integer.toString(fileHASH));
            outputStream.flush();
            Thread.sleep(50);

            sendBytesByBase64(byteFileSignal.get(STOP), data, outputStream);

        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    public void startVideoStreaming(){
        System.out.println("client streaming");
    }

    public void pauseVideoStreaming(){
        System.out.println("client pause stream");
    }

    public void stopVideoStreaming(){
        System.out.println("client stopped stream");
    }
}
