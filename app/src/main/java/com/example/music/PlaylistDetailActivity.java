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
    private ArrayList<String> allAvailableSongs = new ArrayList<>(); // Все доступные песни
    private ArrayList<String> filteredSongs = new ArrayList<>(); // Отфильтрованные песни для отображения
    private DatabaseHelper dbHelper;
    private String playlistName;
    private long userId;
    private long playlistId;
    private ArrayList<String> songUris = new ArrayList<>();

    // Для воспроизведения музыки через сервис
    private MusicService musicService;
    private boolean isBound = false;
    private int currentSongIndex = -1;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;
            if (!songUris.isEmpty()) {
                musicService.setSongList(songUris, 0);
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

        // Запускаем сервис
        Intent serviceIntent = new Intent(this, MusicService.class);
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);

        // Загружаем песни плейлиста
        loadPlaylistSongs();
        loadAllAvailableSongs();

        // Настройка адаптеров
        songsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, songs);
        songsListView.setAdapter(songsAdapter);

        availableSongsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, filteredSongs);
        availableSongsListView.setAdapter(availableSongsAdapter);

        // Обработчик ввода текста для поиска песен
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
            loadAllAvailableSongs();
            filterSongs(songNameEditText.getText().toString());
        });

        // Обработчик добавления песни из списка доступных
        availableSongsListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedSong = filteredSongs.get(position);
            songNameEditText.setText(selectedSong);
            addSongToPlaylist(selectedSong);
        });

        // Обработчик клика по песне в плейлисте (воспроизведение)
        songsListView.setOnItemClickListener((parent, view, position, id) -> {
            playSong(position);
        });

        registerForContextMenu(songsListView);
    }

    private void loadPlaylistSongs() {
        songs.clear();
        songs.addAll(dbHelper.getSongsForPlaylist(playlistId));

        // Загружаем URI песен для воспроизведения
        songUris.clear();
        for (String songName : songs) {
            String uri = dbHelper.getMediaUriByTitle(songName, userId);
            if (uri != null) {
                songUris.add(uri);
            } else {
                // Если URI не найден, удаляем песню из плейлиста
                dbHelper.removeSongFromPlaylist(playlistId, songName);
            }
        }

        // Удаляем песни без URI из списка
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
        // Получаем все песни из медиатеки пользователя
        allAvailableSongs.addAll(dbHelper.getAllMediaTitles(userId));
        // Убираем песни, которые уже есть в плейлисте
        allAvailableSongs.removeAll(songs);
    }

    private void filterSongs(String query) {
        filteredSongs.clear();
        if (query.isEmpty()) {
            // Если запрос пустой, показываем все доступные песни
            filteredSongs.addAll(allAvailableSongs);
        } else {
            // Фильтруем песни по введенному тексту
            String lowerCaseQuery = query.toLowerCase();
            for (String song : allAvailableSongs) {
                if (song.toLowerCase().startsWith(lowerCaseQuery)) {
                    filteredSongs.add(song);
                }
            }
        }
        availableSongsAdapter.notifyDataSetChanged();
    }

    private void addSongToPlaylist(String songName) {
        // Проверяем, что песня существует в медиатеке
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

            // Обновляем список доступных песен
            allAvailableSongs.remove(songName);
            filterSongs("");

            Toast.makeText(this, "Песня добавлена", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Ошибка добавления песни", Toast.LENGTH_SHORT).show();
        }
    }

    private void playSong(int position) {
        if (isBound && position >= 0 && position < songUris.size()) {
            currentSongIndex = position;
            musicService.play(position);

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

                // Обновляем список доступных песен
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
    }
}