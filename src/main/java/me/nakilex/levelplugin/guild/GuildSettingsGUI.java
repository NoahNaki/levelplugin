package me.nakilex.levelplugin.guild;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TextUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import me.nakilex.levelplugin.utils.gui.widgets.ActionWidget;
import me.nakilex.levelplugin.utils.gui.widgets.GuiContext;
import me.nakilex.levelplugin.utils.gui.widgets.GuiLayout;
import me.nakilex.levelplugin.utils.gui.widgets.GuiWidget;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class GuildSettingsGUI implements Listener {
    private final GuildManager manager;
    private GuildMemberGUI memberGUI;
    private final Map<UUID, Integer> roleIndex = new HashMap<>();
    private static final int SIZE = 45;
    private static final String TITLE = "Guild Settings";
    private static final int BACK_SLOT = 0;
    private static final int[] PERM_SLOTS = {10,11,12,13,14,15};
    private final List<GuiWidget> widgets;

    public GuildSettingsGUI(GuildManager manager) {
        this.manager = manager;
        this.widgets = buildWidgets();
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    public void setMemberGUI(GuildMemberGUI memberGUI) {
        this.memberGUI = memberGUI;
    }

    public void open(Player player) {
        Guild g = manager.getGuild(player.getUniqueId());
        if (g == null) return;
        int rIdx = roleIndex.getOrDefault(player.getUniqueId(), 0);
        ItemStack filler = GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE);
        Inventory inv = GuiBuilder.create(SIZE, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .fillEmptySlots(false)
                .border()
                .build();
        renderWidgets(inv, player);
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, filler);
            }
        }
        player.openInventory(inv);
    }

    private ItemStack buildItem(Guild g, GuildPermission perm, int roleIdx) {
        GuildRole role = GuildRole.values()[roleIdx];
        boolean allowed = g.getPermissions(role).has(perm);
        String permName = TextUtil.beautifyWords(perm.name());
        String roleName = TextUtil.beautifyWords(role.name());
        List<String> extra = new ArrayList<>();
        extra.add(ChatColor.GRAY + "Role: " + ChatColor.WHITE + roleName);
        extra.add("");
        extra.addAll(TooltipUtil.clickInstructions("to toggle", "to cycle role"));
        return GuiUtil.createToggleItem(allowed,
                ChatColor.AQUA + permName,
                extra.toArray(new String[0]));
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!GuiUtil.titleMatches(e.getView().getTitle(), TITLE)) return;
        Player player = (Player) e.getWhoClicked();
        Guild g = manager.getGuild(player.getUniqueId());
        if (g == null) return;
        if (handleWidgetClick(e, player)) {
            return;
        }
    }

    private List<GuiWidget> buildWidgets() {
        List<GuiWidget> widgetList = new ArrayList<>();
        widgetList.add(new ActionWidget(BACK_SLOT,
                context -> GuiUtil.getNexoItem("arrow_left2", ChatColor.GRAY + "Back"),
                (click, context) -> {
                    if (memberGUI != null) {
                        memberGUI.open(context.player());
                    }
                }));
        GuildPermission[] perms = GuildPermission.values();
        for (int i = 0; i < perms.length && i < PERM_SLOTS.length; i++) {
            GuildPermission perm = perms[i];
            int slot = PERM_SLOTS[i];
            widgetList.add(new ActionWidget(slot,
                    context -> buildItem(getGuild(context.player()), perm, getRoleIndex(context.player())),
                    (click, context) -> handlePermissionClick(context.player(), perm, click.isRightClick())));
        }
        return widgetList;
    }

    private void renderWidgets(Inventory inventory, Player player) {
        GuiLayout layout = new GuiLayout(inventory);
        GuiContext context = new GuiContext(player, inventory);
        for (GuiWidget widget : widgets) {
            widget.contribute(layout, context);
        }
    }

    private boolean handleWidgetClick(InventoryClickEvent event, Player player) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
            return false;
        }
        GuiWidget widget = widgets.stream()
                .filter(w -> w.handlesSlot(slot))
                .findFirst()
                .orElse(null);
        if (widget == null) {
            return false;
        }
        event.setCancelled(true);
        widget.onClick(slot, event.getClick(), new GuiContext(player, event.getView().getTopInventory()));
        return true;
    }

    private void handlePermissionClick(Player player, GuildPermission perm, boolean rightClick) {
        Guild guild = getGuild(player);
        if (guild == null) return;
        int rIdx = getRoleIndex(player);
        if (rightClick) {
            rIdx = (rIdx + 1) % GuildRole.values().length;
            roleIndex.put(player.getUniqueId(), rIdx);
        } else {
            GuildRole role = GuildRole.values()[rIdx];
            manager.setPermission(player.getUniqueId(), role, perm, !guild.getPermissions(role).has(perm));
        }
        open(player);
    }

    private int getRoleIndex(Player player) {
        return roleIndex.getOrDefault(player.getUniqueId(), 0);
    }

    private Guild getGuild(Player player) {
        return manager.getGuild(player.getUniqueId());
    }
}
