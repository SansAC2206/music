package com.example.music.Models;

import java.io.Serializable;
import java.util.ArrayList;

public class User implements Serializable {

    private String login;
    private String password;

    private String nickname;

    public static ArrayList<User> users = new ArrayList<>();

    public User(String nickname, String login, String password) {
        this.nickname = nickname;
        this.login = login;
        this.password = password;
    }

    public User(){

    }

    public String getNickname() { return nickname; }

    public void setNickname(String nickname) { this.nickname = nickname; }

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
}