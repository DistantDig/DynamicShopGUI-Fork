package com.Lino.dynamicShopGUI.utils;

import com.Lino.dynamicShopGUI.DynamicShopGUI;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ComponentParser {

    public static final class FishStats {
        public final Double size;
        public final Double weight;
        public final String rarity;

        public FishStats(Double size, Double weight, String rarity) {
            this.size = size;
            this.weight = weight;
            this.rarity = rarity;
        }
    }

    public static FishStats parseFishStats(String componentString) {
        if (componentString == null || componentString.isBlank()) return new FishStats(null,null,null);

        double size = parseNumberField(componentString, "size");
        double weight = parseNumberField(componentString, "weight");
        String rarity = parseStringField(componentString, "rarity");

        return new FishStats(size, weight, rarity);
    }

    public static String parseGearRarity(String componentString) {
        if (componentString == null || componentString.isBlank()) return "";

        return parseStringField(componentString, "tiered_modifier");
    }

    private static double parseNumberField(String componentString, String key) {
        Pattern pattern = Pattern.compile(
                "(?i)(?:\\b|\\\")" + Pattern.quote(key) + "(?:\\b|\\\")\\s*[:=]\\s*([-+]?\\d+(?:\\.\\d+)?)([dDfFlL]?)"
        );
        Matcher matcher = pattern.matcher(componentString);
        if (!matcher.find()) return 0;

        String number = matcher.group(1);
        try {
            return Double.parseDouble(number);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static String parseStringField(String componentString, String key) {
        Pattern fishPattern = Pattern.compile(
                "(?i)(?:\\b|\\\")" + Pattern.quote(key) + "(?:\\b|\\\")\\s*[:=]\\s*(\"[^\"]*\"|'[^']*'|[^,}\\]\\s]+)"
        );
        Pattern gearPattern = Pattern.compile(
                "\\b" + Pattern.quote(key) + "\\s*=\\s*(?:\\\\\")?\"(.*?)(?:\\\\\")?\""
        );
        Matcher matcher = gearPattern.matcher(componentString);
        DynamicShopGUI.getInstance().getLogger().info("Regex: " + gearPattern.matcher(componentString).find());

        if (matcher.find()) {
            String value = matcher.group(1).trim(); // e.g. tiered:standard_weapons/common
            if (value.isEmpty()) return null;
            DynamicShopGUI.getInstance().getLogger().info("Value: " + value);

            int slash = value.lastIndexOf('/');
            DynamicShopGUI.getInstance().getLogger().info("Slash index: " + slash);
            if (slash < 0 || slash == value.length() - 1) return null;

            String rarity = value.substring(slash + 1).trim().toLowerCase(Locale.ROOT);
            DynamicShopGUI.getInstance().getLogger().info("Rarity: " + rarity);
            if (rarity.isEmpty()) return null;

            return rarity;

        } else {
            matcher = fishPattern.matcher(componentString);
            if (!matcher.find()) return null;

            String raw = matcher.group(1);
            return stripQuotes(raw.trim());
        }


    }

    private static String stripQuotes(String string) {
        if (string == null) return null;
        string = string.trim();
        if ((string.startsWith("\"") && string.endsWith("\"")) || (string.startsWith("'") && string.endsWith("'"))) {
            return string.substring(1, string.length() - 1);
        }
        return string;
    }
}
