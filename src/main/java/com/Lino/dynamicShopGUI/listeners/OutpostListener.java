package com.Lino.dynamicShopGUI.listeners;

import com.Lino.dynamicShopGUI.DynamicShopGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.kingdoms.events.general.KingdomGUIOpenEvent;

public class OutpostListener implements Listener {

    private final DynamicShopGUI plugin;

    public OutpostListener(DynamicShopGUI plugin) { this.plugin = plugin; }

    @EventHandler
    public void onOutpostOpen(KingdomGUIOpenEvent event) {
        if (event.getPlayer().getPlayer() != null
                && event.getGUI().getName().contains("structures/outpost")) {

            Player player = event.getPlayer().getPlayer();

            event.setCancelled(true);
            event.getGUI().close();

            plugin.getGUIManager().openCategoryMenu(player, "ores", 0);
            player.sendMessage(plugin.getShopConfig().getMessage("commands.open-success",
                    "%player%", player.getName()));
        }
        else {
            DynamicShopGUI.getInstance().getLogger().info(
                    plugin.getShopConfig().getMessage("commands.open-failed",
                    "%player%", event.getPlayer().getPlayer() != null
                            ? event.getPlayer().getPlayer().getDisplayName() : "null")
            );
        }
    }
}
