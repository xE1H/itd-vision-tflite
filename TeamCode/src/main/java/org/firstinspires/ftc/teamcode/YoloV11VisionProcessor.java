package org.firstinspires.ftc.teamcode;

import static org.firstinspires.ftc.teamcode.VisionConfiguration.*;
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
import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class YoloV11VisionProcessor implements VisionProcessor {
    private List<YoloV11Inference.Detection> detectionList = new ArrayList<>();
    private YoloV11Inference detector;

    private YoloV11VisionPostProcessor postProcessor;

    Mat cameraMatrix = new Mat(3, 3, CvType.CV_64F);
    Mat distCoeffs = new Mat(1, 5, CvType.CV_64F);

    @Override
    public void init(int width, int height, CameraCalibration calibration) {
        detector = new YoloV11Inference(MODEL_FILE_PATH, MODEL_INPUT_SIZE, CONFIDENCE_THRESHOLD, LABELS, Math.max(width, height), Y_OFFSET);

        double[] calibrationData = new double[]{
                FX, 0, CX,
                0, FY, CY,
                0, 0, 1
        };
        cameraMatrix.put(0, 0, calibrationData);

        distCoeffs = new Mat(1, 5, CvType.CV_64F);
        double[] distCoeffData = new double[]{K1, K2, P1, P2, K3};
        distCoeffs.put(0, 0, distCoeffData);
    }

    public void setPostProcessor(YoloV11VisionPostProcessor postProcessor) {
        this.postProcessor = postProcessor;
    }

    @Override
    public Object processFrame(Mat frame, long captureTimeNanos) {
        Mat undistorted = new Mat();
        Calib3d.undistort(frame, undistorted, cameraMatrix, distCoeffs);

        Bitmap bitmap = getBitmap(frame);

        detectionList = detector.detect(bitmap);

        if (postProcessor != null) {
            postProcessor.processDetections(undistorted, detectionList);
        }

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
        if (postProcessor != null) {
            postProcessor.onDrawFrame(canvas, onscreenWidth, onscreenHeight, scaleBmpPxToCanvasPx, scaleCanvasDensity, userContext);
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
