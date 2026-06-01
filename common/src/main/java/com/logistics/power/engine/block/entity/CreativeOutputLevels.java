package com.logistics.power.engine.block.entity;

final class CreativeOutputLevels {
    static final long[] DEFAULT_LEVELS = {20, 40, 80, 160, 320, 640, 1280};

    private final long[] levels;
    private int index;

    CreativeOutputLevels() {
        this(DEFAULT_LEVELS);
    }

    CreativeOutputLevels(long[] levels) {
        if (levels.length == 0) {
            throw new IllegalArgumentException("levels must not be empty");
        }
        this.levels = levels.clone();
    }

    long cycle() {
        index = (index + 1) % levels.length;
        return outputRate();
    }

    void restore(int index) {
        this.index = index >= 0 && index < levels.length ? index : 0;
    }

    int index() {
        return index;
    }

    long outputRate() {
        return levels[index];
    }

    float pistonSpeed() {
        return 0.02F * (index + 1);
    }
}
