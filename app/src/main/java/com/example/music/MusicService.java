package com.example.music;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class MusicService extends Service {

    private final IBinder binder = new MusicBinder();
    private MediaPlayer mediaPlayer;
    private ArrayList<String> songUris;
    private int currentIndex = 0;


    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setSongList(ArrayList<String> songUris, int startIndex) {
        this.songUris = songUris;
        this.currentIndex = startIndex;
        play(currentIndex);
    }

    private MediaPlayer.OnCompletionListener completionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            playNext(); // Автоматически переключаем на следующую песню
        }
    };

    public void play(int index) {
        if (songUris == null || index >= songUris.size()) return;

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }

        Uri uri = Uri.parse(songUris.get(index));
        mediaPlayer = MediaPlayer.create(this, uri);
        mediaPlayer.setOnCompletionListener(completionListener); // Устанавливаем слушатель
        mediaPlayer.start();
        currentIndex = index;
    }

    public void playPause() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.pause();
            else mediaPlayer.start();
        }
    }

    public void playNext() {
        if (currentIndex < songUris.size() - 1) {
            play(currentIndex + 1);
        }
    }

    public void playPrevious() {
        if (currentIndex > 0) {
            play(currentIndex - 1);
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public int getCurrentPosition() {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
    }

    // Получение длительности трека (в миллисекундах)
    public int getDuration() {
        return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
    }

    // Перемотка трека
    public void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
