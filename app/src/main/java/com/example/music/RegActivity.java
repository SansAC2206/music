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

public class RegActivity extends AppCompatActivity {

    EditText nickEditTextReg, loginEditTextReg, passwordEditTextReg;
    private DatabaseHelper dbHelper;

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

        dbHelper = new DatabaseHelper(this);
        nickEditTextReg = findViewById(R.id.editTextNickReg);
        loginEditTextReg = findViewById(R.id.editTextLoginReg);
        passwordEditTextReg = findViewById(R.id.editTextPasswordReg);
    }

    public void RegBtn_Click(View view) {
        if (editTextIsEmpty(nickEditTextReg) || editTextIsEmpty(loginEditTextReg) || editTextIsEmpty(passwordEditTextReg)) {
            Snackbar.make(view, "Заполните все поля!", 2500).show();
        }
        else if (passwordEditTextReg.getText().toString().length() < 8) {
            Snackbar.make(view, "Пароль меньше 8 символов!", 2500).show();
        }
        else {
            String login = loginEditTextReg.getText().toString();
            if (dbHelper.checkUserExists(login)) {
                Snackbar.make(view, "Пользователь с такой почтой уже существует!",2500).show();
            }
            else {
                long userId = dbHelper.addUser(
                        login,
                        passwordEditTextReg.getText().toString(),
                        nickEditTextReg.getText().toString()
                );

                if (userId != -1) {
                    Snackbar.make(view, "Вы успешно зарегистрировались!",2500).show();
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                } else {
                    Snackbar.make(view, "Ошибка регистрации",2500).show();
                }
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

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }
}