package me.nakilex.levelplugin.hud.render;

import me.nakilex.levelplugin.hud.assets.HudAdvanceGlyphs;
import me.nakilex.levelplugin.hud.core.HudCanvas;
import me.nakilex.levelplugin.hud.core.HudResolvedElement;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

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

    public HudBossBarRenderer(int lines, int lineHeightPx, int canvasWidthPx, int canvasHeightPx,
                              boolean mergeBossBar, Key hudFontKey) {
        this.lines = Math.max(1, lines);
        this.lineHeightPx = Math.max(1, lineHeightPx);
        this.canvasWidthPx = Math.max(1, canvasWidthPx);
        this.canvasHeightPx = Math.max(1, canvasHeightPx);
        this.mergeBossBar = mergeBossBar;
        this.hudFontKey = hudFontKey;
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
            String lineText = composeLine(elements);
            linesOut.add(lineText);
            componentsOut.add(composeComponent(lineText));
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

    private String composeLine(List<HudResolvedElement> elements) {
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

    private Component composeComponent(String lineText) {
        if (lineText == null || lineText.isEmpty()) {
            return Component.empty();
        }
        TextComponent.Builder builder = Component.text();
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

    private void appendSegment(TextComponent.Builder builder, StringBuilder segment, boolean isGlyph) {
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

    private record RenderOp(HudResolvedElement element, int order) {
    }
}
