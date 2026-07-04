package com.example.stopclock;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class MainActivity extends AppCompatActivity {
    private static final long ALARM_BEEP_INTERVAL_MS = 900L;
    private static final long ALARM_BEEP_DURATION_MS = 250L;

    private MaterialCardView timerCard;
    private TextView timeText;
    private TextView statusText;
    private EditText customMinutesInput;
    private MaterialButton restartButton;
    private MaterialButton stopZeroButton;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private CountDownTimer countDownTimer;
    private ToneGenerator toneGenerator;
    private Vibrator vibrator;

    private long remainingMs = 0L;
    private long lastStartedDurationMs = 0L;
    private long endAtElapsedRealtime = 0L;
    private boolean timerRunning = false;
    private boolean alarmActive = false;

    private final Runnable alarmRunnable = new Runnable() {
        @Override
        public void run() {
            if (!alarmActive) {
                return;
            }
            playBeep();
            handler.postDelayed(this, ALARM_BEEP_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindViews();
        setupListeners();
        vibrator = getSystemService(Vibrator.class);

        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        } else {
            updateUi();
        }
    }

    private void bindViews() {
        timerCard = findViewById(R.id.timerCard);
        timeText = findViewById(R.id.timeText);
        statusText = findViewById(R.id.statusText);
        customMinutesInput = findViewById(R.id.customMinutesInput);
        restartButton = findViewById(R.id.restartButton);
        stopZeroButton = findViewById(R.id.stopZeroButton);
    }

    private void setupListeners() {
        findViewById(R.id.preset1Button).setOnClickListener(v -> startCountdownMinutes(1));
        findViewById(R.id.preset3Button).setOnClickListener(v -> startCountdownMinutes(3));
        findViewById(R.id.preset5Button).setOnClickListener(v -> startCountdownMinutes(5));
        findViewById(R.id.preset10Button).setOnClickListener(v -> startCountdownMinutes(10));
        findViewById(R.id.startCustomButton).setOnClickListener(v -> startCustomCountdown());
        restartButton.setOnClickListener(v -> restartTimer());
        stopZeroButton.setOnClickListener(v -> stopAndZero());
        findViewById(R.id.resetButton).setOnClickListener(v -> resetTimer());
    }

    private void restoreState(Bundle state) {
        remainingMs = state.getLong(KEY_REMAINING_MS, 0L);
        lastStartedDurationMs = state.getLong(KEY_LAST_DURATION_MS, 0L);
        endAtElapsedRealtime = state.getLong(KEY_END_AT, 0L);
        timerRunning = state.getBoolean(KEY_TIMER_RUNNING, false);
        alarmActive = state.getBoolean(KEY_ALARM_ACTIVE, false);
        customMinutesInput.setText(state.getString(KEY_INPUT_TEXT, ""));

        if (alarmActive) {
            startAlarm();
        } else if (timerRunning) {
            long restoredRemaining = Math.max(0L, endAtElapsedRealtime - SystemClock.elapsedRealtime());
            if (restoredRemaining <= 0L) {
                triggerAlarm();
            } else {
                startTimer(restoredRemaining);
            }
        } else {
            updateUi();
        }
    }

    private void startCustomCountdown() {
        Editable text = customMinutesInput.getText();
        if (text == null || TextUtils.isEmpty(text.toString().trim())) {
            customMinutesInput.setError(getString(R.string.error_enter_minutes));
            return;
        }

        try {
            int minutes = Integer.parseInt(text.toString().trim());
            if (minutes <= 0) {
                customMinutesInput.setError(getString(R.string.error_positive_minutes));
                return;
            }
            customMinutesInput.setError(null);
            startCountdownMinutes(minutes);
        } catch (NumberFormatException ex) {
            customMinutesInput.setError(getString(R.string.error_valid_number));
        }
    }

    private void startCountdownMinutes(int minutes) {
        startTimer(minutes * 60_000L);
        Toast.makeText(this, getString(R.string.started_countdown, minutes), Toast.LENGTH_SHORT).show();
    }

    private void startTimer(long durationMs) {
        stopAlarm();
        cancelTimerOnly();

        timerRunning = true;
        alarmActive = false;
        lastStartedDurationMs = Math.max(1L, durationMs);
        remainingMs = lastStartedDurationMs;
        endAtElapsedRealtime = SystemClock.elapsedRealtime() + remainingMs;

        countDownTimer = new CountDownTimer(remainingMs, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingMs = millisUntilFinished;
                updateUi();
            }

            @Override
            public void onFinish() {
                remainingMs = 0L;
                timerRunning = false;
                countDownTimer = null;
                updateUi();
                triggerAlarm();
            }
        }.start();

        updateUi();
    }

    private void triggerAlarm() {
        timerRunning = false;
        remainingMs = 0L;
        startAlarm();
    }

    private void startAlarm() {
        alarmActive = true;
        updateUi();
        handler.removeCallbacks(alarmRunnable);
        handler.post(alarmRunnable);
        vibrate();
    }

    private void stopAlarm() {
        alarmActive = false;
        handler.removeCallbacks(alarmRunnable);
        stopTone();
        updateUi();
    }

    private void resetTimer() {
        cancelTimerOnly();
        stopAlarm();
        remainingMs = 0L;
        endAtElapsedRealtime = 0L;
        timerRunning = false;
        customMinutesInput.setError(null);
        updateUi();
    }

    private void restartTimer() {
        if (lastStartedDurationMs <= 0L) {
            Toast.makeText(this, R.string.error_enter_minutes, Toast.LENGTH_SHORT).show();
            return;
        }
        startTimer(lastStartedDurationMs);
    }

    private void stopAndZero() {
        cancelTimerOnly();
        stopAlarm();
        remainingMs = 0L;
        endAtElapsedRealtime = 0L;
        timerRunning = false;
        updateUi();
    }

    private void cancelTimerOnly() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        timerRunning = false;
    }

    private void updateUi() {
        timeText.setText(formatTime(remainingMs));
        if (alarmActive) {
            statusText.setText(R.string.status_alarm);
            timerCard.setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.timer_alarm_stroke));
            timerCard.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#FB7185")));
            timerCard.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#2A1220")));
        } else {
            if (timerRunning) {
                statusText.setText(R.string.status_counting_down);
            } else {
                statusText.setText(R.string.status_idle);
            }
            timerCard.setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.timer_normal_stroke));
            timerCard.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#2DD4BF")));
            timerCard.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#102033")));
        }
        restartButton.setEnabled(lastStartedDurationMs > 0L);
        stopZeroButton.setEnabled(alarmActive || timerRunning || remainingMs > 0L || lastStartedDurationMs >= 0L);
    }

    private String formatTime(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void ensureToneGenerator() {
        if (toneGenerator == null) {
            toneGenerator = new ToneGenerator(android.media.AudioManager.STREAM_ALARM, 90);
        }
    }

    private void playBeep() {
        try {
            ensureToneGenerator();
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, (int) ALARM_BEEP_DURATION_MS);
        } catch (RuntimeException ignored) {
            Toast.makeText(this, R.string.error_audio, Toast.LENGTH_SHORT).show();
        }
    }

    private void stopTone() {
        if (toneGenerator != null) {
            toneGenerator.stopTone();
        }
    }

    @SuppressWarnings("deprecation")
    private void vibrate() {
        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(250L, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(250L);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!alarmActive) {
            return;
        }
        vibrate();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!alarmActive) {
            stopTone();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelTimerOnly();
        handler.removeCallbacks(alarmRunnable);
        stopTone();
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(KEY_REMAINING_MS, remainingMs);
        outState.putLong(KEY_LAST_DURATION_MS, lastStartedDurationMs);
        outState.putLong(KEY_END_AT, endAtElapsedRealtime);
        outState.putBoolean(KEY_TIMER_RUNNING, timerRunning);
        outState.putBoolean(KEY_ALARM_ACTIVE, alarmActive);
        CharSequence input = customMinutesInput.getText();
        outState.putString(KEY_INPUT_TEXT, input == null ? "" : input.toString());
    }

    private static final String KEY_REMAINING_MS = "remaining_ms";
    private static final String KEY_LAST_DURATION_MS = "last_duration_ms";
    private static final String KEY_END_AT = "end_at";
    private static final String KEY_TIMER_RUNNING = "timer_running";
    private static final String KEY_ALARM_ACTIVE = "alarm_active";
    private static final String KEY_INPUT_TEXT = "input_text";
}
