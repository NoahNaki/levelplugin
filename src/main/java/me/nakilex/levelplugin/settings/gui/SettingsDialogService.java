package me.nakilex.levelplugin.settings.gui;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.settings.SettingsActions;
import me.nakilex.levelplugin.settings.data.PlayerSettings;
import me.nakilex.levelplugin.settings.data.PlayerVisibility;
import me.nakilex.levelplugin.settings.environment.PersonalTimeType;
import me.nakilex.levelplugin.settings.environment.PersonalWeatherType;
import me.nakilex.levelplugin.settings.environment.PlayerEnvironmentService;
import me.nakilex.levelplugin.settings.managers.SettingsManager;
import me.nakilex.levelplugin.spells.input.SpellInputMode;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Native Minecraft dialog front-end for player settings.
 *
 * The inventory menu treated every setting as a click-toggle that reopened the
 * whole chest. Native dialogs express the same data as real form controls, so a
 * category is edited as one form and submitted once. Values are applied through
 * {@link SettingsActions}, which skips fields the player did not change.
 *
 * Every control is a fixed-width single-option button, matching the vanilla
 * Options screen. Boolean inputs are deliberately not used: Paper's builder for
 * them exposes no width, so the client centres each checkbox row on its own
 * label and the column of controls comes out ragged.
 */
@SuppressWarnings("UnstableApiUsage")
public final class SettingsDialogService {

    private static final int BODY_WIDTH = 400;
    private static final int INPUT_WIDTH = 300;
    private static final int BUTTON_WIDTH = 150;
    private static final int MENU_BUTTON_WIDTH = 170;

    private static final String OPTION_ON = "on";
    private static final String OPTION_OFF = "off";

    // Existing Nexo glyphs from plugins/Nexo/glyphs/icons_pack.yml. Nexo generates
    // these in nexo:default, not minecraft:default, and native dialogs do not run
    // Nexo's <glyph:...> tag parser, so the component must select the font itself.
    // Icons are used for screens and for the settings that name a concept of their
    // own; plain on/off rows stay bare.
    private static final Key NEXO_GLYPH_FONT = Key.key("nexo", "default");
    private static final String GLYPH_STAR = "\uA45D";      // star_icon
    private static final String GLYPH_SWORD = "\uA458";     // sword_icon
    private static final String GLYPH_SUN = "\uA463";       // sun_icon
    private static final String GLYPH_MOON = "\uA468";      // moon_icon
    private static final String GLYPH_HEART = "\uA44F";     // heart_icon
    private static final String GLYPH_KEY = "\uA45A";       // key_icon
    private static final String GLYPH_BOOK = "\uA461";      // book_icon
    private static final String GLYPH_TREES = "\uA455";     // trees_icon
    private static final String GLYPH_SCROLL = "\uA469";    // scroll_icon
    private static final String GLYPH_DIAMOND = "\uA462";   // diamond_icon
    private static final String GLYPH_MANA = "\uA456";      // mana_icon
    private static final String GLYPH_SHIELD = "\uA459";    // shield_icon

    private static final String KEY_VISIBILITY = "player_visibility";
    private static final String KEY_LOOT_RARITY = "loot_pickup_rarity";
    private static final String KEY_SPELL_INPUT = "spell_input_mode";
    private static final String KEY_WEATHER = "personal_weather";
    private static final String KEY_TIME = "personal_time";

    private final JavaPlugin plugin;
    private final SettingsManager settingsManager;
    private final SettingsActions actions;
    private final SettingsGUI settingsGUI;

    public SettingsDialogService(JavaPlugin plugin, SettingsManager settingsManager, SettingsGUI settingsGUI) {
        this.plugin = plugin;
        this.settingsManager = settingsManager;
        this.settingsGUI = settingsGUI;
        this.actions = new SettingsActions(settingsManager);
    }

    // ---------------------------------------------------------------- root

    public void showRootDialog(Player player) {
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(menuButton(GLYPH_SWORD, "Combat & Spells",
                "Damage feedback, loot filtering and spell casting",
                this::showCombatDialog));
        buttons.add(menuButton(GLYPH_SUN, "Visual & Sound",
                "Holograms, cutscenes, sounds and on-screen reminders",
                this::showVisualDialog));
        buttons.add(menuButton(GLYPH_HEART, "Social & Privacy",
                "Glow, balance visibility and who you can see",
                this::showSocialDialog));
        buttons.add(menuButton(GLYPH_TREES, "Personal Environment",
                "Client-side weather and time",
                this::showEnvironmentDialog));
        buttons.add(menuButton(GLYPH_KEY, "Spell Keybinds",
                "Bind your spells to inputs, per class",
                target -> settingsGUI.openSpellKeybinds(target)));
        buttons.add(menuButton(GLYPH_BOOK, "Spells",
                "Browse your class spells and their scaling",
                target -> toInventory(target, settingsGUI::openSpellUpgrades)));
        buttons.add(menuButton(GLYPH_SCROLL, "Classic Menu",
                "Opens the original inventory settings menu",
                target -> toInventory(target, settingsGUI::openLegacyMenu)));

        // The exit button closes the dialog itself, because these screens do not
        // use the default CLOSE after-action. See keepOpen().
        ActionButton close = ActionButton.builder(Component.text("Close", NamedTextColor.WHITE))
                .width(BUTTON_WIDTH)
                .action(DialogAction.customClick(
                        (response, audience) -> {
                            if (!(audience instanceof Player clicked)) return;
                            Bukkit.getScheduler().runTask(plugin, clicked::closeDialog);
                        },
                        oneUseCallback()
                ))
                .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(keepOpen(DialogBase.builder(iconText(GLYPH_STAR, "Settings", NamedTextColor.WHITE))
                        .body(List.of(body("Pick a category. Changes in a category are applied when you save."))))
                        .build())
                .type(DialogType.multiAction(buttons)
                        .exitAction(close)
                        .columns(2)
                        .build())
        );

        player.showDialog(dialog);
    }

    // ------------------------------------------------------------ combat

    private void showCombatDialog(Player player) {
        PlayerSettings settings = settingsManager.getSettings(player);

        List<DialogInput> inputs = List.of(
                toggle("dmg_chat", "Damage Chat", settings.isDmgChatEnabled()),
                toggle("dmg_numbers", "Damage Numbers", settings.isDmgNumberEnabled()),
                raritySelector(settings),
                spellInputSelector(settings)
        );

        List<DialogBody> body = List.of(
                body("Loot Pickup Filter picks up armour and weapons of the chosen rarity or higher."),
                body("Mouse Combo casts with R/L sequences (RRL, RLR, RRR, RLL). "
                        + "Mouse + Keyboard casts with Sneak + Click or Sneak + Sneak.")
        );

        showCategory(player, GLYPH_SWORD, "Combat & Spells", inputs, body, (values, target) -> applyCombat(target, values));
    }

    private int applyCombat(Player player, FormValues values) {
        PlayerSettings settings = settingsManager.getSettings(player);
        int changed = 0;
        changed += count(actions.setDamageChat(player, settings, values.bool("dmg_chat", settings.isDmgChatEnabled())));
        changed += count(actions.setDamageNumbers(player, settings, values.bool("dmg_numbers", settings.isDmgNumberEnabled())));
        changed += count(actions.setLootPickupRarity(player, settings, selectedRarity(values, settings)));
        changed += count(actions.setSpellInputMode(player, settings, selectedSpellInput(values, settings)));
        return changed;
    }

    // ------------------------------------------------------------ visual

    private void showVisualDialog(Player player) {
        PlayerSettings settings = settingsManager.getSettings(player);

        List<DialogInput> inputs = List.of(
                toggle("drop_details", "Drop Details", settings.isDropDetailsEnabled()),
                toggle("drop_details_chat", "Drop Details Chat", settings.isDropDetailsChatEnabled()),
                toggle("auto_skip_cutscenes", "Auto Skip Cutscenes", settings.isAutoSkipCutscenes()),
                toggle("auto_skip_songs", "Auto Skip Songs", settings.isAutoSkipSongs()),
                toggle("npc_sounds", "NPC Sound Effects", settings.isNpcSoundEffectsEnabled()),
                toggle("achievement_sounds", "Achievement Sounds", settings.isAchievementSoundEffectsEnabled()),
                toggle("skill_point_reminder", "Skill Point Reminder", settings.isSkillPointReminderEnabled()),
                toggle("full_inventory_title", "Full Inventory Title", settings.isFullInventoryTitleEnabled()),
                toggle("tips", "Tips", settings.isTipsEnabled()),
                toggle("booster_boss_bar", "Booster Boss Bar", settings.isBoosterBossBarEnabled()),
                toggle("quest_particles", "Quest Path Particles", settings.isQuestTrackingParticlesEnabled())
        );

        showCategory(player, GLYPH_SUN, "Visual & Sound", inputs,
                List.of(body("NPC and achievement sounds are client-side only.")),
                (values, target) -> applyVisual(target, values));
    }

    private int applyVisual(Player player, FormValues values) {
        PlayerSettings settings = settingsManager.getSettings(player);
        int changed = 0;
        changed += count(actions.setDropDetails(player, settings, values.bool("drop_details", settings.isDropDetailsEnabled())));
        changed += count(actions.setDropDetailsChat(player, settings, values.bool("drop_details_chat", settings.isDropDetailsChatEnabled())));
        changed += count(actions.setAutoSkipCutscenes(player, settings, values.bool("auto_skip_cutscenes", settings.isAutoSkipCutscenes())));
        changed += count(actions.setAutoSkipSongs(player, settings, values.bool("auto_skip_songs", settings.isAutoSkipSongs())));
        changed += count(actions.setNpcSoundEffects(player, settings, values.bool("npc_sounds", settings.isNpcSoundEffectsEnabled())));
        changed += count(actions.setAchievementSoundEffects(player, settings, values.bool("achievement_sounds", settings.isAchievementSoundEffectsEnabled())));
        changed += count(actions.setSkillPointReminder(player, settings, values.bool("skill_point_reminder", settings.isSkillPointReminderEnabled())));
        changed += count(actions.setFullInventoryTitle(player, settings, values.bool("full_inventory_title", settings.isFullInventoryTitleEnabled())));
        changed += count(actions.setTips(player, settings, values.bool("tips", settings.isTipsEnabled())));
        changed += count(actions.setBoosterBossBar(player, settings, values.bool("booster_boss_bar", settings.isBoosterBossBarEnabled())));
        changed += count(actions.setQuestTrackingParticles(player, settings, values.bool("quest_particles", settings.isQuestTrackingParticlesEnabled())));
        return changed;
    }

    // ------------------------------------------------------------ social

    private void showSocialDialog(Player player) {
        PlayerSettings settings = settingsManager.getSettings(player);
        boolean visibilityLocked = settingsGUI.isVisibilityLocked(player);

        List<DialogInput> inputs = new ArrayList<>();
        inputs.add(toggle("party_glow", "Party Glow", settings.isPartyGlowEnabled()));
        inputs.add(toggle("friend_glow", "Friend Glow", settings.isFriendGlowEnabled()));
        inputs.add(toggle("balance_public", "Public Balance", settings.isBalancePublic()));
        inputs.add(toggle("chat_games", "Chat Games", settings.isChatGamesEnabled()));
        if (!visibilityLocked) {
            inputs.add(visibilitySelector(settings));
        }

        DialogBody note = visibilityLocked
                ? DialogBody.plainMessage(Component.text(
                        "Player Visibility is locked until Office Errands is complete, so it is not shown here.",
                        NamedTextColor.RED), BODY_WIDTH)
                : body("Player Visibility controls which other players are rendered for you.");

        showCategory(player, GLYPH_HEART, "Social & Privacy", inputs, List.of(note),
                (values, target) -> applySocial(target, values));
    }

    private int applySocial(Player player, FormValues values) {
        PlayerSettings settings = settingsManager.getSettings(player);
        int changed = 0;
        changed += count(actions.setPartyGlow(player, settings, values.bool("party_glow", settings.isPartyGlowEnabled())));
        changed += count(actions.setFriendGlow(player, settings, values.bool("friend_glow", settings.isFriendGlowEnabled())));
        changed += count(actions.setBalancePublic(player, settings, values.bool("balance_public", settings.isBalancePublic())));
        changed += count(actions.setChatGames(player, settings, values.bool("chat_games", settings.isChatGamesEnabled())));

        // Re-check the lock at submit time: the quest may have started while the
        // form was open, and the input is absent when it was locked on render.
        if (!settingsGUI.isVisibilityLocked(player)) {
            changed += count(actions.setPlayerVisibility(player, settings, selectedVisibility(values, settings)));
        }
        return changed;
    }

    // ------------------------------------------------------- environment

    private void showEnvironmentDialog(Player player) {
        PlayerEnvironmentService environment = settingsGUI.getEnvironmentService();
        if (environment == null) {
            ChatMessageUtil.send(player, MessageType.ERROR, "Personal environment is unavailable.");
            showRootDialog(player);
            return;
        }

        PersonalWeatherType weather = environment.getCurrentWeatherOrReset(player);
        PersonalTimeType time = environment.getCurrentTimeOrReset(player);

        List<DialogInput> inputs = List.of(
                options(KEY_WEATHER, GLYPH_SUN, "Personal Weather", PersonalWeatherType.values(), weather,
                        this::weatherOption),
                options(KEY_TIME, GLYPH_MOON, "Personal Time", PersonalTimeType.values(), time,
                        this::timeOption)
        );

        showCategory(player, GLYPH_TREES, "Personal Environment", inputs,
                List.of(body("Weather and time are changed for your client only. "
                        + "World Default hands control back to the server.")),
                (values, target) -> applyEnvironment(target, values));
    }

    private int applyEnvironment(Player player, FormValues values) {
        PlayerEnvironmentService environment = settingsGUI.getEnvironmentService();
        if (environment == null) {
            return 0;
        }
        int changed = 0;

        PersonalWeatherType weather = PersonalWeatherType.fromInput(values.option(KEY_WEATHER));
        if (weather != null && weather != environment.getCurrentWeatherOrReset(player)) {
            environment.applyWeather(player, weather);
            changed++;
        }

        PersonalTimeType time = PersonalTimeType.fromInput(values.option(KEY_TIME));
        if (time != null && time != environment.getCurrentTimeOrReset(player)) {
            environment.applyTime(player, time);
            changed++;
        }
        return changed;
    }

    // ------------------------------------------------------- shared frame

    private void showCategory(
            Player player,
            String glyph,
            String title,
            List<DialogInput> inputs,
            List<DialogBody> body,
            BiFunction<FormValues, Player, Integer> apply
    ) {
        ActionButton save = ActionButton.builder(Component.text("Save Changes", NamedTextColor.WHITE))
                .tooltip(Component.text("Apply every value on this screen"))
                .width(BUTTON_WIDTH)
                .action(DialogAction.customClick(
                        (response, audience) -> {
                            if (!(audience instanceof Player clicked)) return;
                            // The response view is read here, in the callback, and
                            // the values are applied on the main thread because
                            // every setting below touches Bukkit state.
                            FormValues values = FormValues.read(response, inputs);
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                int changed = apply.apply(values, clicked);
                                settingsManager.saveActiveProfileSettings(clicked);
                                ChatMessageUtil.send(clicked, changed > 0 ? MessageType.SUCCESS : MessageType.INFO,
                                        changed == 0
                                                ? "No settings were changed."
                                                : changed + (changed == 1 ? " setting updated." : " settings updated."));
                                showRootDialog(clicked);
                            });
                        },
                        oneUseCallback()
                ))
                .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(keepOpen(DialogBase.builder(iconText(glyph, title, NamedTextColor.WHITE))
                        .body(body)
                        .inputs(inputs))
                        .build())
                .type(DialogType.confirmation(save, returnButton("Back")))
        );

        player.showDialog(dialog);
    }

    /**
     * Keeps the dialog screen open when a button is clicked.
     *
     * With the default {@code CLOSE} after-action the client tears the screen
     * down before the next dialog arrives. Going through "no screen" is what
     * re-centres the mouse — the client only recentres when it opens a screen
     * from none — so every hop between settings screens threw the cursor back to
     * the middle. Leaving the screen up means the next dialog replaces this one
     * in place and the pointer stays where the player left it.
     *
     * The cost is that nothing closes on its own any more: buttons that leave
     * the dialog system have to close it themselves.
     *
     * {@code pause} must be turned off to go with it. It defaults to true, and
     * Paper rejects a pausing dialog whose after-action never unpauses the game
     * ("Dialogs that pause the game must use after_action values that unpause it
     * after user action!"). Pausing only affects singleplayer anyway.
     */
    private DialogBase.Builder keepOpen(DialogBase.Builder builder) {
        return builder
                .afterAction(DialogBase.DialogAfterAction.NONE)
                .pause(false);
    }

    /** Hands off to an inventory GUI, closing the dialog screen first. */
    private void toInventory(Player player, Consumer<Player> open) {
        player.closeDialog();
        open.accept(player);
    }

    private ActionButton returnButton(String label) {
        return ActionButton.builder(Component.text(label, NamedTextColor.WHITE))
                .tooltip(Component.text("Discard the values on this screen"))
                .width(BUTTON_WIDTH)
                .action(DialogAction.customClick(
                        (response, audience) -> {
                            if (!(audience instanceof Player clicked)) return;
                            Bukkit.getScheduler().runTask(plugin, () -> showRootDialog(clicked));
                        },
                        oneUseCallback()
                ))
                .build();
    }

    private ActionButton menuButton(String glyph, String label, String tooltip, Consumer<Player> onClick) {
        return ActionButton.builder(iconText(glyph, label, NamedTextColor.WHITE))
                .tooltip(Component.text(tooltip))
                .width(MENU_BUTTON_WIDTH)
                .action(DialogAction.customClick(
                        (response, audience) -> {
                            if (!(audience instanceof Player clicked)) return;
                            Bukkit.getScheduler().runTask(plugin, () -> onClick.accept(clicked));
                        },
                        oneUseCallback()
                ))
                .build();
    }

    // ------------------------------------------------------------ inputs

    private DialogBody body(String text) {
        return DialogBody.plainMessage(Component.text(text, NamedTextColor.WHITE), BODY_WIDTH);
    }

    /**
     * An on/off control rendered as a vanilla-style "Label: On" cycle button.
     * Toggles carry no glyph: an icon on every row of a long list reads as
     * decoration and adds nothing the label does not already say.
     */
    private DialogInput toggle(String key, String label, boolean initial) {
        List<SingleOptionDialogInput.OptionEntry> entries = List.of(
                SingleOptionDialogInput.OptionEntry.create(OPTION_ON,
                        Component.text("On", NamedTextColor.GREEN), initial),
                SingleOptionDialogInput.OptionEntry.create(OPTION_OFF,
                        Component.text("Off", NamedTextColor.RED), !initial)
        );
        return DialogInput.singleOption(key, Component.text(label, NamedTextColor.WHITE), entries)
                .width(INPUT_WIDTH)
                .build();
    }

    /**
     * A fixed-width selector over an enum, so every control lines up.
     *
     * The width is fixed independently of the label, so a glyph here costs no
     * alignment — the button stays the same size whatever the icon renders as.
     */
    private <T extends Enum<T>> DialogInput options(
            String key,
            String glyph,
            String label,
            T[] values,
            T current,
            java.util.function.Function<T, Component> display
    ) {
        List<SingleOptionDialogInput.OptionEntry> entries = new ArrayList<>();
        for (T value : values) {
            entries.add(SingleOptionDialogInput.OptionEntry.create(
                    value.name(),
                    display.apply(value),
                    value == current
            ));
        }
        return DialogInput.singleOption(key, iconText(glyph, label, NamedTextColor.WHITE), entries)
                .width(INPUT_WIDTH)
                .build();
    }

    private DialogInput raritySelector(PlayerSettings settings) {
        return options(KEY_LOOT_RARITY, GLYPH_DIAMOND, "Loot Pickup Filter",
                settings.getLootPickupRarities(), settings.getLootPickupRarity(),
                rarity -> Component.text(formatEnum(rarity.name()), rarityColor(rarity)));
    }

    private DialogInput spellInputSelector(PlayerSettings settings) {
        return options(KEY_SPELL_INPUT, GLYPH_MANA, "Spell Input Mode",
                SpellInputMode.values(), settings.getSpellInputMode(),
                mode -> Component.text(mode.getDisplayName(), NamedTextColor.AQUA));
    }

    private DialogInput visibilitySelector(PlayerSettings settings) {
        return options(KEY_VISIBILITY, GLYPH_SHIELD, "Player Visibility",
                PlayerVisibility.values(), settings.getPlayerVisibility(),
                visibility -> Component.text(visibilityLabel(visibility), visibilityColor(visibility)));
    }

    // ----------------------------------------------------------- response

    /**
     * A submitted form read out of the dialog response.
     *
     * The response view is only read inside the dialog callback, which does not
     * run on the main thread. Snapshotting it keeps the view off the scheduled
     * task that actually applies the values.
     */
    private record FormValues(Map<String, String> options) {

        static FormValues read(DialogResponseView response, List<DialogInput> inputs) {
            Map<String, String> options = new HashMap<>();
            for (DialogInput input : inputs) {
                if (input instanceof SingleOptionDialogInput) {
                    // singleOption reports the selected option id through getText.
                    String key = input.key();
                    options.put(key, response.getText(key));
                }
            }
            return new FormValues(options);
        }

        boolean bool(String key, boolean fallback) {
            String value = options.get(key);
            if (OPTION_ON.equals(value)) return true;
            if (OPTION_OFF.equals(value)) return false;
            return fallback;
        }

        String option(String key) {
            return options.get(key);
        }
    }

    private ItemRarity selectedRarity(FormValues values, PlayerSettings settings) {
        String id = values.option(KEY_LOOT_RARITY);
        if (id == null) return settings.getLootPickupRarity();
        for (ItemRarity rarity : settings.getLootPickupRarities()) {
            if (rarity.name().equals(id)) return rarity;
        }
        return settings.getLootPickupRarity();
    }

    private SpellInputMode selectedSpellInput(FormValues values, PlayerSettings settings) {
        String id = values.option(KEY_SPELL_INPUT);
        if (id == null) return settings.getSpellInputMode();
        for (SpellInputMode mode : SpellInputMode.values()) {
            if (mode.name().equals(id)) return mode;
        }
        return settings.getSpellInputMode();
    }

    private PlayerVisibility selectedVisibility(FormValues values, PlayerSettings settings) {
        String id = values.option(KEY_VISIBILITY);
        if (id == null) return settings.getPlayerVisibility();
        for (PlayerVisibility visibility : PlayerVisibility.values()) {
            if (visibility.name().equals(id)) return visibility;
        }
        return settings.getPlayerVisibility();
    }

    // ------------------------------------------------------------ helpers

    private int count(boolean changed) {
        return changed ? 1 : 0;
    }

    private String visibilityLabel(PlayerVisibility visibility) {
        return switch (visibility) {
            case SHOW_ALL -> "Show All Players";
            case FRIENDS_ONLY -> "Friends Only";
            case HIDE_ALL -> "Hide All Players";
        };
    }

    private Component weatherOption(PersonalWeatherType weather) {
        return switch (weather) {
            case CLEAR -> Component.text("Clear", NamedTextColor.YELLOW);
            case RAIN -> Component.text("Rain", NamedTextColor.AQUA);
            case THUNDER -> Component.text("Thunder", NamedTextColor.DARK_AQUA);
            case RESET -> Component.text("World Default", NamedTextColor.GRAY);
        };
    }

    private Component timeOption(PersonalTimeType time) {
        return switch (time) {
            case DAY -> Component.text("Day", NamedTextColor.YELLOW);
            case NIGHT -> Component.text("Night", NamedTextColor.BLUE);
            case SUNSET -> Component.text("Sunset", NamedTextColor.GOLD);
            case RESET -> Component.text("World Default", NamedTextColor.GRAY);
        };
    }

    private NamedTextColor visibilityColor(PlayerVisibility visibility) {
        return switch (visibility) {
            case SHOW_ALL -> NamedTextColor.GREEN;
            case FRIENDS_ONLY -> NamedTextColor.YELLOW;
            case HIDE_ALL -> NamedTextColor.GRAY;
        };
    }

    /** Mirrors ItemRarity's ChatColor so the dropdown reads like an item name. */
    private NamedTextColor rarityColor(ItemRarity rarity) {
        return switch (rarity) {
            case UNCOMMON -> NamedTextColor.GREEN;
            case RARE -> NamedTextColor.BLUE;
            case EPIC -> NamedTextColor.LIGHT_PURPLE;
            case LEGENDARY -> NamedTextColor.GOLD;
            case MYTHIC -> NamedTextColor.RED;
            case FABLED -> NamedTextColor.AQUA;
            default -> NamedTextColor.GRAY;
        };
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

    private String formatEnum(String raw) {
        String lower = raw.toLowerCase(Locale.ROOT);
        return lower.substring(0, 1).toUpperCase(Locale.ROOT) + lower.substring(1);
    }

    private ClickCallback.Options oneUseCallback() {
        return ClickCallback.Options.builder()
                .uses(1)
                .lifetime(Duration.ofMinutes(10))
                .build();
    }
}
