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
    private static final int BUTTON_HEIGHT = 17;
    private static final int TAB_Y = 0;
    private static final int TAB_HEIGHT = 23;
    private static final int GEMS_TAB_X = 32;
    private static final int VIP_TAB_X = 114;
    private static final int TAB_WIDTH = 79;
    private static final int CHECKOUT_X = 474;
    private static final int CHECKOUT_Y = 283;
    private static final int CHECKOUT_WIDTH = 203;
    private static final int REMOVE_X = 670;
    private static final int REMOVE_SIZE = 24;
    private static final int FIRST_REMOVE_Y = 211;
    private static final int SECOND_REMOVE_Y = 240;
    private static final int CART_PRICE_RIGHT = 662;
    private static final Key SPACE_FONT = Key.key("minecraft", "space");
    /**
     * Unfiltered vanilla bitmap font. Using minecraft:default here lets the
     * client's Force Unicode Font option change advances behind our back.
     */
    private static final Key STORE_TEXT_FONT = Key.key("minecraft", "store_text");
    /** Vertical variants used for text inside buttons that sit between the 9px slice rows. */
    private static final Key STORE_TEXT_UP_3_FONT = Key.key("minecraft", "store_text_up3");
    private static final Key STORE_TEXT_UP_4_FONT = Key.key("minecraft", "store_text_up4");
    /** A single space-provider character whose advance is exactly WIDTH. */
    private static final String WIDTH_ANCHOR = "\uF821";
    /** Minecraft bitmap glyphs add one pixel of advance after their visible width. */
    private static final int GLYPH_GAP = 1;

    private static final Offer[] OFFERS = {
            new Offer(1_000, "$4.99", 62, 153, 67),
            new Offer(2_000, "$9.99", 196, 153, 67),
            new Offer(5_000, "$24.99", 330, 153, 67),
            new Offer(13_000, "$49.99", 71, 301, 121),
            new Offer(30_000, "$99.99", 276, 301, 121)
    };

    private final JavaPlugin plugin;
    private final Map<UUID, StoreState> states = new HashMap<>();

    public StoreDialogDemo(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getLogger().info("[DialogDemoStore] Loaded compact width-pinned bitmap layout v10.");
    }

    public void show(Player player) {
        StoreState state = states.computeIfAbsent(player.getUniqueId(), ignored -> new StoreState());
        ActionButton close = ActionButton.builder(Component.text("Close", NamedTextColor.WHITE))
                .width(280)
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
                if (rowIntersects(row, offer.buttonY, BUTTON_HEIGHT)) {
                    int index = i;
                    out = out.append(hit(offer.buttonX, offer.buttonWidth,
                            "Add " + offer.gems + " Gems", () -> add(player, index)));
                }
            }
            if (rowIntersects(row, TAB_Y, TAB_HEIGHT)) {
                out = out.append(hit(GEMS_TAB_X, TAB_WIDTH, "Gems", () -> message(player, "Gems category selected.")))
                        .append(hit(VIP_TAB_X, TAB_WIDTH, "VIP", () -> message(player, "VIP products are coming next.")));
            }
            if (rowIntersects(row, CHECKOUT_Y, BUTTON_HEIGHT)) {
                out = out.append(hit(CHECKOUT_X, CHECKOUT_WIDTH, "Checkout", () -> checkout(player, state)));
            }
            if (rowIntersects(row, FIRST_REMOVE_Y, REMOVE_SIZE) && !state.cart.isEmpty()) {
                out = out.append(hit(REMOVE_X, REMOVE_SIZE, "Remove first item", () -> remove(player, state, 0)));
            }
            if (rowIntersects(row, SECOND_REMOVE_Y, REMOVE_SIZE) && state.cart.size() > 1) {
                out = out.append(hit(REMOVE_X, REMOVE_SIZE, "Remove second item", () -> remove(player, state, 1)));
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
            labels.add(centeredLabel(GEMS_TAB_X, TAB_WIDTH, "Gems", NamedTextColor.WHITE));
            labels.add(centeredLabel(VIP_TAB_X, TAB_WIDTH, "VIP", NamedTextColor.DARK_GRAY));
        }
        if (row == 14) {
            labels.add(new Label(43, "1,000 Gems", NamedTextColor.LIGHT_PURPLE));
            labels.add(new Label(177, "2,000 Gems", NamedTextColor.LIGHT_PURPLE));
            labels.add(new Label(311, "5,000 Gems", NamedTextColor.LIGHT_PURPLE));
        }
        if (row == 18) {
            labels.add(iconButtonLabel(62, 67, "$4.99", NamedTextColor.WHITE));
            labels.add(iconButtonLabel(196, 67, "$9.99", NamedTextColor.WHITE));
            labels.add(iconButtonLabel(330, 67, "$24.99", NamedTextColor.WHITE));
        }
        if (row == 30) {
            labels.add(new Label(43, "13,000 Gems", NamedTextColor.LIGHT_PURPLE));
            labels.add(new Label(248, "30,000 Gems", NamedTextColor.LIGHT_PURPLE));
        }
        if (row == 34) {
            labels.add(iconButtonLabel(71, 121, "$49.99", NamedTextColor.WHITE));
            labels.add(iconButtonLabel(276, 121, "$99.99", NamedTextColor.WHITE));
        }
        if (row == 5) labels.add(new Label(545, "Your Account", NamedTextColor.GOLD));
        if (row == 7) labels.add(new Label(547, "0 Gems", NamedTextColor.LIGHT_PURPLE));
        if (row == 9) labels.add(new Label(547, "Rank: VIP", NamedTextColor.GOLD));
        if (row == 10) labels.add(new Label(547, "Status: Expires in", NamedTextColor.DARK_GRAY));
        if (row == 11) labels.add(new Label(547, "30d 23h", NamedTextColor.DARK_GRAY));
        if (row == 20) labels.add(new Label(463, "YOUR CART", NamedTextColor.WHITE));
        if (row == 24 && !state.cart.isEmpty()) labels.add(cartLabel(state.cart.get(0), 463));
        if (row == 27 && state.cart.size() > 1) labels.add(cartLabel(state.cart.get(1), 463));
        if (row == 30) labels.add(new Label(463, "Total: " + total(state), NamedTextColor.WHITE));
        if (row == 32) labels.add(iconButtonLabel(CHECKOUT_X, CHECKOUT_WIDTH, "Checkout", NamedTextColor.WHITE));

        for (Label label : labels) {
            int width = storeTextWidth(label.text);
            Key font = switch (row) {
                // The small price buttons and checkout text render about 3px too low.
                case 18, 32 -> STORE_TEXT_UP_3_FONT;

                // The wide price buttons sit about 4px below the center of their green area.
                case 34 -> STORE_TEXT_UP_4_FONT;

                default -> STORE_TEXT_FONT;
            };
            line = line.append(shift(label.x))
                    .append(Component.text(label.text, label.color).font(font))
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
        int priceStart = CART_PRICE_RIGHT - storeTextWidth(offer.price);
        int gapPixels = Math.max(4, priceStart - x - storeTextWidth(item));
        int spaces = Math.max(1, gapPixels / 4);
        return new Label(x, item + " ".repeat(spaces) + offer.price,
                NamedTextColor.DARK_GRAY);
    }

    private static Label centeredLabel(int x, int width, String text, NamedTextColor color) {
        return new Label(x + (width - storeTextWidth(text)) / 2, text, color);
    }

    private static Label iconButtonLabel(int x, int width, String text, NamedTextColor color) {
        int iconWidth = 10;
        int gap = 3;
        int contentWidth = iconWidth + gap + storeTextWidth(text);
        return new Label(x + (width - contentWidth) / 2 + iconWidth + gap, text, color);
    }

    private static boolean rowIntersects(int row, int y, int height) {
        int rowTop = row * LINE_HEIGHT;
        return rowTop < y + height && rowTop + LINE_HEIGHT > y;
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
