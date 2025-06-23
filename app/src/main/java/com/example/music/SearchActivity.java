package com.example.music;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class SearchActivity extends AppCompatActivity {
    private ArrayList<String> allSongs;
    private ArrayList<String> allUris;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        allSongs = getIntent().getStringArrayListExtra("all_songs");
        allUris = getIntent().getStringArrayListExtra("all_uris");

        if (allSongs == null || allUris == null || allSongs.isEmpty()) {
            Toast.makeText(this, "Нет песен для поиска", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        EditText searchEditText = findViewById(R.id.searchEditText);
        Button searchButton = findViewById(R.id.searchButton);

        searchButton.setOnClickListener(v -> {
            String query = searchEditText.getText().toString().trim();
            if (!query.isEmpty()) {
                Intent resultIntent = new Intent(this, SearchResultActivity.class);
                resultIntent.putExtra("search_query", query);
                resultIntent.putStringArrayListExtra("all_songs", allSongs);
                resultIntent.putStringArrayListExtra("all_uris", allUris);
                startActivity(resultIntent);
            } else {
                searchEditText.setError("Введите название песни");
            }
        });
    }

    public void SearcBtnBack_Click(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}