package com.example.hw3;

import android.content.ContentUris;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class Mp3File implements Parcelable {
    private String id;          // 파일 id
    private String data;        // 파일 path
    private String name;        // 파일명
    private String album_id;    // 앨범 id

    public Mp3File(String id, String data, String name, String album_id) {
        this.id = id;
        this.data = data;
        this.name = name;
        this.album_id = album_id;
    }

    protected Mp3File(Parcel in) {
        id = in.readString();
        data = in.readString();
        name = in.readString();
        album_id = in.readString();
    }

    public static final Creator<Mp3File> CREATOR = new Creator<Mp3File>() {
        @Override
        public Mp3File createFromParcel(Parcel in) {
            return new Mp3File(in);
        }

        @Override
        public Mp3File[] newArray(int size) {
            return new Mp3File[size];
        }
    };

    public String getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public String getData() {
        return data;
    }

    public Uri getAlbumArtUri() {
        Uri album_art = Uri.parse("content://media/external/audio/albumart");
        Uri art_uri = ContentUris.withAppendedId(album_art, Long.parseLong(album_id)); // album id 붙이기
        return art_uri;
    }

    public String getMp3Info() {  // 디버깅용
        StringBuilder str = new StringBuilder();
        str.append("id : " + id + "\n");
        str.append("data : " + data + "\n");
        str.append("name : " + name + "\n");
        str.append("album art : " + album_id + "\n");
        return str.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.data);
        dest.writeString(this.name);
        dest.writeString(this.album_id);
    }
}
