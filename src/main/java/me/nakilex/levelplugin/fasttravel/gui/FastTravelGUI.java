package me.nakilex.levelplugin.fasttravel.gui;

import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.fasttravel.FastTravelManager;
import me.nakilex.levelplugin.fakeblock.ModelGate;
import me.nakilex.levelplugin.fakeblock.ModelGateManager;
import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import me.nakilex.levelplugin.utils.TeleportUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class FastTravelGUI implements Listener {
    private static final String TITLE = ChatColor.BLACK + "Fast Travel";
    private static final int SIZE = 54;
    private static final int[] POINT_SLOTS = {
            10,11,12,13,14,15,16,
            19,20,21,22,23,24,25,
            28,29,30,31,32,33,34,
            37,38,39,40,41,42,43
    };
    private static final int ITEMS_PER_PAGE = POINT_SLOTS.length;
    private static final int PREV_PAGE = 45;
    private static final int NEXT_PAGE = 53;
    private static final int SORT_SLOT = 50;
    private static final int FILTER_SLOT = 49;

    private final FastTravelManager manager;
    private final EconomyManager economy;
    private final ModelGateManager gateManager;
    private final Map<UUID,Integer> pageMap = new HashMap<>();
    private final Map<UUID,Integer> sortMap = new HashMap<>();
    private final Map<UUID,Integer> typeMap = new HashMap<>();
    /** Gate id to exclude when showing options for each player. */
    private final Map<UUID,String> excludeMap = new HashMap<>();

    public FastTravelGUI(FastTravelManager manager, EconomyManager economy, ModelGateManager gateManager) {
        this.manager = manager;
        this.economy = economy;
        this.gateManager = gateManager;
        Bukkit.getPluginManager().registerEvents(this, manager.getPlugin());
    }

    /**
     * Open the fast travel menu for the player without excluding any gate.
     */
    public void open(Player player) {
        excludeMap.remove(player.getUniqueId());
        open(player, pageMap.getOrDefault(player.getUniqueId(),0));
    }

    /**
     * Open the fast travel menu while excluding the given gate from the list.
     */
    public void open(Player player, ModelGate currentGate) {
        if(currentGate != null){
            excludeMap.put(player.getUniqueId(), currentGate.getId().toLowerCase());
        } else {
            excludeMap.remove(player.getUniqueId());
        }
        open(player, pageMap.getOrDefault(player.getUniqueId(),0));
    }

    private void open(Player player, int page) {
        pageMap.put(player.getUniqueId(), page);
        Inventory gui = Bukkit.createInventory(null, SIZE, TITLE);
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        if(fm!=null){ fm.setDisplayName(" "); filler.setItemMeta(fm); }
        for(int i=0;i<SIZE;i++){
            if(i<9 || i>=45 || i%9==0 || i%9==8){ gui.setItem(i,filler); }
        }
        int filter = typeMap.getOrDefault(player.getUniqueId(),0);
        List<ModelGate> list = new ArrayList<>(gateManager.getGates());
        String exclude = excludeMap.get(player.getUniqueId());
        if(exclude != null){
            list.removeIf(g -> g.getId().equalsIgnoreCase(exclude));
        }
        if(filter==1) list.removeIf(g->!g.isTown());
        else if(filter==2) list.removeIf(ModelGate::isTown);
        int mode = sortMap.getOrDefault(player.getUniqueId(),0);
        list.sort(getComparator(mode,player));
        int start = page*ITEMS_PER_PAGE;
        int slot=0;
        for(int i=start;i<list.size() && slot<ITEMS_PER_PAGE;i++){
            ModelGate pt=list.get(i);
            boolean unlocked = manager.isUnlocked(player, pt.getId());
            ItemStack item = new ItemStack(unlocked?Material.LODESTONE:Material.BARRIER);
            ItemMeta meta=item.getItemMeta();
            if(meta!=null){
                ChatColor col = pt.isTown() ? ChatColor.AQUA : ChatColor.RED;
                meta.setDisplayName(col+""+ChatColor.BOLD+formatName(pt.getId()));
                List<String> lore=new ArrayList<>();
                if(unlocked){
                    int cost=(int)player.getLocation().distance(pt.getLocation());
                    lore.add(ChatColor.GRAY+"Teleportation Cost:");
                    lore.add(ChatColor.WHITE+""+cost+ChatColor.YELLOW+" <glyph:coins_icon>");
                } else {
                    lore.add(ChatColor.DARK_GRAY+"Locked");
                }
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            gui.setItem(POINT_SLOTS[slot++], item);
        }
        if(page>0) gui.setItem(PREV_PAGE, createArrow(ChatColor.GREEN+"Previous"));
        if(list.size()> (page+1)*ITEMS_PER_PAGE) gui.setItem(NEXT_PAGE, createArrow(ChatColor.GREEN+"Next"));
        gui.setItem(SORT_SLOT, createSortButton(mode));
        gui.setItem(FILTER_SLOT, createFilterButton(filter));
        player.openInventory(gui);
    }

    private Comparator<ModelGate> getComparator(int mode, Player player){
        return switch (mode){
            case 0 -> Comparator.comparing((ModelGate g)-> g.getLocation().distanceSquared(player.getLocation())).reversed();
            case 1 -> Comparator.comparing(g-> g.getLocation().distanceSquared(player.getLocation()));
            case 2 -> Comparator.comparing(ModelGate::getId,String.CASE_INSENSITIVE_ORDER);
            default -> {
                String last = manager.getLastUsed(player);
                yield (a,b)->{
                    if(last==null) return a.getId().compareToIgnoreCase(b.getId());
                    if(a.getId().equalsIgnoreCase(last)) return -1;
                    if(b.getId().equalsIgnoreCase(last)) return 1;
                    return a.getId().compareToIgnoreCase(b.getId());
                };}
        };
    }

    @EventHandler
    public void onClick(InventoryClickEvent event){
        if(!event.getView().getTitle().equals(TITLE)) return;
        event.setCancelled(true);
        if(!(event.getWhoClicked() instanceof Player player)) return;
        Inventory inv=event.getInventory();
        int slot=event.getRawSlot();
        if(slot<0 || slot>=inv.getSize()) return;
        if(slot==PREV_PAGE){
            int p=pageMap.getOrDefault(player.getUniqueId(),0); open(player, Math.max(0,p-1)); return; }
        if(slot==NEXT_PAGE){
            int p=pageMap.getOrDefault(player.getUniqueId(),0); open(player,p+1); return; }
        if(slot==SORT_SLOT){
            int m=sortMap.getOrDefault(player.getUniqueId(),0); m=(m+1)%4; sortMap.put(player.getUniqueId(),m); open(player,pageMap.getOrDefault(player.getUniqueId(),0)); return; }
        if(slot==FILTER_SLOT){
            int f=typeMap.getOrDefault(player.getUniqueId(),0); f=(f+1)%3; typeMap.put(player.getUniqueId(),f); open(player,pageMap.getOrDefault(player.getUniqueId(),0)); return; }
        // find point
        int filter=typeMap.getOrDefault(player.getUniqueId(),0);
        List<ModelGate> list=new ArrayList<>(gateManager.getGates());
        if(filter==1) list.removeIf(g->!g.isTown());
        else if(filter==2) list.removeIf(ModelGate::isTown);
        list.sort(getComparator(sortMap.getOrDefault(player.getUniqueId(),0),player));
        int index= Arrays.binarySearch(POINT_SLOTS, slot);
        if(index<0) return;
        int start=pageMap.getOrDefault(player.getUniqueId(),0)*ITEMS_PER_PAGE;
        int actual=start+index;
        if(actual>=list.size()) return;
        ModelGate target=list.get(actual);
        if(!manager.isUnlocked(player,target.getId())) return;
        int cost=(int)player.getLocation().distance(target.getLocation());
        if(economy.getBalance(player)<cost){
            player.sendMessage(ChatColor.RED+"You need "+cost+" coins to travel.");
            return;
        }
        player.closeInventory();
        startCast(player,target,cost);
    }

    private void startCast(Player player, ModelGate target, int cost){
        economy.deductCoins(player,cost);
        manager.recordUse(player,target.getId());
        var startLoc=player.getLocation().clone();
        new BukkitRunnable(){
            int t=60;
            @Override public void run(){
                if(!player.isOnline()){ cancel(); return; }
                if(player.getLocation().distanceSquared(startLoc)>0.1){ player.sendMessage(ChatColor.RED+"Teleport cancelled."); cancel(); return; }
                double radius=3.0*(t/60.0);
                for(int i=0;i<20;i++){
                    double angle=2*Math.PI*i/20.0;
                    double x=radius*Math.cos(angle); double z=radius*Math.sin(angle);
                    player.getWorld().spawnParticle(org.bukkit.Particle.DRAGON_BREATH,startLoc.clone().add(x,1,z),0,0,0,0,0);
                }
                if(--t<=0){
                    // Teleport slightly away from the gate so the player doesn't
                    // spawn inside the beacon model. Offset two blocks in the
                    // direction from the gate to the player's starting point.
                    org.bukkit.Location dest = target.getLocation().clone();
                    org.bukkit.util.Vector offset = startLoc.toVector().subtract(dest.toVector());
                    offset.setY(0);
                    if(offset.lengthSquared() > 0.0001){
                        offset.normalize().multiply(2);
                        dest.add(offset);
                    }
                    TeleportUtils.teleportWithEffect(player, dest);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,40,0,false,false));
                    player.getWorld().spawnParticle(org.bukkit.Particle.FLASH,player.getLocation(),20,0.5,0.5,0.5,0);
                    Main.getInstance().getQuestManager().handleWaystoneUse(player, target.getId());
                    cancel();
                }
            }
        }.runTaskTimer(manager.getPlugin(),0L,1L);
    }

    private ItemStack createArrow(String name){
        ItemStack it=new ItemStack(Material.ARROW);
        ItemMeta meta=it.getItemMeta();
        if(meta!=null){ meta.setDisplayName(name); it.setItemMeta(meta); }
        return it;
    }

    private ItemStack createSortButton(int mode){
        ItemStack it=new ItemStack(Material.COMPARATOR);
        ItemMeta meta=it.getItemMeta();
        if(meta!=null){
            meta.setDisplayName(ChatColor.AQUA+"Sort By");
            List<String> lore=new ArrayList<>();
            String[] opts={"Distance Far","Distance Close","A-Z","Last Used"};
            for(int i=0;i<opts.length;i++){
                lore.add(rangeLine(i,mode,opts[i]));
            }
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack createFilterButton(int mode){
        ItemStack it=new ItemStack(Material.BOOK);
        ItemMeta meta=it.getItemMeta();
        if(meta!=null){
            meta.setDisplayName(ChatColor.AQUA+"Type Filter");
            List<String> lore=new ArrayList<>();
            String[] opts={"Show All","Towns","Dungeons"};
            for(int i=0;i<opts.length;i++){
                lore.add(rangeLine(i,mode,opts[i]));
            }
            meta.setLore(lore); it.setItemMeta(meta);
        }
        return it;
    }

    private String rangeLine(int index,int current,String label){
        ChatColor color=index==current?ChatColor.WHITE:ChatColor.GRAY;
        ChatColor bullet=index==current?ChatColor.GREEN:ChatColor.DARK_GRAY;
        return bullet+"- "+color+label;
    }

    /**
     * Format a gate id into a nicer display name.
     */
    private String formatName(String id){
        if(id==null) return "";
        String[] parts=id.split("[_-]");
        StringBuilder sb=new StringBuilder();
        for(String p:parts){
            if(p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
}
