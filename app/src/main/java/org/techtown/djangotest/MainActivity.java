package org.techtown.djangotest;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.loader.content.CursorLoader;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import org.apache.commons.io.FileUtils;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_PICK = 1;
    static final int REQUEST_MANAGE_EXTERNAL_STORAGE_PERMISSION = 101; // 권한 요청 코드
    Uri selectedImageUri;
    ImageView imageView;
    Button selectImageButton, uploadButton, loadImageButton;
    JsonPlaceHolderApi jsonPlaceHolderApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        selectImageButton = findViewById(R.id.selectImageButton);
        uploadButton = findViewById(R.id.uploadButton);
        loadImageButton = findViewById(R.id.loadImageButton);

        // 권한 확인 및 요청
        checkPermissions();

        // Retrofit 설정
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://158.179.174.155:8000/") // 서버 URL을 자신의 서버로 수정
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        jsonPlaceHolderApi = retrofit.create(JsonPlaceHolderApi.class);

        // 이미지 선택 버튼 클릭 리스너
        selectImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQUEST_IMAGE_PICK);
        });

        // 업로드 버튼 클릭 리스너
        uploadButton.setOnClickListener(v -> {
            if (selectedImageUri != null) {
                try {
                    uploadImage(selectedImageUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(MainActivity.this, "이미지를 선택하세요", Toast.LENGTH_SHORT).show();
            }
        });

        // 최신 이미지 로드 버튼 클릭 리스너
        loadImageButton.setOnClickListener(v -> updateImageViewWithLatestImage());
    }

    // 권한 확인 메서드
    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                // 권한이 허용되지 않았을 때 권한 요청
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, REQUEST_MANAGE_EXTERNAL_STORAGE_PERMISSION);
            }
        }
    }

    // 권한 요청 결과 처리 메서드
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_MANAGE_EXTERNAL_STORAGE_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    // 권한이 허용된 경우
                    Toast.makeText(this, "권한 허용됨", Toast.LENGTH_SHORT).show();
                } else {
                    // 권한이 거부된 경우
                    Toast.makeText(this, "권한이 거부되었습니다. 이미지를 업로드할 수 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            imageView.setImageURI(selectedImageUri);
        }
    }

    // 이미지 업로드 메서드
    private void uploadImage(Uri imageUri) throws IOException {
        File file = new File(getRealPathFromURI(imageUri));
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("image", file.getName(), requestFile);
        RequestBody description = RequestBody.create(MediaType.parse("text/plain"), "Image Upload");

        Call<ResponseBody> call = jsonPlaceHolderApi.uploadImage(body, description);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Upload Successful", Toast.LENGTH_SHORT).show();
                    // 다운로드 메서드 호출 제거
                } else {
                    Toast.makeText(MainActivity.this, "Upload failed: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Upload failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 최신 이미지 URL을 받아와서 이미지뷰에 표시
    private void updateImageViewWithLatestImage() {
        Call<String> call = jsonPlaceHolderApi.getLatestImageUrl();
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    String latestImageUrl ="http://158.179.174.155:8000" + response.body();
                    Log.d("MainActivity", "Latest Image URL: " + latestImageUrl);

                    // 피카소를 사용하여 이미지뷰에 이미지 로드
                    Picasso.get().load(latestImageUrl).into(imageView);
                    Toast.makeText(MainActivity.this, "Image Loaded", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to load image URL: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Failed to load image URL: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // URI에서 실제 파일 경로 가져오는 메서드
    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        CursorLoader loader = new CursorLoader(this, contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String result = cursor.getString(column_index);
        cursor.close();
        return result;
    }
}
