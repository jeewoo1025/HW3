package com.example.hw3;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.util.ArrayList;

public class ForegroundService extends Service {
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

    MsgRequestHandler mMsgRequestHandler = null;
    Messenger mMessengerService = null;   // 서비스 측 메신저
    static final int THE_END = 100;

    // Mp3 관련 변수
    private int mIdx;
    private ArrayList<Mp3File> mp3List;
    private MediaPlayer mPlayer;
    private boolean wasPlaying;

    // 채널 id, Notification id, Builder
    static String CHANNEL_ID = "1025";
    static int NOTIFICATION_ID = 1025;
    private NotificationCompat.Builder builder;
    private RemoteViews customLayout;

    // 리스너
    // 에러 발생시 메시지 출력 리스너
    MediaPlayer.OnErrorListener mOnError = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer player, int what, int extra) {
            String err = "OnError occured what = " + what + " , extra = " + extra;
            Log.d("AAAA", err);
            return false;
        }
    };

    /////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////

    public ForegroundService() {
        mIdx = 0;
        wasPlaying = false;

        mPlayer = new MediaPlayer();
        mPlayer.setOnErrorListener(mOnError);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("AAAA", "ForegroundService - onCreate()");

        // Notification Channel 등록
        createNotificationChannel();

        // 요청 메시지를 처리할 핸들러 객체 생성 및 처리 핸들러 등록
        mMsgRequestHandler = new MsgRequestHandler();
        mMessengerService = new Messenger((mMsgRequestHandler));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d("AAAA", "ForegroundService - onStartCommand()");

        // Started 서비스 인텐트 수신
        if(intent == null)
            return START_STICKY;

        String action = intent.getAction();
        if(action.equals(STARTFOREGROUND_ACTION)) {
            if(intent.hasExtra("list")) {
                mp3List = intent.getParcelableArrayListExtra("list");
                LoadMedia(0);
            } else {
                Log.d("AAAA", "ForegroundService - 정보가 없습니다");
            }
        } else if(action.equals(PLAY_ACTION)) {  // 재생 or 일시정지
            if(mPlayer.isPlaying() == false) {  // 재생으로 바꿈
                mPlayer.start();
                changePlayIcon(true);  // 알림창 내용 바꿈
            } else {  // 일시정지로 바꿈
                mPlayer.pause();
                changePlayIcon(false);
            }

            // 방송하기
            Intent bIntent = new Intent();
            bIntent.setAction(PLAY_ACTION);
            bIntent.putExtra("play", mPlayer.isPlaying());
            sendBroadcast(bIntent);
        } else if(action.equals(NEXTPLAY_ACTION) || action.equals(PREVPLAY_ACTION)) {  // 이전 혹은 다음 버튼
            wasPlaying = mPlayer.isPlaying();
            if(action.equals(PREVPLAY_ACTION)) {
                mIdx = (mIdx == 0 ? mp3List.size() - 1 : mIdx - 1);
            } else {
                mIdx = (mIdx == mp3List.size() - 1 ? 0 : mIdx + 1);
            }

            mPlayer.reset();
            if(LoadMedia(mIdx)) {
                if(wasPlaying)
                    mPlayer.start();
                updateNotification(mIdx);

                // 방송하기
                Intent bIntent = new Intent();
                bIntent.setAction(action);  // PREVPLAY_ACTION or NEXTPLAY_ACTION
                bIntent.putExtra("idx", mIdx);
                bIntent.putExtra("time", mPlayer.getDuration());
                sendBroadcast(bIntent);
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d("AAAA", "ForegroundService - onDestroy()");
        if(mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("AAAA", "Foreground Service - onBind() " + intent);
        return mMessengerService.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("AAAA", "Foreground Service - onUnbind() " + intent);
        return super.onUnbind(intent);
    }

    public boolean LoadMedia(int idx) {  // Mp3 파일 로드하기
        if(idx < 0)
            return false;

        try {
            mPlayer.setDataSource(mp3List.get(idx).getData());
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("AAAA", "LoadMedia : " + e.getMessage());
            return false;
        }

        if(Prepare() == false)
            return false;
        return true;
    }

    public boolean Prepare() {
        try {
            mPlayer.prepare();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    // 클라이언트에서 전달된 요청 메시지를 처리하는 클래스
    class MsgRequestHandler extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case REQUEST_FIRSTPLAY: {
                    Log.d("AAAA", "foreground - REQUEST_FIRSTPLAY : " + String.valueOf(msg.arg1));
                    mIdx = msg.arg1;

                    mPlayer.reset();  // MediaPlayer 초기화
                    if(LoadMedia(mIdx)) {
                        mPlayer.start();

                        createNotification(mp3List.get(mIdx));  // 알림 생성

                       // 응답 메시지 보내기 - arg1 : 시간
                        Log.d("AAAA", String.valueOf(mPlayer.getDuration()));
                        Message replyMsg = Message.obtain(null, RESPONSE_FIRSTPLAY, mPlayer.getDuration(), 0);
                        try {
                            msg.replyTo.send(replyMsg);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                            break;
                        }
                    }
                    break;
                }
                case REQUEST_PLAY: {
                    if(mPlayer.isPlaying() == false) { // 재생
                        mPlayer.start();
                        changePlayIcon(true);
                    } else {  // 일시정지
                        mPlayer.pause();
                        changePlayIcon(false);
                    }
                    break;
                }
                case REQUEST_PREVPLAY:
                case REQUEST_NEXTPLAY: {
                    wasPlaying = mPlayer.isPlaying();
                    if(msg.arg1 == THE_END) {  // PlayMusicActivity에서 음악이 끝났을 때의 인자
                        wasPlaying = true;
                    }

                    int replyWhat = 0;
                    if(msg.what == REQUEST_NEXTPLAY) {
                        mIdx = (mIdx == mp3List.size() - 1 ? 0 : mIdx + 1);
                        replyWhat = RESPONSE_NEXTPLAY;
                    }
                    else {
                        mIdx = (mIdx == 0 ? mp3List.size() - 1 : mIdx - 1);
                        replyWhat = RESPONSE_PREVPLAY;
                    }

                    mPlayer.reset();
                    if(LoadMedia(mIdx)) {
                        if(wasPlaying)
                            mPlayer.start();
                        updateNotification(mIdx);

                        // 응답 메시지 보내기 - arg1 : 파일 위치, arg2 : 재생시간
                        Message replyMsg = Message.obtain(null, replyWhat, mIdx, mPlayer.getDuration());
                        try {
                            msg.replyTo.send(replyMsg);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                }
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "hw3 설지우";
            String description = "hw3 설지우의 음악 플레이어";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void updateNotification(int i) {
        customLayout.setTextViewText(R.id.alarmTitle, mp3List.get(i).getName());   // 파일 이름

        // 앨범 사진
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), mp3List.get(i).getAlbumArtUri());
            customLayout.setImageViewBitmap(R.id.alarmAlbumArt, bitmap);
        } catch (IOException e) {
            Log.d("AAAA", e.getMessage());
            customLayout.setImageViewResource(R.id.alarmAlbumArt, R.drawable.song);
        }
        startForeground(NOTIFICATION_ID, builder.build());
    }

    public void changePlayIcon(boolean isPlaying) {  // 알림창에서 재생 및 일시정지 아이콘 바꾸기
        if(isPlaying) {  // 재생
            customLayout.setImageViewResource(R.id.alarmPlay, R.drawable.pause);
            builder.setSmallIcon(R.drawable.alarm_play);
        } else {  // 일시정지
            customLayout.setImageViewResource(R.id.alarmPlay, R.drawable.play);
            builder.setSmallIcon(R.drawable.alarm_pause);
        }
        startForeground(NOTIFICATION_ID, builder.build());
    }

    public void createNotification(Mp3File mp3File) {  // REQUEST_FIRSTPLAY 때 호출된다
        // Play 버튼 누르면 이후 동작
        Intent playIntent = new Intent(this, ForegroundService.class);
        playIntent.setAction(PLAY_ACTION);
        PendingIntent playPendingIntent = PendingIntent.getService(this, 0, playIntent, 0);

        // Next 버튼 누르면 이후 동작
        Intent nextIntent = new Intent(this, ForegroundService.class);
        nextIntent.setAction(NEXTPLAY_ACTION);
        PendingIntent nextPendingIntent = PendingIntent.getService(this, 0, nextIntent, 0);

        // Prev 버튼 누르면 이후 동작
        Intent prevIntent = new Intent(this, ForegroundService.class);
        prevIntent.setAction(PREVPLAY_ACTION);
        PendingIntent prevPendingIntent = PendingIntent.getService(this, 0, prevIntent, 0);

        // 앨범이미지 클릭시 PlayActivity 보여주기
        Intent moveIntent = new Intent(this, PlayMusicActivity.class);
        PendingIntent movePendingIntent = PendingIntent.getActivity(this, 0, moveIntent,0);

        // 알림창 구성
        customLayout = new RemoteViews(getPackageName(), R.layout.alarm);
        customLayout.setOnClickPendingIntent(R.id.alarmAlbumArt, movePendingIntent);    // 앨범 사진 클릭시
        customLayout.setOnClickPendingIntent(R.id.alarmPrev, prevPendingIntent);        // Prev 버튼 클릭시
        customLayout.setOnClickPendingIntent(R.id.alarmNext, nextPendingIntent);        // Next 버튼 클릭시
        customLayout.setOnClickPendingIntent(R.id.alarmPlay, playPendingIntent);        // Play 버튼 클릭시
        customLayout.setTextViewText(R.id.alarmTitle, mp3File.getName());               // 파일 이름

        // 파일 사진
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), mp3File.getAlbumArtUri());
            customLayout.setImageViewBitmap(R.id.alarmAlbumArt, bitmap);
        } catch (IOException e) {
            Log.d("AAAA", e.getMessage());
            customLayout.setImageViewResource(R.id.alarmAlbumArt, R.drawable.song);
        }

        // 알림창 생성 및 layout 적용
        builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.alarm_play)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(customLayout);
        startForeground(NOTIFICATION_ID, builder.build());
    }
}
