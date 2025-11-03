package com.example.imageviewdemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.imageviewdemo.databinding.ActivityMainBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private String site_url = "http://10.0.2.2:8000";  // 에뮬레이터용
    private CloadImage taskDownload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // View Binding 초기화
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 버튼 클릭 리스너 설정
        binding.btnLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickDownload();
            }
        });

        binding.btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickUpload();
            }
        });
    }

    private void onClickDownload() {
        if (taskDownload != null && taskDownload.getStatus() == AsyncTask.Status.RUNNING) {
            taskDownload.cancel(true);
        }

        binding.textView.setText("이미지 다운로드 중...");
        taskDownload = new CloadImage();
        taskDownload.execute(site_url + "/api_root/Post/");
        Toast.makeText(getApplicationContext(), "Download 시작", Toast.LENGTH_SHORT).show();
    }

    private void onClickUpload() {
        Toast.makeText(getApplicationContext(), "Upload (구현 예정)", Toast.LENGTH_LONG).show();
    }

    private class CloadImage extends AsyncTask<String, Integer, List<Bitmap>> {

        @Override
        protected List<Bitmap> doInBackground(String... urls) {
            List<Bitmap> bitmapList = new ArrayList<>();

            try {
                String apiUrl = urls[0];
                // ⚠️ 여기에 Django에서 생성한 토큰을 입력하세요!
                String token = "e3acd79499b1bdc8d155861abed9728849a5556f";

                URL urlAPI = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) urlAPI.openConnection();
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder result = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    is.close();

                    String strJson = result.toString();
                    JSONArray aryJson = new JSONArray(strJson);

                    // 배열 내 모든 이미지 다운로드
                    for (int i = 0; i < aryJson.length(); i++) {
                        JSONObject post_json = aryJson.getJSONObject(i);
                        String imageUrl = post_json.getString("image");

                        if (!imageUrl.equals("") && !imageUrl.equals("null")) {
                            URL myImageUrl = new URL(imageUrl);
                            HttpURLConnection imgConn = (HttpURLConnection) myImageUrl.openConnection();
                            InputStream imgStream = imgConn.getInputStream();
                            Bitmap imageBitmap = BitmapFactory.decodeStream(imgStream);

                            if (imageBitmap != null) {
                                bitmapList.add(imageBitmap);
                            }
                            imgStream.close();
                        }
                    }
                } else {
                    // 에러 처리
                    System.out.println("Response Code: " + responseCode);
                }

            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

            return bitmapList;
        }

        @Override
        protected void onPostExecute(List<Bitmap> images) {
            if (images == null || images.isEmpty()) {
                binding.textView.setText("불러올 이미지가 없습니다.\n서버와 토큰을 확인하세요.");
            } else {
                binding.textView.setText("이미지 로드 성공! (" + images.size() + "개)");

                ImageAdapter adapter = new ImageAdapter(images);
                binding.recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                binding.recyclerView.setAdapter(adapter);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}