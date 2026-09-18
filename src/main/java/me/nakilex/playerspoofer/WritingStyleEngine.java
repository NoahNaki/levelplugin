package me.nakilex.playerspoofer;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

final class WritingStyleEngine {
    private WritingStyleEngine() {}

    static WritingStyle styleFor(FakePlayer bot) {
        int bucket = Math.floorMod(bot.name().toLowerCase(Locale.ROOT).hashCode(), 100);
        if (bucket < 15) return WritingStyle.CLEAN;
        if (bucket < 37) return WritingStyle.CASUAL;
        if (bucket < 55) return WritingStyle.BARE;
        if (bucket < 73) return WritingStyle.ABBREVIATED;
        if (bucket < 83) return WritingStyle.POLITE;
        if (bucket < 93) return WritingStyle.EXPRESSIVE;
        return WritingStyle.MIXED;
    }

    static String description(FakePlayer bot) {
        return switch (styleFor(bot)) {
            case CLEAN -> "writes with normal capitalization and punctuation, rarely abbreviates, still sounds casual";
            case CASUAL -> "usually writes lowercase, uses light punctuation and occasional slang";
            case BARE -> "writes short lowercase messages with almost no punctuation";
            case ABBREVIATED -> "writes lowercase and naturally uses abbreviations like u, ur, rn, tbh, btw, wdym, idk and pls";
            case POLITE -> "writes clearly, uses punctuation, and is more likely to say please/thanks instead of sounding blunt";
            case EXPRESSIVE -> "writes casually and expressively, sometimes uses bro/bruh or an exclamation mark, but does not add random laughter filler";
            case MIXED -> "has inconsistent capitalization and punctuation, sometimes clean and sometimes very casual";
        };
    }

    static String apply(FakePlayer bot, String input) {
        if (input == null) return "";
        String text = input.trim().replaceAll("\\s+", " ");
        if (text.isBlank()) return text;

        WritingStyle style = styleFor(bot);
        boolean question = looksLikeQuestion(text);
        return switch (style) {
            case CLEAN -> clean(text, question);
            case CASUAL -> casual(text, question);
            case BARE -> bare(text);
            case ABBREVIATED -> abbreviated(text, question);
            case POLITE -> polite(bot, text, question);
            case EXPRESSIVE -> expressive(text, question);
            case MIXED -> mixed(text, question);
        };
    }

    private static String clean(String text, boolean question) {
        text = normalizeCommon(text);
        text = sentenceCase(text);
        text = stripTerminalPunctuation(text);
        if (question) return text + "?";
        return wordCount(text) >= 5 ? text + "." : text;
    }

    private static String casual(String text, boolean question) {
        text = normalizeCommon(text).toLowerCase(Locale.ROOT);
        text = stripTerminalPunctuation(text);
        if (ThreadLocalRandom.current().nextDouble() < 0.30D) text = replaceCasualPronouns(text);
        if (question && ThreadLocalRandom.current().nextDouble() < 0.70D) text += "?";
        return text;
    }

    private static String bare(String text) {
        text = normalizeCommon(text).toLowerCase(Locale.ROOT);
        text = replaceCasualPronouns(text);
        return stripTerminalPunctuation(text);
    }

    private static String abbreviated(String text, boolean question) {
        text = normalizeCommon(text).toLowerCase(Locale.ROOT);
        text = text.replaceAll("\\bwhat do you mean\\b", "wdym")
                .replaceAll("\\bto be honest\\b", "tbh")
                .replaceAll("\\bby the way\\b", "btw")
                .replaceAll("\\bright now\\b", "rn")
                .replaceAll("\\bi do not know\\b", "idk")
                .replaceAll("\\bi don't know\\b", "idk")
                .replaceAll("\\bplease\\b", "pls")
                .replaceAll("\\bbecause\\b", "bc");
        text = replaceCasualPronouns(text);
        text = stripTerminalPunctuation(text);
        if (question && ThreadLocalRandom.current().nextDouble() < 0.45D) text += "?";
        return text;
    }

    private static String polite(FakePlayer bot, String text, boolean question) {
        text = normalizeCommon(text);
        text = sentenceCase(stripTerminalPunctuation(text));
        if (question && !containsPlease(text) && ThreadLocalRandom.current().nextDouble() < 0.45D) {
            String token = Math.floorMod(bot.name().hashCode(), 2) == 0 ? "please" : "pls";
            text = text + " " + token;
        }
        if (question) return text + "?";
        return wordCount(text) >= 4 ? text + "." : text;
    }

    private static String expressive(String text, boolean question) {
        text = normalizeCommon(text).toLowerCase(Locale.ROOT);
        text = stripTerminalPunctuation(text);
        if (question) text += ThreadLocalRandom.current().nextDouble() < 0.78D ? "?" : "";
        else if (ThreadLocalRandom.current().nextDouble() < 0.16D) text += "!";
        return text;
    }

    private static String mixed(String text, boolean question) {
        text = normalizeCommon(text);
        if (ThreadLocalRandom.current().nextBoolean()) text = text.toLowerCase(Locale.ROOT);
        else text = sentenceCase(text);
        text = stripTerminalPunctuation(text);
        if (question && ThreadLocalRandom.current().nextDouble() < 0.75D) text += "?";
        else if (!question && wordCount(text) >= 6 && ThreadLocalRandom.current().nextDouble() < 0.35D) text += ".";
        return text;
    }

    private static String normalizeCommon(String text) {
        return text.replaceAll("(?i)\\bim\\b", "I'm")
                .replaceAll("(?i)\\bdont\\b", "don't")
                .replaceAll("(?i)\\bdoesnt\\b", "doesn't")
                .replaceAll("(?i)\\bisnt\\b", "isn't")
                .replaceAll("(?i)\\bcant\\b", "can't")
                .replaceAll("(?i)\\bwont\\b", "won't")
                .replaceAll("(?i)\\bwhats\\b", "what's")
                .replaceAll("(?i)\\bthats\\b", "that's")
                .replaceAll("(?i)\\byoure\\b", "you're")
                .replaceAll("(?i)\\bive\\b", "I've")
                .replaceAll("(?i)\\bill\\b", "I'll");
    }

    private static String replaceCasualPronouns(String text) {
        return text.replaceAll("\\bare you\\b", "r u")
                .replaceAll("\\byou're\\b", "ur")
                .replaceAll("\\byour\\b", "ur")
                .replaceAll("\\byou\\b", "u");
    }

    private static String sentenceCase(String text) {
        if (text.isBlank()) return text;
        char first = text.charAt(0);
        if (Character.isLetter(first) && Character.isLowerCase(first)) {
            return Character.toUpperCase(first) + text.substring(1);
        }
        return text;
    }

    private static String stripTerminalPunctuation(String text) {
        return text.replaceFirst("[.!?]+$", "").trim();
    }

    private static boolean looksLikeQuestion(String text) {
        String lower = text.trim().toLowerCase(Locale.ROOT);
        if (lower.endsWith("?")) return true;
        String[] starters = {
                "what ", "why ", "how ", "where ", "when ", "who ", "does ", "do ",
                "is ", "are ", "can ", "could ", "should ", "anyone ", "anybody ",
                "which ", "wdym ", "what's ", "whats "
        };
        for (String starter : starters) {
            if (lower.startsWith(starter)) return true;
        }
        return false;
    }

    private static boolean containsPlease(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains(" please") || lower.contains(" pls");
    }


    private static int wordCount(String text) {
        return text.isBlank() ? 0 : text.trim().split("\\s+").length;
    }
}
