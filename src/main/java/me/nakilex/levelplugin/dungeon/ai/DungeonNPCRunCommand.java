package me.nakilex.levelplugin.dungeon.ai;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.dungeon.Dungeon;
import me.nakilex.levelplugin.dungeon.DungeonManager;
import me.nakilex.levelplugin.dungeon.DungeonMobSpawnListener;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class DungeonNPCRunCommand implements CommandExecutor {
    private final Main plugin;
    private final DungeonManager dungeonManager;

    public DungeonNPCRunCommand(Main plugin) {
        this.plugin = plugin;
        this.dungeonManager = plugin.getDungeonManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (args.length < 1) return false;
        boolean hire = false;
        if (args[args.length - 1].equalsIgnoreCase("hire")) {
            hire = true;
            args = java.util.Arrays.copyOf(args, args.length - 1);
        }
        String name = String.join(" ", args);
        Dungeon dungeon = dungeonManager.spawnDungeon(player.getLocation(), name);
        if (dungeon == null) {
            player.sendMessage(ChatColor.RED + "Layout not found.");
            return true;
        }
        NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, hire ? ChatColor.GREEN + "Mercenary" : ChatColor.AQUA + "Dungeon NPC");
        npc.spawn(player.getLocation());
        npc.setProtected(false);
        addSentinelTrait(npc);
        DungeonMobSpawnListener listener = new DungeonMobSpawnListener(dungeonManager, plugin);
        DungeonNPCRunner runner = new DungeonNPCRunner(npc, dungeon, dungeonManager, listener, hire ? player : null);
        runner.start(plugin);
        player.sendMessage(ChatColor.YELLOW + "NPC started running the dungeon.");
        return true;
    }

    private void addSentinelTrait(NPC npc) {
        try {
            Class<?> trait = Class.forName("org.mcmonkey.sentinel.SentinelTrait");
            java.lang.reflect.Method getOrAdd = NPC.class.getMethod("getOrAddTrait", Class.class);
            Object sentinel = getOrAdd.invoke(npc, trait);
            java.lang.reflect.Field speed = trait.getField("speed");
            speed.setDouble(sentinel, 1.5);
            java.lang.reflect.Field fight = trait.getField("fightback");
            fight.setBoolean(sentinel, true);
        } catch (ClassNotFoundException ignored) {
            // Sentinel not installed
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
