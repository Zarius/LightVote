package com.gmail.zariust.LightVote;

import java.io.IOException;

import com.gmail.zariust.LightVote.metrics.Metrics;

public class Dependencies {

    public static Metrics metrics = null;
    private final LightVote parent;

    public Dependencies(LightVote lightVote) {
        this.parent = lightVote;
    }

    public final static void init(LightVote lightVote) {
        Dependencies dependencies = new Dependencies(lightVote);
        dependencies.enableMetricsIfConfigured();
    }

    public void enableMetricsIfConfigured() {
        if (parent.config.enableMetrics) {
            try {
                metrics = new Metrics(parent);
                metrics.start();
            } catch (IOException e) {
                // Failed to submit the stats :-(
            }
        }
    }
}