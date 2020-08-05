<%@ page import="com.jakewharton.byteunits.BinaryByteUnit; com.google.common.io.BaseEncoding" %>
<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="${grailsApplication.config.skin.layout}"/>
    <meta name="breadcrumbParent" content="${grailsApplication.config.grails.serverURL},${message(code:"doi.homepage.title")}"/>
    <title>DOI Details</title>

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
<div>

    <g:render template="${displayTemplate}" />

</div>
</body>
</html>
