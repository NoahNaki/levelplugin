package me.nakilex.levelplugin.guild.quests;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.guild.Guild;
import me.nakilex.levelplugin.guild.GuildManager;
import me.nakilex.levelplugin.utils.GuiUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** GUI displaying weekly guild quests. */
public class GuildQuestGUI implements Listener {
    private final GuildManager guildManager;
    private final GuildQuestManager questManager;

    private static final int SIZE = 27;
    private static final String TITLE = ChatColor.BLACK + "Guild Quests";
    private static final int[] QUEST_SLOTS = {10, 13, 16};

    public GuildQuestGUI(GuildManager guildManager, GuildQuestManager questManager) {
        this.guildManager = guildManager;
        this.questManager = questManager;
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    public void open(Player player) {
        Guild guild = guildManager.getGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(ChatColor.RED + "You are not in a guild.");
            return;
        }
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        ItemStack filler = GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE);
        GuiUtil.fillBorder(inv, filler);

        List<GuildQuest> quests = questManager.getQuests(guild);
        for (int i = 0; i < quests.size() && i < QUEST_SLOTS.length; i++) {
            inv.setItem(QUEST_SLOTS[i], buildItem(quests.get(i), questManager.getRerollsRemaining(guild) > 0));
        }
        player.openInventory(inv);
    }

    private ItemStack buildItem(GuildQuest quest, boolean canReroll) {
        ItemStack item = GuiUtil.getNexoItem(quest.getType().getIconId(), ChatColor.GOLD + quest.getType().getDisplayName());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Progress: " + quest.getProgress() + "/" + quest.getAmount());
            lore.add(ChatColor.GRAY + "Difficulty: " + GuiUtil.glyphStars(quest.getDifficulty()));
            lore.add(" ");
            GuildQuestReward r = quest.getReward();
            lore.add(ChatColor.GREEN + "Guild Rewards:");
            lore.add(ChatColor.YELLOW + "- " + r.getGuildExp() + " exp");
            lore.add(ChatColor.YELLOW + "- " + r.getGuildCoins() + " coins");
            if (r.getPersonalReward() != null) {
                lore.add(ChatColor.GREEN + "Personal Rewards:");
                if (r.getPersonalReward().getXp() > 0) {
                    lore.add(ChatColor.GRAY + "- " + r.getPersonalReward().getXp() + " xp");
                }
                if (r.getPersonalReward().getCoins() > 0) {
                    lore.add(ChatColor.GRAY + "- " + r.getPersonalReward().getCoins() + " coins");
                }
            }
            if (canReroll) {
                lore.add(" ");
                lore.add(ChatColor.WHITE + "Right-click " + ChatColor.GRAY + "to reroll");
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!ChatColor.stripColor(e.getView().getTitle()).equals(ChatColor.stripColor(TITLE))) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player player)) return;
        Guild guild = guildManager.getGuild(player.getUniqueId());
        if (guild == null) return;
        int slot = e.getRawSlot();
        List<GuildQuest> quests = questManager.getQuests(guild);
        for (int i = 0; i < QUEST_SLOTS.length && i < quests.size(); i++) {
            if (slot == QUEST_SLOTS[i] && e.isRightClick()) {
                questManager.reroll(guild, i);
                open(player);
                return;
            }
        }
    }
}
