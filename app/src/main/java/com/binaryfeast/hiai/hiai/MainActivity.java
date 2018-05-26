package com.binaryfeast.hiai.hiai;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.Shader;
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
import com.huawei.hiai.vision.image.sr.ImageSuperResolution;
import com.huawei.hiai.vision.visionkit.common.BoundingBox;
import com.huawei.hiai.vision.visionkit.common.Frame;
import com.huawei.hiai.vision.visionkit.face.Face;
import com.huawei.hiai.vision.visionkit.face.FaceLandmark;
import com.huawei.hiai.vision.visionkit.image.ImageResult;
import com.huawei.hiai.vision.visionkit.image.sr.SuperResolutionConfiguration;

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
                Log.d(LOG_TAG, "get uri");
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

            //Paint paint = new Paint();
            //paint.setColor(Color.GREEN);
            for (Face face : faces) {
                BoundingBox faceRect = face.getFaceRect();
                /*paint.setStyle(Paint.Style.STROKE);
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
                canvas.drawText(strFace, textX, textY, paint);*/

                Paint paint = new Paint();
                paint.setColor(0xFFFFFFFF);
                //canvas.drawRect(0, 0, bmp.getWidth(), bmp.getHeight(), paint);

                Bitmap faceBmp = Bitmap.createBitmap(bmp, faceRect.getLeft(), faceRect.getTop(), faceRect.getWidth(), faceRect.getHeight());
                Path path = new Path();
                path.addOval(faceRect.getWidth() / 6, faceRect.getHeight() / 6, faceRect.getWidth() - faceRect.getWidth() / 6, faceRect.getHeight() - faceRect.getHeight() / 6, Path.Direction.CW);
                path.setFillType(Path.FillType.WINDING);
                faceBmp.setHasAlpha(true);
                Canvas faceCanvas = new Canvas(faceBmp);
                faceCanvas.clipOutPath(path);
                faceCanvas.drawColor(0x000000000, PorterDuff.Mode.CLEAR);

                Bitmap alpha = faceBmp.extractAlpha();
                Paint paintBlur = new Paint();
                BlurMaskFilter blurMaskFilter = new BlurMaskFilter(faceRect.getWidth() / 20, BlurMaskFilter.Blur.INNER);
                paintBlur.setMaskFilter(blurMaskFilter);
                faceCanvas.drawBitmap(alpha, 0, 0, paintBlur);

                //Create inner blur
                /*lurMaskFilter = new BlurMaskFilter(faceRect.getWidth() / 20, BlurMaskFilter.Blur.NORMAL);
                paintBlur.setMaskFilter(blurMaskFilter);
                canvas.drawBitmap(faceBmp, faceRect.getLeft(), faceRect.getTop(), paintBlur);*/
                canvas.drawBitmap(faceBmp, faceRect.getLeft(), faceRect.getTop(), null);

                FaceLandmark leftEye = null;
                FaceLandmark rightEye = null;
                FaceLandmark nose = null;
                FaceLandmark leftLip = null;
                FaceLandmark rightLip = null;
                List<FaceLandmark> landmarks = face.getLandmarks();
                for (FaceLandmark landmark : landmarks) {
                    if (landmark.getType() == 0)
                        leftEye = landmark;
                    if (landmark.getType() == 1)
                        rightEye = landmark;
                    if (landmark.getType() == 2)
                        nose = landmark;
                    if (landmark.getType() == 3)
                        leftLip = landmark;
                    if (landmark.getType() == 4)
                        rightLip = landmark;
                }

                if (leftLip != null && rightLip != null) {
                        int lipWidth = rightLip.getPosition().x - leftLip.getPosition().x;
                        int lipHeight = lipWidth / 3;
                        int eyeWidth = faceRect.getWidth() / 6;
                        int eyeHeight = faceRect.getHeight() / 12;

                        Bitmap lipBmp = Bitmap.createBitmap(bmp, leftLip.getPosition().x, rightLip.getPosition().y - lipHeight / 2, lipWidth, lipHeight);
                        Bitmap scaledLipBmp = Bitmap.createScaledBitmap(lipBmp, eyeWidth, eyeHeight, false);

                        path = new Path();
                        path.addOval(0, 0, eyeWidth, eyeHeight, Path.Direction.CW);
                        path.setFillType(Path.FillType.WINDING);
                        scaledLipBmp.setHasAlpha(true);
                        Canvas lipCanvas = new Canvas(scaledLipBmp);
                        lipCanvas.clipOutPath(path);
                        lipCanvas.drawColor(0x000000000, PorterDuff.Mode.CLEAR);

                        if (leftEye != null) {
                            //canvas.drawBitmap(lipBmp, leftEye.getPosition().x - lipWidth / 2, leftEye.getPosition().y - lipHeight / 2, null);
                            canvas.drawBitmap(scaledLipBmp, leftEye.getPosition().x - eyeWidth / 2, leftEye.getPosition().y - eyeHeight / 2, null);

                            Bitmap leftEyeBmp = Bitmap.createBitmap(bmp, leftEye.getPosition().x - eyeWidth / 2, leftEye.getPosition().y - eyeHeight / 2, eyeWidth, eyeHeight);
                            Bitmap scaledLeftEyeBmp = Bitmap.createScaledBitmap(leftEyeBmp, lipWidth, lipHeight, false);

                            path = new Path();
                            path.addOval(0, 0, lipWidth, lipHeight, Path.Direction.CW);
                            path.setFillType(Path.FillType.WINDING);
                            scaledLeftEyeBmp.setHasAlpha(true);
                            Canvas eyeCanvas = new Canvas(scaledLeftEyeBmp);
                            eyeCanvas.clipOutPath(path);
                            eyeCanvas.drawColor(0x000000000, PorterDuff.Mode.CLEAR);


                            //canvas.drawBitmap(leftEyeBmp, (leftLip.getPosition().x + rightLip.getPosition().x) / 2 - eyeWidth / 2, (leftLip.getPosition().y + rightLip.getPosition().y) / 2 - eyeHeight / 2, null);
                            canvas.drawBitmap(scaledLeftEyeBmp, (leftLip.getPosition().x + rightLip.getPosition().x) / 2 - lipWidth / 2, (leftLip.getPosition().y + rightLip.getPosition().y) / 2 - lipHeight / 2, null);
                        }

                    if (rightEye != null)
                        //canvas.drawBitmap(lipBmp, rightEye.getPosition().x - lipWidth / 2, rightEye.getPosition().y - lipHeight / 2, null);
                        canvas.drawBitmap(scaledLipBmp, rightEye.getPosition().x - eyeWidth / 2, rightEye.getPosition().y - eyeHeight / 2, null);
                }

                /*if (leftEye != null) {
                    int width = faceRect.getWidth() / 7;
                    int height = faceRect.getHeight() / 12;
                    Bitmap leftEyeBmp = Bitmap.createBitmap(tempBmp, leftEye.getPosition().x - width / 2, leftEye.getPosition().y - height / 2, width, height);

                    canvas.drawBitmap(leftEyeBmp, leftEye.getPosition().x - width / 2, leftEye.getPosition().y + height / 2, null);
                    //canvas.drawBitmap(leftEyeBmp, landmark.getPosition().x - width / 2, landmark.getPosition().y + height / 2  + height, null);
                    //canvas.drawBitmap(leftEyeBmp, landmark.getPosition().x - width / 2, landmark.getPosition().y + height / 2 + height * 2, null);

                    Frame frame = new Frame();
                    frame.setBitmap(leftEyeBmp);
                }*/

                /*Bitmap faceBmp = Bitmap.createBitmap(tempBmp, faceRect.getLeft(), faceRect.getTop(), faceRect.getWidth(), faceRect.getHeight());;
                Bitmap shrunkBmp = Bitmap.createScaledBitmap(faceBmp,faceRect.getWidth() / 6, faceRect.getHeight() / 6, false);

                Frame frame = new Frame();
                frame.setBitmap(shrunkBmp);

                ImageSuperResolution superResolution = new ImageSuperResolution(MainActivity.this);

                SuperResolutionConfiguration paras = new SuperResolutionConfiguration(
                        SuperResolutionConfiguration.SISR_SCALE_3X,
                        SuperResolutionConfiguration.SISR_QUALITY_HIGH);

                superResolution.setSuperResolutionConfiguration(paras);

                ImageResult result = superResolution.doSuperResolution(frame, null);*/

                //Log.e(LOG_TAG, "" + result.getResultCode());
                /*if (result.getBitmap() == null) {
                    Log.e(LOG_TAG, "Result bitmap is null!");

                    return;
                }

                canvas.drawBitmap(result.getBitmap(), faceRect.getLeft(), faceRect.getTop(), null);*/

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