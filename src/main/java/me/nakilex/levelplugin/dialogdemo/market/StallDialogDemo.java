package me.nakilex.levelplugin.dialogdemo.market;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static me.nakilex.levelplugin.dialogdemo.market.DialogPixels.ascentFor;
import static me.nakilex.levelplugin.dialogdemo.market.DialogPixels.glyph;
import static me.nakilex.levelplugin.dialogdemo.market.DialogPixels.lastLineFor;
import static me.nakilex.levelplugin.dialogdemo.market.DialogPixels.shift;
import static me.nakilex.levelplugin.dialogdemo.market.DialogPixels.styledTooltip;

/**
 * A single-offer showcase: one pet framed inside the empty market stall, with
 * its name and a price button under it.
 *
 * This is the bundle-store shape rather than the grid shape -- one thing being
 * sold, shown large, with the art doing the work. {@link MarketDialogDemo} is
 * the 5x3 browser; the two share {@link DialogPixels} and nothing else.
 *
 * <h2>Layering</h2>
 *
 * The stall is 13 slices of 9px, one per text line. The pet sits inside its
 * transparent interior, drawn on the last line its art reaches so that no
 * later stall slice paints over it.
 *
 * The pet's click and hover come from a zero-pixel shift emitted *before* the
 * stall slice on every line the art crosses. Click and hover resolve by walking
 * the line and taking the component whose accumulated advance holds the cursor,
 * and the stall slice spans the full width -- so anything after it is
 * unreachable. See the market docs.
 */
public final class StallDialogDemo implements TabExecutor {

    private static final int BODY_WIDTH = 420;

    private static final String STALL_SLICE_PREFIX = "market_stall_s";
    /** Cropped stall art: 172x109, so 13 lines and an advance of 172+1. */
    private static final int STALL_WIDTH = 172;
    private static final int STALL_HEIGHT = 109;
    private static final int STALL_ADVANCE = STALL_WIDTH + 1;
    private static final int STALL_SLICES =
            (STALL_HEIGHT + DialogPixels.LINE_HEIGHT - 1) / DialogPixels.LINE_HEIGHT;

    /** Pet heights baked into market_stall.yml; nothing else resolves. */
    private static final int[] BAKED_PET_SIZES = {48, 56, 64, 72};
    /**
     * Source art dimensions; width scales as PET_SRC_W/PET_SRC_H of the height.
     *
     * The raw render was 207x272 and did not render at all: Minecraft stitches
     * font glyphs into a 256x256 atlas, and a bitmap over that in either
     * dimension is dropped with no error -- the client just draws its
     * missing-character box. It was the only texture over 256 in the whole
     * pack. 256 itself is fine (the shop window slices are 256x9), so the
     * generator caps the longest side at 256 and this follows it.
     */
    private static final int PET_SRC_W = 195;
    private static final int PET_SRC_H = 256;

    private final JavaPlugin plugin;

    // Live tuning, same contract as the market editor: server-side numbers
    // read at render time, so every edit lands on the next frame.
    //
    // Defaults centre the pet in the stall's transparent interior, measured
    // off the texture as x 19..152, y 25..100 -- centre (85.5, 62.5).
    private int petSize = 64;
    private int petX = 61;
    private int petY = 30;

    private static final String PET_NAME = "Ember Chick";
    private static final int PET_PRICE = 2000;

    public StallDialogDemo(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // -------------------------------------------------------------- command

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
            case "pet" -> pair(player, args);
            case "size" -> size(player, args);
            case "nudge" -> nudge(player, args);
            case "reset" -> {
                petSize = 64;
                petX = 61;
                petY = 30;
                player.sendMessage(ChatColor.GREEN + "Stall layout reset.");
                show(player);
            }
            case "dump" -> dump(player);
            default -> player.sendMessage(ChatColor.RED
                    + "Usage: /marketstall [pet <x> <y>|size <48|56|64|72>|nudge <dir> [n]|reset|dump]");
        }
        return true;
    }

    private void pair(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /marketstall pet <x> <y>");
            return;
        }
        Integer x = parseInt(args[1]);
        Integer y = parseInt(args[2]);
        if (x == null || y == null) {
            player.sendMessage(ChatColor.RED + "Both values must be whole numbers.");
            return;
        }
        petX = x;
        petY = y;
        player.sendMessage(ChatColor.GREEN + "pet -> " + ChatColor.WHITE + petX + ", " + petY);
        show(player);
    }

    private void size(Player player, String[] args) {
        Integer value = args.length >= 2 ? parseInt(args[1]) : null;
        boolean baked = false;
        if (value != null) {
            for (int s : BAKED_PET_SIZES) {
                baked |= s == value;
            }
        }
        if (!baked) {
            // Height is a glyph property baked into the pack, so this selects
            // between pre-generated variants rather than setting a number.
            player.sendMessage(ChatColor.RED + "Size must be one of: 48, 56, 64, 72.");
            return;
        }
        petSize = value;
        player.sendMessage(ChatColor.GREEN + "size -> " + ChatColor.WHITE + petSize
                + ChatColor.GRAY + " (renders " + petWidth() + "px wide)");
        show(player);
    }

    private void nudge(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /marketstall nudge <up|down|left|right> [amount]");
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
            case "up" -> petY -= amount;
            case "down" -> petY += amount;
            case "left" -> petX -= amount;
            case "right" -> petX += amount;
            default -> {
                player.sendMessage(ChatColor.RED + "Direction must be up, down, left or right.");
                return;
            }
        }
        player.sendMessage(ChatColor.GREEN + "pet -> " + ChatColor.WHITE + petX + ", " + petY);
        show(player);
    }

    private void dump(Player player) {
        List<String> out = new ArrayList<>();
        out.add("// ---- /marketstall dump ----");
        out.add("petSize = " + petSize + "; petX = " + petX + "; petY = " + petY + ";");
        out.add("// pet renders " + petWidth() + "x" + petSize
                + ", stall interior is 134x76 at x19..152 y25..100");
        out.add("// ---- end dump ----");
        for (String line : out) {
            player.sendMessage(ChatColor.GRAY + line);
            Bukkit.getLogger().info("[marketstall] " + line);
        }
        player.sendMessage(ChatColor.YELLOW + "Also in the server console - copy it from there.");
    }

    // ------------------------------------------------------------- rendering

    private void show(Player player) {
        ActionButton buy = ActionButton.builder(Component.empty()
                        .append(glyph("diamond_icon"))
                        .append(Component.text("  " + PET_PRICE + " Gems", NamedTextColor.LIGHT_PURPLE)))
                .tooltip(Component.text("Purchase " + PET_NAME))
                .width(180)
                .action(DialogAction.customClick(
                        (response, audience) -> {
                            if (!(audience instanceof Player clicked)) {
                                return;
                            }
                            Bukkit.getScheduler().runTask(plugin, () -> clicked.sendMessage(
                                    ChatColor.LIGHT_PURPLE + "Would buy " + PET_NAME
                                            + " for " + PET_PRICE + " Gems."));
                        },
                        reusableCallback()))
                .build();

        ActionButton close = ActionButton.builder(Component.text("Close", NamedTextColor.RED))
                .width(180)
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

        Component name = Component.empty()
                .append(Component.text(PET_NAME, NamedTextColor.GREEN))
                .decoration(TextDecoration.ITALIC, false);

        Component caption = Component.text("Hover the pet to inspect it", NamedTextColor.DARK_GRAY);

        Component status = Component.text(
                "pet " + petX + "," + petY + "   size " + petSize + " (" + petWidth() + "px wide)",
                NamedTextColor.DARK_GRAY);

        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Market Stall", NamedTextColor.GOLD))
                        .body(List.of(
                                DialogBody.plainMessage(renderStall(), BODY_WIDTH),
                                DialogBody.plainMessage(name, BODY_WIDTH),
                                DialogBody.plainMessage(caption, BODY_WIDTH),
                                DialogBody.plainMessage(status, BODY_WIDTH)))
                        .pause(false)
                        .afterAction(DialogBase.DialogAfterAction.NONE)
                        .build())
                .type(DialogType.multiAction(List.of(buy))
                        .exitAction(close)
                        .columns(1)
                        .build()));

        player.showDialog(dialog);
    }

    /**
     * The stall with the pet inside it.
     *
     * Per line: the pet's invisible hit target, then the stall slice, then the
     * pet itself if this is the last line its art reaches. Every line closes at
     * the same width, or the client centres them differently and the art tears.
     */
    private Component renderStall() {
        int drawLine = lastLineFor(petY, petSize, STALL_SLICES);
        Component out = Component.empty();

        for (int line = 0; line < STALL_SLICES; line++) {
            if (line > 0) {
                out = out.append(Component.newline());
            }

            int pen = 0;
            int ascent = ascentFor(petY, petSize, line);

            // Hit target first: a shift with the pet's advance and style, no
            // pixels. Anything after the full-width stall slice is unreachable.
            if (ascent >= 0) {
                out = out.append(shift(petX))
                        .append(shift(petWidth() + 1)
                                .hoverEvent(petTooltip())
                                .clickEvent(petClick()));
                pen = petX + petWidth() + 1;
            }

            out = out.append(shift(-pen))
                    .append(glyph(STALL_SLICE_PREFIX + line))
                    .append(shift(-STALL_ADVANCE));
            pen = 0;

            if (line == drawLine) {
                out = out.append(shift(petX))
                        .append(glyph("market_pet_" + petSize + "_a" + ascent)
                                .hoverEvent(petTooltip())
                                .clickEvent(petClick()));
                pen = petX + petWidth() + 1;
            }

            out = out.append(shift(STALL_ADVANCE - pen));
        }
        return out;
    }

    private int petWidth() {
        return Math.round((float) PET_SRC_W / PET_SRC_H * petSize);
    }

    private HoverEvent<HoverEvent.ShowItem> petTooltip() {
        return styledTooltip(
                Component.text(PET_NAME, NamedTextColor.GREEN),
                List.of(
                        Component.text("A hatchling that follows you around.", NamedTextColor.GRAY),
                        Component.empty(),
                        Component.text(PET_PRICE + " Gems", NamedTextColor.LIGHT_PURPLE),
                        Component.text("Click to preview", NamedTextColor.DARK_GRAY)),
                "rare");
    }

    private ClickEvent petClick() {
        return ClickEvent.callback(
                audience -> {
                    if (!(audience instanceof Player clicked)) {
                        return;
                    }
                    Bukkit.getScheduler().runTask(plugin, () -> clicked.sendMessage(
                            ChatColor.GREEN + "Previewing " + PET_NAME + "..."));
                },
                reusableCallback());
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
            return List.of("pet", "size", "nudge", "reset", "dump");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("nudge")) {
            return List.of("up", "down", "left", "right");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("size")) {
            return List.of("48", "56", "64", "72");
        }
        return List.of();
    }
}
