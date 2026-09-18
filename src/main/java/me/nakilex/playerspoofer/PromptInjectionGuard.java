package me.nakilex.playerspoofer;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Small local guard for obvious attempts to turn normal Minecraft chat into model-control text.
 * This is deliberately conservative: normal gameplay questions should continue through, while
 * prompt/system/API/meta requests are simply not added to the LLM transcript and receive no AI reply.
 */
final class PromptInjectionGuard {
    private static final List<Pattern> META_PATTERNS = List.of(
            Pattern.compile("\\b(ignore|forget|disregard|override)\\b.{0,40}\\b(previous|prior|above|system|developer|instructions?|rules?|prompt)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(system|developer)\\s+(prompt|message|instructions?|rules?)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(what|show|tell|reveal|print|repeat|leak|give)\\b.{0,45}\\b(your|the)\\s+(instructions?|system prompt|developer message|prompt|rules?)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(api|openrouter|ollama)\\s*[-_ ]?key\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(jailbreak|prompt injection|developer mode|do anything now|DAN)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(you are|act as|pretend to be)\\b.{0,24}\\b(chatgpt|an ai|a language model|system|developer)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(are you|r u)\\b.{0,12}\\b(ai|chatgpt|a bot|language model)\\b", Pattern.CASE_INSENSITIVE)
    );

    private PromptInjectionGuard() {}

    static boolean shouldIgnore(String raw) {
        if (raw == null || raw.isBlank()) return false;
        String text = raw.toLowerCase(Locale.ROOT);
        for (Pattern pattern : META_PATTERNS) {
            if (pattern.matcher(text).find()) return true;
        }
        return false;
    }

    static String sanitizeTranscriptLine(String raw, int maxChars) {
        if (raw == null) return "";
        String text = raw.replace('§', ' ')
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('<', '‹')
                .replace('>', '›')
                .replaceAll("\\s+", " ")
                .trim();
        if (shouldIgnore(text)) return "[ignored meta/prompt-injection attempt]";
        int cap = Math.max(40, maxChars);
        return text.length() <= cap ? text : text.substring(0, cap);
    }
}
