package me.nakilex.playerspoofer;

import java.util.List;

/**
 * Candidates sourced from the public NameMC skin catalogue. The plugin still verifies
 * every bulk username through Citizens/Mojang before exposing it in TAB, because a
 * NameMC catalogue entry can outlive a username change.
 */
final class UsernamePool {
    private UsernamePool() {}

    static final List<String> DEFAULT_NAMES = List.of(
            "yibbu", "manyero", "koakumaris", "prettysadtoday", "yeowun", "undespairing",
            "nx9_msmc_nzh", "xWeyzow", "Nospu", "0NLYTOBY", "EMPR7", "qRose_", "Conetic",
            "Niflu", "Frostivn", "4alex_", "cwsy", "roobay", "Luvcie", "WBBM", "Esoteric_Artist",
            "orphicparadox", "femboykittycat", "HD1", "LA_Lemons", "vtja", "Dcape07", "Whimzzical",
            "lack_of_skill", "z3nr", "Allyish", "samwqq", "Sebro35", "Thiago_Riv01", "_Unicorn_08",
            "SkinEditor", "hotcrime", "Levylzz", "sieqgewinnt", "strawberrycreaam", "Stefanio2014",
            "Kaelyn", "lordimtired", "kiireeiiu", "ITSPANDASAMA", "SCAMP1013", "SupremeStar_",
            "hugoisaura", "omri_real", "mwrder", "James_gamer_mc", "Squiggles_000", "PlasticBaggies",
            "ahxgi", "Snnowy", "torci", "Ce1estina", "Gewy", "Aeuros", "Anyes", "SCNNLD",
            "WoodlandHydra37", "youner", "_HisLily", "nobodylov3su", "cyw", "tedzillaa", "JinFrog2",
            "LittleVet", "IamStormz", "snuush", "Seumm", "M1dnightM1st", "Ym7_", "Ledion77",
            "D3xDSnow", "Anglestitch15", "blqckbear", "m_gp", "noeluv_zanny", "Ziiruk", "German",
            "_Hxlcyon", "Blubbery_Muffin", "Purrfectttt", "jeltude", "andwh", "_Wex__", "Ryoomen",
            "ryderds", "Kiramark", "OxyMoxy84", "vealentines", "suawa", "NycuS", "nevadaah",
            "SecretWadyy"
    );

    // The same NameMC catalogue is a good donor source because entries are skin-focused.
    static final List<String> DEFAULT_SKIN_DONORS = DEFAULT_NAMES;
}
