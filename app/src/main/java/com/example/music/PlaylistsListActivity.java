package com.example.music;

import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class PlaylistsListActivity extends AppCompatActivity {
    private ListView playlistsListView;
    private ArrayAdapter<String> playlistsAdapter;
    private ArrayList<String> playlists = new ArrayList<>();
    private DatabaseHelper dbHelper;
    private long userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlists_list);

        userId = getIntent().getLongExtra("userId", -1);
        if (userId == -1) {
            Toast.makeText(this, "Ошибка: пользователь не идентифицирован", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbHelper = new DatabaseHelper(this);

        playlistsListView = findViewById(R.id.playlistsListView);
        Button createPlaylistButton = findViewById(R.id.createPlaylistButton);

        // Загружаем плейлисты
        loadPlaylists();

        // Регистрируем контекстное меню для ListView
        registerForContextMenu(playlistsListView);

        createPlaylistButton.setOnClickListener(v -> {
            Intent intent = new Intent(PlaylistsListActivity.this, PlaylistActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        playlistsListView.setOnItemClickListener((parent, view, position, id) -> {
            String playlistName = playlists.get(position);
            Intent intent = new Intent(PlaylistsListActivity.this, PlaylistDetailActivity.class);
            intent.putExtra("playlistName", playlistName);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });
    }

    private void loadPlaylists() {
        playlists.clear();
        playlists.addAll(dbHelper.getPlaylistsForUser(userId));

        playlistsAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, playlists);
        playlistsListView.setAdapter(playlistsAdapter);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle("Действия с плейлистом");
        menu.add(0, v.getId(), 0, "Удалить");
        menu.add(0, v.getId(), 0, "Отмена");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int position = info.position;

        if (item.getTitle().equals("Удалить")) {
            String playlistName = playlists.get(position);
            long playlistId = dbHelper.getPlaylistId(playlistName, userId);

            if (dbHelper.deletePlaylist(playlistId)) {
                playlists.remove(position);
                playlistsAdapter.notifyDataSetChanged();
                Toast.makeText(this, "Плейлист удален", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Ошибка удаления плейлиста", Toast.LENGTH_SHORT).show();
            }
            return true;
        } else if (item.getTitle().equals("Отмена")) {
            return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Обновляем список при возвращении на экран
        loadPlaylists();
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }
}