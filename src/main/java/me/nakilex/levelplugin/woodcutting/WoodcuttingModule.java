package me.nakilex.levelplugin.woodcutting;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.woodcutting.animation.BlockDisplayFactory;
import me.nakilex.levelplugin.woodcutting.animation.FallDirectionResolver;
import me.nakilex.levelplugin.woodcutting.animation.FallingTreeAnimator;
import me.nakilex.levelplugin.woodcutting.drop.TreeDropService;
import me.nakilex.levelplugin.woodcutting.protection.PlacedBlockTracker;
import me.nakilex.levelplugin.woodcutting.replant.ReplantService;
import me.nakilex.levelplugin.woodcutting.tool.AxeDamageService;
import me.nakilex.levelplugin.woodcutting.tool.AxeValidator;
import me.nakilex.levelplugin.woodcutting.tree.TreeDetector;
import me.nakilex.levelplugin.woodcutting.tree.TreeRootFinder;
import me.nakilex.levelplugin.woodcutting.tree.TreeTypeRegistry;
import me.nakilex.levelplugin.woodcutting.tree.TreeValidator;

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
        BlockDisplayFactory blockDisplayFactory = new BlockDisplayFactory();
        this.fallingTreeAnimator = new FallingTreeAnimator(plugin, config, new FallDirectionResolver());
        TreeDropService treeDropService = new TreeDropService();
        ReplantService replantService = new ReplantService(config);
        WoodcuttingService service = new WoodcuttingService(config, axeDamageService, blockDisplayFactory, fallingTreeAnimator, treeDropService, replantService);
        this.listener = new WoodcuttingListener(config, treeTypeRegistry, axeValidator, treeDetector, service);
    }

    public WoodcuttingConfig config() { return config; }
    public WoodcuttingListener listener() { return listener; }
    public PlacedBlockTracker placedBlockTracker() { return placedBlockTracker; }
    public void reload() { config.reload(); }
    public void shutdown() { fallingTreeAnimator.shutdown(); }
}
