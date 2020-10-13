package au.org.ala.doi.ws

import au.org.ala.doi.Doi
import au.org.ala.doi.DoiSearchService
import au.org.ala.doi.DoiSearchServiceSpec
import au.org.ala.doi.DoiService
import au.org.ala.doi.MintResponse
import au.org.ala.doi.storage.Storage
import au.org.ala.doi.util.DoiProvider
import au.org.ala.doi.util.DoiProviderMapping
import com.google.common.io.ByteSource
import com.google.common.io.Files
import grails.converters.JSON
import grails.plugins.elasticsearch.ElasticSearchResult
import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.apache.http.HttpStatus
import org.apache.lucene.search.TotalHits
import org.springframework.web.multipart.MultipartFile
import spock.lang.Specification

class DoiControllerSpec extends Specification implements ControllerUnitTest<DoiController>, DataTest {

    Storage storage
    DoiService doiService
    DoiSearchService doiSearchService

    def setupSpec() {
        mockDomain Doi
    }

    def setup() {
        storage = Mock(Storage)
        controller.storage = storage
        doiService = Mock(DoiService)
        controller.doiService = doiService
        doiSearchService = Mock(DoiSearchService)
        controller.doiSearchService = doiSearchService
    }

//    def "getDoi should return a HTTP 400 (BAD_REQUEST) if no id is provided"() {
//        when:
//        controller.getDoi()
//
//        then:
//        response.status == HttpStatus.SC_BAD_REQUEST
//    }

    def "show should search for DOI records by UUID if the provided id is a UUID"() {
        setup:
        String uuid = UUID.randomUUID().toString()
        when:
        params.id = uuid
        controller.show()

        then:
        1 * doiService.findByUuid(uuid)
        0 * doiService.findByDoi(_)
    }

    def "show should search for DOI records by DOI if the provided id is not a UUID"() {
        setup:
        String id = "10.5072/63/56F35A9D3ECF7"
        when:
        params.id = id
        controller.show()

        then:
        0 * doiService.findByUuid(_)
        1 * doiService.findByDoi(id)
    }

    def "show should return a 404 (NOT_FOUND) if there was no matching DOI"() {
        setup:
        String id = "10.5072/63/56F35A9D3ECF7"
        when:
        params.id = id
        controller.show()

        then:
        0 * doiService.findByUuid(_)
        1 * doiService.findByDoi(id) >> null
        response.status == HttpStatus.SC_NOT_FOUND
    }

    def "show should return the matching DOI entity as JSON"() {
        setup:
        Doi doi = new Doi(uuid: UUID.randomUUID())

        when:
        params.id = doi.uuid.toString()
        controller.show()

        then:
        1 * doiService.findByUuid(doi.uuid.toString()) >> doi
        response.contentType == "application/json;charset=UTF-8"
    }

//    def "download should return a HTTP 400 (BAD_REQUEST) if no id is provided"() {
//        when:
//        controller.download()
//
//        then:
//        response.status == HttpStatus.SC_BAD_REQUEST
//    }

    def "download should search for DOI records by UUID if the provided id is a UUID"() {
        setup:
        String uuid = UUID.randomUUID().toString()
        when:
        params.id = uuid
        controller.download()

        then:
        1 * doiService.findByUuid(uuid)
        0 * doiService.findByDoi(_)
    }

    def "download should search for DOI records by DOI if the provided id is not a UUID"() {
        setup:
        String id = "10.5072/63/56F35A9D3ECF7"
        when:
        params.id = id
        controller.download()

        then:
        0 * doiService.findByUuid(_)
        1 * doiService.findByDoi(id)
    }

    def "download should return a 404 (NOT_FOUND) if there was no matching DOI"() {
        setup:
        String id = "10.5072/63/56F35A9D3ECF7"
        when:
        params.id = id
        controller.download()

        then:
        0 * doiService.findByUuid(_)
        1 * doiService.findByDoi(id) >> null
        response.status == HttpStatus.SC_NOT_FOUND
    }

    def "download should return a 404 (NOT_FOUND) if there was no file for the DOI"() {
        setup:
        String id = "10.5072/63/56F35A9D3ECF7"
        when:
        params.id = id
        controller.download()

        then:
        0 * doiService.findByUuid(_)
        1 * doiService.findByDoi(id) >> new Doi(uuid: UUID.randomUUID())
        1 * storage.getFileForDoi(_) >> null
        response.status == HttpStatus.SC_NOT_FOUND
    }

    def "download should return the matching DOI's file"() {
        setup:
        Doi doi = new Doi(uuid: UUID.randomUUID(), contentType: "text/plain", filename: "bla.txt")
        File dir = new File("${System.getProperty("java.io.tmpdir")}/${doi.uuid}")
        dir.mkdirs()
        File file = new File(dir, "bla.txt")
        file.createNewFile()
        file << "file content"
        ByteSource bs = Files.asByteSource(file)

        when:
        params.id = doi.uuid.toString()
        controller.download()

        then:
        1 * doiService.findByUuid(doi.uuid.toString()) >> doi
        1 * storage.getFileForDoi(_) >> bs
        response.getHeader("Content-disposition") == 'attachment;filename=bla.txt'
        response.contentType == doi.contentType
        response.text == "file content"
    }

    def "save should return a 400 BAD_REQUEST if the provider is invalid"() {
        when:
        request.JSON.provider = "rubbish"
        request.JSON.applicationUrl = "http://example.org/applicationUrl"
        request.JSON.providerMetadata = '{"foo": "bar"}'
        request.JSON.title = 'title'
        request.JSON.authors = 'authors'
        request.JSON.description = 'description'
        request.JSON.fileUrl = "url"
        controller.save()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
    }

    def "save should invoke the DOI Service if provided all metadata and the fileUrl for non-multipart requests"() {
        when:
        request.JSON.provider = DoiProvider.ANDS.name()
        request.JSON.applicationUrl = "http://example.org/applicationUrl"
        request.JSON.providerMetadata = [foo: "bar"]
        request.JSON.title = 'title'
        request.JSON.authors = 'authors'
        request.JSON.description = 'description'
        request.JSON.fileUrl = "fileUrl"
        request.JSON.userId = '1'
        request.JSON.active = false
        controller.save()

        then:
        1 * controller.doiService.mintDoi(DoiProvider.ANDS, [foo: "bar"], "title", "authors", "description", null, "http://example.org/applicationUrl", "fileUrl", null, null, null, null, '1', false, null, null) >> new MintResponse()
    }

    def "save should invoke the DOI Service if provided all metadata and a file in a multipart requests"() {
        setup:
        MultipartFile file = Mock(MultipartFile)

        when:
        Map data = [:]
        data.provider = DoiProvider.ANDS.name()
        data.applicationUrl = "http://example.org/applicationUrl"
        data.providerMetadata = [foo: "bar"]
        data.title = 'title'
        data.authors = 'authors'
        data.description = 'description'
        data.licence = ['licence 1', 'licence 2']
        data.active = false

        controller.request.addParameter("json", (data as JSON) as String)
        controller.request.addFile(file)
        controller.save()

        then:
        1 * controller.doiService.mintDoi(DoiProvider.ANDS, [foo: "bar"], "title", "authors", "description", ["licence 1", "licence 2"], "http://example.org/applicationUrl", null, file, null, null, null, null, false, null, null) >> new MintResponse()
    }

    def "save should map provider if provided all metadata and the fileUrl for non-multipart requests"() {

        setup:
        DoiProviderMapping doiProviderMapping = new DoiProviderMapping()
        doiProviderMapping.doiProviderMapping = [ ALA: 'DATACITE' ]
        doiProviderMapping.init()

        when:
        request.JSON.provider = 'ALA'
        request.JSON.applicationUrl = "http://example.org/applicationUrl"
        request.JSON.providerMetadata = [foo: "bar"]
        request.JSON.title = 'title'
        request.JSON.authors = 'authors'
        request.JSON.description = 'description'
        request.JSON.fileUrl = "fileUrl"
        request.JSON.userId = '1'
        request.JSON.active = false
        controller.save()

        then:
        1 * controller.doiService.mintDoi(DoiProvider.DATACITE, [foo: "bar"], "title", "authors", "description", null, "http://example.org/applicationUrl", "fileUrl", null, null, null, null, '1', false, null, null) >> new MintResponse()
    }

    def "search applies defaults to all parameters"() {

        setup:
        ElasticSearchResult result = new ElasticSearchResult([total: new TotalHits(20, TotalHits.Relation.EQUAL_TO)])

        when:
        controller.search()

        then:
        1 * controller.doiSearchService.searchDois(10, 0, "", null, "dateMinted", "desc") >> result
        response.status == HttpStatus.SC_OK
    }

    def "search will pass user supplied parameters to the search service"() {
        setup:
        ElasticSearchResult result = new ElasticSearchResult([total: new TotalHits(20, TotalHits.Relation.EQUAL_TO)])

        when:
        params.max = 20
        params.offset = 10
        params.sort = "title"
        params.order = "asc"
        params.q = "query string"
        params.fq = ["field1:value1", "field2:value2"]
        controller.search()

        then:
        1 * controller.doiSearchService.searchDois(20, 10, "query string", [field1:'value1', field2:'value2'], "title", "asc") >> result
        response.status == HttpStatus.SC_OK
    }

    def "search will return a 422 if invalid fq parameters are supplied"() {
        when:
        params.fq = ["field1:value1", "field1:value2"]
        controller.search()

        then:
        response.status == HttpStatus.SC_UNPROCESSABLE_ENTITY

        when:
        params.fq = ["malformed filter"]
        controller.search()

        then:
        response.status == HttpStatus.SC_UNPROCESSABLE_ENTITY
    }

}
