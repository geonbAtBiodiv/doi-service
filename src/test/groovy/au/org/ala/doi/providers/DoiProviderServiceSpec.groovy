package au.org.ala.doi.providers

import au.org.ala.doi.exceptions.DoiMintingException
import au.org.ala.doi.util.ServiceResponse
import org.apache.http.HttpStatus
import spock.lang.Specification

class DoiProviderServiceSpec extends Specification {

    def "generateLandingPageUrl should return the customLandingPageUrl if provided"() {
        given:
        DoiProviderService service = [
                generateRequestPayload: { uuid, metadata, landingPage -> "payload"},
                invokeCreateService: {}
        ] as DoiProviderService

        when:
        String url = service.generateLandingPageUrl("uuid", "customLandingPage")

        then:
        url == "customLandingPage"
    }

    def "generateLandingPageUrl should return a standard, constructed URL when there is no customLandingPageUrl"() {
        given:
        DoiProviderService service = [
                generateRequestPayload: { uuid, metadata, landingPage -> "payload"},
                invokeCreateService: {},
                generateLandingPageUrl: {"serverUrl/doi/uuid1"}
        ] as DoiProviderService

        when:
        String url = service.generateLandingPageUrl("uuid1")

        then:
        url == "serverUrl/doi/uuid1"
    }

    def "mintDoi should throw an IllegalArgumentException if no uuid was provided"() {
        given:
        DoiProviderService service = [
                generateRequestPayload: { throw new Exception("test")},
                invokeCreateService: {},
                getGenericLandingPageUrlPrefix: {"serverUrl"}
        ] as DoiProviderService

        when:
        service.mintDoi(null, [foo: "bar"])

        then:
        thrown IllegalArgumentException
    }

    def "mintDoi should throw an IllegalArgumentException if no metadata was provided"() {
        given:
        DoiProviderService service = [
                generateRequestPayload: { throw new Exception("test")},
                invokeCreateService: {},
                getGenericLandingPageUrlPrefix: {"serverUrl"}
        ] as DoiProviderService

        when:
        service.mintDoi("uuid", [:])

        then:
        thrown IllegalArgumentException
    }

    def "mintDoi should throw a DoiMintingException if generateRequestPayload results in an exception"() {
        given:
        DoiProviderService service = [
                generateRequestPayload: { uuid, Map m, String s -> throw new Exception("test")},
                invokeCreateService: {},
                generateLandingPageUrl: {uuid, custom -> "serverUrl/doi/uuid1"}
        ] as DoiProviderService

        when:
        service.mintDoi("uuid", [foo: "bar"])

        then:
        thrown DoiMintingException
    }

    def "mintDoi should throw a DoiMintingException if invokeService results in an exception"() {
        given:
        DoiProviderService service = [
                generateRequestPayload: { uuid, Map metadata, String landingPage -> "payload"},
                invokeCreateService: { String s, String t ->  throw new Exception("test")},
                generateLandingPageUrl: {uuid, custom -> "serverUrl/doi/uuid1"}
        ] as DoiProviderService

        when:
        service.mintDoi("uuid", [foo: "bar"])

        then:
        thrown DoiMintingException
    }

    def "mintDoi should throw a DoiMintingException if invokeService returns a ServiceResponse with a result code other than HTTP 200"() {
        given:
        DoiProviderService service = [
                generateRequestPayload: { uuid, metadata, landingPage -> "payload"},
                invokeCreateService: { payload, landingPage -> new ServiceResponse(HttpStatus.SC_BAD_REQUEST, "bad")},
                generateLandingPageUrl: {uuid, custom -> "serverUrl/doi/uuid1"}
        ] as DoiProviderService

        when:
        service.mintDoi("uuid", [foo: "bar"])

        then:
        thrown DoiMintingException
    }

    def "mintDoi should throw a DoiMintingException if invokeService returns a ServiceResponse with a result code of HTTP 200 but not DOI"() {
        given:
        DoiProviderService service = [
                generateRequestPayload: { uuid, metadata, landingPage -> "payload"},
                invokeCreateService: { payload, landingPage -> new ServiceResponse(HttpStatus.SC_OK, "bad")},
                generateLandingPageUrl: {uuid, custom -> "serverUrl/doi/uuid1"}
        ] as DoiProviderService

        when:
        service.mintDoi("uuid", [foo: "bar"])

        then:
        thrown DoiMintingException
    }

    def "mintDoi should return the new DOI on success"() {
        given:
        DoiProviderService service = [
                generateRequestPayload: { uuid, metadata, landingPage -> "payload"},
                invokeCreateService: { payload, landingPage -> new ServiceResponse("newDoi")},
                generateLandingPageUrl: {uuid, custom -> "serverUrl/doi/uuid1"}
        ] as DoiProviderService

        when:
        String doi = service.mintDoi("uuid", [foo: "bar"])

        then:
        doi == "newDoi"
    }
}
