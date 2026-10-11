package com.gentlejournal.domain.model

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val emoji: String
) {
    SYSTEM("system",  "System default", "System default", "\uD83D\uDD04"),
    EN("en",          "English",        "English",        "\uD83C\uDDFA\uD83C\uDDF8"),
    DE("de",          "German",         "Deutsch",        "\uD83C\uDDE9\uD83C\uDDEA"),
    FR("fr",          "French",         "Fran\u00E7ais",  "\uD83C\uDDEB\uD83C\uDDF7"),
    ES("es",          "Spanish",        "Espa\u00F1ol",   "\uD83C\uDDEA\uD83C\uDDF8"),
    IT("it",          "Italian",        "Italiano",       "\uD83C\uDDEE\uD83C\uDDF9"),
    PT("pt",          "Portuguese",     "Portugu\u00EAs", "\uD83C\uDDF5\uD83C\uDDF9"),
    NL("nl",          "Dutch",          "Nederlands",     "\uD83C\uDDF3\uD83C\uDDF1"),
    EL("el",          "Greek",          "\u0395\u03BB\u03BB\u03B7\u03BD\u03B9\u03BA\u03AC", "\uD83C\uDDEC\uD83C\uDDF7"),
    PL("pl",          "Polish",         "Polski",         "\uD83C\uDDF5\uD83C\uDDF1"),
    SV("sv",          "Swedish",        "Svenska",        "\uD83C\uDDF8\uD83C\uDDEA"),
    DA("da",          "Danish",         "Dansk",          "\uD83C\uDDE9\uD83C\uDDF0"),
    RO("ro",          "Romanian",       "Rom\u00E2n\u0103", "\uD83C\uDDF7\uD83C\uDDF4");
}
