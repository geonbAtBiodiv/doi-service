package au.org.ala.doi.providers

import au.org.ala.doi.exceptions.DoiMintingException
import au.org.ala.doi.util.ServiceResponse
import grails.testing.services.ServiceUnitTest
import grails.util.Holders
import grails.web.mapping.LinkGenerator
import org.apache.http.HttpStatus
import org.gbif.api.model.common.DoiData
import org.gbif.api.model.common.DoiStatus
import org.gbif.datacite.rest.client.DataCiteClient
import org.gbif.doi.metadata.datacite.DataCiteMetadata
import org.gbif.doi.metadata.datacite.DateType
import org.gbif.doi.metadata.datacite.TitleType
import org.gbif.doi.service.DoiHttpException
import org.gbif.doi.service.datacite.DataCiteValidator
import org.gbif.doi.service.datacite.RestJsonApiDataCiteService
import org.grails.spring.beans.factory.InstanceFactoryBean
import spock.lang.Specification
import spock.lang.Unroll


@Unroll
class DataCiteServiceSpec extends Specification implements ServiceUnitTest<DataCiteService> {

    def setup() {
        defineBeans {
            grailsLinkGenerator(InstanceFactoryBean, Stub(LinkGenerator), LinkGenerator)
        }

        service.dataCiteClient = Mock(DataCiteClient)
        service.restDataCiteService = Mock(RestJsonApiDataCiteService)
    }


    def "serviceStatus should return the 'OK' status code if the call to the DataCite service run correctly"() {
        setup:
        def metadata = Mock(DataCiteMetadata)
        metadata.getIdentifier() >> Mock(DataCiteMetadata.Identifier)
        metadata.getIdentifier().getValue() >> "10.1000/example1"
        service.restDataCiteService.register(_, _, _) >> {}

        when:
        ServiceResponse response = service.invokeCreateService(metadata, "bla")

        then:
        response.getHttpStatus() == HttpStatus.SC_OK
    }

    def "serviceStatus should return the 'BAD_REQUEST' status code if the call to the DataCite gives some exception"() {
        setup:
        def metadata = Mock(DataCiteMetadata)
        metadata.getIdentifier() >> Mock(DataCiteMetadata.Identifier)
        metadata.getIdentifier().getValue() >> "10.1000/example2"
        service.restDataCiteService.register(_, _, _) >> { throw new DoiHttpException() }

        when:
        ServiceResponse response = service.invokeCreateService(metadata, "bla")

        then:
        response.getHttpStatus() == HttpStatus.SC_BAD_REQUEST
    }

    def "deactivate should not fail when the DOI status is already hidden" () {
        setup:
        service.restDataCiteService.resolve(_) >> new DoiData(status)

        when:
        service.deactivateDoi("10.1000/example3")

        then:
        noExceptionThrown()

        where:
        status << [DoiStatus.RESERVED, DoiStatus.DELETED]
    }

    def "activate should not fail when the DOI status is already public" () {
        setup:
        service.restDataCiteService.resolve(_) >> new DoiData(DoiStatus.REGISTERED)

        when:
        service.activateDoi("10.1000/example4")

        then:
        noExceptionThrown()
    }

    def "activate should fail when the DOI status is deleted" () {
        setup:
        service.restDataCiteService.resolve(_) >> new DoiData(DoiStatus.DELETED)

        when:
        service.activateDoi("10.1000/example5")

        then:
        thrown DoiMintingException
    }

    def "generateRequestPayload should map all metadata fields to the DataCite xml schema"() {
        given:
        Map metadataMap = [:]
        def uuid = UUID.randomUUID()
        metadataMap.authors = ["author1", "author2"]
        metadataMap.title = "publicationTitle"
        metadataMap.subtitle = "publicationSubtitle"
        metadataMap.publisher = "publisherName"
        metadataMap.publicationYear = "2016"
        metadataMap.subjects = ["subject1", "subject2"]
        metadataMap.contributors = [[type: "Editor", name: "bob"], [type: "Editor", name: "jill"]]
        metadataMap.resourceType = "Text"
        metadataMap.resourceText = "resourceText"
        metadataMap.descriptions = [[type: "Other", text: "description1"], [type: "Other", text: "description2"]]
        metadataMap.createdDate = "createdDate"
        metadataMap.rights = ["rights statement 1", "rights statement 2"]

        when:
        DataCiteMetadata dcMetadata = service.generateRequestPayload(uuid.toString(), metadataMap, "landingPageUrl")

        then:
        dcMetadata.identifier.getIdentifierType() == "DOI"
        dcMetadata.identifier.getValue() == "${Holders.config.datacite.doi.service.prefix}/${Holders.config.datacite.doi.service.shoulder}.${uuid}"
        dcMetadata.titles.getTitle().get(0).getValue() == metadataMap.title
        dcMetadata.titles.getTitle().get(1).getValue() == metadataMap.subtitle
        dcMetadata.titles.getTitle().get(1).getTitleType() == TitleType.SUBTITLE
        dcMetadata.creators.getCreator().get(0).getCreatorName().getValue() == (metadataMap.authors as String[])[0]
        dcMetadata.creators.getCreator().get(1).getCreatorName().getValue() == (metadataMap.authors as String[])[1]
        dcMetadata.publisher.getValue() == metadataMap.publisher
        dcMetadata.publicationYear == metadataMap.publicationYear
        dcMetadata.resourceType.getResourceTypeGeneral().value() == metadataMap.resourceType
        dcMetadata.resourceType.getValue() == metadataMap.resourceText
        dcMetadata.dates.getDate().get(0).getValue() == metadataMap.createdDate
        dcMetadata.dates.getDate().get(0).getDateType() == DateType.CREATED
        DataCiteValidator.toXml(dcMetadata, true)
    }
}