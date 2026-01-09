package me.nakilex.levelplugin.hud.render;

import me.nakilex.levelplugin.hud.core.HudElement;
import me.nakilex.levelplugin.hud.core.HudLayout;
import me.nakilex.levelplugin.hud.placeholders.HudPlaceholderService;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.utils.DefaultFontInfo;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HudRenderer {
    private static final int SPACE_WIDTH = DefaultFontInfo.SPACE.getLength() + 1;

    public String render(Player player, HudLayout layout, HudPlaceholderService placeholderService) {
        if (layout == null) {
            return "";
        }
        List<RenderedElement> renderables = new ArrayList<>();
        for (HudElement element : layout.getElements()) {
            if (!element.shouldRender(player)) {
                continue;
            }
            String resolved = placeholderService.resolve(player, element.getText());
            if (resolved.isBlank()) {
                continue;
            }
            renderables.add(new RenderedElement(element.getX(), element.getY(), element.getLayer(), resolved, element.getId()));
        }
        if (renderables.isEmpty()) {
            return "";
        }
        renderables.sort(Comparator.comparingInt(RenderedElement::y)
                .thenComparingInt(RenderedElement::layer)
                .thenComparingInt(RenderedElement::x));
        StringBuilder builder = new StringBuilder();
        int currentPx = 0;
        for (RenderedElement element : renderables) {
            if (element.y() != 0) {
                continue;
            }
            int targetPx = Math.max(0, element.x());
            while (currentPx + SPACE_WIDTH <= targetPx) {
                builder.append(' ');
                currentPx += SPACE_WIDTH;
            }
            builder.append(element.text());
            currentPx += ChatFormatter.pixelLength(element.text());
        }
        return builder.toString();
    }

    public List<String> describe(Player player, HudLayout layout, HudPlaceholderService placeholderService) {
        List<String> lines = new ArrayList<>();
        if (layout == null) {
            return lines;
        }
        for (HudElement element : layout.getElements()) {
            String resolved = element.shouldRender(player)
                    ? placeholderService.resolve(player, element.getText())
                    : "(hidden)";
            lines.add(element.getId() + " [" + element.getType() + "] x=" + element.getX()
                    + " y=" + element.getY() + " layer=" + element.getLayer()
                    + " -> " + resolved);
        }
        return lines;
    }

    private record RenderedElement(int x, int y, int layer, String text, String id) {
    }
}
