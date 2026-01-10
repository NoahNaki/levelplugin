package me.nakilex.levelplugin.hud.render;

import me.nakilex.levelplugin.hud.assets.HudAdvanceGlyphs;
import me.nakilex.levelplugin.hud.core.HudCanvas;
import me.nakilex.levelplugin.hud.core.HudResolvedElement;
import me.nakilex.levelplugin.hud.core.HudTextAlign;
import me.nakilex.levelplugin.utils.DefaultFontInfo;
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
    private final boolean mergeBossBar;
    private final Key hudFontKey;
    private final Map<Character, Integer> glyphWidths;

    public HudBossBarRenderer(int lines, int lineHeightPx, int canvasWidthPx, boolean mergeBossBar, Key hudFontKey,
                              Map<Character, Integer> glyphWidths) {
        this.lines = Math.max(1, lines);
        this.lineHeightPx = Math.max(1, lineHeightPx);
        this.canvasWidthPx = Math.max(1, canvasWidthPx);
        this.mergeBossBar = mergeBossBar;
        this.hudFontKey = hudFontKey;
        this.glyphWidths = glyphWidths == null ? Map.of() : Map.copyOf(glyphWidths);
    }

    @Override
    public HudRenderOutput render(HudCanvas canvas) {
        Map<Integer, List<HudResolvedElement>> byLine = new TreeMap<>();
        int minLine = Integer.MAX_VALUE;
        for (HudResolvedElement element : canvas.getElements()) {
            int lineIndex = mergeBossBar ? 0 : rawLineIndex(element.getY());
            minLine = Math.min(minLine, lineIndex);
            byLine.computeIfAbsent(lineIndex, id -> new ArrayList<>()).add(element);
        }
        int lineOffset = minLine == Integer.MAX_VALUE ? 0 : Math.max(0, -minLine);
        List<String> linesOut = new ArrayList<>();
        List<Component> componentsOut = new ArrayList<>();
        for (int line = 0; line < lines; line++) {
            int sourceLine = mergeBossBar ? 0 : line - lineOffset;
            List<HudResolvedElement> elements = byLine.getOrDefault(sourceLine, List.of());
            String lineText = composeLine(elements);
            linesOut.add(lineText);
            componentsOut.add(composeComponent(lineText));
        }
        return new HudRenderOutput(linesOut, componentsOut);
    }

    private int rawLineIndex(int y) {
        return (int) Math.floor((double) y / lineHeightPx);
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
            int deltaPx = targetPx - currentPx;
            currentPx += appendAdvance(builder, deltaPx);
            builder.append(text);
            currentPx += (int) Math.round(pixelLength(text) * element.getScale());
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
        int base = switch (element.getAlign()) {
            case CENTER -> canvasWidthPx / 2;
            case RIGHT -> canvasWidthPx;
            case LEFT -> 0;
        };
        return (int) Math.round(base + element.getX() * element.getScale());
    }

    private int pixelLength(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int px = 0;
        boolean previousCode = false;
        boolean bold = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§') {
                previousCode = true;
                continue;
            }
            if (previousCode) {
                previousCode = false;
                bold = c == 'l' || c == 'L';
                continue;
            }
            if (isGlyph(c)) {
                int glyphWidth = glyphWidths.getOrDefault(c, DefaultFontInfo.SPACE.getLength());
                px += glyphWidth + 1;
                continue;
            }
            DefaultFontInfo dFI = DefaultFontInfo.getDefaultFontInfo(c);
            px += (bold ? DefaultFontInfo.getBoldLength() : dFI.getLength()) + 1;
        }
        return px;
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
}
