package com.example.music;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
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
    private long userId;
    private DatabaseHelper dbHelper;

    private final ActivityResultLauncher<Intent> pickSongLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    try {
                        getContentResolver().takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );

                        String name = getFileName(uri);

                        if (dbHelper.isMediaExistsForUser(uri.toString(), userId)) {
                            Toast.makeText(this, "Эта песня уже добавлена в ваш аккаунт", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        long mediaId = dbHelper.addMedia(
                                name,
                                uri.toString(),
                                "audio",
                                null,
                                userId
                        );

                        if (mediaId != -1) {
                            songList.add(name);
                            songUris.add(uri.toString());
                            adapter.notifyDataSetChanged();
                            Toast.makeText(this, "Песня добавлена", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e("MusicApp", "Error adding song", e);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            currentSongIndex = savedInstanceState.getInt("currentSongIndex", -1);
        }

        userId = getIntent().getLongExtra("userId", -1);
        if (userId == -1) {
            Toast.makeText(this, "Ошибка: пользователь не идентифицирован", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbHelper = new DatabaseHelper(this);

        songList = new ArrayList<>(dbHelper.getAllMediaTitles(userId));
        songUris = new ArrayList<>(dbHelper.getAllMediaUris(userId));

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
            currentSongIndex = position;
            Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
            intent.putExtra("userId", userId);
            intent.putStringArrayListExtra("songList", songList);
            intent.putStringArrayListExtra("songUris", songUris);
            intent.putExtra("currentIndex", position);
            startActivity(intent);
        });

        Button playlistsButton = findViewById(R.id.playlistsButton);
        playlistsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PlaylistsListActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("currentSongIndex", currentSongIndex);
    }

    private String getFileName(Uri uri) {
        String name = null;
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
            }
        }
        return name != null ? name : uri.getLastPathSegment();
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
                } else if (currentSongIndex == position) {
                    currentSongIndex = -1;
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
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
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