package me.nakilex.levelplugin.hud.conditions;

import me.nakilex.levelplugin.hud.placeholders.HudPlaceholderService;

public class HudConditionContext {
    private final HudPlaceholderService placeholderService;

    public HudConditionContext(HudPlaceholderService placeholderService) {
        this.placeholderService = placeholderService;
    }

    public HudPlaceholderService getPlaceholderService() {
        return placeholderService;
    }
}
