package me.nakilex.levelplugin.player.woodcutting;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.woodcutting.animation.BlockDisplayFactory;
import me.nakilex.levelplugin.player.woodcutting.animation.FallDirectionResolver;
import me.nakilex.levelplugin.player.woodcutting.animation.FallingTreeAnimator;
import me.nakilex.levelplugin.player.woodcutting.drop.TreeDropService;
import me.nakilex.levelplugin.player.woodcutting.protection.PlacedBlockTracker;
import me.nakilex.levelplugin.player.woodcutting.replant.ReplantService;
import me.nakilex.levelplugin.player.woodcutting.tool.AxeDamageService;
import me.nakilex.levelplugin.player.woodcutting.tool.AxeValidator;
import me.nakilex.levelplugin.player.woodcutting.tree.TreeDetector;
import me.nakilex.levelplugin.player.woodcutting.tree.TreeRootFinder;
import me.nakilex.levelplugin.player.woodcutting.tree.TreeTypeRegistry;
import me.nakilex.levelplugin.player.woodcutting.tree.TreeValidator;

public class WoodcuttingModule {
    private final WoodcuttingConfig config;
    private final TreeTypeRegistry treeTypeRegistry;
    private final PlacedBlockTracker placedBlockTracker;
    private final FallingTreeAnimator fallingTreeAnimator;
    private final WoodcuttingListener listener;

    public WoodcuttingModule(Main plugin) {
        this.config = new WoodcuttingConfig(plugin);
        this.treeTypeRegistry = new TreeTypeRegistry(config);
        this.placedBlockTracker = new PlacedBlockTracker(treeTypeRegistry);
        TreeValidator treeValidator = new TreeValidator(config, placedBlockTracker);
        TreeDetector treeDetector = new TreeDetector(config, treeTypeRegistry, new TreeRootFinder(), treeValidator);
        AxeValidator axeValidator = new AxeValidator(config);
        AxeDamageService axeDamageService = new AxeDamageService(config);
        BlockDisplayFactory blockDisplayFactory = new BlockDisplayFactory(config);
        this.fallingTreeAnimator = new FallingTreeAnimator(plugin, config, new FallDirectionResolver());
        TreeDropService treeDropService = new TreeDropService();
        ReplantService replantService = new ReplantService(config);
        WoodcuttingService service = new WoodcuttingService(config, axeDamageService, blockDisplayFactory, fallingTreeAnimator, treeDropService, replantService);
        this.listener = new WoodcuttingListener(plugin, config, treeTypeRegistry, axeValidator, treeDetector, service);
        plugin.getLogger().info("[Woodcutting] Module initialized");
    }

    public WoodcuttingConfig config() { return config; }
    public WoodcuttingListener listener() { return listener; }
    public PlacedBlockTracker placedBlockTracker() { return placedBlockTracker; }
    public void reload() { config.reload(); }
    public void shutdown() { fallingTreeAnimator.shutdown(); }
}
