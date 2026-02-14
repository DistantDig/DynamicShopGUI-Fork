package com.Lino.dynamicShopGUI.handlers;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import com.Lino.dynamicShopGUI.config.CategoryConfigLoader;
import com.Lino.dynamicShopGUI.gui.BulkSellMenuGUI;
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

        Bukkit.getScheduler().runTask(plugin, player::updateInventory);
    }

    private void processSellTransaction(Player player) {
        final String category = plugin.getGUIManager().getPlayerCategory(player.getUniqueId());

        // Snapshot sell items first to avoid reading changing inventory later
        final Map<Integer, ItemStack> itemsToSell = new LinkedHashMap<>();
        for (int slot : BulkSellMenuGUI.SELL_SLOTS) {
            ItemStack item = player.getOpenInventory().getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                itemsToSell.put(slot, item.clone());
            }
        }

        if (itemsToSell.isEmpty()) {
            player.sendMessage(plugin.getShopConfig().getPrefix() + plugin.getShopConfig().getMessage("errors.nothing-to-sell"));
            if (plugin.getShopConfig().isSoundEnabled()) {
                player.playSound(player.getLocation(), "entity.villager.no", 0.5f, 1.0f);
            }
            return;
        }

        List<CompletableFuture<SellComputation>> futures = new ArrayList<>();
        for (Map.Entry<Integer, ItemStack> entry : itemsToSell.entrySet()) {
            int slot = entry.getKey();
            ItemStack stack = entry.getValue();
            futures.add(sellItemStack(player, stack, stack.getAmount(), slot));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream().map(CompletableFuture::join).toList())
                .thenAccept(results -> plugin.getServer().getScheduler().runTask(plugin, () -> {
                    // If any failed, abort safely and return items
                    Optional<SellComputation> failed = results.stream().filter(r -> !r.success).findFirst();
                    if (failed.isPresent()) {
                        player.sendMessage(plugin.getShopConfig().getPrefix() + failed.get().message);
                        if (plugin.getShopConfig().isSoundEnabled()) {
                            player.playSound(player.getLocation(), "entity.villager.no", 0.5f, 1.0f);
                        }
                        returnItemsToPlayer(player, true);
                        plugin.getGUIManager().openBulkSellMenu(player, category);
                        return;
                    }

                    double grossTotal = 0.0;
                    double taxTotal = 0.0;
                    double netTotal = 0.0;
                    int totalAmount = 0;

                    // Apply inventory changes + logging exactly once on main thread
                    for (SellComputation r : results) {
                        player.getOpenInventory().setItem(r.slot, new ItemStack(Material.AIR));

                        grossTotal += r.grossValue;
                        taxTotal += r.tax;
                        netTotal += r.netValue;
                        totalAmount += r.amount;

                        plugin.getDatabaseManager().logTransaction(
                                player.getUniqueId().toString(),
                                r.material,
                                "SELL",
                                r.amount,
                                r.unitPrice,
                                r.netValue
                        );
                    }

                    grossTotal = BigDecimal.valueOf(grossTotal).setScale(2, RoundingMode.HALF_UP).doubleValue();
                    taxTotal = BigDecimal.valueOf(taxTotal).setScale(2, RoundingMode.HALF_UP).doubleValue();
                    netTotal = BigDecimal.valueOf(netTotal).setScale(2, RoundingMode.HALF_UP).doubleValue();

                    // ONE payment
                    plugin.getEconomy().depositPlayer(player, netTotal);
                    plugin.getItemWorthManager().clearCache();

                    String message;
                    if (taxTotal > 0) {
                        message = plugin.getShopConfig().getMessage(
                                "transaction.sell-success-with-tax",
                                "%amount%", String.valueOf(totalAmount),
                                "%item%", "items",
                                "%price%", String.format("%.2f", netTotal),
                                "%tax%", String.format("%.2f", taxTotal)
                        );
                    } else {
                        message = plugin.getShopConfig().getMessage(
                                "transaction.sell-success",
                                "%amount%", String.valueOf(totalAmount),
                                "%item%", "items",
                                "%price%", String.format("%.2f", netTotal)
                        );
                    }

                    player.sendMessage(plugin.getShopConfig().getPrefix() + message);
                    if (plugin.getShopConfig().isSoundEnabled()) {
                        player.playSound(player.getLocation(), "entity.experience_orb.pickup", 0.7f, 1.0f);
                    }

                    // reopen ONCE
                    plugin.getGUIManager().openBulkSellMenu(player, category);
                }))
                .exceptionally(throwable -> {
                    throwable.printStackTrace();
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        player.sendMessage(plugin.getShopConfig().getMessage("errors.transaction-error"));
                        returnItemsToPlayer(player, true);
                        plugin.getGUIManager().openBulkSellMenu(player, category);
                    });
                    return null;
                });
    }

    private CompletableFuture<SellComputation> sellItemStack(Player player, ItemStack itemStack, int amount, int slot) {
        Material material = itemStack.getType();
        String category = plugin.getGUIManager().getPlayerCategory(player.getUniqueId());
        double configValue = getConfigPrice(itemStack, category);

        return plugin.getDatabaseManager().getShopItem(material).thenApply(item -> {
            if (item == null && !isAllowedItem(itemStack, category)) {
                return SellComputation.failure(slot, "This item cannot be sold");
            }

            double grossValue = addModifiers(itemStack, configValue, category) * amount;
            grossValue = BigDecimal.valueOf(grossValue).setScale(2, RoundingMode.HALF_UP).doubleValue();

            double tax = plugin.getShopConfig().calculateTax(material, category, grossValue);
            tax = BigDecimal.valueOf(tax).setScale(2, RoundingMode.HALF_UP).doubleValue();

            double netValue = BigDecimal.valueOf(grossValue - tax).setScale(2, RoundingMode.HALF_UP).doubleValue();

            double unitPrice = amount > 0
                    ? BigDecimal.valueOf(grossValue / amount).setScale(2, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;

            return SellComputation.success(slot, material, amount, grossValue, tax, netValue, unitPrice);
        }).exceptionally(throwable -> {
            throwable.printStackTrace();
            return SellComputation.failure(slot, "An error occurred during the transaction");
        });
    }

    private double addModifiers(ItemStack itemStack, double baseValue, String category) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null || category == null) return baseValue;

        switch (category) {
            case "bulk_food" -> {
                FoodReader.FoodStats foodStats = FoodReader.readFoodStats(itemStack);
                return baseValue + ((double) foodStats.nutrition() / 8) + (foodStats.saturationModifier() / 8);
            }
            case "bulk_fish" -> {
                String component = itemMeta.getAsComponentString();
                if (component != null && component.contains("starcatcher")) {
                    ComponentParser.FishStats fishStats = ComponentParser.parseFishStats(component);
                    int rarityModifier = switch (fishStats.rarity) {
                        case "uncommon" -> 2;
                        case "rare" -> 3;
                        case "epic" -> 4;
                        case "legendary" -> 5;
                        default -> 1;
                    };
                    return (baseValue + (fishStats.size / 100.0) + (fishStats.weight / 1000.0)) * rarityModifier;
                }
                return baseValue;
            }
            case "bulk_tools" -> {
                ItemStatsReader.CombatStats gearStats = ItemStatsReader.getCombatStats(itemStack);
                int rarityModifier = switch (gearStats.rarity()) {
                    case "reinforced", "resilient", "keen", "extended", "critical", "swift" -> 2;
                    case "rare", "fortified", "sharp", "hasteful", "blessed", "warded" -> 3;
                    case "epic", "air_infused", "air_infused_melee", "arcane_infused", "arcane_infused_melee",
                         "eath_infused", "earth_infused_melee", "fire_infused", "fire_infused_melee",
                         "frost_infused", "frost_infused_melee", "light_infused", "light_infused_melee",
                         "water_infused", "water_infused_melee" -> 4;
                    case "legendary" -> 5;
                    default -> 1;
                };
                return (baseValue + (10 * gearStats.attackDamage()) + (10 * gearStats.armor())) * rarityModifier;
            }
            default -> {
                plugin.getLogger().warning("Category " + category + " is unaccounted for.");
                return baseValue;
            }
        }
    }

    // Helper DTO
    private static final class SellComputation {
        final boolean success;
        final int slot;
        final Material material;
        final int amount;
        final double grossValue;
        final double tax;
        final double netValue;
        final double unitPrice;
        final String message;

        private SellComputation(boolean success, int slot, Material material, int amount,
                                double grossValue, double tax, double netValue, double unitPrice, String message) {
            this.success = success;
            this.slot = slot;
            this.material = material;
            this.amount = amount;
            this.grossValue = grossValue;
            this.tax = tax;
            this.netValue = netValue;
            this.unitPrice = unitPrice;
            this.message = message;
        }

        static SellComputation success(int slot, Material material, int amount,
                                       double grossValue, double tax, double netValue, double unitPrice) {
            return new SellComputation(true, slot, material, amount, grossValue, tax, netValue, unitPrice, null);
        }

        static SellComputation failure(int slot, String message) {
            return new SellComputation(false, slot, Material.AIR, 0, 0, 0, 0, 0, message);
        }
    }
}
