package me.nakilex.levelplugin.debug.gui;

import hm.zelha.particlesfx.particles.parents.Particle;
import me.nakilex.levelplugin.debug.particles.ParticleDebugManager;
import me.nakilex.levelplugin.debug.particles.ParticleDebugRegistry;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ParticleDebugGUI implements Listener {
    private static final String TITLE = "Particle Debugger";
    private static final int PAGE_SIZE = GuiUtil.PAGED_SLOTS.length;
    private static final int PREVIOUS_SLOT = 45;
    private static final int NEXT_SLOT = 53;

    private final ParticleDebugRegistry registry;
    private final ParticleDebugManager debugManager;
    private final Map<UUID, Integer> pages = new HashMap<>();

    public ParticleDebugGUI(ParticleDebugRegistry registry, ParticleDebugManager debugManager) {
        this.registry = registry;
        this.debugManager = debugManager;
    }

    public void open(Player player) {
        open(player, pages.getOrDefault(player.getUniqueId(), 0));
    }

    private void open(Player player, int page) {
        List<ParticleDebugRegistry.ParticleDefinition> particles = registry.getParticles();
        pages.put(player.getUniqueId(), page);

        GuiBuilder builder = GuiBuilder.create(54, TITLE + " - Page " + (page + 1))
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .border()
                .fillEmptySlots(false);

        int startIndex = page * PAGE_SIZE;
        int slotIndex = 0;
        for (int i = startIndex; i < particles.size() && slotIndex < PAGE_SIZE; i++) {
            ParticleDebugRegistry.ParticleDefinition definition = particles.get(i);
            builder.setItem(GuiUtil.PAGED_SLOTS[slotIndex++], createParticleItem(definition));
        }

        if (page > 0) {
            builder.setItem(PREVIOUS_SLOT, GuiUtil.getNexoItem("arrow_left", ChatColor.GREEN + "Previous"));
        }
        if (particles.size() > (page + 1) * PAGE_SIZE) {
            builder.setItem(NEXT_SLOT, GuiUtil.getNexoItem("arrow_right", ChatColor.GREEN + "Next"));
        }

        player.openInventory(builder.build());
    }

    private ItemStack createParticleItem(ParticleDebugRegistry.ParticleDefinition definition) {
        ItemStack item = new ItemStack(Material.FIREWORK_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + definition.displayName());
            List<String> lore = TooltipUtil.bulletList(
                    "ID: " + definition.id(),
                    "Preview spawns for 10 seconds"
            );
            lore.add("");
            lore.addAll(TooltipUtil.clickInstructions("to preview at your location", null));
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!GuiUtil.normalizeTitle(event.getView().getTitle()).startsWith(TITLE)) {
            return;
        }
        event.setCancelled(true);
        int slot = event.getRawSlot();
        int page = pages.getOrDefault(player.getUniqueId(), 0);
        List<ParticleDebugRegistry.ParticleDefinition> particles = registry.getParticles();

        if (slot == PREVIOUS_SLOT && page > 0) {
            open(player, page - 1);
            return;
        }
        if (slot == NEXT_SLOT && particles.size() > (page + 1) * PAGE_SIZE) {
            open(player, page + 1);
            return;
        }

        for (int i = 0; i < GuiUtil.PAGED_SLOTS.length; i++) {
            if (GuiUtil.PAGED_SLOTS[i] == slot) {
                int index = page * PAGE_SIZE + i;
                if (index >= particles.size()) {
                    return;
                }
                ParticleDebugRegistry.ParticleDefinition definition = particles.get(index);
                Particle particle = definition.createParticle();
                if (particle == null) {
                    ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                            "Unable to create particle " + definition.displayName() + ".");
                    return;
                }
                debugManager.startPreview(player, particle, player.getLocation().add(0, 1, 0));
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                        "Previewing " + definition.displayName() + " for 10 seconds.");
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!GuiUtil.normalizeTitle(event.getView().getTitle()).startsWith(TITLE)) {
            return;
        }
        pages.remove(event.getPlayer().getUniqueId());
    }
}
