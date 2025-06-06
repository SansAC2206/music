package com.example.music.Models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class User implements Serializable {
    private String nickname;
    private String login;
    private String password;
    private long userId; // Добавляем ID пользователя

    // Статический список пользователей (если еще нужен)
    public static ArrayList<User> users = new ArrayList<>();

    // Конструкторы

    public User(String nickname, String login, String password) {
        this.nickname = nickname;
        this.login = login;
        this.password = password;
    }

    // Конструктор с ID
    public User(long userId, String nickname, String login, String password) {
        this.userId = userId;
        this.nickname = nickname;
        this.login = login;
        this.password = password;
    }

    // Геттеры и сеттеры
    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }
}