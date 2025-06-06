package com.example.music;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class MainActivity extends AppCompatActivity {
    private static final int PICK_SONG_REQUEST = 1;
    private static final int REQUEST_PERMISSION = 100;
    private static final int SEARCH_RESULT_REQUEST = 2;

    private ArrayList<String> songList = new ArrayList<>();
    private ArrayList<String> songUris = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private int currentSongIndex = -1;
    private long userId; // ID текущего пользователя
    private DatabaseHelper dbHelper;

    private ActivityResultLauncher<Intent> pickSongLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Получаем ID пользователя из Intent
        userId = getIntent().getLongExtra("userId", -1);
        if (userId == -1) {
            Toast.makeText(this, "Ошибка: пользователь не идентифицирован", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbHelper = new DatabaseHelper(this);

        // Загружаем песни из базы данных
        songList = new ArrayList<>(dbHelper.getAllMediaTitles(userId));
        songUris = new ArrayList<>(dbHelper.getAllMediaUris(userId));

        pickSongLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri songUri = result.getData().getData();
                        String songName = songUri.getLastPathSegment();

                        if (!songUris.contains(songUri.toString())) {
                            // Добавляем в базу данных
                            long mediaId = dbHelper.addMedia(
                                    songName,
                                    songUri.toString(),
                                    "audio",
                                    null, // imageUri можно добавить позже
                                    userId
                            );

                            if (mediaId != -1) {
                                songList.add(songName);
                                songUris.add(songUri.toString());
                                adapter.notifyDataSetChanged();
                                Toast.makeText(this, "Песня добавлена", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Ошибка добавления песни", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "Эта песня уже добавлена", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
            }
        }

        ListView listView = findViewById(R.id.songList);
        Button searchButton = findViewById(R.id.searchButton);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, songList);
        listView.setAdapter(adapter);

        registerForContextMenu(listView);

        Button addButton = findViewById(R.id.addSongButton);
        addButton.setOnClickListener(v -> selectSong());

        searchButton.setOnClickListener(v -> {
            if (songList.isEmpty()) {
                Toast.makeText(this, "Сначала добавьте песни", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent searchIntent = new Intent(MainActivity.this, SearchActivity.class);
            searchIntent.putStringArrayListExtra("all_songs", songList);
            searchIntent.putStringArrayListExtra("all_uris", songUris);
            startActivity(searchIntent);
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
            intent.putStringArrayListExtra("songList", songList);
            intent.putStringArrayListExtra("songUris", songUris);
            intent.putExtra("currentIndex", position);
            startActivity(intent);
        });
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle("Действия с песней");
        menu.add(0, v.getId(), 0, "Удалить");
        menu.add(0, v.getId(), 0, "Отмена");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int position = info.position;

        if (item.getTitle().equals("Удалить")) {
            if (currentSongIndex == position && mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                currentSongIndex = -1;
            }

            // Удаляем песню из базы данных
            String uriToDelete = songUris.get(position);
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            int deletedRows = db.delete(
                    DatabaseHelper.TABLE_MEDIA,
                    DatabaseHelper.COLUMN_MEDIA_URI + " = ? AND " + DatabaseHelper.COLUMN_USER_ADDED + " = ?",
                    new String[]{uriToDelete, String.valueOf(userId)}
            );
            db.close();

            if (deletedRows > 0) {
                songList.remove(position);
                songUris.remove(position);
                adapter.notifyDataSetChanged();

                if (currentSongIndex > position) {
                    currentSongIndex--;
                }

                Toast.makeText(this, "Песня удалена", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Ошибка удаления песни", Toast.LENGTH_SHORT).show();
            }
            return true;
        } else if (item.getTitle().equals("Отмена")) {
            return true;
        }
        return super.onContextItemSelected(item);
    }

    private void selectSong() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
        pickSongLauncher.launch(intent);
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