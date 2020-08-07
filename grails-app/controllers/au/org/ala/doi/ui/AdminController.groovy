package au.org.ala.doi.ui

import au.org.ala.doi.DoiService
import au.org.ala.doi.storage.Storage
import au.org.ala.doi.util.DoiProvider
import au.org.ala.web.AuthService
import grails.converters.JSON
import grails.plugins.elasticsearch.ElasticSearchService
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.MultipartHttpServletRequest

import static au.org.ala.doi.util.StateAssertions.*

class AdminController {

    DoiService doiService
    Storage storage
    AuthService authService
    ElasticSearchService elasticSearchService

    def index() {
        // Only used to render admin main page

        def listParams = [
            max: params.int('max', 20),
            offset: params.int('offset', 0)

        ]
        String sort = params.getOrDefault('sort', 'dateCreated')
        String order = params.getOrDefault('order', 'desc')

        def doiInstanceList = doiService.listDois(listParams.max, listParams.offset, sort, order)

        [
                doiInstanceList: doiInstanceList,
                listParams: listParams,
                storageType: storage.description
        ]
    }

    def mintDoi() {
        // Only used to render mint DOI page
    }

    /**
     * Mint a new DOI or register and existing DOI within the DOI Service
     *
     */
    def createDoi() {
        try {
            log.debug("params: ${params}")

            checkArgument params.authors, "Authors is required"
            checkArgument params.title, "Title is required"
            checkArgument params.description, "Description is required"
            checkArgument params.applicationUrl, "Application URL is required"
            checkArgument params.newExistingDoiRadio, "Either Mint New DOI or Register existing DOI must be selected"

            if (!params.file && !params.fileUrl) {
                throw new IllegalArgumentException("Either File or File URL needs to be provided")
            }

            MultipartFile file = null
            if (request instanceof MultipartHttpServletRequest) {
                file = request.getFile(request.fileNames[0])
                log.debug("File ${file}")
                if (!file?.size) {
                    // Ignore empty files, looks like we are getting an empty file for the missing file parameter rather than no file at all.                 //
                    file = null
                }
            }

            def applicationMetadata
            if (params?.applicationMetadata && params.applicationMetadata.trim()) {
                applicationMetadata = JSON.parse(params.applicationMetadata)
            }

            def providerMetadata
            if (params?.newExistingDoiRadio == "existing") {
                checkArgument params.existingDoi, "Existing DOI is required if registering an existing DOI"
            } else {
                checkArgument params.providerMetadata, "Provider metadata is required if minting a new DOI"
                checkArgument params.provider, "Provider is required if minting a new DOI"
                providerMetadata = JSON.parse(params.providerMetadata)
            }

            DoiProvider provider = DoiProvider.byName(params.provider)

            def userId = params.boolean('linkToUser', false) ? authService.userId : null

            def result = doiService.mintDoi(
                    provider,
                    providerMetadata,
                    params.title,
                    params.authors,
                    params.description,
                    [params.licence],
                    params.applicationUrl,
                    params.fileUrl,
                    file,
                    applicationMetadata,
                    params.customLandingPageUrl,
                    params.existingDoi,
                    userId,
                    true,
                    []
            )

            log.debug("Result: ${result}")
            if (!result?.error) {
                redirect(url: result.doiServiceLandingPage)
            } else {
                throw new Exception(result?.error)
            }

        }
        catch (e) {
            log.error("Error while trying to mint a DOI from UI: ${e}", e)
            def errorMessage = e?.cause?.message ?: e.message
            render view: "mintDoi", model: [status: "error", errorMessage: errorMessage, mintParameters: params]
        }
    }

    /**
     * Manual trigger to index all DOIs currently in the database.
     */
    def indexAll() {
        elasticSearchService.index()
        redirect action:'index'
    }
}
