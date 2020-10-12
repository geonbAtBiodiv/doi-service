package au.org.ala.doi.ws

import groovy.transform.CompileStatic
import org.elasticsearch.search.aggregations.Aggregation
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField

/**
 * Wrapper class to keep webservice response consistent
 * Introduced due to ES Grails plugin change from 2.4.2 to 2.7.0.RC1
 */
@CompileStatic
class DoiElasticSearchResult {
    Long total
    String totalRel
    List searchResults = []
    List<Map<String, HighlightField>> highlight = []
    Map<String, Float> scores = [:]
    Map<String, Object[]> sort = [:]
    Map<String, Aggregation> aggregations = [:]
}