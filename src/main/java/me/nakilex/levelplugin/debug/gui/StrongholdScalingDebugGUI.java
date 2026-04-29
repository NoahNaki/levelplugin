package me.nakilex.levelplugin.debug.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.stronghold.run.StrongholdRunManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class StrongholdScalingDebugGUI implements Listener {
    private static final String TITLE = "Stronghold Scaling";
    private static StrongholdScalingDebugGUI instance;
    private boolean registered;
    public static StrongholdScalingDebugGUI getInstance(){ if(instance==null) instance=new StrongholdScalingDebugGUI(); return instance; }
    public void open(Player player){ if(player==null)return; ensureRegistered(); Inventory inv=Bukkit.createInventory(null,27,TITLE); render(inv); player.openInventory(inv);}    
    private void ensureRegistered(){ if(registered)return; Main plugin=Main.getInstance(); if(plugin!=null){ Bukkit.getPluginManager().registerEvents(this,plugin); registered=true; } }
    private void render(Inventory inv){
        GuiUtil.fillBorder(inv, GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE));
        StrongholdRunManager run = Main.getInstance().getStrongholdRunManager();
        if (run == null) return;
        StrongholdRunManager.StageScalingConfig c = run.getStageScalingConfig();
        inv.setItem(10, item(Material.HEART_OF_THE_SEA, "Stage HP Growth", c.stageHealthGrowth()));
        inv.setItem(12, item(Material.IRON_SWORD, "Stage DMG Growth", c.stageDamageGrowth()));
        inv.setItem(14, item(Material.APPLE, "Wave HP Growth", c.waveHealthGrowth()));
        inv.setItem(16, item(Material.STONE_SWORD, "Wave DMG Growth", c.waveDamageGrowth()));
    }
    private ItemStack item(Material m, String name, double value){ List<String> lore=new ArrayList<>(); lore.add(ChatColor.GRAY+"Current: "+ChatColor.WHITE+String.format("%.3f",value)); lore.add(" "); lore.addAll(TooltipUtil.clickInstructions("to increase (right click decrease)",null)); return GuiUtil.createGuiItem(m, ChatColor.GOLD+name, lore);}    
    @EventHandler public void onClick(InventoryClickEvent e){ if(!(e.getWhoClicked() instanceof Player p)) return; if(!TITLE.equals(e.getView().getTitle())) return; e.setCancelled(true); if(e.getClickedInventory()!=e.getView().getTopInventory()) return; StrongholdRunManager run=Main.getInstance().getStrongholdRunManager(); if(run==null) return; var c=run.getStageScalingConfig(); double step=(e.getClick()==ClickType.SHIFT_LEFT||e.getClick()==ClickType.SHIFT_RIGHT)?0.02:0.005; double sign=(e.getClick()==ClickType.RIGHT||e.getClick()==ClickType.SHIFT_RIGHT)?-1:1; switch(e.getRawSlot()){ case 10-> run.updateStageScalingConfig(new StrongholdRunManager.StageScalingConfig(Math.max(0,c.stageHealthGrowth()+sign*step), c.stageDamageGrowth(), c.waveHealthGrowth(), c.waveDamageGrowth(), c.waveSpeedGrowth())); case 12-> run.updateStageScalingConfig(new StrongholdRunManager.StageScalingConfig(c.stageHealthGrowth(), Math.max(0,c.stageDamageGrowth()+sign*step), c.waveHealthGrowth(), c.waveDamageGrowth(), c.waveSpeedGrowth())); case 14-> run.updateStageScalingConfig(new StrongholdRunManager.StageScalingConfig(c.stageHealthGrowth(), c.stageDamageGrowth(), Math.max(0,c.waveHealthGrowth()+sign*step), c.waveDamageGrowth(), c.waveSpeedGrowth())); case 16-> run.updateStageScalingConfig(new StrongholdRunManager.StageScalingConfig(c.stageHealthGrowth(), c.stageDamageGrowth(), c.waveHealthGrowth(), Math.max(0,c.waveDamageGrowth()+sign*step), c.waveSpeedGrowth())); default-> {return;} }
        ChatMessageUtil.send(p, ChatMessageUtil.MessageType.SUCCESS, "Updated stronghold scaling."); render(e.getView().getTopInventory()); }
}
