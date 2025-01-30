package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.vision.VisionPortal;

import android.graphics.Bitmap;
import android.util.Size;

import java.util.List;

@TeleOp
public class TestVision extends LinearOpMode {

    @Override
    public void runOpMode() throws InterruptedException {
        YoloV11VisionProcessor processor = new YoloV11VisionProcessor();
        processor.setPostProcessor(new OrientationDeterminerPostProcessor());

        VisionPortal portal = new VisionPortal.Builder()
                .setCamera(hardwareMap.get(WebcamName.class, "Webcam 1"))
                .setCameraResolution(new Size(1280, 720))
                .enableLiveView(true)
                .setStreamFormat(VisionPortal.StreamFormat.MJPEG)
                .addProcessor(processor)
                .build();

        portal.resumeStreaming();

        while (opModeInInit()) {
            sleep(1);
        }
    }
}