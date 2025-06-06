package com.example.music;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class PlaylistActivity extends AppCompatActivity {
    private ListView playlistListView;
    private ArrayAdapter<String> playlistAdapter;
    private ArrayList<String> playlists = new ArrayList<>();
    private DatabaseHelper dbHelper;
    private long userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);

        userId = getIntent().getLongExtra("userId", -1);
        if (userId == -1) {
            Toast.makeText(this, "Ошибка: пользователь не идентифицирован", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbHelper = new DatabaseHelper(this);

        playlistListView = findViewById(R.id.playlistListView);
        Button createPlaylistBtn = findViewById(R.id.createPlaylistBtn);
        final EditText playlistNameEditText = findViewById(R.id.playlistNameEditText);

        // Загружаем плейлисты из базы данных
        loadPlaylists();

        createPlaylistBtn.setOnClickListener(v -> {
            String playlistName = playlistNameEditText.getText().toString().trim();
            if (!playlistName.isEmpty()) {
                long playlistId = dbHelper.createPlaylist(playlistName, null, userId);
                if (playlistId != -1) {
                    playlists.add(playlistName);
                    playlistAdapter.notifyDataSetChanged();
                    playlistNameEditText.setText("");
                    Toast.makeText(this, "Плейлист создан", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Ошибка создания плейлиста", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Введите название плейлиста", Toast.LENGTH_SHORT).show();
            }
        });

        playlistListView.setOnItemClickListener((parent, view, position, id) -> {
            // Открываем плейлист для просмотра/редактирования
            Intent intent = new Intent(PlaylistActivity.this, PlaylistDetailActivity.class);
            intent.putExtra("playlistName", playlists.get(position));
            intent.putExtra("userId", userId);
            startActivity(intent);
        });
    }

    private void loadPlaylists() {
        // Здесь нужно реализовать загрузку плейлистов из базы данных
        // Это примерный код, вам нужно адаптировать его под вашу базу данных
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_PLAYLISTS,
                new String[]{DatabaseHelper.COLUMN_PLAYLIST_NAME},
                DatabaseHelper.COLUMN_USER_ADDED + " = ?",
                new String[]{String.valueOf(userId)},
                null, null, null
        );

        playlists.clear();
        if (cursor.moveToFirst()) {
            do {
                playlists.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();

        playlistAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, playlists);
        playlistListView.setAdapter(playlistAdapter);
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }
}
