package backsoft.utils;

import java.util.Map;
import java.util.TreeMap;

public class CommonPhrases {

    public enum SIGNAL{START, NEXT, PLAY, STOP, CORRECT, MISTAKE, BYEBYE};

    public static final Map<SIGNAL, String> videoSignal = new TreeMap<>();
    public static final Map<SIGNAL, String> byteFileSignal = new TreeMap<>();
    public static final Map<SIGNAL, String> system = new TreeMap<>();

    static {
        byteFileSignal.put(SIGNAL.START, "image-start");
        byteFileSignal.put(SIGNAL.STOP, "image-stop");
        byteFileSignal.put(SIGNAL.CORRECT, "image-checked");
        byteFileSignal.put(SIGNAL.MISTAKE, "image-fail");

        videoSignal.put(SIGNAL.START, "video-start");
        videoSignal.put(SIGNAL.PLAY, "video-contin");
        videoSignal.put(SIGNAL.NEXT, "video-nextframe");
        videoSignal.put(SIGNAL.STOP, "video-stop");
        videoSignal.put(SIGNAL.CORRECT, "video-checked");
        videoSignal.put(SIGNAL.MISTAKE, "video-fail");

        system.put(SIGNAL.BYEBYE, "system-quit");
    }

}
