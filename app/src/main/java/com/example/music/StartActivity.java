package com.example.music;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.music.Models.User;

import java.util.Optional;

public class StartActivity extends AppCompatActivity {

    Button authBtn, regBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_start);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        authBtn = findViewById(R.id.btnAuth);
        regBtn = findViewById(R.id.regBtn);
    }

    public void MainAuthBtn_Click(View view) {
        Intent intent = new Intent(this, AuthActivity.class);
        startActivity(intent);
        finish();
    }

    public void RegBtnMain_Click(View view) {
        Intent intent = new Intent(this, RegActivity.class);
        startActivity(intent);
        finish();
    }

    private Optional<User> findUserByLoginPass(String login, String password) {
        return User.users.stream().filter(user -> user.getLogin().equals(login))
                .filter(user -> user.getPassword().equals(password)).findFirst();
    }
}