package me.nakilex.levelplugin.utils;

import me.nakilex.levelplugin.Main;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;

public class MessageStrings {
    File languageFile;
    FileConfiguration languageFileConfiguration;

    private HashMap<Translations, String> translations;

    public static final String NO_PERMISSION = Main.PREFIX + "";

    public MessageStrings() {
        this.createLanguageFile();
        this.translations = new HashMap<Translations, String>();

        this.initDefaultTranslations();
        this.saveAndLoadConfig();
    }

    public String getTranslation(Translations translation) {
        return this.translations.get(translation);
    }

    public void reloadConfig() {
        this.initDefaultTranslations();
        this.saveAndLoadConfig();
    }

    private void initDefaultTranslations() {
        if(Main.getPlugin().getConfigValues().LANGUAGE_VERSION.equals("de")) {
            // German translation

            this.translations.put(Translations.NO_PERMISSION, "§cDu hast keine Berechtigung dazu!");
            this.translations.put(Translations.WRONG_USAGE, "§cFalsche Benutzung des Befehls /trade! Bitte verwende " +
                "§6/trade <Name>§c oder " +
                "§6/trade accept§c, um ein eingehendes Handelsangebot anzunehmen.");
            this.translations.put(Translations.TRADE_REQUEST_SENT, "§aDas Handelsangebot wurde an §6%s§a gesendet!");
            this.translations.put(Translations.RELOADED_CONFIG, "Die Konfiguration wurde neu geladen!");
            this.translations.put(Translations.AUTHOR_OF_PLUGIN_IS, "§aDer Autor des Trade-Plugins ist §6Robby3St. §a" +
                "Finde das Plugin auf §6GitHub ");
            this.translations.put(Translations.PLUGIN_VERSION_IS, "§aDie aktuelle Version des Plugins ist: §6%s. " +
                "§aDu kannst nach neuen Originalversionen hier suchen: §6GitHub %s");
            this.translations.put(Translations.DOWNLOAD_PLUGIN_HERE, "§aDu kannst das Original-Trade-Plugin von Robby3St hier herunterladen: ");
            this.translations.put(Translations.COULD_NOT_FIND_PLAYER_WITH_THAT_NAME_PLEASE_USE_COMMAND, "%s§cKonnte keinen Spieler mit dem Namen §6'%s'§c finden. Bitte verwende " +
                "§6/trade <Name>§c oder §6/trade accept§c, um ein eingehendes Handelsangebot anzunehmen!");
            this.translations.put(Translations.COULD_NOT_FIND_PLAYER_WITH_THAT_NAME, "§cKonnte keinen Spieler mit diesem Namen finden!");
            this.translations.put(Translations.MUST_BE_A_PLAYER, "Du musst ein Spieler sein, um das zu tun!");
            this.translations.put(Translations.DEAL_PARTNERS_FIELD, "§7§l§i(Feld des Handelspartners)");
            this.translations.put(Translations.DEAL_WITH, "§6§lHandel mit %s");
            this.translations.put(Translations.FILLER_ITEM, "§7§l(Füll-Item)");
            this.translations.put(Translations.ACCEPT_TRADE_ITEM, "§2§lHandel annehmen");
            this.translations.put(Translations.OPPOSITE_DID_NOT_ACCEPTED_TRADE_ITEM, "§c§lGegenüber hat noch nicht angenommen");
            this.translations.put(Translations.OPPOSITE_ACCEPTS_DEAL_ITEM, "§a§lGegenüber akzeptiert diesen Handel");
            this.translations.put(Translations.OWN_DECLINE_DEAL_ITEM, "§c§lDiesen Handel ablehnen");
            this.translations.put(Translations.OWN_ACCEPT_DEAL_ITEM, "§a§lDiesen Handel akzeptieren");
            this.translations.put(Translations.YOU_DECLINED_DEAL, "Du hast den Handel mit %s abgelehnt, indem du dein Inventar geschlossen hast!");
            this.translations.put(Translations.OPPONENT_DECLINED_DEAL, " hat den Handel abgelehnt!");
            this.translations.put(Translations.DEAL_PARTNERS_LORE_1, "§r§lItem des ");
            this.translations.put(Translations.DEAL_PARTNERS_LORE_2, "§r§lDealpartners");
            this.translations.put(Translations.CAN_NOT_TRADE_WITH_YOURSELF, "§cDu kannst nicht mit dir selbst handeln!");
            this.translations.put(Translations.ALREADY_SENT_TRADE_REQUEST, "§cDu hast bereits eine Handelsanfrage an §6%s gesendet! " +
                "§cBitte brich den Handel zuerst ab, indem du §8/trade cancel§c benutzt,");
            this.translations.put(Translations.YOU_GOT_A_NEW_TRADE_OFFER, "Du hast ein neues Handelsangebot von §6%s§r erhalten! Akzeptiere, um zu handeln.");
            this.translations.put(Translations.TRADE_REQUEST_BY_EXPIRED, "§8§l§ka§r §cDie Handelsanfrage von §6%s§c ist abgelaufen!");
            this.translations.put(Translations.OWN_TRADE_REQUEST_EXPIRED, "§cDeine Handelsanfrage an §6%s§c ist abgelaufen!");
            this.translations.put(Translations.PLAYER_DID_NOT_SENT_YOU_A_TRADE_REQUEST, "Dieser Spieler hat dir kein Handelsangebot gesendet. Tut uns leid.");
            this.translations.put(Translations.YOU_GOT_MORE_THAN_ONE_OFFER, "Du hast mehr als 1 Handelsangebot erhalten! " +
                "Bitte benutze /trade accept <Name>, um das Handelsangebot eines Spielers anzunehmen.");
            this.translations.put(Translations.PLAYER_WENT_OFFLINE, "Entschuldigung, aber dieser Spieler ist offline gegangen!");
            this.translations.put(Translations.YOU_GOT_NO_TRADING_OFFER, "Entschuldigung, aber du hast kein Handelsangebot erhalten.");
            this.translations.put(Translations.YOU_CANCELED_YOUR_TRADE_REQUEST, "Du hast deinen Handel mit %s abgebrochen!");
            this.translations.put(Translations.OPPONENT_CANCELED_TRADE_OFFER, "%s hat das Handelsangebot mit dir abgebrochen.");
            this.translations.put(Translations.NO_TRADES_TO_CANCEL, "Entschuldigung, aber du hast keine Handelsangebote zum Abbrechen!");
            this.translations.put(Translations.OPPONENT_DENIED_TRADE_REQUEST, "%s hat deine Handelsanfrage abgelehnt!");
            this.translations.put(Translations.YOU_DECLINED_TRADE_REQUEST, "Du hast die Handelsanfrage von %s abgelehnt.");
            this.translations.put(Translations.GOT_NO_REQUESTS_TO_DENY, "Du hast keine Handelsanfragen zum Ablehnen!");
            this.translations.put(Translations.YOU_ENABLED_USE_WITHOUT_PERMISSION, "Spieler können nun §6ohne explizite Berechtigung§r verhandeln.");
            this.translations.put(Translations.YOU_DISABLED_USE_WITHOUT_PERMISSION, "Spieler brauchen nun die Berechtigung §6trade.trade, §rum handeln zu können.");
            this.translations.put(Translations.CHAT_BUTTON_ACCEPT, "Annehmen");
            this.translations.put(Translations.CHAT_BUTTON_DENY, "Ablehnen");
            this.translations.put(Translations.PLAYER_TO_FAR_AWAY, "§cDer Spieler, dem du eine Handelsanfrage senden möchtest, ist weiter als die erlaubten §6§l%s Blöcke §centfernt.");
            this.translations.put(Translations.BLOCKED_ALL, "§8Du erhältst nun §ckeine Handelsanfragen§8 mehr!");
            this.translations.put(Translations.UNBLOCKED_ALL, "§8Du kannst nun wieder §avon allen §8Handelsanfragen erhalten!");
            this.translations.put(Translations.BLOCKED_PLAYER, "§8Du §cverbietest§8 es nun §6%s§8 dir Handelsanfragen zu senden!");
            this.translations.put(Translations.UNBLOCKED_PLAYER, "§8Du §aerlaubst§8 es nun §6%s§8 dir Handelsanfragen zu senden!");
            this.translations.put(Translations.PLAYER_DOES_NOT_ACCEPT_TRADE_REQUESTS, "§cDieser Spieler erlaubt leider keine Handelsanfragen!");
        } else {
            // English translations

            this.translations.put(Translations.NO_PERMISSION, "§cYou don't have the permissions to do that!");
            this.translations.put(Translations.WRONG_USAGE, "§cWrong usage of the command /trade! Please use " +
                "§6/trade <Name>§c or " +
                "§6/trade accept§c, to accept an incoming trade.");
            this.translations.put(Translations.TRADE_REQUEST_SENT, "§aThe trade request was now sent to §6%s!");
            this.translations.put(Translations.RELOADED_CONFIG, "Reloaded the config!");
            this.translations.put(Translations.AUTHOR_OF_PLUGIN_IS, "§aAuthor of the trade plugin is §6Robby3St. §a" +
                "Find the plugin on §6GitHub ");
            this.translations.put(Translations.PLUGIN_VERSION_IS, "§aThe current used version of the pluin is: §6%s. " +
                "§aYou can check for original newer versions here: §6GitHub %s");
            this.translations.put(Translations.DOWNLOAD_PLUGIN_HERE, "§aYou can download the original " +
                "trade plugin by Robby3St here: ");
            this.translations.put(Translations.COULD_NOT_FIND_PLAYER_WITH_THAT_NAME_PLEASE_USE_COMMAND, "%s§cCould not " +
                "find a player with the name §6'%s'. §cPlease use " +
                "§6/trade <Name>§c or §6/trade accept, §cto accept an incoming trade!");
            this.translations.put(Translations.COULD_NOT_FIND_PLAYER_WITH_THAT_NAME, "§cCould not find a player " +
                "with that name!");
            this.translations.put(Translations.MUST_BE_A_PLAYER, "You must be a player, to do this!");
            this.translations.put(Translations.DEAL_PARTNERS_FIELD, "§7§l§i(Deal partner's field)");
            this.translations.put(Translations.DEAL_WITH, "§6§lDeal with %s");
            this.translations.put(Translations.FILLER_ITEM, "§7§l(Filler)");
            this.translations.put(Translations.ACCEPT_TRADE_ITEM, "§2§lAccept Trade");
            this.translations.put(Translations.OPPOSITE_DID_NOT_ACCEPTED_TRADE_ITEM, "§c§lOpposite didn't accepted yet");
            this.translations.put(Translations.OPPOSITE_ACCEPTS_DEAL_ITEM, "§a§lOpposite accepts this deal");
            this.translations.put(Translations.OWN_DECLINE_DEAL_ITEM, "§c§lDecline this deal");
            this.translations.put(Translations.OWN_ACCEPT_DEAL_ITEM, "§a§lAccept this deal");
            this.translations.put(Translations.YOU_DECLINED_DEAL, "You declined the deal with %s by closing your inventory!");
            this.translations.put(Translations.OPPONENT_DECLINED_DEAL, " declined the deal!");
            this.translations.put(Translations.DEAL_PARTNERS_LORE_1, "§r§lDeal partner's ");
            this.translations.put(Translations.DEAL_PARTNERS_LORE_2, "§r§litem");
            this.translations.put(Translations.CAN_NOT_TRADE_WITH_YOURSELF, "§cYou can't trade with yourself!");
            this.translations.put(Translations.ALREADY_SENT_TRADE_REQUEST, "§cYou already sent a trade request to §6%s! " +
                "§cPlease cancel the trade, by using §8/trade cancel§c first,");
            this.translations.put(Translations.YOU_GOT_A_NEW_TRADE_OFFER, "You got a new trade offer by §6%s! Accept to trade.");
            this.translations.put(Translations.TRADE_REQUEST_BY_EXPIRED, "§8§l§ka§r §cThe trade request by §6%s§c expired!");
            this.translations.put(Translations.OWN_TRADE_REQUEST_EXPIRED, "§cYour trade request to §6%s§r §cexpired!");
            this.translations.put(Translations.PLAYER_DID_NOT_SENT_YOU_A_TRADE_REQUEST, "This player is not in a trade " +
                "offer with you. Sorry.");
            this.translations.put(Translations.YOU_GOT_MORE_THAN_ONE_OFFER, "You got more than 1 trade offer! " +
                "Please use /trade accept <Name> to accept a specific trade by a player");
            this.translations.put(Translations.PLAYER_WENT_OFFLINE, "Sorry, but this player went offline!");
            this.translations.put(Translations.YOU_GOT_NO_TRADING_OFFER, "Sorry, but you got no trading offer.");
            this.translations.put(Translations.YOU_CANCELED_YOUR_TRADE_REQUEST, "You cancelled your trade with %s!");
            this.translations.put(Translations.OPPONENT_CANCELED_TRADE_OFFER, "%s canceled the trade with you.");
            this.translations.put(Translations.NO_TRADES_TO_CANCEL, "Sorry, but you got no trade offers to cancel!");
            this.translations.put(Translations.OPPONENT_DENIED_TRADE_REQUEST, "%s denied your trading request!");
            this.translations.put(Translations.YOU_DECLINED_TRADE_REQUEST, "Declined trade request by ");
            this.translations.put(Translations.GOT_NO_REQUESTS_TO_DENY, "You got no trade requests to deny!");
            this.translations.put(Translations.YOU_ENABLED_USE_WITHOUT_PERMISSION, "You enabled the mode for using trades without permission!");
            this.translations.put(Translations.YOU_DISABLED_USE_WITHOUT_PERMISSION, "You disabled the mode for using trades without permission!");
            this.translations.put(Translations.CHAT_BUTTON_ACCEPT, "Accept");
            this.translations.put(Translations.CHAT_BUTTON_DENY, "Deny");
            this.translations.put(Translations.PLAYER_TO_FAR_AWAY, "§cThe player, you try to send a trade request, is too far away from you. The maximum distance is §6§l%s blocks.");
            this.translations.put(Translations.BLOCKED_ALL, "§8You §cblocked§8 all players to send you a trade request!");
            this.translations.put(Translations.UNBLOCKED_ALL, "§8You §aunblocked§8 all players to send you a trade request!");
            this.translations.put(Translations.BLOCKED_PLAYER, "§8You §cblocked§c §6'%s'§8 to send you trade requests!");
            this.translations.put(Translations.UNBLOCKED_PLAYER, "§8You §aunblocked§c §6'%s'§8 to send you trade requests!");
            this.translations.put(Translations.PLAYER_DOES_NOT_ACCEPT_TRADE_REQUESTS, "§cThis player does not allow trade requests!");
            this.translations.put(Translations.THIS_FEATURE_IS_NOT_ENABLED_IN_CONFIG, "§cThis feature is not enabled in config!");
        }
    }

    private void saveAndLoadConfig() {
        boolean hasWritten = false;
        for(Translations t : this.translations.keySet()) {
            if(!languageFileConfiguration.contains(t.toString())) {
                languageFileConfiguration.set(t.name().toString(), this.translations.get(t));
                hasWritten = true;
            } else {
                this.translations.replace(t, languageFileConfiguration.getString(t.name().toString()));
            }
        }
        if(hasWritten) {
            try {
                languageFileConfiguration.save(this.languageFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void createLanguageFile() {
        // Creates the default language file
        languageFile = new File(Main.getPlugin().getDataFolder() + "/languages/",
            Main.getPlugin().getConfigValues().LANGUAGE_VERSION + ".yml");
        if (!languageFile.exists()) {
            languageFile.getParentFile().mkdirs();
            try {
                languageFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
//            try {
//                Main.getPlugin().saveResource(
//                        Main.getPlugin().getConfigValues().LANGUAGE_VERSION + ".yml", false);
//            } catch (Exception e) {
//                Main.getPlugin().getLogger().log(Level.INFO, "Could not load a language.yml from embedded " +
//                        "resources with the language version " + Main.getPlugin().getConfigValues().LANGUAGE_VERSION);
//            }
        }

        languageFileConfiguration = new YamlConfiguration();
        try {
            languageFileConfiguration.load(languageFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }
}