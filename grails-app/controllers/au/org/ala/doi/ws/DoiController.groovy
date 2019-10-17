package au.org.ala.doi.ws

import au.ala.org.ws.security.RequireApiKey
import au.ala.org.ws.security.SkipApiKeyCheck
import au.org.ala.doi.BasicWSController
import au.org.ala.doi.MintResponse
import au.org.ala.doi.SearchDoisCommand
import au.org.ala.doi.exceptions.DoiNotFoundException
import au.org.ala.doi.exceptions.DoiUpdateException
import au.org.ala.doi.exceptions.DoiValidationException
import au.org.ala.doi.storage.Storage
import com.google.common.io.ByteSource
import grails.plugins.elasticsearch.ElasticSearchResult
import grails.web.http.HttpHeaders
import io.swagger.annotations.Api
import io.swagger.annotations.ApiImplicitParam
import io.swagger.annotations.ApiImplicitParams
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import io.swagger.annotations.ResponseHeader
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.MultipartHttpServletRequest

import javax.servlet.http.HttpServletRequest
import javax.validation.constraints.NotNull

import static au.org.ala.doi.util.Utils.isUuid

import au.org.ala.doi.Doi
import au.org.ala.doi.util.DoiProvider
import au.org.ala.doi.DoiService
import grails.converters.JSON

import static javax.servlet.http.HttpServletResponse.SC_CREATED
import static javax.servlet.http.HttpServletResponse.SC_OK

@Api(value = "/api", tags = ["DOI"], description = "DOI API")
@RequireApiKey
class DoiController extends BasicWSController {

    static responseFormats = ['json']

    static namespace = "v1"

    DoiService doiService
    Storage storage

    /**
     * Mint a new DOI. POST only. Must have an ALA API Key.
     *
     * This endpoint accepts 2 formats:
     * <ol>
     *     <li>A Multipart Request, where the metadata is in a parameter called 'json' and the file associated with the DOI is provided in the request; or
     *     <li>A standard post with a JSON body, with a mandatory 'fileUrl' property containing a URL where the file for the DOI can be downloaded from.
     * </ol>
     *
     * The request must have JSON object with the following structure:
     * <pre>
     * {
     *     provider: "ANDS", // the doi provider to use (see {@link DoiProvider} for a list of supported providers)
     *     applicationUrl: "http://....", // the url to the relevant page on the source application. This is NOT the landing page: it is used to provide a link ON the landing page back to the original source of the publication/data/etc for the DOI.
     *     providerMetadata: { // the provider-specific metadata to be sent with the DOI minting request
     *         ...
     *     },
     *     title: "...", // title to be displayed on the landing page
     *     authors: "...", // author(s) to be displayed on the landing page
     *     description: "...", // description to be displayed on the landing page
     *
     *
     *     // the following are optional
     *     fileUrl: "http://....", // the url to use to download the file for the DOI (use this, or send the file as a multipart request)
     *     customLandingPageUrl: "http://...", // an application-specific landing page that you want the DOI to resolve to. If not provided, the default ALA-DOI landing page will be used.
     *     applicationMetadata: { // any application-specific metadata you want to display on the landing page in ALA-DOI
     *         ...
     *     }
     * }
     * </pre>
     *
     * If "fileUrl" is not provided, then you must send the file in a multipart request with the metadata as a JSON string in a form part called 'json'.
     *
     * @return JSON response containing the DOI and the landing page on success, HTTP 500 on failure
     */
    @ApiOperation(
            value = "Mint / Register / Reserve a DOI",
            nickname = "doi",
            produces = "application/json",
            consumes = "application/json",
            httpMethod = "POST",
            response = MintResponse,
            code = 201,
            responseHeaders = [
                    @ResponseHeader(
                            name = 'Location',
                            description = 'URL for minted / registered / reserved DOI',
                            response = String
                    )
            ]
    )
    @ApiResponses([
            @ApiResponse(code = 405,
                    message = "Method Not Allowed. Only POST is allowed"),
            @ApiResponse(code = 500,
                    message = "If the DOI already exists or there is an error while storing the file or contacting the DOI service")
    ])
    @ApiImplicitParams([
            @ApiImplicitParam(
                    paramType = "body",
                    value = "JSON request body.  The metadata for the mint request, may include a fileUrl that this service will fetch and use as the file for the DOI.  Provider metadata is provider specific",
                    dataType = 'au.org.ala.doi.MintRequest'),
            @ApiImplicitParam(name = "Accept-Version",
                    paramType = "header",
                    required = true,
                    dataType = "string",
                    defaultValue = "1.0",
                    allowableValues = "1.0",
                    value = "The API version"),
            @ApiImplicitParam(name = "apiKey",
                    paramType = "header",
                    required = true,
                    dataType = "string",
                    value = "An valid API Key from the apikey service")
    ])
    def save() {
        Map json = getJson(request)

        if (validateMintRequest(json)) {
            MultipartFile file = null
            if (request instanceof MultipartHttpServletRequest) {
                file = request.getFile(request.fileNames[0])
            }

            MintResponse result = doiService.mintDoi(DoiProvider.byName(json.provider), json.providerMetadata, json.title,
                    json.authors, json.description, json.licence, json.applicationUrl, json.fileUrl, file, json.applicationMetadata,
                    json.customLandingPageUrl, null, json.userId, json.active, json.authorisedRoles, json.displayTemplate)

            if (result?.uuid) {
                response.addHeader(HttpHeaders.LOCATION,
                        grailsLinkGenerator.link( method: 'GET', resource: this.controllerName, action: 'show',id: result.uuid, absolute: true,
                                namespace: hasProperty('namespace') ? this.namespace : null ))
            }
            render result as JSON, status: SC_CREATED
        }
    }

    /**
     * Dummy method to enumerate the multipart file upload to swigger
     */
    @ApiOperation(
            value = "Mint / Register / Reserve a DOI",
            nickname = "doi",
            produces = "application/json",
            consumes = "multipart/form-data",
            httpMethod = "PUT",
            response = MintResponse,
            code = 201,
            responseHeaders = [
                    @ResponseHeader(
                            name = 'Location',
                            description = 'URL for minted / registered / reserved DOI',
                            response = String
                    )
            ]
    )
    @ApiResponses([
            @ApiResponse(code = 405,
                    message = "Method Not Allowed. Only POST is allowed"),
            @ApiResponse(code = 500,
                    message = "If the DOI already exists or there is an error while storing the file or contacting the DOI service")
    ])
    @ApiImplicitParams([
            @ApiImplicitParam(name="file",
                    paramType = "formData",
                    value = "The file to upload",
                    dataType = 'file'),
            @ApiImplicitParam(name="json",
                    paramType = "formData",
                    value = "JSON request body.  The metadata for the mint request.  Provider metadata is provider specific.",
                    dataType = 'au.org.ala.doi.MintRequest'),
            @ApiImplicitParam(name = "Accept-Version",
                    paramType = "header",
                    required = true,
                    dataType = "string",
                    defaultValue = "1.0",
                    allowableValues = "1.0",
                    value = "The API version"),
            @ApiImplicitParam(name = "apiKey",
                    paramType = "header",
                    required = true,
                    dataType = "string",
                    value = "An valid API Key from the apikey service")
    ])
    def upload() {
    }

    private static Map getJson(HttpServletRequest request) {
        Map json = request.getJSON()

        if (!json && request instanceof MultipartHttpServletRequest) {
            json = JSON.parse(request.getParameter("json")) as Map
        }

        json
    }

    private boolean validateMintRequest(Map json) {
        boolean valid = true

        if (areMandatoryMetadataFieldsMissing(json)) {
            log.debug("Rejecting request with missing mandatory parameters. Provided parameters: ${json}")
            badRequest "provider, title, authors, description, applicationUrl and providerMetadata must be provided " +
                    "in the request's JSON body"

            valid = false
        } else if (!DoiProvider.byName(json.provider)) {
            log.debug("Rejecting request with invalid provider ${json.provider}")
            badRequest "invalid provider: must be one of ${DoiProvider.values()*.name().join(", ")}"

            valid = false
        }

        valid
    }

    private static boolean areMandatoryMetadataFieldsMissing(json) {
        !json.provider || !json.applicationUrl || !json.providerMetadata || !json.title || !json.authors || !json.description
    }

    private static boolean areFileAndUrlMissing(json, request) {
        !json.fileUrl && (!(request instanceof MultipartHttpServletRequest) || !request.fileNames)
    }

    @ApiOperation(
            value = "List DOIs",
            nickname = "doi",
            produces = "application/json",
            httpMethod = "GET",
            response = Doi,
            responseContainer = "List",
            responseHeaders = [
                    @ResponseHeader(
                            name = 'Link',
                            description = 'Paging links',
                            response = String
                    ),
                    @ResponseHeader(
                            name = 'X-Total-Count',
                            description = 'Total number of results matching parameters',
                            response = Integer
                    )
            ]
    )
    @ApiResponses([
            @ApiResponse(code = 405,
                    message = "Method Not Allowed. Only GET is allowed")
    ])
    @ApiImplicitParams([
            @ApiImplicitParam(name = "max",
                    paramType = "query",
                    required = false,
                    defaultValue = '10',
                    value = "max number of dois to return",
                    dataType = "Integer"),
            @ApiImplicitParam(name = "offset",
                    paramType = "query",
                    required = false,
                    defaultValue = '0',
                    value = "index of the first record to return",
                    dataType = "Integer"),
            @ApiImplicitParam(name = "sort",
                    paramType = "query",
                    required = false,
                    defaultValue = 'dateMinted',
                    value = "the field to sort the results by",
                    allowableValues = 'dateMinted,dateCreated,lastUpdated,title',
                    dataType = "string"),
            @ApiImplicitParam(name = "order",
                    paramType = "query",
                    required = false,
                    defaultValue = 'asc',
                    value = "the direction to sort the results by",
                    allowableValues = 'asc,desc',
                    dataType = "string"),
            @ApiImplicitParam(name = "userId",
                    paramType = "query",
                    required = false,
                    value = "Add a userid filter, userid should be the user's numeric user id",
                    dataType = "string"),
            @ApiImplicitParam(name = "activeStatus",
                    paramType = "query",
                    required = false,
                    value = "Filters DOIs returned based on active flag. Valid values are 'all', 'active' or 'inactive'. If omitted it defaults to 'active'",
                    dataType = "string"),
            @ApiImplicitParam(name = "Accept-Version",
                    paramType = "header",
                    required = true,
                    dataType = "string",
                    defaultValue = "1.0",
                    allowableValues = "1.0",
                    value = "The API version")
    ])
    @SkipApiKeyCheck
    def index(Integer max) {

        max = Math.min(max ?: 10, 100)
        int offset = params.int('offset', 0)
        String sort = params.get('sort', 'dateMinted')
        String order = params.get('order', 'desc')

        String userId = params.get('userId')
        String title = params.get('title')
        String authors = params.get('authors')
        String licence = params.get('licence')
        String activeStatus = params.boolean("activeStatus")


        def eqParams = [:]
        if (userId) eqParams << [ userId : userId ]
        if (title) eqParams << [ title : title ]
        if (authors) eqParams << [ authors : authors ]
        if (licence) eqParams << [ licence : licence ]


        if (activeStatus != null) {
            if(activeStatus == 'inactive') {
                eqParams << [ active : false ]
            } else if(activeStatus == 'all') {
                // just skip the filter completely
            } else {
                // Any other value will force default filter
                eqParams << [ active : true ]
            }
        } else {
            eqParams << [ active : true ]
        }

        def list = doiService.listDois(max, offset, sort, order, eqParams)
        def totalCount = list.totalCount

        addPaginationHeaders(eqParams, totalCount, offset, max, sort, order)

        respond list
    }

    private void addPaginationHeaders(Map nonPaginationParams, int totalCount, int offset, int max, String sort, String order) {
        response.addIntHeader('X-Total-Count', totalCount)
        if (offset + max < totalCount) {
            response.addHeader('Link', createLink(params: nonPaginationParams + [max: max, offset: offset + max, sort: sort, order: order]) + '; rel="next"')
        }
        if (offset > 0) {
            response.addHeader('Link', createLink(params: nonPaginationParams + [max: max, offset: Math.max(0, offset - max), sort: sort, order: order]) + '; rel="prev"')
        }
        response.addHeader('Link', createLink(params: nonPaginationParams + [max: max, offset: 0, sort: sort, order: order]) + '; rel="first"')
        response.addHeader('Link', createLink(params: nonPaginationParams + [max: max, offset: Math.max(0, totalCount - max), sort: sort, order: order]) + '; rel="last"')
    }


    @ApiOperation(
            value = "Search DOIs",
            nickname = "doi/search",
            produces = "application/json",
            httpMethod = "GET",
            response = Doi,
            responseContainer = "List",
            responseHeaders = [
                    @ResponseHeader(
                            name = 'Link',
                            description = 'Paging links',
                            response = String
                    ),
                    @ResponseHeader(
                            name = 'X-Total-Count',
                            description = 'Total number of results matching parameters',
                            response = Integer
                    )
            ]
    )
    @ApiResponses([
            @ApiResponse(code = 405,
                    message = "Method Not Allowed. Only GET is allowed")
    ])
    @ApiImplicitParams([
            @ApiImplicitParam(name = "q",
                    paramType = "query",
                    required = false,
                    defaultValue = "",
                    value = "An elasticsearch Simple Query String formatted string.",
                    dataType = "string"),
            @ApiImplicitParam(name = "max",
                    paramType = "query",
                    required = false,
                    defaultValue = '10',
                    value = "max number of dois to return",
                    dataType = "Integer"),
            @ApiImplicitParam(name = "offset",
                    paramType = "query",
                    required = false,
                    defaultValue = '0',
                    value = "index of the first record to return",
                    dataType = "Integer"),
            @ApiImplicitParam(name = "sort",
                    paramType = "query",
                    required = false,
                    defaultValue = 'dateMinted',
                    value = "the field to sort the results by",
                    allowableValues = 'dateMinted,dateCreated,lastUpdated,title',
                    dataType = "string"),
            @ApiImplicitParam(name = "order",
                    paramType = "query",
                    required = false,
                    defaultValue = 'asc',
                    value = "the direction to sort the results by",
                    allowableValues = 'asc,desc',
                    dataType = "string"),
            @ApiImplicitParam(name = "fq",
                    required = false,
                    value = "filters the search results by by supplied fields.  Each value must be a string of the form fieldName:filterTerm.  To filter on DOI applicationMetadata, use a fieldName of 'applicationMetadata.field'",
                    dataType = "array"),
            @ApiImplicitParam(name = "Accept-Version",
                    paramType = "header",
                    required = true,
                    dataType = "string",
                    defaultValue = "1.0",
                    allowableValues = "1.0",
                    value = "The API version")
    ])
    @SkipApiKeyCheck
    def search(SearchDoisCommand command) {

        if (!command.validate()) {
            respond command
        }
        else {
            ElasticSearchResult result = doiService.searchDois(command.max, command.offset, command.q, command.filters, command.sort, command.order)
            def totalCount = result.total as int

            Map queryParams = [q:command.q, fq:command.fq]
            addPaginationHeaders(queryParams, totalCount, command.offset, command.max, command.sort, command.order)

            respond result
        }
    }


    /**
     * Retrieve the metadata for a doi by either UUID or DOI
     *
     * @param id Either the local UUID or the DOI identifier
     * @return JSON response containing the metadata for the requested doi
     */
    @ApiOperation(
            value = "Get a stored DOI and its metadata",
            nickname = "doi/{id}",
            produces = "application/json",
            httpMethod = "GET",
            response = Doi
    )
    @ApiResponses([
            @ApiResponse(code = 404,
                    message = "DOI or UUID not found in this system"),
            @ApiResponse(code = 405,
                    message = "Method Not Allowed. Only GET, PUT, POST, PATCH is supported")
    ])
    @ApiImplicitParams([
            @ApiImplicitParam(name = "id",
                    paramType = "path",
                    dataType = "string",
                    required = true,
                    value = "Either the DOI (encoded or unencoded) or the UUID"),
            @ApiImplicitParam(name = "Accept-Version",
                    paramType = "header",
                    required = true,
                    dataType = "string",
                    defaultValue = "1.0",
                    allowableValues = "1.0",
                    value = "The API version"),
    ])
    @SkipApiKeyCheck
    def show(@NotNull String id) {
        Doi doi = queryForResource(id)

        if (!doi) {
            notFound "No doi was found for ${params.id}"
        } else {
            render doi as JSON
        }
    }

    /**
     * Retrieve the file for a doi by either UUID or DOI
     *
     * @param id Either the local UUID or the DOI identifier
     * @return the file associated with the DOI
     */
    @ApiOperation(
            value = "Download the file associated with a DOI",
            nickname = "doi/{id}/download",
            produces = "application/octet-stream",
            httpMethod = "GET",
            response = File
    )
    @ApiResponses([
            @ApiResponse(code = 404,
                    message = "DOI or UUID not found in this system"),
            @ApiResponse(code = 405,
                    message = "Method Not Allowed. Only GET is supported"),
            @ApiResponse(code = 401,
                    message = "The file contains sensitive data and requires authentication. Use the DOI Service Web UI to download the file")
    ])
    @ApiImplicitParams([
            @ApiImplicitParam(name = "id",
                    paramType = "path",
                    dataType = "string",
                    required = true,
                    value = "Either the DOI (encoded or unencoded) or the UUID"),
            @ApiImplicitParam(name = "Accept-Version",
                    paramType = "header",
                    required = true,
                    dataType = "string",
                    defaultValue = "1.0",
                    allowableValues = "1.0"),
    ])
    @SkipApiKeyCheck
    def download(@NotNull String id) {
        Doi doi = queryForResource(id)

        if (!doi) {
            notFound "No doi was found for ${id}"
        } else if(doi.authorisedRoles) {
            response.addHeader('Link', createLink(uri: "/doi/${doi.uuid}/download") + '; rel="alternate"')
            notAuthorised "Sensitive data files can only be downloaded via DOI Service GUI for authenticated users only"
        } else {
            ByteSource byteSource = storage.getFileForDoi(doi)
            if (byteSource) {
                response.setContentType(doi.contentType)
                response.setHeader("Content-disposition", "attachment;filename=${doi.filename}")
                byteSource.openStream().withStream {
                    response.outputStream << it
                }
                response.outputStream.flush()
            } else {
                notFound "No file was found for DOI ${doi.doi} (uuid = ${doi.uuid})"
            }
        }
    }

    @ApiOperation(
            value = "Update the stored metadata or add a file to a DOI",
            nickname = "doi/{id}",
            produces = "application/json",
            consumes = "application/json",
            httpMethod = "PATCH",
            response = Doi,
            responseHeaders = [
                    @ResponseHeader(
                            name = 'Location',
                            description = 'URL for minted / registered / reserved DOI',
                            response = String
                    )
            ]
    )
    @ApiResponses([
            @ApiResponse(code = 405,
                    message = "Method Not Allowed. Only GET, PUT, POST, PATCH is supported"),
            @ApiResponse(code = 400,
                    message = "Attempting to update the file when there is already an existing file"),
            @ApiResponse(code = 404,
                    message = "DOI or UUID not found in this system"),
            @ApiResponse(code = 422,
                    message = "If the request body creates an invalid DOI entry"),
            @ApiResponse(code = 500,
                    message = "There is an error while storing the file or contacting the DOI service")
    ])
    @ApiImplicitParams([
            @ApiImplicitParam(name = "id",
                    paramType = "path",
                    required = true,
                    dataType = "string",
                    value = "Either the DOI (encoded or unencoded) or the UUID"),
            @ApiImplicitParam(
                    paramType = "body",
                    required = true,
                    dataType = 'au.org.ala.doi.UpdateRequest',
                    value = "The values to update the DOI with.  This will patch the existing DOI object with the provided values.  Only the following values are accepted: 'providerMetadata', 'customLandingPageUrl', 'title', 'authors', 'description', 'licence', 'applicationUrl','applicationMetadata'"),
            @ApiImplicitParam(name = "Accept-Version",
                    paramType = "header",
                    required = true,
                    dataType = "string",
                    defaultValue = "1.0",
                    allowableValues = "1.0",
                    value = "The API version"),
            @ApiImplicitParam(name = "apiKey",
                    paramType = "header",
                    required = true,
                    dataType = "string",
                    value = "An valid API Key from the apikey service")
    ])
    def patch(@NotNull String id) {
        update(id)
    }

    @ApiOperation(
            value = "Update the stored metadata or add a file to a DOI",
            nickname = "doi/{id}",
            produces = "application/json",
            consumes = "application/json",
            httpMethod = "PUT",
            response = Doi,
            responseHeaders = [
                    @ResponseHeader(
                            name = 'Location',
                            description = 'URL for minted / registered / reserved DOI',
                            response = String
                    )
            ]
    )
    @ApiResponses([
            @ApiResponse(code = 405,
                    message = "Method Not Allowed. Only GET, PUT, POST, PATCH is supported"),
            @ApiResponse(code = 400,
                    message = "Attempting to update the file when there is already an existing file"),
            @ApiResponse(code = 404,
                    message = "DOI or UUID not found in this system"),
            @ApiResponse(code = 422,
                    message = "If the request body creates an invalid DOI entry"),
            @ApiResponse(code = 500,
                    message = "There is an error while storing the file or contacting the DOI service")
    ])
    @ApiImplicitParams([
            @ApiImplicitParam(name = "id",
                    paramType = "path",
                    dataType = "string",
                    required = true,
                    value = "Either the DOI (encoded or unencoded) or the UUID"),
            @ApiImplicitParam(
                    paramType = "body",
                    required = true,
                    dataType = 'au.org.ala.doi.UpdateRequest',
                    value = "The values to update the DOI with.  This will patch the existing DOI object with the provided values.  Only the following values are accepted: 'providerMetadata', 'customLandingPageUrl', 'title', 'authors', 'description', 'licence', 'applicationUrl','applicationMetadata'"),
            @ApiImplicitParam(name = "Accept-Version",
                    paramType = "header",
                    required = true,
                    dataType = "string",
                    defaultValue = "1.0",
                    allowableValues = "1.0",
                    value = "The API version"),
            @ApiImplicitParam(name = "apiKey",
                    paramType = "header",
                    required = true,
                    dataType = "string",
                    value = "An valid API Key from the apikey service")
    ])
    def update(@NotNull String id) {

        def objectToBind = getJson(request)
        MultipartFile file = null
        if (request instanceof MultipartHttpServletRequest) {
            file = request.getFile(request.fileNames[0])
        }

        Doi instance
        try {
            instance = doiService.updateDoi(id, objectToBind, file)
        } catch (DoiNotFoundException e) {
            notFound()
            return
        } catch (DoiUpdateException e) {
            badRequest(e.message)
            return
        } catch (DoiValidationException e) {
            unprocessableEntity()
            return
        }

        response.addHeader(HttpHeaders.LOCATION,
                grailsLinkGenerator.link( resource: this.controllerName, action: 'show',id: id, absolute: true,
                        namespace: hasProperty('namespace') ? this.namespace : null ))
        respond instance, [status: SC_OK]
    }

    /** Dummy method for file upload update for Swagger  */
    @ApiOperation(
            value = "Update the stored metadata or add a file to a DOI",
            nickname = "doi/{id}",
            produces = "application/json",
            consumes = "multipart/form-data",
            httpMethod = "POST",
            response = Doi,
            responseHeaders = [
                    @ResponseHeader(
                            name = 'Location',
                            description = 'URL for minted / registered / reserved DOI',
                            response = String
                    )
            ]
    )
    @ApiResponses([
            @ApiResponse(code = 405,
                    message = "Method Not Allowed. Only GET, PUT, POST, PATCH is supported"),
            @ApiResponse(code = 400,
                    message = "Attempting to update the file when there is already an existing file"),
            @ApiResponse(code = 404,
                    message = "DOI or UUID not found in this system"),
            @ApiResponse(code = 422,
                    message = "If the request body creates an invalid DOI entry"),
            @ApiResponse(code = 500,
                    message = "There is an error while storing the file or contacting the DOI service")
    ])
    @ApiImplicitParams([
            @ApiImplicitParam(name = "id",
                    paramType = "path",
                    dataType = "string",
                    required = true,
                    value = "Either the DOI (encoded or unencoded) or the UUID"),
            @ApiImplicitParam(name="json",
                    paramType = "formData",
                    required = false,
                    dataType = 'au.org.ala.doi.UpdateRequest',
                    value = "The values to update the DOI with.  This will patch the existing DOI object with the provided values.  Only the following values are accepted: 'providerMetadata', 'customLandingPageUrl', 'title', 'authors', 'description', 'licence', 'applicationUrl','applicationMetadata'"),
            @ApiImplicitParam(name="file",
                    paramType = "formData",
                    required = false,
                    dataType = 'file',
                    value = "The file to upload"
            ),
            @ApiImplicitParam(name = "Accept-Version",
                    paramType = "header",
                    required = true,
                    dataType = "string",
                    defaultValue = "1.0",
                    allowableValues = "1.0",
                    value = "The API version"),
            @ApiImplicitParam(name = "apiKey",
                    paramType = "header",
                    required = true,
                    dataType = "string",
                    value = "An valid API Key from the apikey service")
    ])
    def updateUpload() {

    }

    protected Doi queryForResource(Serializable id) {
        String idString = id instanceof String ? id : id.toString()
        isUuid(idString) ? doiService.findByUuid(idString) : doiService.findByDoi(idString)
    }

}
