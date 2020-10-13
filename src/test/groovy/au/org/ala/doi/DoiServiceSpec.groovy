package au.org.ala.doi

import au.org.ala.doi.exceptions.DoiMintingException
import au.org.ala.doi.exceptions.DoiUpdateException
import au.org.ala.doi.exceptions.DoiValidationException
import au.org.ala.doi.providers.AndsService
import au.org.ala.doi.storage.Storage
import au.org.ala.doi.util.DoiProvider
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import grails.converters.JSON
import grails.plugins.elasticsearch.ElasticSearchService
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import groovy.json.JsonSlurper
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.grails.web.converters.configuration.ConvertersConfigurationInitializer
import org.grails.web.json.JSONObject
import org.springframework.web.multipart.MultipartFile
import spock.lang.Specification

class DoiServiceSpec extends Specification implements ServiceUnitTest<DoiService>, DataTest {

    void setupSpec() {
        mockDomain Doi
    }

    def setup() {
        new ConvertersConfigurationInitializer().afterPropertiesSet() // force converters plugin to register default converters
        config.support.email = 'supportEmail'
        config.doi.service.mock = false
        service.andsService = Mock(AndsService)
        service.emailService = Mock(EmailService)
        service.storage = Mock(Storage)
    }

    def "mintDoi should throw IllegalArgumentException if no provider is given"() {
        when:
        service.mintDoi(null, [foo: "bar"], "title", "authors", "description", ["licence 1", "licence 2"] ,"applicationurl", "url", null)

        then:
        thrown IllegalArgumentException
    }

    def "mintDoi should throw IllegalArgumentException if no providerMetada is given"() {
        when:
        service.mintDoi(DoiProvider.ANDS, [:], "title", "authors", "description", ["licence 1", "licence 2"],"applicationurl", "url", null)

        then:
        thrown IllegalArgumentException
    }

    def "mintDoi should throw IllegalArgumentException if no applicationUrl is given"() {
        when:
        service.mintDoi(DoiProvider.ANDS, [foo: "bar"], "title", "authors", "description", ["licence 1", "licence 2"], null, "url", null)

        then:
        thrown IllegalArgumentException
    }

    def "mintDoi should throw DoiMintingException if the given defaultDoi DOI already exists in the database"() {
        given: "a DOI is already recorded in the database"
        def doi = '10.1000/000000'
        def entity = new Doi(uuid: UUID.randomUUID(), doi: doi, title: 'title', authors: 'authors', description: 'description', dateMinted: new Date(), provider: DoiProvider.ANDS, applicationUrl: 'http://example.org', userId: "1").save()

        when: "a mintDOI request is made with an existing DOI"

        service.mintDoi(DoiProvider.ANDS, [foo: 'bar'], 'title', 'authors', 'description', ["licence 1", "licence 2"], 'http://example.org', null, null, [:], null, doi, "1")


        then:
        thrown DoiMintingException
    }

    def "mintDoi should throw DoiValidationException if the local database record cannot be validated"() {
        when: "given data with missing mandatory entity parameters (i.e. no description)"
        service.mintDoi(DoiProvider.ANDS, [foo: "bar"], "title", "authors", null, ["licence 1", "licence 2"], "applicationUrl", null, Mock(MultipartFile))

        then:
        thrown DoiValidationException
    }

    def "mintDoi should invoke the ANDS provider service when DoiProvider == ANDS and the metadata is valid"() {
        setup:
        MultipartFile file = Mock(MultipartFile)
        file.contentType >> "application/pdf"
        when:
        service.mintDoi(DoiProvider.ANDS, [foo: "bar"], "title", "authors", "description", ["licence 1", "licence 2"], "http://example.org/applicationUrl", "url", file)

        then:
        1 * service.andsService.mintDoi(_, _, _) >> "newDoi"
    }

    def "mintDoi should send an alert email to support if the DB save fails after the DOI was generated"() {
        setup:
        MultipartFile file = Mock(MultipartFile)
        file.contentType >> "application/pdf"
        service.andsService.mintDoi(_, _, _) >> null

        when:
        service.mintDoi(DoiProvider.ANDS, [foo: "bar"], "title", "authors", "description", ["licence 1", "licence 2"], "http://example.org/applicationUrl", "url", file)

        then:
        1 * service.emailService.sendDoiFailureEmail("supportEmail", "doiservice <no-reply@ala.org.au>", null, _)
    }

    def "mintDoi should return a map with an error message if the DB save fails after the DOI was generated"() {
        setup:
        MultipartFile file = Mock(MultipartFile)
        file.contentType >> "application/pdf"
        service.andsService.mintDoi(_, _, _) >> null

        when:
        MintResponse result = service.mintDoi(DoiProvider.ANDS, [foo: "bar"], "title", "authors", "description", ["licence 1", "licence 2"], "http://example.org/applicationUrl", "url", file)

        then:
        result.error != null
        !result.uuid
    }

    def "mintDoi should return a map with the DOI, landingPageUrl and the UUID on success"() {
        setup:
        MultipartFile file = Mock(MultipartFile)
        file.contentType >> "application/pdf"
        service.andsService.mintDoi(_, _, _) >> "newDoi"
        service.andsService.generateLandingPageUrl(_, _) >> "http://landingpage.com"

        when:
        MintResponse result = service.mintDoi(DoiProvider.ANDS, [foo: "bar"], "title", "authors", "description", ["licence 1", "licence 2"], "http://example.org/applicationUrl", "url", file)

        then:
        !result.error
        result.uuid != null
        result.doi == "newDoi"
        result.landingPage != null
    }

    def "mintDoi should not call andsService if a default DOI is provided"() {
        setup:
        MultipartFile file = Mock(MultipartFile)
        file.contentType >> "application/pdf"
        service.andsService.generateLandingPageUrl(_,null) >> "http://landingpage.com"

        when:
        MintResponse result = service.mintDoi(DoiProvider.ANDS, [foo: "bar"], "title", "authors", "description", ["licence 1", "licence 2"], "http://example.org/applicationUrl", "url", file, null, null, "defaultDOI")

        then:

        0 * service.andsService.mintDoi(_) >> [:]
        !result.error
        result.uuid != null
        result.doi == "defaultDOI"
        result.doiServiceLandingPage != null
    }

    def "updateDoi should allow a multi part file update when there is no existing file"() {
        setup:
        def doi = '10.1000/000000'
        def entity = savedDoi(doi)
        MultipartFile file = Mock(MultipartFile)
        file.originalFilename >> 'test.pdf'
        file.contentType >> "application/pdf"

        when:
        def result = service.updateDoi(entity.doi, [:], file)

        then:
        1 * service.storage.storeFileForDoi(entity, file) >> { Doi d2, MultipartFile f -> d2.filename = f.originalFilename; d2.contentType = f.contentType }
        0 * service.emailService.sendDoiFailureEmail(_, _, _, _)
        0 * service.andsService.updateDoi(_, _, _, _)
        result.doi == doi
        result.uuid == entity.uuid
        result.contentType == 'application/pdf'
        result.filename == 'test.pdf'
    }

    def "updateDoi should allow a URL update when there is no existing file"() {
        setup:
        def doi = '10.1000/000000'
        def entity = savedDoi(doi)
        def url = 'http://example.org'

        when:
        def result = service.updateDoi(entity.doi, [fileUrl: url], null)

        then:
        1 * service.storage.storeFileForDoi(entity, url) >> { Doi d2, String u2 -> d2.filename = 'test.pdf'; d2.contentType = 'application/pdf' }
        0 * service.emailService.sendDoiFailureEmail(_, _, _, _)
        0 * service.andsService.updateDoi(_, _, _, _)
        result.doi == doi
        result.uuid == entity.uuid
        result.contentType == 'application/pdf'
        result.filename == 'test.pdf'
    }

    def "updateDoi should fail if passed a multipart file and a file is already registered"() {
        setup:
        def doi = '10.1000/000000'
        def entity = savedDoi(doi, [filename: 'test.pdf', contentType: 'application/pdf'])
        MultipartFile file = Mock(MultipartFile)
        file.originalFilename >> 'test.pdf'
        file.contentType >> "application/pdf"

        when:
        def result = service.updateDoi(entity.doi, [:], file)

        then:
        0 * service.storage.storeFileForDoi(entity, file)
        thrown DoiUpdateException
    }

    def "updateDoi should fail if passed a URL and a file is already registered"() {
        setup:
        def doi = '10.1000/000000'
        def entity = savedDoi(doi, [filename: 'test.pdf', contentType: 'application/pdf'])
        def url = 'http://example.org'

        when:
        def result = service.updateDoi(entity.doi, [fileUrl: url], null)

        then:
        0 * service.storage.storeFileForDoi(entity, url)
        thrown DoiUpdateException
    }

    def "updateDoi should trigger a call to the DOI provider when the input providerMetadata has changed"() {
        setup:
        def doi = '10.1000/000000'
        def entity = savedDoi(doi)
        def providerMetadata = grailsExtractedMap(entity.providerMetadata) << [something: 'else']

        when:
        def result = service.updateDoi(entity.doi, [providerMetadata: providerMetadata], null)

        then:
        0 * service.storage.storeFileForDoi(entity, _)
        1 * service.andsService.updateDoi(_, _, _, _)
        result.providerMetadata == providerMetadata
    }

    def "updateDoi should trigger a call to the DOI provider when the input providerMetadata has changed 2"() {
        setup:
        def doi = '10.1000/000000'
        def entity = savedDoi(doi)
        def providerMetadata = grailsExtractedMap(entity.providerMetadata)
        providerMetadata.contributors[0].name = 'dave'

        when:
        def result = service.updateDoi(entity.doi, [providerMetadata: providerMetadata], null)

        then:
        0 * service.storage.storeFileForDoi(entity, _)
        1 * service.andsService.updateDoi(_, _, _, _)
        result.providerMetadata == providerMetadata
    }

    def "updateDoi should not trigger a call to the DOI provider when the input providerMetadata is the same as the existing provider metadata"() {
        setup:
        def doi = '10.1000/000000'
        def entity = savedDoi(doi)
        def providerMetadata = grailsExtractedMap(entity.providerMetadata)

        when:
        def result = service.updateDoi(entity.doi, [providerMetadata: providerMetadata], null)

        then:
        0 * service.storage.storeFileForDoi(entity, _)
        0 * service.andsService.updateDoi(_, _, _, _)
        result.providerMetadata == providerMetadata
    }

    def "updateDoi should trigger a call to the DOI provider when the input customLandingPageUrl is the same as the existing provider metadata"() {
        setup:
        def doi = '10.1000/000000'
        def entity = savedDoi(doi, [customLandingPageUrl: 'http://example.org'])
        def newLandingPage = 'http://example.org/some-other-url'

        when:
        def result = service.updateDoi(entity.doi, [customLandingPageUrl: newLandingPage], null)

        then:
        0 * service.storage.storeFileForDoi(entity, _)
        1 * service.andsService.updateDoi(_, _, _, _)
        result.customLandingPageUrl == newLandingPage

    }

    def "updateDoi should not trigger a call to the DOI provider when the input customLandingPageUrl is the same as the existing provider metadata"() {
        setup:
        def doi = '10.1000/000000'
        def entity = savedDoi(doi)

        when:
        def result = service.updateDoi(entity.doi, [customLandingPageUrl: entity.customLandingPageUrl], null)

        then:
        0 * service.storage.storeFileForDoi(entity, _)
        0 * service.andsService.updateDoi(_, _, _, _)

    }

    def "updateDoi should update all other fields"() {
        setup:
        def doi = '10.1000/000000'
        def entity = savedDoi(doi)
        def updates = [
                'title': 'new title',
                'authors': 'new authors',
                'description': 'new description',
                'applicationUrl': 'http://new.application.url.org',
                'applicationMetadata': grailsExtractedMap(['new': ['meta':'data'],data:'newMeta'])
        ]

        when:
        def result = service.updateDoi(entity.doi, updates, null)

        then:
        0 * service.storage.storeFileForDoi(entity, _)
        0 * service.andsService.updateDoi(_, _, _, _)
        result.title == 'new title'
        result.authors == 'new authors'
        result.description == 'new description'
        result.applicationUrl == 'http://new.application.url.org'
        result.applicationMetadata == ['new': ['meta':'data'],data:'newMeta']
    }

    def "updateDoi should not allow updates to certain fields"() {
        setup:
        def doi = '10.1000/000000'
        def entity = savedDoi(doi)
        def updates = [
                id: entity.id + 1,
                uuid: UUID.randomUUID().toString(),
                doi: '10.1001/111111',
                dateMinted: entity.dateMinted + 1,
                provider: 'GITHUB',
                filename: 'jabberwocky.txt',
                contentType: 'image/webp',
                version: 123456789,
                dateCreated: new Date(),
                lastUpdated: new Date()
        ]

        when:
        def result = service.updateDoi(entity.doi, updates, null)

        then:
        result.id != updates.id
        result.id == entity.id
        result.uuid.toString() != updates.uuid
        result.uuid == entity.uuid
        result.doi == entity.doi
        result.doi != updates.doi
        result.dateMinted == entity.dateMinted
        result.provider == entity.provider
        result.filename == entity.filename
        result.contentType == entity.contentType
        result.version == entity.version
        result.dateCreated == entity.dateCreated
        result.lastUpdated == entity.lastUpdated
    }

    private static Doi doi(String doi, Map nullables = [:]) {
        /*
            applicationMetadata nullable: true
            customLandingPageUrl nullable: true, url: true
            applicationUrl nullable: true, url: true
            filename nullable: true
            contentType nullable: true
         */

        new Doi(uuid: UUID.randomUUID(), doi: doi,
                title: 'title', authors: 'authors',
                description: 'description',
                licence: ["licence 1","licence 2"],
                dateMinted: new Date(),
                provider: DoiProvider.ANDS,
                providerMetadata: userTypeExtractedMap('{"title": "<Title>", "authors": ["<Author>"], "subjects": ["<Subjects>"], "subtitle": "<Subtitle>", "publisher": "<Publisher>", "createdDate": "YYYY-MM-ddThh:mm:ssZ", "contributors": [{"name": "<Contributor>", "type": "<Editor|etc>"}], "descriptions": [{"text": "<Description>", "type": "<Other|etc>"}], "resourceText": "<Species information|etc>", "resourceType": "<Text|etc>", "publicationYear": 2017}'),
                applicationMetadata: nullables['applicationMetadata'],
                customLandingPageUrl: nullables['customLandingPage'],
                applicationUrl: nullables['applicationUrl'],
                filename: nullables['filename'],
                contentType: nullables['contentType'])
    }

    private static Map grailsExtractedMap(Map map) {
        grailsExtractedMap((map as JSON).toString())
    }
    private static Map grailsExtractedMap(String json) {
        (JSONObject) JSON.parse(json)
    }

    static Gson gson = new GsonBuilder().serializeNulls().create() // to match the PG hibernate extensions
    private static Map userTypeExtractedMap(Map map) {
        userTypeExtractedMap(gson.toJson(map))
    }
    private static Map userTypeExtractedMap(String json) {
        def jsonObject = gson.fromJson(json, HashMap) // Spoiler alert, this is how the plugin does it
    }

    private static Doi savedDoi(String doiValue, Map nullables = [:]) {
        doi(doiValue, nullables).save()
    }
}
