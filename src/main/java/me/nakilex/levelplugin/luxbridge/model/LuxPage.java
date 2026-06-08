package me.nakilex.levelplugin.luxbridge.model;

import java.util.List;
import java.util.Map;

public record LuxPage(
        String id,
        List<String> lines,
        String typingInfoLine,
        String steadyInfoLine,
        String gotoPage,
        int timer,
        List<String> preActions,
        List<String> postActions,
        List<String> exitActions,
        Map<String, LuxAnswer> answers
) {
}
