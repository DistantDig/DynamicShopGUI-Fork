package com.Lino.dynamicShopGUI.utils;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.Arrays;

public final class FoodReader {

    public record FoodStats(int nutrition, float saturationModifier) {
    }

    private static volatile boolean init;
    private static Method asNmsCopy;
    private static Method nmsGetItem;
    private static Method itemGetFoodProperties;
    private static Method foodNutritionAccessor;
    private static Method foodSaturationAccessor;

    public static FoodStats readFoodStats(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) return new FoodStats(0, 0);

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) return new FoodStats(0, 0);

        return getFoodValueReflect(itemStack);
    }

    private static void ensureInit() throws Exception {
        if (init) return;
        synchronized (FoodReader.class) {
            if (init) return;

            // Resolve the CraftBukkit package dynamically: org.bukkit.craftbukkit.v1_21_R1
            String cbPkg = Bukkit.getServer().getClass().getPackage().getName();
            String craftItemStackName = cbPkg + ".inventory.CraftItemStack";

            Class<?> craftItemStackClz = Class.forName(craftItemStackName);
            asNmsCopy = craftItemStackClz.getMethod("asNMSCopy", ItemStack.class);

            // NMS classes
            Class<?> nmsItemStackClz = Class.forName("net.minecraft.world.item.ItemStack");
            Class<?> nmsItemClz = Class.forName("net.minecraft.world.item.Item");
            Class<?> livingEntityClz = Class.forName("net.minecraft.world.entity.LivingEntity");
            Class<?> foodPropsClz = Class.forName("net.minecraft.world.food.FoodProperties");

            // Find ItemStack.getItem() by signature
            nmsGetItem = Arrays.stream(nmsItemStackClz.getMethods())
                    .filter(m -> m.getParameterCount() == 0 && m.getReturnType().equals(nmsItemClz))
                    .findFirst()
                    .orElseThrow(() -> new NoSuchMethodException("Could not find ItemStack -> Item getter"));

            // Find Item.getFoodProperties(ItemStack, LivingEntity) by signature
            itemGetFoodProperties = Arrays.stream(nmsItemClz.getMethods())
                    .filter(m ->
                            m.getReturnType().equals(foodPropsClz)
                                    && m.getParameterCount() == 2
                                    && m.getParameterTypes()[0].equals(nmsItemStackClz)
                                    && livingEntityClz.isAssignableFrom(m.getParameterTypes()[1])
                    )
                    .findFirst()
                    .orElseThrow(() -> new NoSuchMethodException("Could not find Item#getFoodProperties(ItemStack, LivingEntity)"));

            // FoodProperties
            var components = foodPropsClz.getRecordComponents();
            Method nutrition = null;
            Method saturation = null;

            for (var comps: components) {
                if (comps.getType() == int.class && nutrition == null) nutrition = comps.getAccessor();
                if (comps.getType() == int.class && saturation == null) saturation = comps.getAccessor();
            }

            if (nutrition == null || saturation == null) {
                throw new NoSuchMethodException("Could not resolve FoodProperties record accessor");
            }

            foodNutritionAccessor = nutrition;
            foodSaturationAccessor = saturation;

            init = true;
        }
    }

    private static FoodStats getFoodValueReflect(ItemStack bukkitStack) {
        try {
            ensureInit();

            Object nmsStack = asNmsCopy.invoke(null, bukkitStack);
            Object nmsItem = nmsGetItem.invoke(nmsStack);

            // Item.getFoodProperties(ItemStack, LivingEntity) -> FoodProperties or null
            Object foodProps = itemGetFoodProperties.invoke(nmsItem, nmsStack, null);
            if (foodProps == null) return null;

            int nutrition = (int) foodNutritionAccessor.invoke(foodProps);
            float satMod = ((Number) foodSaturationAccessor.invoke(foodProps)).floatValue();

            return new FoodStats(nutrition, satMod);
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    private FoodReader() {}
}
