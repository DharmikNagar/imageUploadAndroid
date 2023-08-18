package com.imageupload;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64OutputStream;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.util.Base64;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    AppCompatImageView imgView;
    Button btn,btnCamera,btnSubmit;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int STORAGE_PERMISSION_CODE = 101;
    Bitmap bitmap;
    private static final int pic_id = 123;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imgView = findViewById(R.id.imgView);
        btn = findViewById(R.id.btn);
        btnCamera = findViewById(R.id.btnCamera);
        btnSubmit = findViewById(R.id.btnSubmit);

        ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if(result.getResultCode() == Activity.RESULT_OK){
                    Intent data = result.getData();
                    Uri uri = data.getData();
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),uri);
                        imgView.setImageBitmap(bitmap);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, STORAGE_PERMISSION_CODE);

                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                activityResultLauncher.launch(intent);
            }
        });

        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_CODE);

                Intent camera_intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(camera_intent, pic_id);
            }
        });

        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");  // Set the MIME type or specific type of files you want to allow
                startActivityForResult(intent, 2);
            }
        });

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                upload();
            }
        });
    }

    public void checkPermission(String permission, int requestCode)
    {
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[] { permission }, requestCode);
        }
        else {
            Toast.makeText(MainActivity.this, "Permission already granted", Toast.LENGTH_SHORT).show();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == pic_id) {
            bitmap = (Bitmap) data.getExtras().get("data");
            imgView.setImageBitmap(bitmap);

            saveImage(bitmap,MainActivity.this);
        }

        if (requestCode == 2 && resultCode == RESULT_OK) {
            Uri selectedFileUri = data.getData();
            File selectedFile = new File(selectedFileUri.getPath());
            String base64String = getStringFileofBase64(selectedFile);

            Log.e("base64String>>>",base64String);
        }
    }

    public void upload(){
        ByteArrayOutputStream byteArrayOutputStream;
        byteArrayOutputStream = new ByteArrayOutputStream();
        if(bitmap!=null){
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,byteArrayOutputStream);
            Log.e("Bitmap>>",bitmap+"");
            byte[] bytes = byteArrayOutputStream.toByteArray();
            final String byte64 = Base64.encodeToString(bytes, Base64.DEFAULT);
            Log.e("byte64>>",byte64 +"");
            RetrofitInstance.getRetrofitInstance().apiInterface.add_Item(byte64).enqueue(new Callback<JsonObject>() {

                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    Log.e("data>>>",response.body()+"");
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.e("Fail>>",t.getMessage());
                }
            });
        }
    }

    private void saveImage(Bitmap bm, Context context) {
        File imageFolder = new File(context.getCacheDir(), "images");
        try {
            if (!imageFolder.exists()) {
                imageFolder.mkdirs();
            }

            Date date = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
            String name = dateFormat.format(date) + ".png";
            File file = new File(imageFolder, name);

            FileOutputStream stream = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(stream);

            bm.compress(Bitmap.CompressFormat.PNG, 100, bos);
            bos.flush();
            bos.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getStringFileofBase64(File f) {
        InputStream inputStream = null;
        String encodedFile= "", lastVal;
        try {
            inputStream = new FileInputStream(f.getAbsolutePath());

            byte[] buffer = new byte[10240];//specify the size to allow
            int bytesRead;
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Base64OutputStream output64 = new Base64OutputStream(output, Base64.DEFAULT);

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                output64.write(buffer, 0, bytesRead);
            }
            output64.close();
            encodedFile =  output.toString();
        }
        catch (FileNotFoundException e1 ) {
            e1.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        lastVal = encodedFile;
        return lastVal;
    }

}