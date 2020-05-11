package au.org.ala.doi

import au.org.ala.doi.util.DoiProvider
import grails.util.Holders
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import au.org.ala.doi.ArrayType
import net.kaleidos.hibernate.usertype.JsonbMapType

@ToString
@EqualsAndHashCode
class Doi {

    static final Set<String> ALLOWED_UPDATABLE_PROPERTIES = [
            'providerMetadata', 'customLandingPageUrl', 'title', 'authors', 'description', 'licence', 'applicationUrl',
            'applicationMetadata', 'active'
    ].toSet()

    Long id

    UUID uuid
    String doi
    String title
    String authors
    String userId
    List<String> authorisedRoles

    List<String> licence
    String description
    Date dateMinted
    DoiProvider provider

    String filename
    String contentType
    byte[] fileHash
    Long fileSize

    Map providerMetadata
    Map applicationMetadata

    String customLandingPageUrl
    String applicationUrl

    Boolean active
    Long version
    Date dateCreated
    Date lastUpdated
    String displayTemplate

    static constraints = {
        applicationMetadata nullable: true
        customLandingPageUrl nullable: true, url: true
        applicationUrl nullable: true, url: true
        filename nullable: true
        contentType nullable: true
        fileHash nullable: true
        fileSize nullable: true, min: 0l
        userId nullable: true
        authorisedRoles nullable: true
        displayTemplate nullable: true
    }

    static mapping = {
        doi type: CitextType
        provider defaultValue: DoiProvider.forName(Holders.config.doi.service.provider)
        providerMetadata type: JsonbMapType
        applicationMetadata type: JsonbMapType
        active defaultValue: true
        licence type:ArrayType, params:[type: String]
        authorisedRoles type:ArrayType, params:[type: String]
    }

    def beforeValidate() {
        if (uuid == null) {
            uuid = UUID.randomUUID()
        }

        if(active == null) {
            active = true
        }
    }
}
