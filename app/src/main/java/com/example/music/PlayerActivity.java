package com.example.music;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;


public class PlayerActivity extends AppCompatActivity {
    private TextView songTitle;
    private ImageButton playPauseBtn, nextBtn, prevBtn;
    private SeekBar seekBar;
    private ArrayList<String> songList, songUris;
    private int currentIndex;
    private MusicService musicService;
    private boolean isBound = false;
    private TextView currentTimeText, totalTimeText; // Добавляем TextView для времени
    private Handler updateSeekBarHandler = new Handler();


    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;
            musicService.setSongList(songUris, currentIndex);
            updateUI();
            startSeekBarUpdate(); // Запускаем обновление SeekBar
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
        setContentView(R.layout.activity_player); // Стандартный макет (без YouTube Music)

        new DatabaseHelper(this);

        // Инициализация элементов
        currentTimeText = findViewById(R.id.currentTimeText);
        totalTimeText = findViewById(R.id.totalTimeText);
        songTitle = findViewById(R.id.songTitle);
        playPauseBtn = findViewById(R.id.playPauseButton);
        nextBtn = findViewById(R.id.nextButton);
        prevBtn = findViewById(R.id.prevButton);
        seekBar = findViewById(R.id.seekBar);

        // Получение данных
        songList = getIntent().getStringArrayListExtra("songList");
        songUris = getIntent().getStringArrayListExtra("songUris");
        currentIndex = getIntent().getIntExtra("currentIndex", 0);

        // Запуск сервиса
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);

        // Кнопки
        playPauseBtn.setOnClickListener(v -> togglePlayPause());
        nextBtn.setOnClickListener(v -> playNext());
        prevBtn.setOnClickListener(v -> playPrevious());

        // SeekBar
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

    private Runnable updateSeekBarRunnable = new Runnable() {
        @Override
        public void run() {
            if (isBound && musicService != null) {
                int currentPosition = musicService.getCurrentPosition();
                int duration = musicService.getDuration();

                seekBar.setProgress(currentPosition);
                seekBar.setMax(duration);

                // Обновляем текстовые поля времени
                currentTimeText.setText(formatTime(currentPosition));
                totalTimeText.setText(formatTime(duration));

                // Проверяем, достиг ли трек конца
                if (currentPosition >= duration - 500 && duration > 0) { // 500ms буфер
                    playNext();
                }

                updateSeekBarHandler.postDelayed(this, 500);
            }
        }
    };

    // Метод для форматирования времени (мм:сс)
    private String formatTime(int milliseconds) {
        int seconds = (milliseconds / 1000) % 60;
        int minutes = (milliseconds / (1000 * 60)) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }


    private void updateUI() {
        if (isBound) {
            songTitle.setText(songList.get(currentIndex));
            playPauseBtn.setImageResource(
                    musicService.isPlaying() ?
                            android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play
            );
            seekBar.setMax(musicService.getDuration());
            seekBar.setProgress(musicService.getCurrentPosition());
        }
    }

    // Запуск обновления SeekBar
    private void startSeekBarUpdate() {
        updateSeekBarHandler.postDelayed(updateSeekBarRunnable, 0);
    }

    // Остановка обновления SeekBar
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
        if (isBound && currentIndex < songList.size() - 1) {
            currentIndex++;
            musicService.play(currentIndex);
            updateUI();
        }
    }

    private void playPrevious() {
        if (isBound && currentIndex > 0) {
            currentIndex--;
            musicService.play(currentIndex);
            updateUI();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) unbindService(serviceConnection);
    }
}