package dev.bencke.robots.utils;

import org.bukkit.ChatColor;

import java.util.List;
import java.util.stream.Collectors;

public class ColorUtil {

    public static String colorize(String text) {
        if (text == null) return null;
        return text.replace('&', '§');
    }

    public static List<String> colorize(List<String> texts) {
        return texts.stream()
                .map(ColorUtil::colorize)
                .collect(Collectors.toList());
    }
}