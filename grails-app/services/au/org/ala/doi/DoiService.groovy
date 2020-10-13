package au.org.ala.doi

import au.org.ala.doi.exceptions.DoiMintingException
import au.org.ala.doi.exceptions.DoiNotFoundException
import au.org.ala.doi.exceptions.DoiUpdateException
import au.org.ala.doi.exceptions.DoiValidationException
import au.org.ala.doi.providers.AndsService
import au.org.ala.doi.providers.DataCiteService
import au.org.ala.doi.providers.DoiProviderService
import au.org.ala.doi.providers.MockService
import au.org.ala.doi.storage.Storage
import au.org.ala.doi.util.DoiProvider
import com.google.common.collect.Maps
import grails.core.GrailsApplication
import grails.gorm.PagedResultList
import grails.gorm.transactions.ReadOnly
import grails.gorm.transactions.Transactional
import org.springframework.validation.Errors
import org.springframework.web.multipart.MultipartFile

import static au.org.ala.doi.util.StateAssertions.checkArgument
import static au.org.ala.doi.util.Utils.isUuid

class DoiService extends BaseDataAccessService {

    GrailsApplication grailsApplication
    AndsService andsService
    DataCiteService dataCiteService
    MockService mockService
    Storage storage
    EmailService emailService
    DoiSearchService doiSearchService

//    @Value('${doi.service.mock:false}')
    boolean isUseMockDoiService() {
        grailsApplication.config.getProperty('doi.service.mock', Boolean, false)
    }

    @Transactional
    MintResponse mintDoi(DoiProvider provider, Map providerMetadata, String title, String authors, String description,
                List<String> licence, String applicationUrl, String fileUrl, MultipartFile file,
                Map applicationMetadata = [:], String customLandingPageUrl = null, String defaultDoi = null,
                String userId = null, Boolean active = true, List<String> authorisedRoles=[], String displayTemplate = null) {
        checkArgument provider
        log.debug("Trying to mint with provider: $provider")
        if(!defaultDoi) {
            checkArgument providerMetadata, "No provider metadata has been sent"
        }
        checkArgument applicationUrl, "No url to the original application has been sent"

        if (defaultDoi && Doi.countByDoi(defaultDoi) > 0) {
            throw new DoiMintingException("DOI $defaultDoi already exists in service!")
        }

        UUID uuid = UUID.randomUUID()

//        String contentType = file ? file.contentType : new URL(fileUrl).openConnection().contentType
        Doi entity = new Doi(uuid: uuid, customLandingPageUrl: customLandingPageUrl, dateMinted: new Date(),
                title: title, authors: authors, description: description, licence: licence, provider: provider,
                providerMetadata: providerMetadata, applicationMetadata: applicationMetadata,
                applicationUrl: applicationUrl, userId: userId, authorisedRoles: authorisedRoles,
                displayTemplate: displayTemplate, active: active)

        entity.doi = "dummyForValidation" // doi is a mandatory field, so we set a temp value to validate the rest of the entity
        if (entity.validate()) {
            if (file) {
                storage.storeFileForDoi(entity, file)
            } else if (fileUrl) {
                storage.storeFileForDoi(entity, fileUrl)
            }

            String uuidString = uuid.toString()
            String doi = defaultDoi ?:  getProviderService(provider).mintDoi(uuidString, providerMetadata, customLandingPageUrl)
            entity.doi = doi

            if(!active) { // DOIs are active by default, no need of explicit activation
                getProviderService(provider).deactivateDoi(doi)
            }

            boolean success = save entity

            MintResponse result
            if (!success) {
                log.error("A DOI was generated successfully through ${provider}, but the DB record failed to save. No default landing page will exist for this DOI!")
                sendPostDOICreationErrorEmail(entity.doi, entity.errors)
                result = new MintResponse(uuid: null, doi: doi, error: "A DOI was generated, but the server failed to save to the local DB. No default landing page will exist for this DOI!", status: "error")
            } else {
                result = new MintResponse(uuid: uuid, doi: doi, landingPage: getProviderService(provider).generateLandingPageUrl(uuidString, customLandingPageUrl)
                          , doiServiceLandingPage: getProviderService(provider).generateLandingPageUrl(uuidString, null), status: "ok")
            }

            return result
        } else {
            // should never happen, so we can just throw a generic exception which will result in a HTTP 500 response
            throw new DoiValidationException(entity.uuid, entity.doi, entity.errors)
        }
    }



    @Transactional
    def updateDoi(String id, Map objectToBind, MultipartFile multipartFile) {
        Doi instance = isUuid(id) ? findByUuid(id) : findByDoi(id)
        if (!instance) {
            transactionStatus.setRollbackOnly()
            throw new DoiNotFoundException(id)
        }

        def url = objectToBind.remove('fileUrl')
        if (instance.filename && (multipartFile || url)) {
            throw new DoiUpdateException("$id already has a file associated and a ${multipartFile ? 'file' : 'URL'} was provided!")
        }

        // can't change id, uuid or doi on update
        final allowedProperties = Doi.ALLOWED_UPDATABLE_PROPERTIES
        final disallowedProperties = objectToBind.keySet() - allowedProperties
        if (disallowedProperties) {
            log.warn('Dropping {} from update request', disallowedProperties)
        }
        objectToBind.keySet().retainAll(allowedProperties)
        /*
        json.providerMetadata, json.title,
        json.authors, json.description, json.applicationUrl, json.fileUrl, file, json.applicationMetadata,
        json.customLandingPageUrl
        */

        def newProviderMetadata = objectToBind['providerMetadata'] as Map
        def newCustomLandingPageUrl = objectToBind['customLandingPageUrl']
        def newActive = objectToBind['active']
        def updateProviderMetadata = newProviderMetadata != null && instance.providerMetadata != newProviderMetadata
        def updateActive = newActive != null && instance.active != newActive
        if (updateProviderMetadata && log.isDebugEnabled()) {
            log.debug("updateDoi: providerMetadata difference: {}", Maps.difference(instance.providerMetadata, newProviderMetadata))
        }
        def updateCustomLandingPageUrl = newCustomLandingPageUrl != null && instance.customLandingPageUrl != newCustomLandingPageUrl

        instance.properties = objectToBind

        if (!instance.validate()) {
            transactionStatus.setRollbackOnly()
            throw new DoiValidationException(instance.uuid, instance.doi, instance.errors)
//            respond instance.errors, view:'edit' // STATUS CODE 422
        }

        if (multipartFile) {
            storage.storeFileForDoi(instance, multipartFile)
        } else if (url) {
            storage.storeFileForDoi(instance, url)
        }

        if (updateProviderMetadata || updateCustomLandingPageUrl) {
            getProviderService(instance.provider).updateDoi(instance.doi, instance.uuid.toString(), updateProviderMetadata ? instance.providerMetadata : null, updateCustomLandingPageUrl ? instance.customLandingPageUrl : null)
        }

        if(updateActive) {
            if(newActive) {
                // activate ANDS DOI
                getProviderService(instance.provider).activateDoi(instance.doi)
            } else {
                // deactivate ANDS DOI
                getProviderService(instance.provider).deactivateDoi(instance.doi)
            }
        }

        if (!instance.save()) {
            log.error("A DOI update for ${instance.doi} was successful through ${instance.provider}, but the DB record failed to save!")
            sendPostDOICreationErrorEmail(instance.doi, instance.errors)
        }

        return instance
    }


    void sendPostDOICreationErrorEmail(String doi, Errors errors) {
        def recipient = grailsApplication.config.getProperty('support.email', String, 'support@ala.org.au')
        def noreply = grailsApplication.config.getProperty('support.noreply', String, 'no-reply@ala.org.au')
        emailService.sendDoiFailureEmail(recipient, "doiservice <$noreply>", doi, errors)
    }

    Doi findByUuid(String uuid) {
        checkArgument uuid

        Doi.findByUuid(UUID.fromString(uuid))
    }

    Doi findByDoi(String doi) {
        checkArgument doi

        Doi.findByDoi(doi)
    }

    @ReadOnly
    PagedResultList listDois(int pageSize, int startFrom, String sortBy = "dateMinted", String sortOrder = "desc", Map eqParams = null) {
        def criteria = Doi.createCriteria()
        criteria.list (max: pageSize, offset: startFrom) {
            if (eqParams) {
                allEq(eqParams)
            }
            order(sortBy, sortOrder)
        }
    }

    // Replace this with a factory if/when other DOI providers are supported
    private DoiProviderService getProviderService(DoiProvider provider) {

        if (useMockDoiService) {
            log.info("Using mock provider service")
            return mockService
        }

        switch (provider) {
            case DoiProvider.ANDS:
                return andsService
            case DoiProvider.DATACITE:
                return dataCiteService
        }

        throw new MissingResourceException("unsupported DoiProvider ${provider}", DoiService as String, provider as String)
    }
}
