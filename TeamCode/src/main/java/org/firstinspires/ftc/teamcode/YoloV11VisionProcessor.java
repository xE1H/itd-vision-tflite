package org.firstinspires.ftc.teamcode;

import static org.firstinspires.ftc.teamcode.YoloV11VisionProcessorConfig.CONFIDENCE_THRESHOLD;
import static org.firstinspires.ftc.teamcode.YoloV11VisionProcessorConfig.LABELS;
import static org.firstinspires.ftc.teamcode.YoloV11VisionProcessorConfig.LABEL_COLORS;
import static org.firstinspires.ftc.teamcode.YoloV11VisionProcessorConfig.MODEL_FILE_PATH;
import static org.firstinspires.ftc.teamcode.YoloV11VisionProcessorConfig.MODEL_INPUT_SIZE;
import static org.firstinspires.ftc.teamcode.YoloV11VisionProcessorConfig.Y_OFFSET;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import org.firstinspires.ftc.robotcore.internal.camera.calibration.CameraCalibration;
import org.firstinspires.ftc.vision.VisionProcessor;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class YoloV11VisionProcessor implements VisionProcessor {
    private List<YoloV11Inference.Detection> detectionList = new ArrayList<>();
    private YoloV11Inference detector;

    @Override
    public void init(int width, int height, CameraCalibration calibration) {
        detector = new YoloV11Inference(MODEL_FILE_PATH, MODEL_INPUT_SIZE, CONFIDENCE_THRESHOLD, LABELS, Math.max(width, height), Y_OFFSET);
    }

    @Override
    public Object processFrame(Mat frame, long captureTimeNanos) {
        Bitmap bitmap = getBitmap(frame);

        detectionList = detector.detect(bitmap);

        return null;
    }

    @Override
    public void onDrawFrame(Canvas canvas, int onscreenWidth, int onscreenHeight, float scaleBmpPxToCanvasPx, float scaleCanvasDensity, Object userContext) {
        for (YoloV11Inference.Detection detection : detectionList) {
            Paint paint = new Paint();
            paint.setColor(LABEL_COLORS.get(detection.label));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5);

            canvas.drawRect(
                    detection.x1 * scaleBmpPxToCanvasPx,
                    detection.y1 * scaleBmpPxToCanvasPx,
                    detection.x2 * scaleBmpPxToCanvasPx,
                    detection.y2 * scaleBmpPxToCanvasPx,
                    paint
            );
        }
    }

    private Bitmap getBitmap(Mat frame) {
        Bitmap bitmap = null;
        if (frame == null) {
            return null;
        }

        try {
            Mat convertedMat = new Mat();

            // If the Mat is in BGR format, convert it to RGBA
            if (frame.channels() == 3) {
                Imgproc.cvtColor(frame, convertedMat, Imgproc.COLOR_BGR2RGBA);
            } else if (frame.channels() == 1) {
                Imgproc.cvtColor(frame, convertedMat, Imgproc.COLOR_GRAY2RGBA);
            } else {
                convertedMat = frame;
            }

            bitmap = Bitmap.createBitmap(convertedMat.cols(), convertedMat.rows(),
                    Bitmap.Config.ARGB_8888);

            Utils.matToBitmap(convertedMat, bitmap);

            if (convertedMat != frame) {
                convertedMat.release();
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (bitmap != null) {
                bitmap.recycle();
                bitmap = null;
            }
        }
        return bitmap;
    }
}
