package org.firstinspires.ftc.teamcode;

import java.util.Map;

public class YoloV11VisionProcessorConfig {
    public static final String MODEL_FILE_PATH = "best_float32.tflite";
    public static final int MODEL_INPUT_SIZE = 320;
    public static final float CONFIDENCE_THRESHOLD = 0.5f;

    public static final int Y_OFFSET = -280; // This number, in theory, should be able to be
    // calculated out of the model input size and the camera view size, but I could not figure it
    // out.
    // Maybe: 16:9 aspect ratio, so model input size / 16 = 40,
    // 16 - 9 = 7 segments left, so 40 * 7 = 280
    // but this makes no sense to me, so I'm not sure.

    public static final String[] LABELS = {"bluesample", "redsample", "yellowsample"};

    public static final Map<String, Integer> LABEL_COLORS = Map.of(
        "bluesample", 0xFF0000FF,
        "redsample", 0xFFFF0000,
        "yellowsample", 0xFFFFFF00
    );
}
