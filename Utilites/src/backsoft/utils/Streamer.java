package backsoft.utils;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

import static backsoft.utils.CommonPhrases.SIGNAL.*;
import static backsoft.utils.CommonPhrases.imageSignal;
import static backsoft.utils.CommonPhrases.videoSignal;
import static org.opencv.videoio.Videoio.*;

public class Streamer {

    static{ System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    public static class StreamerBuilder{

        private Streamer streamer;

        public StreamerBuilder(){
            streamer = new Streamer();
        }

        public StreamerBuilder setVideoToStream(File videoToStream){
            streamer.filePath = videoToStream.toPath();
            streamer.capture = new VideoCapture(videoToStream.getAbsolutePath());
            streamer.framesAmount  = (long) streamer.capture.get(CAP_PROP_FRAME_COUNT);
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

    public static byte[] readBytesFromBase64(String haltWord, String stopWord, DataInputStream in) throws IOException {

        String utf = "";
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            utf = in.readUTF();
            if (utf.equals(haltWord)) return null;
            while (!utf.equals(stopWord)){
                bout.write(Base64.getDecoder().decode(utf));
                utf = in.readUTF();
            }
            return bout.toByteArray();
        } catch (IllegalArgumentException iae){
            System.out.println(utf);
            System.out.println(in.available());
        }
      return null;
    }

    public void sendBytesByBase64(String stopWord, InputStream in, DataOutputStream out) throws IOException{

        int chunkSize = 64;

        while (true){
            byte[] chunk = new byte[chunkSize];
            int bytesRead = in.read(chunk);
            if (bytesRead < 0) break;
            if (bytesRead != chunkSize)
                chunk = Arrays.copyOf(chunk, bytesRead);
            out.writeUTF(Base64.getEncoder().encodeToString(chunk));
        }
        out.writeUTF(stopWord);
    }

    public static BufferedImage convertToBuffImage(byte[] imageInByte){

        InputStream in = new ByteArrayInputStream(imageInByte);
        BufferedImage bImageFromConvert = null;
        try {
            bImageFromConvert = ImageIO.read(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bImageFromConvert;
    }

    public static Image convertToFxImage(BufferedImage image) {
        return image == null ? null : SwingFXUtils.toFXImage(image, null);
    }

    static ByteArrayInputStream mat2BufferedImage(Mat image) {
        MatOfByte mob = new MatOfByte();
        Imgcodecs.imencode(".jpg", image, mob);
        return new ByteArrayInputStream(mob.toArray());
    }

    private VideoCapture capture = null;
    private DataOutputStream outputStream = null;
    private String dataName;
    private Path filePath;
    private long framesAmount;
    private long counter = 0;
    Thread streamThread;
    AtomicBoolean pauseFlag = new AtomicBoolean(false);
    Runnable streamRun = ()-> {
        while (true) {
            if (!pauseFlag.get()) {
                try {
                    if (counter < framesAmount) {
                        if (pauseFlag.get()) continue;
                        outputStream.writeUTF(videoSignal.get(PLAY));

                        counter++;
                        Mat frame = new Mat();
                        capture.read(frame);
                        if (pauseFlag.get()) continue;
                        sendBytesByBase64(videoSignal.get(NEXT), mat2BufferedImage(frame), outputStream);

                    } else {
                        if (pauseFlag.get()) continue;
                        outputStream.writeUTF(videoSignal.get(STOP));
                        break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };


    private Streamer(){}


    public void startFileStreaming(){

        try {
            outputStream.writeUTF(imageSignal.get(START));
            outputStream.writeUTF(dataName);
            int fileHASH = Arrays.hashCode(Files.readAllBytes(filePath));
            outputStream.writeUTF(Integer.toString(fileHASH));

            sendBytesByBase64(imageSignal.get(STOP), Files.newInputStream(filePath), outputStream);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startVideoStreaming(){

        if (pauseFlag.get()){ pauseFlag.set(false);return;}
        try {
            outputStream.writeUTF(videoSignal.get(START));
            outputStream.writeUTF(dataName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        streamThread = new Thread(streamRun);
        streamThread.start();
    }

    public void pauseVideoStreaming(){
        System.out.println("client pause stream");
        pauseFlag.set(true);
    }

    public void stopVideoStreaming(){
        pauseFlag.set(true);
        System.out.println("client stopped stream");
        capture = new VideoCapture(filePath.toAbsolutePath().toString());
        if (streamThread != null && streamThread.isAlive()) {
            streamThread.interrupt();
            try {
                outputStream.writeUTF(videoSignal.get(STOP));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        pauseFlag.set(false);
    }
}
