package me.nakilex.levelplugin.fishing.core.action;

import me.nakilex.levelplugin.fishing.api.FishingContext;
import me.nakilex.levelplugin.fishing.api.action.Action;
import me.nakilex.levelplugin.fishing.core.FishingArgs;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class GiveItemAction implements Action {
    @Override
    public void execute(FishingContext ctx, Map<String, Object> args) {
        Player player = ctx.getPlayer();
        if (player == null) {
            return;
        }
        String materialName = FishingArgs.getString(args, "material");
        if (materialName == null) {
            return;
        }
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            return;
        }
        int amount = Math.max(1, FishingArgs.getInt(args, "amount", 1));
        ItemStack stack = new ItemStack(material, amount);
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(stack);
        if (!leftovers.isEmpty()) {
            leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        }
    }
}
