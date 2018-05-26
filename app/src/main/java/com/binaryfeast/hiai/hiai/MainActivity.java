package com.binaryfeast.hiai.hiai;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.huawei.hiai.vision.common.ConnectionCallback;
import com.huawei.hiai.vision.common.VisionBase;
import com.huawei.hiai.vision.visionkit.common.BoundingBox;
import com.huawei.hiai.vision.visionkit.face.Face;
import com.huawei.hiai.vision.visionkit.face.FaceLandmark;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.support.v4.content.ContextCompat;

/**
 * Created by huarong on 2018/2/26.
 */
public class MainActivity extends AppCompatActivity implements MMListener {
    private static final String LOG_TAG = "face_detect";
    private Button btnTake;
    private Button btnSelect;
    private ImageView ivImage;
    private TextView tvFace;

    private static final int REQUEST_IMAGE_TAKE = 100;
    private static final int REQUEST_IMAGE_SELECT = 200;

    private Uri fileUri;
    private Bitmap bmp;
    private ProgressDialog dialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ivImage = (ImageView) findViewById(R.id.bgImageView);
        tvFace = (TextView) findViewById(R.id.faceTextView);

        btnTake = (Button) findViewById(R.id.snapButton);
        btnTake.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                initDetect();
                //Log.d(LOG_TAG, "get uri");
                fileUri = getOutputMediaFileUri();
                Log.d(LOG_TAG, "end get uri = " + fileUri);
                Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                i.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
                startActivityForResult(i, REQUEST_IMAGE_TAKE);
            }
        });

        btnSelect = (Button) findViewById(R.id.selectButton);
        btnSelect.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                initDetect();
                Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, REQUEST_IMAGE_SELECT);
            }
        });
        //To connect HiAi Engine service using VisionBase
        VisionBase.init(MainActivity.this,new ConnectionCallback(){
            @Override
            public void onServiceConnect() {
                //This callback method is called when the connection to the service is successful.
                //Here you can initialize the detector class, mark the service connection status, and more.
                Log.i(LOG_TAG, "onServiceConnect ");
            }

            @Override
            public void onServiceDisconnect() {
                //This callback method is called when disconnected from the service.
                //You can choose to reconnect here or to handle exceptions.
                Log.i(LOG_TAG, "onServiceDisconnect");
            }
        });

        requestPermissions();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == REQUEST_IMAGE_TAKE || requestCode == REQUEST_IMAGE_SELECT) && resultCode == RESULT_OK) {
            String imgPath;

            if (requestCode == REQUEST_IMAGE_TAKE) {
                imgPath = getExternalFilesDir(".")+ fileUri.getPath();
            } else {
                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                Cursor cursor = MainActivity.this.getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                imgPath = cursor.getString(columnIndex);
                cursor.close();
            }
            Log.i(LOG_TAG, "imgPath = " + imgPath);
            bmp = BitmapFactory.decodeFile(imgPath);
            dialog = ProgressDialog.show(MainActivity.this,
                    "Predicting...", "Wait for one sec...", true);

            FaceDetectTask cnnTask = new FaceDetectTask(MainActivity.this);
            cnnTask.execute(bmp);
        } else {
            btnTake.setEnabled(true);
            btnSelect.setEnabled(true);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
    @Override
    public void onTaskCompleted(List<Face> faces) {

        Bitmap tempBmp = bmp.copy(Bitmap.Config.ARGB_8888, true);

        if (faces == null) {
            tvFace.setText("not get face");
        } else {
            Canvas canvas = new Canvas(tempBmp);

            Paint paint = new Paint();
            paint.setColor(Color.GREEN);
            for (Face face : faces) {
                BoundingBox faceRect = face.getFaceRect();
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth((float)(faceRect.getWidth())/100);
                canvas.drawRect(faceRect.getLeft(), faceRect.getTop(),  faceRect.getLeft()+faceRect.getWidth(), faceRect.getTop()+faceRect.getHeight(), paint);
                List<FaceLandmark> landmarks = face.getLandmarks();
                for (FaceLandmark landmark : landmarks) {
                    canvas.drawPoint(landmark.getPosition().x, landmark.getPosition().y, paint);
                }

                int textHeight = faceRect.getWidth()/10;
                paint.setTextSize(textHeight);
                paint.setStyle(Paint.Style.FILL);
                int textX = faceRect.getLeft();
                int textY = faceRect.getTop()+faceRect.getHeight()+textHeight;
                String strFace = "yaw: " + face.getYaw();
                canvas.drawText(strFace, textX, textY, paint);
                strFace = "pitch: " + face.getPitch();
                textY += textHeight;
                canvas.drawText(strFace, textX, textY, paint);
                strFace = "roll: " + face.getRoll();
                textY += textHeight;
                canvas.drawText(strFace, textX, textY, paint);
            }

        }

        ivImage.setImageBitmap(tempBmp);

        btnTake.setEnabled(true);
        btnSelect.setEnabled(true);

        if (dialog != null) {
            dialog.dismiss();
        }
    }
    private void initDetect() {
        btnTake.setEnabled(false);
        btnSelect.setEnabled(false);
        tvFace.setText("");
    }
    /**
     * Create a file Uri for saving an image or video
     */
    private  Uri getOutputMediaFileUri() {
        //return Uri.fromFile(getOutputMediaFile(type));
        Log.d(LOG_TAG, "authority = " + getPackageName() + ".provider");
        Log.d(LOG_TAG, "getApplicationContext = " + getApplicationContext());
        return FileProvider.getUriForFile(this, getPackageName() +".fileprovider", getOutputMediaFile());
    }
    /**
     * Create a File for saving an image
     */
    private File getOutputMediaFile() {
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile = new File(getExternalFilesDir("images"),"image.jpg");
        if (mediaFile.exists()) {
            mediaFile.delete();
        }
        Log.d(LOG_TAG, "mediaFile " + mediaFile);
        return mediaFile;
    }
    private void requestPermissions(){
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int permission = ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if(permission!= PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.CAMERA},0x0010);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}