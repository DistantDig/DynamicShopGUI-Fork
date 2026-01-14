package com.Lino.dynamicShopGUI.gui;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import com.Lino.dynamicShopGUI.managers.GUIManager;
import com.Lino.dynamicShopGUI.models.ShopItem;
import com.Lino.dynamicShopGUI.utils.GUIUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

public class CategoryMenuGUI {

    private final DynamicShopGUI plugin;
    private final GUIManager guiManager;

    public CategoryMenuGUI(DynamicShopGUI plugin, GUIManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }

    public void open(Player player, String category, int page) {
        String displayName = plugin.getShopConfig().getCategoryDisplayName(category.toLowerCase());
        String title = plugin.getShopConfig().getMessage("gui.category-title", "%category%", displayName);
        Inventory inv = Bukkit.createInventory(null, 54, title);

        ItemStack glassFiller = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glassFiller.getItemMeta();
        glassMeta.setDisplayName(" ");
        glassFiller.setItemMeta(glassMeta);

        for (int i = 45; i < 54; i++) {
            inv.setItem(i, glassFiller);
        }

        ItemStack decorGlass = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta decorMeta = decorGlass.getItemMeta();
        decorMeta.setDisplayName(" ");
        decorGlass.setItemMeta(decorMeta);

        inv.setItem(45, decorGlass);
        inv.setItem(53, decorGlass);

        plugin.getDatabaseManager().getItemsByCategory(category).thenAccept(items -> {
            List<Map.Entry<Material, ShopItem>> sortedItems = new ArrayList<>(items.entrySet());
            sortedItems.sort(Map.Entry.comparingByKey());

            int itemsPerPage = plugin.getShopConfig().getItemsPerPage();
            int totalPages = (int) Math.ceil((double) sortedItems.size() / itemsPerPage);
            final int finalPage = Math.max(0, Math.min(page, totalPages - 1));

            int startIndex = finalPage * itemsPerPage;
            int endIndex = Math.min(startIndex + itemsPerPage, sortedItems.size());

            int slot = 0;
            for (int i = startIndex; i < endIndex && slot < 45; i++) {
                Map.Entry<Material, ShopItem> entry = sortedItems.get(i);
                ShopItem shopItem = entry.getValue();

                double restockThreshold = plugin.getShopConfig().getRestockTriggerThreshold();
                if (shopItem.getStock() < shopItem.getMaxStock() * restockThreshold && plugin.getShopConfig().isRestockEnabled()) {
                    if (!plugin.getRestockManager().isRestocking(shopItem.getMaterial())) {
                        plugin.getRestockManager().startRestockTimer(shopItem.getMaterial());
                    }
                }

                ItemStack displayItem = createShopItemDisplay(shopItem);
                inv.setItem(slot++, displayItem);
            }

            if (finalPage > 0) {
                ItemStack prevPage = new ItemStack(Material.ARROW);
                ItemMeta prevMeta = prevPage.getItemMeta();
                prevMeta.setDisplayName(plugin.getShopConfig().getMessage("gui.previous-page"));
                List<String> prevLore = new ArrayList<>();
                prevLore.add(plugin.getShopConfig().getMessage("gui.page-label",
                        "%current%", String.valueOf(finalPage),
                        "%total%", String.valueOf(totalPages)));
                prevMeta.setLore(prevLore);
                prevPage.setItemMeta(prevMeta);
                inv.setItem(48, prevPage);
            }

            ItemStack backButton = new ItemStack(Material.ARROW);
            ItemMeta backMeta = backButton.getItemMeta();
            backMeta.setDisplayName(plugin.getShopConfig().getMessage("gui.back-to-categories"));
            backButton.setItemMeta(backMeta);
            inv.setItem(49, backButton);

            if (plugin.getShopConfig().getAllCategories().containsKey("bulk_" + category.toLowerCase())) {
                ItemStack bulkSellButton = new ItemStack(Material.CHEST_MINECART);
                ItemMeta bulkMeta = bulkSellButton.getItemMeta();
                bulkMeta.setDisplayName(plugin.getShopConfig().getMessage("gui.bulk-sell-button"));
                bulkSellButton.setItemMeta(bulkMeta);
                inv.setItem(52, bulkSellButton);
            }

            if (finalPage < totalPages - 1) {
                ItemStack nextPage = new ItemStack(Material.ARROW);
                ItemMeta nextMeta = nextPage.getItemMeta();
                nextMeta.setDisplayName(plugin.getShopConfig().getMessage("gui.next-page"));
                List<String> nextLore = new ArrayList<>();
                nextLore.add(plugin.getShopConfig().getMessage("gui.page-label",
                        "%current%", String.valueOf(finalPage + 2),
                        "%total%", String.valueOf(totalPages)));
                nextMeta.setLore(nextLore);
                nextPage.setItemMeta(nextMeta);
                inv.setItem(50, nextPage);
            }

            if (totalPages > 1) {
                ItemStack pageInfo = new ItemStack(Material.PAPER);
                ItemMeta pageMeta = pageInfo.getItemMeta();
                pageMeta.setDisplayName(plugin.getShopConfig().getMessage("gui.page-info",
                        "%current%", String.valueOf(finalPage + 1),
                        "%total%", String.valueOf(totalPages)));
                List<String> pageLore = new ArrayList<>();
                pageLore.add(plugin.getShopConfig().getMessage("gui.total-items", "%amount%", String.valueOf(sortedItems.size())));
                pageLore.add(plugin.getShopConfig().getMessage("gui.items-per-page", "%amount%", String.valueOf(itemsPerPage)));
                pageMeta.setLore(pageLore);
                pageInfo.setItemMeta(pageMeta);
                inv.setItem(52, pageInfo);
            }

            double taxRate = plugin.getShopConfig().getTaxRate(category) * 100;
            ItemStack categoryInfo = new ItemStack(plugin.getShopConfig().getCategoryIcon(category));
            ItemMeta categoryMeta = categoryInfo.getItemMeta();
            categoryMeta.setDisplayName(plugin.getShopConfig().getMessage("gui.player-name", "%name%", displayName));
            List<String> categoryLore = new ArrayList<>();
            categoryLore.add("");
            categoryLore.add(plugin.getShopConfig().getMessage("gui.category-tax", "%tax%", String.format("%.1f", taxRate)));
            categoryLore.add(plugin.getShopConfig().getMessage("gui.category-items", "%items%", String.valueOf(sortedItems.size())));
            categoryMeta.setLore(categoryLore);
            categoryInfo.setItemMeta(categoryMeta);
            inv.setItem(46, categoryInfo);

            Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(inv));
        });
    }

    private ItemStack createShopItemDisplay(ShopItem item) {
        ItemStack display = new ItemStack(item.getMaterial());
        ItemMeta meta = display.getItemMeta();

        meta.setDisplayName(plugin.getShopConfig().getMessage("gui.item-name", "%item%",
                GUIUtils.formatMaterialName(item.getMaterial())));

        List<String> lore = new ArrayList<>();

        if (plugin.getShopConfig().showStock()) {
            lore.add(plugin.getShopConfig().getMessage("gui.stock-display",
                    "%stock%", String.valueOf(item.getStock()),
                    "%max%", String.valueOf(item.getMaxStock())));
        }

        lore.add("");
        lore.add(plugin.getShopConfig().getMessage("gui.buy-price",
                "%price%", String.format("%.2f", item.getBuyPrice())));

        double sellPrice = item.getSellPrice();
        if (plugin.getShopConfig().showTaxInfo() && plugin.getShopConfig().isTaxEnabled()) {
            double tax = plugin.getShopConfig().calculateTax(item.getMaterial(), item.getCategory(), sellPrice);
            double netPrice = sellPrice - tax;

            if (tax > 0) {
                lore.add(plugin.getShopConfig().getMessage("gui.sell-price-with-tax",
                        "%price%", String.format("%.2f", sellPrice),
                        "%net%", String.format("%.2f", netPrice)));
            } else {
                lore.add(plugin.getShopConfig().getMessage("gui.sell-price",
                        "%price%", String.format("%.2f", sellPrice)) + " " +
                        plugin.getShopConfig().getMessage("gui.tax-exempt"));
            }
        } else {
            lore.add(plugin.getShopConfig().getMessage("gui.sell-price",
                    "%price%", String.format("%.2f", sellPrice)));
        }

        if (plugin.getShopConfig().showPriceTrends()) {
            lore.add("");
            lore.add(GUIUtils.formatPriceChange(plugin, item));
        }

        if (plugin.getRestockManager().isRestocking(item.getMaterial())) {
            String countdown = plugin.getRestockManager().getRestockCountdown(item.getMaterial());
            if (countdown != null) {
                lore.add("");
                if (item.getStock() == 0) {
                    lore.add(plugin.getShopConfig().getMessage("restock.out-of-stock-purchase"));
                } else {
                    lore.add(plugin.getShopConfig().getMessage("restock.restocking"));
                }
                lore.add(plugin.getShopConfig().getMessage("restock.countdown", "%time%", countdown));
                if (item.getStock() == 0) {
                    lore.add("");
                    lore.add(plugin.getShopConfig().getMessage("restock.can-still-sell"));
                    lore.add(plugin.getShopConfig().getMessage("gui.right-click-sell"));
                }
            }
        } else if (item.getStock() == 0) {
            lore.add("");
            lore.add(plugin.getShopConfig().getMessage("restock.out-of-stock-purchase"));
            lore.add(plugin.getShopConfig().getMessage("restock.can-still-sell"));
            lore.add(plugin.getShopConfig().getMessage("gui.right-click-sell"));
        } else {
            lore.add("");
            lore.add(plugin.getShopConfig().getMessage("gui.left-click-buy"));
            lore.add(plugin.getShopConfig().getMessage("gui.right-click-sell"));
        }

        double decayThreshold = plugin.getShopConfig().getStockDecayTriggerThreshold();
        if (plugin.getShopConfig().isStockDecayEnabled() && item.getStock() >= item.getMaxStock() * decayThreshold) {
            String decayCountdown = plugin.getRestockManager().getDecayCountdown();
            if (decayCountdown != null) {
                lore.add("");
                lore.add(plugin.getShopConfig().getMessage("restock.decay-countdown", "%time%", decayCountdown));
            }
        }

        meta.setLore(lore);
        display.setItemMeta(meta);

        return display;
    }
}