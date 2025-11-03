package com.example.imageviewdemo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.imageviewdemo.databinding.ActivityImageDetailBinding;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ImageDetailActivity extends AppCompatActivity {

    private ActivityImageDetailBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityImageDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Intent에서 데이터 가져오기
        String title = getIntent().getStringExtra("title");
        String text = getIntent().getStringExtra("text");
        String imageUrl = getIntent().getStringExtra("imageUrl");

        // 제목 설정
        if (title != null && !title.isEmpty()) {
            binding.detailTitle.setText(title);
        } else {
            binding.detailTitle.setText("제목 없음");
        }

        // 본문 내용 설정
        if (text != null && !text.isEmpty()) {
            binding.detailText.setText(text);
        } else {
            binding.detailText.setText("내용이 없습니다.");
        }

        // 이미지 로드
        if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.equals("null")) {
            new LoadImageTask().execute(imageUrl);
        } else {
            binding.progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "이미지를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
        }

        // 닫기 버튼
        binding.closeButton.setOnClickListener(v -> finish());

        // 이미지 클릭 시 확대/축소 (선택적)
        binding.detailImage.setOnClickListener(v -> {
            // 이미지 클릭 시 전체화면으로 볼 수 있는 기능 추가 가능
            Toast.makeText(ImageDetailActivity.this, "이미지를 확대하여 보고 있습니다", Toast.LENGTH_SHORT).show();
        });
    }

    private class LoadImageTask extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected void onPreExecute() {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.detailImage.setVisibility(View.GONE);
        }

        @Override
        protected Bitmap doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                input.close();
                connection.disconnect();
                return bitmap;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            binding.progressBar.setVisibility(View.GONE);
            if (result != null) {
                binding.detailImage.setVisibility(View.VISIBLE);
                binding.detailImage.setImageBitmap(result);
            } else {
                Toast.makeText(ImageDetailActivity.this, "이미지 로드 실패", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}

