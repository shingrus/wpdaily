package com.shingrus.wpdaily;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public final class ImageDescription {

    private byte[] data = null;
    private String url = null;
    private Integer insertedAt = -1;
    private  String linkPage = null;
    private  int id = -1;


    private static final String _log_tag = "WPD/ID";

    private final String provider;


    public final Integer getInsertedAt() {
        return insertedAt;
    }

    public final String getUrl() {
        return url;
    }

    public final byte[] getData() {
        return data;
    }

    public final String getProvider() {
        return provider;
    }

    public final int getID () {
        return id;
    }

    public final void setID(int id) {
        this.id = id;
    }


//    public void setData(byte[] data) {
//        this.data = data;
//    }

//    public void setUrl(String url) {
//        this.url = url;
//    }

//    public void setInsertedAt(Integer insertedAt) {
//        this.insertedAt = insertedAt;
//    }

    public void setLinkPage(String linkPage) {
        this.linkPage = linkPage;
    }

    public String getLinkPage() {
        return linkPage;
    }

    public ImageDescription(String url) {
        data = null;
        this.url = url;
        insertedAt  = -1;
        provider = "";
        linkPage = "";

    }

    public ImageDescription(String url, Integer insertedAt, String provider, String linkPage, byte[] data) {
        this.data = data;
        this.url = url;
        this.insertedAt = insertedAt;
        this.provider = provider;
        this.linkPage = linkPage;
    }
}
