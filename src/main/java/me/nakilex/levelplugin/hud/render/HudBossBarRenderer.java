package me.nakilex.levelplugin.hud.render;

import me.nakilex.levelplugin.hud.assets.HudAdvanceGlyphs;
import me.nakilex.levelplugin.hud.core.HudCanvas;
import me.nakilex.levelplugin.hud.core.HudResolvedElement;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class HudBossBarRenderer implements HudRenderer {
    private static final int PUA_START = 0xE000;
    private static final int PUA_END = 0xF8FF;

    private final int lines;
    private final int lineHeightPx;
    private final int canvasWidthPx;
    private final int canvasHeightPx;
    private final boolean mergeBossBar;
    private final Key hudFontKey;
    private final Map<String, TextColor> shaderColors;

    public HudBossBarRenderer(int lines, int lineHeightPx, int canvasWidthPx, int canvasHeightPx,
                              boolean mergeBossBar, Key hudFontKey, Map<String, TextColor> shaderColors) {
        this.lines = Math.max(1, lines);
        this.lineHeightPx = Math.max(1, lineHeightPx);
        this.canvasWidthPx = Math.max(1, canvasWidthPx);
        this.canvasHeightPx = Math.max(1, canvasHeightPx);
        this.mergeBossBar = mergeBossBar;
        this.hudFontKey = hudFontKey;
        this.shaderColors = shaderColors == null ? Map.of() : shaderColors;
    }

    @Override
    public HudRenderOutput render(HudCanvas canvas) {
        Map<Integer, List<HudResolvedElement>> byLine = new TreeMap<>();
        for (HudResolvedElement element : canvas.getElements()) {
            int lineIndex = mergeBossBar ? 0 : clampLine(computeLineIndex(element));
            byLine.computeIfAbsent(lineIndex, id -> new ArrayList<>()).add(element);
        }
        List<String> linesOut = new ArrayList<>();
        List<Component> componentsOut = new ArrayList<>();
        for (int line = 0; line < lines; line++) {
            int sourceLine = mergeBossBar ? 0 : line;
            List<HudResolvedElement> elements = byLine.getOrDefault(sourceLine, List.of());
            String lineText = composeLineText(elements);
            linesOut.add(lineText);
            componentsOut.add(composeComponent(elements));
        }
        return new HudRenderOutput(linesOut, componentsOut);
    }

    private int clampLine(int row) {
        if (lines <= 1) {
            return 0;
        }
        return Math.max(0, Math.min(lines - 1, row));
    }

    private int computeLineIndex(HudResolvedElement element) {
        int lineHeight = Math.max(1, lineHeightPx);
        return Math.floorDiv(element.getY(), lineHeight);
    }

    private String composeLineText(List<HudResolvedElement> elements) {
        if (elements.isEmpty()) {
            return "";
        }
        List<RenderOp> sorted = new ArrayList<>();
        for (int index = 0; index < elements.size(); index++) {
            sorted.add(new RenderOp(elements.get(index), index));
        }
        sorted.sort(Comparator.comparingInt((RenderOp op) -> op.element().getLayer())
                .thenComparingInt(RenderOp::order));
        StringBuilder builder = new StringBuilder();
        int currentPx = 0;
        int originShift = -(canvasWidthPx / 2);
        appendAdvance(builder, originShift);
        for (RenderOp op : sorted) {
            HudResolvedElement element = op.element();
            String text = element.getText();
            if (text == null || text.isBlank()) {
                continue;
            }
            int targetPx = alignedX(element);
            int deltaPx = targetPx - currentPx;
            appendAdvance(builder, deltaPx);
            builder.append(text);
            int widthPx = (int) Math.round(element.getWidth() * element.getScale());
            appendAdvance(builder, -(deltaPx + widthPx));
        }
        return builder.toString();
    }

    private Component composeComponent(List<HudResolvedElement> elements) {
        if (elements == null || elements.isEmpty()) {
            return Component.empty();
        }
        List<RenderOp> sorted = new ArrayList<>();
        for (int index = 0; index < elements.size(); index++) {
            sorted.add(new RenderOp(elements.get(index), index));
        }
        sorted.sort(Comparator.comparingInt((RenderOp op) -> op.element().getLayer())
                .thenComparingInt(RenderOp::order));
        TextComponent.Builder builder = Component.text();
        int currentPx = 0;
        int originShift = -(canvasWidthPx / 2);
        appendAdvanceComponent(builder, originShift);
        for (RenderOp op : sorted) {
            HudResolvedElement element = op.element();
            String text = element.getText();
            if (text == null || text.isBlank()) {
                continue;
            }
            int targetPx = alignedX(element);
            int deltaPx = targetPx - currentPx;
            appendAdvanceComponent(builder, deltaPx);
            builder.append(elementComponent(element));
            int widthPx = (int) Math.round(element.getWidth() * element.getScale());
            appendAdvanceComponent(builder, -(deltaPx + widthPx));
        }
        return builder.build();
    }

    private Component elementComponent(HudResolvedElement element) {
        Component piece = Component.text(element.getText());
        if (isGlyphText(element.getText()) && hudFontKey != null) {
            piece = piece.font(hudFontKey);
            TextColor color = shaderColors.get(element.getShaderKey());
            if (color != null) {
                piece = piece.color(color);
            }
        }
        return piece;
    }

    private boolean isGlyph(char c) {
        return c >= PUA_START && c <= PUA_END;
    }

    private boolean isGlyphText(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§') {
                i++;
                continue;
            }
            if (!isGlyph(c)) {
                return false;
            }
        }
        return true;
    }

    private int alignedX(HudResolvedElement element) {
        return (int) Math.round(element.getX() * element.getScale());
    }

    private int appendAdvance(StringBuilder builder, int deltaPx) {
        if (deltaPx == 0) {
            return 0;
        }
        int moved = 0;
        int remaining = deltaPx;
        while (remaining != 0) {
            int step = Math.min(HudAdvanceGlyphs.MAX_ADVANCE, Math.abs(remaining));
            if (remaining < 0) {
                step = -step;
            }
            builder.append(HudAdvanceGlyphs.codepointForAdvance(step));
            moved += step;
            remaining -= step;
        }
        return moved;
    }

    private int appendAdvanceComponent(TextComponent.Builder builder, int deltaPx) {
        if (deltaPx == 0) {
            return 0;
        }
        StringBuilder advances = new StringBuilder();
        int moved = appendAdvance(advances, deltaPx);
        Component piece = Component.text(advances.toString());
        if (hudFontKey != null) {
            piece = piece.font(hudFontKey);
        }
        builder.append(piece);
        return moved;
    }

    private record RenderOp(HudResolvedElement element, int order) {
    }
}
