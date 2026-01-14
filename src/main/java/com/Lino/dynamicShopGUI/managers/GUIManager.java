package com.Lino.dynamicShopGUI.managers;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import com.Lino.dynamicShopGUI.gui.BulkSellMenuGUI;
import com.Lino.dynamicShopGUI.gui.MainMenuGUI;
import com.Lino.dynamicShopGUI.gui.CategoryMenuGUI;
import com.Lino.dynamicShopGUI.gui.TransactionMenuGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import java.util.*;

public class GUIManager {

    private final DynamicShopGUI plugin;
    private final Map<UUID, String> playerCategory = new HashMap<>();
    private final Map<UUID, Material> playerSelectedItem = new HashMap<>();
    private final Map<UUID, Integer> playerPage = new HashMap<>();
    private final Map<UUID, GUIType> playerGUIType = new HashMap<>();
    private final Map<UUID, Boolean> playerTransactionType = new HashMap<>();

    private final MainMenuGUI mainMenuGUI;
    private final CategoryMenuGUI categoryMenuGUI;
    private final TransactionMenuGUI transactionMenuGUI;
    private final BulkSellMenuGUI bulkSellMenuGUI;

    public enum GUIType {
        MAIN_MENU,
        CATEGORY_MENU,
        TRANSACTION_BUY,
        TRANSACTION_SELL,
        BULK_SELL
    }

    public GUIManager(DynamicShopGUI plugin) {
        this.plugin = plugin;
        this.mainMenuGUI = new MainMenuGUI(plugin);
        this.categoryMenuGUI = new CategoryMenuGUI(plugin, this);
        this.transactionMenuGUI = new TransactionMenuGUI(plugin, this);
        this.bulkSellMenuGUI = new BulkSellMenuGUI(plugin, this);
    }

    public void openMainMenu(Player player) {
        playerGUIType.put(player.getUniqueId(), GUIType.MAIN_MENU);
        mainMenuGUI.open(player);
    }

    public void setPlayerMainMenu(UUID uuid) {
        playerGUIType.put(uuid, GUIType.MAIN_MENU);
    }

    public void openCategoryMenu(Player player, String category, int page) {
        playerCategory.put(player.getUniqueId(), category);
        playerPage.put(player.getUniqueId(), page);
        playerGUIType.put(player.getUniqueId(), GUIType.CATEGORY_MENU);
        categoryMenuGUI.open(player, category, page);
    }

    public void openTransactionMenu(Player player, Material material, boolean isBuying) {
        playerSelectedItem.put(player.getUniqueId(), material);
        playerTransactionType.put(player.getUniqueId(), isBuying);
        playerGUIType.put(player.getUniqueId(), isBuying ? GUIType.TRANSACTION_BUY : GUIType.TRANSACTION_SELL);
        transactionMenuGUI.open(player, material, isBuying);
    }

    public void openBulkSellMenu(Player player, String category) {
        playerCategory.put(player.getUniqueId(), category);
        playerGUIType.put(player.getUniqueId(), GUIType.BULK_SELL);
        bulkSellMenuGUI.open(player, category);
    }

    public String getPlayerCategory(UUID uuid) {
        return playerCategory.get(uuid);
    }

    public Material getPlayerSelectedItem(UUID uuid) {
        return playerSelectedItem.get(uuid);
    }

    public void setPlayerSelectedItem(UUID uuid, Material material) {
        playerSelectedItem.put(uuid, material);
    }

    public int getPlayerPage(UUID uuid) {
        return playerPage.getOrDefault(uuid, 0);
    }

    public GUIType getPlayerGUIType(UUID uuid) {
        return playerGUIType.get(uuid);
    }

    public boolean getPlayerTransactionType(UUID uuid) {
        return playerTransactionType.getOrDefault(uuid, true);
    }

    public void clearPlayerData(UUID uuid) {
        playerCategory.remove(uuid);
        playerSelectedItem.remove(uuid);
        playerPage.remove(uuid);
        playerGUIType.remove(uuid);
        playerTransactionType.remove(uuid);
    }
}