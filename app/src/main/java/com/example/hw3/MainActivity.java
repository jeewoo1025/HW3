package com.example.hw3;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private BaseAdapterEx mAdapter;
    private ArrayList<Mp3File> mp3List;
    private ListView listView;

    // 액션 코드
    static final String STARTFOREGROUND_ACTION = "com.example.hw3.foregroundservice";

    /////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////

    public void getMp3Files() {
        final Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        final String[] projection = new String[] {
                MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATA,               // id, path
                MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.ALBUM_ID}; // 파일명, 앨범 id

        // .mp3 파일 확장자
        final String selection = MediaStore.Files.FileColumns.MIME_TYPE + "=?";
        final String[] selectionArgs = new String[] {MimeTypeMap.getSingleton().getMimeTypeFromExtension("mp3")};

        Cursor cursor = getContentResolver().query(uri, projection, selection, selectionArgs, null);
        while(cursor.moveToNext()) {
            String id = cursor.getString(0);            // _ID
            String data = cursor.getString(1);          // _DATA
            if(data.endsWith("mp3") == false)
                continue;

            String name = cursor.getString(2).replace("+", " ");  // _DISPLAY_NAME
            String album_id = cursor.getString(3);      // _ALBUM_ID

            mp3List.add(new Mp3File(id, data, name, album_id));
        }
        cursor.close();

        // 어댑터 생성/데이터 설정/리스트뷰에 어댑어 설정
        mAdapter = new BaseAdapterEx(MainActivity.this, mp3List);
        listView.setAdapter(mAdapter);

        // Started 서비스 인텐트 보내기
        Intent intent = new Intent(STARTFOREGROUND_ACTION);
        intent.setPackage("com.example.hw3");
        intent.putParcelableArrayListExtra("list", mp3List); // mp3 파일 정보
        startService(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 리스트뷰
        listView = (ListView)findViewById(R.id.listView);
        mp3List = new ArrayList<>();

        // 권한 check 하기
        if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else {
            getMp3Files();
        }

        // 리스트뷰 클릭 이벤트
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d("AAAA", "onItemClick : position - " + i);

                Intent intent = new Intent(MainActivity.this, PlayMusicActivity.class);
                intent.putExtra("position", i); // 위치 정보
                intent.putParcelableArrayListExtra("list", mp3List); // mp3 파일 정보
                startActivity(intent);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == 1) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getMp3Files();
            }
        }
    }
}