package com.Gabou.sereneseasonsplus.api;

public record CloudStamp(long id, float x, float z, float radius, boolean raining, boolean thundering) {
    public boolean contains(float px, float pz) {
        float dx = px - x;
        float dz = pz - z;
        return dx * dx + dz * dz <= radius * radius;
    }
}

