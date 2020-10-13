package au.org.ala.doi.providers

import au.org.ala.doi.RestService
import au.org.ala.doi.util.ServiceResponse
import com.google.common.io.Resources
import grails.testing.services.ServiceUnitTest
import grails.web.mapping.LinkGenerator
import groovyx.net.http.ContentType
import org.apache.http.HttpStatus
import org.grails.spring.beans.factory.InstanceFactoryBean
import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class AndsServiceSpec extends Specification implements ServiceUnitTest<AndsService> {

    def setup() {
        defineBeans {
            grailsLinkGenerator(InstanceFactoryBean, Stub(LinkGenerator), LinkGenerator)
        }
        service.restService = Mock(RestService)
        service.grailsApplication = [
                config: [
                        ands: [
                                doi: [
                                        service: [
                                                url: "andsUrl/"
                                        ],
                                        app: [
                                                id: "appId"
                                        ],
                                        key: "andsKey"
                                ]
                        ]
                ]
        ]
    }

    def "serviceStatus should return the HTTP error status that the ANDS service responded with"() {
        when:
        service.restService.get(_, _, _) >> [status: status, data: [response: [responsecode: AndsService.ANDS_RESPONSE_STATUS_OK]]]
        Map response = service.serviceStatus()

        then:
        response.statusCode == status

        where:
        status << [400, 404, 500]
    }

    def "serviceStatus should return the ANDS 'OK' status code if the call to the ANDS service results in a HTTP 200"() {
        when:
        service.restService.get(_, _, _) >> [status: HttpStatus.SC_OK, data: [response: [responsecode: AndsService.ANDS_RESPONSE_STATUS_OK]]]
        Map response = service.serviceStatus()

        then:
        response.statusCode == AndsService.ANDS_RESPONSE_STATUS_OK
    }

    def "serviceStatus should return an status code of E001 if the call to ANDS resulted in an exception"() {
        when:
        service.restService.get(_, _, _) >> { throw new Exception("test") }
        Map response = service.serviceStatus()

        then:
        response.statusCode == AndsService.ANDS_DEAD_STATUS_CODE
    }

    def "invokeService should return the 'ANDS unavailable' status code if the ANDS service is not responding"() {
        when:
        service.restService.get(_, _, _) >> { throw new Exception("test") }

        ServiceResponse resp = service.invokeCreateService("bla", "bla")

        then:
        resp.httpStatus == AndsService.ANDS_UNAVAILABLE_CODE
    }

    def "invokeService should post to the ANDS mint URL"() {
        when:
        service.restService.get(_, _, _) >> [status: HttpStatus.SC_OK, data: [response: [responsecode: AndsService.ANDS_RESPONSE_STATUS_OK]]]

        service.invokeCreateService("some xml", "blabla")

        then:
        1 * service.restService.post("andsUrl/mint.json/", _, _, _, _, _) >> [status: HttpStatus.SC_OK, data: [response: [responsecode: AndsService.ANDS_RESPONSE_MINT_SUCCESS, doi: "newDoi"]]]
    }

    def "invokeService should post the provided xml to the ANDS service"() {
        when:
        service.restService.get(_, _, _) >> [status: HttpStatus.SC_OK, data: [response: [responsecode: AndsService.ANDS_RESPONSE_STATUS_OK]]]

        service.invokeCreateService("some xml", "blabla")

        then:
        1 * service.restService.post(_, [xml: "some xml"], ContentType.JSON, ContentType.URLENC, _, _) >> [status: HttpStatus.SC_OK, data: [response: [responsecode: AndsService.ANDS_RESPONSE_MINT_SUCCESS, doi: "newDoi"]]]
    }

    def "invokeService should construct a secret key by Base64 encoding the appId and Key properties and pass it as a header to ANDS"() {
        setup:
        service.restService.get(_, _, _) >> [status: HttpStatus.SC_OK, data: [response: [responsecode: AndsService.ANDS_RESPONSE_STATUS_OK]]]
        String expectedSecret = "appId:andsKey".encodeAsBase64()
        Map expectedHeaders = [Accept: ContentType.JSON, Authorization: "Basic ${expectedSecret}"]

        when:
        service.invokeCreateService("some xml", "blabla")

        then:
        1 * service.restService.post(_, _, _, _, expectedHeaders, _) >> [status: HttpStatus.SC_OK, data: [response: [responsecode: AndsService.ANDS_RESPONSE_MINT_SUCCESS, doi: "newDoi"]]]
    }

    def "invokeService should pass the AppId and Landing Page URL as query params to ANDS"() {
        setup:
        service.restService.get(_, _, _) >> [status: HttpStatus.SC_OK, data: [response: [responsecode: AndsService.ANDS_RESPONSE_STATUS_OK]]]
        Map expectedQuery = [app_id: "appId", url: "landingPageUrl"]

        when:
        service.invokeCreateService("some xml", "landingPageUrl")

        then:
        1 * service.restService.post(_, _, _, _, _, expectedQuery) >> [status: HttpStatus.SC_OK, data: [response: [responsecode: AndsService.ANDS_RESPONSE_MINT_SUCCESS, doi: "newDoi"]]]
    }

    def "invokeService should return the DOI when the mint process was successful"() {
        setup:
        service.restService.get(_, _, _) >> [status: HttpStatus.SC_OK, data: [response: [responsecode: AndsService.ANDS_RESPONSE_STATUS_OK]]]
        1 * service.restService.post(_, _, _, _, _, _) >> [status: HttpStatus.SC_OK, data: [response: [responsecode: AndsService.ANDS_RESPONSE_MINT_SUCCESS, doi: "newDoi"]]]


        when:
        ServiceResponse resp = service.invokeCreateService("some xml", "landingPageUrl")

        then:
        resp.doi == "newDoi"
    }

    def "invokeService should return the HTTP status code when the service call fails"() {
        setup:
        service.restService.get(_, _, _) >> [status: HttpStatus.SC_OK, data: [response: [responsecode: AndsService.ANDS_RESPONSE_STATUS_OK]]]
        1 * service.restService.post(_, _, _, _, _, _) >> [status: HttpStatus.SC_BAD_REQUEST]


        when:
        ServiceResponse resp = service.invokeCreateService("some xml", "landingPageUrl")

        then:
        resp.httpStatus == HttpStatus.SC_BAD_REQUEST
    }

    def "invokeService should return the ANDS error message when the mint process fails"() {
        setup:
        service.restService.get(_, _, _) >> [status: HttpStatus.SC_OK, data: [response: [responsecode: AndsService.ANDS_RESPONSE_STATUS_OK]]]
        1 * service.restService.post(_, _, _, _, _, _) >> [status: HttpStatus.SC_OK, data: [response: [responsecode: AndsService.ANDS_RESPONSE_STATUS_DEAD, message: "message", verbosemessage: "verbose message"]]]


        when:
        ServiceResponse resp = service.invokeCreateService("some xml", "landingPageUrl")

        then:
        resp.httpStatus == HttpStatus.SC_OK // the http call succeeded, but ANDS itself failed
        resp.errorMessage == "The service invocation returned HTTP 200 and error 'message: verbose message' with error code MT091"
    }

    def "generateRequestPayload should map all metadata fields to the ANDS xml schema"() {
        given:
        Map andsMetadata = [:]
        andsMetadata.authors = ["author1", "author2"]
        andsMetadata.title = "publicationTitle"
        andsMetadata.subtitle = "publicationSubtitle"
        andsMetadata.publisher = "publisherName"
        andsMetadata.publicationYear = "2016"
        andsMetadata.subjects = ["subject1", "subject2"]
        andsMetadata.contributors = [[type: "Editor", name: "bob"], [type: "Editor", name: "jill"]]
        andsMetadata.resourceType = "Text"
        andsMetadata.resourceText = "resourceText"
        andsMetadata.descriptions = [[type: "Other", text: "description1"], [type: "Other", text: "description2"]]
        andsMetadata.createdDate = "createdDate"
        andsMetadata.rights = ["rights statement 1", "rights statement 2"]

        def standardizeSpaces = { text -> text.replaceAll(" +", " ") }

        String expectedXml = Resources.getResource('resources/ValidANDSRequest.xml').text

        when:
        String xml = service.generateRequestPayload("uuid", andsMetadata, "landingPageUrl")

        then:
        standardizeSpaces(xml) == standardizeSpaces(expectedXml)
    }
}