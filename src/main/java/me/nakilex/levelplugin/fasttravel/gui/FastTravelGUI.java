package me.nakilex.levelplugin.fasttravel.gui;

import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.fasttravel.FastTravelManager;
import me.nakilex.levelplugin.fasttravel.data.FastTravelPoint;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class FastTravelGUI implements Listener {
    private static final String TITLE = ChatColor.DARK_AQUA + "Fast Travel";
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

    private final FastTravelManager manager;
    private final EconomyManager economy;
    private final Map<UUID,Integer> pageMap = new HashMap<>();
    private final Map<UUID,Integer> sortMap = new HashMap<>();
    private final Map<UUID, FastTravelPoint> sourceMap = new HashMap<>();

    public FastTravelGUI(FastTravelManager manager, EconomyManager economy) {
        this.manager = manager;
        this.economy = economy;
        Bukkit.getPluginManager().registerEvents(this, manager.getPlugin());
    }

    public void open(Player player) {
        FastTravelPoint src = manager.getNearbyUnlockedPoint(player);
        if(src == null){
            player.sendMessage(ChatColor.RED + "You must be at an unlocked waystone to use fast travel.");
            return;
        }
        sourceMap.put(player.getUniqueId(), src);
        open(player, pageMap.getOrDefault(player.getUniqueId(),0));
    }

    private void open(Player player, int page) {
        pageMap.put(player.getUniqueId(), page);
        FastTravelPoint source = sourceMap.get(player.getUniqueId());
        if(source == null){
            // safety check, fallback to player location as source
            source = manager.getNearbyUnlockedPoint(player);
            if(source == null){
                player.sendMessage(ChatColor.RED + "You must be at an unlocked waystone to use fast travel.");
                return;
            }
            sourceMap.put(player.getUniqueId(), source);
        }
        Inventory gui = Bukkit.createInventory(null, SIZE, TITLE);
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        if(fm!=null){ fm.setDisplayName(" "); filler.setItemMeta(fm); }
        for(int i=0;i<SIZE;i++){
            if(i<9 || i>=45 || i%9==0 || i%9==8){ gui.setItem(i,filler); }
        }
        FastTravelPoint source = sourceMap.get(player.getUniqueId());
        List<FastTravelPoint> list = new ArrayList<>();
        for(FastTravelPoint pt: manager.getPoints()) if(pt.isTown()) list.add(pt);
        int mode = sortMap.getOrDefault(player.getUniqueId(),0);
        list.sort(getComparator(mode, player, source));
        int start = page*ITEMS_PER_PAGE;
        int slot=0;
        for(int i=start;i<list.size() && slot<ITEMS_PER_PAGE;i++){
            FastTravelPoint pt=list.get(i);
            boolean unlocked = manager.isUnlocked(player, pt.getName());
            ItemStack item = new ItemStack(unlocked?Material.LODESTONE:Material.BARRIER);
            ItemMeta meta=item.getItemMeta();
            if(meta!=null){
                meta.setDisplayName(pt.getColor()+""+ChatColor.BOLD+pt.getName());
                List<String> lore=new ArrayList<>();
                if(unlocked){
                    int cost=(int)source.getLocation().distance(pt.getLocation());
                    lore.add(ChatColor.GRAY+pt.getDescription());
                    lore.add(" ");
                    lore.add(ChatColor.GRAY+"Teleportation Cost:");
                    lore.add(ChatColor.WHITE+""+cost+ChatColor.YELLOW+" ⛃");
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
        player.openInventory(gui);
    }

    private Comparator<FastTravelPoint> getComparator(int mode, Player player, FastTravelPoint source){
        return switch (mode){
            case 0 -> Comparator.comparing((FastTravelPoint p)-> p.getLocation().distanceSquared(source.getLocation())).reversed();
            case 1 -> Comparator.comparing(p-> p.getLocation().distanceSquared(source.getLocation()));
            case 2 -> Comparator.comparing(FastTravelPoint::getName,String.CASE_INSENSITIVE_ORDER);
            default -> {
                String last = manager.getLastUsed(player);
                yield (a,b)->{
                    if(last==null) return a.getName().compareToIgnoreCase(b.getName());
                    if(a.getName().equalsIgnoreCase(last)) return -1;
                    if(b.getName().equalsIgnoreCase(last)) return 1;
                    return a.getName().compareToIgnoreCase(b.getName());
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
        // find point
        FastTravelPoint source = sourceMap.get(player.getUniqueId());
        if(source == null){
            player.closeInventory();
            player.sendMessage(ChatColor.RED + "You are no longer at a waystone.");
            return;
        }
        List<FastTravelPoint> list=new ArrayList<>();
        for(FastTravelPoint pt: manager.getPoints()) if(pt.isTown()) list.add(pt);
        list.sort(getComparator(sortMap.getOrDefault(player.getUniqueId(),0),player, source));
        int index= Arrays.binarySearch(POINT_SLOTS, slot);
        if(index<0) return;
        int start=pageMap.getOrDefault(player.getUniqueId(),0)*ITEMS_PER_PAGE;
        int actual=start+index;
        if(actual>=list.size()) return;
        FastTravelPoint target=list.get(actual);
        if(!manager.isUnlocked(player,target.getName())) return;
        int cost=(int)source.getLocation().distance(target.getLocation());
        if(economy.getBalance(player)<cost){
            player.sendMessage(ChatColor.RED+"You need "+cost+" coins to travel.");
            return;
        }
        player.closeInventory();
        startCast(player,target,source,cost);
    }

    private void startCast(Player player, FastTravelPoint target, FastTravelPoint source, int cost){
        economy.deductCoins(player,cost);
        manager.recordUse(player,target.getName());
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
                    player.teleport(target.getLocation());
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,40,0,false,false));
                    player.getWorld().spawnParticle(org.bukkit.Particle.FLASH,player.getLocation(),20,0.5,0.5,0.5,0);
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
            meta.setDisplayName(ChatColor.AQUA+"Sort");
            List<String> lore=new ArrayList<>();
            String[] opts={"Distance Far","Distance Close","A-Z","Last Used"};
            for(int i=0;i<opts.length;i++){
                String pre=i==mode?ChatColor.GREEN+"➤ ":ChatColor.GRAY+"  ";
                lore.add(pre+opts[i]);
            }
            meta.setLore(lore); it.setItemMeta(meta);
        }
        return it;
    }
}
