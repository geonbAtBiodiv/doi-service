package au.org.ala.doi.util

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct

/**
 * setup mapping of DoiProvider alternate names
 */
@Component
@Slf4j
class DoiProviderMapping {

    @Value('#{${doi.service.provider.mapping:{ALA: \'DATACITE\' }}}')
    Map<String, String> doiProviderMapping

    @PostConstruct
    void init() {

        doiProviderMapping?.each { String altName, String providerName ->

            DoiProvider doiProvider = DoiProvider.valueOf(providerName)

            boolean existingAltName = DoiProvider.values().any { DoiProvider provider ->

                if (provider.name().equalsIgnoreCase(altName)) {
                    log.warn "provider alternate name '${altName}' matches DoiProvider.${provider.name()}"
                    return true
                } else if (provider.altNames.any { alt -> alt.equalsIgnoreCase(altName) }) {
                    log.warn "provider alternate name '${altName}' already used by DoiProvider.${provider.name()}"
                    return true
                }

                return false
            }

            if (!existingAltName) {
                doiProvider.altNames << altName
            }
        }
    }
}