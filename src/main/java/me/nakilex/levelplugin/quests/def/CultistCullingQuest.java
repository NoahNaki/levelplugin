package me.nakilex.levelplugin.quests.def;

import io.lumine.mythic.api.exceptions.InvalidMobTypeException;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.fakeblock.QuestGateManager;
import me.nakilex.levelplugin.quests.data.BeaconTargets;
import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.data.QuestObjective;
import me.nakilex.levelplugin.quests.data.QuestObjectiveType;
import me.nakilex.levelplugin.quests.data.QuestRewardCompat;
import me.nakilex.levelplugin.quests.data.QuestCompletionScript;
import me.nakilex.levelplugin.quests.data.QuestResetScript;
import me.nakilex.levelplugin.quests.data.QuestScript;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CultistCullingQuest extends Quest implements QuestScript, QuestCompletionScript, QuestResetScript {
    public static final String ID = "cultistculling";
    public static final int NPC_ID = 823;
    public static final int REQUIRED_LEVEL = 20;
    public static final String RITUAL_TARGET = "cultist_ritual";
    private static final int CONTACT_NPC_ID = 1510;
    private static final String CONTACT_TALK_TARGET = "npc" + CONTACT_NPC_ID + "_mystery";
    private static final String WORLD_NAME = "mmorpg";
    private static final String GATE_ID = "cultisthq";
    private static final double TRIGGER_RADIUS_SQ = 30 * 30;

    private static final List<String> INTRO_DIALOG = List.of(
            "Seras|Word is a cult has been stirring trouble well beyond these woods.",
            "Seras|You're not the same fresh-faced adventurer from before—think you can stamp out their rituals?",
            "Seras|Disrupt every circle you find, then seek out the contact waiting to debrief you."
    );

    private static final List<String> CONTACT_DIALOG = List.of(
            "???|You move quietly for someone who just ruined a handful of rituals.",
            "???|Piwan said you'd be thorough. Keep that up and the cult won't see us coming."
    );

    public static List<String> getContactDialog() {
        return CONTACT_DIALOG;
    }

    public static String getContactTalkTarget() {
        return CONTACT_TALK_TARGET;
    }

    public static int getContactNpcId() {
        return CONTACT_NPC_ID;
    }

    private static final List<RitualSite> RITUAL_SITES = List.of(
            new RitualSite("shadow_sorcerer", "Shadow_Sorcerer", new Location(world(), 262.5, 73, -364.5)),
            new RitualSite("tenebris", "hv_tenebris", new Location(world(), 176.5, 80, -629.5)),
            new RitualSite("gravekeeper", "Nocsy_FPV2-Gravekeeper", new Location(world(), 329.5, 73, 175.5)),
            new RitualSite("crowknight", "thecrowknight", new Location(world(), -329.5, 87, 36.5)),
            new RitualSite("piglinking", "LRD_PIGLINKING", new Location(world(), -1161.5, 66, -834.5))
    );

    private static CultistCullingQuest instance;
    private static boolean listenersRegistered;

    private final Map<UUID, Map<String, UUID>> activeSpawns = new HashMap<>();
    private final Map<UUID, ActiveRitual> mobOwners = new HashMap<>();

    public CultistCullingQuest() {
        super(
                ID,
                "Cultist Culling",
                "Halt the cult's scattered rituals and report back to a covert contact.",
                createObjectives(),
                REQUIRED_LEVEL,
                List.of(SerasQuest.ID),
                null,
                QuestRewardCompat.create(1200, 600, 0, List.of()),
                NPC_ID,
                INTRO_DIALOG,
                true
        );
        instance = this;
        ensureListeners();
    }

    private static List<QuestObjective> createObjectives() {
        return List.of(
                new QuestObjective(QuestObjectiveType.KILL, RITUAL_TARGET, RITUAL_SITES.size(), false, null,
                        "Disrupt cultist rituals"),
                new QuestObjective(QuestObjectiveType.TALK, CONTACT_TALK_TARGET, 1, false,
                        BeaconTargets.npc(CONTACT_NPC_ID),
                        "Report to the mysterious contact")
        );
    }

    public static void registerTalkTargets(QuestManager questManager) {
        questManager.registerTalkTarget(CONTACT_TALK_TARGET, "Mysterious Person", "Mysterious Person");
    }

    @Override
    public void onStart(Player player, Main plugin) {
        refreshGateState(player);
    }

    @Override
    public void onComplete(Player player, Main plugin) {
        clearTracking(player.getUniqueId());
        refreshGateState(player);
    }

    @Override
    public void onReset(Player player, Main plugin) {
        clearTracking(player.getUniqueId());
        QuestGateManager gates = plugin.getQuestGateManager();
        if (gates != null) {
            gates.closeGateInstant(player, GATE_ID);
        }
    }

    private static World world() {
        return Bukkit.getWorld(WORLD_NAME);
    }

    private static void ensureListeners() {
        if (listenersRegistered) {
            return;
        }
        listenersRegistered = true;
        Main plugin = Main.getInstance();
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onMove(PlayerMoveEvent event) {
                if (instance != null) {
                    instance.handleMove(event.getPlayer(), event.getTo());
                }
            }

            @EventHandler
            public void onDeath(EntityDeathEvent event) {
                if (instance != null) {
                    instance.handleDeath(event.getEntity());
                }
            }

            @EventHandler
            public void onMythicDeath(MythicMobDeathEvent event) {
                if (instance != null && event.getEntity() instanceof LivingEntity living) {
                    instance.handleDeath(living);
                }
            }

            @EventHandler
            public void onJoin(PlayerJoinEvent event) {
                if (instance != null) {
                    instance.refreshGateState(event.getPlayer());
                }
            }

            @EventHandler
            public void onQuit(PlayerQuitEvent event) {
                if (instance != null) {
                    instance.clearTracking(event.getPlayer().getUniqueId());
                }
            }
        }, plugin);
    }

    private void handleMove(Player player, Location to) {
        if (player == null || to == null || to.getWorld() == null) {
            return;
        }
        QuestManager questManager = Main.getInstance().getQuestManager();
        if (questManager == null) {
            return;
        }
        PlayerQuestProgress progress = questManager.getProgress(player.getUniqueId(), ID);
        if (progress == null) {
            return;
        }
        if (progress.getProgress(0) >= getObjectives().get(0).getAmount()) {
            prepareContact(player);
            refreshGateState(player);
            return;
        }

        for (RitualSite site : RITUAL_SITES) {
            if (questManager.hasFlag(player.getUniqueId(), ID, site.key())) {
                continue;
            }
            if (!site.isInRange(to)) {
                continue;
            }
            if (isActive(player.getUniqueId(), site.key())) {
                pruneIfInvalid(player.getUniqueId(), site.key());
                continue;
            }
            spawnRitual(player, site);
        }
    }

    private void handleDeath(LivingEntity entity) {
        if (entity == null) {
            return;
        }
        UUID mobId = entity.getUniqueId();
        ActiveRitual ritual = mobOwners.remove(mobId);
        if (ritual == null) {
            return;
        }
        clearActive(ritual.playerId(), ritual.siteKey());
        Player player = Bukkit.getPlayer(ritual.playerId());
        if (player == null || !player.isOnline()) {
            return;
        }
        if (entity.getKiller() == null || !player.getUniqueId().equals(entity.getKiller().getUniqueId())) {
            return;
        }

        QuestManager questManager = Main.getInstance().getQuestManager();
        if (questManager == null) {
            return;
        }
        questManager.setFlag(player.getUniqueId(), ID, ritual.siteKey());
        questManager.handleKill(player, RITUAL_TARGET, true);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS, "Cultist Ritual Halted.");

        if (ritualsCleared(questManager, player.getUniqueId())) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "A mysterious figure awaits your report.");
            prepareContact(player);
            refreshGateState(player);
        }
    }

    private void spawnRitual(Player player, RitualSite site) {
        Location loc = site.location();
        if (loc == null || loc.getWorld() == null) {
            return;
        }
        Entity mob;
        try {
            mob = MythicBukkit.inst().getAPIHelper().spawnMythicMob(site.mobId(), loc);
        } catch (InvalidMobTypeException ex) {
            Main.getInstance().getLogger().warning("Unable to spawn ritual mob '" + site.mobId() + "': " + ex.getMessage());
            return;
        }
        if (!(mob instanceof LivingEntity living)) {
            return;
        }
        UUID playerId = player.getUniqueId();
        UUID mobId = living.getUniqueId();
        activeSpawns.computeIfAbsent(playerId, k -> new HashMap<>()).put(site.key(), mobId);
        mobOwners.put(mobId, new ActiveRitual(playerId, site.key()));
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                "Dark energy gathers nearby. The cult begins a ritual...");
    }

    private boolean isActive(UUID playerId, String key) {
        Map<String, UUID> map = activeSpawns.get(playerId);
        return map != null && map.containsKey(key);
    }

    private void pruneIfInvalid(UUID playerId, String key) {
        Map<String, UUID> map = activeSpawns.get(playerId);
        if (map == null) {
            return;
        }
        UUID mobId = map.get(key);
        if (mobId == null) {
            map.remove(key);
            return;
        }
        Entity mob = Bukkit.getEntity(mobId);
        if (mob == null || mob.isDead()) {
            clearActive(playerId, key);
        }
    }

    private void clearActive(UUID playerId, String key) {
        Map<String, UUID> map = activeSpawns.get(playerId);
        if (map != null) {
            UUID mobId = map.remove(key);
            if (map.isEmpty()) {
                activeSpawns.remove(playerId);
            }
            if (mobId != null) {
                mobOwners.remove(mobId);
            }
        }
    }

    private void clearTracking(UUID playerId) {
        Map<String, UUID> map = activeSpawns.remove(playerId);
        if (map != null) {
            for (UUID mobId : map.values()) {
                mobOwners.remove(mobId);
            }
        }
    }

    private boolean ritualsCleared(QuestManager questManager, UUID playerId) {
        for (RitualSite site : RITUAL_SITES) {
            if (!questManager.hasFlag(playerId, ID, site.key())) {
                return false;
            }
        }
        return true;
    }

    private void prepareContact(Player player) {
        if (player == null) {
            return;
        }
        NPC npc = CitizensAPI.getNPCRegistry().getById(CONTACT_NPC_ID);
        if (npc == null || npc.getEntity() == null) {
            return;
        }
        Entity entity = npc.getEntity();
        if (entity instanceof LivingEntity living) {
            living.setCustomNameVisible(false);
            living.customName(Component.text("Mysterious Person", NamedTextColor.DARK_PURPLE));
        }
        npc.spawn(npc.getStoredLocation());
    }

    private void refreshGateState(Player player) {
        QuestGateManager gates = Main.getInstance().getQuestGateManager();
        if (gates == null || player == null) {
            return;
        }
        QuestManager questManager = Main.getInstance().getQuestManager();
        boolean unlocked = false;
        if (questManager != null) {
            PlayerQuestProgress progress = questManager.getProgress(player.getUniqueId(), ID);
            unlocked = questManager.hasCompleted(player.getUniqueId(), ID)
                    || (progress != null && progress.getProgress(0) >= getObjectives().get(0).getAmount());
        }
        if (unlocked) {
            gates.openGateInstant(player, GATE_ID);
            QuestManager qm = Main.getInstance().getQuestManager();
            if (qm != null && !qm.hasCompleted(player.getUniqueId(), ID)) {
                prepareContact(player);
            }
        } else {
            gates.closeGateInstant(player, GATE_ID);
        }
    }

    private record RitualSite(String key, String mobId, Location location) {
        boolean isInRange(Location loc) {
            if (loc == null || location == null) {
                return false;
            }
            if (location.getWorld() == null || !location.getWorld().equals(loc.getWorld())) {
                return false;
            }
            return location.distanceSquared(loc) <= TRIGGER_RADIUS_SQ;
        }
    }

    private record ActiveRitual(UUID playerId, String siteKey) {
    }
}
