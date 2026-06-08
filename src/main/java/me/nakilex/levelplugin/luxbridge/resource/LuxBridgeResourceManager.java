package me.nakilex.levelplugin.luxbridge.resource;

import me.nakilex.levelplugin.luxbridge.util.LuxBridgeElementUtil;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LuxBridgeResourceManager {
    private static final String RESOURCE_ROOT = "luxbridge";
    public static final String NEXO_ASSET_PATH = "plugins/Nexo/pack/external_packs/levelplugin-dialogue-hud/assets/levelplugin_dialogue";
    public static final String PACK_NAMESPACE = "levelplugin_dialogue";
    private static final Pattern JSON_STRING_FIELD = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern JSON_NUMBER_FIELD = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(-?\\d+)");
    private static final Map<String, Integer> FALLBACK_WIDTHS = Map.ofEntries(
            Map.entry("dialogue-background", 209), Map.entry("answer-background", 134), Map.entry("character-background", 32),
            Map.entry("hand", 14), Map.entry("fog", 256), Map.entry("name-start", 3), Map.entry("name-mid", 2), Map.entry("name-end", 3),
            Map.entry("kingdom-hand", 14), Map.entry("kingdom-dialogue", 209), Map.entry("kingdom-answer", 134), Map.entry("kingdom-character", 32),
            Map.entry("kingdom-name-start", 3), Map.entry("kingdom-name-mid", 2), Map.entry("kingdom-name-end", 3)
    );
    private static final Map<String, Integer> FALLBACK_HEIGHTS = Map.ofEntries(
            Map.entry("dialogue-background", 64), Map.entry("answer-background", 64), Map.entry("character-background", 64),
            Map.entry("hand", 16), Map.entry("fog", 256), Map.entry("name-start", 16), Map.entry("name-mid", 16), Map.entry("name-end", 16),
            Map.entry("kingdom-hand", 16), Map.entry("kingdom-dialogue", 64), Map.entry("kingdom-answer", 64), Map.entry("kingdom-character", 64),
            Map.entry("kingdom-name-start", 16), Map.entry("kingdom-name-mid", 16), Map.entry("kingdom-name-end", 16)
    );

    private static final Map<String, String> FONT_TEXTURE_ALIASES = Map.of(
            "luxdialogues_font.png", "levelplugin_dialogue_font.png",
            "luxdialogues_accented.png", "levelplugin_dialogue_accented.png",
            "luxdialogues_nonlatin.png", "levelplugin_dialogue_nonlatin.png"
    );


    private static final String[] ASCII_CHARS = {
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000",
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000",
            " !\"#$%&'()*+,-./",
            "0123456789:;<=>?",
            "@ABCDEFGHIJKLMNO",
            "PQRSTUVWXYZ[\\]^_",
            "`abcdefghijklmno",
            "pqrstuvwxyz{|}~\u0000",
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000",
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000£\u0000\u0000ƒ",
            "\u0000\u0000\u0000\u0000\u0000\u0000ªº\u0000\u0000¬\u0000\u0000\u0000«»",
            "░▒▓│┤╡╢╖╕╣║╗╝╜╛┐",
            "└┴┬├─┼╞╟╚╔╩╦╠═╬╧",
            "╨╤╥╙╘╒╓╫╪┘┌█▄▌▐▀",
            "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000∅∈\u0000",
            "≡±≥≤⌠⌡÷≈°∙\u0000√ⁿ²■\u0000"
    };

    private static final String[] NONLATIN_EUROPEAN_CHARS = {
            "¡‰­·₴≠¿×ØÞһðøþΑΒ",
            "ΓΔΕΖΗΘΙΚΛΜΝΞΟΠΡΣ",
            "ΤΥΦΧΨΩαβγδεζηθικ",
            "λμνξοπρςστυφχψωЂ",
            "ЅІЈЉЊЋАБВГДЕЖЗИК",
            "ЛМНОПРСТУФХЦЧШЩЪ",
            "ЫЬЭЮЯабвгдежзикл",
            "мнопрстуфхцчшщъы",
            "ьэюяєѕіјљњ–—‘’“”",
            "„…⁊←↑→↓⇄＋ƏəɛɪҮүӨ",
            "өʻˌ;ĸẞß₽€ѢѣѴѵӀѲѳ",
            "⁰¹³⁴⁵⁶⁷⁸⁹⁺⁻⁼⁽⁾ⁱ™",
            "ʔʕ⧈⚔☠ҚқҒғҰұӘәҖҗҢ",
            "ңҺאבגדהוזחטיכלמם",
            "נןסעפףצץקר¢¤¥©®µ",
            "¶¼½¾·‐‚†‡•‱′″‴‵‶",
            "‷‹›※‼‽⁂⁈⁉⁋⁎⁏⁑⁒⁗℗",
            "−∓∞☀☁☈Є☲☵☽♀♂⚥♠♣♥",
            "♦♩♪♫♬♭♮♯⚀⚁⚂⚃⚄⚅ʬ⚡",
            "⛏✔❄❌❤⭐⸘⸮⸵⸸⹁⹋⥝ᘔƐ߈",
            "ϛㄥⱯᗺƆᗡƎℲ⅁ꞰꞀԀꝹᴚ⟘∩",
            "Ʌ⅄ɐɔǝɟᵷɥᴉɾʞꞁɯɹʇʌ",
            "ʍʎԱԲԳԴԶԷԹԺԻԼԽԾԿՀ",
            "ՁՂՃՄՅՆՇՈՉՋՌՍՎՏՐՑ",
            "ՒՓՔՕՖՙաբգդեզէըթժ",
            "իլխծկհձղճմյնշոչպ",
            "ջռսվտրցւփքօֆևשתԸ",
            "՚՛՜՝՞՟ՠֈ֏¯ſƷʒǷƿȜ",
            "ȝȤȥ˙Ꝛꝛ‑⅋⏏⏩⏪⏭⏮⏯⏴⏵",
            "⏶⏷⏸⏹⏺⏻⏼⏽⭘▲▶▼◀●◦◘",
            "⚓⛨ĲĳǉꜨꜩꜹꜻﬀﬁﬂﬃﬅ�Ե",
            "Պᚠᚢᚣᚤᚥᚦᚧᚨᚩᚪᚫᚬᚭᚮᚯ",
            "ᚰᚱᚲᚳᚴᚶᚷᚸᚹᚺᚻᚼᚽᚾᚿᛀ",
            "ᛁᛂᛃᛄᛅᛆᛇᛈᛉᛊᛋᛌᛍᛎᛏᛐ",
            "ᛑᛒᛓᛔᛕᛖᛗᛘᛙᛚᛛᛜᛝᛞᛟᛠ",
            "ᛡᛢᛣᛤᛥᛦᛧᛨᛩᛪ᛫᛬᛭ᛮᛯᛰ",
            "ᛱᛲᛳᛴᛵᛶᛷᛸ☺☻¦☹ך׳״װ",
            "ױײ־׃׆´¨ᴀʙᴄᴅᴇꜰɢʜᴊ",
            "ᴋʟᴍɴᴏᴘꞯʀꜱᴛᴜᴠᴡʏᴢ§",
            "ɱɳɲʈɖɡʡɕʑɸʝʢɻʁɦʋ",
            "ɰɬɮʘǀǃǂǁɓɗᶑʄɠʛɧɫ",
            "ɨʉʊɘɵɤɜɞɑɒɚɝƁƉƑƩ",
            "ƲႠႡႢႣႤႥႦႧႨႩႪႫႬႭႮ",
            "ႯႰႱႲႳႴႵႶႷႸႹႺႻႼႽႾ",
            "ႿჀჁჂჃჄჅჇჍაბგდევზ",
            "თიკლმნოპჟრსტუფქღ",
            "ყშჩცძწჭხჯჰჱჲჳჴჵჶ",
            "ჷჸჹჺ჻ჼჽჾჿתּשׂפֿפּכּײַיִ",
            "וֹוּבֿבּꜧꜦɺⱱʠʗʖɭɷɿʅʆ",
            "ʓʚ₪₾֊ⴀⴁⴂⴃⴄⴅⴆⴡⴇⴈⴉ",
            "ⴊⴋⴌⴢⴍⴎⴏⴐⴑⴒⴣⴓⴔⴕⴖⴗ",
            "ⴘⴙⴚⴛⴜⴝⴞⴤⴟⴠⴥ⅛⅜⅝⅞⅓",
            "⅔✉☂☔☄⛄☃⌛⌚⚐✎❣♤♧♡♢",
            "⛈☰☱☳☴☶☷↔⇒⇏⇔⇵∀∃∄∉",
            "∋∌⊂⊃⊄⊅∧∨⊻⊼⊽∥≢⋆∑⊤",
            "⊥⊢⊨≔∁∴∵∛∜∂⋃⊆⊇□△▷",
            "▽◁◆◇○◎☆★✘₀₁₂₃₄₅₆",
            "₇₈₉₊₋₌₍₎∫∮∝⌀⌂⌘〒ɼ",
            "ƄƅẟȽƚƛȠƞƟƧƨƪƸƹƻƼ",
            "ƽƾȡȴȵȶȺⱥȻȼɆɇȾⱦɁɂ",
            "ɃɄɈɉɊɋɌɍɎɏẜẝỼỽỾỿ",
            "Ꞩꞩ𐌰𐌱𐌲𐌳𐌴𐌵𐌶𐌷𐌸𐌹𐌺𐌻𐌼𐌽",
            "𐌾𐌿𐍀𐍁𐍂𐍃𐍄𐍅𐍆𐍇𐍈𐍉𐍊🌧🔥🌊",
            "⅐⅑⅕⅖⅗⅙⅚⅟↉🗡🏹🪓🔱🎣🧪⚗",
            "⯪⯫Ɑ🛡✂🍖🪣🔔⏳⚑₠₡₢₣₤₥",
            "₦₩₫₭₮₰₱₲₳₵₶₷₸₹₺₻",
            "₼₿\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"
    };

    private static final String[] ACCENTED_CHARS = {
            "ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏ",
            "ÐÑÒÓÔÕÖÙÚÛÜÝàáâã",
            "äåæçìíîïñòóôõöùú",
            "ûüýÿĀāĂăĄąĆćĈĉĊċ",
            "ČčĎďĐđĒēĔĕĖėĘęĚě",
            "ĜĝḠḡĞğĠġĢģĤĥĦħĨĩ",
            "ĪīĬĭĮįİıĴĵĶķĹĺĻļ",
            "ĽľĿŀŁłŃńŅņŇňŊŋŌō",
            "ŎŏŐőŒœŔŕŖŗŘřŚśŜŝ",
            "ŞşŠšŢţŤťŦŧŨũŪūŬŭ",
            "ŮůŰűŲųŴŵŶŷŸŹźŻżŽ",
            "žǼǽǾǿȘșȚțΆΈΉΊΌΎΏ",
            "ΐΪΫάέήίΰϊϋόύώЀЁЃ",
            "ЇЌЍЎЙйѐёђѓїћќѝўџ",
            "ҐґḂḃḊḋḞḟḢḣḰḱṀṁṖṗ",
            "ṠṡṪṫẀẁẂẃẄẅỲỳèéêë",
            "ŉǧǫЏḍḥṛṭẒỊịỌọỤụ№",
            "ȇƔɣʃ⁇ǱǲǳǄǅǆǇǈǊǋǌ",
            "ℹᵫꜲꜳꜴꜵꜶꜷꜸꜺꜼꜽꝎꝏꝠꝡ",
            "ﬄﬆᚡᚵƠơƯưẮắẤấẾếốỚ",
            "ớỨứẰằẦầỀềồỜờỪừẢả",
            "ẲẳẨẩẺẻổỞỂểỈỉỎỏỔở",
            "ỦủỬửỶỷẠạẶặẬậẸẹỆệ",
            "ỘộỢợỰựỴỵỐƕẪẫỖỗữ☞",
            "☜☮ẴẵẼẽỄễỒỠỡỮỸỹҘҙ",
            "ҠҡҪҫǶ⚠⓪①②③④⑤⑥⑦⑧⑨",
            "⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳ⒶⒷⒸⒹⒺ",
            "ⒻⒼⒽⒾⒿⓀⓁⓂⓃⓄⓅⓆⓇⓈⓉⓊ",
            "ⓋⓌⓍⓎⓏⓐⓑⓒⓓⓔⓕⓖⓗⓘⓙⓚ",
            "ⓛⓜⓝⓞⓟⓠⓡⓢⓣⓤⓥⓦⓧⓨⓩ̧",
            "ʂʐɶǍǎǞǟǺǻȂȃȦȧǠǡḀ",
            "ḁȀȁḆḇḄḅᵬḈḉḐḑḒḓḎḏ",
            "ḌᵭḔḕḖḗḘḙḜḝȨȩḚḛȄȅ",
            "ȆᵮǴǵǦḦḧḨḩḪḫȞȟḤẖḮ",
            "ḯȊȋǏǐȈȉḬḭǰȷǨǩḲḳḴ",
            "ḵḺḻḼḽḶḷḸḹⱢḾḿṂṃᵯṄ",
            "ṅṆṇṊṋǸǹṈṉᵰǬǭȬȭṌṍ",
            "ṎṏṐṑṒṓȎȏȪȫǑǒȮȯȰȱ",
            "ȌȍǪṔṕᵱȒȓṘṙṜṝṞṟȐȑ",
            "ṚᵳᵲṤṥṦṧṢṣṨṩᵴṰṱṮṯ",
            "ṬẗᵵṲṳṶṷṸṹṺṻǓǔǕǖǗ",
            "ǘǙǚǛǜṴṵȔȕȖṾṿṼṽẆẇ",
            "ẈẉẘẌẍẊẋȲȳẎẏẙẔẕẐẑ",
            "ẓᵶǮǯẛꜾꜿǢǣᵺỻᴂᴔꭣȸʣ",
            "ʥʤʩʪʫȹʨʦʧꭐꭑ₧Ỻאַאָƀ",
            "ƂƃƇƈƊƋƌƓǤǥƗƖɩƘƙƝ",
            "ƤƥɽƦƬƭƫƮȗƱƜƳƴƵƶƢ",
            "ƣȢȣʭʮʯﬔﬕﬗﬖﬓӐӑӒӓӶ",
            "ӷҔҕӖӗҼҽҾҿӚӛӜӝӁӂӞ",
            "ӟӢӣӤӥӦӧӪӫӰӱӮӯӲӳӴ",
            "ӵӸӹӬӭѶѷӔӺԂꚂꚀꚈԪԬꚄ",
            "ԄԐӠԆҊӃҞҜԞԚӅԮԒԠԈԔ",
            "ӍӉԨӇҤԢԊҨԤҦҎԖԌꚐҬꚊ",
            "ꚌԎҲӼӾԦꚔҴꚎҶӋҸꚒꚖꚆҌ",
            "ԘԜӕӻԃꚃꚁꚉԫԭꚅԅԑӡԇҋ",
            "ӄҟҝԟԛӆԯԓԡԉԕӎӊԩӈҥ",
            "ԣԋҩԥҧҏԗԍꚑҭꚋꚍԏҳӽӿ",
            "ԧꚕҵꚏҷӌҹꚓꚗꚇҍԙԝἈἀἉ",
            "ἁἊἂἋἃἌἄἍἅἎἆἏἇᾺὰᾸ",
            "ᾰᾹᾱΆάᾈᾀᾉᾁᾊᾂᾋᾃᾌᾄᾍ",
            "ᾅᾎᾆᾏᾇᾼᾴᾶᾷᾲᾳἘἐἙἑἚ",
            "ἒἛἓἜἔἝἕῈΈὲέἨἠῊὴἩ",
            "ἡἪἢἫἣἬἤἭἥἮἦἯἧᾘᾐᾙ",
            "ᾑᾚᾒᾛᾓᾜᾔᾝᾕᾞᾖᾟᾗΉήῌ",
            "ῃῂῄῆῇῚὶΊίἸἰἹἱἺἲἻ",
            "ἳἼἴἽἵἾἶἿἷῘῐῙῑῒΐῖ",
            "ῗῸὸΌόὈὀὉὁὊὂὋὃὌὄὍ",
            "ὅῬῤῥῪὺΎύὙὑὛὓὝὕὟὗ",
            "ῨῠῩῡϓϔῢΰῧὐὒὔῦὖῺὼ",
            "ΏώὨὠὩὡὪὢὫὣὬὤὭὥὮὦ",
            "Ὧὧᾨᾠᾩᾡᾪᾢᾫᾣᾬᾤᾭᾥᾮᾦ",
            "ᾯᾧῼῳῲῴῶῷ☯☐☑☒ƍƺⱾȿ",
            "ⱿɀᶀꟄꞔᶁᶂᶃꞕᶄᶅᶆᶇᶈᶉᶊ",
            "ᶋᶌᶍꟆᶎᶏᶐᶒᶓᶔᶕᶖᶗᶘᶙᶚ",
            "ẚ⅒⅘₨₯\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"
    };

    private final JavaPlugin plugin;
    private final Map<String, LuxImageDefinition> images = new LinkedHashMap<>();
    private LuxLineDefinitions lines = new LuxLineDefinitions(40, 5, 25, 9, 0, 3, 75, 9);
    private final Set<String> generatedFontFiles = new LinkedHashSet<>();

    public LuxBridgeResourceManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reloadAndGenerate() {
        loadDefinitions();
        ensureOriginalFontTextureNames();
        generateFontJsons();
    }

    public Map<String, LuxImageDefinition> images() {
        return Collections.unmodifiableMap(images);
    }

    public LuxImageDefinition image(String id) {
        return images.get(id);
    }

    public LuxLineDefinitions lines() {
        return lines;
    }

    public File assetRoot() {
        return new File(plugin.getServer().getWorldContainer(), NEXO_ASSET_PATH);
    }

    public File textureDirectory() {
        return new File(assetRoot(), "textures");
    }

    public File fontTextureDirectory() {
        return new File(textureDirectory(), "font");
    }

    public File fontDirectory() {
        return new File(assetRoot(), "font");
    }

    public List<String> generatedFontFiles() {
        return List.copyOf(generatedFontFiles);
    }

    public List<String> requiredTextureFiles() {
        return images.values().stream().map(LuxImageDefinition::file).distinct().toList();
    }

    public List<String> assetDiagnostics() {
        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.GOLD + "[LuxBridge Assets]");
        lines.add(ChatColor.GRAY + "External pack base: " + ChatColor.WHITE + NEXO_ASSET_PATH + "/");
        lines.add(ChatColor.GRAY + "Resolved base: " + ChatColor.WHITE + assetRoot().getPath());
        lines.add("");
        appendTextureDiagnostics(lines);
        lines.add("");
        appendFontTextureDiagnostics(lines);
        lines.add("");
        appendFontDiagnostics(lines);
        lines.add("");
        appendImageYamlDiagnostics(lines);
        lines.add("");
        appendLineYamlDiagnostics(lines);
        lines.add("");
        appendOffsetDiagnostics(lines);
        return lines;
    }

    private void loadDefinitions() {
        images.clear();
        YamlConfiguration imageConfig = loadBundledYaml(RESOURCE_ROOT + "/Pack/Images/images.yml");
        ConfigurationSection imageSection = imageConfig.getConfigurationSection("Images");
        int codepoint = 0xE100;
        if (imageSection != null) {
            for (String id : imageSection.getKeys(false)) {
                ConfigurationSection section = imageSection.getConfigurationSection(id);
                if (section == null) continue;
                String file = section.getString("file", id + ".png");
                int[] size = detectImageSize(id, file);
                images.put(id, new LuxImageDefinition(
                        id,
                        file,
                        section.getBoolean("is-arrow", false),
                        Math.max(1, section.getInt("reduction-ratio", 1)),
                        section.getInt("ascent", 0),
                        size[0],
                        size[1],
                        new String(Character.toChars(codepoint++))
                ));
            }
        }

        YamlConfiguration lineConfig = loadBundledYaml(RESOURCE_ROOT + "/Pack/Lines/lines.yml");
        lines = new LuxLineDefinitions(
                lineConfig.getInt("Character-Name.ascent", 40),
                lineConfig.getInt("Dialogue-Lines.count", 5),
                lineConfig.getInt("Dialogue-Lines.ascent", 25),
                lineConfig.getInt("Dialogue-Lines.space", 9),
                lineConfig.getInt("Information-Line.ascent", 0),
                lineConfig.getInt("Answer-Lines.count", 3),
                lineConfig.getInt("Answer-Lines.ascent", 75),
                lineConfig.getInt("Answer-Lines.space", 9)
        );
    }

    private YamlConfiguration loadBundledYaml(String path) {
        try (InputStreamReader reader = new InputStreamReader(plugin.getResource(path), StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (Exception exception) {
            plugin.getLogger().warning("[LuxBridge] Could not load bundled resource " + path + ": " + exception.getMessage());
            return new YamlConfiguration();
        }
    }

    private int[] detectImageSize(String id, String fileName) {
        File file = new File(assetRoot(), "textures/" + fileName);
        if (!file.exists()) file = new File(assetRoot(), fileName);
        if (file.exists()) {
            try {
                BufferedImage image = ImageIO.read(file);
                if (image != null) return new int[] {image.getWidth(), image.getHeight()};
            } catch (IOException ignored) {
            }
        }
        return new int[] {FALLBACK_WIDTHS.getOrDefault(id, 16), FALLBACK_HEIGHTS.getOrDefault(id, 16)};
    }

    private void ensureOriginalFontTextureNames() {
        File fontTextureDir = fontTextureDirectory();
        for (Map.Entry<String, String> alias : FONT_TEXTURE_ALIASES.entrySet()) {
            File target = new File(fontTextureDir, alias.getKey());
            if (target.exists()) continue;
            File renamedSource = new File(fontTextureDir, alias.getValue());
            if (!renamedSource.exists()) continue;
            try {
                if (!fontTextureDir.exists() && !fontTextureDir.mkdirs()) {
                    plugin.getLogger().warning("[LuxBridge] Could not create font texture folder: " + fontTextureDir.getPath());
                    continue;
                }
                Files.copy(renamedSource.toPath(), target.toPath());
                plugin.getLogger().info("[LuxBridge] Copied font texture alias " + alias.getValue() + " -> " + alias.getKey());
            } catch (IOException exception) {
                plugin.getLogger().warning("[LuxBridge] Failed to copy font texture alias " + alias.getValue()
                        + " -> " + alias.getKey() + ": " + exception.getMessage());
            }
        }
    }

    private void generateFontJsons() {
        generatedFontFiles.clear();
        File fontDir = fontDirectory();
        if (!fontDir.exists() && !fontDir.mkdirs()) {
            plugin.getLogger().warning("[LuxBridge] Could not create Nexo font folder: " + fontDir.getPath());
            return;
        }
        writeGeneratedFont(fontDir, "offset_chars.json", offsetFontJson());
        for (LuxImageDefinition image : images.values()) {
            writeGeneratedFont(fontDir, image.id() + ".json", imageFontJson(image));
        }
        writeGeneratedFont(fontDir, "levelplugin_dialogue_character_name.json", textFontJson(lines.characterNameAscent()));
        writeGeneratedFont(fontDir, "levelplugin_dialogue_info.json", textFontJson(lines.informationLineAscent()));
        for (int i = 1; i <= lines.dialogueLineCount(); i++) {
            writeGeneratedFont(fontDir, "levelplugin_dialogue_line_" + i + ".json", textFontJson(lines.dialogueLineAscent() - ((i - 1) * lines.dialogueLineSpace())));
        }
        for (int i = 1; i <= lines.answerLineCount(); i++) {
            writeGeneratedFont(fontDir, "levelplugin_dialogue_answer_" + i + ".json", textFontJson(lines.answerLineAscent() - ((i - 1) * lines.answerLineSpace())));
        }
    }

    private void writeGeneratedFont(File fontDir, String fileName, String json) {
        generatedFontFiles.add(fileName);
        write(new File(fontDir, fileName), json);
    }

    private void write(File file, String json) {
        try {
            Files.writeString(file.toPath(), json, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            plugin.getLogger().warning("[LuxBridge] Failed to write " + file.getPath() + ": " + exception.getMessage());
        }
    }

    private String offsetFontJson() {
        return "{\n  \"providers\": [\n    {\"type\":\"space\",\"advances\":{\"" + LuxBridgeElementUtil.NEGATIVE_OFFSET + "\":-1,\"" + LuxBridgeElementUtil.POSITIVE_OFFSET + "\":1}}\n  ]\n}";
    }

    private String imageFontJson(LuxImageDefinition image) {
        String texture = image.file().endsWith(".png") ? image.file().substring(0, image.file().length() - 4) : image.file();
        return "{\n  \"providers\": [\n    {\n      \"type\": \"bitmap\",\n      \"file\": \"levelplugin_dialogue:" + escape(texture) + "\",\n      \"ascent\": " + image.ascent() + ",\n      \"height\": " + Math.max(1, image.height() / Math.max(1, image.reductionRatio())) + ",\n      \"chars\": [\"" + image.glyph() + "\"]\n    }\n  ]\n}";
    }

    private String textFontJson(int ascent) {
        return "{\n  \"providers\": [\n"
                + textProviderJson("font/luxdialogues_nonlatin", ascent, null, NONLATIN_EUROPEAN_CHARS) + ",\n"
                + textProviderJson("font/luxdialogues_accented", ascent + 3, 12, ACCENTED_CHARS) + ",\n"
                + textProviderJson("font/luxdialogues_font", ascent, null, ASCII_CHARS) + "\n"
                + "  ]\n}";
    }

    private String textProviderJson(String texture, int ascent, Integer height, String[] chars) {
        return "    {\"type\":\"bitmap\",\"file\":\"" + PACK_NAMESPACE + ":" + texture
                + "\",\"ascent\":" + ascent
                + (height == null ? "" : ",\"height\":" + height)
                + ",\"chars\":" + charsJson(chars) + "}";
    }

    private String charsJson(String[] chars) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < chars.length; i++) {
            if (i > 0) builder.append(',');
            builder.append('\"').append(escapeJson(chars[i])).append('\"');
        }
        return builder.append(']').toString();
    }

    private void appendTextureDiagnostics(List<String> output) {
        output.add(ChatColor.YELLOW + "Required textures:");
        for (String texture : requiredTextureFiles()) {
            File file = new File(textureDirectory(), texture);
            output.add(status(file.exists()) + " textures/" + texture);
        }
    }

    private void appendFontTextureDiagnostics(List<String> output) {
        output.add(ChatColor.YELLOW + "Required text font textures:");
        for (String texture : FONT_TEXTURE_ALIASES.keySet()) {
            File file = new File(fontTextureDirectory(), texture);
            String source = FONT_TEXTURE_ALIASES.get(texture);
            output.add(status(file.exists()) + " textures/font/" + texture
                    + (file.exists() ? "" : ChatColor.GRAY + " (alias source: textures/font/" + source + ")"));
        }
    }

    private void appendFontDiagnostics(List<String> output) {
        output.add(ChatColor.YELLOW + "Generated font JSONs:");
        for (String fileName : generatedFontFiles()) {
            File file = new File(fontDirectory(), fileName);
            output.add(status(file.exists()) + " font/" + fileName);
            if (file.exists()) {
                validateFontJson(fileName, file, output);
            }
        }
    }

    private void validateFontJson(String fileName, File file, List<String> output) {
        try {
            String json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            Map<String, String> strings = stringFields(json);
            Map<String, Integer> numbers = numberFields(json);
            String providerType = strings.get("type");
            String texture = strings.get("file");
            if ("offset_chars.json".equals(fileName)) {
                boolean hasNegative = json.contains(LuxBridgeElementUtil.NEGATIVE_OFFSET);
                boolean hasPositive = json.contains(LuxBridgeElementUtil.POSITIVE_OFFSET);
                output.add(detail(("space".equals(providerType) || json.contains("\"type\":\"space\"")), "offset provider type=" + providerType));
                output.add(detail(hasNegative && hasPositive, "offset chars negative=" + codepointList(LuxBridgeElementUtil.NEGATIVE_OFFSET) + " positive=" + codepointList(LuxBridgeElementUtil.POSITIVE_OFFSET)));
                return;
            }
            boolean bitmap = "bitmap".equals(providerType);
            boolean hasAscent = numbers.containsKey("ascent");
            boolean hasHeight = numbers.containsKey("height");
            output.add(detail(bitmap && hasAscent && hasHeight, "provider=" + providerType + " ascent=" + numbers.get("ascent") + " height=" + numbers.get("height")));
            if (texture != null) {
                boolean levelPluginNamespace = texture.startsWith(PACK_NAMESPACE + ":");
                File resolved = levelPluginNamespace ? textureFile(texture) : null;
                boolean resolvedTexture = resolved != null && resolved.exists();
                output.add(detail(levelPluginNamespace && resolvedTexture, "mapping " + fontKey(fileName) + " -> " + texture + " -> " + relativeTexturePath(texture) + (resolvedTexture ? "" : " (missing texture or namespace)")));
            } else if (!isTextFont(fileName)) {
                output.add(ChatColor.RED + "MISSING file mapping in " + fileName);
            }
        } catch (IOException exception) {
            output.add(ChatColor.RED + "INVALID font/" + fileName + " could not be read: " + exception.getMessage());
        }
    }

    private void appendImageYamlDiagnostics(List<String> output) {
        output.add(ChatColor.YELLOW + "images.yml loaded keys:");
        for (LuxImageDefinition image : images.values()) {
            output.add(ChatColor.GRAY + image.id() + ChatColor.WHITE + " -> " + image.file()
                    + ChatColor.DARK_GRAY + " glyph=" + image.glyph() + " ascent=" + image.ascent()
                    + " size=" + image.width() + "x" + image.height());
        }
    }

    private void appendLineYamlDiagnostics(List<String> output) {
        output.add(ChatColor.YELLOW + "lines.yml loaded keys:");
        output.add(ChatColor.GRAY + "Character-Name" + ChatColor.WHITE + " ascent=" + lines.characterNameAscent());
        output.add(ChatColor.GRAY + "Dialogue-Lines" + ChatColor.WHITE + " count=" + lines.dialogueLineCount() + " ascent=" + lines.dialogueLineAscent() + " space=" + lines.dialogueLineSpace());
        output.add(ChatColor.GRAY + "Information-Line" + ChatColor.WHITE + " ascent=" + lines.informationLineAscent());
        output.add(ChatColor.GRAY + "Answer-Lines" + ChatColor.WHITE + " count=" + lines.answerLineCount() + " ascent=" + lines.answerLineAscent() + " space=" + lines.answerLineSpace());
    }

    private void appendOffsetDiagnostics(List<String> output) {
        File offsetFile = new File(fontDirectory(), "offset_chars.json");
        output.add(ChatColor.YELLOW + "Offset diagnostics:");
        output.add(status(offsetFile.exists()) + " font/offset_chars.json");
        output.add(ChatColor.GRAY + "offset range count: " + ChatColor.WHITE + "2 single-pixel advances");
        output.add(ChatColor.GRAY + "loaded offset font key: " + ChatColor.WHITE + PACK_NAMESPACE + ":offset_chars");
        output.add(ChatColor.GRAY + "unicode range used: " + ChatColor.WHITE
                + codepointList(LuxBridgeElementUtil.NEGATIVE_OFFSET) + " negative, "
                + codepointList(LuxBridgeElementUtil.POSITIVE_OFFSET) + " positive");
    }

    private File textureFile(String resourceLocation) {
        String path = resourceLocation.substring((PACK_NAMESPACE + ":").length());
        if (!path.endsWith(".png")) path = path + ".png";
        return new File(textureDirectory(), path);
    }

    private String relativeTexturePath(String resourceLocation) {
        if (!resourceLocation.startsWith(PACK_NAMESPACE + ":")) return "unresolved";
        String path = resourceLocation.substring((PACK_NAMESPACE + ":").length());
        if (!path.endsWith(".png")) path = path + ".png";
        return "textures/" + path;
    }

    private static Map<String, String> stringFields(String json) {
        Map<String, String> fields = new LinkedHashMap<>();
        Matcher matcher = JSON_STRING_FIELD.matcher(json);
        while (matcher.find()) fields.put(matcher.group(1), matcher.group(2));
        return fields;
    }

    private static Map<String, Integer> numberFields(String json) {
        Map<String, Integer> fields = new LinkedHashMap<>();
        Matcher matcher = JSON_NUMBER_FIELD.matcher(json);
        while (matcher.find()) fields.put(matcher.group(1), Integer.parseInt(matcher.group(2)));
        return fields;
    }

    private static boolean isTextFont(String fileName) {
        return fileName.startsWith("levelplugin_dialogue_");
    }

    private static String fontKey(String fileName) {
        return fileName.endsWith(".json") ? fileName.substring(0, fileName.length() - 5) : fileName;
    }

    private static String status(boolean found) {
        return (found ? ChatColor.GREEN + "FOUND " : ChatColor.RED + "MISSING ") + ChatColor.WHITE;
    }

    private static String detail(boolean ok, String message) {
        return (ok ? ChatColor.GREEN + "OK " : ChatColor.RED + "BAD ") + ChatColor.GRAY + message;
    }

    private static String codepointList(String value) {
        return value.codePoints()
                .mapToObj(codepoint -> String.format("U+%04X", codepoint))
                .reduce((first, second) -> first + "," + second)
                .orElse("none");
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String escapeJson(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (character == '\\') {
                builder.append("\\\\");
            } else if (character == '"') {
                builder.append("\\\"");
            } else if (character < 0x20) {
                builder.append(String.format("\\u%04x", (int) character));
            } else {
                builder.append(character);
            }
        }
        return builder.toString();
    }
}
