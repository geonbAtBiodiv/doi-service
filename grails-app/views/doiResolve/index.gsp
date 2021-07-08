<g:set var="orgNameShort" value="${grailsApplication.config.skin.orgNameShort}"/>
<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="${grailsApplication.config.skin.layout}"/>
    <title><g:message code="doi.homepage.title" args="[orgNameShort]" /></title>

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
    <div class="row">
        <div class="col-xs-12 col-sm-12 col-md-12 col-lg-12">
            <div class="panel panel-default">
                <div class="panel-body">

                    <div class="word-limit">
                        <h1 class="heading-xlarge"><g:message code="index.doi.title" args="[orgNameShort]"/></h1>

                        <p class="lead">
                            <g:message code="index.doi.description" />
                        </p>
                    </div>
                </div>
            </div>
            <g:if test="${dois.totalCount > pageSize}">

                <div class="row">
                    <nav class="col-sm-12 col-centered text-center">
                        <div class="pagination pagination-lg">
                            <hf:paginate total="${dois.totalCount}" controller="doiResolve" action="index"
                                         omitLast="false" omitFirst="false" prev="&laquo;" next="&raquo;"
                                         max="${pageSize}" offset="${offset}"/>
                        </div>
                    </nav>
                </div>
            </g:if>
            <div class="panel panel-default">
                <div class="panel-body">

                    <div class="word-limit">
                        <g:if test="${false}"><!-- filter to be implemented at a later date -->
                            <div class="well">
                                <form class="form-horizontal">
                                    <div class="form-group">
                                        <label for="filter" class="col-sm-2 control-label"><g:message code="index.search.filter" /></label>

                                        <div class="col-sm-10">
                                            <div class="input-group">
                                                <input id="filter" type="text" class="form-control"
                                                       placeholder="${message(code:"index.refine.placeholder")}">
                                                <span class="input-group-btn">
                                                    <a class="btn btn-default" type="button"><g:message code="index.filter.btn" /></a>
                                                </span>
                                            </div>

                                            <p class="help-block"><g:message code="index.filter.description" /></p>
                                        </div>
                                    </div>
                                </form>
                            </div>
                        </g:if>

                        <div class="row">
                            <div class="col-md-10">

                                <ol class="list-unstyled break-word">
                                    <g:each in="${dois}" var="doi">
                                        <li>

                                                <h4 class="search-result "><a class="${doi.active? '': 'text-muted'}"
                                                        href="${request.contextPath}/doi/${doi.uuid}">${doi.title}</a>
                                                    <g:if test="${!doi.active}">
                                                        <small class="badge badge-secondary"><g:message code="doi.inactive"/></small>
                                                    </g:if>
                                                </h4>

                                                <div class="padding-bottom-10"><a href="https://doi.org/${doi.doi}" type="button" class="doi doi-sm"><span><g:message code="index.item.doi" /></span><span>${doi.doi}</span></a></div>
                                                <div class="padding-bottom-10"><strong><g:message code="index.item.created" /></strong> ${doi.dateMinted}</div>
                                                <div class="padding-bottom-10"><strong><g:message code="index.item.author" /></strong> ${doi.authors}</div>
                                                <div class="padding-bottom-20">
                                                    ${doi.description}
                                                </div>
                                        </li>
                                    </g:each>
                                </ol>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <g:if test="${dois.totalCount > pageSize}">
                <div class="small pull-right"><g:message code="index.pagination.description" args="[offset + 1, Math.min(dois.totalCount, offset + pageSize), dois.totalCount]" /></div>
                <div class="clear"></div>

                <div class="row">
                    <nav class="col-sm-12 col-centered text-center">
                        <div class="pagination pagination-lg">
                            <hf:paginate total="${dois.totalCount}" controller="doiResolve" action="index"
                                        omitLast="false" omitFirst="false" prev="&laquo;" next="&raquo;"
                                        max="${pageSize}" offset="${offset}"/>
                        </div>
                    </nav>
                </div>
            </g:if>
        </div>
        <div style="color:white;" class="pull-right">
            <g:if test="${isAdmin}">
                <g:link controller="admin"><g:message code="index.admin.tools.btn" /></g:link>
            </g:if>
        </div>
    </div>
</div>
</body>
</html>
