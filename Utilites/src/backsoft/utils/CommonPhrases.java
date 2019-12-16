package backsoft.utils;

import java.util.Map;
import java.util.TreeMap;

public class CommonPhrases {

    public enum SIGNAL{START, NEXT, PLAY, STOP, CORRECT, MISTAKE, BYEBYE};

    public static final Map<SIGNAL, String> videoSignal = new TreeMap<>();
    public static final Map<SIGNAL, String> imageSignal = new TreeMap<>();
    public static final Map<SIGNAL, String> system = new TreeMap<>();

    static {
        imageSignal.put(SIGNAL.START, "image-start");
        imageSignal.put(SIGNAL.STOP, "image-stop");
        imageSignal.put(SIGNAL.CORRECT, "image-checked");
        imageSignal.put(SIGNAL.MISTAKE, "image-fail");

        videoSignal.put(SIGNAL.START, "video-start");
        videoSignal.put(SIGNAL.PLAY, "video-contin");
        videoSignal.put(SIGNAL.NEXT, "video-nextframe");
        videoSignal.put(SIGNAL.STOP, "video-stop");
        videoSignal.put(SIGNAL.CORRECT, "video-checked");
        videoSignal.put(SIGNAL.MISTAKE, "video-fail");

        system.put(SIGNAL.BYEBYE, "system-quit");
    }

}
