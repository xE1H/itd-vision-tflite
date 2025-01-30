package org.firstinspires.ftc.teamcode;

import android.graphics.Canvas;
import android.graphics.Paint;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class OrientationDeterminerPostProcessor extends YoloV11VisionPostProcessor {
    List<SampleOrientation> samples = new ArrayList<>();

    public class SampleOrientation {
        public double relativeX;
        public double relativeY;
        public double relativeZ;

        public boolean isVerticallyOriented;

        public SampleOrientation(double relativeX, double relativeY, double relativeZ, boolean isVerticallyOriented) {
            this.relativeX = relativeX;
            this.relativeY = relativeY;
            this.relativeZ = relativeZ;
            this.isVerticallyOriented = isVerticallyOriented;
        }
    }

    @Override
    public void processDetections(Mat undistorted, List<YoloV11Inference.Detection> detectionList) {
        for (YoloV11Inference.Detection detection : detectionList) {
            int x1 = (int) detection.x1;
            int y1 = (int) detection.y1;
            int x2 = (int) detection.x2;
            int y2 = (int) detection.y2;

            int width = x2 - x1;
            int height = y2 - y1;
            boolean isVerticallyOriented = width < height;
            // todo add rgip to get true xyz
        }
    }

    public List<SampleOrientation> getSamples() {
        return samples;
    }

    @Override
    public void onDrawFrame(Canvas canvas, int onscreenWidth, int onscreenHeight, float scaleBmpPxToCanvasPx, float scaleCanvasDensity, Object userContext) {
    }
}
