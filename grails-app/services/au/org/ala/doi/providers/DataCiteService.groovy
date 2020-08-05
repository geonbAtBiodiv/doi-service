package au.org.ala.doi.providers

import au.org.ala.doi.util.ServiceResponse
import com.github.jasminb.jsonapi.JSONAPIDocument
import org.apache.http.HttpStatus
import org.apache.http.impl.EnglishReasonPhraseCatalog
import org.gbif.api.model.common.DOI
import org.gbif.api.model.common.DoiData
import org.gbif.api.model.common.DoiStatus
import org.gbif.datacite.rest.client.model.DoiSimplifiedModel
import org.gbif.datacite.rest.client.model.EventType
import org.gbif.doi.metadata.datacite.*
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Creators.Creator
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Titles
import org.gbif.doi.service.DoiExistsException
import org.gbif.doi.service.DoiHttpException

import static org.gbif.doi.metadata.datacite.DataCiteMetadata.*
import static org.gbif.doi.metadata.datacite.DataCiteMetadata.RightsList.Rights
import static org.gbif.doi.metadata.datacite.DataCiteMetadata.Titles.Title

class DataCiteService extends DoiProviderService {
    def dataCiteClient
    def restDataCiteService
    def grailsApplication

    String getPrefix() {
        grailsApplication.config.getProperty('datacite.doi.service.prefix', String, "10.XXXXX")
    }

    String getShoulder() {
        grailsApplication.config.getProperty('datacite.doi.service.shoulder', String, "ala")
    }

    String getPublicationLang() {
        grailsApplication.config.getProperty('doi.publicationLang', String, "en")
    }

    def generateRequestPayload(String uuid, Map metadata, String landingPageUrl, String doi = null) {
        def dcMetadata = new DataCiteMetadata()
        // doi is a mandatory element in the schema, in a ANDS mint request the value is ignored
        // so here we generate one using prefix/shoulder.uuid
        def doiValue = doi ?: "${getPrefix()}/${getShoulder()}.${uuid}"
        dcMetadata.identifier = new Identifier(value: doiValue as String, identifierType: 'DOI')

        // Creators
        def creators = new Creators()
        if (metadata.creators) {
            creators.creator.addAll(metadata.creators.collect { creator -> new Creator(creatorName: new Creator.CreatorName(value: creator.name as String)) })
        }
        // We add also authors as creators like AndsService
        if (metadata.authors) {
            creators.creator.addAll(metadata.authors.collect { author -> new Creator(creatorName: new Creator.CreatorName(value: author as String)) })
        }
        dcMetadata.setCreators(creators)

        // Titles
        def titles = new Titles()
        if (metadata.titles) {
            for (def title in metadata.titles) {
                def dcTitle = new Title()
                dcTitle.setValue(title.title as String)
                titles.title.add(dcTitle)
            }
        }
        // Adding v3 title as AndsService
        if (metadata.title) {
            def dcTitle = new Title()
            dcTitle.setValue(metadata.title as String)
            titles.title.add(dcTitle)
        }
        if (metadata.subtitle) {
            def sub = new Title()
            sub.setValue(metadata.subtitle as String)
            sub.setTitleType(TitleType.SUBTITLE)
            titles.title.add(sub)
        }
        dcMetadata.setTitles(titles)

        def publisher = new Publisher()
        publisher.setValue(metadata.publisher as String)
        dcMetadata.setPublisher(publisher)

        dcMetadata.setPublicationYear((String) metadata.publicationYear ?:
                Calendar.getInstance().get(Calendar.YEAR).toString())

        // Subjects
        if (metadata.subjects) {
            def subjects = new Subjects()
            metadata.subjects.each {
                def subject = new Subjects.Subject()
                subject.setValue((String) it)
                subjects.subject.add(subject)
            }
            dcMetadata.setSubjects(subjects)
        }

        // Contributors
        if (metadata.contributors) {
            def contributors = new Contributors()
            for (contrib in metadata.contributors) {
                def contributor = new Contributors.Contributor()
                def contributorType = ContributorType.fromValue(contrib.type as String)
                contributor.setContributorType(contributorType)
                def cName = new Contributors.Contributor.ContributorName()
                cName.setValue(contrib.name as String)
                contributor.setContributorName(cName)
                contributors.contributor.add(contributor)
            }
            dcMetadata.setContributors(contributors)
        }


        // Created dates
        if (metadata.createdDate) {
            def dates = new Dates()
            def createdDate = new Dates.Date()
            createdDate.setDateType(DateType.CREATED)
            createdDate.setValue(metadata.createdDate as String)
            dates.date.add(createdDate)
            dcMetadata.setDates(dates)
        }

        // Rights
        if (metadata.rights) {
            def rightsList = new RightsList()
            metadata.rights.each {
                def rights = new Rights()
                rights.setValue((String) it)
                rightsList.rights.add(rights)
            }
            dcMetadata.setRightsList(rightsList)
        }

        dcMetadata.setLanguage(getPublicationLang())

        // ResourceType
        def resourceType = new DataCiteMetadata.ResourceType()
        def resourceTypeGeneral = ResourceType.fromValue(metadata.resourceType as String)
        resourceType.setValue(metadata.resourceText as String)
        resourceType.setResourceTypeGeneral(resourceTypeGeneral)
        dcMetadata.setResourceType(resourceType)

        // Descriptions
        if (metadata.descriptions) {
            def descriptions = new Descriptions()
            metadata.descriptions.each {
                def description = new Descriptions.Description()
                def descriptionType = DescriptionType.fromValue(it.type as String)
                description.getContent().add(it.text as String)
                description.setDescriptionType(descriptionType)
                descriptions.description.add(description)
            }
            dcMetadata.setDescriptions(descriptions)
        }

        return dcMetadata
    }

    @Override
    ServiceResponse invokeCreateService(Object requestPayload, String landingPageUrl) {
        def dcMetadata = requestPayload as DataCiteMetadata
        def doiS = dcMetadata.getIdentifier().getValue()
        log.debug "Creating doi $doiS"
        def doi = new DOI(doiS)
        try {
            restDataCiteService.register(doi, new URI(landingPageUrl), dcMetadata)
            return successResponse(doiS)
        } catch (Exception e) {
            return processErrorResponse(doi, e)
        }
    }

    @Override
    ServiceResponse invokeUpdateService(String doiS, Map requestPayload, String landingPageUrl) {
        log.debug "Updating doi $doi"
        def dcMetadata = requestPayload as DataCiteMetadata
        def doi = new DOI(doiS)
        try {
            restDataCiteService.update(doi, new URI(landingPageUrl))
            restDataCiteService.update(doi, dcMetadata)
            return successResponse(doiS)
        } catch (Exception e) {
            return processErrorResponse(doi, e)
        }
    }

    @Override
    ServiceResponse invokeDeactivateService(String doi) {
        log.debug "Deactivating doi $doi"
        return updateStatus(doi, EventType.HIDE)
    }

    // DOI (EZID) state : DataCite Equivalent
    // --------------------------------------
    // REGISTERED (public): Findable
    // RESERVED (reserved): Draft
    // DELETED (unavailable): Registered
    // NEW (-) : ()
    // FAILED (-): ()
    private ServiceResponse updateStatus(String doiS, EventType event) {
        def doi = new DOI(doiS)
        def response
        DoiData doiData = restDataCiteService.resolve(doi)
        if (event == EventType.HIDE  && (doiData.getStatus() == DoiStatus.RESERVED ||
                doiData.getStatus() == DoiStatus.DELETED)
        ) {
            // As with ANDS service, when already deactivated we return success
            log.debug "Deactivated doi $doi"
            response =  successResponse(doiS)
        } else if (event == EventType.PUBLISH  && doiData.getStatus() == DoiStatus.REGISTERED ) {
            // Same here
            log.debug "Activated doi $doi"
            response = successResponse(doiS)
        } else if (doiData.getStatus() != DoiStatus.RESERVED && doiData.getStatus() != DoiStatus.REGISTERED) {
            response = new ServiceResponse(HttpStatus.SC_BAD_REQUEST, "Only a reserved/registered doi can be updated. DOI "
                    + doi.getDoiName() + " status is " + doiData.getStatus(), null)
        } else {
            try {
                DoiSimplifiedModel model = new DoiSimplifiedModel()
                model.setDoi(doi.getDoiName())
                model.setUrl(doiData.getTarget().toString())
                model.setEvent(event.toString())
                JSONAPIDocument<DoiSimplifiedModel> jsonApiWrapper = new JSONAPIDocument(model)
                dataCiteClient.updateDoi(doi.getDoiName(), jsonApiWrapper)
                response = successResponse(doiS)
            } catch (Exception e) {
                response = processErrorResponse(doi, e)
            }
        }
        return response
    }

    @Override
    ServiceResponse invokeActivateService(String doi) {
        log.debug "Activating doi $doi"
        return updateStatus(doi, EventType.PUBLISH)
    }

    ServiceResponse successResponse(String doi) {
        def response = new ServiceResponse(HttpStatus.SC_OK, '', "")
        log.info("Success processing DOI " + doi)
        response.doi = doi
        return response
    }

    ServiceResponse processErrorResponse(DOI doi, Exception e) {
        if (e instanceof DoiExistsException)
            return new ServiceResponse(HttpStatus.SC_BAD_REQUEST, e.getMessage(), e.getCause().toString())
        else if (e instanceof DoiHttpException) {
            def statusMessage = EnglishReasonPhraseCatalog.INSTANCE.getReason(e.getStatus(), null)
            log.error("Error processing DOI "
            + doi.getDoiName() + " with http status " + e.getStatus())
            return new ServiceResponse(e.getStatus(), statusMessage, e.getCause().toString())
        }
        log.error("Error processing DOI "
            + doi.getDoiName() + ": " + e.getMessage())
        return new ServiceResponse(HttpStatus.SC_BAD_REQUEST, e.getMessage(),"")
    }
}
