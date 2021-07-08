package au.org.ala.doi

import au.org.ala.doi.util.DoiProvider
import com.google.common.io.BaseEncoding
import grails.converters.JSON
import org.grails.web.converters.marshaller.ObjectMarshaller
import org.grails.web.converters.marshaller.json.GenericJavaBeanMarshaller

class BootStrap {
    def messageSource

    def init = { servletContext ->

        messageSource.setBasenames(
                "file:///var/opt/atlas/i18n/doi/messages",
                "file:///opt/atlas/i18n/doi/messages",
                "WEB-INF/grails-app/i18n/messages",
                "classpath:messages"
        )

        JSON.registerObjectMarshaller(UUID) { uuid ->
            uuid.toString()
        }
        JSON.registerObjectMarshaller(DoiProvider) { provider ->
            provider.toString()
        }

        JSON.registerObjectMarshaller(Doi) { Doi doi ->
            [
                    id                  : doi.id,
                    fileSize            : doi.fileSize,
                    dateCreated         : doi.dateCreated,
                    providerMetadata    : doi.providerMetadata,
                    customLandingPageUrl: doi.customLandingPageUrl,
                    dateMinted          : doi.dateMinted,
                    uuid                : doi.uuid,
                    lastUpdated         : doi.lastUpdated,
                    active              : doi.active,
                    doi                 : doi.doi,
                    applicationMetadata : doi.applicationMetadata,
                    provider            : doi.provider,
                    title               : doi.title,
                    applicationUrl      : doi.applicationUrl,
                    fileHash            : doi.fileHash,
                    filename            : doi.filename,
                    contentType         : doi.contentType,
                    authors             : doi.authors,
                    licence             : doi.licence,
                    description         : doi.description
            ]
        }

//        JSON.registerObjectMarshaller(byte[]) { bytes ->
//            BaseEncoding.base16().encode(bytes)
//        }


    }
    def destroy = {
    }
}
