package com.anima.villagers;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Показ предметов, которые держит житель.
 * Через action bar + ItemDisplay (когда появится сущность).
 */
public class VillagerEquipment {

    /** Показывает игрокам, что житель держит. */
    public static void showHeldItem(ServerWorld world, VillagerCharacter v, VillagerActivity act) {
        if (act == null || act.holdingItem == null || act.holdingItem.equals("minecraft:air")) return;

        String itemName = act.holdingItem.replace("minecraft:", "")
            .replace("_", " ");
        Text msg = Text.literal("§7[§e" + v.name + "§7] §fдержит: §b" + itemName +
            " §7(" + act.currentActivity + ")");

        world.getPlayers(p ->
            p.squaredDistanceTo(v.x, v.y, v.z) <= 40 * 40
        ).forEach(p -> p.sendMessage(msg, true));
    }

    /** Получить Item из строки. */
    public static Item parseItem(String id) {
        try {
            return Registries.ITEM.get(new Identifier(id));
        } catch (Exception e) {
            return null;
        }
    }

    /** ItemStack для будущей сущности. */
    public static ItemStack getItemStack(VillagerActivity act) {
        Item item = parseItem(act.holdingItem);
        return item != null ? new ItemStack(item) : ItemStack.EMPTY;
    }
}
