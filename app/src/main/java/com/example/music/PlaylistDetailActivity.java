package com.example.music;

import android.content.Intent;
import android.os.Bundle;
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
    private EditText songNameEditText;
    private Button addSongBtn;
    private ArrayAdapter<String> songsAdapter;
    private ArrayList<String> songs = new ArrayList<>();
    private DatabaseHelper dbHelper;
    private String playlistName;
    private long userId;
    private long playlistId;

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

        // Инициализация UI элементов
        playlistTitle = findViewById(R.id.playlistTitle);
        songsListView = findViewById(R.id.songsListView);
        songNameEditText = findViewById(R.id.songNameEditText);
        addSongBtn = findViewById(R.id.addSongBtn);

        // Устанавливаем название плейлиста
        playlistTitle.setText(playlistName);

        // Получаем ID плейлиста из базы данных
        playlistId = dbHelper.getPlaylistId(playlistName, userId);
        if (playlistId == -1) {
            Toast.makeText(this, "Ошибка: плейлист не найден", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Загружаем песни плейлиста
        loadSongs();

        // Настройка адаптера для списка песен
        songsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, songs);
        songsListView.setAdapter(songsAdapter);

        // Обработчик добавления новой песни
        addSongBtn.setOnClickListener(v -> {
            String songName = songNameEditText.getText().toString().trim();
            if (!songName.isEmpty()) {
                long songId = dbHelper.addSongToPlaylist(playlistId, songName);
                if (songId != -1) {
                    songs.add(songName);
                    songsAdapter.notifyDataSetChanged();
                    songNameEditText.setText("");
                    Toast.makeText(this, "Песня добавлена", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Ошибка добавления песни", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Введите название песни", Toast.LENGTH_SHORT).show();
            }
        });

        // Обработчик клика по песне (например, для воспроизведения)
        songsListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedSong = songs.get(position);
            Toast.makeText(this, "Воспроизведение: " + selectedSong, Toast.LENGTH_SHORT).show();
            // Здесь можно добавить логику воспроизведения песни
        });
    }

    private void loadSongs() {
        songs.clear();
        songs.addAll(dbHelper.getSongsForPlaylist(playlistId));
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }
}