package me.nakilex.levelplugin.spells.gui;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.ClassUtil;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.classes.managers.PlayerClassManager;
import me.nakilex.levelplugin.player.profile.ProfileManager;
import me.nakilex.levelplugin.settings.managers.SettingsManager;
import me.nakilex.levelplugin.spells.input.SpellInputHudManager;
import me.nakilex.levelplugin.spells.input.SpellInputMode;
import me.nakilex.levelplugin.spells.input.SpellInputType;
import me.nakilex.levelplugin.spells.input.SpellKeybindLayout;
import me.nakilex.levelplugin.spells.input.SpellKeybindManager;
import me.nakilex.levelplugin.spells.input.SpellKeybindSlot;
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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Native Minecraft dialog front-end for spell keybinds.
 *
 * Keybinds are edited against a pending profile and only written when the
 * player saves, exactly as the inventory editor did. Native dialogs cannot
 * re-render in place, so switching class or input mode reads the current form,
 * folds it into the pending state, and re-shows the dialog — edits made before
 * the switch survive it.
 */
@SuppressWarnings("UnstableApiUsage")
public final class SpellKeybindDialogService {

    private static final int BODY_WIDTH = 400;
    private static final int INPUT_WIDTH = 300;
    private static final int BUTTON_WIDTH = 150;

    private static final String UNBOUND = "UNBOUND";

    // Nexo glyphs from plugins/Nexo/glyphs/icons_pack.yml, in the nexo:default font.
    private static final Key NEXO_GLYPH_FONT = Key.key("nexo", "default");
    private static final String GLYPH_STAR = "\uA45D";   // star_icon
    private static final String GLYPH_MANA = "\uA456";   // mana_icon
    private static final String GLYPH_CLASS = "\uA432";  // class_icon
    private static final String GLYPH_KEY = "\uA45A";    // key_icon

    private static final List<SpellInputType> BINDABLE_SPELLS = List.of(
            SpellInputType.SPELL_1,
            SpellInputType.SPELL_2,
            SpellInputType.SPELL_3,
            SpellInputType.SPELL_4
    );

    private final JavaPlugin plugin;
    private final SettingsManager settingsManager;
    private final SpellKeybindManager keybindManager = SpellKeybindManager.getInstance();
    private final Consumer<Player> onReturn;

    /** Pending, unsaved edits per player, dropped on save or cancel. */
    private final Map<UUID, EditState> states = new HashMap<>();

    public SpellKeybindDialogService(JavaPlugin plugin, SettingsManager settingsManager, Consumer<Player> onReturn) {
        this.plugin = plugin;
        this.settingsManager = settingsManager;
        this.onReturn = onReturn;
    }

    public void open(Player player) {
        if (player == null) {
            return;
        }
        states.remove(player.getUniqueId());
        show(player);
    }

    private void show(Player player) {
        EditState state = states.computeIfAbsent(player.getUniqueId(), id -> createState(player));
        boolean archerFamily = ClassUtil.isArcherFamily(state.viewClass);
        EnumMap<SpellKeybindSlot, SpellInputType> bindings = state.bindings(player.getUniqueId());

        List<DialogInput> inputs = new ArrayList<>();
        for (SpellKeybindSlot slot : SpellKeybindSlot.values()) {
            inputs.add(bindingInput(state.viewMode, slot, bindings.get(slot), archerFamily));
        }

        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(Component.text()
                .append(iconOnly(GLYPH_CLASS)).append(Component.space())
                .append(Component.text("Class: ", NamedTextColor.WHITE))
                .append(Component.text(state.viewClass.getDisplayName(), NamedTextColor.AQUA))
                .append(Component.text("    "))
                .append(iconOnly(GLYPH_MANA)).append(Component.space())
                .append(Component.text("Input mode: ", NamedTextColor.WHITE))
                .append(Component.text(state.viewMode.getDisplayName(), NamedTextColor.AQUA))
                .build(), BODY_WIDTH));
        body.add(text("Each control is one input. Pick which spell it casts, or Unbound to clear it. "
                + "Keybinds are saved per class and per input mode.", NamedTextColor.WHITE));

        List<String> issues = validate(bindings);
        if (!issues.isEmpty()) {
            body.add(text(String.join("  ", issues), NamedTextColor.RED));
        }

        List<PlayerClass> classes = unlockedClasses(player);

        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(button(GLYPH_STAR, NamedTextColor.GREEN, "Save", "Write these keybinds to your profile",
                (values, clicked) -> {
                    apply(clicked, values);
                    save(clicked);
                    states.remove(clicked.getUniqueId());
                    onReturn.accept(clicked);
                }, inputs));
        buttons.add(button(GLYPH_MANA, NamedTextColor.AQUA, "Switch Input Mode", "Edit the other input mode; current edits are kept",
                (values, clicked) -> {
                    apply(clicked, values);
                    states.get(clicked.getUniqueId()).viewMode = state.viewMode.next();
                    show(clicked);
                }, inputs));
        if (classes.size() > 1) {
            buttons.add(button(GLYPH_CLASS, NamedTextColor.AQUA, "Switch Class", "Edit another unlocked class; current edits are kept",
                    (values, clicked) -> {
                        apply(clicked, values);
                        EditState current = states.get(clicked.getUniqueId());
                        int idx = classes.indexOf(current.viewClass);
                        current.viewClass = classes.get(((idx < 0 ? 0 : idx) + 1) % classes.size());
                        show(clicked);
                    }, inputs));
        }
        buttons.add(button(null, NamedTextColor.RED, "Cancel", "Discard every unsaved keybind change",
                (values, clicked) -> {
                    states.remove(clicked.getUniqueId());
                    onReturn.accept(clicked);
                }, inputs));

        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(iconText(GLYPH_KEY, "Spell Keybinds", NamedTextColor.WHITE))
                        .body(body)
                        .inputs(inputs)
                        // Do not let the client close the screen between dialogs:
                        // going through "no screen" is what re-centres the mouse,
                        // and Switch Class / Switch Input Mode re-show this screen
                        // constantly. Replacing it in place keeps the pointer put.
                        // pause must be off to go with it — Paper rejects a
                        // pausing dialog whose after-action never unpauses.
                        .afterAction(DialogBase.DialogAfterAction.NONE)
                        .pause(false)
                        .build())
                .type(DialogType.multiAction(buttons).columns(2).build())
        );

        player.showDialog(dialog);
    }

    // ------------------------------------------------------------ inputs

    private DialogInput bindingInput(SpellInputMode mode, SpellKeybindSlot slot, SpellInputType bound,
                                     boolean archerFamily) {
        String sequence = mode == SpellInputMode.MOUSE_COMBO
                ? SpellKeybindLayout.comboSequenceForSlot(archerFamily, slot)
                : SpellKeybindLayout.keyboardSequenceForSlot(archerFamily, slot);

        // Same read as the inventory editor gave: unbound is a problem, bound is fine.
        List<SingleOptionDialogInput.OptionEntry> entries = new ArrayList<>();
        entries.add(SingleOptionDialogInput.OptionEntry.create(UNBOUND,
                Component.text("Unbound", NamedTextColor.RED), bound == null));
        for (SpellInputType spell : BINDABLE_SPELLS) {
            entries.add(SingleOptionDialogInput.OptionEntry.create(spell.name(),
                    Component.text(SpellKeybindLayout.spellDisplayName(spell), NamedTextColor.GREEN),
                    spell == bound));
        }

        return DialogInput.singleOption(slot.name(), iconText(GLYPH_MANA, sequence, NamedTextColor.AQUA), entries)
                .width(INPUT_WIDTH)
                .build();
    }

    private ActionButton button(String glyph, NamedTextColor color, String label, String tooltip,
                                java.util.function.BiConsumer<Map<String, String>, Player> action,
                                List<DialogInput> inputs) {
        return ActionButton.builder(glyph == null
                        ? Component.text(label, color)
                        : iconText(glyph, label, color))
                .tooltip(Component.text(tooltip))
                .width(BUTTON_WIDTH)
                .action(DialogAction.customClick(
                        (response, audience) -> {
                            if (!(audience instanceof Player clicked)) return;
                            // Read the response inside the callback; it does not
                            // run on the main thread, so apply the values there.
                            Map<String, String> values = read(response, inputs);
                            Bukkit.getScheduler().runTask(plugin, () -> action.accept(values, clicked));
                        },
                        oneUseCallback()
                ))
                .build();
    }

    private Map<String, String> read(DialogResponseView response, List<DialogInput> inputs) {
        Map<String, String> values = new HashMap<>();
        for (DialogInput input : inputs) {
            // singleOption reports the selected option id through getText.
            values.put(input.key(), response.getText(input.key()));
        }
        return values;
    }

    // ------------------------------------------------------------- state

    /** Folds a submitted form into the pending bindings for the viewed class/mode. */
    private void apply(Player player, Map<String, String> values) {
        EditState state = states.get(player.getUniqueId());
        if (state == null) {
            return;
        }
        EnumMap<SpellKeybindSlot, SpellInputType> bindings = state.bindings(player.getUniqueId());
        for (SpellKeybindSlot slot : SpellKeybindSlot.values()) {
            String id = values.get(slot.name());
            if (id == null) {
                continue;
            }
            if (UNBOUND.equals(id)) {
                bindings.remove(slot);
                continue;
            }
            for (SpellInputType spell : BINDABLE_SPELLS) {
                if (spell.name().equals(id)) {
                    bindings.put(slot, spell);
                    break;
                }
            }
        }
    }

    private void save(Player player) {
        EditState state = states.get(player.getUniqueId());
        if (state == null) {
            return;
        }
        UUID playerId = player.getUniqueId();

        for (Map.Entry<PlayerClass, Map<SpellInputMode, EnumMap<SpellKeybindSlot, SpellInputType>>> entry
                : state.profiles.entrySet()) {
            for (Map.Entry<SpellInputMode, EnumMap<SpellKeybindSlot, SpellInputType>> modeEntry
                    : entry.getValue().entrySet()) {
                keybindManager.setBindings(playerId, entry.getKey(), modeEntry.getKey(), modeEntry.getValue());
            }
        }

        settingsManager.getSettings(player).setSpellInputMode(state.viewMode);

        Integer slot = ProfileManager.getInstance().getActiveSlot(playerId);
        if (slot != null && slot >= 0) {
            settingsManager.saveProfileSettings(playerId, slot);
            keybindManager.saveProfileBindings(playerId, slot);
        } else {
            Bukkit.getLogger().warning("[LevelPlugin][SpellKeybindDialog] Could not persist keybind/profile settings for "
                    + player.getName() + " because no active profile slot is set.");
        }

        Main.getInstance().getPlayerConfig().saveConfigFile();
        SpellInputHudManager.getInstance().sync(player);
        ChatMessageUtil.send(player, MessageType.SUCCESS, "Spell keybinds and input mode saved.");
    }

    private EditState createState(Player player) {
        EditState state = new EditState();
        state.viewClass = PlayerClassManager.getInstance().getPlayerClass(player);
        state.viewMode = settingsManager.getSettings(player).getSpellInputMode();
        return state;
    }

    private List<String> validate(EnumMap<SpellKeybindSlot, SpellInputType> bindings) {
        List<String> issues = new ArrayList<>();
        Map<SpellInputType, Integer> counts = new EnumMap<>(SpellInputType.class);
        int unbound = 0;
        for (SpellKeybindSlot slot : SpellKeybindSlot.values()) {
            SpellInputType bound = bindings.get(slot);
            if (bound == null) {
                unbound++;
            } else {
                counts.merge(bound, 1, Integer::sum);
            }
        }
        if (unbound > 0) {
            issues.add(unbound + (unbound == 1 ? " input is unbound." : " inputs are unbound."));
        }
        List<String> duplicates = new ArrayList<>();
        for (Map.Entry<SpellInputType, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > 1) {
                duplicates.add(SpellKeybindLayout.spellDisplayName(entry.getKey()));
            }
        }
        if (!duplicates.isEmpty()) {
            issues.add("Bound more than once: " + String.join(", ", duplicates) + ".");
        }
        return issues;
    }

    private List<PlayerClass> unlockedClasses(Player player) {
        StatsManager.PlayerStats stats = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        Set<PlayerClass> unlocked = stats.unlockedClasses == null ? Set.of() : stats.unlockedClasses;
        List<PlayerClass> ordered = new ArrayList<>();
        for (PlayerClass playerClass : PlayerClass.values()) {
            if (unlocked.contains(playerClass)) {
                ordered.add(playerClass);
            }
        }
        if (ordered.isEmpty()) {
            ordered.add(PlayerClassManager.getInstance().getPlayerClass(player));
        }
        return ordered;
    }

    private DialogBody text(String value, NamedTextColor color) {
        return DialogBody.plainMessage(Component.text(value, color), BODY_WIDTH);
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

    private ClickCallback.Options oneUseCallback() {
        return ClickCallback.Options.builder()
                .uses(1)
                .lifetime(Duration.ofMinutes(10))
                .build();
    }

    private final class EditState {
        private PlayerClass viewClass;
        private SpellInputMode viewMode;
        private final Map<PlayerClass, Map<SpellInputMode, EnumMap<SpellKeybindSlot, SpellInputType>>> profiles =
                new EnumMap<>(PlayerClass.class);

        EnumMap<SpellKeybindSlot, SpellInputType> bindings(UUID playerId) {
            return profiles
                    .computeIfAbsent(viewClass, cls -> {
                        Map<SpellInputMode, EnumMap<SpellKeybindSlot, SpellInputType>> profile =
                                new EnumMap<>(SpellInputMode.class);
                        for (SpellInputMode mode : SpellInputMode.values()) {
                            profile.put(mode, keybindManager.getBindings(playerId, cls, mode));
                        }
                        return profile;
                    })
                    .get(viewMode);
        }
    }
}
