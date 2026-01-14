package com.Lino.dynamicShopGUI.listeners;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import com.Lino.dynamicShopGUI.gui.BulkSellMenuGUI;
import com.Lino.dynamicShopGUI.handlers.BulkSellMenuHandler;
import com.Lino.dynamicShopGUI.handlers.CategoryMenuHandler;
import com.Lino.dynamicShopGUI.handlers.MainMenuHandler;
import com.Lino.dynamicShopGUI.handlers.TransactionMenuHandler;
import com.Lino.dynamicShopGUI.managers.GUIManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.Lino.dynamicShopGUI.managers.GUIManager.GUIType.BULK_SELL;

public class ShopListener implements Listener {

    private final DynamicShopGUI plugin;
    private final MainMenuHandler mainMenuHandler;
    private final CategoryMenuHandler categoryMenuHandler;
    private final TransactionMenuHandler transactionMenuHandler;
    private final BulkSellMenuHandler bulkSellMenuHandler;

    public ShopListener(DynamicShopGUI plugin) {
        this.plugin = plugin;
        this.mainMenuHandler = new MainMenuHandler(plugin);
        this.categoryMenuHandler = new CategoryMenuHandler(plugin);
        this.transactionMenuHandler = new TransactionMenuHandler(plugin);
        this.bulkSellMenuHandler = new BulkSellMenuHandler(plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        GUIManager.GUIType guiType = plugin.getGUIManager().getPlayerGUIType(player.getUniqueId());
        if (guiType == null) return;

        ItemStack clicked = event.getCurrentItem();
        ItemStack held = event.getCursor();
        if ((clicked == null || clicked.getType() == Material.AIR)
        && (held == null || held.getType() == Material.AIR)) return;

        if (plugin.getShopConfig().isSoundEnabled()) {
            player.playSound(player.getLocation(), "ui.button.click", 0.5f, 1.0f);
        }

        String title = ChatColor.stripColor(event.getView().getTitle());

        switch (guiType) {
            case MAIN_MENU:
                event.setCancelled(true);
                if (event.getClickedInventory() != event.getView().getTopInventory()) { return; }
                mainMenuHandler.handleClick(player, clicked, event.getSlot());
                break;
            case CATEGORY_MENU:
                event.setCancelled(true);
                if (event.getClickedInventory() != event.getView().getTopInventory()) { return; }
                categoryMenuHandler.handleClick(player, clicked, event.getClick(), title, event.getSlot());
                break;
            case TRANSACTION_BUY:
                event.setCancelled(true);
                if (event.getClickedInventory() != event.getView().getTopInventory()) { return; }
                transactionMenuHandler.handleClick(player, clicked, true, event.getSlot());
                break;
            case TRANSACTION_SELL:
                event.setCancelled(true);
                if (event.getClickedInventory() != event.getView().getTopInventory()) { return; }
                transactionMenuHandler.handleClick(player, clicked, false, event.getSlot());
                break;
            case BULK_SELL:
                bulkSellMenuHandler.handleClick(event);
                break;
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getCursor() == null || event.getCursor().getAmount() <= 1) { return; }
        GUIManager.GUIType guiType = plugin.getGUIManager().getPlayerGUIType(player.getUniqueId());
        if (guiType == null) return;

        if (guiType == BULK_SELL) {
            bulkSellMenuHandler.handleDrag(event);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getPlayer();

        if (plugin.getGUIManager().getPlayerGUIType(player.getUniqueId()) != null) {
            // Collect items to return for bulk sell GUI
            List<ItemStack> itemsToReturn = new ArrayList<>();
            if (BULK_SELL == plugin.getGUIManager().getPlayerGUIType(player.getUniqueId())) {
                for (int slot : BulkSellMenuGUI.SELL_SLOTS) {
                    ItemStack item = event.getInventory().getItem(slot);
                    if (item != null && item.getType() != Material.AIR) {
                        itemsToReturn.add(item);
                    }
                }
            }

            delayReturnItems(player, itemsToReturn);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (plugin.getGUIManager().getPlayerGUIType(player.getUniqueId()) != null) {
            // Collect items to return for bulk sell GUI
            List<ItemStack> itemsToReturn = new ArrayList<>();
            if (BULK_SELL == plugin.getGUIManager().getPlayerGUIType(player.getUniqueId())) {
                for (int slot : BulkSellMenuGUI.SELL_SLOTS) {
                    ItemStack item = player.getOpenInventory().getTopInventory().getItem(slot);
                    if (item != null && item.getType() != Material.AIR) {
                        itemsToReturn.add(item);
                    }
                }
            }

            delayReturnItems(player, itemsToReturn);
        }
    }

    private void delayReturnItems(Player player, List<ItemStack> itemsToReturn) {
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    for (ItemStack leftover : itemsToReturn) {
                        player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                    }
                }

                if (player.getOpenInventory().getTopInventory().getType() == org.bukkit.event.inventory.InventoryType.CRAFTING) {
                    plugin.getGUIManager().clearPlayerData(player.getUniqueId());
                }

                // Return items if any
                if (plugin.getGUIManager().getPlayerGUIType(player.getUniqueId()) == null) {
                    for (ItemStack item : itemsToReturn) {
                        HashMap<Integer, ItemStack> notReturned = player.getInventory().addItem(item);
                        if (!notReturned.isEmpty()) {
                            for (ItemStack leftover : notReturned.values()) {
                                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                            }
                        }
                    }
                }
            }
        }.runTaskLater(plugin, 1L);
    }
}