package au.org.ala.doi

import au.org.ala.doi.providers.DataCiteService
import au.org.ala.doi.providers.DoiAmazonS3Service
import au.org.ala.doi.storage.DoiStorageProvider
import au.org.ala.doi.storage.FileStorage
import au.org.ala.doi.storage.S3Storage
import au.org.ala.doi.storage.Storage
import au.org.ala.doi.storage.SwiftStorage
import com.amazonaws.services.s3.model.CannedAccessControlList
import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import groovy.util.logging.Slf4j
import org.gbif.datacite.rest.client.configuration.ClientConfiguration
import org.gbif.datacite.rest.client.retrofit.DataCiteRetrofitSyncClient
import org.gbif.doi.service.datacite.RestJsonApiDataCiteService
import org.javaswift.joss.client.factory.AccountConfig
import org.javaswift.joss.client.factory.AccountFactory
import org.javaswift.joss.model.Account
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Lazy

@Slf4j
class Application extends GrailsAutoConfiguration {
    static void main(String[] args) {
        System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "true")
        GrailsApp.run(Application, args)
    }

    @Bean
    Storage storage(DoiAmazonS3Service doiAmazonS3Service) {
        String doiStorageProvider = grailsApplication.config.getProperty('doi.storage.provider', String)
        String fileStoreLocation = grailsApplication.config.getProperty('file.store', String)
        Boolean privateCloudStorage = grailsApplication.config.getProperty('doi.storage.cloud.private', Boolean, true)

        DoiStorageProvider provider = DoiStorageProvider.fromString(doiStorageProvider)
        switch(provider) {
            case DoiStorageProvider.S3:
                log.debug("Using S3 for storage provider")
                def acl = privateCloudStorage ? CannedAccessControlList.Private : CannedAccessControlList.PublicRead
                return new S3Storage(doiAmazonS3Service, acl)
            case DoiStorageProvider.LOCAL:
                log.debug("Using local file system for storage provider")
                return new FileStorage(fileStoreLocation)
            case DoiStorageProvider.SWIFT:
                log.debug("Using SwiftStack for storage provider")
                def container = grailsApplication.config.getProperty('doi.storage.swift.container', String)
                return new SwiftStorage(account(), container, privateCloudStorage)
            default:
                throw new IllegalStateException("Configuration property doi.storage.provider not supported.  Value is ${doiStorageProvider}.")
        }
    }

    @Bean
    @Lazy
    Account account() {
        def username = grailsApplication.config.getProperty('doi.storage.swift.username', String)
        def password = grailsApplication.config.getProperty('doi.storage.swift.password', String)
        def authUrl = grailsApplication.config.getProperty('doi.storage.swift.authUrl', String)
        def tenantId = grailsApplication.config.getProperty('doi.storage.swift.tenantId', String)
        def tenantName = grailsApplication.config.getProperty('doi.storage.swift.tenantName', String)
        AccountConfig config = new AccountConfig()
        config.with {
            setUsername(username)
            setPassword(password)
            setAuthUrl(authUrl)
            setTenantId(tenantId)
            setTenantName(tenantName)
        }

        return new AccountFactory(config).createAccount()
    }
    
    @Bean
    @Lazy
    ClientConfiguration dataCiteClientConf() {
        ClientConfiguration clientConf = ClientConfiguration.builder().withBaseApiUrl(
                grailsApplication.config.datacite.doi.service.baseApiUrl as String).withTimeOut(
                grailsApplication.config.datacite.doi.service.timeOut as long).withFileCacheMaxSizeMb(
                grailsApplication.config.datacite.doi.service.fileCacheMaxSizeMb as long).withUser(
                grailsApplication.config.datacite.doi.service.user as String).withPassword(
                grailsApplication.config.datacite.doi.service.password as String).build()
        log.info "Using $grailsApplication.config.datacite.doi.service.baseApiUrl baseApiUrl"
        return clientConf
    }

    @Bean
    @Lazy
    DataCiteRetrofitSyncClient dataCiteClient(ClientConfiguration dataCiteClientConf) {
        return new DataCiteRetrofitSyncClient(dataCiteClientConf)
    }

    @Bean
    @Lazy
    RestJsonApiDataCiteService restDataCiteService(DataCiteRetrofitSyncClient dataCiteClient) {
        return new RestJsonApiDataCiteService(dataCiteClient)
    }

}
