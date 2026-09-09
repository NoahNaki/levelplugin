package me.nakilex.levelplugin.auctionhouse;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.items.listeners.StaticItemListener;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Native Minecraft dialog front-end for auction interactions.
 *
 * The inventory GUI remains the auction browser. This service handles the
 * interactions that are better represented as forms/confirmations:
 * listing, bidding, BIN purchase, searching and listing cancellation.
 */
@SuppressWarnings("UnstableApiUsage")
public final class AuctionDialogService {

    private static final int BODY_WIDTH = 400;
    private static final int INPUT_WIDTH = 340;
    private static final int BUTTON_WIDTH = 165;
    // Minecraft's native item-dialog renderer keeps the actual ItemStack icon at its
    // vanilla GUI size. width/height only resize the surrounding body. Keep the
    // body compact so the item does not look tiny inside a huge empty frame.
    private static final int SELL_ITEM_PREVIEW_SIZE = 40;
    private static final int ITEM_PREVIEW_SIZE = 40;
    private static final Key NEXO_GLYPH_FONT = Key.key("nexo", "default");

    // Existing Nexo glyphs from plugins/Nexo/glyphs/icons_pack.yml.
    // Nexo generates these in nexo:default, not minecraft:default. Native
    // dialogs do not run Nexo's <glyph:...> tag parser, so the glyph component
    // must explicitly select the generated font.
    private static final String GLYPH_AUCTION = "\uA471";      // auctionhouse
    private static final String GLYPH_COINS = "\uA457";        // coins_icon
    private static final String GLYPH_HOURGLASS = "\uA45E";    // hourglass_icon
    private static final String GLYPH_SEARCH = "\uA460";       // magnifying_glass_icon
    private static final String GLYPH_MARKET = "\uA472";       // market
    private static final String GLYPH_GAVEL = "\uA451";        // judge_gavel_icon

    private final JavaPlugin plugin;
    private final AuctionHouseManager manager;
    private final EconomyManager economy;

    public AuctionDialogService(JavaPlugin plugin, AuctionHouseManager manager, EconomyManager economy) {
        this.plugin = plugin;
        this.manager = manager;
        this.economy = economy;
    }

    public void showSellDialog(Player player, ItemStack expectedHandItem, Runnable onReturn) {
        showSellDialog(player, expectedHandItem, "100", "0", 6.0f, onReturn);
    }

    private void showSellDialog(
            Player player,
            ItemStack expectedHandItem,
            String startInitial,
            String binInitial,
            float durationInitial,
            Runnable onReturn
    ) {
        ItemStack preview = expectedHandItem.clone();

        ActionButton submit = ActionButton.builder(iconText(GLYPH_MARKET, "Create Listing", NamedTextColor.GREEN))
                .tooltip(Component.text("Validate the values and list the item"))
                .width(BUTTON_WIDTH)
                .action(DialogAction.customClick(
                        (response, audience) -> {
                            if (!(audience instanceof Player clickedPlayer)) return;

                            String startText = safeInput(response.getText("start_price"), startInitial);
                            String binText = safeInput(response.getText("bin_price"), binInitial);
                            Float durationValue = response.getFloat("duration_hours");
                            float duration = durationValue == null ? durationInitial : durationValue;

                            Bukkit.getScheduler().runTask(plugin, () -> {
                                Integer startPrice = parseInteger(startText);
                                Integer binPrice = parseInteger(binText);
                                long durationHours = Math.max(1L, Math.min(
                                        AuctionHouseManager.MAX_DURATION_HOURS,
                                        Math.round(duration)
                                ));

                                String validationError = validateListingValues(startPrice, binPrice);
                                if (validationError != null) {
                                    ChatMessageUtil.send(clickedPlayer, MessageType.ERROR, validationError);
                                    showSellDialog(clickedPlayer, expectedHandItem, startText, binText, duration, onReturn);
                                    return;
                                }

                                ItemStack current = clickedPlayer.getInventory().getItemInMainHand();
                                if (!sameStack(current, expectedHandItem)) {
                                    ChatMessageUtil.send(clickedPlayer, MessageType.ERROR,
                                            "The item in your hand changed. Nothing was listed.");
                                    runReturn(onReturn);
                                    return;
                                }
                                if (StaticItemListener.isStaticItem(current) || ItemUtil.isSoulbound(current)) {
                                    ChatMessageUtil.send(clickedPlayer, MessageType.ERROR, "You cannot list that item.");
                                    runReturn(onReturn);
                                    return;
                                }

                                ItemStack listingItem = current.clone();
                                clickedPlayer.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                                if (!manager.listItem(clickedPlayer, listingItem, startPrice, binPrice, durationHours)) {
                                    clickedPlayer.getInventory().setItemInMainHand(listingItem);
                                }
                                runReturn(onReturn);
                            });
                        },
                        oneUseCallback()
                ))
                .build();

        ActionButton cancel = returnButton("Cancel", NamedTextColor.RED, onReturn);

        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(iconText(GLYPH_AUCTION, "Create Auction Listing", NamedTextColor.GOLD))
                        .body(List.of(
                                itemBody(preview, SELL_ITEM_PREVIEW_SIZE),
                                DialogBody.plainMessage(
                                        Component.text("Choose a starting bid, optional Buy It Now price, and listing duration.", NamedTextColor.GRAY),
                                        BODY_WIDTH
                                ),
                                DialogBody.plainMessage(
                                        iconText(GLYPH_COINS, "BIN = 0 disables instant purchase. Listing tax is charged when the listing is created.", NamedTextColor.DARK_GRAY),
                                        BODY_WIDTH
                                )
                        ))
                        .inputs(List.of(
                                DialogInput.text("start_price", iconText(GLYPH_COINS, "Starting price", NamedTextColor.YELLOW))
                                        .initial(startInitial)
                                        .maxLength(12)
                                        .width(INPUT_WIDTH)
                                        .build(),
                                DialogInput.text("bin_price", iconText(GLYPH_MARKET, "Buy It Now price (0 = disabled)", NamedTextColor.YELLOW))
                                        .initial(binInitial)
                                        .maxLength(12)
                                        .width(INPUT_WIDTH)
                                        .build(),
                                DialogInput.numberRange(
                                                "duration_hours",
                                                iconText(GLYPH_HOURGLASS, "Duration", NamedTextColor.YELLOW),
                                                1.0f,
                                                (float) AuctionHouseManager.MAX_DURATION_HOURS
                                        )
                                        .initial(clampDuration(durationInitial))
                                        .step(1.0f)
                                        .labelFormat("%s: %s hours")
                                        .width(INPUT_WIDTH)
                                        .build()
                        ))
                        .build())
                .type(DialogType.confirmation(submit, cancel))
        );

        player.showDialog(dialog);
    }

    public void showSearchDialog(Player player, String currentTerm, Consumer<String> onApply, Runnable onCancel) {
        String initial = currentTerm == null ? "" : currentTerm;

        ActionButton apply = ActionButton.builder(iconText(GLYPH_SEARCH, "Apply Search", NamedTextColor.GREEN))
                .tooltip(Component.text("Filter the auction browser"))
                .width(BUTTON_WIDTH)
                .action(DialogAction.customClick(
                        (response, audience) -> {
                            if (!(audience instanceof Player clickedPlayer)) return;
                            String value = response.getText("search_term");
                            String term = value == null ? "" : value.trim();
                            Bukkit.getScheduler().runTask(plugin, () -> onApply.accept(term));
                        },
                        oneUseCallback()
                ))
                .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(iconText(GLYPH_SEARCH, "Search Auctions", NamedTextColor.GOLD))
                        .body(List.of(DialogBody.plainMessage(
                                Component.text("Search by the item's displayed name. Leave the field empty to clear the search.", NamedTextColor.GRAY),
                                BODY_WIDTH
                        )))
                        .inputs(List.of(
                                DialogInput.text("search_term", iconText(GLYPH_SEARCH, "Search term", NamedTextColor.YELLOW))
                                        .initial(initial)
                                        .maxLength(64)
                                        .width(INPUT_WIDTH)
                                        .build()
                        ))
                        .build())
                .type(DialogType.confirmation(
                        apply,
                        returnButton("Cancel", NamedTextColor.RED, onCancel)
                ))
        );

        player.showDialog(dialog);
    }

    public void showBidDialog(Player player, UUID listingId, Runnable onReturn) {
        AuctionItem auction = manager.getAuction(listingId);
        if (!ensureAvailable(player, auction, onReturn)) return;

        int minimumBid = Math.max(auction.getStartingPrice(), auction.getCurrentBid() + 1);
        int currentBid = auction.getCurrentBid();
        String sellerName = sellerName(auction);

        ActionButton placeBid = ActionButton.builder(iconText(GLYPH_GAVEL, "Place Bid", NamedTextColor.GREEN))
                .tooltip(Component.text("Coins are reserved until you are outbid or the auction ends"))
                .width(BUTTON_WIDTH)
                .action(DialogAction.customClick(
                        (response, audience) -> {
                            if (!(audience instanceof Player clickedPlayer)) return;
                            String raw = response.getText("bid_amount");
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                Integer amount = parseInteger(raw);
                                if (amount == null || amount <= 0) {
                                    ChatMessageUtil.send(clickedPlayer, MessageType.ERROR, "Enter a valid positive bid amount.");
                                    showBidDialog(clickedPlayer, listingId, onReturn);
                                    return;
                                }
                                if (manager.bid(clickedPlayer, listingId, amount)) {
                                    ChatMessageUtil.send(clickedPlayer, MessageType.SUCCESS, "Bid placed.");
                                }
                                runReturn(onReturn);
                            });
                        },
                        oneUseCallback()
                ))
                .build();

        Component details = Component.text()
                .append(Component.text("Seller: ", NamedTextColor.GRAY))
                .append(Component.text(sellerName, NamedTextColor.WHITE))
                .append(Component.newline())
                .append(iconOnly(GLYPH_COINS)).append(Component.space())
                .append(Component.text("Current bid: ", NamedTextColor.GRAY))
                .append(Component.text(currentBid > 0 ? coins(currentBid) : "No bids yet", NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(iconOnly(GLYPH_GAVEL)).append(Component.space())
                .append(Component.text("Minimum bid: ", NamedTextColor.GRAY))
                .append(Component.text(coins(minimumBid), NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(iconOnly(GLYPH_COINS)).append(Component.space())
                .append(Component.text("Your balance: ", NamedTextColor.GRAY))
                .append(Component.text(coins(economy.getBalance(player)), NamedTextColor.GREEN))
                .append(Component.newline())
                .append(iconOnly(GLYPH_HOURGLASS)).append(Component.space())
                .append(Component.text("Time remaining: ", NamedTextColor.GRAY))
                .append(Component.text(timeRemaining(auction), NamedTextColor.AQUA))
                .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(iconText(GLYPH_GAVEL, "Place Auction Bid", NamedTextColor.GOLD))
                        .body(List.of(
                                itemBody(auction.getItem()),
                                DialogBody.plainMessage(details, BODY_WIDTH)
                        ))
                        .inputs(List.of(
                                DialogInput.text("bid_amount", iconText(GLYPH_COINS, "Bid amount", NamedTextColor.YELLOW))
                                        .initial(String.valueOf(minimumBid))
                                        .maxLength(12)
                                        .width(INPUT_WIDTH)
                                        .build()
                        ))
                        .build())
                .type(DialogType.confirmation(
                        placeBid,
                        returnButton("Cancel", NamedTextColor.RED, onReturn)
                ))
        );

        player.showDialog(dialog);
    }

    public void showPurchaseDialog(Player player, UUID listingId, Runnable onReturn) {
        AuctionItem auction = manager.getAuction(listingId);
        if (!ensureAvailable(player, auction, onReturn)) return;
        if (auction.getBinPrice() <= 0) {
            ChatMessageUtil.send(player, MessageType.ERROR, "This item has no Buy It Now price.");
            runReturn(onReturn);
            return;
        }

        ActionButton buy = ActionButton.builder(iconText(GLYPH_MARKET, "Buy Now", NamedTextColor.GREEN))
                .tooltip(Component.text("Purchase this listing for " + coins(auction.getBinPrice())))
                .width(BUTTON_WIDTH)
                .action(DialogAction.customClick(
                        (response, audience) -> {
                            if (!(audience instanceof Player clickedPlayer)) return;
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                manager.buyNow(clickedPlayer, listingId);
                                runReturn(onReturn);
                            });
                        },
                        oneUseCallback()
                ))
                .build();

        Component details = Component.text()
                .append(Component.text("Seller: ", NamedTextColor.GRAY))
                .append(Component.text(sellerName(auction), NamedTextColor.WHITE))
                .append(Component.newline())
                .append(iconOnly(GLYPH_MARKET)).append(Component.space())
                .append(Component.text("Buy It Now: ", NamedTextColor.GRAY))
                .append(Component.text(coins(auction.getBinPrice()), NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(iconOnly(GLYPH_COINS)).append(Component.space())
                .append(Component.text("Your balance: ", NamedTextColor.GRAY))
                .append(Component.text(coins(economy.getBalance(player)), NamedTextColor.GREEN))
                .append(Component.newline())
                .append(iconOnly(GLYPH_HOURGLASS)).append(Component.space())
                .append(Component.text("Time remaining: ", NamedTextColor.GRAY))
                .append(Component.text(timeRemaining(auction), NamedTextColor.AQUA))
                .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(iconText(GLYPH_MARKET, "Confirm Purchase", NamedTextColor.GOLD))
                        .body(List.of(
                                itemBody(auction.getItem()),
                                DialogBody.plainMessage(details, BODY_WIDTH),
                                DialogBody.plainMessage(
                                        Component.text("The listing is checked again when you confirm, so stale or already-sold listings cannot be purchased.", NamedTextColor.DARK_GRAY),
                                        BODY_WIDTH
                                )
                        ))
                        .build())
                .type(DialogType.confirmation(
                        buy,
                        returnButton("Cancel", NamedTextColor.RED, onReturn)
                ))
        );

        player.showDialog(dialog);
    }

    public void showCancelListingDialog(Player player, UUID listingId, Runnable onReturn) {
        AuctionItem auction = manager.getAuction(listingId);
        if (!ensureAvailable(player, auction, onReturn)) return;
        if (!auction.getSeller().equals(player.getUniqueId())) {
            ChatMessageUtil.send(player, MessageType.ERROR, "You do not own that listing.");
            runReturn(onReturn);
            return;
        }

        ActionButton cancelListing = ActionButton.builder(iconText(GLYPH_AUCTION, "Cancel Listing", NamedTextColor.RED))
                .tooltip(Component.text("Return the item and refund the current bidder"))
                .width(BUTTON_WIDTH)
                .action(DialogAction.customClick(
                        (response, audience) -> {
                            if (!(audience instanceof Player clickedPlayer)) return;
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                if (manager.cancelListing(clickedPlayer, listingId)) {
                                    ChatMessageUtil.send(clickedPlayer, MessageType.INFO, "Listing cancelled.");
                                }
                                runReturn(onReturn);
                            });
                        },
                        oneUseCallback()
                ))
                .build();

        Component details = Component.text()
                .append(iconOnly(GLYPH_COINS)).append(Component.space())
                .append(Component.text("Starting price: ", NamedTextColor.GRAY))
                .append(Component.text(coins(auction.getStartingPrice()), NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(iconOnly(GLYPH_GAVEL)).append(Component.space())
                .append(Component.text("Current bid: ", NamedTextColor.GRAY))
                .append(Component.text(
                        auction.getCurrentBid() > 0 ? coins(auction.getCurrentBid()) : "No bids yet",
                        NamedTextColor.YELLOW
                ))
                .append(Component.newline())
                .append(iconOnly(GLYPH_HOURGLASS)).append(Component.space())
                .append(Component.text("Time remaining: ", NamedTextColor.GRAY))
                .append(Component.text(timeRemaining(auction), NamedTextColor.AQUA))
                .build();

        Component warning = auction.getHighestBidder() == null
                ? Component.text("The item will be returned to you.", NamedTextColor.GRAY)
                : Component.text("The current highest bidder will be refunded automatically.", NamedTextColor.RED);

        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(iconText(GLYPH_AUCTION, "Cancel Auction Listing?", NamedTextColor.RED))
                        .body(List.of(
                                itemBody(auction.getItem()),
                                DialogBody.plainMessage(details, BODY_WIDTH),
                                DialogBody.plainMessage(warning, BODY_WIDTH)
                        ))
                        .build())
                .type(DialogType.confirmation(
                        cancelListing,
                        returnButton("Keep Listing", NamedTextColor.GREEN, onReturn)
                ))
        );

        player.showDialog(dialog);
    }

    private boolean ensureAvailable(Player player, AuctionItem auction, Runnable onReturn) {
        if (auction != null && auction.getStatus() == AuctionStatus.ACTIVE && auction.getEndTime() > System.currentTimeMillis()) {
            return true;
        }
        ChatMessageUtil.send(player, MessageType.ERROR, "That auction is no longer available.");
        runReturn(onReturn);
        return false;
    }

    private DialogBody itemBody(ItemStack item) {
        return itemBody(item, ITEM_PREVIEW_SIZE);
    }

    private DialogBody itemBody(ItemStack item, int size) {
        return DialogBody.item(item.clone())
                .showDecorations(true)
                .showTooltip(true)
                .width(size)
                .height(size)
                .build();
    }

    private Component iconOnly(String glyph) {
        // White keeps the resource-pack bitmap's original colors instead of tinting it.
        return Component.text(glyph, NamedTextColor.WHITE).font(NEXO_GLYPH_FONT);
    }

    private Component iconText(String glyph, String text, NamedTextColor color) {
        // Keep the font-scoped icon as a child. If it were the root component,
        // Adventure style inheritance would also apply nexo:default to the label.
        return Component.empty()
                .append(iconOnly(glyph))
                .append(Component.space())
                .append(Component.text(text, color));
    }

    private ActionButton returnButton(String label, NamedTextColor color, Runnable onReturn) {
        return ActionButton.builder(Component.text(label, color))
                .width(BUTTON_WIDTH)
                .action(DialogAction.customClick(
                        (response, audience) -> Bukkit.getScheduler().runTask(plugin, () -> runReturn(onReturn)),
                        oneUseCallback()
                ))
                .build();
    }

    private ClickCallback.Options oneUseCallback() {
        return ClickCallback.Options.builder()
                .uses(1)
                .lifetime(Duration.ofMinutes(10))
                .build();
    }

    private void runReturn(Runnable onReturn) {
        if (onReturn != null) {
            onReturn.run();
        }
    }

    private String validateListingValues(Integer startPrice, Integer binPrice) {
        if (startPrice == null || startPrice <= 0) {
            return "Starting price must be a positive whole number.";
        }
        if (binPrice == null || binPrice < 0) {
            return "BIN price must be 0 or a positive whole number.";
        }
        if (binPrice > 0 && binPrice < startPrice) {
            return "BIN price cannot be lower than the starting price.";
        }
        return null;
    }

    private Integer parseInteger(String input) {
        if (input == null) return null;
        String normalized = input.trim().replace(",", "").replace("_", "").replace(" ", "");
        if (normalized.isEmpty()) return null;
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String safeInput(String value, String fallback) {
        return value == null ? fallback : value;
    }

    private float clampDuration(float duration) {
        return Math.max(1.0f, Math.min((float) AuctionHouseManager.MAX_DURATION_HOURS, duration));
    }

    private boolean sameStack(ItemStack current, ItemStack expected) {
        return current != null
                && expected != null
                && !current.getType().isAir()
                && current.getAmount() == expected.getAmount()
                && current.isSimilar(expected);
    }

    private String sellerName(AuctionItem auction) {
        OfflinePlayer seller = Bukkit.getOfflinePlayer(auction.getSeller());
        String name = seller.getName();
        if (name != null && !name.isBlank()) {
            return name;
        }
        String raw = auction.getSeller().toString();
        return raw.substring(0, Math.min(8, raw.length()));
    }

    private String coins(int amount) {
        return String.format(Locale.US, "%,d Coins", amount);
    }

    private String timeRemaining(AuctionItem auction) {
        long seconds = Math.max(0L, (auction.getEndTime() - System.currentTimeMillis()) / 1000L);
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long secs = seconds % 60L;
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        if (minutes > 0) {
            return minutes + "m " + secs + "s";
        }
        return secs + "s";
    }
}
