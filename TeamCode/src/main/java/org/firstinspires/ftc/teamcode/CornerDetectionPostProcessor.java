package org.firstinspires.ftc.teamcode;

import android.graphics.Canvas;
import android.graphics.Paint;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class CornerDetectionPostProcessor extends YoloV11VisionPostProcessor {
    List<Point> corners = new ArrayList<>();

    @Override
    public void processDetections(Mat undistorted, List<YoloV11Inference.Detection> detectionList) {
        corners.clear();
        for (YoloV11Inference.Detection detection : detectionList) {
            int x1 = (int) detection.x1;
            int y1 = (int) detection.y1;
            int x2 = (int) detection.x2;
            int y2 = (int) detection.y2;

            // Seperate the image of the detection window only
            Mat detectionWindow = new Mat(undistorted, new org.opencv.core.Rect(x1, y1, x2 - x1, y2 - y1));
            Mat grayDetectionWindow = new Mat();
            Imgproc.cvtColor(detectionWindow, grayDetectionWindow, Imgproc.COLOR_RGB2GRAY);

            MatOfPoint corners = new MatOfPoint();
            // Do corner detection on the detection window
            Imgproc.goodFeaturesToTrack(grayDetectionWindow, corners, 4, 0.01, 10);

            // Add the corners to the list of corners
            for (Point corner : corners.toList()) {
                this.corners.add(new Point(corner.x + x1, corner.y + y1));
            }
            detectionWindow.release();
            grayDetectionWindow.release();
            corners.release();
        }
    }

    @Override
    public void onDrawFrame(Canvas canvas, int onscreenWidth, int onscreenHeight, float scaleBmpPxToCanvasPx, float scaleCanvasDensity, Object userContext) {
        Paint paint = new Paint();
        paint.setColor(0xFF00FF00);
        paint.setStyle(Paint.Style.FILL);
        // Draw corners on the canvas
        for (Point corner : corners) {
            canvas.drawCircle((float) corner.x, (float) corner.y, 10, paint);
        }
    }
}
