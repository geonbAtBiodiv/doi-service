package au.org.ala.doi

import grails.converters.JSON
import grails.plugins.elasticsearch.ElasticSearchContextHolder
import grails.plugins.elasticsearch.ElasticSearchResult
import grails.plugins.elasticsearch.ElasticSearchService
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.elasticsearch.action.search.SearchRequest
import spock.lang.Specification

class DoiSearchServiceSpec extends Specification implements ServiceUnitTest<DoiSearchService>, DataTest {

    void setup() {
        mockDomain Doi
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.elasticSearchContextHolder = new ElasticSearchContextHolder()
        service.elasticSearchService.elasticSearchContextHolder.config = new ConfigObject()
    }

    def "searchDois should use a simple query string for the search term"() {

        setup:
        Map searchParams = [:]
        mockSearch(searchParams)

        when:
        service.searchDois(10, 0, "test")

        then:
        def query = JSON.parse(searchParams.receivedSearchRequest.source().query().toString())
        query.bool != null
        query.bool.must != null
        query.bool.must.size() == 1
        query.bool.must[0].simple_query_string.query == 'test'
    }

    def "searchDois should use a match all query if no search term is supplied"() {

        setup:
        Map searchParams = [:]
        mockSearch(searchParams)

        when:
        service.searchDois(10, 0, "")

        then:
        def query = JSON.parse(searchParams.receivedSearchRequest.source().query().toString())
        query.bool != null
        query.bool.must != null
        query.bool.must.size() == 1
        query.bool.must[0].match_all != null
    }

    def "searchDois should use the non-analyzed keyword field to filter supplied filter terms"() {

        setup:
        Map searchParams = [:]
        mockSearch(searchParams)

        when:
        service.searchDois(10, 0, "test", [fieldName:"filterTerm"])

        then:
        def query = JSON.parse(searchParams.receivedSearchRequest.source().query().toString())
        query.bool.must[0].simple_query_string.query == 'test'
        query.bool.filter.term[0]['fieldName.keyword'].value == 'filterTerm'
    }

    private void mockSearch(Map resultHolder) {
        service.elasticSearchService.metaClass.search = { SearchRequest searchRequest, Map params ->
            resultHolder.receivedSearchRequest = searchRequest
            resultHolder.receivedParams = params
            return new ElasticSearchResult()
        }
    }
}
