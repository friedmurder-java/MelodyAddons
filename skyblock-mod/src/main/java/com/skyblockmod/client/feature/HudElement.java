package com.skyblockmod.client.feature;

public class HudElement {
    public float x;
    public float y;
    public float scale;
    public boolean visible;
    public String id;

    public HudElement() {
        this.x = 10;
        this.y = 10;
        this.scale = 1.0f;
        this.visible = true;
    }

    public HudElement(String id, float x, float y, float scale) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.scale = scale;
        this.visible = true;
    }
}