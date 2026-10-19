package com.logistics.core.lib.client.screen.tab;

/** A screen-space rectangle in GUI pixels. Bounds are half-open: {@code [x, x + w) × [y, y + h)}. */
public record TabRect(int x, int y, int w, int h) {

    public int right() {
        return x + w;
    }

    public int bottom() {
        return y + h;
    }

    public boolean contains(double px, double py) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    public boolean intersects(TabRect other) {
        return x < other.x + other.w && other.x < x + w && y < other.y + other.h && other.y < y + h;
    }
}
