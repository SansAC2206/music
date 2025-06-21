package com.example.music;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class PlaylistDetailActivity extends AppCompatActivity {
    private TextView playlistTitle;
    private ListView songsListView;
    private ListView availableSongsListView;
    private EditText songNameEditText;
    private Button addSongBtn;
    private Button searchSongsBtn;
    private ArrayAdapter<String> songsAdapter;
    private ArrayAdapter<String> availableSongsAdapter;
    private ArrayList<String> songs = new ArrayList<>();
    private ArrayList<String> availableSongs = new ArrayList<>();
    private DatabaseHelper dbHelper;
    private String playlistName;
    private long userId;
    private long playlistId;

    // Для воспроизведения музыки
    private MediaPlayer mediaPlayer;
    private int currentSongIndex = -1;
    private ArrayList<String> songUris = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_detail);

        // Получаем данные из Intent
        Intent intent = getIntent();
        playlistName = intent.getStringExtra("playlistName");
        userId = intent.getLongExtra("userId", -1);

        if (playlistName == null || userId == -1) {
            Toast.makeText(this, "Ошибка: данные плейлиста не получены", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbHelper = new DatabaseHelper(this);
        mediaPlayer = new MediaPlayer();

        // Инициализация UI элементов
        playlistTitle = findViewById(R.id.playlistTitle);
        songsListView = findViewById(R.id.songsListView);
        availableSongsListView = findViewById(R.id.availableSongsListView);
        songNameEditText = findViewById(R.id.songNameEditText);
        addSongBtn = findViewById(R.id.addSongBtn);
        searchSongsBtn = findViewById(R.id.searchSongsBtn);

        // Устанавливаем название плейлиста
        playlistTitle.setText(playlistName);

        // Получаем ID плейлиста
        playlistId = dbHelper.getPlaylistId(playlistName, userId);
        if (playlistId == -1) {
            Toast.makeText(this, "Ошибка: плейлист не найден", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Загружаем песни плейлиста
        loadPlaylistSongs();
        loadAvailableSongs();

        // Настройка адаптеров
        songsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, songs);
        songsListView.setAdapter(songsAdapter);

        availableSongsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, availableSongs);
        availableSongsListView.setAdapter(availableSongsAdapter);

        // Обработчик добавления новой песни по имени
        addSongBtn.setOnClickListener(v -> {
            String songName = songNameEditText.getText().toString().trim();
            if (!songName.isEmpty()) {
                addSongToPlaylist(songName);
            } else {
                Toast.makeText(this, "Введите название песни", Toast.LENGTH_SHORT).show();
            }
        });

        // Обработчик поиска песен
        searchSongsBtn.setOnClickListener(v -> {
            loadAvailableSongs();
        });

        // Обработчик добавления песни из списка доступных
        availableSongsListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedSong = availableSongs.get(position);
            addSongToPlaylist(selectedSong);
        });

        // Обработчик клика по песне в плейлисте (воспроизведение)
        songsListView.setOnItemClickListener((parent, view, position, id) -> {
            playSong(position);
        });
    }

    private void loadPlaylistSongs() {
        songs.clear();
        songs.addAll(dbHelper.getSongsForPlaylist(playlistId));

        // Загружаем URI песен для воспроизведения
        songUris.clear();
//        for (String songName : songs) {
//            String uri = dbHelper.getMediaUriByTitle(songName, userId);
//            songUris.add(uri);
//        }

        if (songsAdapter != null) {
            songsAdapter.notifyDataSetChanged();
        }
    }


    private void loadAvailableSongs() {
        availableSongs.clear();
        // Получаем все песни из медиатеки пользователя
        availableSongs.addAll(dbHelper.getAllMediaTitles(userId));
        // Убираем песни, которые уже есть в плейлисте
        availableSongs.removeAll(songs);

        if (availableSongsAdapter != null) {
            availableSongsAdapter.notifyDataSetChanged();
        }
    }

    private void addSongToPlaylist(String songName) {
        long songId = dbHelper.addSongToPlaylist(playlistId, songName);
        if (songId != -1) {
            songs.add(songName);
//            // Добавляем URI новой песни
//            String uri = dbHelper.getMediaUriByTitle(songName, userId);
//            songUris.add(uri);

            songsAdapter.notifyDataSetChanged();
            songNameEditText.setText("");
            // Обновляем список доступных песен
            availableSongs.remove(songName);
            availableSongsAdapter.notifyDataSetChanged();
            Toast.makeText(this, "Песня добавлена", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Ошибка добавления песни", Toast.LENGTH_SHORT).show();
        }
    }

    private void playSong(int position) {
        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                mediaPlayer.reset();
            }

            currentSongIndex = position;
            String uri = songUris.get(position);
            mediaPlayer.setDataSource(this, Uri.parse(uri));
            mediaPlayer.prepare();
            mediaPlayer.start();

            Toast.makeText(this, "Воспроизведение: " + songs.get(position), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка воспроизведения", Toast.LENGTH_SHORT).show();
            Log.e("Playback", "Error playing song", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        dbHelper.close();
    }
}