<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="${grailsApplication.config.skin.layout}n"/>
    <title>ALA DOI Repository</title>

    <!-- HTML5 shim and Respond.js for IE8 support of HTML5 elements and media queries -->
    <!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
    <!--[if lt IE 9]>
        <script src="https://oss.maxcdn.com/html5shiv/3.7.2/html5shiv.min.js"></script>
        <script src="https://oss.maxcdn.com/respond/1.4.2/respond.min.js"></script>
    <![endif]-->

    %{-- Google Analytics --}%
    <script>
        window.ga = window.ga || function () {
                    (ga.q = ga.q || []).push(arguments)
                };
        ga.l = +new Date;
        ga('create', '${grailsApplication.config.googleAnalyticsId}', 'auto');
        ga('send', 'pageview');
    </script>
    <script async src='//www.google-analytics.com/analytics.js'></script>
    %{--End Google Analytics--}%

    <asset:stylesheet src="doi.css"/>
</head>

<body>
<g:include view="common/_envWarning.gsp"/>

<div class="panel">
    <div class="panel-heading">
        <h1>ALA DOI Web Service v1</h1>
    </div>

    <div class="panel-body">
        <h2>Mint a new DOI</h2>

        <h3>POST to /api/doi <small>Accept-Version: 1.0</small></h3>

        <p>
            Mint a new DOI. Must have an ALA API Key.

            This endpoint accepts 2 formats:
        </p>
        <ol>
            <li>A Multipart Request, where the metadata is in a parameter called 'json' and the file associated with the DOI is provided in the request; or
            <li>A standard post with a JSON body, with a mandatory 'fileUrl' property containing a URL where the file for the DOI can be downloaded from.
        </ol>

        <p>
            The request must have JSON object with the following structure:
        </p>
        <pre>
            {
            provider: "ANDS", // the doi provider to use (see {@link DoiProvider} for a list of supported providers)
            applicationUrl: "http://....", // the url to the relevant page on the source application. This is NOT the landing page: it is used to provide a link ON the landing page back to the original source of the publication/data/etc for the DOI.
            providerMetadata: { // the provider-specific metadata to be sent with the DOI minting request
            ...
            },
            title: "...", // title to be displayed on the landing page
            authors: "...", // author(s) to be displayed on the landing page
            description: "...", // description to be displayed on the landing page


            // the following are optional
            fileUrl: "http://....", // the url to use to download the file for the DOI (use this, or send the file as a multipart request)
            customLandingPageUrl: "http://...", // an application-specific landing page that you want the DOI to resolve to. If not provided, the default ALA-DOI landing page will be used.
            applicationMetadata: { // any application-specific metadata you want to display on the landing page in ALA-DOI
            ...
            }
            }
        </pre>

        <p>
            If "fileUrl" is not provided, then you must send the file in a multipart request with the metadata as a JSON string in a form part called 'json'.

            @return JSON response containing the DOI and the landing page on success, HTTP 500 on failure
        </p>

        <h3 class="padding-top-1">ANDS Metadata</h3>

        ANDS metadata can be provided as a 'flattened' JSON structure in the 'providerMetadata' block, where each ANDS term is a top-level element. E.g.

        <pre>
            {
            "authors": [
            "Fred Smith",
            "Jill Jones"
            ],
            "title": "acacia blablabla",
            "subtitle": "Version 2",
            "publisher": "Flora of Australia",
            "publicationYear": 2016,
            "subjects": [
            "acacia blablabla"
            ],
            "contributors": [
            {
            "type": "Editor",
            "name": "Fred Smith"
            }
            ],
            "resourceType": "Text",
            "resourceText": "Species information",
            "descriptions": [
            {
            "type": "Other",
            "text": "Taxonomic treatment for acacia blablabla"
            }
            ],
            "createdDate": "2016-03-30T04:20:23Z"
            }
        </pre>

        <h2 class="padding-top-1">Get DOI Metadata</h2>

        <h3>GET /api/doi/$id <small>Accept-Version: 1.0</small></h3>

        <p>
            Retrieves the DOI metadata as JSON. $id can be either the local UUID or the DOI (e.g. 10.5072/63/56FB3479E2515).
        </p>

        <h2 class="padding-top-1">Download DOI File</h2>

        <h3>GET /api/doi/$id/download <small>Accept-Version: 1.0</small></h3>

        <p>
            Retrieves the DOI metadata as JSON. $id can be either the local UUID or the DOI (e.g. 10.5072/63/56FB3479E2515).
        </p>
    </div>
</div>

</body>
</html>
