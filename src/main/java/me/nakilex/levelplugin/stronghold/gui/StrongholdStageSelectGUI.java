package me.nakilex.levelplugin.stronghold.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.*;

public class StrongholdStageSelectGUI implements Listener {
    private static final String TITLE = "Stronghold Stage Select";
    private static final int SIZE = 54;
    private final Map<UUID, Integer> pages = new HashMap<>();
    public StrongholdStageSelectGUI(){ Bukkit.getPluginManager().registerEvents(this, Main.getInstance()); }
    public void open(Player p){ pages.put(p.getUniqueId(),0); renderAndOpen(p); }
    private void renderAndOpen(Player p){
        int page = pages.getOrDefault(p.getUniqueId(),0);
        Inventory inv = Bukkit.createInventory(null,SIZE,TITLE);
        GuiUtil.fillBorder(inv, GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE));
        int bestStage = Math.max(1, Main.getInstance().getStrongholdRunManager().getHighestStageProgress(p.getUniqueId()).stage());
        int maxStage = Math.max(5, bestStage);
        int start = page*28 + 1;
        int slot=10;
        for(int st=start; st<=maxStage && st<start+28; st++){
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Start from checkpoint " + ChatColor.WHITE + st + "-1");
            lore.add(" "); lore.addAll(TooltipUtil.clickInstructions("to start this stage", null));
            inv.setItem(slot, GuiUtil.createGuiItem(Material.ENDER_EYE, ChatColor.LIGHT_PURPLE + "Stage " + st + "-1", lore));
            slot++; if(slot%9==8) slot+=2;
        }
        inv.setItem(45, GuiUtil.createGuiItem(Material.ARROW, ChatColor.YELLOW+"Prev Page", List.of(ChatColor.GRAY+"Page "+(page+1))));
        inv.setItem(53, GuiUtil.createGuiItem(Material.ARROW, ChatColor.YELLOW+"Next Page", List.of(ChatColor.GRAY+"Page "+(page+1))));
        p.openInventory(inv);
    }
    @EventHandler public void onClick(InventoryClickEvent e){ if(!(e.getWhoClicked() instanceof Player p)) return; if(!TITLE.equals(e.getView().getTitle())) return; e.setCancelled(true); if(e.getClickedInventory()!=e.getView().getTopInventory()) return; int raw=e.getRawSlot(); int page=pages.getOrDefault(p.getUniqueId(),0); if(raw==45){ pages.put(p.getUniqueId(), Math.max(0,page-1)); renderAndOpen(p); return;} if(raw==53){ pages.put(p.getUniqueId(), page+1); renderAndOpen(p); return;} if(raw<10||raw>43||raw%9==0||raw%9==8) return; int idx=(raw-10)-((raw-10)/9)*2; int stage=(page*28)+idx+1; p.closeInventory(); Main.getInstance().getStrongholdRunManager().startSoloRun(p, stage); }
}
