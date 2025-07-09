package me.nakilex.levelplugin.quests.def;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.quests.data.*;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * Intro quest that plays a short conversation with Piwan.
 */
public class NewBeginningPart1Quest extends Quest implements QuestScript {
    private static List<QuestObjective> createObjectives() {
        // Use a custom target so normal NPC click handling doesn't finish the quest
        return List.of(new QuestObjective(QuestObjectiveType.TALK, "npc600_done", 1));
    }

    public NewBeginningPart1Quest() {
        super(
                "newbeginning1",
                "A New Beginning I",
                "Meet Piwan after arriving in the new world.",
                createObjectives(),
                1,
                List.of("officeerrands"),
                null,
                QuestRewardCompat.create(100, 0, 0, List.of(),
                        List.of(PlayerClass.ARCHER, PlayerClass.WARRIOR,
                                PlayerClass.MAGE, PlayerClass.ROGUE)),
                null,
                null
        );
    }

    @Override
    public void onStart(Player player, Main plugin) {
        new BukkitRunnable() {
            boolean triggered = false;
            @Override
            public void run() {
                NPC npc = CitizensAPI.getNPCRegistry().getById(600);
                if (npc == null || !npc.isSpawned()) return;
                if (!player.isOnline()) { cancel(); return; }
                if (!npc.getEntity().getWorld().equals(player.getWorld())) return;
                if (!triggered && player.getLocation().distanceSquared(npc.getEntity().getLocation()) <= 100) {
                    playDialog(player, plugin, npc);
                    triggered = true;
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
   }

    private void playDialog(Player player, Main plugin, NPC npc) {
        InetSocketAddress addr = player.getAddress();
        String ip = addr != null && addr.getAddress() != null ? addr.getAddress().getHostAddress() : "unknown";
        String[] lines = new String[] {
                "Hey you there!",
                "Where did you suddenly appear from, I could've sworn there wasn't anyone there a second ago, and your clothes, your certainly dont look from around here, are you perhaps a noble from another country?",
                "What? You came from another world, I guess that somewhat makes sense, my mom used to tell me that she once met an adventurer who claimed the same thing, said they're from a place called " + ip + ".",
                "My name is Piwan, since you're not from around here, I could show you around my village."
        };

        // Send the first line immediately when close
        player.sendMessage(ChatColor.YELLOW + npc.getName() + ChatColor.WHITE + ": " + lines[0]);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);

        final Listener[] listener = new Listener[1];
        listener[0] = new Listener() {
            int idx = 1;

            @org.bukkit.event.EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
            public void onInteract(org.bukkit.event.player.PlayerInteractEntityEvent event) {
                if (!event.getPlayer().equals(player)) return;
                if (event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND) return;
                if (!CitizensAPI.getNPCRegistry().isNPC(event.getRightClicked())) return;
                NPC clicked = CitizensAPI.getNPCRegistry().getNPC(event.getRightClicked());
                if (clicked.getId() != 600) return;
                event.setCancelled(true);

                if (idx >= lines.length) return;
                player.sendMessage(ChatColor.YELLOW + npc.getName() + ChatColor.WHITE + ": " + lines[idx]);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                idx++;

                if (idx >= lines.length) {
                    org.bukkit.event.HandlerList.unregisterAll(listener[0]);
                    Main.getInstance().getQuestManager().handleTalk(player, "npc600_done");
                    moveNpc(player, npc, plugin);
                }
            }
        };
        org.bukkit.Bukkit.getPluginManager().registerEvents(listener[0], plugin);
    }

    /** Move Piwan to a new location for this player only. */
    private void moveNpc(Player player, NPC npc, Main plugin) {
        org.bukkit.Location loc = npc.getEntity().getLocation().clone().add(10, 0, 0);
        NPC clone = npc.copy();
        clone.getOrAddTrait(net.citizensnpcs.trait.CurrentLocation.class).setLocation(loc);
        clone.spawn(loc);
        if (clone.isSpawned()) {
            clone.getEntity().teleport(loc, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
            clone.getEntity().setGravity(false);
        }
        for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (!p.equals(player)) {
                p.hideEntity(plugin, clone.getEntity());
            }
        }
        player.hideEntity(plugin, npc.getEntity());
    }
}
