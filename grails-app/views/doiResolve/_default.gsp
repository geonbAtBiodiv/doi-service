<%@ page import="com.jakewharton.byteunits.BinaryByteUnit; com.google.common.io.BaseEncoding" %>
<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="${grailsApplication.config.skin.layout}"/>

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

<div class="col-sm-12 col-md-9 col-lg-9">
    <h1 class="heading-medium ${doi.active? '': 'text-muted'}">${doi.title}
        <g:if test="${!doi.active}">
            <small class="badge badge-secondary"><g:message code="doi.inactive"/></small>
        </g:if>
    </h1>


    <div class="row">
        <div class="col-xs-12 col-sm-12 col-md-12 col-lg-12">
            <div class="panel panel-default">
                <div class="panel-body">
                    <div class="word-limit break-word">
                        <h2 class="heading-medium padding-bottom-10">${doi.authors}</h2>
                        <div class="row">
                            <div class="col-xs-12 col-sm-12 col-md-12 col-lg-12">
                                <div class="well">
                                    <p><g:message code="default.view.to.access.this.resource.you.can" /></p>
                                    <p>
                                        <a class="btn btn-default" href="${doi.applicationUrl}"
                                           title="Go to source"><g:message code="default.view.go.to.the.source" /></a>
                                        <a class="btn btn-primary"
                                           href="${request.contextPath}/doi/${doi.uuid}/download"
                                           title="Download file">Download file</a>
                                    </p>
                                </div>
                            </div>
                        </div>
                        <div class="row">
                            <div class="col-xs-12 col-sm-12 col-md-12 col-lg-12">

                                <div class="padding-bottom-10">${doi.description}</div>

                                <div class="padding-bottom-10"><a href="https://doi.org/${doi.doi}" type="button" class="doi doi-sm"><span>DOI</span><span>${doi.doi}</a></div>

                                <div class="row padding-bottom-10">
                                    <div class="col-md-3">
                                        <strong><g:message code="default.view.created" /></strong>
                                    </div>
                                    <div class="col-md-9">
                                        ${doi.dateMinted}
                                    </div>
                                </div>
                                <div class="row padding-bottom-10">
                                    <div class="col-md-3">
                                        <strong><g:message code="default.view.licence" /></strong>
                                    </div>
                                    <div class="col-md-9">
                                        <g:if test="${doi.licence}">
                                            <ul>
                                            <g:each in="${doi.licence}" var="licence" >
                                                <li>${licence}</li>
                                            </g:each>
                                            </ul>
                                        </g:if>
                                    </div>
                                </div>
                                <g:if test="${doi.customLandingPageUrl}">
                                    <div class="row padding-bottom-10">
                                        <div class="col-md-3">
                                            <strong><g:message code="default.view.landing.page" /></strong>
                                        </div>
                                        <div class="col-md-9">
                                            <g:message code="default.view.registered.with.application" /> <a href="${doi.customLandingPageUrl}"><g:message code="default.view.application.landing.page" /></a>
                                        </div>
                                    </div>
                                </g:if>
                                <g:if test="${doi.fileHash}">
                                    <div class="row padding-bottom-10">
                                        <div class="col-md-3">
                                            <strong><g:message code="default.view.file.sha.256" /></strong>
                                        </div>
                                        <div class="col-md-9">
                                            ${BaseEncoding.base16().encode(doi.fileHash)}
                                        </div>
                                    </div>
                                </g:if>
                                <g:if test="${doi.fileSize}">
                                    <div class="row padding-bottom-10">
                                        <div class="col-md-3">
                                            <strong><g:message code="default.view.file.size" /></strong>
                                        </div>
                                        <div class="col-md-9">
                                            ${BinaryByteUnit.format(doi.fileSize)}
                                        </div>
                                    </div>
                                </g:if>

                                <g:render template="metadata" model="[metadata: doi.applicationMetadata]"/>

                                <g:if test="${isAdmin}">
                                    <div class=" padding-top-10">
                                        <h3><g:message code="default.view.admin.only.fields" /></h3>
                                    </div>
                                    <div class="row padding-bottom-10">
                                        <div class="col-md-3">
                                            <strong><g:message code="default.view.user.id" /></strong>
                                        </div>
                                        <div class="col-md-9">
                                            ${doi.userId}
                                        </div>
                                    </div>

                                    <g:if test="${doi.authorisedRoles}">
                                        <div>
                                            <g:message code="default.view.roles" />
                                        </div>
                                        <div class="row padding-bottom-10">
                                            <div class="col-md-3">
                                                <strong><g:message code="default.view.sensitive.roles" /></strong>
                                            </div>
                                            <div class="col-md-9">
                                                <ul>
                                                    <g:each var="role" in="${doi.authorisedRoles}">
                                                        <li>${role}</li>
                                                    </g:each>
                                                </ul>
                                            </div>
                                        </div>
                                    </g:if>
                                </g:if>
                            </div>
                        </div>
                        <div class="row">
                            <div class="col-xs-12 col-sm-12 col-md-12 col-lg-12">
                                <div class="alert alert-info alert-dismissible" role="alert">
                                    <button type="button" class="close" data-dismiss="alert"
                                            aria-label="Close"><span aria-hidden="true">&times;</span></button>
                                    <g:message code="default.view.support" args="[grailsApplication?.config.supportContact, message(code:'default.view.contact.title', args:[grailsApplication.config.skin.orgNameLong])]" />
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
</body>
</html>
