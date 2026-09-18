package me.nakilex.playerspoofer;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Template chatter shaped around actual prison-server conversation: progression questions,
 * short answers, prices/value uncertainty, upgrade talk, greetings and social acknowledgements.
 */
final class TemplateChatEngine {
    private static final List<String> GENERAL = List.of(
            "is token finder worth leveling early",
            "what do you guys put gems into first",
            "does anyone use prism break much",
            "is prodigy worth saving tokens for",
            "what pick lvl are you guys at",
            "how much do you guys put into fortune",
            "is prestige finder worth it early",
            "what enchant do you usually upgrade after fortune",
            "does super token miner feel worth the gems",
            "im saving for another pick upgrade",
            "finally got another pick level",
            "mornin all",
            "gg",
            "back"
    );

    private static final List<String> GRINDER = List.of(
            "what level token finder are you guys running",
            "im debating prodigy or more fortune",
            "does doomfall feel worth 40k gems",
            "how high do you take prestige finder",
            "is second hand worth pushing early",
            "im saving gems for prism break",
            "tornado gets expensive fast",
            "anyone tried meteor shower yet",
            "black hole or acid rain first",
            "what do you usually spend gems on first"
    );

    private static final List<String> EXPLORER = List.of(
            "does mine size go up with pick lvl",
            "what unlocks after the next tier",
            "where do i check quests again",
            "is there a list of rebirth unlocks",
            "does anything important unlock before rebirth",
            "what does the next mine tier change",
            "is prestige finder only from gems"
    );

    private static final List<String> SOCIAL = List.of(
            "mornin",
            "gg",
            "welcome",
            "what enchant are you guys saving for",
            "is token finder still worth pushing",
            "what pick lvl are you at",
            "anyone know if doomfall is worth it",
            "how much fortune do you guys have"
    );

    private static final List<String> QUIET = List.of(
            "gg", "back", "nice", "fr", "what enchant first", "rebirth worth it", "token finder worth it"
    );

    private static final List<String> AFK = List.of(
            "sry was afk", "back", "one sec", "brb", "wasnt looking at chat", "im back", "what did i miss"
    );

    private static final List<String> GENERIC_REPLIES = List.of(
            "fr", "same", "ye", "not sure tbh", "true", "probably", "idk", "depends tbh", "fair"
    );

    String autonomous(FakePlayer bot) {
        String personality = bot.personalityId().toLowerCase(Locale.ROOT);
        List<String> pool = switch (personality) {
            case "grinder" -> GRINDER;
            case "explorer" -> EXPLORER;
            case "social" -> SOCIAL;
            case "quiet" -> QUIET;
            case "afk-prone" -> AFK;
            default -> GENERAL;
        };
        return pick(pool);
    }

    String reply(FakePlayer bot, String message) {
        String lower = message == null ? "" : message.toLowerCase(Locale.ROOT);

        if (isWelcomeBack(lower)) return pick(List.of("ty", "thanks", "thx", "appreciate it"));
        if (isBackMessage(lower)) return welcomeBack(bot);
        if (containsAny(lower, "hello", "hey", "yo ", "yo", "sup", "hiya", "mornin", "morning")) {
            return pick(List.of("yo", "hey", "mornin", "welcome", "sup"));
        }
        if (containsAny(lower, "thanks", "thank you", "ty ", "thx", "tysm")) {
            return youreWelcome(bot);
        }
        if (containsAny(lower, "wish me luck", "gl me", "need luck")) {
            return pick(List.of("gl", "good luck", "you got it"));
        }
        if (containsAny(lower, "trade me", "buying", "selling", "sell ", "buy ", "rate", "price", "worth")) {
            return pick(List.of(
                    "not sure what the rate is rn",
                    "idk current prices tbh",
                    "prices change a lot",
                    "ask around before you sell it",
                    "im just checking prices rn"
            ));
        }
        if (containsAny(lower, "can i have", "give me", "spare", "lend me")) {
            return pick(List.of("sry im saving mine", "im low too rn", "nah i need mine", "cant rn"));
        }
        if (containsAny(lower, "rebirth", "prestige")) {
            return pick(List.of(
                    "i wouldnt rush it",
                    "im waiting until upgrades slow down",
                    "depends how close you are",
                    "seems worth it once progress slows",
                    "im still saving for mine"
            ));
        }
        if (containsAny(lower, "token finder")) {
            return pick(List.of("ive been putting levels into it", "mine is still pretty low", "seems useful early", "im still leveling mine"));
        }
        if (containsAny(lower, "prodigy")) {
            return pick(List.of("25k base is kinda steep", "im saving for it rn", "havent pushed it much yet", "im debating that too"));
        }
        if (containsAny(lower, "doomfall")) {
            return pick(List.of("40k gems base is rough", "havent tried it much yet", "im saving gems before touching it", "idk if id rush it"));
        }
        if (containsAny(lower, "prism break")) {
            return pick(List.of("30k base is a lot", "im saving for it", "havent leveled it much yet", "depends what else you need"));
        }
        if (containsAny(lower, "meteor shower")) {
            return pick(List.of("900k token base is expensive", "havent got much into it yet", "im nowhere near maxing that", "looks good but its pricey"));
        }
        if (containsAny(lower, "black hole")) {
            return pick(List.of("750k base hurts", "im saving for it", "havent tested it enough", "im still on the cheaper enchants"));
        }
        if (containsAny(lower, "acid rain")) {
            return pick(List.of("650k base isnt cheap", "havent tested it enough", "im saving for it", "idk if id take it before black hole"));
        }
        if (containsAny(lower, "tornado")) {
            return pick(List.of("500k base adds up fast", "mine is still low level", "im saving tokens for it", "havent pushed it much yet"));
        }
        if (containsAny(lower, "enchant", "upgrade first", "best upgrade", "what do you guys max")) {
            return pick(List.of(
                    "depends what youre going for",
                    "fortune is solid",
                    "i usually put most into progression stuff first",
                    "idk im still testing stuff",
                    "i wouldnt spread levels too much"
            ));
        }
        if (containsAny(lower, "fortune")) {
            return pick(List.of("fortune feels useful", "i put a lot into fortune", "depends on your setup", "i think its worth leveling"));
        }
        if (containsAny(lower, "token", "tokens")) {
            return pick(List.of(
                    "mine disappear on upgrades",
                    "im saving mine rn",
                    "they go fast later",
                    "not sure what the rate is rn",
                    "i always end up spending mine"
            ));
        }
        if (containsAny(lower, "gem", "gems")) {
            return pick(List.of("im saving mine rn", "i barely spend them", "not sure whats best for gems yet", "i keep mine for later"));
        }
        if (containsAny(lower, "pick lvl", "pickaxe level", "pick level")) {
            return pick(List.of("not that high yet", "mine still has a while to go", "im leveling it rn", "higher than yesterday at least"));
        }
        if (containsAny(lower, "quest", "quests")) {
            return pick(List.of("i always forget those too", "i just claim them when i notice", "pretty sure its in /quests"));
        }
        if (containsAny(lower, "mine size", "mine tier", "next tier", "mine level")) {
            return pick(List.of("i think it changes with progression", "not sure tbh", "mine got bigger after i leveled", "check when you hit the next tier"));
        }
        if (containsAny(lower, "what are you doing", "what u doing", "wyd")) {
            return pick(List.of("upgrading my pick", "checking enchants", "just mining", "looking at upgrades rn"));
        }
        if (containsAny(lower, "lag", "ping")) {
            return pick(List.of("mine seems fine", "bit laggy for me too", "im good rn", "ye had a spike before"));
        }
        if (lower.endsWith("?") || lower.contains("?")) {
            return pick(List.of("not sure tbh", "i think so", "probably", "idk", "depends"));
        }
        return fakeToFakeReply();
    }

    String thanks(FakePlayer bot) {
        return switch (WritingStyleEngine.styleFor(bot)) {
            case CLEAN -> pick(List.of("Thanks", "Thank you", "Appreciate it"));
            case CASUAL -> pick(List.of("thanks", "ty", "appreciate it"));
            case BARE -> pick(List.of("ty", "thx", "thanks"));
            case ABBREVIATED -> pick(List.of("ty", "thx", "ty man", "tysm"));
            case POLITE -> pick(List.of("thank you", "thanks, appreciate it", "thank you, that helps"));
            case EXPRESSIVE -> pick(List.of("ty bro", "thanks man", "appreciate it"));
            case MIXED -> pick(List.of("thanks", "ty", "Thank you"));
        };
    }

    String youreWelcome(FakePlayer bot) {
        return switch (WritingStyleEngine.styleFor(bot)) {
            case CLEAN, POLITE -> pick(List.of("No problem", "You're welcome", "No worries"));
            case ABBREVIATED, BARE -> pick(List.of("np", "yw", "all good"));
            case EXPRESSIVE -> pick(List.of("np bro", "all good", "no worries"));
            default -> pick(List.of("np", "all good", "no worries", "yw"));
        };
    }

    String welcomeBack(FakePlayer bot) {
        return switch (WritingStyleEngine.styleFor(bot)) {
            case CLEAN, POLITE -> pick(List.of("Welcome back", "wb"));
            case ABBREVIATED, BARE -> "wb";
            default -> pick(List.of("wb", "welcome back"));
        };
    }

    String fakeToFakeReply() {
        return pick(GENERIC_REPLIES);
    }

    static boolean isBackMessage(String lower) {
        if (lower == null) return false;
        String s = lower.trim().toLowerCase(Locale.ROOT).replace("'", "");
        return s.equals("back") || s.equals("im back") || s.equals("i am back") || s.equals("back now") || s.equals("im here");
    }

    static boolean isWelcomeBack(String lower) {
        if (lower == null) return false;
        String s = lower.trim();
        return s.equals("wb") || s.startsWith("wb ") || s.equals("welcome back") || s.startsWith("welcome back ");
    }

    private static boolean containsAny(String input, String... values) {
        for (String value : values) if (input.contains(value)) return true;
        return false;
    }

    private static String pick(List<String> values) {
        return values.get(ThreadLocalRandom.current().nextInt(values.size()));
    }
}
