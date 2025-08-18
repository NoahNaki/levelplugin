package me.nakilex.levelplugin.pathfinding.npc;

import me.nakilex.levelplugin.spells.managers.CooldownManager;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Equipment;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

/**
 * Default pathfinding NPC that mimics an assassin mercenary.
 */
public class AssassinMercenary implements PathNpc {
    private static final String SKILL_DASH = "awakassassin_ravagingdash";
    private static final String SKILL_LETHAL = "awakassassin_lethalcombo";
    private static final String SKILL_SHADOW = "awakassassin_shadowstep";
    private static final String SKILL_FLURRY = "awakassassin_bladeflurry";

    @Override
    public float speedMultiplier() {
        return 4.5f;
    }

    @Override
    public void equip(NPC npc) {
        Equipment equip = npc.getOrAddTrait(Equipment.class);
        equip.set(Equipment.EquipmentSlot.HAND, new ItemStack(Material.NETHERITE_SWORD));
        equip.set(Equipment.EquipmentSlot.HELMET, new ItemStack(Material.NETHERITE_HELMET));
        equip.set(Equipment.EquipmentSlot.CHESTPLATE, new ItemStack(Material.NETHERITE_CHESTPLATE));
        equip.set(Equipment.EquipmentSlot.LEGGINGS, new ItemStack(Material.NETHERITE_LEGGINGS));
        equip.set(Equipment.EquipmentSlot.BOOTS, new ItemStack(Material.NETHERITE_BOOTS));
    }

    @Override
    public void handleCombat(NPC npc, LivingEntity target, CooldownManager cd) {
        Location npcLoc = npc.getEntity().getLocation();
        Location targetLoc = target.getLocation();
        double distSq = npcLoc.distanceSquared(targetLoc);
        if (distSq > 9) {
            if (!cast(npc, SKILL_DASH, 5, target, cd)) {
                npc.getNavigator().setTarget(target, true);
            }
        } else {
            cast(npc, SKILL_LETHAL, 1, target, cd);
            cast(npc, SKILL_SHADOW, 8, target, cd);
            cast(npc, SKILL_FLURRY, 10, target, cd);
        }
    }

    @Override
    public String name() {
        return "assassinmercenary";
    }
}

