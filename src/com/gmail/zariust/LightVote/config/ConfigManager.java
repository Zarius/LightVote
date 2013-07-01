package com.gmail.zariust.LightVote.config;

import com.gmail.zariust.LightVote.LightVote;

public class ConfigManager {
    public static void load(LightVote parent) {
        ConfigV1 config = new ConfigV1(parent);
        config.loadConfig(parent);
    }

}
