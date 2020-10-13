package au.org.ala.doi

import grails.validation.Validateable

/**
 * Accepts and validates parameters supplied to the doi search operation.
 */
class SearchDoisCommand implements Validateable {

    Integer max = 10
    Integer offset = 0
    String q = ""
    List<String> fq = null
    String sort = 'dateMinted'
    String order = 'desc'

    Map filters

    static constraints = {
        filters nullable: true
        fq nullable: true, validator: { val, obj ->  obj.errors.getFieldError('fq') == null}

    }

    Map getFilters() {
        if (!filters) {
            filters = parseFilters()
        }
        filters
    }

    /** Builds a map with key = fieldName value = filterTerm from a list of strings each formatted as "fieldName:filterTerm" */
    private Map parseFilters() {
        if (!fq) {
            return null
        }
        Map filters = [:]
        fq.each { String filter ->
            List nameValuePair = filter.split(":")
            if (nameValuePair.size() != 2) {
                errors.rejectValue("fq", "doi.param.fq.invalidFilter", [filter] as Object[], "Invalid fq parameter ${filter}.  Must be of the form fieldName:value")
            }
            if (filters[nameValuePair[0]]) {
                errors.rejectValue("fq","doi.param.fq.duplicateFilterFieldSpecified", [nameValuePair[0]] as Object[], "Invalid fq parameter ${nameValuePair[0]}.  Multiple filters for the same field are not supported.")
            }
            filters[nameValuePair[0]] = nameValuePair[1]
        }
        filters
    }
}
