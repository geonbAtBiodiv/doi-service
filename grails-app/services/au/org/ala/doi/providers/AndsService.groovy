package au.org.ala.doi.providers

import au.org.ala.doi.RestService
import au.org.ala.doi.exceptions.DoiMintingException
import au.org.ala.doi.util.ServiceResponse
import groovy.xml.MarkupBuilder
import groovyx.net.http.ContentType
import org.apache.http.HttpStatus
import org.apache.http.impl.EnglishReasonPhraseCatalog

/**
 * ANDS service documentation can be found here: http://ands.org.au/services/cmd-technical-document.pdf
 * <p/>
 * ANDS schema documentation can be found here: https://schema.datacite.org/meta/kernel-3/doc/DataCite-MetadataKernel_v3.1.pdf
 */
class AndsService extends DoiProviderService {
    static final String DATA_CITE_XSD_VERSION = "3"
    static final String DATA_CITE_XSD = "http://schema.datacite.org/meta/kernel-${DATA_CITE_XSD_VERSION}/metadata.xsd"

    static final String ANDS_RESPONSE_STATUS_OK = "MT090"
    static final String ANDS_RESPONSE_STATUS_DEAD = "MT091"
    static final String ANDS_RESPONSE_MINT_SUCCESS = "MT001"
    static final String ANDS_RESPONSE_UPDATE_SUCCESS = "MT002"
    static final String ANDS_RESPONSE_DEACTIVATE_SUCCESS = "MT003"
    static final String ANDS_RESPONSE_ACTIVATE_SUCCESS = "MT004"
    static final String ANDS_RESPONSE_UNEXPECTED_ERROR = "MT010"
    static final String ANDS_DEAD_STATUS_CODE = "E001"

    static final String ANDS_ALREADY_ACTIVE_ERROR_MESSAGE = "not set to INACTIVE so cannot activate it"
    static final String ANDS_ALREADY_INACTIVE_ERROR_MESSAGE = "not set to ACTIVE so cannot deactivate it"


    static final int ANDS_UNAVAILABLE_CODE = 0

    def grailsApplication
    RestService restService

    Map serviceStatus() {
        String andsUrl = "${grailsApplication.config.ands.doi.service.url}status.json"

        Map status = [:]
        try {
            Map response = restService.get(andsUrl, ContentType.JSON, ContentType.JSON)
            if ((response.status as int) == HttpStatus.SC_OK) {
                status.statusCode = response?.data?.response.responsecode
                status.message = "${response?.data?.response.message} - ${response?.data?.response.verbosemessage}"
            } else {
                status.statusCode = response.status
                status.message = EnglishReasonPhraseCatalog.INSTANCE.getReason(response.status, null)
            }
        } catch (Exception e) {
            status.statusCode = ANDS_DEAD_STATUS_CODE
            status.message = e.getMessage()
            log.error "DOI Service health check failed", e
        }

        status
    }

    ServiceResponse invokeCreateService(requestXml, String landingPageUrl) throws DoiMintingException {
        ServiceResponse result

        Map andsServiceStatus = serviceStatus()
        if (andsServiceStatus.statusCode == ANDS_RESPONSE_STATUS_OK) {
            log.debug "Requesting new DOI from ANDS..."
            // The ANDS URL must have a trailing slash or you get an empty response back
            String andsUrl = "${grailsApplication.config.ands.doi.service.url}mint.json/"
            String appId = "${grailsApplication.config.ands.doi.app.id}"

            String secret = "${appId}:${grailsApplication.config.ands.doi.key}".encodeAsBase64()

            Map query = [app_id: "${appId}", url: landingPageUrl]
            Map headers = [Accept: ContentType.JSON, Authorization: "Basic ${secret}"]

            Map response = restService.post(andsUrl, [xml: requestXml], ContentType.JSON, ContentType.URLENC, headers, query)

            if (response.status as int == HttpStatus.SC_OK) {
                def json = response.data
                log.debug "DOI response = ${json}"

                if (json.response.responsecode == ANDS_RESPONSE_MINT_SUCCESS) {
                    log.debug "Minted new doi ${json.response.doi}"
                    result = new ServiceResponse(json.response.doi)
                } else {
                    result = new ServiceResponse(HttpStatus.SC_OK, "${json?.response?.message}: ${json?.response?.verbosemessage}", json.response.responsecode)
                }
            } else {
                result = new ServiceResponse(response.status, EnglishReasonPhraseCatalog.INSTANCE.getReason(response.status, null))
            }
        } else {
            result = new ServiceResponse(ANDS_UNAVAILABLE_CODE, "The ANDS DOI minting service is not available.")
        }

        result
    }

    ServiceResponse invokeUpdateService(String doi, Map requestXml, String landingPageUrl) throws DoiMintingException {
        ServiceResponse result

        Map andsServiceStatus = serviceStatus()
        if (andsServiceStatus.statusCode == ANDS_RESPONSE_STATUS_OK) {
            log.debug "Requesting new DOI from ANDS..."
            // The ANDS URL must have a trailing slash or you get an empty response back
            //https://researchdata.ands.org.au/api/doi/update.{response_type}/?app_id={app_id}&doi={doi}&url={url}
            String andsUrl = "${grailsApplication.config.ands.doi.service.url}update.json/"
            String appId = "${grailsApplication.config.ands.doi.app.id}"

            String secret = "${appId}:${grailsApplication.config.ands.doi.key}".encodeAsBase64()

            Map query = [app_id: "${appId}", doi: doi]
            if (landingPageUrl != null) {
                query << [url: landingPageUrl]
            }

            Map headers = [Accept: ContentType.JSON, Authorization: "Basic ${secret}"]

            Map response = restService.post(andsUrl, requestXml ? [xml: requestXml] : null, ContentType.JSON, ContentType.URLENC, headers, query)

            if (response.status as int == HttpStatus.SC_OK) {
                def json = response.data
                log.debug "DOI response = ${json}"

                if (json.response.responsecode == ANDS_RESPONSE_UPDATE_SUCCESS) {
                    log.debug "Updated doi $doi"
                    result = new ServiceResponse(json.response.doi)
                } else {
                    result = new ServiceResponse(HttpStatus.SC_OK, "${json?.response?.message}: ${json?.response?.verbosemessage}", json.response.responsecode)
                }
            } else {
                result = new ServiceResponse(response.status, EnglishReasonPhraseCatalog.INSTANCE.getReason(response.status, null))
            }
        } else {
            result = new ServiceResponse(ANDS_UNAVAILABLE_CODE, "The ANDS DOI minting service is not available.")
        }

        result
    }

    ServiceResponse invokeDeactivateService(String doi) throws DoiMintingException {
        ServiceResponse result

        Map andsServiceStatus = serviceStatus()
        if (andsServiceStatus.statusCode == ANDS_RESPONSE_STATUS_OK) {
            log.debug "Deactivating ANDS DOI."
            // The ANDS URL must have a trailing slash or you get an empty response back
            //https://researchdata.ands.org.au/api/doi/update.{response_type}/?app_id={app_id}&doi={doi}&url={url}
            String andsUrl = "${grailsApplication.config.ands.doi.service.url}deactivate.json/"
            String appId = "${grailsApplication.config.ands.doi.app.id}"

            String secret = "${appId}:${grailsApplication.config.ands.doi.key}".encodeAsBase64()

            Map query = [app_id: "${appId}", doi: doi]

            Map headers = [Accept: ContentType.JSON, Authorization: "Basic ${secret}"]

            Map response = restService.get(andsUrl, ContentType.JSON, ContentType.URLENC, headers, query)

            if (response.status as int == HttpStatus.SC_OK) {
                def json = response.data
                log.debug "DOI response = ${json}"

                if (json.response.responsecode == ANDS_RESPONSE_DEACTIVATE_SUCCESS) {
                    log.debug "Deactivated doi $doi"
                    result = new ServiceResponse(json.response.doi)
                } else if (json.response.responsecode == ANDS_RESPONSE_UNEXPECTED_ERROR
                    && json?.response?.verbosemessage?.contains(ANDS_ALREADY_INACTIVE_ERROR_MESSAGE)    ) {
                    //TODO Hack parse doi already deactivated error message and treat it as a successful call
                    // As of 18 Jan 2018 ANDS does not provide an API call to return the active status for a DOI (lame)
                    // https://jira.ands.org.au/browse/SD-35566
                    log.debug "Deactivated doi $doi"
                    result = new ServiceResponse(json.response.doi)
                } else {
                    result = new ServiceResponse(HttpStatus.SC_OK, "${json?.response?.message}: ${json?.response?.verbosemessage}", json.response.responsecode)
                }
            } else {
                result = new ServiceResponse(response.status, EnglishReasonPhraseCatalog.INSTANCE.getReason(response.status, null))
            }
        } else {
            result = new ServiceResponse(ANDS_UNAVAILABLE_CODE, "The ANDS DOI minting service is not available.")
        }

        result
    }

    ServiceResponse invokeActivateService(String doi) throws DoiMintingException {
        ServiceResponse result

        Map andsServiceStatus = serviceStatus()
        if (andsServiceStatus.statusCode == ANDS_RESPONSE_STATUS_OK) {
            log.debug "Activating ANDS DOI."
            // The ANDS URL must have a trailing slash or you get an empty response back
            //https://researchdata.ands.org.au/api/doi/update.{response_type}/?app_id={app_id}&doi={doi}&url={url}
            String andsUrl = "${grailsApplication.config.ands.doi.service.url}activate.json/"
            String appId = "${grailsApplication.config.ands.doi.app.id}"

            String secret = "${appId}:${grailsApplication.config.ands.doi.key}".encodeAsBase64()

            Map query = [app_id: "${appId}", doi: doi]

            Map headers = [Accept: ContentType.JSON, Authorization: "Basic ${secret}"]

            Map response = restService.get(andsUrl, ContentType.JSON, ContentType.URLENC, headers, query)

            if (response.status as int == HttpStatus.SC_OK) {
                def json = response.data
                log.debug "DOI response = ${json}"

                if (json.response.responsecode == ANDS_RESPONSE_ACTIVATE_SUCCESS) {
                    log.debug "Activated doi $doi"
                    result = new ServiceResponse(json.response.doi)
                } else if (json.response.responsecode == ANDS_RESPONSE_UNEXPECTED_ERROR
                        && json?.response?.verbosemessage?.contains(ANDS_ALREADY_ACTIVE_ERROR_MESSAGE)    ) {
                    //TODO Hack parse doi already deactivated error message and treat it as a successful call
                    // As of 18 Jan 2018 ANDS does not provide an API call to return the active status for a DOI (lame)
                    // https://jira.ands.org.au/browse/SD-35566
                    log.debug "Activated doi $doi"
                    result = new ServiceResponse(json.response.doi)
                } else { //TODO Hack parse doi already deactivated error message and treat it as a successful call
                    result = new ServiceResponse(HttpStatus.SC_OK, "${json?.response?.message}: ${json?.response?.verbosemessage}", json.response.responsecode)
                }
            } else {
                result = new ServiceResponse(response.status, EnglishReasonPhraseCatalog.INSTANCE.getReason(response.status, null))
            }
        } else {
            result = new ServiceResponse(ANDS_UNAVAILABLE_CODE, "The ANDS DOI minting service is not available.")
        }

        result
    }


    def generateRequestPayload(String uuid, Map metadata, String landingPageUrl, String doi = null) {
        StringWriter writer = new StringWriter()

        def doiValue = doi ?: "10.5072/example" // doi is a mandatory element in the schema, in a mint request the value is ignored

        MarkupBuilder xml = new MarkupBuilder(writer)

        xml.mkp.xmlDeclaration(version: "1.0", encoding: "utf-8")

        xml.resource(xmlns: "http://datacite.org/schema/kernel-${DATA_CITE_XSD_VERSION}",
                "xmlns:xsi": "http://www.w3.org/2001/XMLSchema-instance",
                "xsi:schemaLocation": "http://datacite.org/schema/kernel-${DATA_CITE_XSD_VERSION} ${DATA_CITE_XSD}") {
            identifier(identifierType: "DOI", doiValue)
            creators() {
                creator() {
                    metadata.authors.each {
                        creatorName(it)
                    }
                }
            }
            titles() {
                title("${metadata.title}")
                if (metadata.subtitle) {
                    title(titleType: "Subtitle", metadata.subtitle)
                }
            }
            publisher(metadata.publisher)
            publicationYear(metadata.publicationYear ?: Calendar.getInstance().get(Calendar.YEAR))
            if (metadata.subjects) {
                subjects() {
                    metadata.subjects.each {
                        subject(it)
                    }
                }
            }
            if (metadata.contributors) {
                contributors() {
                    metadata.contributors.each { contrib ->
                        contributor(contributorType: contrib.type) {
                            contributorName(contrib.name)
                        }
                    }
                }
            }
            if (metadata.createdDate) {
                dates() {
                    date(dateType: "Created", metadata.createdDate)
                }
            }
            if (metadata.rights) {
                rightsList() {
                    metadata.rights.each {
                        rights(it)
                    }
                }
            }
            language("en")
            resourceType(resourceTypeGeneral: metadata.resourceType, metadata.resourceText)
            if (metadata.descriptions) {
                descriptions() {
                    metadata.descriptions.each {
                        description(descriptionType: it.type, it.text)
                    }
                }
            }
        }

        writer.toString()
    }
}
