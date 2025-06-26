package com.example.music;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;

public class PlayerActivity extends AppCompatActivity {
    private TextView songTitle;
    private ImageButton playPauseBtn, nextBtn, prevBtn, repeatButton, shuffleButton;
    private SeekBar seekBar;
    private ArrayList<String> songTitles, songUris;
    private MusicService musicService;
    private boolean isBound = false;
    private TextView currentTimeText, totalTimeText;
    private Handler updateSeekBarHandler = new Handler();
    private int currentMode = MusicService.MODE_NORMAL;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;
            musicService.setSongList(songTitles, songUris, getIntent().getIntExtra("currentIndex", 0));
            currentMode = musicService.getPlaybackMode();
            updateUI();
            updateModeButtons();
            startSeekBarUpdate();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            stopSeekBarUpdate();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        currentTimeText = findViewById(R.id.currentTimeText);
        totalTimeText = findViewById(R.id.totalTimeText);
        songTitle = findViewById(R.id.songTitle);
        playPauseBtn = findViewById(R.id.playPauseButton);
        nextBtn = findViewById(R.id.nextButton);
        prevBtn = findViewById(R.id.prevButton);
        seekBar = findViewById(R.id.seekBar);
        repeatButton = findViewById(R.id.repeatButton);
        shuffleButton = findViewById(R.id.shuffleButton);

        songTitles = getIntent().getStringArrayListExtra("songList");
        songUris = getIntent().getStringArrayListExtra("songUris");

        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);

        playPauseBtn.setOnClickListener(v -> togglePlayPause());
        nextBtn.setOnClickListener(v -> playNext());
        prevBtn.setOnClickListener(v -> playPrevious());

        repeatButton.setOnClickListener(v -> {
            if (isBound) {
                currentMode = musicService.getPlaybackMode();
                if (currentMode == MusicService.MODE_NORMAL) {
                    currentMode = MusicService.MODE_REPEAT_ALL;
                } else if (currentMode == MusicService.MODE_REPEAT_ALL) {
                    currentMode = MusicService.MODE_REPEAT_ONE;
                } else {
                    currentMode = MusicService.MODE_NORMAL;
                }
                musicService.setPlaybackMode(currentMode);
                updateModeButtons();
            }
        });

        shuffleButton.setOnClickListener(v -> {
            if (isBound) {
                currentMode = musicService.getPlaybackMode();
                if (currentMode == MusicService.MODE_SHUFFLE) {
                    currentMode = MusicService.MODE_NORMAL;
                } else {
                    currentMode = MusicService.MODE_SHUFFLE;
                }
                musicService.setPlaybackMode(currentMode);
                updateModeButtons();
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && isBound) {
                    musicService.seekTo(progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void updateModeButtons() {
        if (isBound) {
            currentMode = musicService.getPlaybackMode();
        }

        repeatButton.setColorFilter(Color.GRAY);
        shuffleButton.setColorFilter(Color.GRAY);

        switch (currentMode) {
            case MusicService.MODE_REPEAT_ALL:
                repeatButton.setImageResource(R.drawable.ic_repeat);
                repeatButton.setColorFilter(ContextCompat.getColor(this, R.color.colorAccent));
                break;
            case MusicService.MODE_REPEAT_ONE:
                repeatButton.setImageResource(R.drawable.ic_repeat_one);
                repeatButton.setColorFilter(ContextCompat.getColor(this, R.color.colorAccent));
                break;
            case MusicService.MODE_SHUFFLE:
                shuffleButton.setColorFilter(ContextCompat.getColor(this, R.color.colorAccent));
                break;
        }
    }

    private Runnable updateSeekBarRunnable = new Runnable() {
        @Override
        public void run() {
            if (isBound && musicService != null) {
                int currentPosition = musicService.getCurrentPosition();
                int duration = musicService.getDuration();

                seekBar.setProgress(currentPosition);
                seekBar.setMax(duration);

                currentTimeText.setText(formatTime(currentPosition));
                totalTimeText.setText(formatTime(duration));

                if (currentPosition >= duration - 500 && duration > 0) {
                    if (currentMode != MusicService.MODE_REPEAT_ONE) {
                        playNext();
                    }
                }

                updateSeekBarHandler.postDelayed(this, 500);
            }
        }
    };

    public void PlayerBtnBack_Click(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private String formatTime(int milliseconds) {
        int seconds = (milliseconds / 1000) % 60;
        int minutes = (milliseconds / (1000 * 60)) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void updateUI() {
        if (isBound && musicService.getCurrentSong() != null) {
            songTitle.setText(musicService.getCurrentSong().title);
            playPauseBtn.setImageResource(
                    musicService.isPlaying() ?
                            android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play
            );
            seekBar.setMax(musicService.getDuration());
            seekBar.setProgress(musicService.getCurrentPosition());
        }
    }

    private void startSeekBarUpdate() {
        updateSeekBarHandler.postDelayed(updateSeekBarRunnable, 0);
    }

    private void stopSeekBarUpdate() {
        updateSeekBarHandler.removeCallbacks(updateSeekBarRunnable);
    }

    private void togglePlayPause() {
        if (isBound) {
            musicService.playPause();
            updateUI();
        }
    }

    private void playNext() {
        if (isBound) {
            musicService.playNext();
            updateUI();
        }
    }

    private void playPrevious() {
        if (isBound) {
            musicService.playPrevious();
            updateUI();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
        }
    }
}