package com.vicmatskiv.pointblank.item;

import com.google.gson.JsonObject;

public record BulletData(float velocity, float speedOffset, float maxSpeedOffset, float inaccuracy) {
    public static BulletData fromJson(JsonObject obj) {
        if(obj.has("bulletData"))
            obj = obj.getAsJsonObject("bulletData");

        float velocity = obj.has("velocity") ? obj.get("velocity").getAsFloat() : 2.5f;
        float speedOffset = obj.has("speedOffset") ? obj.get("speedOffset").getAsFloat() : 4.0f;
        float maxSpeedOffset = obj.has("maxSpeedOffset") ? obj.get("maxSpeedOffset").getAsFloat() : 5.0f;
        float inaccuracy = obj.has("inaccuracy") ? obj.get("inaccuracy").getAsFloat() : 20.0f;
        return new BulletData(velocity, speedOffset, maxSpeedOffset, inaccuracy);
    }
}
