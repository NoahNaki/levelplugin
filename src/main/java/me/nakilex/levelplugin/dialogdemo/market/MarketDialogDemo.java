package me.nakilex.levelplugin.dialogdemo.market;

import com.nexomc.nexo.NexoPlugin;
import com.nexomc.nexo.glyphs.Glyph;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A market screen painted over a custom shop-window texture, plus the in-game
 * editor used to align it.
 *
 * <h2>Why this is glyphs and not items</h2>
 *
 * A real {@code ItemStack} cannot be positioned. Dialogs have exactly two body
 * kinds, {@code item} and {@code plainMessage}, every body is its own row, and
 * every row self-centres -- {@code DialogBase.Builder} has no alignment field
 * at any level. The equipment demo hit this wall and had to become a centred
 * vertical column. So a 5x3 grid of goods over a fixed background is not
 * reachable with item bodies at any spacing, and the art has to be glyphs in a
 * plain message.
 *
 * <h2>Horizontal: the shift font</h2>
 *
 * Nexo's {@code Shift} glyphs live in its generated {@code default.json}, which
 * this server currently fails to deserialize, so every shift renders as a
 * missing-glyph box. This class uses its own font instead --
 * {@code assets/minecraft/font/space.json}, a vanilla {@code space} provider
 * with power-of-two advances from -1024 to +1024. It is a separate file with
 * its own codepoints, so it is unaffected by whatever is wrong with Nexo's
 * include, and any integer offset decomposes exactly.
 *
 * <h2>Vertical: baked ascent variants</h2>
 *
 * Ascent lives in the pack the client already downloaded, so it cannot be
 * changed from here. Lines advance in 9px steps, which is far too coarse to
 * align to a slot. The way round it is the one the hex node arrived at:
 * pre-bake every value and select at runtime. {@code market.yml} carries nine
 * ascent variants per size, so a row's y splits into whole lines plus an
 * {@code a0..a8} remainder and lands on any pixel.
 *
 * <h2>Draw order</h2>
 *
 * The window art is sliced into 9px rows, one per line. For each line the
 * renderer draws that slice, then shifts back to x=0 and draws whichever icons
 * fall on the line. Later glyphs paint over earlier ones on the same line, so
 * the goods land on top of the window rather than beside it.
 */
public final class MarketDialogDemo implements TabExecutor {

    private static final Key SPACE_FONT = Key.key("minecraft:space");
    private static final int SHIFT_NEG_BASE = 0xF800;
    private static final int SHIFT_POS_BASE = 0xF810;
    private static final int[] SHIFT_STEPS = {1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024};

    private static final int LINE_HEIGHT = 9;
    private static final int BODY_WIDTH = 420;

    /** The window art, sliced into 9px rows by scratchpad/slice_shop_window.py. */
    private static final String WINDOW_SLICE_PREFIX = "market_window_s";
    private static final int WINDOW_HEIGHT = 256;
    private static final int WINDOW_SLICES = WINDOW_HEIGHT / LINE_HEIGHT + 1;

    /**
     * How far the pen moves across one window slice: the 256px art plus the
     * 1px inter-glyph gap.
     *
     * This is also the width every line is padded to. A dialog centres each
     * line on its own measured width, and a line carrying icons emits shift
     * characters that change that width -- row 1 measured 214px against a bare
     * line's 257px, so it centred 21px off and the window came out torn.
     * Padding every line to the same total is what holds the stack straight.
     */
    private static final int WINDOW_ADVANCE = WINDOW_HEIGHT + 1;

    private final JavaPlugin plugin;
    private final MarketLayout layout = new MarketLayout();

    public MarketDialogDemo(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------- command

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (args.length == 0) {
            show(player);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "origin"  -> pair(player, args, "origin", v -> layout.originX = v, v -> layout.originY = v);
            case "pitch"   -> pair(player, args, "pitch",  v -> layout.pitchX  = v, v -> layout.pitchY  = v);
            case "window"  -> pair(player, args, "window", v -> layout.windowX = v, v -> layout.windowY = v);
            case "size"    -> size(player, args);
            case "advance" -> single(player, args, "advance", v -> layout.advanceAdjust = v);
            case "slot"    -> slot(player, args);
            case "nudge"   -> nudge(player, args);
            case "reset"   -> {
                layout.reset();
                player.sendMessage(ChatColor.GREEN + "Layout reset to defaults.");
                show(player);
            }
            case "dump"    -> dump(player);
            default -> player.sendMessage(ChatColor.RED
                    + "Usage: /marketdemo [origin|pitch|window|size|advance|slot|nudge|reset|dump]");
        }
        return true;
    }

    private interface IntSetter {
        void set(int value);
    }

    private void pair(Player player, String[] args, String name, IntSetter x, IntSetter y) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /marketdemo " + name + " <x> <y>");
            return;
        }
        Integer px = parseInt(args[1]);
        Integer py = parseInt(args[2]);
        if (px == null || py == null) {
            player.sendMessage(ChatColor.RED + "Both values must be whole numbers.");
            return;
        }
        x.set(px);
        y.set(py);
        player.sendMessage(ChatColor.GREEN + name + " -> " + ChatColor.WHITE + px + ", " + py);
        show(player);
    }

    private void single(Player player, String[] args, String name, IntSetter setter) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /marketdemo " + name + " <value>");
            return;
        }
        Integer value = parseInt(args[1]);
        if (value == null) {
            player.sendMessage(ChatColor.RED + "Value must be a whole number.");
            return;
        }
        setter.set(value);
        player.sendMessage(ChatColor.GREEN + name + " -> " + ChatColor.WHITE + value);
        show(player);
    }

    private void size(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /marketdemo size <16|24>");
            return;
        }
        Integer value = parseInt(args[1]);
        if (value == null || !layout.sizeIsBaked(value)) {
            // Only baked sizes resolve: size is a glyph height, which lives in
            // the pack, so this picks between pre-generated variants rather
            // than setting a number the client reads.
            player.sendMessage(ChatColor.RED + "Size must be one of the baked variants: 16 or 24.");
            return;
        }
        layout.iconSize = value;
        // Each good scales its own advance, so nothing to reset here.
        player.sendMessage(ChatColor.GREEN + "size -> " + ChatColor.WHITE + value);
        show(player);
    }

    private void slot(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "Usage: /marketdemo slot <0-14> <dx> <dy>");
            return;
        }
        Integer index = parseInt(args[1]);
        Integer dx = parseInt(args[2]);
        Integer dy = parseInt(args[3]);
        if (index == null || dx == null || dy == null || index < 0 || index >= MarketLayout.SLOTS) {
            player.sendMessage(ChatColor.RED + "Slot must be 0-14 and offsets whole numbers.");
            return;
        }
        layout.nudgeX[index] = dx;
        layout.nudgeY[index] = dy;
        player.sendMessage(ChatColor.GREEN + "slot " + index + " nudge -> " + ChatColor.WHITE + dx + ", " + dy);
        show(player);
    }

    private void nudge(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /marketdemo nudge <up|down|left|right> [amount]");
            return;
        }
        int amount = 1;
        if (args.length >= 3) {
            Integer parsed = parseInt(args[2]);
            if (parsed != null) {
                amount = parsed;
            }
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "up"    -> layout.originY -= amount;
            case "down"  -> layout.originY += amount;
            case "left"  -> layout.originX -= amount;
            case "right" -> layout.originX += amount;
            default -> {
                player.sendMessage(ChatColor.RED + "Direction must be up, down, left or right.");
                return;
            }
        }
        player.sendMessage(ChatColor.GREEN + "origin -> " + ChatColor.WHITE
                + layout.originX + ", " + layout.originY);
        show(player);
    }

    /**
     * Print the tuned numbers in a form that can be pasted straight back into
     * {@link MarketLayout}, so a session at the keyboard turns into committed
     * defaults without anyone transcribing values by hand.
     */
    private void dump(Player player) {
        List<String> out = new ArrayList<>();
        out.add("// ---- /marketdemo dump ----");
        out.add("originX = " + layout.originX + "; originY = " + layout.originY + ";");
        out.add("pitchX = " + layout.pitchX + "; pitchY = " + layout.pitchY + ";");
        out.add("iconSize = " + layout.iconSize + "; advanceAdjust = " + layout.advanceAdjust + ";");
        out.add("windowX = " + layout.windowX + "; windowY = " + layout.windowY + ";");
        for (int i = 0; i < MarketLayout.SLOTS; i++) {
            if (layout.nudgeX[i] != 0 || layout.nudgeY[i] != 0) {
                out.add("nudgeX[" + i + "] = " + layout.nudgeX[i]
                        + "; nudgeY[" + i + "] = " + layout.nudgeY[i] + ";");
            }
        }
        out.add("// resolved slot origins (x,y relative to the window art):");
        StringBuilder row = new StringBuilder("// ");
        for (int i = 0; i < MarketLayout.SLOTS; i++) {
            row.append(layout.slotX(i)).append(',').append(layout.slotY(i)).append("  ");
            if (i % MarketLayout.COLS == MarketLayout.COLS - 1) {
                out.add(row.toString());
                row = new StringBuilder("// ");
            }
        }
        out.add("// ---- end dump ----");

        // Console too: chat wraps and mangles this, the log does not, and the
        // log is what gets copied back.
        for (String line : out) {
            player.sendMessage(ChatColor.GRAY + line);
            Bukkit.getLogger().info("[marketdemo] " + line);
        }
        player.sendMessage(ChatColor.YELLOW
                + "Also written to the server console/latest.log - copy it from there.");
    }

    // ------------------------------------------------------------ rendering

    private void show(Player player) {
        Component art = renderWindow();

        Component status = Component.text(
                "origin " + layout.originX + "," + layout.originY
                        + "   pitch " + layout.pitchX + "," + layout.pitchY
                        + "   size " + layout.iconSize
                        + "   advAdj " + layout.advanceAdjust,
                NamedTextColor.DARK_GRAY);

        ActionButton close = ActionButton.builder(Component.text("Close", NamedTextColor.RED))
                .width(150)
                .action(DialogAction.customClick(
                        (response, audience) -> {
                            if (!(audience instanceof Player clicked)) {
                                return;
                            }
                            // after_action NONE means nothing closes on its own.
                            Bukkit.getScheduler().runTask(plugin, clicked::closeDialog);
                        },
                        reusableCallback()))
                .build();

        ActionButton dumpButton = ActionButton.builder(Component.text("Dump spacing", NamedTextColor.YELLOW))
                .tooltip(Component.text("Print the current numbers to chat and the console"))
                .width(150)
                .action(DialogAction.customClick(
                        (response, audience) -> {
                            if (!(audience instanceof Player clicked)) {
                                return;
                            }
                            Bukkit.getScheduler().runTask(plugin, () -> dump(clicked));
                        },
                        reusableCallback()))
                .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Market", NamedTextColor.GOLD))
                        .body(List.of(
                                DialogBody.plainMessage(art, BODY_WIDTH),
                                DialogBody.plainMessage(status, BODY_WIDTH)))
                        // Keeps the cursor where the player left it between
                        // re-renders, which matters when every edit re-shows.
                        .pause(false)
                        .afterAction(DialogBase.DialogAfterAction.NONE)
                        .build())
                .type(DialogType.multiAction(List.of(dumpButton))
                        .exitAction(close)
                        .columns(1)
                        .build()));

        player.showDialog(dialog);
    }

    /**
     * The window art with the goods painted over it.
     *
     * One text line per 9px slice of the window. Icons are emitted after the
     * slice on whichever line they land on, so they paint on top.
     */
    private Component renderWindow() {
        Component out = Component.empty();
        for (int line = 0; line < WINDOW_SLICES; line++) {
            if (line > 0) {
                out = out.append(Component.newline());
            }

            // Pass 1: hit targets, BEFORE the window slice.
            //
            // Click and hover resolve by walking the line accumulating each
            // component's advance and taking the component whose range holds
            // the cursor. The window slice spans the full 257px, so an icon
            // placed after it is unreachable -- that is why the callbacks were
            // attached and never fired, and why /dialogdemo hex works: its
            // lines carry the art and nothing else.
            //
            // A hit target does not need to be the icon. It only needs the
            // icon's advance and the icon's style, so it is a shift: it moves
            // the pen exactly as far and draws nothing at all. An earlier
            // version emitted the real glyph here, which meant every icon was
            // drawn 4-6 times to show once.
            int pen = 0;
            for (int i = 0; i < MarketLayout.SLOTS; i++) {
                if (ascentFor(i, line) < 0) {
                    continue;
                }
                MarketGood good = MarketGood.values()[i];
                int x = layout.slotX(i);
                int advance = good.advanceAt(layout.iconSize) + layout.advanceAdjust;
                out = out.append(shift(x - pen)).append(hitTarget(good, advance));
                pen = x + advance;
            }

            // The window itself.
            out = out.append(shift(-pen))
                    .append(glyph(WINDOW_SLICE_PREFIX + line))
                    .append(shift(-WINDOW_ADVANCE));

            // Pass 2: the icon, drawn exactly once, on the last line its art
            // reaches. It has to be the last such line: the window slice for
            // every line the icon crosses would otherwise paint over it.
            pen = 0;
            for (int i = 0; i < MarketLayout.SLOTS; i++) {
                if (line != lastLineFor(i)) {
                    continue;
                }
                MarketGood good = MarketGood.values()[i];
                int x = layout.slotX(i);
                int advance = good.advanceAt(layout.iconSize) + layout.advanceAdjust;
                // Styled here as well as in pass 1. Which of the two the client
                // resolves a cursor to depends on whether it takes the first or
                // the last match, and styling both covers either.
                out = out.append(shift(x - pen))
                        .append(icon(good, ascentFor(i, line)));
                pen = x + advance;
            }

            // Every line must end the same width, or the client centres them
            // differently and the window shears.
            out = out.append(shift(WINDOW_ADVANCE - pen));
        }
        return out;
    }

    /**
     * A click and hover target with no pixels: a shift that moves the pen the
     * width of the icon and carries the icon's style.
     */
    private Component hitTarget(MarketGood good, int advance) {
        return shift(advance)
                .hoverEvent(tooltipFor(good))
                .clickEvent(buyCallback(good));
    }

    /** The last text line this slot's art reaches, i.e. where it gets drawn. */
    private int lastLineFor(int index) {
        int last = -1;
        for (int line = 0; line < WINDOW_SLICES; line++) {
            if (ascentFor(index, line) >= 0) {
                last = line;
            }
        }
        return last;
    }

    /**
     * The ascent that draws slot {@code index} on {@code line}, or -1 if the
     * icon does not reach that line.
     *
     * A glyph's top lands at (baseline - ascent) and lines advance 9px. The
     * window slices pin the constant: they sit at art y = line*9 with ascent 9.
     * So for a top at art y:
     *
     *     ascent = line * 9 + 9 - y
     *
     * independent of height.
     *
     * A good is emitted on every line its art overlaps rather than only the
     * line holding its top, because a dialog hit-tests a click to a single 9px
     * strip at the line's baseline. One emission would leave most of the icon
     * dead to clicks -- the same reason the hex node is sliced. The extra
     * emissions draw identical pixels in the same place, so they cost nothing
     * visually and make the whole face clickable.
     */
    private int ascentFor(int index, int line) {
        int y = layout.slotY(index);
        int height = layout.iconSize;

        // Line L's clickable strip covers art rows [L*9+2, L*9+11]; the icon
        // covers [y, y+height]. Emit only where those actually overlap.
        int strip = line * LINE_HEIGHT;
        if (strip <= y - 11 || strip >= y + height - 2) {
            return -1;
        }
        int ascent = strip + 9 - y;
        // Ascents below 1 are not generated, and anything past height+8 cannot
        // overlap a line by construction.
        return (ascent >= 1 && ascent <= height + 8) ? ascent : -1;
    }

    /** One good's icon at an explicit ascent, carrying its tooltip and buy callback. */
    private Component icon(MarketGood good, int ascent) {
        String id = "market_" + good.key + "_" + layout.iconSize + "_a" + ascent;
        return glyph(id).hoverEvent(tooltipFor(good)).clickEvent(buyCallback(good));
    }

    private HoverEvent<HoverEvent.ShowItem> tooltipFor(MarketGood good) {
        return styledTooltip(
                Component.text(good.label, good.color),
                List.of(
                        Component.text("Stack: " + good.stack, NamedTextColor.GRAY),
                        Component.text("Price: " + good.price, NamedTextColor.YELLOW),
                        Component.text("Click to buy", NamedTextColor.DARK_GRAY)),
                good.tooltipStyle);
    }

    private ClickEvent buyCallback(MarketGood good) {
        return ClickEvent.callback(
                audience -> {
                    if (!(audience instanceof Player clicked)) {
                        return;
                    }
                    Bukkit.getScheduler().runTask(plugin, () -> clicked.sendMessage(
                            ChatColor.GREEN + "Would buy " + good.stack + "x "
                                    + good.label + " for " + good.price + "."));
                },
                reusableCallback());
    }

    // -------------------------------------------------------------- helpers

    /**
     * A horizontal offset in GUI pixels, from the dedicated space font.
     *
     * Powers of two from 1 to 1024 are present in both signs, so every integer
     * in range decomposes exactly -- no rounding, no accumulation.
     */
    private Component shift(int pixels) {
        if (pixels == 0) {
            return Component.empty();
        }
        int base = pixels < 0 ? SHIFT_NEG_BASE : SHIFT_POS_BASE;
        int remaining = Math.abs(pixels);
        StringBuilder chars = new StringBuilder();
        for (int i = SHIFT_STEPS.length - 1; i >= 0 && remaining > 0; i--) {
            while (remaining >= SHIFT_STEPS[i]) {
                chars.appendCodePoint(base + i);
                remaining -= SHIFT_STEPS[i];
            }
        }
        return Component.text(chars.toString()).font(SPACE_FONT);
    }

    private Component glyph(String glyphId) {
        Glyph glyph = NexoPlugin.instance().fontManager().glyphFromID(glyphId);
        if (glyph == null) {
            return Component.text("[missing:" + glyphId + "]", NamedTextColor.RED);
        }
        return glyph.glyphComponent().color(NamedTextColor.WHITE);
    }

    /** Pack-framed tooltip; see NativeDialogDemoCommand#styledTooltip. */
    private HoverEvent<HoverEvent.ShowItem> styledTooltip(Component title, List<Component> lore, String style) {
        ItemStack carrier = new ItemStack(Material.PAPER);
        ItemMeta meta = carrier.getItemMeta();
        if (meta != null) {
            meta.displayName(title.decoration(TextDecoration.ITALIC, false));
            meta.lore(lore.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).toList());
            carrier.setItemMeta(meta);
        }
        ItemUtil.setKeyedComponent(carrier, DataComponentTypes.TOOLTIP_STYLE, Key.key("minecraft:" + style));
        return carrier.asHoverEvent();
    }

    private ClickCallback.Options reusableCallback() {
        return ClickCallback.Options.builder()
                .uses(500)
                .lifetime(Duration.ofMinutes(30))
                .build();
    }

    private Integer parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("origin", "pitch", "window", "size", "advance", "slot", "nudge", "reset", "dump");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("nudge")) {
            return List.of("up", "down", "left", "right");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("size")) {
            return List.of("16", "24");
        }
        return List.of();
    }
}
