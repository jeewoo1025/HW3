package com.example.hw3;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.ArrayList;

public class PlayMusicActivity extends AppCompatActivity {
    // 요청 메시지 코드
    static final int REQUEST_FIRSTPLAY = 1001;
    static final int REQUEST_PLAY = 1002;
    static final int REQUEST_NEXTPLAY = 1003;
    static final int REQUEST_PREVPLAY = 1004;

    // 응답 메시지 코드
    static final int RESPONSE_FIRSTPLAY = 2001;
    static final int RESPONSE_PLAY = 2002;
    static final int RESPONSE_NEXTPLAY = 2003;
    static final int RESPONSE_PREVPLAY = 2004;

    // 액션 코드
    static final String STARTFOREGROUND_ACTION = "com.example.hw3.foregroundservice";
    static final String PLAY_ACTION = "com.example.hw3.play";
    static final String NEXTPLAY_ACTION = "com.example.hw3.nextplay";
    static final String PREVPLAY_ACTION = "com.example.hw3.prevplay";

    // 메신저 : ForegroundService(서비스) - PlayMusicActivity(클라이언트)
    Messenger mMessengerService = null;   // 서비스 측 메신저
    Messenger mMessengerResponse = null;  // 클라이언트 측 메신저
    MsgResponseHandler mMsgResponseHandler = null;
    static final int THE_END = 100;       // 음악이 하나 끝남

    // 타이머
    ProgressTimer progressTimer = null;
    long remainMilli = 0, songMilli = 0;

    // Mp3 파일
    private ArrayList<Mp3File> mp3List;
    private int idx = -1;
    private boolean wasPlaying;

    // 이미지 뷰
    private ImageView imgPlayAlbum, btnPlay;
    private TextView txtPlayFile, txtPlayTime, txtTotalTime;
    private ProgressBar progressBar;

    // Broastcast Receiver
    private BroadcastReceiver broadcastReceiver;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d("AAAA", "onServiceConnected");
            mMessengerService = new Messenger(iBinder);

            // 처음 시작 메시지 보내기
            Message msg = Message.obtain(null, REQUEST_FIRSTPLAY, idx, 0); // position
            msg.replyTo = mMessengerResponse;  // 클라이언트 측 메신저 객체
            try {
                mMessengerService.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d("AAAA", "onService Disconnected");
            mMessengerService = null;
        }
    };

    /////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////

    public void processIntent(Intent intent) {  // 인텐트 처리
        if(intent != null) {
            if(intent.hasExtra("position") && intent.hasExtra("list")) {
                idx = intent.getIntExtra("position", 0);
                mp3List = intent.getParcelableArrayListExtra("list");
            } else {
                Log.d("AAAA", "정보가 없습니다");
                Toast.makeText(this, "정보가 없습니다", Toast.LENGTH_SHORT).show();
                return;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_music);

        // 뷰 초기화
        imgPlayAlbum = (ImageView)findViewById(R.id.imgPlayAlbum);
        btnPlay = (ImageView)findViewById(R.id.btnPlay);
        txtPlayFile = (TextView)findViewById(R.id.txtPlayFile);
        txtPlayTime = (TextView)findViewById(R.id.txtPlayTime);
        txtTotalTime = (TextView)findViewById(R.id.txtTotalTime);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        // 응답 메시지를 처리할 핸들러 객체 생성 및 처리 핸들러 등록
        mMsgResponseHandler = new MsgResponseHandler();
        mMessengerResponse = new Messenger((mMsgResponseHandler));

        // 인텐트 수신
        Intent intent = getIntent();
        processIntent(intent);

        // 서비스 바인딩
        Intent serviceIntent = new Intent(STARTFOREGROUND_ACTION);
        serviceIntent.setPackage("com.example.hw3");
        bindService(serviceIntent, mConnection, BIND_AUTO_CREATE);

        if(idx >= 0 && mp3List != null) {  // 뷰 set
            txtPlayFile.setText(mp3List.get(idx).getName());  // 파일명

            // 앨범 사진
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), mp3List.get(idx).getAlbumArtUri());
                imgPlayAlbum.setImageBitmap(bitmap);
            } catch (IOException e) {
                Log.d("AAAA", e.getMessage());
                imgPlayAlbum.setImageResource(R.drawable.song);
            }
        }

        // Broadcast Receiver 생성 및 정의
        IntentFilter filter = new IntentFilter();
        filter.addAction(PLAY_ACTION);     // 재생 or 일시정지
        filter.addAction(NEXTPLAY_ACTION); // 다음
        filter.addAction(PREVPLAY_ACTION); // 이전

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if(action.equals(PLAY_ACTION)) {
                    boolean isPlaying = intent.getBooleanExtra("play", false);
                    if(isPlaying) { // 재생으로 바꿈
                        btnPlay.setImageResource(R.drawable.pause);
                        wasPlaying = true;

                        resumeTime();   // 타이머 재개
                    } else { // 일시정지로 바꿈
                        btnPlay.setImageResource(R.drawable.play);
                        wasPlaying = false;

                        cancelTime();    // 타이머 종료
                    }
                } else if(action.equals(NEXTPLAY_ACTION) || action.equals(PREVPLAY_ACTION)) {
                    remainMilli = 0;
                    songMilli = 0;
                    cancelTime();  // 이전에 타이머가 있으면 종료

                    idx = intent.getIntExtra("idx", 0);
                    updateFileInfo(idx);

                    // 재생시간
                    int time = intent.getIntExtra("time", 0);
                    setTxtTime(time);
                }
            }
        };

        registerReceiver(broadcastReceiver, filter);
    }

    public void onClick(View view) {             // |<(이전) , ||(일시정지 or 재생), >|(다음) 버튼
        switch (view.getId()) {
            case R.id.btnPlay:
                try {
                    Message msg = Message.obtain(null, REQUEST_PLAY);
                    msg.replyTo = mMessengerResponse;  // 클라이언트 측 메신저 객체
                    mMessengerService.send(msg);

                    if(wasPlaying == false) {   // 재생으로 바꿈
                        btnPlay.setImageResource(R.drawable.pause);
                        wasPlaying = true;

                        resumeTime();  // 타이머 재개
                    } else {    // 일시정지로 바꿈
                        btnPlay.setImageResource(R.drawable.play);
                        wasPlaying = false;

                        cancelTime(); // 타이머 종료
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.btnPlaySkipNext:
            case R.id.btnPlaySkipPrev:
                remainMilli = 0;
                songMilli = 0;
                cancelTime();  // 이전에 타이머가 있으면 종료

                Message msg;
                if(view.getId() == R.id.btnPlaySkipNext)
                    msg = Message.obtain(null, REQUEST_NEXTPLAY,0 ,0 );
                else
                    msg = Message.obtain(null, REQUEST_PREVPLAY, 0, 0);
                msg.replyTo = mMessengerResponse;
                try {
                    mMessengerService.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        unbindService(mConnection);  // 서비스 unbind
        cancelTime();  // CountDownTimer 취소
        unregisterReceiver(broadcastReceiver);  // 브로드캐스트 리시버 해제
        super.onDestroy();
    }

    class MsgResponseHandler extends Handler {  // 응답 메시지 핸들러
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case RESPONSE_FIRSTPLAY: {
                    wasPlaying = true;
                    setTxtTime(msg.arg1);
                    break;
                }
                case RESPONSE_PREVPLAY:
                case RESPONSE_NEXTPLAY: {
                    updateFileInfo(msg.arg1);   // arg1 : 파일 index
                    idx = msg.arg1;

                    setTxtTime(msg.arg2);  // arg2 : 재생시간
                    break;
                }
            }
        }
    }

    public void updateFileInfo(int i) {  // 파일명(노래제목)과 파일 사진(ALBUM_ART) 업데이트
        txtPlayFile.setText(mp3List.get(i).getName());  // 파일명
        // 앨범 사진
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), mp3List.get(i).getAlbumArtUri());
            imgPlayAlbum.setImageBitmap(bitmap);
        } catch (IOException e) {
            Log.d("AAAA", e.getMessage());
            imgPlayAlbum.setImageResource(R.drawable.song);
        }
    }

    public void setTxtTime(int total) {  // 시간 : milli seconds
        progressBar.setMax(total);  // 최대값
        progressBar.setProgress(0); // 현재값

        songMilli = total;
        String min = String.format("%02d", (total/1000/60)%60);
        String sec = String.format("%02d", (total/1000)%60);
        txtTotalTime.setText(min + " : " + sec);
        txtPlayTime.setText("00 : 00");

        remainMilli = total;
        //Log.d("AAAA", "song : "  + songMilli + " , remain : " + remainMilli);

        if(wasPlaying)
            resumeTime();
    }

    public void resumeTime() {  // CountDownTimer 시작 (start)
        progressTimer = new ProgressTimer(remainMilli, 1000);
        progressTimer.start();
    }

    public void cancelTime() {  // CountDownTimer 정지 (cancel)
        if(progressTimer != null)
            progressTimer.cancel();
    }

    public void changeMilliToTime(long milli) {  // 밀리초 --> 분:초 로 변환하기
        String min = String.format("%02d", (milli/1000/60)%60);
        String sec = String.format("%02d", (milli/1000)%60);
        txtPlayTime.setText(min + " : " + sec);
    }

    // 카운드다운 Timer
    private class ProgressTimer extends CountDownTimer {
        public ProgressTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long l) {  // 1초마다 onTick 함수가 호출됨 (I : 현재남은시간)
            long nowMilli = (songMilli - l);
            changeMilliToTime(nowMilli);
            progressBar.setProgress((int)nowMilli);

            remainMilli = l;  // 현재 남은 시간 저장
        }

        @Override
        public void onFinish() {
            // 카운트다운이 완료되면 최종 초를 출력
            changeMilliToTime(songMilli);
            progressBar.setProgress((int)songMilli);

            // 다음곡 넘어가기
            remainMilli = 0;
            songMilli = 0;

            Message msg  = Message.obtain(null, REQUEST_NEXTPLAY, THE_END, 0);
            msg.replyTo = mMessengerResponse;
            try {
                mMessengerService.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}