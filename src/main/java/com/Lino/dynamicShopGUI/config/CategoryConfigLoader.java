package com.Lino.dynamicShopGUI.config;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class CategoryConfigLoader {
    private final DynamicShopGUI plugin;
    private final Map<String, CategoryConfig> categories = new HashMap<>();

    private final String[] CATEGORY_FILES = {
            "armor.yml", "building.yml", "farming.yml", "food.yml", "floral.yml",
            "ores.yml", "redstone.yml", "tools.yml", "mobdrops.yml", "fish.yml",
            "bulk_fish.yml", "bulk_food.yml", "bulk_tools.yml"
    };

    public CategoryConfigLoader(DynamicShopGUI plugin) {
        this.plugin = plugin;
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        loadCategories();
    }

    public void reload() {
        categories.clear();
        loadCategories();
    }

    private void loadCategories() {
        for (String fileName : CATEGORY_FILES) {
            loadCategoryFile(fileName);
        }

        plugin.getLogger().info("Loaded " + categories.size() + " shop categories.");
    }

    private void loadCategoryFile(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);

        if (!file.exists()) {
            try {
                if (plugin.getResource(fileName) != null) {
                    plugin.saveResource(fileName, false);
                } else {
                    return;
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to save default " + fileName + ": " + e.getMessage());
                return;
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        String categoryId = fileName.replace(".yml", "").toLowerCase();

        ConfigurationSection categorySection = config.getConfigurationSection("category");
        if (categorySection == null) {
            plugin.getLogger().warning("File " + fileName + " does not have a 'category' section. Skipping.");
            return;
        }

        String displayName = categorySection.getString("display-name", categoryId);
        Material icon = Material.CHEST;
        String iconName = categorySection.getString("icon", "CHEST");
        try {
            icon = Material.valueOf(iconName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid icon material in " + fileName + ": " + iconName);
        }

        double taxRate = categorySection.getDouble("tax-rate", 15.0);

        CategoryConfig categoryConfig = new CategoryConfig(displayName, icon);
        categoryConfig.setTaxRate(taxRate);

        ConfigurationSection defaultsSection = config.getConfigurationSection("defaults");
        int defaultMaxStock = 1000;
        int defaultMinStock = 0;
        int defaultInitialStock = 100;

        if (defaultsSection != null) {
            defaultMaxStock = defaultsSection.getInt("max-stock", 1000);
            defaultMinStock = defaultsSection.getInt("min-stock", 0);
            defaultInitialStock = defaultsSection.getInt("initial-stock", 100);
        }

        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                try {
                    Material material = Material.valueOf(key.toUpperCase());

                    double price;
                    int maxStock = defaultMaxStock;
                    int minStock = defaultMinStock;
                    int initialStock = defaultInitialStock;

                    if (itemsSection.isConfigurationSection(key)) {
                        ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                        price = itemSection.getDouble("price");
                        if (itemSection.contains("max-stock")) maxStock = itemSection.getInt("max-stock");
                        if (itemSection.contains("min-stock")) minStock = itemSection.getInt("min-stock");
                        if (itemSection.contains("initial-stock")) initialStock = itemSection.getInt("initial-stock");
                    } else {
                        price = itemsSection.getDouble(key);
                    }

                    ItemConfig itemConfig = new ItemConfig(price, initialStock, minStock, maxStock);
                    categoryConfig.addItem(material, itemConfig);

                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material in " + fileName + ": " + key);
                }
            }
        }

        categories.put(categoryId, categoryConfig);
    }

    public CategoryConfig getCategory(String name) {
        if (name == null) return null;
        return categories.get(name.toLowerCase());
    }

    public Map<String, CategoryConfig> getAllCategories() {
        return new HashMap<>(categories);
    }

    public static class CategoryConfig {
        private final String displayName;
        private final Material icon;
        private final Map<Material, ItemConfig> items = new HashMap<>();
        private double taxRate = 15.0;

        public CategoryConfig(String displayName, Material icon) {
            this.displayName = displayName;
            this.icon = icon;
        }

        public void addItem(Material material, ItemConfig config) {
            items.put(material, config);
        }

        public String getDisplayName() { return displayName; }
        public Material getIcon() { return icon; }
        public Map<Material, ItemConfig> getItems() { return items; }
        public ItemConfig getItemConfig(Material material) { return items.get(material); }
        public double getTaxRate() { return taxRate; }
        public void setTaxRate(double taxRate) { this.taxRate = taxRate; }
    }

    public static class ItemConfig {
        private final double price;
        private final int initialStock;
        private final int minStock;
        private final int maxStock;

        public ItemConfig(double price, int initialStock, int minStock, int maxStock) {
            this.price = price;
            this.initialStock = initialStock;
            this.minStock = minStock;
            this.maxStock = maxStock;
        }

        public double getPrice() { return price; }
        public int getInitialStock() { return initialStock; }
        public int getMinStock() { return minStock; }
        public int getMaxStock() { return maxStock; }
    }
}