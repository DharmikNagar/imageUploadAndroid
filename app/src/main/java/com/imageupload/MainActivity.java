package com.imageupload;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.imageupload.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private String currentPhotoPath;
    File selectedFile;
    private ArrayList<String> upArrayList,upArrayListPath;
    private File mFileSelectedCoursePdf = null;
    private String uploadedpdfurl = "";
    AppCompatImageView imgView;
    Button btn,btnCamera,btnSubmit,btnPdf;
    ActivityResultLauncher<Intent> activityResultLauncher;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int STORAGE_PERMISSION_CODE = 101;
    private File mFileSelectedStudentImage = null;
    Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imgView = findViewById(R.id.imgView);
        btn = findViewById(R.id.btn);
        btnCamera = findViewById(R.id.btnCamera);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnPdf = findViewById(R.id.btnPdf);

        // Initialize ArrayLists
        upArrayList = new ArrayList<>();
        upArrayListPath = new ArrayList<>();

        btn.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            activityResultLauncher.launch(intent);
        });

        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if(result.getResultCode() == Activity.RESULT_OK){
                Intent data = result.getData();
                Uri uri = data.getData();
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),uri);
                    imgView.setImageBitmap(bitmap);

                    WeakReference<Bitmap> git = new WeakReference<>(Bitmap.createScaledBitmap(bitmap,
                            bitmap.getHeight(),bitmap.getWidth(),false).copy(
                            Bitmap.Config.RGB_565,true
                    ));

                    Bitmap bm = git.get();
                    saveImage(bm,MainActivity.this);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });


        btnCamera.setOnClickListener(view -> {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
            } else {
                captureImage();
            }
        });

        imgView.setOnClickListener(view -> {
            Uri contentUri = FileProvider.getUriForFile(this, "com.imageupload.MainActivity.provider", selectedFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(contentUri, "application/pdf");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        });

        btnPdf.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/pdf");
            startActivityForResult(intent, STORAGE_PERMISSION_CODE);
        });

        btnSubmit.setOnClickListener(view -> {
            // Handle submission button click
        });
    }

    private void saveImage(Bitmap bm, Context context){
        File imageFolder = new File(context.getCacheDir(),"images");

        try {
            imageFolder.mkdirs();
            mFileSelectedStudentImage = new File(imageFolder,"captured_image.jpg");
            FileOutputStream stream = new FileOutputStream(mFileSelectedStudentImage);
            bm.compress(Bitmap.CompressFormat.JPEG,100,stream);
            stream.flush();
            stream.close();

        }catch (Exception e){
            Log.e("Exception>>>>",e.getStackTrace()+"");
        }
    }

    private void captureImage() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            try {
                mFileSelectedStudentImage = createImageFile();
                Uri photoURI = FileProvider.getUriForFile(MainActivity.this,
                        "com.imageupload.MainActivity.fileprovider",
                        mFileSelectedStudentImage);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, 1);
            } catch (IOException ex) {
                Log.e("Image Capture Error>>>",ex.getMessage());
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            Bitmap bitmap = BitmapFactory.decodeFile(mFileSelectedStudentImage.getAbsolutePath());
            imgView.setImageBitmap(bitmap);
        }
        if (requestCode == STORAGE_PERMISSION_CODE && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            if (uri != null) {
                selectedFile = new File(uri.getPath());
                if (!selectedFile.exists()) {
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(uri);
                        String fileName = getFileName(uri);
                        File tempFile = new File(getCacheDir(), fileName);

                        if(tempFile != null){
                            FileOutputStream outputStream = new FileOutputStream(tempFile);
                            imgView.setImageResource(R.drawable.download);
                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = inputStream.read(buffer)) > 0) {
                                outputStream.write(buffer, 0, length);
                            }
                            outputStream.close();
                            inputStream.close();
                        } else {
                            // Handle the case where the temp file is null.
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(this, "File Does Not Exist", Toast.LENGTH_SHORT).show();
                }

                mFileSelectedCoursePdf = new File(uri.getPath());
                upArrayListPath.add("" + uri);

                uploadedpdfurl = "";

                upArrayList.add(mFileSelectedCoursePdf.getName());
            }
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }
}
