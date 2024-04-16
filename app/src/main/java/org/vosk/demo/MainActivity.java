package org.vosk.demo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Objects;

public class MainActivity extends Activity {

    private static final int PERMISSIONS_REQUEST = 1;
    private static final int MEDIA_PROJECTION_REQUEST = 2;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MEDIA_PROJECTION_REQUEST) {
            if (resultCode == RESULT_OK && data != null) {
                Intent audioCaptureIntent = new Intent(this, AudioCaptureService.class);
                audioCaptureIntent.setAction(AudioCaptureService.ACTION_START);
                audioCaptureIntent.putExtra(AudioCaptureService.EXTRA_RESULT_DATA, data);
                startForegroundService(audioCaptureIntent);
                setupView(true);
            }
        }
    }

    private void setupView(boolean isRunning) {
        button.setSelected(isRunning);
        for (int i = 0; i < radioGroup.getChildCount(); i++) {
            radioGroup.getChildAt(i).setEnabled(!isRunning);
        }
        for (int i = 0; i < radioGroup2.getChildCount(); i++) {
            radioGroup2.getChildAt(i).setEnabled(!isRunning);
        }
        button.setText(isRunning ? R.string.stop : R.string.start);
        if (isRunning) {
            button.setOnClickListener(v -> stop());
        } else {
            button.setOnClickListener(v -> start());
        }
    }

    private void requestPermission() {
        Intent mediaProjectionIntent = ((MediaProjectionManager)
                getSystemService(Context.MEDIA_PROJECTION_SERVICE))
                .createScreenCaptureIntent();
        startActivityForResult(mediaProjectionIntent, MEDIA_PROJECTION_REQUEST);
    }

    private void start() {
        if (!Settings.canDrawOverlays(this)) {
            Intent myIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            startActivity(myIntent);
        } else {
            String[] permissions = {Manifest.permission.RECORD_AUDIO};
            int recordPermissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions = new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS};
            }

            if (recordPermissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST);
            } else {
                requestPermission();
            }
        }
    }

    private void stop() {
        Intent audioCaptureIntent = new Intent(this, AudioCaptureService.class);
        audioCaptureIntent.setAction(AudioCaptureService.ACTION_STOP);
        startForegroundService(audioCaptureIntent);
        setupView(false);
    }

    RadioGroup radioGroup;
    RadioGroup radioGroup2;
    Button button;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.main);
        LanguageModel modelId = LanguageModel.current(this);
        TranslateModel translateModel = TranslateModel.current(this);
        radioGroup = findViewById(R.id.radioGroup);
        radioGroup2 = findViewById(R.id.radioGroup2);
        radioGroup.check(modelId.radioId());
        radioGroup2.check(translateModel.radioId());
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> LanguageModel.saveIndex(this, Objects.requireNonNull(LanguageModel.fromId(checkedId)).ordinal()));
        radioGroup2.setOnCheckedChangeListener((group, checkedId) -> TranslateModel.saveIndex(this, Objects.requireNonNull(TranslateModel.fromId(checkedId)).ordinal()));
        button = findViewById(R.id.button);
        setupView(AudioCaptureService.isAppInForeground);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestPermission();
            }
        }
    }
}
