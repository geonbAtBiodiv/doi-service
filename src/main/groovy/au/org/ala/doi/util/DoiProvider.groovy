package au.org.ala.doi.util

enum DoiProvider {

    ANDS,
    DATACITE

    static DoiProvider byName(String name) {
        values().find { it.name().equalsIgnoreCase(name) }
    }
}