package com.example.music;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.music.Models.User;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "MusicApp.db";
    private static final int DATABASE_VERSION = 1;

    // Таблица пользователей
    private static final String TABLE_USERS = "users";
    private static final String COLUMN_USER_ID = "user_id";
    private static final String COLUMN_USER_EMAIL = "email";
    private static final String COLUMN_USER_PASSWORD = "password";
    private static final String COLUMN_USER_NICKNAME = "nickname";

    // Таблица медиа (песни/видео)
    public static final String TABLE_MEDIA = "media";
    private static final String COLUMN_MEDIA_ID = "media_id";
    private static final String COLUMN_MEDIA_TITLE = "title";
    public static final String COLUMN_MEDIA_URI = "uri";
    private static final String COLUMN_MEDIA_TYPE = "type"; // "audio" или "video"
    private static final String COLUMN_MEDIA_IMAGE_URI = "image_uri";
    public static final String COLUMN_USER_ADDED = "user_id_added";

    // Таблица плейлистов
    public static final String TABLE_PLAYLISTS = "playlists";
    private static final String COLUMN_PLAYLIST_ID = "playlist_id";
    public static final String COLUMN_PLAYLIST_NAME = "name";
    private static final String COLUMN_PLAYLIST_IMAGE_URI = "image_uri";

    // Таблица связи медиа и плейлистов (многие ко многим)
    private static final String TABLE_PLAYLIST_MEDIA = "playlist_media";
    private static final String COLUMN_PLAYLIST_MEDIA_ID = "id";
    private static final String COLUMN_PM_PLAYLIST_ID = "playlist_id";
    private static final String COLUMN_PM_MEDIA_ID = "media_id";

    // Таблица песен (добавлена для совместимости с PlaylistDetailActivity)
    private static final String TABLE_SONGS = "songs";
    private static final String COLUMN_SONG_ID = "song_id";
    private static final String COLUMN_SONG_NAME = "name";
    private static final String COLUMN_SONG_PLAYLIST_ID = "playlist_id";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Создание таблицы пользователей
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
                + COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_USER_EMAIL + " TEXT UNIQUE,"
                + COLUMN_USER_PASSWORD + " TEXT,"
                + COLUMN_USER_NICKNAME + " TEXT" + ")";
        db.execSQL(CREATE_USERS_TABLE);

        // Создание таблицы медиа
        String CREATE_MEDIA_TABLE = "CREATE TABLE " + TABLE_MEDIA + "("
                + COLUMN_MEDIA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_MEDIA_TITLE + " TEXT,"
                + COLUMN_MEDIA_URI + " TEXT UNIQUE,"
                + COLUMN_MEDIA_TYPE + " TEXT,"
                + COLUMN_MEDIA_IMAGE_URI + " TEXT,"
                + COLUMN_USER_ADDED + " INTEGER,"
                + "FOREIGN KEY(" + COLUMN_USER_ADDED + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + ")" + ")";
        db.execSQL(CREATE_MEDIA_TABLE);

        // Создание таблицы плейлистов
        String CREATE_PLAYLISTS_TABLE = "CREATE TABLE " + TABLE_PLAYLISTS + "("
                + COLUMN_PLAYLIST_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_PLAYLIST_NAME + " TEXT,"
                + COLUMN_PLAYLIST_IMAGE_URI + " TEXT,"
                + COLUMN_USER_ADDED + " INTEGER,"
                + "FOREIGN KEY(" + COLUMN_USER_ADDED + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + ")" + ")";
        db.execSQL(CREATE_PLAYLISTS_TABLE);

        // Создание таблицы связи плейлистов и медиа
        String CREATE_PLAYLIST_MEDIA_TABLE = "CREATE TABLE " + TABLE_PLAYLIST_MEDIA + "("
                + COLUMN_PLAYLIST_MEDIA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_PM_PLAYLIST_ID + " INTEGER,"
                + COLUMN_PM_MEDIA_ID + " INTEGER,"
                + "FOREIGN KEY(" + COLUMN_PM_PLAYLIST_ID + ") REFERENCES " + TABLE_PLAYLISTS + "(" + COLUMN_PLAYLIST_ID + "),"
                + "FOREIGN KEY(" + COLUMN_PM_MEDIA_ID + ") REFERENCES " + TABLE_MEDIA + "(" + COLUMN_MEDIA_ID + ")" + ")";
        db.execSQL(CREATE_PLAYLIST_MEDIA_TABLE);

        // Создание таблицы песен (для простой реализации)
        String CREATE_SONGS_TABLE = "CREATE TABLE " + TABLE_SONGS + "("
                + COLUMN_SONG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_SONG_NAME + " TEXT,"
                + COLUMN_SONG_PLAYLIST_ID + " INTEGER,"
                + "FOREIGN KEY(" + COLUMN_SONG_PLAYLIST_ID + ") REFERENCES " + TABLE_PLAYLISTS + "(" + COLUMN_PLAYLIST_ID + ")" + ")";
        db.execSQL(CREATE_SONGS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLAYLIST_MEDIA);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SONGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLAYLISTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEDIA);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    // Методы для работы с пользователями
    public long addUser(String email, String password, String nickname) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_EMAIL, email);
        values.put(COLUMN_USER_PASSWORD, password);
        values.put(COLUMN_USER_NICKNAME, nickname);

        long result = db.insert(TABLE_USERS, null, values);
        db.close();
        return result;
    }

    public boolean checkUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_USER_ID};
        String selection = COLUMN_USER_EMAIL + " = ?" + " AND " + COLUMN_USER_PASSWORD + " = ?";
        String[] selectionArgs = {email, password};

        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        db.close();

        return count > 0;
    }

    public boolean checkUserExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_USER_ID};
        String selection = COLUMN_USER_EMAIL + " = ?";
        String[] selectionArgs = {email};

        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        db.close();

        return count > 0;
    }

    public User getUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_USER_ID, COLUMN_USER_NICKNAME};
        String selection = COLUMN_USER_EMAIL + " = ?" + " AND " + COLUMN_USER_PASSWORD + " = ?";
        String[] selectionArgs = {email, password};

        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            User user = new User(
                    cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_NICKNAME)),
                    email,
                    password
            );
            cursor.close();
            db.close();
            return user;
        }

        if (cursor != null) {
            cursor.close();
        }
        db.close();
        return null;
    }

    // Методы для работы с медиа
    public long addMedia(String title, String uri, String type, String imageUri, long userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_MEDIA_TITLE, title);
        values.put(COLUMN_MEDIA_URI, uri);
        values.put(COLUMN_MEDIA_TYPE, type);
        values.put(COLUMN_MEDIA_IMAGE_URI, imageUri);
        values.put(COLUMN_USER_ADDED, userId);

        long result = db.insert(TABLE_MEDIA, null, values);
        db.close();
        return result;
    }

    public List<String> getAllMediaUris(long userId) {
        List<String> mediaUris = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String[] columns = {COLUMN_MEDIA_URI};
        String selection = COLUMN_USER_ADDED + " = ?";
        String[] selectionArgs = {String.valueOf(userId)};

        Cursor cursor = db.query(TABLE_MEDIA, columns, selection, selectionArgs, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                mediaUris.add(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEDIA_URI)));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return mediaUris;
    }

    public List<String> getAllMediaTitles(long userId) {
        List<String> mediaTitles = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String[] columns = {COLUMN_MEDIA_TITLE};
        String selection = COLUMN_USER_ADDED + " = ?";
        String[] selectionArgs = {String.valueOf(userId)};

        Cursor cursor = db.query(TABLE_MEDIA, columns, selection, selectionArgs, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                mediaTitles.add(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEDIA_TITLE)));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return mediaTitles;
    }

    // Методы для работы с плейлистами
    public long createPlaylist(String name, String imageUri, long userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PLAYLIST_NAME, name);
        values.put(COLUMN_PLAYLIST_IMAGE_URI, imageUri);
        values.put(COLUMN_USER_ADDED, userId);

        long result = db.insert(TABLE_PLAYLISTS, null, values);
        db.close();
        return result;
    }

    public long addMediaToPlaylist(long playlistId, long mediaId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PM_PLAYLIST_ID, playlistId);
        values.put(COLUMN_PM_MEDIA_ID, mediaId);

        long result = db.insert(TABLE_PLAYLIST_MEDIA, null, values);
        db.close();
        return result;
    }

    public List<String> getPlaylistMediaUris(long playlistId) {
        List<String> mediaUris = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT m." + COLUMN_MEDIA_URI + " FROM " + TABLE_MEDIA + " m " +
                "INNER JOIN " + TABLE_PLAYLIST_MEDIA + " pm ON m." + COLUMN_MEDIA_ID + " = pm." + COLUMN_PM_MEDIA_ID + " " +
                "WHERE pm." + COLUMN_PM_PLAYLIST_ID + " = ?";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(playlistId)});

        if (cursor.moveToFirst()) {
            do {
                mediaUris.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return mediaUris;
    }

    public long getPlaylistId(String playlistName, long userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_PLAYLISTS,
                new String[]{COLUMN_PLAYLIST_ID},
                COLUMN_PLAYLIST_NAME + " = ? AND " + COLUMN_USER_ADDED + " = ?",
                new String[]{playlistName, String.valueOf(userId)},
                null, null, null
        );

        long playlistId = -1;
        if (cursor.moveToFirst()) {
            playlistId = cursor.getLong(0);
        }
        cursor.close();
        db.close();
        return playlistId;
    }

    public long addSongToPlaylist(long playlistId, String songName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SONG_NAME, songName);
        values.put(COLUMN_SONG_PLAYLIST_ID, playlistId);
        long result = db.insert(TABLE_SONGS, null, values);
        db.close();
        return result;
    }

    public ArrayList<String> getSongsForPlaylist(long playlistId) {
        ArrayList<String> songs = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_SONGS,
                new String[]{COLUMN_SONG_NAME},
                COLUMN_SONG_PLAYLIST_ID + " = ?",
                new String[]{String.valueOf(playlistId)},
                null, null, null
        );

        if (cursor.moveToFirst()) {
            do {
                songs.add(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SONG_NAME)));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return songs;
    }
}