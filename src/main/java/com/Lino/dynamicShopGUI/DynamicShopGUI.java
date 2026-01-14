package com.Lino.dynamicShopGUI;

import com.Lino.dynamicShopGUI.listeners.AutoSellChestListener;
import com.Lino.dynamicShopGUI.listeners.AutoHarvesterListener;
import com.Lino.dynamicShopGUI.managers.AutoSellChestManager;
import com.Lino.dynamicShopGUI.managers.DiscordManager;
import com.Lino.dynamicShopGUI.managers.ItemWorthManager;
import com.Lino.dynamicShopGUI.placeholders.ShopPlaceholders;
import org.bukkit.plugin.java.JavaPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import com.Lino.dynamicShopGUI.database.DatabaseManager;
import com.Lino.dynamicShopGUI.managers.ShopManager;
import com.Lino.dynamicShopGUI.managers.GUIManager;
import com.Lino.dynamicShopGUI.managers.RestockManager;
import com.Lino.dynamicShopGUI.commands.ShopCommand;
import com.Lino.dynamicShopGUI.commands.SellCommand;
import com.Lino.dynamicShopGUI.commands.ShopTabCompleter;
import com.Lino.dynamicShopGUI.commands.SellTabCompleter;
import com.Lino.dynamicShopGUI.listeners.ShopListener;
import com.Lino.dynamicShopGUI.config.ShopConfig;

public class DynamicShopGUI extends JavaPlugin {

    private static DynamicShopGUI instance;
    private Economy economy;
    private DatabaseManager databaseManager;
    private ShopManager shopManager;
    private GUIManager guiManager;
    private ShopConfig shopConfig;
    private RestockManager restockManager;
    private ItemWorthManager itemWorthManager;
    private AutoSellChestManager autoSellChestManager;
    private DiscordManager discordManager;

    @Override
    public void onEnable() {
        instance = this;

        if (!setupEconomy()) {
            getLogger().severe("Vault economy not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
            getLogger().warning("ProtocolLib non trovato! La funzione Item Worth non funzionerà.");
        }

        saveDefaultConfig();
        shopConfig = new ShopConfig(this);

        databaseManager = new DatabaseManager(this);
        if (!databaseManager.initialize()) {
            getLogger().severe("Failed to initialize database! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        discordManager = new DiscordManager(this);
        restockManager = new RestockManager(this);
        shopManager = new ShopManager(this);
        guiManager = new GUIManager(this);

        itemWorthManager = new ItemWorthManager(this);
//        itemWorthManager.start();

        autoSellChestManager = new AutoSellChestManager(this);

        registerCommands();
        registerListeners();

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ShopPlaceholders(this).register();
            getLogger().info("PlaceholderAPI hook registered!");
        }

        getLogger().info("DynamicShopGUI has been enabled!");
    }

    @Override
    public void onDisable() {
        if (itemWorthManager != null) {
            itemWorthManager.stop();
        }

        if (autoSellChestManager != null) {
            autoSellChestManager.shutdown();
        }

        if (restockManager != null) {
            restockManager.shutdown();
        }

        if (databaseManager != null) {
            databaseManager.close();
        }

        getLogger().info("DynamicShopGUI has been disabled!");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    private void registerCommands() {
        getCommand("shop").setExecutor(new ShopCommand(this));
        getCommand("shop").setTabCompleter(new ShopTabCompleter());

        getCommand("sell").setExecutor(new SellCommand(this));
        getCommand("sell").setTabCompleter(new SellTabCompleter());
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new ShopListener(this), this);
        getServer().getPluginManager().registerEvents(new AutoSellChestListener(this), this);
        getServer().getPluginManager().registerEvents(new AutoHarvesterListener(this), this);
    }

    public static DynamicShopGUI getInstance() {
        return instance;
    }

    public Economy getEconomy() {
        return economy;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public GUIManager getGUIManager() {
        return guiManager;
    }

    public ShopConfig getShopConfig() {
        return shopConfig;
    }

    public RestockManager getRestockManager() {
        return restockManager;
    }

    public ItemWorthManager getItemWorthManager() {
        return itemWorthManager;
    }

    public AutoSellChestManager getAutoSellChestManager() {
        return autoSellChestManager;
    }

    public DiscordManager getDiscordManager() {
        return discordManager;
    }
}