package au.org.ala.doi.providers

import au.org.ala.doi.exceptions.DoiMintingException
import au.org.ala.doi.util.ServiceResponse
import grails.web.mapping.LinkGenerator
import org.apache.http.HttpStatus
import org.springframework.beans.factory.annotation.Autowired

import static au.org.ala.doi.util.StateAssertions.*

/**
 * Abstract base class for interacting with DOI minting providers such as ANDS. Each individual provider will have its
 * own subclass to handle provider-specific payload construction and service invocation.
 */
abstract class DoiProviderService {

    @Autowired
    LinkGenerator grailsLinkGenerator

    /**
     * Mint a new DOI
     *
     * @param uuid Unique local identifier for the DOI - this is used to form the url that the doi will resolve to
     * @param metadata A map containing the provider-specific metadata, to be mapped onto the provider's web service api
     * @param customLandingPageUrl (defaults to null) A custom, application-specific landing page for the DOI to resolve to.
     * @return the new DOI if the minting process is successful
     * @throws DoiMintingException if the minting process fails for any reason
     * @throws IllegalArgumentException if the uuid or metadata are missing
     */
    String mintDoi(String uuid, Map metadata, String customLandingPageUrl = null) throws DoiMintingException {
        checkArgument uuid
        checkArgument metadata

        String landingPageUrl = generateLandingPageUrl(uuid, customLandingPageUrl)

        def requestPayload
        try {
            requestPayload = generateRequestPayload(uuid, metadata, landingPageUrl)
        } catch (Exception e) {
            log.error('Failed to construct the provider request payload', e)
            throw new DoiMintingException("Failed to construct the provider request payload", e)
        }

        ServiceResponse response
        try {
            response = invokeCreateService(requestPayload, landingPageUrl)
        } catch (Exception e) {
            log.error('Failed to invoke the provider mint web service', e)
            throw new DoiMintingException("Failed to invoke the provider mint web service", e)
        }

        if (response?.httpStatus == HttpStatus.SC_OK && response?.doi) {
            log.info("DOI ${response.doi} generated for local id ${uuid}: resolves to ${landingPageUrl}")

            response.doi
        } else {
            throw new DoiMintingException("Failed to invoke the provider web mint service: ${response.getErrorMessage()}")
        }
    }

    void updateDoi(String doi, String uuid, Map metadata = null, String customLandingPageUrl = null) throws DoiMintingException {
        checkArgument doi
        checkArgument uuid
        checkArgument metadata

        String landingPageUrl
        if (customLandingPageUrl != null) {
            landingPageUrl = generateLandingPageUrl(uuid, customLandingPageUrl)
        } else {
            landingPageUrl = null
        }

        def requestPayload
        if (metadata) {
            try {
                requestPayload = generateRequestPayload(uuid, metadata, landingPageUrl, doi)
            } catch (Exception e) {
                log.error('Failed to construct the provider request payload', e)
                throw new DoiMintingException("Failed to construct the provider request payload", e)
            }
        } else {
            requestPayload = null
        }

        ServiceResponse response
        try {
            response = invokeUpdateService(doi, requestPayload as Map, landingPageUrl)
        } catch (Exception e) {
            log.error('Failed to invoke the provider update web service', e)
            throw new DoiMintingException("Failed to invoke the provider update web service", e)
        }

        if (response?.httpStatus == HttpStatus.SC_OK && response?.doi) {
            log.info("DOI ${response.doi} generated for local id ${uuid}: resolves to ${landingPageUrl}")
        } else {
            throw new DoiMintingException("Failed to invoke the provider update web service: ${response.getErrorMessage()}")
        }
    }

    void deactivateDoi(String doi) throws DoiMintingException {
        checkArgument doi

        ServiceResponse response
        try {
            response = invokeDeactivateService(doi)
        } catch (Exception e) {
            log.error('Failed to invoke the provider deactivate web service', e)
            throw new DoiMintingException("Failed to invoke the provider deactivate web service", e)
        }

        if (response?.httpStatus == HttpStatus.SC_OK && response?.doi) {
            log.info("DOI ${response.doi} deactivated for ${doi}")
        } else {
            throw new DoiMintingException("Failed to invoke the provider deactivate web service: ${response.getErrorMessage()}")
        }
    }


    void activateDoi(String doi) throws DoiMintingException {
        checkArgument doi

        ServiceResponse response
        try {
            response = invokeActivateService(doi)
        } catch (Exception e) {
            log.error('Failed to invoke the provider activate web service', e)
            throw new DoiMintingException("Failed to invoke the provider activate web service", e)
        }

        if (response?.httpStatus == HttpStatus.SC_OK && response?.doi) {
            log.info("DOI ${response.doi} activated for ${doi}")
        } else {
            throw new DoiMintingException("Failed to invoke the provider activate web service: ${response.getErrorMessage()}")
        }
    }


    /**
     * Convert the provider metadata Map into whatever format is required for the web service (e.g. JSON, XML, etc)
     *
     * @param uuid The local unique identifier for the DOI
     * @param metadata The provider metadata required by the DOI Provider
     * @param landingPageUrl The landing page that the DOI needs to resolve to
     * @return The payload for the DOI minting request to be sent to the provider
     */
    abstract def generateRequestPayload(String uuid, Map metadata, String landingPageUrl, String doi = null)

    /**
     * Invoke the DOI provider's minting service, passing it the payload constructed in {@link #generateRequestPayload(java.util.Map, java.lang.String)}
     *
     * @param requestPayload The payload required by the provider
     * @param landingPageUrl The landing page that the DOI needs to resolve to
     * @return ServiceResponse object containing the doi if successful, or the error message, httpStatus and/or provider-specific error code if the call failed
     */
    abstract ServiceResponse invokeCreateService(requestPayload, String landingPageUrl)

    /**
     * Invoke the DOI provider's update service.  The request payload or landingPageUrl may be null, which indicates
     * that they do not need to be updated.
     *
     * @param requestPayload The payload required by the provider, may be null to indicate no update
     * @param landingPageUrl The landing page that the DOI needs to resolve to, may be null to indicate no update
     * @return ServiceResponse object containing the doi if successful, or the error message, httpStatus and/or provider-specific error code if the call failed
     */
    abstract ServiceResponse invokeUpdateService(String doi, Map requestPayload, String landingPageUrl)

    /**
     * Construct the final landing page url for the DOI. If a custom landing page is provided, that will be used,
     * otherwise a generic ALA DOI service landing page will be used.
     *
     * @param uuid The local unique identifier for the DOI
     * @param customLandingPageUrl A custom (application-specific) landing page to use for the DOI if desired (defaults to null)
     * @return The final landing page URL for the DOI
     */
    String generateLandingPageUrl(String uuid, String customLandingPageUrl = null) {
        checkArgument uuid

        customLandingPageUrl ?: grailsLinkGenerator.link(absolute: true, method: 'GET', controller: 'doiResolve', action: 'doi', id: uuid)
    }

    String getGenericLandingPageUrlPrefix() {
        grailsLinkGenerator.link(uri: '/', absolute: true, method: 'GET')
    }

    /**
     * Invoke the DOI provider's deactivate service.
     * @param doi The DOI to deactivate
     * @return ServiceResponse object containing the doi if successful, or the error message, httpStatus and/or provider-specific error code if the call failed
     */
    abstract ServiceResponse invokeDeactivateService(String doi)

    /**
     * Invoke the DOI provider's activate service.
     * @param doi The DOI to deactivate
     * @return ServiceResponse object containing the doi if successful, or the error message, httpStatus and/or provider-specific error code if the call failed
     */
    abstract ServiceResponse invokeActivateService(String doi)

}