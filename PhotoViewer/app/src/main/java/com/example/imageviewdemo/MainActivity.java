package com.example.imageviewdemo;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.imageviewdemo.databinding.ActivityMainBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    // 로컬 사이트
    // private String site_url = "http://10.0.2.2:8000";

    // 파이썬 애니웨어 사이트
    private String site_url = "https://ksw090711.pythonanywhere.com";
    private String token = "e3acd79499b1bdc8d155861abed9728849a5556f";
    private CloadImage taskDownload;

    private static final int PICK_IMAGE_REQUEST = 100;
    private Uri selectedImageUri;
    private AlertDialog uploadDialog;
    
    private PostAdapter postAdapter;
    private boolean isAscending = false;
    private boolean isDarkMode = false;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 다크모드 설정 복원
        sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        isDarkMode = sharedPreferences.getBoolean("isDarkMode", false);
        applyTheme();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 동기화 버튼
        binding.btnLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickDownload();
            }
        });

        // 새 이미지 게시 버튼
        binding.btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickUpload();
            }
        });

        // 정렬 버튼
        binding.btnSort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSort();
            }
        });

        // 다크모드 버튼
        binding.btnDarkMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleDarkMode();
            }
        });

        // 검색 기능
        binding.searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (postAdapter != null) {
                    postAdapter.filter(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        updateDarkModeButtonText();
    }

    private void applyTheme() {
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void toggleDarkMode() {
        isDarkMode = !isDarkMode;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isDarkMode", isDarkMode);
        editor.apply();
        
        applyTheme();
        updateDarkModeButtonText();
    }

    private void updateDarkModeButtonText() {
        if (isDarkMode) {
            binding.btnDarkMode.setText("라이트모드");
        } else {
            binding.btnDarkMode.setText("다크모드");
        }
    }

    private void toggleSort() {
        if (postAdapter != null) {
            isAscending = !isAscending;
            if (isAscending) {
                postAdapter.sortByDateAscending();
                binding.btnSort.setText("오래된순");
                Toast.makeText(this, "오래된순으로 정렬", Toast.LENGTH_SHORT).show();
            } else {
                postAdapter.sortByDateDescending();
                binding.btnSort.setText("최신순");
                Toast.makeText(this, "최신순으로 정렬", Toast.LENGTH_SHORT).show();
            }
        }
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
        showUploadDialog();
    }

    private void showUploadDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.upload_dialog, null);
        builder.setView(dialogView);

        ImageView previewImage = dialogView.findViewById(R.id.preview_image);
        Button btnSelectImage = dialogView.findViewById(R.id.btn_select_image);
        EditText editTitle = dialogView.findViewById(R.id.edit_title);
        EditText editText = dialogView.findViewById(R.id.edit_text);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnUpload = dialogView.findViewById(R.id.btn_upload);

        uploadDialog = builder.create();

        // 이미지 선택 버튼
        btnSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, PICK_IMAGE_REQUEST);
            }
        });

        // 취소 버튼
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadDialog.dismiss();
                selectedImageUri = null;
            }
        });

        // 업로드 버튼
        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = editTitle.getText().toString().trim();
                String text = editText.getText().toString().trim();

                if (selectedImageUri == null) {
                    Toast.makeText(MainActivity.this, "이미지를 선택해주세요", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (title.isEmpty()) {
                    Toast.makeText(MainActivity.this, "제목을 입력해주세요", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (text.isEmpty()) {
                    Toast.makeText(MainActivity.this, "내용을 입력해주세요", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 업로드 시작
                new UploadPost().execute(title, text);
                uploadDialog.dismiss();
            }
        });

        uploadDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();

            if (uploadDialog != null && uploadDialog.isShowing()) {
                ImageView previewImage = uploadDialog.findViewById(R.id.preview_image);
                if (previewImage != null) {
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                        previewImage.setImageBitmap(bitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private class CloadImage extends AsyncTask<String, Integer, List<Post>> {

        @Override
        protected List<Post> doInBackground(String... urls) {
            List<Post> postList = new ArrayList<>();

            try {
                String apiUrl = urls[0];
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

                    for (int i = 0; i < aryJson.length(); i++) {
                        JSONObject post_json = aryJson.getJSONObject(i);
                        
                        int id = post_json.optInt("id", i);
                        String title = post_json.optString("title", "제목 없음");
                        String text = post_json.optString("text", "");
                        String imageUrl = post_json.optString("image", "");
                        String createdDate = post_json.optString("published_date", 
                                            post_json.optString("created_date", ""));

                        Bitmap imageBitmap = null;
                        if (!imageUrl.isEmpty() && !imageUrl.equals("null")) {
                            try {
                                URL myImageUrl = new URL(imageUrl);
                                HttpURLConnection imgConn = (HttpURLConnection) myImageUrl.openConnection();
                                InputStream imgStream = imgConn.getInputStream();
                                imageBitmap = BitmapFactory.decodeStream(imgStream);
                                imgStream.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        if (imageBitmap != null) {
                            Post post = new Post(id, title, text, imageUrl, imageBitmap, createdDate);
                            postList.add(post);
                        }
                    }
                }

            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

            return postList;
        }

        @Override
        protected void onPostExecute(List<Post> posts) {
            if (posts == null || posts.isEmpty()) {
                binding.textView.setText("불러올 이미지가 없습니다.\n서버와 토큰을 확인하세요.");
            } else {
                binding.textView.setText("이미지 로드 성공! (" + posts.size() + "개)");

                postAdapter = new PostAdapter(posts);
                binding.recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                binding.recyclerView.setAdapter(postAdapter);

                // 아이템 클릭 리스너 (이미지 확대 보기)
                postAdapter.setOnItemClickListener(new PostAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(Post post) {
                        Intent intent = new Intent(MainActivity.this, ImageDetailActivity.class);
                        intent.putExtra("title", post.getTitle());
                        intent.putExtra("text", post.getText());
                        intent.putExtra("imageUrl", post.getImageUrl());
                        startActivity(intent);
                    }
                });

                // 삭제 버튼 리스너
                postAdapter.setOnDeleteClickListener(new PostAdapter.OnDeleteClickListener() {
                    @Override
                    public void onDeleteClick(Post post, int position) {
                        showDeleteConfirmDialog(post, position);
                    }
                });
            }
        }
    }

    private void showDeleteConfirmDialog(Post post, int position) {
        new AlertDialog.Builder(this)
                .setTitle("게시물 삭제")
                .setMessage("\"" + post.getTitle() + "\"을(를) 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> {
                    new DeletePostTask(post, position).execute();
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private class UploadPost extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            binding.textView.setText("업로드 중...");
        }

        @Override
        protected String doInBackground(String... params) {
            String title = params[0];
            String text = params[1];

            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";

            try {
                URL url = new URL(site_url + "/api_root/Post/");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

                // author 필드
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"author\"" + lineEnd);
                dos.writeBytes(lineEnd);
                dos.writeBytes("1" + lineEnd);  // author ID

                // title 필드
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"title\"" + lineEnd);
                dos.writeBytes(lineEnd);
                dos.writeBytes(title + lineEnd);

                // text 필드
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"text\"" + lineEnd);
                dos.writeBytes(lineEnd);
                dos.writeBytes(text + lineEnd);

                // image 파일
                String filename = getFileName(selectedImageUri);
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"image\";filename=\"" + filename + "\"" + lineEnd);
                dos.writeBytes(lineEnd);

                InputStream fileInputStream = getContentResolver().openInputStream(selectedImageUri);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                }
                fileInputStream.close();

                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                dos.flush();
                dos.close();

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
                    return "업로드 성공!";
                } else {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    return "업로드 실패: " + responseCode + "\n" + errorResponse.toString();
                }

            } catch (Exception e) {
                e.printStackTrace();
                return "업로드 실패: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(MainActivity.this, result, Toast.LENGTH_LONG).show();
            if (result.contains("성공")) {
                // 업로드 성공 후 자동으로 새로고침
                onClickDownload();
            } else {
                binding.textView.setText(result);
            }
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private class DeletePostTask extends AsyncTask<Void, Void, Boolean> {
        private Post post;
        private int position;

        public DeletePostTask(Post post, int position) {
            this.post = post;
            this.position = position;
        }

        @Override
        protected void onPreExecute() {
            binding.textView.setText("삭제 중...");
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                String deleteUrl = site_url + "/api_root/Post/" + post.getId() + "/";
                URL url = new URL(deleteUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("DELETE");
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int responseCode = conn.getResponseCode();
                conn.disconnect();

                return responseCode == HttpURLConnection.HTTP_NO_CONTENT || 
                       responseCode == HttpURLConnection.HTTP_OK;

            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Toast.makeText(MainActivity.this, "삭제되었습니다.", Toast.LENGTH_SHORT).show();
                if (postAdapter != null) {
                    postAdapter.removeItem(position);
                }
                binding.textView.setText("게시물이 삭제되었습니다.");
            } else {
                Toast.makeText(MainActivity.this, "삭제 실패", Toast.LENGTH_SHORT).show();
                binding.textView.setText("삭제 실패");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}