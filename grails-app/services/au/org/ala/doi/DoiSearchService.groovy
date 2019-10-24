package au.org.ala.doi

import grails.gorm.transactions.ReadOnly
import grails.plugins.elasticsearch.ElasticSearchResult
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery
import static org.elasticsearch.index.query.QueryBuilders.simpleQueryStringQuery
import static org.elasticsearch.index.query.QueryBuilders.termQuery


/**
 * Wraps the plugin ElasticSearchService to modify the data mapping behaviour.  The plugin maps data directly
 * from the search results using grails web data binding.  However, this will exclude some properties deemed
 * read-only (such as dateCreated/lastUpdated).
 * Instead, this service will query for IDs only and return results from the database.
 */
class DoiSearchService {

    // The grails elasticSearch plugin indexes string properties twice by default:
    // first as an analysed field matching the property name,
    // and second as a non-analyzed field with the suffix 'keyword'
    private static String FILTER_FIELD_SUFFIX = '.keyword'

    def elasticSearchService

    /**
     * Builds an elasticsearch query to search DOIs.
     * @param pageSize the number of results to return (paging support)
     * @param startFrom the index of the first result to return (paging support)
     * @param filterParams a Map of fieldname and value used to construct a search filter
     * @param searchTerm a search term that will be used to construct a simpleQueryStringQuery
     *  See: {@link org.apache.lucene.queryparser.simple.SimpleQueryParser} for the supported syntax
     * @param sortBy the field name to sort results by.  If null, the default sort is by relevance.
     * @param sortOrder asc or dsc specifies the order for sorted results.
     */
    @ReadOnly
    ElasticSearchResult searchDois(int pageSize, int startFrom, String searchTerm = "", Map filterParams = null, String sortBy = null, String sortOrder = "dsc") {

        QueryBuilder query = searchTerm ? simpleQueryStringQuery(searchTerm) : matchAllQuery()
        BoolQueryBuilder builder = QueryBuilders.boolQuery().must(query)

        filterParams.each { name, value ->
            builder.filter(termQuery(name+FILTER_FIELD_SUFFIX, value))
        }

        Map params = [from:startFrom, size:pageSize]
        if (sortBy) { // Default is sort by relevance.
            params.sort = sortBy
            params.order = sortOrder
        }
        search(params, builder)
    }

    /**
     * Modifies the way results are returned from the ElasticSearchService.
     * See: {@link grails.plugins.elasticsearch.ElasticSearchService#search(Map, QueryBuilder)}
     */
    private ElasticSearchResult search(Map params, QueryBuilder query) {

        SearchRequest request = elasticSearchService.buildSearchRequest(query, null, params)
        // We are only retrieving the DOI ids from the search to avoid fetching and parsing the data as the
        // matching results will be retrieved from the database.
        request.source.fetchSource(['id'] as String[], new String[0])
        ElasticSearchResult searchResult = elasticSearchService.search(request, params)
        if (searchResult.searchResults) {
            List matchedDoiIds = searchResult.searchResults.collect{ it.id }
            searchResult.searchResults = Doi.findAllByIdInList(matchedDoiIds)
        }

        searchResult
    }
}
