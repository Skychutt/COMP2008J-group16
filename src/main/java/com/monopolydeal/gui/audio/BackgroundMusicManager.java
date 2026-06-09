package com.monopolydeal.gui.audio;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Loops {@code music.wav} for the entire application lifetime.
 */
public final class BackgroundMusicManager {

    private static final String RESOURCE_PATH = "/music.wav";
    private static final double DEFAULT_VOLUME = 0.65;

    private static BackgroundMusicManager instance;

    private MediaPlayer player;
    private double volume = DEFAULT_VOLUME;
    private boolean playing;

    private BackgroundMusicManager() {
    }

    public static BackgroundMusicManager getInstance() {
        if (instance == null) {
            instance = new BackgroundMusicManager();
        }
        return instance;
    }

    public synchronized void start() {
        ensurePlayer();
        if (player == null) {
            return;
        }
        player.setVolume(volume);
        if (!playing) {
            player.play();
            playing = true;
        }
    }

    public synchronized void stop() {
        if (player != null && playing) {
            player.pause();
            player.seek(Duration.ZERO);
            playing = false;
        }
    }

    public synchronized void setVolume(double value) {
        volume = clamp(value);
        if (player != null) {
            player.setVolume(volume);
        }
    }

    public double getVolume() {
        return volume;
    }

    public int getVolumePercent() {
        return (int) Math.round(volume * 100.0);
    }

    private void ensurePlayer() {
        if (player != null) {
            return;
        }

        URL url = resolveMusicUrl();
        if (url == null) {
            System.err.println("Background music not found: music.wav");
            return;
        }

        Media media = new Media(url.toExternalForm());
        player = new MediaPlayer(media);
        player.setCycleCount(MediaPlayer.INDEFINITE);
        player.setVolume(volume);
        player.setOnError(() -> {
            if (player.getError() != null) {
                System.err.println("Background music error: " + player.getError().getMessage());
            }
        });
    }

    private static URL resolveMusicUrl() {
        URL classpath = BackgroundMusicManager.class.getResource(RESOURCE_PATH);
        if (classpath != null) {
            return classpath;
        }

        Path[] candidates = {
                Paths.get("music.wav"),
                Paths.get("src", "main", "resources", "music.wav")
        };
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                try {
                    return candidate.toUri().toURL();
                } catch (MalformedURLException ignored) {
                }
            }
        }
        return null;
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
