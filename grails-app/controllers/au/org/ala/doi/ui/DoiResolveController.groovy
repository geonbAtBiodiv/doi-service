package au.org.ala.doi.ui

import au.org.ala.doi.BasicWSController
import au.org.ala.doi.Doi
import au.org.ala.doi.DoiService
import au.org.ala.doi.storage.Storage
import au.org.ala.web.AuthService
import au.org.ala.web.UserDetails
import au.org.ala.ws.validation.constraints.UUID
import com.google.common.io.ByteSource
import org.springframework.web.context.request.RequestContextHolder
import grails.converters.XML

import javax.validation.constraints.NotNull

import static au.org.ala.doi.util.Utils.isUuid

class DoiResolveController extends BasicWSController {

    static final int DEFAULT_PAGE_SIZE = 20
    static final String DEFAULT_DISPLAY_TEMPLATE = "default"
    DoiService doiService
    Storage storage
    AuthService authService

    protected Doi queryForResource(Serializable id) {
        String idString = id instanceof String ? id : id.toString()
        isUuid(idString) ? doiService.findByUuid(idString) : doiService.findByDoi(idString)
    }


    def index() {
        int pageSize = params.getInt("pageSize", DEFAULT_PAGE_SIZE)
        int offset = params.getInt("offset", 0)

        def isAdmin = RequestContextHolder.currentRequestAttributes()?.isUserInRole("ROLE_ADMIN")

        render view: "index", model: [dois    : doiService.listDois(pageSize, offset, "dateMinted", "desc", !isAdmin ? [active:true]:null),
                                      offset  : offset,
                                      pageSize: pageSize,
                                      isAdmin : isAdmin]
    }

    /**
     * Paginated list of downloads for logged-in user
     *
     * @return
     */
    //TODO Implement myDois page in the same fashion as myDownloads when there is another client minting DOIs
    // for a specific application (profiles for instance).
    def myDownloads() {
        String userId = authService?.getUserId()
        Integer max = Math.min(params.int('max', 10), 100)
        int offset = params.int('offset', 0)
        String sort = params.get('sort', 'dateCreated')
        String order = params.get('order', 'desc')
        log.debug "myDownloads params = ${params}"

        if (userId) {
            def result = doiService.listDois(max, offset, sort, order, [active:true, userId:userId, displayTemplate:"biocache"])
            render view: 'myDownloads', model: [dois: result, totalRecords: result.totalCount]
        } else {
            render(status: "401", text: "No UserId provided - check user is logged in and page is protected by AUTH")
        }
    }

    def doi(@NotNull String id) {
        Doi doi = queryForResource(id)
        if (doi) {
            def isAdmin = RequestContextHolder.currentRequestAttributes()?.isUserInRole("ROLE_ADMIN")

            // Get the right template
            String displayTemplate = params.boolean('useDefaultTemplate', false) ? DEFAULT_DISPLAY_TEMPLATE : doi.displayTemplate
            List<String> availableDisplayTemplates = grailsApplication.config.getProperty('doi.displayTemplates', List)

            if(!availableDisplayTemplates.contains(displayTemplate)) {
                displayTemplate = DEFAULT_DISPLAY_TEMPLATE
        }
            render view: "doi", model: [doi: doi, isAdmin : isAdmin, displayTemplate: displayTemplate]
        } else {
            notFound "No DOI record was found for ${id}"
        }
    }

    def download(@NotNull @UUID String id) {
        Doi doi = doiService.findByUuid(id)

        if (!doi) {
            notFound "No doi was found for ${id}"
        } else {

            // if the DOI is restricted, let's drill down to see if the user trying to access the file should be allowed
            // Requestor in this context is the user trying to download the file
            if(doi.authorisedRoles){
                String requestorId = authService.userId
                if(!requestorId) {
                    // Force authentication for the same download
                    // This method will end up being called again but with an authenticated user.
                    redirect uri: authService.loginUrl( "/doi/${doi.uuid}")
                    log.debug("File for DOI ${doi.doi} (uuid = ${doi.uuid}) is not public, user needs to login to see if he is permitted to download it.")
                    return
                } else if(requestorId != doi.userId) {
                    // must be authorised for all roles
                    doi.authorisedRoles.each {
                        if (authService.userInRole(it)) {
                            render view: "unauthorisedDownload", model: [doi: doi] //TODO
                            log.debug("File for DOI ${doi.doi} (uuid = ${doi.uuid}) is not public and user ${requestorId} does not have the apropriate permissions to access it")
                            return
                        }
                    }
                }
            }

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
}
