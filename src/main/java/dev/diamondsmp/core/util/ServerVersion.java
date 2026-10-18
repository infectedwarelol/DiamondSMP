package dev.diamondsmp.core.util;

import org.bukkit.Bukkit;

public record ServerVersion(int major, int minor, int patch, String minecraft, String software, boolean folia) {

    public static ServerVersion detect() {
        String minecraft = Bukkit.getMinecraftVersion();
        int[] parts = {0, 0, 0};
        String[] split = minecraft.split("[.\\-]");
        for (int index = 0; index < parts.length && index < split.length; index++) {
            parts[index] = parseLeadingInt(split[index]);
        }
        return new ServerVersion(parts[0], parts[1], parts[2], minecraft, Bukkit.getName(), detectFolia());
    }

    public String describe() {
        return software + " " + minecraft + (folia ? " (regionized)" : "");
    }

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    private static int parseLeadingInt(String value) {
        int result = 0;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (!Character.isDigit(character)) {
                break;
            }
            result = result * 10 + (character - '0');
        }
        return result;
    }
}
