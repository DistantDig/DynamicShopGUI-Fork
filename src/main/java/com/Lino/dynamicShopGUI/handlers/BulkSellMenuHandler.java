package com.Lino.dynamicShopGUI.handlers;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import com.Lino.dynamicShopGUI.config.CategoryConfigLoader;
import com.Lino.dynamicShopGUI.gui.BulkSellMenuGUI;
import com.Lino.dynamicShopGUI.managers.ShopManager;
import com.Lino.dynamicShopGUI.utils.ComponentParser;
import com.Lino.dynamicShopGUI.utils.FoodReader;
import com.Lino.dynamicShopGUI.utils.ItemStatsReader;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.Lino.dynamicShopGUI.gui.BulkSellMenuGUI.SELL_SLOT;
import static com.Lino.dynamicShopGUI.utils.GUIUtils.formatMaterialName;

public class BulkSellMenuHandler {

    private final DynamicShopGUI plugin;
    private double currentTotalValue = 0;

    public BulkSellMenuHandler(DynamicShopGUI plugin) {
        this.plugin = plugin;
    }

    public void handleClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack itemStack = event.getCurrentItem();
        Material type = itemStack != null ? itemStack.getType() : Material.AIR;
        Inventory clickedInventory = event.getClickedInventory();
        int slot = event.getSlot();

        if (clickedInventory == null) { return; }

        if (slot == BulkSellMenuGUI.BACK_SLOT && clickedInventory == event.getView().getTopInventory()) {
            String category = plugin.getGUIManager().getPlayerCategory(player.getUniqueId());

            event.setCancelled(true);
            returnItemsToPlayer(player, true);
            if (category != null) {
                int page = plugin.getGUIManager().getPlayerPage(player.getUniqueId());
                plugin.getGUIManager().openCategoryMenu(player, category.split("_")[1], page);
            } else {
                player.closeInventory();
            }
            return;
        }

        if (slot == BulkSellMenuGUI.CLEAR_SLOT && clickedInventory == event.getView().getTopInventory()) {
            event.setCancelled(true);
            returnItemsToPlayer(player, true);
            return;
        }

        if (slot == SELL_SLOT && clickedInventory == event.getView().getTopInventory()) {
            event.setCancelled(true);
            processSellTransaction(player);
            return;
        }

        if (Arrays.stream(BulkSellMenuGUI.SELL_SLOTS).noneMatch( sellSlot -> sellSlot == slot)
                && (Objects.equals(clickedInventory, event.getView().getTopInventory()))) {
            event.setCancelled(true);
            return;
        }

        if (clickedInventory == event.getView().getTopInventory() && (
                type == Material.BLACK_STAINED_GLASS_PANE || type == Material.GRAY_STAINED_GLASS_PANE ||
                type == Material.IRON_BLOCK || type == Material.COAL_BLOCK ||
                type == Material.EMERALD_BLOCK || type == Material.REDSTONE_BLOCK ||
                type == Material.LAPIS_BLOCK || type == Material.LIME_STAINED_GLASS ||
                type == Material.RED_STAINED_GLASS)) {
            event.setCancelled(true);
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                returnItemsToPlayer(player, false);
                updateTotal(player);
            }
        }.runTaskLater(plugin, 1L);
    }

    public void handleDrag(InventoryDragEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack itemStack = event.getOldCursor();

        plugin.getLogger().info("Held item: " + itemStack);
        plugin.getLogger().info("Item allowed: " +
                isAllowedItem(itemStack, plugin.getGUIManager().getPlayerCategory(player.getUniqueId())));
        plugin.getLogger().info("Inventory match: " +
                Objects.equals(event.getInventory(), event.getView().getTopInventory()));

        new BukkitRunnable() {
            @Override
            public void run() {
                returnItemsToPlayer(player, false);
                updateTotal(player);
            }
        }.runTaskLater(plugin, 1L);
    }

    public void returnItemsToPlayer(Player player, boolean allItems) {
        for (int slot : BulkSellMenuGUI.SELL_SLOTS) {
            ItemStack item = player.getOpenInventory().getItem(slot);

            if ((item != null && item.getType() != Material.AIR)
                    && (allItems || !isAllowedItem(item, plugin.getGUIManager().getPlayerCategory(player.getUniqueId())))) {

                HashMap<Integer, ItemStack> notReturned = player.getInventory().addItem(item);
                if (plugin.getShopConfig().isSoundEnabled() && !allItems) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 0.5f, 1.0f);
                }

                if (!notReturned.isEmpty()) {
                    for (ItemStack leftover : notReturned.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                    }
                }
                player.getOpenInventory().setItem(slot, null);
            }
        }
    }

    private boolean isAllowedItem(ItemStack itemStack, String category) {
        CategoryConfigLoader.CategoryConfig categoryConfig = plugin.getShopConfig().getCategoryLoader().getCategory(category);
        if (categoryConfig == null) return false;

        CategoryConfigLoader.ItemConfig itemConfig = categoryConfig.getItemConfig(itemStack.getType());
        boolean isDisabledItem = itemConfig != null && itemConfig.getPrice() == 0;
        if (isDisabledItem) return false;
        ItemMeta itemMeta = itemStack.getItemMeta();

        if (itemMeta != null) {
            if (category.contains("fish")) {
                boolean isStarcatcher = itemMeta.getAsComponentString().contains("starcatcher");

                if (isStarcatcher) {
                    return true;
                }
            }

            else if (category.contains("food")) {
                boolean isFood = FoodReader.readFoodStats(itemStack).nutrition() > 0;
                plugin.getLogger().info("IsAllowed check, has food: " + isFood);
                return isFood;
            }

            else if (category.contains("tools")) {
                ItemStatsReader.CombatStats stats = ItemStatsReader.getCombatStats(itemStack);
                if (stats.attackDamage() != 0 || stats.armor() != 0) return true;
            }
        }
        Set<String> shopItems = plugin.getShopConfig().getShopItems().get(category).keySet();
        return shopItems.contains(itemStack.getType().toString());
    }

    private double getConfigPrice(ItemStack itemStack, String category) {
        CategoryConfigLoader.CategoryConfig categoryConfig = plugin.getShopConfig().getCategoryLoader().getCategory(category);
        if (categoryConfig == null) return 0;
        CategoryConfigLoader.ItemConfig itemConfig = categoryConfig.getItemConfig(itemStack.getType());
        return itemConfig != null ? itemConfig.getPrice() : 0;
    }

    private ItemMeta createNewSellButtonMeta(double totalAmount) {
        ItemStack sellButton = new ItemStack(Material.EMERALD);
        ItemMeta sellMeta = sellButton.getItemMeta();
        sellMeta.setDisplayName(plugin.getShopConfig().getMessage("gui.bulk-sell"));
        sellMeta.setLore(List.of(plugin.getShopConfig().getMessage("gui.bulk-sell-total",
                "%total%", String.valueOf(totalAmount))));
        return sellMeta;
    }

    private void updateTotal(Player player) {
        String category = plugin.getGUIManager().getPlayerCategory(player.getUniqueId());
        Inventory inv = player.getOpenInventory().getTopInventory();
        currentTotalValue = 0;

        for (int slot : BulkSellMenuGUI.SELL_SLOTS) {
            ItemStack item = player.getOpenInventory().getItem(slot);

            if (item != null && item.getType() != Material.AIR) {
                double configPrice = getConfigPrice(item, category);
                currentTotalValue += addModifiers(item, configPrice, category) * item.getAmount();
            }
        }
        currentTotalValue = BigDecimal.valueOf(currentTotalValue)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();

        ItemStack sellSlot = inv.getItem(SELL_SLOT);
        ItemMeta sellSlotMeta = createNewSellButtonMeta(currentTotalValue);
        sellSlot.setItemMeta(sellSlotMeta);
        inv.setItem(SELL_SLOT, sellSlot);

        Bukkit.getScheduler().runTask(plugin, () -> player.updateInventory());
    }

    private void processSellTransaction(Player player) {
        for (int slot : BulkSellMenuGUI.SELL_SLOTS) {
            ItemStack item = player.getOpenInventory().getItem(slot);
            String category = plugin.getGUIManager().getPlayerCategory(player.getUniqueId());

            if (item != null && item.getType() != Material.AIR) {
                sellItemStack(player, item, item.getAmount(), slot).thenAccept(result -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {

                        if (result.isSuccess()) {
                            player.sendMessage(plugin.getShopConfig().getPrefix() + result.getMessage());
                            if (plugin.getShopConfig().isSoundEnabled()) {
                                player.playSound(player.getLocation(), "entity.experience_orb.pickup", 0.7f, 1.0f);
                            }
                        }
                        else {
                            player.sendMessage(plugin.getShopConfig().getPrefix() + result.getMessage());
                            if (plugin.getShopConfig().isSoundEnabled()) {
                                player.playSound(player.getLocation(), "entity.villager.no", 0.5f, 1.0f);
                            }
                            returnItemsToPlayer(player, true);
                        }

                        plugin.getGUIManager().openBulkSellMenu(player, category);
                    });
                }).exceptionally(throwable -> {
                    throwable.printStackTrace();

                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        player.sendMessage(plugin.getShopConfig().getMessage("errors.transaction-error"));
                        plugin.getGUIManager().openBulkSellMenu(player, category);
                    });

                    return null;
                });
            }
        }
    }

    private CompletableFuture<ShopManager.TransactionResult> sellItemStack(Player player, ItemStack itemStack, int amount, int slot) {
        Material material = itemStack.getType();
        String category = plugin.getGUIManager().getPlayerCategory(player.getUniqueId());
        double configValue = getConfigPrice(itemStack, category);

        return plugin.getDatabaseManager().getShopItem(material).thenCompose(item -> {
            if (item == null && !isAllowedItem(itemStack, category)) {
                return CompletableFuture.completedFuture(
                        new ShopManager.TransactionResult(false, "This item cannot be sold"));
            }

            player.getOpenInventory().setItem(slot, new ItemStack(Material.AIR));

            double totalValue = addModifiers(itemStack, configValue * itemStack.getAmount(), category);
            totalValue = BigDecimal.valueOf(totalValue)
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();

            double tax = plugin.getShopConfig().calculateTax(material, category, totalValue);
            double netEarnings = totalValue - tax;

            plugin.getEconomy().depositPlayer(player, netEarnings);
            plugin.getDatabaseManager().logTransaction(player.getUniqueId().toString(),
                    material, "SELL", amount, totalValue / amount, netEarnings);
            plugin.getItemWorthManager().clearCache();

            String message;
            if (tax > 0) {
                message = plugin.getShopConfig().getMessage("transaction.sell-success-with-tax",
                        "%amount%", String.valueOf(amount),
                        "%item%", formatMaterialName(material),
                        "%price%", String.format("%.2f", netEarnings),
                        "%tax%", String.format("%.2f", tax));
            } else {
                message = plugin.getShopConfig().getMessage("transaction.sell-success",
                        "%amount%", String.valueOf(amount),
                        "%item%", formatMaterialName(material),
                        "%price%", String.format("%.2f", netEarnings));
            }

            return CompletableFuture.completedFuture(new ShopManager.TransactionResult(true, message));
        }).exceptionally(throwable -> {
            throwable.printStackTrace();
            return new ShopManager.TransactionResult(false, "An error occurred during the transaction");
        });
    }

    private double addModifiers(ItemStack itemStack, double baseValue, String category) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) return baseValue;

        switch (category){
            case "bulk_food":
                FoodReader.FoodStats foodStats = FoodReader.readFoodStats(itemStack);
                return baseValue + ((double) foodStats.nutrition() / 8) + (foodStats.saturationModifier() / 8);
            case "bulk_fish":
                if (itemMeta.getAsComponentString().contains("starcatcher")) {
                    ComponentParser.FishStats fishStats =  ComponentParser.parseFishStats(itemMeta.getAsComponentString());
                    int rarityModifier = switch (fishStats.rarity) {
                        case "uncommon" -> 2;
                        case "rare" -> 3;
                        case "epic" -> 4;
                        case "legendary" -> 5;
                        default -> 1;
                    };

                    return (baseValue + (fishStats.size / 100) + (fishStats.weight / 1000)) * rarityModifier;
                }
                break;
            case "bulk_tools":
                ItemStatsReader.CombatStats gearStats = ItemStatsReader.getCombatStats(itemStack);
                int rarityModifier = switch (gearStats.rarity()) {
                    case "reinforced", "resilient", "keen", "extended", "critical", "swift" -> 2;
                    case "rare", "fortified", "sharp", "hasteful" -> 3;
                    case "epic" -> 4;
                    case "legendary" -> 5;
                    default -> 1;
                };

                return (baseValue + (10 * gearStats.attackDamage()) + (10 * gearStats.armor())) * rarityModifier;
        }
        plugin.getLogger().warning("Category " + category + " is unaccounted for.");
        return baseValue;
    }
}
