package org.firstinspires.ftc.teamcode;

import static org.tensorflow.lite.nnapi.NnApiDelegate.Options.EXECUTION_PREFERENCE_SUSTAINED_SPEED;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;

import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.nnapi.NnApiDelegate;
import org.tensorflow.lite.support.common.FileUtil;

import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class YoloV11Inference {
    private final Interpreter tflite;
    private final int modelInputWidth;
    private final int modelInputHeight;
    private final float confidenceThreshold;
    private final String[] labels;
    private final float scaleCoef;
    private final int yOffset;

    public static class Detection {
        public float x1, y1, x2, y2;
        public String label;
        public float confidence;

        public Detection(float x1, float y1, float x2, float y2, String label, float confidence) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.label = label;
            this.confidence = confidence;
        }
    }

    public YoloV11Inference(String modelFilePath, int modelInputSize, float confidenceThreshold, String[] labels, float scaleCoef, int yOffset) { //  = {"bluesample", "redsample", "yellowsample"}
        try {
            Context context = AppUtil.getInstance().getApplication().getApplicationContext();

            modelInputHeight = modelInputSize;
            modelInputWidth = modelInputSize;
            this.confidenceThreshold = confidenceThreshold;
            this.labels = labels;
            this.scaleCoef = scaleCoef;
            this.yOffset = yOffset;

            MappedByteBuffer model = FileUtil.loadMappedFile(context, modelFilePath); // "best_float32.tflite"
            if (model == null) {
                throw new RuntimeException("Failed to load model file");
            }

            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(4);
            options.setUseNNAPI(false);
            options.setUseXNNPACK(true);
            options.setAllowBufferHandleOutput(true);
            options.setAllowFp16PrecisionForFp32(true);

//            try {
//                NnApiDelegate.Options nnApiOptions = new NnApiDelegate.Options();
//                nnApiOptions.setExecutionPreference(EXECUTION_PREFERENCE_SUSTAINED_SPEED);
//                nnApiOptions.setAllowFp16(true);
//                nnApiOptions.setUseNnapiCpu(false);  // Don't use CPU
//                NnApiDelegate nnApiDelegate = new NnApiDelegate(nnApiOptions);
//                options.addDelegate(nnApiDelegate);
//                System.out.println("NNAPI delegate loaded");
//            } catch (Exception e) {
//                System.err.println("Failed to load NNAPI delegate: " + e.getMessage());
//                // NNAPI not available, continue with CPU
//            }

            // Create interpreter
            tflite = new Interpreter(model, options);
            tflite.allocateTensors();
            if (tflite == null) {
                throw new RuntimeException("Failed to create interpreter");
            }

        } catch (Exception e) {
            System.err.println("Error initializing YoloDetector: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize YoloDetector", e);
        }
    }

    private Bitmap letterbox(Bitmap originalBitmap) {
        int originalWidth = originalBitmap.getWidth();
        int originalHeight = originalBitmap.getHeight();

        float scale = Math.min(
                (float) modelInputWidth / originalWidth,
                (float) modelInputHeight / originalHeight
        );

        int newWidth = Math.round(originalWidth * scale);
        int newHeight = Math.round(originalHeight * scale);

        int deltaW = modelInputWidth - newWidth;
        int deltaH = modelInputHeight - newHeight;

        Bitmap paddedBitmap = Bitmap.createBitmap(modelInputWidth, modelInputHeight, Bitmap.Config.ARGB_8888);
        paddedBitmap.eraseColor(0xFF727272); // Gray padding (114,114,114)

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true);

        android.graphics.Canvas canvas = new android.graphics.Canvas(paddedBitmap);
        canvas.drawBitmap(scaledBitmap, deltaW / 2f, deltaH / 2f, null);

        return paddedBitmap;
    }

    public List<Detection> detect(Bitmap bitmap) {
        if (bitmap == null || tflite == null) {
            return new ArrayList<>();
        }

        // Letterbox and preprocess
        Bitmap processedBitmap = letterbox(bitmap);

        // Convert to float array and normalize
        int[] intValues = new int[modelInputWidth * modelInputHeight];
        processedBitmap.getPixels(intValues, 0, modelInputWidth, 0, 0, modelInputWidth, modelInputHeight);

        float[][][][] input = new float[1][modelInputHeight][modelInputWidth][3];
        for (int i = 0; i < modelInputHeight; i++) {
            for (int j = 0; j < modelInputWidth; j++) {
                int pixelValue = intValues[i * modelInputWidth + j];
                input[0][i][j][0] = ((pixelValue >> 16) & 0xFF) / 255.0f;
                input[0][i][j][1] = ((pixelValue >> 8) & 0xFF) / 255.0f;
                input[0][i][j][2] = (pixelValue & 0xFF) / 255.0f;
            }
        }
        int bufferSize = 2100;
        // Prepare output
        float[][][] outputBuffer = new float[1][7][bufferSize];

        // Run inference
        ElapsedTime timer = new ElapsedTime();
        timer.reset();
        tflite.run(input, outputBuffer);
        System.out.println("Inference time: " + timer.milliseconds() + "ms");

        // Process results
        List<Detection> detections = new ArrayList<>();

        for (int i = 0; i < bufferSize; i++) {
            float[] scores = new float[labels.length];
            for (int j = 0; j < labels.length; j++) {
                scores[j] = outputBuffer[0][j + 4][i];
            }

            int classIndex = argmax(scores);
            float confidence = scores[classIndex];

            if (confidence > confidenceThreshold) {
                // Get raw values directly from model output
                float x = outputBuffer[0][0][i] * scaleCoef;  // center x
                float y = outputBuffer[0][1][i] * scaleCoef;  // center y
                float w = outputBuffer[0][2][i] * scaleCoef;  // width
                float h = outputBuffer[0][3][i] * scaleCoef;  // height

                // Convert center/width/height to corners without any scaling
                float x1 = x - w / 2;
                float y1 = y - h / 2 + yOffset;
                float x2 = x1 + w;
                float y2 = y1 + h;

                Detection detection = new Detection(
                        x1, y1, x2, y2,
                        labels[classIndex],
                        confidence
                );
                detections.add(detection);
            }
        }

        return applyNMS(detections, 0.45f);
    }

    private int argmax(float[] array) {
        int maxIndex = 0;
        float maxValue = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > maxValue) {
                maxValue = array[i];
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    private float calculateIoU(Detection box1, Detection box2) {
        float x1 = Math.max(box1.x1, box2.x1);
        float y1 = Math.max(box1.y1, box2.y1);
        float x2 = Math.min(box1.x2, box2.x2);
        float y2 = Math.min(box1.y2, box2.y2);

        if (x2 < x1 || y2 < y1) return 0.0f;

        float intersection = (x2 - x1) * (y2 - y1);
        float box1Area = (box1.x2 - box1.x1) * (box1.y2 - box1.y1);
        float box2Area = (box2.x2 - box2.x1) * (box2.y2 - box2.y1);

        return intersection / (box1Area + box2Area - intersection);
    }

    private List<Detection> applyNMS(List<Detection> detections, float nmsThreshold) {
        detections.sort((d1, d2) -> Float.compare(d2.confidence, d1.confidence));
        List<Detection> selectedDetections = new ArrayList<>();
        boolean[] removed = new boolean[detections.size()];

        for (int i = 0; i < detections.size(); i++) {
            if (removed[i]) continue;
            selectedDetections.add(detections.get(i));

            for (int j = i + 1; j < detections.size(); j++) {
                if (removed[j]) continue;
                if (detections.get(i).label.equals(detections.get(j).label)) {
                    float iou = calculateIoU(detections.get(i), detections.get(j));
                    if (iou > nmsThreshold) {
                        removed[j] = true;
                    }
                }
            }
        }

        return selectedDetections;
    }

    public void close() {
        if (tflite != null) {
            tflite.close();
        }
    }
}