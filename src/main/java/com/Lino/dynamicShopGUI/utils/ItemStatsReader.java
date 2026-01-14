package com.Lino.dynamicShopGUI.utils;

import com.google.common.collect.Multimap;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemStatsReader {

    public record CombatStats(
            double attackDamage,
            double armor,
            String rarity
    ) {}

    enum EquipmentSlots {
        HAND(EquipmentSlot.HAND),
        OFF_HAND(EquipmentSlot.OFF_HAND),
        HEAD(EquipmentSlot.HEAD),
        BODY(EquipmentSlot.CHEST),
        LEGS(EquipmentSlot.LEGS),
        FEET(EquipmentSlot.FEET);

        private final EquipmentSlot slot;

        EquipmentSlots(EquipmentSlot slot) {
            this.slot = slot;
        }

        public EquipmentSlot getSlot() {
            return slot;
        }
    }

    public static CombatStats getCombatStats(ItemStack itemStack) {
        double attackTotal = 0.0;
        double armorTotal = 0.0;
        String rarity = null;
        ItemMeta itemMeta = itemStack.getItemMeta();

        for (EquipmentSlots slot : EquipmentSlots.values()) {
            Multimap<Attribute, AttributeModifier> attributes = itemStack.getType().getDefaultAttributeModifiers(
                    slot.getSlot());

            for (AttributeModifier attribute : attributes.values()) {
                if (attribute == null) continue;

                if (attribute.getKey().toString().contains("attack")) {
                    attackTotal += Math.abs(attribute.getAmount());
                }
                if (attribute.getKey().toString().contains("armor")
                        || attribute.getKey().toString().contains("health")
                        || attribute.getKey().toString().contains("equipment")) {
                    armorTotal += Math.abs(attribute.getAmount());
                }
            }
        }

        if (itemMeta != null) {
            rarity = ComponentParser.parseGearRarity(itemMeta.getAsComponentString());
        }

        return new CombatStats(attackTotal, armorTotal, rarity);
    }
}
