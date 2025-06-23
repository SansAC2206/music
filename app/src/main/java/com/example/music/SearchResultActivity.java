package com.example.music;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class SearchResultActivity extends AppCompatActivity {
    private ArrayAdapter<String> adapter;
    private ArrayList<String> searchResults = new ArrayList<>();
    private ArrayList<String> searchResultUris = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_result);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        ListView resultListView = findViewById(R.id.resultListView);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, searchResults);
        resultListView.setAdapter(adapter);

        String query = getIntent().getStringExtra("search_query");
        ArrayList<String> allSongs = getIntent().getStringArrayListExtra("all_songs");
        ArrayList<String> allUris = getIntent().getStringArrayListExtra("all_uris");

        if (query != null && allSongs != null && allUris != null) {
            searchResults.clear();
            searchResultUris.clear();

            for (int i = 0; i < allSongs.size(); i++) {
                if (allSongs.get(i).toLowerCase().contains(query.toLowerCase())) {
                    searchResults.add(allSongs.get(i));
                    searchResultUris.add(allUris.get(i));
                }
            }

            if (searchResults.isEmpty()) {
                searchResults.add("Ничего не найдено");
            }

            adapter.notifyDataSetChanged();
        }

        resultListView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < searchResultUris.size()) {
                Intent intent = new Intent(SearchResultActivity.this, PlayerActivity.class);
                intent.putStringArrayListExtra("songList", searchResults);
                intent.putStringArrayListExtra("songUris", searchResultUris);
                intent.putExtra("currentIndex", position);
                startActivity(intent);
            }
        });
    }
    public void SearchBtnBack_Click(View view) {
        Intent intent = new Intent(this, SearchActivity.class);
        startActivity(intent);
        finish();
    }
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}