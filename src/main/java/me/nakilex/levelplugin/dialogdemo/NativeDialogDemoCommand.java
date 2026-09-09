package me.nakilex.levelplugin.dialogdemo;

import com.nexomc.nexo.NexoPlugin;
import com.nexomc.nexo.glyphs.Glyph;
import com.nexomc.nexo.glyphs.Shift;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.utils.HeadUtil;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Small playground for Minecraft's native 1.21.6+ dialog screens using Paper's
 * 1.21.7+ Dialog API.
 *
 * This deliberately lives beside LevelPlugin's existing NPC/Lux dialogue
 * systems. Nothing in the current quest dialogue flow is replaced by this demo.
 */
public final class NativeDialogDemoCommand implements TabExecutor {

    private static final List<Integer> GLYPH_TEST_SIZES = List.of(8, 12, 16, 24, 32, 48);
    private static final String GLYPH_TEST_PREFIX = "dialog_glyph_test_";
    private static final List<String> REWARD_DAYS = List.of(
            "SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"
    );
    private static final int REWARD_COLUMN_PITCH = 61;
    private static final int REWARD_BOX_ADVANCE = 49;
    private static final List<String> RPG_BUTTON_STATES = List.of(
            "dialog_rpg_button_normal",
            "dialog_rpg_button_selected",
            "dialog_rpg_button_pressed"
    );

    private static final int BODY_WIDTH = 420;
    /**
     * How the full-size node is sliced across text lines.
     *
     * The art is 81px either way; what changes is how many lines it spans, and
     * the focus ring is exactly as tall as those lines.
     *
     *   9 slices ("_s"): one 9px slice per line. Whole icon clickable, ring
     *                    as tall as the art.
     *   3 slices ("_t"): 27px slices tiled with shrinking ascents (27, 9, -9).
     *                    Ring a third as tall, but only the middle band of the
     *                    icon is clickable.
     *   1 slice  ("_o"): the whole 81px art on one line, ascent 43 so the line
     *                    lands across the middle of the icon. Smallest ring
     *                    possible, and only that one line responds.
     */
    private enum SliceSet {
        NINE("_s", 9, 1, 1, "whole icon clickable, ring as tall as the art"),
        THREE("_t", 3, 3, 4, "middle band clickable, ring a third as tall"),
        ONE("_o", 1, 4, 4, "one line clickable, smallest possible ring");

        private final String infix;
        private final int slices;
        /** Lines the neighbouring bodies must leave clear for the art's overhang. */
        private final int clearAbove;
        private final int clearBelow;
        private final String summary;

        SliceSet(String infix, int slices, int clearAbove, int clearBelow, String summary) {
            this.infix = infix;
            this.slices = slices;
            this.clearAbove = clearAbove;
            this.clearBelow = clearBelow;
            this.summary = summary;
        }

        static SliceSet byKey(String key) {
            for (SliceSet set : values()) {
                if (set.infix.equals("_" + key) || set.name().equalsIgnoreCase(key)) {
                    return set;
                }
            }
            return null;
        }
    }

    private static final int MIN_NODE_BODY_WIDTH = 1;
    private static final int MAX_NODE_BODY_WIDTH = BODY_WIDTH;

    /**
     * Live tuning state for the node, changed with {@code /dialogdemo hex ...}.
     *
     * These are server-side numbers, so they take effect on the next render with
     * no restart and no resource-pack rebuild. What is *not* tunable at runtime
     * is the glyph's own pixel height and ascent: those live in the pack the
     * client has already downloaded. The way around that is to pre-bake the
     * sizes -- which is exactly what the three slice sets are -- and switch
     * between them here.
     */
    private SliceSet nodeSliceSet = SliceSet.ONE;

    /**
     * Width of the node's body element, and therefore of both its focus ring and
     * its click target: a body only receives clicks inside its own bounds, so
     * the two shrink together. Independent of what the art measures, because a
     * body message overflows its width rather than being clipped to it.
     */
    private int nodeBodyWidth = 31;
    /**
     * Blank lines reserved above the legend row. A text line is 9px tall and the
     * legend glyphs are 27px, so three lines plus one spare.
     */
    private static final int LEGEND_RESERVED_LINES = 4;

    private final Map<UUID, NodeState> hexStates = new HashMap<>();

    private final JavaPlugin plugin;

    public NativeDialogDemoCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can open native dialogs.");
            return true;
        }

        String subCommand = args.length == 0 ? "notice" : args[0].toLowerCase(Locale.ROOT);

        switch (subCommand) {
            case "notice" -> showNotice(player);
            case "confirm", "confirmation" -> showConfirmation(player);
            case "input", "form" -> showInputForm(player);
            case "glyphs", "glyph" -> showGlyphSizes(player);
            case "ui", "rpg" -> showRpgUiDemo(player, 0);
            case "rewards", "daily" -> showDailyRewards(player);
            case "rewardbuttons", "buttons" -> showDailyRewardButtons(player);
            case "hex", "states", "element5" -> {
                if (args.length > 1) {
                    handleHexTuning(player, args);
                } else {
                    showHexStates(player);
                }
            }
            case "equipment", "gear", "inspect" -> {
                Player target = args.length > 1 ? Bukkit.getPlayer(args[1]) : player;
                if (target == null) {
                    player.sendMessage(ChatColor.RED + "Player not found or offline: " + args[1]);
                } else {
                    showEquipment(player, target);
                }
            }
            default -> player.sendMessage(ChatColor.RED + "Usage: /" + label
                    + " [notice|confirm|input|glyphs|ui|rewards|rewardbuttons|hex|equipment]");
        }

        return true;
    }

    private void showNotice(Player player) {
        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Native Dialog Demo", NamedTextColor.GOLD))
                        .body(List.of(
                                DialogBody.plainMessage(
                                        Component.text(
                                                "This window is rendered by Minecraft's native dialog system, not an inventory GUI or chat overlay.",
                                                NamedTextColor.WHITE
                                        ),
                                        320
                                ),
                                DialogBody.plainMessage(
                                        Component.text("LevelPlugin can build these dynamically for each player.", NamedTextColor.GRAY),
                                        320
                                )
                        ))
                        .build())
                .type(DialogType.notice(
                        ActionButton.builder(Component.text("Close", NamedTextColor.GREEN))
                                .tooltip(Component.text("Close this demo dialog"))
                                .width(120)
                                .build()
                ))
        );

        player.showDialog(dialog);
    }

    private void showConfirmation(Player player) {
        ActionButton accept = ActionButton.builder(Component.text("Accept quest", NamedTextColor.GREEN))
                .tooltip(Component.text("Runs a server-side Java callback"))
                .width(140)
                .action(DialogAction.customClick(
                        (response, audience) -> {
                            if (audience instanceof Player clickedPlayer) {
                                clickedPlayer.sendMessage(Component.text(
                                        "[Dialog Demo] You accepted the example quest.",
                                        NamedTextColor.GREEN
                                ));
                            }
                        },
                        oneUseCallback()
                ))
                .build();

        ActionButton decline = ActionButton.builder(Component.text("Decline", NamedTextColor.RED))
                .tooltip(Component.text("Also handled without an inventory click event"))
                .width(140)
                .action(DialogAction.customClick(
                        (response, audience) -> {
                            if (audience instanceof Player clickedPlayer) {
                                clickedPlayer.sendMessage(Component.text(
                                        "[Dialog Demo] You declined the example quest.",
                                        NamedTextColor.YELLOW
                                ));
                            }
                        },
                        oneUseCallback()
                ))
                .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Quest Confirmation", NamedTextColor.AQUA))
                        .body(List.of(
                                DialogBody.plainMessage(
                                        Component.text("The blacksmith needs 8 Iron Ore. Do you want to accept this quest?"),
                                        320
                                )
                        ))
                        .build())
                .type(DialogType.confirmation(accept, decline))
        );

        player.showDialog(dialog);
    }

    private void showInputForm(Player player) {
        ActionButton submit = ActionButton.builder(Component.text("Submit", NamedTextColor.GREEN))
                .tooltip(Component.text("Send these values back to LevelPlugin"))
                .width(140)
                .action(DialogAction.customClick(
                        (response, audience) -> {
                            if (!(audience instanceof Player clickedPlayer)) {
                                return;
                            }

                            String nickname = response.getText("nickname");
                            Float difficulty = response.getFloat("difficulty");
                            Boolean hints = response.getBoolean("hints");

                            clickedPlayer.sendMessage(Component.text("[Dialog Demo] Form response:", NamedTextColor.AQUA));
                            clickedPlayer.sendMessage(Component.text("Nickname: " + safe(nickname), NamedTextColor.GRAY));
                            clickedPlayer.sendMessage(Component.text(
                                    "Difficulty: " + (difficulty == null ? "?" : Math.round(difficulty)),
                                    NamedTextColor.GRAY
                            ));
                            clickedPlayer.sendMessage(Component.text(
                                    "Hints: " + (Boolean.TRUE.equals(hints) ? "enabled" : "disabled"),
                                    NamedTextColor.GRAY
                            ));
                        },
                        oneUseCallback()
                ))
                .build();

        ActionButton cancel = ActionButton.builder(Component.text("Cancel", NamedTextColor.RED))
                .width(140)
                .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Character Setup", NamedTextColor.LIGHT_PURPLE))
                        .body(List.of(
                                DialogBody.plainMessage(
                                        Component.text("These controls are native client UI and their values are returned to the plugin."),
                                        340
                                )
                        ))
                        .inputs(List.of(
                                DialogInput.text("nickname", Component.text("NPC nickname"))
                                        .initial(player.getName())
                                        .maxLength(32)
                                        .width(280)
                                        .build(),
                                DialogInput.numberRange("difficulty", Component.text("Difficulty"), 1.0f, 10.0f)
                                        .initial(5.0f)
                                        .step(1.0f)
                                        .width(280)
                                        .build(),
                                DialogInput.bool("hints", Component.text("Show quest hints"))
                                        .initial(true)
                                        .build()
                        ))
                        .build())
                .type(DialogType.confirmation(submit, cancel))
        );

        player.showDialog(dialog);
    }

    private void showGlyphSizes(Player player) {
        List<DialogBody> bodies = new ArrayList<>();
        bodies.add(DialogBody.plainMessage(
                Component.text(
                        "Every sample uses the same 16x16 black PNG. Only the Nexo glyph height and ascent change.",
                        NamedTextColor.GRAY
                ),
                360
        ));

        for (int size : GLYPH_TEST_SIZES) {
            // Bitmap glyphs extend upward from the text baseline. Leading blank
            // lines reserve enough room for the larger samples to avoid overlap.
            int leadingLines = Math.max(0, (size - 8 + 8) / 9);
            Component sample = Component.text()
                    .append(Component.text("\n".repeat(leadingLines)))
                    .append(Component.text(size + " px  ", NamedTextColor.YELLOW))
                    .append(testGlyph(GLYPH_TEST_PREFIX + size))
                    .append(Component.text("  same source texture", NamedTextColor.DARK_GRAY))
                    .build();
            bodies.add(DialogBody.plainMessage(sample, 360));
        }

        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Nexo Glyph Size Test", NamedTextColor.GOLD))
                        .body(bodies)
                        .build())
                .type(DialogType.notice(
                        ActionButton.builder(Component.text("Close", NamedTextColor.GREEN))
                                .width(120)
                                .build()
                ))
        );

        player.showDialog(dialog);
    }

    private void showRpgUiDemo(Player player, int buttonState) {
        int normalizedState = Math.floorMod(buttonState, RPG_BUTTON_STATES.size());
        String buttonGlyphId = RPG_BUTTON_STATES.get(normalizedState);
        String buttonStateName = switch (normalizedState) {
            case 1 -> "selected";
            case 2 -> "pressed";
            default -> "normal";
        };

        Component clickableStar = testGlyph("dialog_rpg_star_normal")
                .hoverEvent(Component.text(
                        "This callback does not rebuild the dialog, so the cursor stays put.",
                        NamedTextColor.GOLD
                ))
                .clickEvent(ClickEvent.callback(
                        audience -> {
                            if (audience instanceof Player clickedPlayer) {
                                clickedPlayer.sendMessage(Component.text(
                                        "[Dialog Demo] Static body glyph clicked; the dialog was not refreshed.",
                                        NamedTextColor.GREEN
                                ));
                            }
                        },
                        reusableCallback()
                ));

        Component starRow = Component.empty()
                .append(Component.text("NO REFRESH  ", NamedTextColor.YELLOW))
                .append(clickableStar)
                .append(Component.text("  hover or click; cursor should stay here", NamedTextColor.GRAY));

        Component customBodyButton = testGlyph(buttonGlyphId)
                .hoverEvent(Component.text(
                        "Custom body click target. Current texture: " + buttonStateName
                                + ". Click to load the next texture.",
                        NamedTextColor.GOLD
                ))
                .clickEvent(ClickEvent.callback(
                        audience -> {
                            if (audience instanceof Player clickedPlayer) {
                                int nextState = (normalizedState + 1) % RPG_BUTTON_STATES.size();
                                clickedPlayer.sendMessage(Component.text(
                                        "[Dialog Demo] Body button texture: "
                                                + buttonStateName + " -> "
                                                + switch (nextState) {
                                                    case 1 -> "selected";
                                                    case 2 -> "pressed";
                                                    default -> "normal";
                                                }
                                                + ". Replacing a dialog may reposition the cursor.",
                                        NamedTextColor.GREEN
                                ));
                                reopenRpgUiDemo(clickedPlayer, nextState);
                            }
                        },
                        oneUseCallback()
                ));

        Component bodyButtonRow = Component.empty()
                .append(Component.text("CUSTOM BODY BUTTON  ", NamedTextColor.YELLOW))
                .append(customBodyButton)
                .append(Component.text("  " + buttonStateName, NamedTextColor.GRAY));

        ActionButton nativeAction = ActionButton.builder(
                        Component.empty()
                                .append(testGlyph("dialog_rpg_star_selected"))
                                .append(Component.text("  Native action", NamedTextColor.GREEN))
                )
                .tooltip(Component.text(
                        "Native buttons own their gray frame and built-in hover highlight.",
                        NamedTextColor.GOLD
                ))
                .width(140)
                .action(DialogAction.customClick(
                        (response, audience) -> audience.sendMessage(Component.text(
                                "[Dialog Demo] Native glyph-labelled action button clicked without refreshing.",
                                NamedTextColor.GREEN
                        )),
                        reusableCallback()
                ))
                .build();

        ActionButton close = ActionButton.builder(Component.text("Close", NamedTextColor.RED))
                .tooltip(Component.text("Close the interaction demo"))
                .width(140)
                .action(DialogAction.customClick(
                        (response, audience) -> audience.closeDialog(),
                        oneUseCallback()
                ))
                .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("RPG UI Glyph Interaction Test", NamedTextColor.GOLD))
                        .body(List.of(
                                DialogBody.plainMessage(
                                        Component.text(
                                                "Body glyphs can be clean custom click targets. Native footer buttons always retain Minecraft's button frame. A texture can only change by replacing the dialog; hover alone cannot swap it.",
                                                NamedTextColor.GRAY
                                        ),
                                        420
                                ),
                                DialogBody.plainMessage(starRow, 420),
                                DialogBody.plainMessage(bodyButtonRow, 420)
                        ))
                        .pause(false)
                        .afterAction(DialogBase.DialogAfterAction.NONE)
                        .build())
                .type(DialogType.confirmation(nativeAction, close))
        );

        player.showDialog(dialog);
    }

    private void reopenRpgUiDemo(Player player, int buttonState) {
        // The static click examples use after_action NONE and never reach this
        // method. A texture change still requires a replacement dialog because
        // the client has no API for mutating an already-open screen.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                showRpgUiDemo(player, buttonState);
            }
        });
    }

    private void showDailyRewards(Player player) {
        Component days = centeredRewardTextRow(
                REWARD_DAYS,
                List.of(
                        NamedTextColor.WHITE, NamedTextColor.WHITE, NamedTextColor.WHITE,
                        NamedTextColor.AQUA,
                        NamedTextColor.WHITE, NamedTextColor.WHITE, NamedTextColor.WHITE
                )
        );

        Component freeBoxes = rewardBoxRow(
                List.of("Coins", "Coins", "Coins", "Scrap", "Coins", "Coins", "30 Gems"),
                3
        );
        Component vipBoxes = rewardBoxRow(
                List.of("30 Gems", "30 Gems", "60 Gems", "Scrap", "30 Gems", "60 Gems", "Hat Coin"),
                3
        );

        Component freeLabels = centeredRewardTextRow(
                List.of("COINS", "COINS", "COINS", "SCRAP", "COINS", "COINS", "30 GEMS"),
                List.of(
                        NamedTextColor.DARK_GRAY, NamedTextColor.DARK_GRAY, NamedTextColor.DARK_GRAY,
                        NamedTextColor.AQUA,
                        NamedTextColor.GOLD, NamedTextColor.GOLD, NamedTextColor.LIGHT_PURPLE
                )
        );

        Component vipLabels = centeredRewardTextRow(
                List.of("30 GEMS", "30 GEMS", "60 GEMS", "SCRAP", "30 GEMS", "60 GEMS", "HAT COIN"),
                List.of(
                        NamedTextColor.DARK_GRAY, NamedTextColor.DARK_GRAY, NamedTextColor.DARK_GRAY,
                        NamedTextColor.AQUA,
                        NamedTextColor.DARK_GRAY, NamedTextColor.DARK_GRAY, NamedTextColor.DARK_GRAY
                )
        );

        int highlightStart = (3 * REWARD_COLUMN_PITCH) - 6;
        int highlightAdvance = 61;

        Component rewardGrid = Component.empty()
                // This is one generic translucent white texture. Coloring it
                // here makes the reusable selected-column layer cyan.
                .append(pixelShift(highlightStart))
                .append(testGlyph("dialog_reward_highlight").color(NamedTextColor.AQUA))
                .append(pixelShift(-(highlightStart + highlightAdvance)))
                .append(days)
                // Bitmap glyphs rise from their baseline. These blank lines
                // reserve the same vertical room as the 48px placeholder.
                .append(Component.text("\n\n\n\n\n\n\n"))
                .append(freeBoxes)
                .append(Component.newline())
                .append(freeLabels)
                .append(Component.text("\n\n\n\n\n\n\n"))
                .append(vipBoxes)
                .append(Component.newline())
                .append(vipLabels);

        ActionButton close = ActionButton.builder(Component.text("Close", NamedTextColor.WHITE))
                .tooltip(Component.text("Close the daily rewards mockup"))
                .width(340)
                .action(DialogAction.customClick(
                        (response, audience) -> audience.closeDialog(),
                        oneUseCallback()
                ))
                .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Daily Rewards", NamedTextColor.GOLD))
                        .body(List.of(
                                DialogBody.plainMessage(
                                        Component.text(
                                                "Visual layout prototype — the cyan Wednesday layer is one reusable tinted glyph.",
                                                NamedTextColor.GRAY
                                        ),
                                        600
                                ),
                                DialogBody.plainMessage(rewardGrid, 600)
                        ))
                        .pause(false)
                        .afterAction(DialogBase.DialogAfterAction.NONE)
                        .build())
                .type(DialogType.notice(close))
        );

        player.showDialog(dialog);
    }

    private void showDailyRewardButtons(Player player) {
        List<String> freeRewards = List.of("Coins", "Coins", "Coins", "Scrap", "Coins", "Coins", "30 Gems");
        List<String> vipRewards = List.of("30 Gems", "30 Gems", "60 Gems", "Scrap", "30 Gems", "60 Gems", "Hat Coin");
        List<ActionButton> rewardButtons = new ArrayList<>();

        addRewardButtonRow(rewardButtons, player, "FREE", freeRewards);
        addRewardButtonRow(rewardButtons, player, "VIP", vipRewards);

        ActionButton close = ActionButton.builder(Component.text("Close", NamedTextColor.WHITE))
                .tooltip(Component.text("Close the independent-button demo"))
                .width(180)
                .action(DialogAction.customClick(
                        (response, audience) -> audience.closeDialog(),
                        oneUseCallback()
                ))
                .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Daily Rewards — Native Buttons", NamedTextColor.GOLD))
                        .body(List.of(DialogBody.plainMessage(
                                Component.text(
                                        "Every framed cell below is a separate native action button. This is the clickable layout primitive; its height and frame are controlled by Minecraft.",
                                        NamedTextColor.GRAY
                                ),
                                680
                        )))
                        .pause(false)
                        .afterAction(DialogBase.DialogAfterAction.NONE)
                        .build())
                .type(DialogType.multiAction(rewardButtons)
                        .columns(7)
                        .exitAction(close)
                        .build())
        );

        player.showDialog(dialog);
    }

    private void addRewardButtonRow(
            List<ActionButton> buttons,
            Player player,
            String tier,
            List<String> rewards
    ) {
        for (int index = 0; index < REWARD_DAYS.size(); index++) {
            String day = REWARD_DAYS.get(index);
            String reward = rewards.get(index);
            boolean current = index == 3;

            Component label = Component.empty()
                    .append(testGlyph("dialog_glyph_test_16"))
                    .append(Component.text(" " + day, current ? NamedTextColor.AQUA : NamedTextColor.WHITE));

            buttons.add(ActionButton.builder(label)
                    .tooltip(Component.text(tier + " " + day + ": " + reward,
                            current ? NamedTextColor.AQUA : NamedTextColor.GOLD))
                    .width(92)
                    .action(DialogAction.customClick(
                            (response, audience) -> player.sendMessage(Component.text(
                                    "[Daily Rewards Buttons] " + tier + " " + day + " — " + reward,
                                    current ? NamedTextColor.GREEN : NamedTextColor.GRAY
                            )),
                            reusableCallback()
                    ))
                    .build());
        }
    }

    private Component rewardBoxRow(List<String> rewards, int currentIndex) {
        Component row = Component.empty();
        for (int index = 0; index < rewards.size(); index++) {
            if (index > 0) {
                row = row.append(pixelShift(REWARD_COLUMN_PITCH - REWARD_BOX_ADVANCE));
            }

            String day = List.of("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday").get(index);
            String reward = rewards.get(index);
            boolean current = index == currentIndex;

            Component box = testGlyph("dialog_glyph_test_48")
                    .hoverEvent(Component.text(
                            day + ": " + reward + (current ? " (current day)" : ""),
                            current ? NamedTextColor.AQUA : NamedTextColor.GOLD
                    ))
                    .clickEvent(ClickEvent.callback(
                            audience -> audience.sendMessage(Component.text(
                                    "[Daily Rewards Mockup] " + day + " — " + reward
                                            + (current ? " would be claimable." : " is a layout placeholder."),
                                    current ? NamedTextColor.GREEN : NamedTextColor.GRAY
                            )),
                            reusableCallback()
                    ));
            row = row.append(box);
        }
        return row;
    }

    private Component centeredRewardTextRow(
            List<String> labels,
            List<NamedTextColor> colors
    ) {
        Component row = Component.empty();
        int cursor = 0;

        for (int index = 0; index < labels.size(); index++) {
            String label = labels.get(index);
            // The active pack's dialog font is monospaced at six pixels per
            // character. Nexo shifts handle the inter-column spacing exactly.
            int labelWidth = label.length() * 6;
            int target = (index * REWARD_COLUMN_PITCH) + ((REWARD_BOX_ADVANCE - labelWidth) / 2);
            row = row.append(pixelShift(target - cursor))
                    .append(Component.text(label, colors.get(index)));
            cursor = target + labelWidth;
        }

        return row;
    }

    private Component pixelShift(int pixels) {
        return pixels == 0 ? Component.empty() : Component.text(Shift.INSTANCE.of(pixels));
    }

    private Component testGlyph(String glyphId) {
        Glyph glyph = NexoPlugin.instance().fontManager().glyphFromID(glyphId);
        if (glyph == null) {
            return Component.text("[missing: " + glyphId + "]", NamedTextColor.RED);
        }
        // White leaves the black source pixels unchanged while retaining the
        // font selected by Nexo's generated glyph component.
        return glyph.glyphComponent().color(NamedTextColor.WHITE);
    }

    private ClickCallback.Options oneUseCallback() {
        return ClickCallback.Options.builder()
                .uses(1)
                .lifetime(Duration.ofMinutes(10))
                .build();
    }

    private ClickCallback.Options reusableCallback() {
        return ClickCallback.Options.builder()
                .uses(100)
                .lifetime(Duration.ofMinutes(10))
                .build();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "(empty)" : value;
    }

    // ------------------------------------------------ live equipment view

    private static final int EQUIPMENT_ICON_SIZE = 36;

    /**
     * One equipment slot: how to read it, what to show when it is empty, and
     * whether the empty placeholder should be a live player head or a plain
     * "empty" pane.
     */
    private enum EquipSlot {
        HEAD("Helmet", true) {
            ItemStack read(Player p) { return p.getInventory().getHelmet(); }
        },
        CHEST("Chestplate", false) {
            ItemStack read(Player p) { return p.getInventory().getChestplate(); }
        },
        LEGS("Leggings", false) {
            ItemStack read(Player p) { return p.getInventory().getLeggings(); }
        },
        FEET("Boots", false) {
            ItemStack read(Player p) { return p.getInventory().getBoots(); }
        },
        MAINHAND("Main Hand", false) {
            ItemStack read(Player p) { return p.getInventory().getItemInMainHand(); }
        },
        OFFHAND("Off Hand", false) {
            ItemStack read(Player p) { return p.getInventory().getItemInOffHand(); }
        };

        final String label;
        final boolean emptyShowsHead;

        EquipSlot(String label, boolean emptyShowsHead) {
            this.label = label;
            this.emptyShowsHead = emptyShowsHead;
        }

        abstract ItemStack read(Player p);
    }

    /**
     * Live equipment inspector: renders another player's actual, current
     * ItemStacks as real item icons in a dialog body.
     *
     * The read happens at render time, straight off the target's live
     * inventory -- there is no snapshot or cache -- so re-showing the dialog
     * (the Refresh button, or reopening it) always reflects whatever the
     * target is wearing and holding right now. If the target logs off between
     * render and a Refresh click, the click handler re-resolves the name and
     * reports that instead of rendering a stale item.
     *
     * Two things come from Paper's item body API directly, not from any glyph
     * trick: {@code ItemDialogBody.Builder#description(PlainMessageDialogBody)}
     * puts a label under the icon in the same body element, and
     * {@code showTooltip(true)} gives the icon its full vanilla tooltip
     * (enchantments, durability, custom name) for free -- no styledTooltip()
     * item-wrapping needed, because the icon already *is* the real item.
     */
    /** Big head icon size for the portrait row; distinct from the equipment column below. */
    private static final int PORTRAIT_SIZE = 64;

    private void showEquipment(Player viewer, Player target) {
        List<DialogBody> body = new ArrayList<>();

        // Portrait: a big head icon is the closest the Dialog API gets to a
        // player model. There is no third body kind for a skin/model renderer
        // -- only item and plainMessage exist -- so this is the ceiling, not a
        // corner cut.
        ItemStack portrait = HeadUtil.createPlayerHead(target, null, null);
        body.add(DialogBody.item(portrait)
                .description(DialogBody.plainMessage(
                        Component.text(target.getName(), NamedTextColor.GOLD), BODY_WIDTH))
                .showDecorations(true)
                .showTooltip(false)
                .width(PORTRAIT_SIZE)
                .height(PORTRAIT_SIZE)
                .build());

        body.add(DialogBody.plainMessage(Component.text(
                "Order: Head -> Chest -> Legs -> Feet -> Main Hand -> Off Hand. Hover an icon for its name.",
                NamedTextColor.GRAY), BODY_WIDTH));

        // Every row below is a bare icon with nothing attached to it, on purpose:
        // see equipmentIcon() for why attaching a label here breaks alignment.
        for (EquipSlot slot : EquipSlot.values()) {
            body.add(equipmentIcon(target, slot));
        }

        ActionButton refresh = ActionButton.builder(Component.text("Refresh", NamedTextColor.WHITE))
                .tooltip(Component.text("Re-read " + target.getName() + "'s gear right now"))
                .width(150)
                .action(DialogAction.customClick(
                        (response, audience) -> {
                            if (!(audience instanceof Player clicked)) return;
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                // Re-resolve by name rather than reusing the captured
                                // Player reference: the target may have logged off
                                // and a new session with a stale reference would
                                // silently read whatever that old object still holds.
                                Player live = Bukkit.getPlayerExact(target.getName());
                                if (live == null || !live.isOnline()) {
                                    clicked.sendMessage(Component.text(
                                            target.getName() + " is no longer online.", NamedTextColor.RED));
                                    clicked.closeDialog();
                                    return;
                                }
                                showEquipment(clicked, live);
                            });
                        },
                        reusableCallback()
                ))
                .build();

        ActionButton close = ActionButton.builder(Component.text("Close", NamedTextColor.RED))
                .width(150)
                .action(DialogAction.customClick(
                        (response, audience) -> {
                            if (!(audience instanceof Player clicked)) return;
                            Bukkit.getScheduler().runTask(plugin, clicked::closeDialog);
                        },
                        oneUseCallback()
                ))
                .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text(target.getName() + "'s Equipment", NamedTextColor.GOLD))
                        .body(body)
                        .pause(false)
                        .afterAction(DialogBase.DialogAfterAction.NONE)
                        .build())
                .type(DialogType.confirmation(refresh, close))
        );

        viewer.showDialog(dialog);
    }

    /**
     * One item body: the slot's real ItemStack, or an empty-slot placeholder.
     *
     * Deliberately has no {@code .description()}. Attaching a label glues the
     * icon and the text into one block that centers as a unit, and a short
     * label ("Boots") and a long one ("Main Hand  (empty)") produce
     * different-width blocks -- so the icon itself visibly drifts left or
     * right depending on how long its own label happens to be. Six identical,
     * unlabelled icons all have the same width and therefore all centre to the
     * exact same x, forming a straight column with no guesswork.
     *
     * The slot name still needs to live somewhere, so it goes into the item's
     * own tooltip instead: a "Slot: Helmet" lore line on a *clone*, never on
     * the real live ItemStack. Hovering answers "what is this" without
     * touching the block's width at all.
     */
    private DialogBody equipmentIcon(Player target, EquipSlot slot) {
        ItemStack live = slot.read(target);
        boolean empty = live == null || live.getType().isAir();

        ItemStack shown = empty
                ? emptySlotPlaceholder(target, slot)
                : withSlotLore(live.clone(), slot);

        return DialogBody.item(shown)
                .showDecorations(true)
                .showTooltip(true)
                .width(EQUIPMENT_ICON_SIZE)
                .height(EQUIPMENT_ICON_SIZE)
                .build();
    }

    /** Adds a "Slot: X" lore line to a clone; never touches the player's real item. */
    private ItemStack withSlotLore(ItemStack clone, EquipSlot slot) {
        ItemMeta meta = clone.getItemMeta();
        if (meta == null) return clone;
        List<String> lore = meta.hasLore() && meta.getLore() != null
                ? new ArrayList<>(meta.getLore())
                : new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "Slot: " + ChatColor.GRAY + slot.label);
        meta.setLore(lore);
        clone.setItemMeta(meta);
        return clone;
    }

    /**
     * What an empty slot shows instead of nothing.
     *
     * HEAD falls back to the target's own skin head, so an empty helmet slot
     * still reads as "this player" rather than as a blank gap. Every other
     * slot falls back to a plain gray pane, since a player's actual face in a
     * boot slot would be a strange substitution.
     */
    private ItemStack emptySlotPlaceholder(Player target, EquipSlot slot) {
        if (slot.emptyShowsHead) {
            // The head fallback is a placeholder we constructed ourselves, not a
            // real equipped item, so naming it is safe -- unlike the real
            // ItemStacks in equipmentIcon(), which are only ever labelled on a
            // clone.
            return HeadUtil.createPlayerHead(target, ChatColor.GRAY + "No Helmet Equipped", List.of(
                    ChatColor.DARK_GRAY + "Slot: " + ChatColor.GRAY + slot.label));
        }
        ItemStack pane = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GRAY + "Empty");
            meta.setLore(List.of(ChatColor.DARK_GRAY + "Slot: " + ChatColor.GRAY + slot.label));
            pane.setItemMeta(meta);
        }
        return pane;
    }

    // ------------------------------------------------- element 1 states

    /**
     * Shows the five "element 1" square button states from the Gray minimalistic UI
     * pack: a static legend of all five, then one node to try them on.
     *
     * Two layout rules matter here, and getting either wrong is very visible:
     *
     * Bitmap glyphs rise from their text baseline, and a dialog body clips a
     * message to the height of the lines it actually contains. A tall glyph on
     * a single-line message is therefore both clipped and drawn over whatever
     * sits above it. Each glyph row reserves room with leading blank lines --
     * roughly ascent / 9px per line -- exactly as the rewards mockup does.
     *
     * Gaps use plain spaces (4px each) rather than Nexo's Shift glyphs. Shift
     * characters live in the generated default font, and this server currently
     * fails to deserialize it ("Failed to deserialize resource at:
     * assets/minecraft/font/include/default.json"), which renders every shift
     * as a stray missing-glyph mark between the nodes.
     */
    private void showHexStates(Player player) {
        NodeState current = hexStates.getOrDefault(player.getUniqueId(), NodeState.IDLE);

        Component legend = Component.empty();
        for (int i = 0; i < NodeState.values().length; i++) {
            NodeState state = NodeState.values()[i];
            if (i > 0) {
                legend = legend.append(Component.text("   "));
            }
            legend = legend
                    .append(testGlyph(state.smallGlyphId()))
                    .append(Component.text(" " + state.label, state.color));
        }

        Component legendBlock = Component.empty()
                .append(Component.text(blankLines(LEGEND_RESERVED_LINES)))
                .append(legend)
                // Clearance for the part of the node art that hangs above its
                // own body box; see SliceSet.clearAbove.
                .append(Component.text(blankLines(nodeSliceSet.clearAbove)));

        Component statusLine = Component.empty()
                // Clearance for the part of the node art that hangs below its
                // own body box; see SliceSet.clearBelow.
                .append(Component.text(blankLines(nodeSliceSet.clearBelow)))
                .append(Component.text("Showing ", NamedTextColor.GRAY))
                .append(Component.text(current.label, current.color))
                .append(Component.text(" - click the node to cycle, or use the buttons.", NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text(
                        "width " + nodeBodyWidth + "   slices " + nodeSliceSet.infix
                                + "   retune live with /dialogdemo hex width <n> or hex slices <s/t/o>",
                        NamedTextColor.DARK_GRAY));

        List<ActionButton> buttons = new ArrayList<>();
        for (NodeState state : NodeState.values()) {
            buttons.add(stateButton(state, current));
        }

        ActionButton close = ActionButton.builder(Component.text("Close", NamedTextColor.RED))
                .width(150)
                .action(DialogAction.customClick(
                        (response, audience) -> {
                            if (!(audience instanceof Player clicked)) return;
                            // after_action NONE means nothing closes on its own.
                            Bukkit.getScheduler().runTask(plugin, clicked::closeDialog);
                        },
                        oneUseCallback()
                ))
                .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Element 5 - Node States", NamedTextColor.GOLD))
                        .body(List.of(
                                DialogBody.plainMessage(Component.text(
                                        "All five states of the button node, then one you can switch between them.",
                                        NamedTextColor.GRAY), BODY_WIDTH),
                                DialogBody.plainMessage(legendBlock, BODY_WIDTH),
                                // Its own narrow body: the client draws its focus/hover
                                // ring around the whole body element, so a 420px one
                                // put a full-width box around the icon.
                                DialogBody.plainMessage(clickableNode(current), nodeBodyWidth),
                                DialogBody.plainMessage(statusLine, BODY_WIDTH)
                        ))
                        .pause(false)
                        .afterAction(DialogBase.DialogAfterAction.NONE)
                        .build())
                .type(DialogType.multiAction(buttons)
                        .exitAction(close)
                        .columns(2)
                        .build())
        );

        player.showDialog(dialog);
    }

    /**
     * The full-size node, clickable across its whole face to cycle state.
     *
     * A body click hit-tests to one 9px text line, so a single tall glyph only
     * responds along a thin strip at its bottom. The art is therefore nine 9px
     * slices, one per line, each carrying the same callback: every row of the
     * icon is a real click target, and the art reassembles at native 1:1.
     *
     * The white ring the client draws around this element on focus is not
     * controllable from here -- see the docs. Its size is: it follows the body
     * element's width, so this art gets its own body barely wider than itself.
     */
    private Component clickableNode(NodeState current) {
        NodeState next = NodeState.values()[(current.ordinal() + 1) % NodeState.values().length];

        HoverEvent<HoverEvent.ShowItem> tooltip = styledTooltip(
                Component.text(current.label, current.color),
                List.of(
                        Component.text(current.hint, NamedTextColor.GRAY),
                        Component.text("Click for " + next.label, NamedTextColor.DARK_GRAY)
                ),
                current.tooltipStyle);

        ClickEvent click = ClickEvent.callback(
                audience -> {
                    if (!(audience instanceof Player clicked)) return;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        hexStates.put(clicked.getUniqueId(), next);
                        showHexStates(clicked);
                    });
                },
                reusableCallback()
        );

        Component art = Component.empty();
        for (int slice = 0; slice < nodeSliceSet.slices; slice++) {
            if (slice > 0) {
                art = art.append(Component.newline());
            }
            art = art.append(testGlyph(sliceGlyphId(current, slice))
                    .hoverEvent(tooltip)
                    .clickEvent(click));
        }
        return art;
    }

    /** A real, clickable node: the glyph is the button's label, sized to fit its frame. */
    private ActionButton stateButton(NodeState state, NodeState current) {
        boolean active = state == current;
        Component label = Component.empty()
                .append(testGlyph(state.buttonGlyphId()))
                .append(Component.text("  " + state.label, active ? state.color : NamedTextColor.WHITE));

        return ActionButton.builder(label)
                .tooltip(Component.text(active ? "Currently showing: " + state.hint : state.hint))
                .width(150)
                .action(DialogAction.customClick(
                        (response, audience) -> {
                            if (!(audience instanceof Player clicked)) return;
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                hexStates.put(clicked.getUniqueId(), state);
                                showHexStates(clicked);
                            });
                        },
                        // The dialog stays open (after_action NONE) and is re-shown
                        // on every click, so these buttons must survive being
                        // clicked more than once.
                        reusableCallback()
                ))
                .build();
    }

    /** Blank lines reserve vertical room so a tall glyph is neither clipped nor overlapped. */
    private String blankLines(int count) {
        return "\n".repeat(count);
    }

    /** One horizontal strip of the art, from whichever slice set is currently selected. */
    private String sliceGlyphId(NodeState state, int slice) {
        return state.glyphId + nodeSliceSet.infix + slice;
    }

    /**
     * Live tuning for the node, so its size can be dialled in without a restart:
     * {@code /dialogdemo hex width <n>} and {@code /dialogdemo hex slices <s|t|o>}.
     *
     * Both are server-side numbers read at render time, so the next redraw picks
     * them up. The glyph's own pixel height cannot be changed this way -- it is
     * baked into the pack the client already has -- which is why the three slice
     * sets are pre-generated and merely selected here.
     */
    private void handleHexTuning(Player player, String[] args) {
        String setting = args[1].toLowerCase(Locale.ROOT);
        String value = args.length > 2 ? args[2] : null;

        switch (setting) {
            case "width" -> {
                Integer parsed = value == null ? null : parseInt(value);
                if (parsed == null || parsed < MIN_NODE_BODY_WIDTH || parsed > MAX_NODE_BODY_WIDTH) {
                    player.sendMessage(ChatColor.RED + "Usage: /dialogdemo hex width <"
                            + MIN_NODE_BODY_WIDTH + "-" + MAX_NODE_BODY_WIDTH + ">");
                    return;
                }
                nodeBodyWidth = parsed;
                player.sendMessage(ChatColor.GREEN + "Node body width: " + ChatColor.WHITE + nodeBodyWidth
                        + ChatColor.GRAY + " (ring and click target, art is 74px wide)");
            }
            case "slices" -> {
                SliceSet set = value == null ? null : SliceSet.byKey(value);
                if (set == null) {
                    player.sendMessage(ChatColor.RED + "Usage: /dialogdemo hex slices <s|t|o>");
                    for (SliceSet option : SliceSet.values()) {
                        player.sendMessage(ChatColor.GRAY + "  " + option.infix.substring(1)
                                + " - " + option.slices + " slice(s), " + option.summary);
                    }
                    return;
                }
                nodeSliceSet = set;
                player.sendMessage(ChatColor.GREEN + "Node slice set: " + ChatColor.WHITE
                        + nodeSliceSet.infix + ChatColor.GRAY + " - " + nodeSliceSet.summary);
            }
            default -> {
                player.sendMessage(ChatColor.RED + "Unknown setting: " + setting);
                player.sendMessage(ChatColor.GRAY + "Try: width, slices");
                return;
            }
        }
        showHexStates(player);
    }

    private List<String> filterPrefix(List<String> options, String typed) {
        String prefix = typed.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.startsWith(prefix)) {
                matches.add(option);
            }
        }
        return matches;
    }

    private Integer parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * A hover tooltip drawn with a resource-pack frame instead of the vanilla
     * purple box.
     *
     * The custom frame comes from the {@code tooltip_style} item component, so
     * it is only available to *item* hovers. A plain {@code showText} hover --
     * and an ActionButton's {@code tooltip}, which is a component, not a hover
     * event -- always renders in vanilla styling. Wrapping the text in a
     * throwaway item and hovering that is what unlocks the frame.
     *
     * The style ids are the ones the plugin already ships for item rarities
     * ({@link me.nakilex.levelplugin.items.data.ItemRarity#getTooltipStyle()}),
     * whose sprites live in the pack under
     * {@code textures/gui/sprites/tooltip/<id>_background.png} and
     * {@code _frame.png}.
     */
    private HoverEvent<HoverEvent.ShowItem> styledTooltip(Component title, List<Component> lore, String style) {
        // Any material works: the frame and text come from the components, and
        // a hover tooltip never draws the item's icon.
        ItemStack carrier = new ItemStack(Material.PAPER);
        ItemMeta meta = carrier.getItemMeta();
        if (meta != null) {
            meta.displayName(title.decoration(TextDecoration.ITALIC, false));
            meta.lore(lore.stream()
                    .map(line -> line.decoration(TextDecoration.ITALIC, false))
                    .toList());
            carrier.setItemMeta(meta);
        }
        ItemUtil.setKeyedComponent(carrier, DataComponentTypes.TOOLTIP_STYLE, Key.key("minecraft:" + style));
        return carrier.asHoverEvent();
    }

    private enum NodeState {
        // element_1 (square button), replacing the earlier element_5 hexagon.
        // Order matches the reference sheet: idle, selected, locked, disabled,
        // activated. Tooltip styles are the rarity frames the plugin already
        // ships, picked to match each state colour.
        IDLE("ui_sq_idle", "Idle", NamedTextColor.GRAY, "Available but not chosen.", "common"),
        // The art for this state is the pack's own glow texture, not a reused
        // hover state -- this pack ships "selected" directly.
        SELECTED("ui_sq_selected", "Selected", NamedTextColor.AQUA, "Chosen; cyan halo.", "rare"),
        LOCKED("ui_sq_locked", "Locked", NamedTextColor.RED, "Unreachable; shows the padlock.", "artifact"),
        DISABLED("ui_sq_disabled", "Disabled", NamedTextColor.DARK_GRAY, "Turned off; cannot be selected.", "common"),
        ACTIVATED("ui_sq_activated", "Activated", NamedTextColor.GREEN, "Unlocked; green fill.", "uncommon");

        private final String glyphId;
        private final String label;
        private final NamedTextColor color;
        private final String hint;
        private final String tooltipStyle;

        NodeState(String glyphId, String label, NamedTextColor color, String hint, String tooltipStyle) {
            this.glyphId = glyphId;
            this.label = label;
            this.color = color;
            this.hint = hint;
            this.tooltipStyle = tooltipStyle;
        }

        /** Same texture at a third of the size, for the inline legend. */
        String smallGlyphId() {
            return glyphId + "_small";
        }

        /** Same texture again, small enough to sit inside an action button frame. */
        String buttonGlyphId() {
            return glyphId + "_btn";
        }

        /** One horizontal strip of the full-size art, counted from the top. */
    }
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 2 && args[0].equalsIgnoreCase("hex")) {
            return filterPrefix(List.of("width", "slices"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("hex") && args[1].equalsIgnoreCase("slices")) {
            return filterPrefix(List.of("s", "t", "o"), args[2]);
        }
        if (args.length != 1) {
            return Collections.emptyList();
        }

        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> options = Arrays.asList(
                "notice", "confirm", "input", "glyphs", "ui", "rewards", "rewardbuttons", "hex", "equipment"
        );
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.startsWith(prefix)) {
                matches.add(option);
            }
        }
        return matches;
    }
}
