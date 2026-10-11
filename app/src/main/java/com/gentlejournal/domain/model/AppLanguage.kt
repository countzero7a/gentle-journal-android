package com.gentlejournal.domain.model

// nativeName is the language's own name; the SYSTEM label comes from string resources
enum class AppLanguage(
    val code: String,
    val nativeName: String,
    val emoji: String
) {
    SYSTEM("system",  "System default", "\uD83D\uDD04"),
    EN("en",          "English",        "\uD83C\uDDFA\uD83C\uDDF8"),
    DE("de",          "Deutsch",        "\uD83C\uDDE9\uD83C\uDDEA"),
    FR("fr",          "Fran\u00E7ais",  "\uD83C\uDDEB\uD83C\uDDF7"),
    ES("es",          "Espa\u00F1ol",   "\uD83C\uDDEA\uD83C\uDDF8"),
    IT("it",          "Italiano",       "\uD83C\uDDEE\uD83C\uDDF9"),
    PT("pt",          "Portugu\u00EAs", "\uD83C\uDDF5\uD83C\uDDF9"),
    NL("nl",          "Nederlands",     "\uD83C\uDDF3\uD83C\uDDF1"),
    EL("el",          "\u0395\u03BB\u03BB\u03B7\u03BD\u03B9\u03BA\u03AC", "\uD83C\uDDEC\uD83C\uDDF7"),
    PL("pl",          "Polski",         "\uD83C\uDDF5\uD83C\uDDF1"),
    SV("sv",          "Svenska",        "\uD83C\uDDF8\uD83C\uDDEA"),
    DA("da",          "Dansk",          "\uD83C\uDDE9\uD83C\uDDF0"),
    RO("ro",          "Rom\u00E2n\u0103", "\uD83C\uDDF7\uD83C\uDDF4");
}
