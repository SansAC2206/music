package com.example.music;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class QueueActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_queue);

        ListView listView = findViewById(R.id.queueListView);
        ArrayList<String> songList = getIntent().getStringArrayListExtra("songList");
        int currentIndex = getIntent().getIntExtra("currentIndex", 0);

        ArrayList<String> queue = new ArrayList<>();
        for (int i = currentIndex + 1; i < songList.size(); i++) {
            queue.add(songList.get(i));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, queue);
        listView.setAdapter(adapter);
    }
}

