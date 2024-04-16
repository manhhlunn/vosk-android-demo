package org.vosk.demo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.media.AudioAttributes;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;

import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.StorageService;

import java.io.IOException;
import java.util.Objects;

public class AudioCaptureService extends Service implements RecognitionListener {

    public static boolean isAppInForeground = false;
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private MySpeechService speechService;

    private void initModel() {
        AudioPlaybackCaptureConfiguration config = new AudioPlaybackCaptureConfiguration
                .Builder(mediaProjection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .build();
        StorageService.unpack(this, LanguageModel.current(this).path(), "model",
                (model) -> startAudioCapture(model, config),
                (exception) -> {
                    Log.e("AAA", "initModel:" + exception.getMessage());
                    stopSelf();
                });
    }

    MyTranslateAPI firstTrans;
    MyTranslateAPI secondTrans;


    @Override
    public void onCreate() {
        super.onCreate();
        isAppInForeground = true;
        LibVosk.setLogLevel(LogLevel.INFO);
        createNotificationChannel();
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        firstTrans = new MyTranslateAPI(LanguageModel.current(this).code(), TranslateModel.current(this).code());
        firstTrans.setTranslateListener(new MyTranslateAPI.TranslateListener() {
            @Override
            public void onSuccess(String translatedText) {
                String text;
                if (prevText.isEmpty()) {
                    text = translatedText;
                } else {
                    text = prevText + ".\n" + translatedText;
                }
                textView.setText(text);
            }

            @Override
            public void onFailure(String ErrorText) {
            }
        });
        secondTrans = new MyTranslateAPI(LanguageModel.current(this).code(), TranslateModel.current(this).code());
        secondTrans.setTranslateListener(new MyTranslateAPI.TranslateListener() {
            @Override
            public void onSuccess(String translatedText) {
                prevText = translatedText;
                textView.setText(translatedText);
            }

            @Override
            public void onFailure(String ErrorText) {
            }
        });
        startForeground(SERVICE_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
    }

    private Notification createNotification() {
        PendingIntent notifyPendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Audio Capture Service is running")
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(notifyPendingIntent)
                .build();
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Audio Capture Service Channel",
                NotificationManager.IMPORTANCE_MIN
        );

        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null && action.equals(ACTION_START)) {
                mediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, Objects.requireNonNull(intent.getParcelableExtra(EXTRA_RESULT_DATA)));
                initModel();
            } else if (action != null && action.equals(ACTION_STOP)) {
                stopAudioCapture();
            }
        }

        return Service.START_NOT_STICKY;
    }

    private void startAudioCapture(Model model, AudioPlaybackCaptureConfiguration config) {
        try {
            initOverlay();
            Recognizer rec = new Recognizer(model, 16000.0f);
            speechService = new MySpeechService(rec, config, 16000.0f);
            speechService.startListening(this);
        } catch (IOException e) {
            Log.d("AAA", "startAudioCapture:" + e.getMessage());
            stopSelf();
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        isAppInForeground = false;
        if (mView != null) {
            windowManager.removeView(mView);
        }
        if (speechService != null) {
            speechService.stop();
            speechService.shutdown();
        }
    }

    private void stopAudioCapture() {
        if (speechService != null) {
            speechService.stop();
            speechService = null;
        }

        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }

        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private WindowManager windowManager;
    private View mView;
    private TextView textView;
    private String prevText = "";
    WindowManager.LayoutParams params;

    private String extractText(String jsonString) {
        int startIndex = jsonString.indexOf(":") + 3;
        int endIndex = jsonString.lastIndexOf("\"");
        return jsonString.substring(startIndex, endIndex);
    }

    @SuppressLint({"ClickableViewAccessibility", "InflateParams"})
    private void initOverlay() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        LayoutInflater mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mView = mInflater.inflate(R.layout.overlay, null);
        View rootView = mView.findViewById(R.id.rootView);
        textView = mView.findViewById(R.id.textView);
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        rootView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX
                                + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY
                                + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(mView, params);
                        return true;
                }
                return false;
            }
        });
        windowManager.addView(mView, params);
    }

    private static final int SERVICE_ID = 123;
    private static final String NOTIFICATION_CHANNEL_ID = "AudioCapture channel";
    public static final String ACTION_START = "AudioCaptureService:Start";
    public static final String ACTION_STOP = "AudioCaptureService:Stop";
    public static final String EXTRA_RESULT_DATA = "AudioCaptureService:Extra:ResultData";

    @Override
    public void onPartialResult(String hypothesis) {
        String text = extractText(hypothesis);
        if (text.isEmpty()) {
            textView.setText(prevText);
        } else {
            firstTrans.translate(text);
        }
    }

    @Override
    public void onResult(String hypothesis) {
        String text = extractText(hypothesis);
        if (text.isEmpty()) {
            prevText = text;
            textView.setText(text);
        } else {
            secondTrans.translate(text);
        }
    }

    @Override
    public void onFinalResult(String hypothesis) {

    }

    @Override
    public void onError(Exception exception) {
        stopSelf();
    }

    @Override
    public void onTimeout() {
        Log.d("AAA", "onTimeout");
        stopSelf();
    }
}



