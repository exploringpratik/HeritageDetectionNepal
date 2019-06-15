package com.example.objectdetection;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.common.FirebaseMLException;
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;
import com.google.firebase.ml.vision.label.FirebaseVisionOnDeviceAutoMLImageLabelerOptions;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public class VisionImage extends AppCompatActivity {
    private Uri uri;
    private ImageView selected_image;
    private Bitmap bitmap;
    private static final String TAG = "VisionImage";
    private static final String MY_CAMERA_ID = "my_camera_id";
    private Context context;
    private FirebaseVisionImage image;
    private TextView textView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vision_image);
        selected_image = findViewById(R.id.iv_object);
        textView = findViewById(R.id.tv_prediction);
        uri = (Uri) getIntent().getParcelableExtra("resID_uri");
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            selected_image.setImageBitmap(bitmap);
            selected_image.setRotation(selected_image.getRotation() + 90);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //try {
        image = FirebaseVisionImage.fromBitmap(bitmap);
//            int rotation = getRotationCompensation(MY_CAMERA_ID, VisionImage.this, context);
//            Image acquireLatestImage = acquireLatestImage();
//            image = FirebaseVisionImage.fromMediaImage(acquireLatestImage, rotation);
//
//            FirebaseVisionImageMetadata metadata = new FirebaseVisionImageMetadata.Builder()
//                    .setWidth(480)   // 480x360 is typically sufficient for
//                    .setHeight(360)  // image recognition
//                    .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
//                    .setRotation(rotation)
//                    .build();
//
//            ByteBuffer buffer = acquireLatestImage.getPlanes()[0].getBuffer();
//            image = FirebaseVisionImage.fromByteBuffer(buffer, metadata);

//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }


        FirebaseVisionOnDeviceAutoMLImageLabelerOptions labelerOptions =
                new FirebaseVisionOnDeviceAutoMLImageLabelerOptions.Builder()
                        .setLocalModelName("my_local_model")    // Skip to not use a local model
                        .setRemoteModelName("nyatapola_201951493517")  // Skip to not use a remote model
                        .setConfidenceThreshold((float) 0.5)  // Evaluate your model in the Firebase console
                        // to determine an appropriate value.
                        .build();
        try {
            FirebaseVisionImageLabeler labeler =
                    FirebaseVision.getInstance().getOnDeviceAutoMLImageLabeler(labelerOptions);

            labeler.processImage(image)
                    .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionImageLabel>>() {
                        @Override
                        public void onSuccess(List<FirebaseVisionImageLabel> labels) {
                            String text[] = new String[10];
                            float confidence[] = new float[10];
                            int i = 0;
                            for (FirebaseVisionImageLabel label : labels) {
                                text[i] = label.getText();
                                confidence[i] = label.getConfidence();
                               textView.setText("S No. "+i +" "+ text[i]+" "+confidence[i]);
                               i = i+1;
                            }


                            // ...
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Task failed with an exception
                            // ...
                        }
                    });


        } catch (FirebaseMLException e) {
            e.printStackTrace();
        }


    }

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    /**
     * Get the angle by which an image must be rotated given the device's current
     * orientation.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private int getRotationCompensation(String cameraId, Activity activity, Context context)
            throws CameraAccessException {
        // Get the device's current rotation relative to its "native" orientation.
        // Then, from the ORIENTATIONS table, look up the angle the image must be
        // rotated to compensate for the device's rotation.
        int deviceRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int rotationCompensation = ORIENTATIONS.get(deviceRotation);

        // On most devices, the sensor orientation is 90 degrees, but for some
        // devices it is 270 degrees. For devices with a sensor orientation of
        // 270, rotate the image an additional 180 ((270 + 270) % 360) degrees.
        CameraManager cameraManager = (CameraManager) context.getSystemService(CAMERA_SERVICE);
        int sensorOrientation = cameraManager
                .getCameraCharacteristics(cameraId)
                .get(CameraCharacteristics.SENSOR_ORIENTATION);
        rotationCompensation = (rotationCompensation + sensorOrientation + 270) % 360;

        // Return the corresponding FirebaseVisionImageMetadata rotation value.
        int result;
        switch (rotationCompensation) {
            case 0:
                result = FirebaseVisionImageMetadata.ROTATION_0;
                break;
            case 90:
                result = FirebaseVisionImageMetadata.ROTATION_90;
                break;
            case 180:
                result = FirebaseVisionImageMetadata.ROTATION_180;
                break;
            case 270:
                result = FirebaseVisionImageMetadata.ROTATION_270;
                break;
            default:
                result = FirebaseVisionImageMetadata.ROTATION_0;
                Log.e(TAG, "Bad rotation value: " + rotationCompensation);
        }
        return result;
    }

    public Image acquireLatestImage() {
        return acquireLatestImage();
    }

}
