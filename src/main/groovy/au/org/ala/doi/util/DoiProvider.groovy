package au.org.ala.doi.util

enum DoiProvider {

    ANDS,
    DATACITE

    // list of at alternate names for the DoiProvider
    List<String> altNames = []

    static DoiProvider byName(String name) {
        // lookup the provider by enum name
        DoiProvider provider = values().find { it.name().equalsIgnoreCase(name) }

        // no match, then lookup by alternate name
        if (!provider) {
            provider = values().find { it.altNames.any { String altName -> altName.equalsIgnoreCase(name) } }
        }

        return provider
    }
}

