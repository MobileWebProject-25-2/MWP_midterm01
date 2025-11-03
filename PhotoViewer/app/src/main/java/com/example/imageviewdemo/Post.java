package com.example.imageviewdemo;

import android.graphics.Bitmap;

public class Post {
    private int id;
    private String title;
    private String text;
    private String imageUrl;
    private Bitmap imageBitmap;
    private String createdDate;

    public Post(int id, String title, String text, String imageUrl, Bitmap imageBitmap, String createdDate) {
        this.id = id;
        this.title = title;
        this.text = text;
        this.imageUrl = imageUrl;
        this.imageBitmap = imageBitmap;
        this.createdDate = createdDate;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Bitmap getImageBitmap() {
        return imageBitmap;
    }

    public String getCreatedDate() {
        return createdDate;
    }
}

