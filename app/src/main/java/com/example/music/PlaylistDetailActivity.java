package com.example.music;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.MenuItem;
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
import java.util.List;

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
    private ArrayList<String> allAvailableSongs = new ArrayList<>();
    private ArrayList<String> filteredSongs = new ArrayList<>();
    private DatabaseHelper dbHelper;
    private String playlistName;
    private long userId;
    private long playlistId;
    private ArrayList<String> songUris = new ArrayList<>();

    private MusicService musicService;
    private boolean isBound = false;
    private int currentSongIndex = -1;
    private int currentMode = MusicService.MODE_NORMAL;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;

            // Получаем текущий режим воспроизведения из сервиса
            currentMode = musicService.getPlaybackMode();

            if (!songs.isEmpty() && !songUris.isEmpty()) {
                musicService.setSongList(songs, songUris, 0);
                musicService.setPlaybackMode(currentMode);

                // Если режим REPEAT_ONE и есть текущая песня - воспроизводим ее
                if (currentMode == MusicService.MODE_REPEAT_ONE && currentSongIndex != -1) {
                    musicService.play(currentSongIndex);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_detail);

        Intent intent = getIntent();
        playlistName = intent.getStringExtra("playlistName");
        userId = intent.getLongExtra("userId", -1);

        if (playlistName == null || userId == -1) {
            Toast.makeText(this, "Ошибка: данные плейлиста не получены", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbHelper = new DatabaseHelper(this);

        playlistTitle = findViewById(R.id.playlistTitle);
        songsListView = findViewById(R.id.songsListView);
        availableSongsListView = findViewById(R.id.availableSongsListView);
        songNameEditText = findViewById(R.id.songNameEditText);
        addSongBtn = findViewById(R.id.addSongBtn);
        searchSongsBtn = findViewById(R.id.searchSongsBtn);

        playlistTitle.setText(playlistName);

        playlistId = dbHelper.getPlaylistId(playlistName, userId);
        if (playlistId == -1) {
            Toast.makeText(this, "Ошибка: плейлист не найден", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Intent serviceIntent = new Intent(this, MusicService.class);
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);

        loadPlaylistSongs();
        loadAllAvailableSongs();

        songsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, songs);
        songsListView.setAdapter(songsAdapter);

        availableSongsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, filteredSongs);
        availableSongsListView.setAdapter(availableSongsAdapter);

        songNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterSongs(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        addSongBtn.setOnClickListener(v -> {
            String songName = songNameEditText.getText().toString().trim();
            if (!songName.isEmpty()) {
                addSongToPlaylist(songName);
            } else {
                Toast.makeText(this, "Введите название песни", Toast.LENGTH_SHORT).show();
            }
        });

        searchSongsBtn.setOnClickListener(v -> {
            loadAllAvailableSongs();
            filterSongs(songNameEditText.getText().toString());
        });

        availableSongsListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedSong = filteredSongs.get(position);
            songNameEditText.setText(selectedSong);
            addSongToPlaylist(selectedSong);
        });

        songsListView.setOnItemClickListener((parent, view, position, id) -> {
            playSong(position);
        });

        registerForContextMenu(songsListView);
    }

    private void loadPlaylistSongs() {
        songs.clear();
        songs.addAll(dbHelper.getSongsForPlaylist(playlistId));

        songUris.clear();
        for (String songName : songs) {
            String uri = dbHelper.getMediaUriByTitle(songName, userId);
            if (uri != null) {
                songUris.add(uri);
            } else {
                dbHelper.removeSongFromPlaylist(playlistId, songName);
            }
        }

        for (int i = songs.size() - 1; i >= 0; i--) {
            if (i >= songUris.size() || songUris.get(i) == null) {
                songs.remove(i);
                if (i < songUris.size()) {
                    songUris.remove(i);
                }
            }
        }

        if (songsAdapter != null) {
            songsAdapter.notifyDataSetChanged();
        }
    }

    private void loadAllAvailableSongs() {
        allAvailableSongs.clear();
        allAvailableSongs.addAll(dbHelper.getAllMediaTitles(userId));
        allAvailableSongs.removeAll(songs);
    }

    private void filterSongs(String query) {
        filteredSongs.clear();
        if (query.isEmpty()) {
            filteredSongs.addAll(allAvailableSongs);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (String song : allAvailableSongs) {
                if (song.toLowerCase().contains(lowerCaseQuery)) {
                    filteredSongs.add(song);
                }
            }
        }
        availableSongsAdapter.notifyDataSetChanged();
    }

    private void addSongToPlaylist(String songName) {
        String uri = dbHelper.getMediaUriByTitle(songName, userId);
        if (uri == null) {
            Toast.makeText(this, "Песня не найдена в вашей медиатеке", Toast.LENGTH_SHORT).show();
            return;
        }

        if (dbHelper.isSongInPlaylist(playlistId, songName)) {
            Toast.makeText(this, "Эта песня уже есть в плейлисте", Toast.LENGTH_SHORT).show();
            return;
        }

        long songId = dbHelper.addSongToPlaylist(playlistId, songName);
        if (songId != -1) {
            songs.add(songName);
            songUris.add(uri);
            songsAdapter.notifyDataSetChanged();
            songNameEditText.setText("");

            allAvailableSongs.remove(songName);
            filterSongs("");

            Toast.makeText(this, "Песня добавлена", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Ошибка добавления песни", Toast.LENGTH_SHORT).show();
        }
    }

    private void playSong(int position) {
        if (position >= 0 && position < songUris.size()) {
            currentSongIndex = position;

            if (isBound) {
                // Получаем текущий режим воспроизведения
                currentMode = musicService.getPlaybackMode();

                // Устанавливаем список песен, если он еще не установлен
                musicService.setSongList(songs, songUris, position);

                // Если режим REPEAT_ONE - немедленно начинаем воспроизведение
                if (currentMode == MusicService.MODE_REPEAT_ONE) {
                    musicService.setPlaybackMode(MusicService.MODE_REPEAT_ONE);
                    musicService.play(position);
                }
            }

            // Запускаем PlayerActivity
            Intent playerIntent = new Intent(this, PlayerActivity.class);
            playerIntent.putExtra("userId", userId);
            playerIntent.putStringArrayListExtra("songList", songs);
            playerIntent.putStringArrayListExtra("songUris", songUris);
            playerIntent.putExtra("currentIndex", position);
            startActivity(playerIntent);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == R.id.songsListView) {
            menu.setHeaderTitle("Действия с песней");
            menu.add(0, v.getId(), 0, "Удалить из плейлиста");
            menu.add(0, v.getId(), 0, "Отмена");
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int position = info.position;

        if (item.getTitle().equals("Удалить из плейлиста")) {
            String songName = songs.get(position);
            if (dbHelper.removeSongFromPlaylist(playlistId, songName)) {
                songs.remove(position);
                if (position < songUris.size()) {
                    songUris.remove(position);
                }
                songsAdapter.notifyDataSetChanged();

                loadAllAvailableSongs();
                filterSongs(songNameEditText.getText().toString());

                Toast.makeText(this, "Песня удалена из плейлиста", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Ошибка удаления песни", Toast.LENGTH_SHORT).show();
            }
            return true;
        } else if (item.getTitle().equals("Отмена")) {
            return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
        dbHelper.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPlaylistSongs();
        loadAllAvailableSongs();
        filterSongs(songNameEditText.getText().toString());

        // При возвращении в активность обновляем текущий режим воспроизведения
        if (isBound) {
            currentMode = musicService.getPlaybackMode();
        }
    }
}