package me.nakilex.levelplugin.hud.render;

import me.nakilex.levelplugin.hud.core.HudCanvas;
import me.nakilex.levelplugin.hud.core.HudResolvedElement;
import me.nakilex.levelplugin.hud.core.HudTextAlign;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.utils.DefaultFontInfo;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class HudBossBarRenderer implements HudRenderer {
    private static final int SPACE_WIDTH = DefaultFontInfo.SPACE.getLength() + 1;
    private static final int PUA_START = 0xE000;
    private static final int PUA_END = 0xF8FF;

    private final int lines;
    private final int lineHeightPx;
    private final int canvasWidthPx;
    private final boolean mergeBossBar;
    private final Key hudFontKey;

    public HudBossBarRenderer(int lines, int lineHeightPx, int canvasWidthPx, boolean mergeBossBar, Key hudFontKey) {
        this.lines = Math.max(1, lines);
        this.lineHeightPx = Math.max(1, lineHeightPx);
        this.canvasWidthPx = Math.max(1, canvasWidthPx);
        this.mergeBossBar = mergeBossBar;
        this.hudFontKey = hudFontKey;
    }

    @Override
    public HudRenderOutput render(HudCanvas canvas) {
        Map<Integer, List<HudResolvedElement>> byLine = new TreeMap<>();
        for (HudResolvedElement element : canvas.getElements()) {
            int lineIndex = mergeBossBar ? 0 : mapLine(element.getY());
            byLine.computeIfAbsent(lineIndex, id -> new ArrayList<>()).add(element);
        }
        List<String> linesOut = new ArrayList<>();
        List<Component> componentsOut = new ArrayList<>();
        for (int line = 0; line < lines; line++) {
            List<HudResolvedElement> elements = byLine.getOrDefault(line, List.of());
            String lineText = composeLine(elements);
            linesOut.add(lineText);
            componentsOut.add(composeComponent(lineText));
        }
        return new HudRenderOutput(linesOut, componentsOut);
    }

    private int mapLine(int y) {
        int lineIndex = (int) Math.floor((double) y / lineHeightPx);
        if (lineIndex < 0) {
            return 0;
        }
        return Math.min(lines - 1, lineIndex);
    }

    private String composeLine(List<HudResolvedElement> elements) {
        if (elements.isEmpty()) {
            return "";
        }
        List<HudResolvedElement> sorted = new ArrayList<>(elements);
        sorted.sort(Comparator.comparingInt(HudResolvedElement::getLayer)
                .thenComparingInt(HudResolvedElement::getX));
        StringBuilder builder = new StringBuilder();
        int currentPx = 0;
        for (HudResolvedElement element : sorted) {
            String text = element.getText();
            if (text == null || text.isBlank()) {
                continue;
            }
            int targetPx = alignedX(element);
            while (currentPx + SPACE_WIDTH <= targetPx) {
                builder.append(' ');
                currentPx += SPACE_WIDTH;
            }
            builder.append(text);
            currentPx += (int) Math.round(ChatFormatter.pixelLength(text) * element.getScale());
        }
        return builder.toString();
    }

    private Component composeComponent(String lineText) {
        if (lineText == null || lineText.isEmpty()) {
            return Component.empty();
        }
        Component.Builder builder = Component.text();
        StringBuilder segment = new StringBuilder();
        boolean segmentIsGlyph = isGlyph(lineText.charAt(0));
        for (int i = 0; i < lineText.length(); i++) {
            char c = lineText.charAt(i);
            boolean isGlyph = isGlyph(c);
            if (isGlyph != segmentIsGlyph) {
                appendSegment(builder, segment, segmentIsGlyph);
                segment.setLength(0);
                segmentIsGlyph = isGlyph;
            }
            segment.append(c);
        }
        appendSegment(builder, segment, segmentIsGlyph);
        return builder.build();
    }

    private void appendSegment(Component.Builder builder, StringBuilder segment, boolean isGlyph) {
        if (segment.isEmpty()) {
            return;
        }
        Component piece = Component.text(segment.toString());
        if (isGlyph && hudFontKey != null) {
            piece = piece.font(hudFontKey);
        }
        builder.append(piece);
    }

    private boolean isGlyph(char c) {
        return c >= PUA_START && c <= PUA_END;
    }

    private int alignedX(HudResolvedElement element) {
        int base = switch (element.getAlign()) {
            case CENTER -> canvasWidthPx / 2;
            case RIGHT -> canvasWidthPx;
            case LEFT -> 0;
        };
        return (int) Math.round(base + element.getX() * element.getScale());
    }
}
