package com.example.music;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.music.Models.User;
import com.google.android.material.snackbar.Snackbar;

import java.util.Optional;

public class RegActivity extends AppCompatActivity {

    EditText nickEditTextReg, loginEditTextReg, passwordEditTextReg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reg);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        nickEditTextReg = findViewById(R.id.editTextNickReg);
        loginEditTextReg = findViewById(R.id.editTextLoginReg);
        passwordEditTextReg = findViewById(R.id.editTextPasswordReg);
    }

    public void RegBtn_Click(View view) {
        if (editTextIsEmpty(nickEditTextReg) || editTextIsEmpty(loginEditTextReg) || editTextIsEmpty(passwordEditTextReg))
        {
            Snackbar.make(view, "Заполните все поля!", 2500).show();
        }
        else if (passwordEditTextReg.getText().toString().length() < 8)
        {
            Snackbar.make(view, "Пароль меньше 8 символов!", 2500).show();
        }
        else {
            String login = loginEditTextReg.getText().toString();
            Optional<User> foundUser = findUserByLogin(login);
            if (foundUser.isPresent())
            {
                Snackbar.make(view, "Пользователь с такой почтой уже существует!",2500).show();
            }
            else
            {
                User user = new User(nickEditTextReg.getText().toString(), loginEditTextReg.getText().toString(), passwordEditTextReg.getText().toString());
                User.users.add(user);
                Snackbar.make(view, "Вы успешно зарегестрировались!",2500).show();
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
            }
        }
    }

    public void RegBackBtn_Click(View view) {
        Intent intent = new Intent(this, StartActivity.class);
        startActivity(intent);
        finish();
    }

    private boolean editTextIsEmpty(EditText editText){
        return editText.getText().toString().isEmpty();
    }

    private Optional<User> findUserByLogin(String login) {
        return User.users.stream().filter(user -> user.getLogin().equals(login)).findFirst();
    }
}