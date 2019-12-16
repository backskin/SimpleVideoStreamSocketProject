package backsoft.utils;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.videoio.VideoCapture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.Timer;
import java.util.TimerTask;

import static backsoft.utils.CommonPhrases.SIGNAL.*;
import static backsoft.utils.CommonPhrases.byteFileSignal;
import static backsoft.utils.CommonPhrases.videoSignal;
import static org.opencv.videoio.Videoio.CAP_PROP_FPS;
import static org.opencv.videoio.Videoio.CAP_PROP_FRAME_COUNT;

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



    public static BufferedImage readAFrame(byte[] bytes, int cols, int rows) {

        MatOfByte matOfByte = new MatOfByte();
        matOfByte.fromArray(bytes);
        Mat mat = matOfByte.reshape(3, new int[]{cols, rows});
        return matToBufferedImage(mat);
    }

    public static byte[] readBytesFromBase64(String stopWord, DataInputStream in) throws IOException {

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        String utf = in.readUTF();
        while (!utf.equals(stopWord)){
            bout.write(Base64.getDecoder().decode(utf));
            utf = in.readUTF();
        }
        return bout.toByteArray();
    }

    public void sendBytesByBase64(String stopWord, InputStream in, DataOutputStream out) throws IOException{

        int chunkSize = 256;
        while (in.available() != 0){
            byte[] chunk = new byte[chunkSize];
            int bytesRead = in.read(chunk);
            String str;
            if (bytesRead != chunkSize)
                chunk = Arrays.copyOf(chunk, bytesRead);
            str = Base64.getEncoder().encodeToString(chunk);
            out.writeUTF(str);
            out.flush();
        }
        out.writeUTF(stopWord);
        out.flush();
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
    private long framesAmount;
    private long counter = 0;
    Timer timer = new Timer();

    private Streamer(){}

    private Mat getNextFrame(){
        Mat mat = new Mat();
        capture.read(mat);
        return mat;
    }

    public void startFileStreaming(){

        try {
            outputStream.writeUTF(byteFileSignal.get(START));
            outputStream.flush();
            outputStream.writeUTF(dataName);
            outputStream.flush();

            int fileHASH = Arrays.hashCode(Files.readAllBytes(filePath));
            outputStream.writeUTF(Integer.toString(fileHASH));
            outputStream.flush();

            sendBytesByBase64(byteFileSignal.get(STOP), Files.newInputStream(filePath), outputStream);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startVideoStreaming(){
        System.out.println("client streaming");
        System.out.println("framerate = " + frameRate);
        TimerTask streamTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    outputStream.writeUTF(videoSignal.get(START));
                    outputStream.writeUTF(dataName);
                    if (counter < framesAmount ) {

                        outputStream.writeUTF(videoSignal.get(PLAY));
                        counter++;

                        Mat frame = getNextFrame();
                        MatOfByte bytes = new MatOfByte(frame.reshape(1,
                                frame.cols() * frame.rows() * frame.channels()));
                        byte[] frameInBytes = bytes.toArray();

                        sendBytesByBase64(videoSignal.get(NEXT),new ByteArrayInputStream(frameInBytes), outputStream);
                        outputStream.writeUTF(Integer.toString(frame.cols()));
                        outputStream.writeUTF(Integer.toString(frame.rows()));
                    } else {
                        outputStream.writeUTF(videoSignal.get(STOP));
                        cancel();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        int rate = (int) (1000.0 / frameRate);
        System.out.println("RATE = " + rate);
        timer.scheduleAtFixedRate(streamTask, 0, rate);
    }

    public void pauseVideoStreaming(){
        System.out.println("client pause stream");
        timer.cancel();
        timer = new Timer();
    }

    public void stopVideoStreaming(){
        System.out.println("client stopped stream");
        timer.cancel();
        timer = new Timer();
        try {
            outputStream.writeUTF(videoSignal.get(STOP));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
