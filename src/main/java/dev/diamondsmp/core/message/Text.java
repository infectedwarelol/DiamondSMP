package dev.diamondsmp.core.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Text {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&')
            .hexCharacter('#')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private static final LegacyComponentSerializer SECTION = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final Pattern MINI_TAG = Pattern.compile("<[!#/a-zA-Z][^<>]*>");
    private static final Pattern LEGACY_HEX = Pattern.compile("&#([0-9a-fA-F]{6})");
    private static final Pattern LEGACY_CODE = Pattern.compile("&([0-9a-fk-orA-FK-OR])");
    private static final String NEWLINE_TOKEN = "%nl%";
    private static final String NEWLINE = "\n";

    private static final Map<Character, String> LEGACY_TAGS = Map.ofEntries(
            Map.entry('0', "black"),
            Map.entry('1', "dark_blue"),
            Map.entry('2', "dark_green"),
            Map.entry('3', "dark_aqua"),
            Map.entry('4', "dark_red"),
            Map.entry('5', "dark_purple"),
            Map.entry('6', "gold"),
            Map.entry('7', "gray"),
            Map.entry('8', "dark_gray"),
            Map.entry('9', "blue"),
            Map.entry('a', "green"),
            Map.entry('b', "aqua"),
            Map.entry('c', "red"),
            Map.entry('d', "light_purple"),
            Map.entry('e', "yellow"),
            Map.entry('f', "white"),
            Map.entry('k', "obfuscated"),
            Map.entry('l', "bold"),
            Map.entry('m', "strikethrough"),
            Map.entry('n', "underlined"),
            Map.entry('o', "italic"),
            Map.entry('r', "reset"));

    private Text() {
    }

    public static Component parse(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        String value = input.replace(NEWLINE_TOKEN, NEWLINE);
        if (value.indexOf('§') >= 0) {
            value = LEGACY.serialize(SECTION.deserialize(value));
        }
        if (MINI_TAG.matcher(value).find()) {
            return MINI.deserialize(toMiniMessage(value));
        }
        return LEGACY.deserialize(value);
    }

    public static Component display(String input) {
        return parse(input).decoration(TextDecoration.ITALIC, false);
    }

    public static List<Component> lore(List<String> lines) {
        List<Component> result = new ArrayList<>(lines.size());
        for (String line : lines) {
            result.add(display(line));
        }
        return result;
    }

    public static Component replace(Component component, String raw, Object... placeholders) {
        Component result = component;
        for (int index = 0; index + 1 < placeholders.length; index += 2) {
            String token = "%" + placeholders[index] + "%";
            if (!raw.contains(token)) {
                continue;
            }
            Object value = placeholders[index + 1];
            Component replacement = value instanceof Component inserted
                    ? inserted
                    : Component.text(String.valueOf(value));
            result = result.replaceText(TextReplacementConfig.builder()
                    .matchLiteral(token)
                    .replacement(replacement)
                    .build());
        }
        return result;
    }

    public static String plain(Component component) {
        return component == null ? "" : PlainTextComponentSerializer.plainText().serialize(component);
    }

    public static boolean isBlank(String input) {
        return input == null || input.isBlank();
    }

    private static String toMiniMessage(String input) {
        if (input.indexOf('&') < 0) {
            return input;
        }
        String hexed = LEGACY_HEX.matcher(input).replaceAll("<#$1>");
        Matcher matcher = LEGACY_CODE.matcher(hexed);
        StringBuilder out = new StringBuilder(hexed.length());
        while (matcher.find()) {
            String tag = LEGACY_TAGS.get(Character.toLowerCase(matcher.group(1).charAt(0)));
            matcher.appendReplacement(out,
                    tag == null ? Matcher.quoteReplacement(matcher.group()) : "<" + tag + ">");
        }
        matcher.appendTail(out);
        return out.toString();
    }
}
