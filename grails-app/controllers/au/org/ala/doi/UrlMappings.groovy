package au.org.ala.doi

class UrlMappings {

    static mappings = {

        // service api
        group("/api") {
            // Default route, no Accept-Version, should dupe the latest version
            head "/doi"(controller: "doi", action: "index", namespace: "v1")
            get "/doi"(controller: "doi", action: "index", namespace: "v1")
            post "/doi"(controller: "doi", action: "save", namespace: "v1")
            get "/doi/search"(controller: "doi", action: "search", namespace: "v1")
            head "/doi/$id**/download"(controller: "doi", action: "download", namespace: "v1")
            head "/doi/$id**"(controller: "doi", action: "show", namespace: "v1")
            get "/doi/$id**/download"(controller: "doi", action: "download", namespace: "v1")
            get "/doi/$id**"(controller: "doi", action: "show", namespace: "v1")
            post "/doi/$id**"(controller: "doi", action: "update", namespace: "v1")
            put "/doi/$id**"(controller: "doi", action: "update", namespace: "v1")
            patch "/doi/$id**"(controller: "doi", action: "patch", namespace: "v1")

            // Accept-Version: 1.0
            head "/doi"(version: "1.0", controller: "doi", action: "index", namespace: "v1")
            get "/doi"(version: "1.0", controller: "doi", action: "index", namespace: "v1")
            post "/doi"(version: "1.0", controller: "doi", action: "save", namespace: "v1")
            put "/doi"(version: "1.0", controller: "doi", action: "save", namespace: "v1") // work around for swagger
            head "/doi/$id**/download"(version: "1.0", controller: "doi", action: "download", namespace: "v1")
            head "/doi/$id**"(version: "1.0", controller: "doi", action: "show", namespace: "v1")
            get "/doi/$id**/download"(version: "1.0", controller: "doi", action: "download", namespace: "v1")
            get "/doi/$id**"(version: "1.0", controller: "doi", action: "show", namespace: "v1")
            post "/doi/$id**"(version: "1.0", controller: "doi", action: "update", namespace: "v1")
            put "/doi/$id**"(version: "1.0", controller: "doi", action: "update", namespace: "v1")
            patch "/doi/$id**"(version: "1.0", controller: "doi", action: "patch", namespace: "v1")

            "/"(controller: 'apiDoc', action: 'getDocuments', method: 'get')

            // legacy API mappings, DO NOT ADD V2 HERE!
            post "/v1/mintDoi"(controller: "doi", action: "save", namespace: "v1")
            get "/v1/doi/$id**/download"(controller: "doi", action: "download", namespace: "v1")
            get "/v1/doi/$id**"(controller: "doi", action: "show", namespace: "v1")
        }

        // User interface
        get "/logout/logout"(controller: "logout", action: 'logout')
        head "/doi/$id/download"(controller: "doiResolve", action: "download")
        get "/doi/$id/download"(controller: "doiResolve", action: "download")
        head "/doi/$id/downloadAuth"(controller: "doiResolve", action: "download")
        get "/doi/$id/downloadAuth"(controller: "doiResolve", action: "download")
        head "/doi/$id**"(controller: "doiResolve", action: "doi")
        get "/doi/$id**"(controller: "doiResolve", action: "doi")
        head "/doi"(controller: "doiResolve", action: "index")
        get "/doi"(controller: "doiResolve", action: "index")
        head "/myDownloads"(controller: "doiResolve", action: "myDownloads")
        get "/myDownloads"(controller: "doiResolve", action: "myDownloads")
        head "/"(controller: "doiResolve", action: "index")
        get "/"(controller: "doiResolve", action: "index")
        get "/admin"(controller:'admin', action:"index")
        get "/admin/mintDoi"(controller:'admin', action:"mintDoi")
        post "/admin/createDoi"(controller:'admin', action: "createDoi")
        get "/admin/indexAll"(controller:'admin', action:"indexAll")

        "500" view: "/error"
        "400" view: "/error"
        "404" view: "/notfound"
        "403" view: "/unauthorised"
        "401" view: "/unauthorised"
    }
}
