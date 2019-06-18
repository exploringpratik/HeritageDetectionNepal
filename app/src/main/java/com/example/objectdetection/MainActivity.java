package com.example.objectdetection;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.ml.common.modeldownload.FirebaseLocalModel;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager;
import com.google.firebase.ml.common.modeldownload.FirebaseRemoteModel;
import com.soundcloud.android.crop.Crop;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    // button for each available classifier
    private Button button;

    // for permission requests
    public static final int REQUEST_PERMISSION = 300;

    // request code for permission requests to the os for image
    public static final int REQUEST_IMAGE = 100;

    // will hold uri of image obtained from camera
    private Uri imageUri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // request permission to use the camera on the user's phone
        if (ActivityCompat.checkSelfPermission(this.getApplicationContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, REQUEST_PERMISSION);
        }

        // request permission to write data (aka images) to the user's external storage of their phone
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION);
        }

        // request permission to read data (aka images) from the user's external storage of their phone
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION);
        }


        FirebaseModelDownloadConditions conditions = new FirebaseModelDownloadConditions.Builder()
                .requireWifi()
                .build();
        FirebaseRemoteModel remoteModel = new FirebaseRemoteModel.Builder("nyatapola_201951493517")
                .enableModelUpdates(true)
                .setInitialDownloadConditions(conditions)
                .setUpdatesDownloadConditions(conditions)
                .build();
        FirebaseModelManager.getInstance().registerRemoteModel(remoteModel);

        FirebaseLocalModel localModel = new FirebaseLocalModel.Builder("my_local_model")
                .setAssetFilePath("manifest.json")
                .build();
        FirebaseModelManager.getInstance().registerLocalModel(localModel);


        // on click for inception float model
        button = (Button) findViewById(R.id.b_camera);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openCameraIntent();
            }
        });

    }

    // opens camera for user
    private void openCameraIntent() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera");
        // tell camera where to store the resulting picture
        imageUri = getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        // start camera, and wait for it to finish
        startActivityForResult(intent, REQUEST_IMAGE);
    }

    // checks that the user has allowed all the required permission of read and write and camera. If not, notify the user and close the application
    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(getApplicationContext(), "This application needs read, write, and camera permissions to run. Application now closing.", Toast.LENGTH_LONG).show();
                System.exit(0);
            }
        }
    }

    // dictates what to do after the user takes an image, selects and image, or crops an image
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // if the camera activity is finished, obtained the uri, and send it to 'Classify' activity
        if (requestCode == REQUEST_IMAGE && resultCode == RESULT_OK) {
            try {
                Intent i = new Intent(MainActivity.this, VisionImage.class);
                // put image data in extras to send
                i.putExtra("resID_uri", imageUri);
                startActivity(i);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }


    }


//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data){
//        super.onActivityResult(requestCode, resultCode, data);
//        // if the camera activity is finished, obtained the uri, crop it to make it square, and send it to 'Classify' activity
//        if(requestCode == REQUEST_IMAGE && resultCode == RESULT_OK) {
//            try {
//                Uri source_uri = imageUri;
//                Uri dest_uri = Uri.fromFile(new File(getCacheDir(), "cropped"));
//                // need to crop it to square image as CNN's always required square input
//                Crop.of(source_uri, dest_uri).asSquare().start(MainActivity.this);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//
//        // if cropping acitivty is finished, get the resulting cropped image uri and send it to 'Classify' activity
//        else if(requestCode == Crop.REQUEST_CROP && resultCode == RESULT_OK){
//            imageUri = Crop.getOutput(data);
//            Intent i = new Intent(MainActivity.this, VisionImage.class);
//            // put image data in extras to send
//            i.putExtra("resID_uri", imageUri);
//
//            startActivity(i);
//        }
//    }
}

