package au.org.ala.doi.providers

import com.google.common.io.Resources
import grails.testing.services.ServiceUnitTest
import grails.util.Holders
import grails.web.mapping.LinkGenerator
import org.gbif.datacite.rest.client.configuration.ClientConfiguration
import org.gbif.doi.metadata.datacite.DataCiteMetadata
import org.gbif.doi.metadata.datacite.DateType
import org.gbif.doi.metadata.datacite.TitleType
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
        service.restDataCiteService = Mock(RestJsonApiDataCiteService)
        service.clientConf = ClientConfiguration.builder()
                .withBaseApiUrl("https://api.test.datacite.org/")
                .withTimeOut(60)
                .withFileCacheMaxSizeMb(64)
                .withUser("bob").withPassword("12345").build()
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