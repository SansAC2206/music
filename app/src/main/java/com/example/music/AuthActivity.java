package com.example.music;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.music.Models.User;
import com.google.android.material.snackbar.Snackbar;

import java.util.Optional;

public class AuthActivity extends AppCompatActivity {

    EditText editTextLogin;
    EditText editTextPassword;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_auth);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new DatabaseHelper(this);
        editTextLogin = findViewById(R.id.editTextLoginAuth);
        editTextPassword = findViewById(R.id.editTextPasswordAuth);
    }

    public void AuthBtn_Click(View view) {
        String login = editTextLogin.getText().toString();
        String password = editTextPassword.getText().toString();

        if (login.isEmpty() || password.isEmpty()) {
            Snackbar.make(view, "Введите логин и пароль", 3000).show();
            return;
        }

        User user = dbHelper.getUser(login, password);

        if (user != null) {
            Snackbar.make(view, "Добро пожаловать " + user.getNickname() + "!", 3000).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("userId", user.getUserId()); // Передаем ID пользователя
            startActivity(intent);
        } else {
            Snackbar.make(view, "Неверный логин или пароль!", 3000).show();
        }
    }

    public void AuthBackBtn_Click(View view) {
        Intent intent = new Intent(this, StartActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }
}