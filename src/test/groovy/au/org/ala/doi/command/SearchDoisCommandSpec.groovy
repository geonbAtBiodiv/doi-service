package au.org.ala.doi.command

import au.org.ala.doi.SearchDoisCommand
import spock.lang.Specification


class SearchDoisCommandSpec extends Specification {


    def "it is valid to use all defaults for a DOI search"() {
        expect:
        new SearchDoisCommand().validate() == true
    }

    def "the command will convert string filters into a map"() {
        setup:
        SearchDoisCommand command = new SearchDoisCommand(q:"query", fq:["field1:value1"])

        expect:
        command.validate() == true
        command.filters == [field1:"value1"]
    }

    def "each fq filter parameter must be formatted as name:value"() {
        setup:
        SearchDoisCommand command = new SearchDoisCommand(q:"query", fq:["field1:value1", "field2-malformed"])

        expect:
        command.validate() == false
        command.errors.getFieldError('fq') != null

    }


    def "each field may only appear once in the fq filter parameter list"() {
        setup:
        SearchDoisCommand command = new SearchDoisCommand(q:"query", fq:["field1:value1", "field1:value2"])

        expect:
        command.validate() == false
        command.errors.getFieldError('fq') != null

    }
}
