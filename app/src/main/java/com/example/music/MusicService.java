package com.example.music;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;

public class MusicService extends Service {
    public static final int MODE_NORMAL = 0;
    public static final int MODE_REPEAT_ALL = 1;
    public static final int MODE_REPEAT_ONE = 2;
    public static final int MODE_SHUFFLE = 3;

    private final IBinder binder = new MusicBinder();
    private MediaPlayer mediaPlayer;
    private ArrayList<Song> songs;
    private int currentIndex = 0;
    private int currentMode = MODE_NORMAL;
    private ArrayList<Integer> shuffleOrder = new ArrayList<>();
    private boolean isShuffled = false;

    public class Song {
        public String uri;
        public String title;

        public Song(String uri, String title) {
            this.uri = uri;
            this.title = title;
        }
    }

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

    public void setSongList(ArrayList<String> titles, ArrayList<String> songUris, int startIndex) {
        this.songs = new ArrayList<>();
        for (int i = 0; i < songUris.size(); i++) {
            songs.add(new Song(songUris.get(i), titles.get(i)));
        }
        this.currentIndex = startIndex;
        if (currentMode == MODE_SHUFFLE) {
            generateShuffleOrder();
        }
        play(currentIndex);
    }

    private MediaPlayer.OnCompletionListener completionListener = mp -> {
        switch (currentMode) {
            case MODE_REPEAT_ONE:
                play(currentIndex);
                break;
            case MODE_SHUFFLE:
                playNextShuffled();
                break;
            default:
                playNext();
        }
    };

    public void setPlaybackMode(int mode) {
        currentMode = mode;
        if (mode == MODE_SHUFFLE && !isShuffled) {
            generateShuffleOrder();
            isShuffled = true;
        } else if (mode != MODE_SHUFFLE) {
            isShuffled = false;
        }
    }

    public int getPlaybackMode() {
        return currentMode;
    }

    private void generateShuffleOrder() {
        shuffleOrder.clear();
        for (int i = 0; i < songs.size(); i++) {
            shuffleOrder.add(i);
        }
        Collections.shuffle(shuffleOrder);

        if (currentIndex >= 0 && currentIndex < shuffleOrder.size()) {
            int currentPos = shuffleOrder.indexOf(currentIndex);
            if (currentPos != -1) {
                Collections.swap(shuffleOrder, 0, currentPos);
            }
        }
    }

    private void playNextShuffled() {
        if (songs == null || songs.isEmpty()) return;

        if (shuffleOrder.isEmpty() || currentIndex >= shuffleOrder.size()) {
            generateShuffleOrder();
        }

        int currentShufflePos = shuffleOrder.indexOf(currentIndex);
        if (currentShufflePos == -1 || currentShufflePos >= shuffleOrder.size() - 1) {
            generateShuffleOrder();
            currentShufflePos = 0;
        } else {
            currentShufflePos++;
        }

        play(shuffleOrder.get(currentShufflePos));
    }

    public void play(int index) {
        if (songs == null || index >= songs.size()) return;

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }

        Uri uri = Uri.parse(songs.get(index).uri);
        mediaPlayer = MediaPlayer.create(this, uri);
        mediaPlayer.setOnCompletionListener(completionListener);
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
        if (currentMode == MODE_SHUFFLE) {
            playNextShuffled();
            return;
        }

        if (currentIndex < songs.size() - 1) {
            play(currentIndex + 1);
        } else if (currentMode == MODE_REPEAT_ALL) {
            play(0);
        }
    }

    public void playPrevious() {
        if (currentMode == MODE_SHUFFLE) {
            if (shuffleOrder.isEmpty()) {
                generateShuffleOrder();
            }

            int currentShufflePos = shuffleOrder.indexOf(currentIndex);
            if (currentShufflePos <= 0) {
                play(shuffleOrder.get(0));
            } else {
                play(shuffleOrder.get(currentShufflePos - 1));
            }
            return;
        }

        if (currentIndex > 0) {
            play(currentIndex - 1);
        } else if (currentMode == MODE_REPEAT_ALL) {
            play(songs.size() - 1);
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public Song getCurrentSong() {
        if (songs == null || currentIndex < 0 || currentIndex >= songs.size()) {
            return null;
        }
        return songs.get(currentIndex);
    }

    public int getCurrentPosition() {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
    }

    public int getDuration() {
        return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
    }

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