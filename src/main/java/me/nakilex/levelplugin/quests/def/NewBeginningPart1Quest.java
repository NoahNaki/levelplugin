package me.nakilex.levelplugin.quests.def;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.quests.data.*;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * Intro quest that plays a short conversation with Piwan.
 */
public class NewBeginningPart1Quest extends Quest implements QuestScript {
    private static List<QuestObjective> createObjectives() {
        // Use a custom target so normal NPC click handling doesn't finish the quest
        return List.of(new QuestObjective(QuestObjectiveType.TALK, "npc536_done", 1));
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
                NPC npc = CitizensAPI.getNPCRegistry().getById(536);
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
                "Hey! Where did you come from? I swear you weren’t there a second ago—and those clothes... you're not from around here, are you? A foreign noble, maybe?",
                "Another world? Huh. Oddly enough, my mom once met someone who said the same—claimed they came from a place called " + ip + ".",
                "I'm Piwan. I can show you around the village."
        };

        // Send the first line immediately when close with numbering
        player.sendMessage(ChatColor.GRAY + "[1/" + lines.length + "] "
                + ChatColor.YELLOW + npc.getName() + ChatColor.WHITE + ": " + lines[0]);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);

        final Listener[] listener = new Listener[1];
        listener[0] = new Listener() {
            int idx = 1;

            @EventHandler(priority = EventPriority.LOWEST)
            public void onInteract(PlayerInteractEntityEvent event) {
                if (!event.getPlayer().equals(player)) return;
                if (event.getHand() == EquipmentSlot.OFF_HAND) return;
                if (!CitizensAPI.getNPCRegistry().isNPC(event.getRightClicked())) return;
                NPC clicked = CitizensAPI.getNPCRegistry().getNPC(event.getRightClicked());
                if (clicked.getId() != 536) return;
                event.setCancelled(true);

                if (idx >= lines.length) return;
                player.sendMessage(ChatColor.GRAY + "[" + (idx + 1) + "/" + lines.length + "] "
                        + ChatColor.YELLOW + npc.getName() + ChatColor.WHITE + ": " + lines[idx]);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                idx++;

                if (idx >= lines.length) {
                    org.bukkit.event.HandlerList.unregisterAll(listener[0]);
                    // Delay quest completion slightly so the final line can be read
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (!player.isOnline()) return;
                        Main.getInstance().getQuestManager().handleTalk(player, "npc536_done");
                        moveNpc(player, npc, plugin);
                    }, 40L); // 2 seconds
                }
            }
        };
        org.bukkit.Bukkit.getPluginManager().registerEvents(listener[0], plugin);
    }

    /** Move Piwan to a new location for this player only. */
    private void moveNpc(Player player, NPC npc, Main plugin) {
        org.bukkit.Location loc = npc.getEntity().getLocation().clone().add(10, 0, 0);

        // Spawn the moved NPC with id 537 at the new location
        NPC moved = CitizensAPI.getNPCRegistry().getById(537);
        if (moved != null) {
            moved.spawn(loc);
            if (moved.isSpawned()) {
                moved.getEntity().teleport(loc, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
                moved.getEntity().setGravity(false);
            }
            // Hide from everyone until the player walks away from NPC 536
            for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                p.hideEntity(plugin, moved.getEntity());
            }
        }

        // Show the moved NPC only after the player leaves the old one
        new BukkitRunnable() {
            boolean shown = false;
            @Override
            public void run() {
                if (!player.isOnline()) { cancel(); return; }
                if (player.getLocation().distanceSquared(npc.getEntity().getLocation()) > 100) {
                    player.hideEntity(plugin, npc.getEntity());
                    if (moved != null && moved.isSpawned() && !shown) {
                        player.showEntity(plugin, moved.getEntity());
                        shown = true;
                    }
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }
}
