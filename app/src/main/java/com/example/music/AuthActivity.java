package com.example.music;

import android.os.Bundle;
import android.content.Intent;
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
        editTextLogin = findViewById(R.id.editTextLoginAuth);
        editTextPassword = findViewById(R.id.editTextPasswordAuth);
    }

    public void AuthBtn_Click(View view) {
        String login = editTextLogin.getText().toString();
        String password = editTextPassword.getText().toString();

        Optional<User> foundUser = findUserByLoginPass(login, password);

        if (foundUser.isPresent()) {
            User user = foundUser.get();
            Snackbar.make(view, "Добро пожаловать " + user.getNickname() + "!", 3000).show();
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
        else {
            Snackbar.make(view, "Неверный логин или пароль!", 3000).show();
        }
    }

    public void AuthBackBtn_Click(View view) {
        Intent intent = new Intent(this, StartActivity.class);
        startActivity(intent);
        finish();
    }

    private Optional<User> findUserByLoginPass(String login, String password) {
        return User.users.stream().filter(user -> user.getLogin().equals(login))
                .filter(user -> user.getPassword().equals(password)).findFirst();
    }
}