package com.Lino.dynamicShopGUI.gui;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import com.Lino.dynamicShopGUI.managers.GUIManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class BulkSellMenuGUI {

    private final DynamicShopGUI plugin;
    private final GUIManager guiManager;

    public static final int[] SELL_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public static final int CLEAR_SLOT = 47;
    public static final int SELL_SLOT = 51;
    public static final int BACK_SLOT = 49;

    public BulkSellMenuGUI(DynamicShopGUI plugin, GUIManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }

    public void open(Player player, String category) {
        String displayName = plugin.getShopConfig().getCategoryDisplayName(category.toLowerCase());
        String title = plugin.getShopConfig().getMessage("gui.bulk-sell-title", "%category%", displayName);

        Inventory inv = Bukkit.createInventory(null, 54, title);

        setupSimplifiedBorders(inv);

        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName(plugin.getShopConfig().getMessage("gui.back"));
        backButton.setItemMeta(backMeta);
        inv.setItem(BACK_SLOT, backButton);

        ItemStack clearButton = new ItemStack(Material.HOPPER);
        ItemMeta clearMeta = clearButton.getItemMeta();
        clearMeta.setDisplayName(plugin.getShopConfig().getMessage("gui.clear-inventory"));
        clearMeta.setLore(List.of(plugin.getShopConfig().getMessage("gui.clear-inventory-desc")));
        clearButton.setItemMeta(clearMeta);
        inv.setItem(CLEAR_SLOT, clearButton);

        ItemStack sellButton = createNewSellButton(inv, 0.0);
        inv.setItem(SELL_SLOT, sellButton);

        Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(inv));
    }

    public ItemStack createNewSellButton(Inventory inv, double totalAmount) {
        ItemStack sellButton = inv.getItem(SELL_SLOT);
        if (sellButton.getType() != Material.EMERALD) {
            sellButton = new ItemStack(Material.EMERALD);
        }
        ItemMeta sellMeta = sellButton.getItemMeta();
        sellMeta.setDisplayName(plugin.getShopConfig().getMessage("gui.bulk-sell"));
        sellMeta.setLore(List.of(plugin.getShopConfig().getMessage("gui.bulk-sell-total", "%total%", String.valueOf(totalAmount))));
        sellButton.setItemMeta(sellMeta);
        return sellButton;
    }

    private void setupSimplifiedBorders(Inventory inv) {
        ItemStack glassFiller = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glassFiller.getItemMeta();
        glassMeta.setDisplayName(" ");
        glassFiller.setItemMeta(glassMeta);

        for (int i = 0; i < 54; i++) {
            inv.setItem(i, glassFiller);
        }

        ItemStack decorBorder = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta decorMeta = decorBorder.getItemMeta();
        decorMeta.setDisplayName(" ");
        decorMeta.addEnchant(Enchantment.PROTECTION, 1, true);
        decorMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        decorBorder.setItemMeta(decorMeta);

        for (int i = 0; i < 9; i++) {
            inv.setItem(i, decorBorder);
            inv.setItem(45 + i, decorBorder);
        }

        for (int row = 1; row < 5; row++) {
            inv.setItem(row * 9, decorBorder);
            inv.setItem(row * 9 + 8, decorBorder);
        }

        ItemStack accentCorner = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta accentMeta = accentCorner.getItemMeta();
        accentMeta.setDisplayName(" ");
        accentCorner.setItemMeta(accentMeta);

        inv.setItem(0, accentCorner);
        inv.setItem(8, accentCorner);
        inv.setItem(45, accentCorner);
        inv.setItem(53, accentCorner);

        for (int i = 10; i <= 16; i++) {
            inv.setItem(i, null);
        }
        for (int i = 19; i <= 25; i++) {
            inv.setItem(i, null);
        }
        for (int i = 28; i <= 34; i++) {
            inv.setItem(i, null);
        }
        for (int i = 37; i <= 43; i++) {
            inv.setItem(i, null);
        }

        inv.setItem(48, glassFiller);
        inv.setItem(49, null);
        inv.setItem(50, glassFiller);
    }
}
