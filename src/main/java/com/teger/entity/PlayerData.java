package com.teger.entity;

import com.teger.annotation.PrimaryKey;

public class PlayerData {

    @PrimaryKey
    private String uniqueId;

    private int walk;

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public int getWalk() {
        return walk;
    }

    public void setWalk(int walk) {
        this.walk = walk;
    }
}
