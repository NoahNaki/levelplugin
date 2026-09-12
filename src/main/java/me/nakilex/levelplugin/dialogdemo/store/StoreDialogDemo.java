package me.nakilex.levelplugin.dialogdemo.store;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static me.nakilex.levelplugin.dialogdemo.market.DialogPixels.glyph;
import static me.nakilex.levelplugin.dialogdemo.market.DialogPixels.shift;

/** Interactive store-layout prototype for {@code /dialogdemo store}. */
public final class StoreDialogDemo {
    private static final int WIDTH = 730;
    /**
     * This is the client's line-wrapping limit, not the displayed widget
     * width. A composed row temporarily walks to the far edge before our
     * negative-advance glyphs return its pen to the origin. StringSplitter
     * wraps immediately when that intermediate walk exceeds its limit and
     * cannot be "unwrapped" by a later negative advance. Paper 1.21.8
     * restricts plain-message body widths to 1..1024, so use the maximum
     * valid wrapping width. The rendered store itself is still anchored to
     * the final 730-pixel WIDTH_ANCHOR below.
     */
    private static final int BODY_WIDTH = 1024;
    private static final int HEIGHT = 350;
    private static final int LINE_HEIGHT = 9;
    private static final int CHUNK_WIDTH = 240;
    private static final int CHUNKS = 4;
    private static final Key SPACE_FONT = Key.key("minecraft", "space");
    /**
     * Unfiltered vanilla bitmap font. Using minecraft:default here lets the
     * client's Force Unicode Font option change advances behind our back.
     */
    private static final Key STORE_TEXT_FONT = Key.key("minecraft", "store_text");
    /** A single space-provider character whose advance is exactly WIDTH. */
    private static final String WIDTH_ANCHOR = "\uF821";
    /** Minecraft bitmap glyphs add one pixel of advance after their visible width. */
    private static final int GLYPH_GAP = 1;

    private static final Offer[] OFFERS = {
            new Offer(1_000, "$4.99", 7, 152, 89),
            new Offer(2_000, "$9.99", 158, 152, 89),
            new Offer(5_000, "$24.99", 309, 152, 89),
            new Offer(13_000, "$49.99", 7, 312, 153),
            new Offer(30_000, "$99.99", 232, 312, 153)
    };

    private final JavaPlugin plugin;
    private final Map<UUID, StoreState> states = new HashMap<>();

    public StoreDialogDemo(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getLogger().info("[DialogDemoStore] Loaded width-pinned bitmap layout v5.");
    }

    public void show(Player player) {
        StoreState state = states.computeIfAbsent(player.getUniqueId(), ignored -> new StoreState());
        ActionButton close = ActionButton.builder(Component.text("Close", NamedTextColor.RED))
                .width(340)
                .action(DialogAction.customClick((response, audience) -> {
                    if (audience instanceof Player clicked) {
                        Bukkit.getScheduler().runTask(plugin, clicked::closeDialog);
                    }
                }, reusableCallback()))
                .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("View our Store!", NamedTextColor.WHITE))
                        .body(List.of(DialogBody.plainMessage(render(player, state), BODY_WIDTH)))
                        .pause(false)
                        .afterAction(DialogBase.DialogAfterAction.NONE)
                        .build())
                .type(DialogType.notice(close)));
        player.showDialog(dialog);
    }

    private Component render(Player player, StoreState state) {
        Component out = Component.empty();
        int rows = (HEIGHT + LINE_HEIGHT - 1) / LINE_HEIGHT;
        for (int row = 0; row < rows; row++) {
            if (row > 0) out = out.append(Component.newline());

            // Invisible, line-local hit targets go before the opaque canvas.
            for (int i = 0; i < OFFERS.length; i++) {
                Offer offer = OFFERS[i];
                if (row >= offer.buttonY / LINE_HEIGHT && row <= (offer.buttonY + 24) / LINE_HEIGHT) {
                    int index = i;
                    out = out.append(hit(offer.buttonX, offer.buttonWidth,
                            "Add " + offer.gems + " Gems", () -> add(player, index)));
                }
            }
            if (row == 0) {
                out = out.append(hit(0, 102, "Gems", () -> message(player, "Gems category selected.")))
                        .append(hit(105, 102, "VIP", () -> message(player, "VIP products are coming next.")));
            }
            if (row >= 32 && row <= 34) {
                out = out.append(hit(464, 254, "Checkout", () -> checkout(player, state)));
            }
            if (row >= 24 && row <= 26 && !state.cart.isEmpty()) {
                out = out.append(hit(694, 26, "Remove first item", () -> remove(player, state, 0)));
            }
            if (row >= 27 && row <= 29 && state.cart.size() > 1) {
                out = out.append(hit(694, 26, "Remove second item", () -> remove(player, state, 1)));
            }

            int pen = 0;
            for (int chunk = 0; chunk < CHUNKS; chunk++) {
                int x = chunk * CHUNK_WIDTH;
                int width = Math.min(CHUNK_WIDTH, WIDTH - x);
                out = out.append(shift(x - pen))
                        .append(glyph("store_canvas_r" + row + "_c" + chunk));

                // Every generated slice has alpha=1 width pins in its first and
                // last columns. Minecraft therefore reports the exact nominal
                // width for every row/chunk, even when the real artwork is
                // transparent there. This prevents per-row centering drift.
                pen = x + width + GLYPH_GAP;
            }
            out = out.append(shift(-pen));
            out = appendLabels(out, row, state);
            out = out.append(Component.text(WIDTH_ANCHOR).font(SPACE_FONT));
        }
        return out;
    }

    private Component appendLabels(Component line, int row, StoreState state) {
        List<Label> labels = new ArrayList<>();
        if (row == 1) {
            labels.add(new Label(40, "Gems", NamedTextColor.WHITE));
            labels.add(new Label(148, "VIP", NamedTextColor.DARK_GRAY));
        }
        if (row == 14) {
            labels.add(new Label(16, "1,000 Gems", NamedTextColor.LIGHT_PURPLE));
            labels.add(new Label(167, "2,000 Gems", NamedTextColor.LIGHT_PURPLE));
            labels.add(new Label(318, "5,000 Gems", NamedTextColor.LIGHT_PURPLE));
        }
        if (row == 18) {
            labels.add(new Label(43, "$4.99", NamedTextColor.WHITE));
            labels.add(new Label(194, "$9.99", NamedTextColor.WHITE));
            labels.add(new Label(340, "$24.99", NamedTextColor.WHITE));
        }
        if (row == 32) {
            labels.add(new Label(16, "13,000 Gems", NamedTextColor.LIGHT_PURPLE));
            labels.add(new Label(241, "30,000 Gems", NamedTextColor.LIGHT_PURPLE));
        }
        if (row == 36) {
            labels.add(new Label(58, "$49.99", NamedTextColor.WHITE));
            labels.add(new Label(278, "$99.99", NamedTextColor.WHITE));
        }
        if (row == 5) labels.add(new Label(549, "Your Account", NamedTextColor.GOLD));
        if (row == 7) labels.add(new Label(551, "0 Gems", NamedTextColor.LIGHT_PURPLE));
        if (row == 9) labels.add(new Label(551, "Rank: VIP", NamedTextColor.GOLD));
        if (row == 10) labels.add(new Label(551, "Status: Expires in", NamedTextColor.DARK_GRAY));
        if (row == 11) labels.add(new Label(551, "30d 23h", NamedTextColor.DARK_GRAY));
        if (row == 21) labels.add(new Label(467, "YOUR CART", NamedTextColor.WHITE));
        if (row == 24 && !state.cart.isEmpty()) labels.add(cartLabel(state.cart.get(0), 467));
        if (row == 27 && state.cart.size() > 1) labels.add(cartLabel(state.cart.get(1), 467));
        if (row == 30) labels.add(new Label(467, "Total: " + total(state), NamedTextColor.WHITE));
        if (row == 33) labels.add(new Label(552, "Checkout", NamedTextColor.WHITE));

        for (Label label : labels) {
            int width = storeTextWidth(label.text);
            line = line.append(shift(label.x))
                    .append(Component.text(label.text, label.color).font(STORE_TEXT_FONT))
                    .append(shift(-(label.x + width)));
        }
        return line;
    }

    private Label cartLabel(int offerIndex, int x) {
        Offer offer = OFFERS[offerIndex];
        String item = "1x " + String.format("%,d", offer.gems) + " Gems";
        // One component with a measured spacer keeps the amount left-aligned
        // and the price at a stable column without introducing another
        // independently positioned text run on the same canvas row.
        int gapPixels = Math.max(4, 205 - storeTextWidth(item) - storeTextWidth(offer.price));
        int spaces = Math.max(1, gapPixels / 4);
        return new Label(x, item + " ".repeat(spaces) + offer.price,
                NamedTextColor.DARK_GRAY);
    }

    /** Exact advances from Minecraft 1.21.8's built-in font/ascii.png. */
    private static int storeTextWidth(String text) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            width += switch (text.charAt(i)) {
                case ' ' -> 4;
                case 'I' -> 4;
                case 'i' -> 2;
                case 'f', 'k' -> 5;
                case 'l' -> 3;
                case 't' -> 4;
                case ',', '.', ':' -> 2;
                default -> 6;
            };
        }
        return width;
    }

    private Component hit(int x, int width, String tooltip, Runnable action) {
        return shift(x).append(shift(width)
                .hoverEvent(Component.text(tooltip, NamedTextColor.YELLOW))
                .clickEvent(ClickEvent.callback(audience -> action.run(), reusableCallback())))
                .append(shift(-(x + width)));
    }

    private void add(Player player, int offerIndex) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            StoreState state = states.computeIfAbsent(player.getUniqueId(), ignored -> new StoreState());
            if (state.cart.size() >= 2) {
                player.sendMessage(ChatColor.RED + "The demo cart holds two offers.");
            } else {
                state.cart.add(offerIndex);
            }
            show(player);
        });
    }

    private void remove(Player player, StoreState state, int index) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (index < state.cart.size()) state.cart.remove(index);
            show(player);
        });
    }

    private void checkout(Player player, StoreState state) {
        Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(state.cart.isEmpty()
                ? ChatColor.RED + "Your cart is empty."
                : ChatColor.GREEN + "Demo checkout: " + total(state) + " (no purchase was made)."));
    }

    private void message(Player player, String text) {
        Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(ChatColor.GRAY + text));
    }

    private String total(StoreState state) {
        double sum = 0;
        for (int index : state.cart) sum += Double.parseDouble(OFFERS[index].price.substring(1));
        return "$" + String.format("%.2f", sum);
    }

    private ClickCallback.Options reusableCallback() {
        return ClickCallback.Options.builder().uses(500).lifetime(Duration.ofMinutes(30)).build();
    }

    private record Offer(int gems, String price, int buttonX, int buttonY, int buttonWidth) {}
    private record Label(int x, String text, NamedTextColor color) {}
    private static final class StoreState { private final List<Integer> cart = new ArrayList<>(); }
}
