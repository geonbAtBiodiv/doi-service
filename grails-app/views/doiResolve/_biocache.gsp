<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="${grailsApplication.config.skin.layout}"/>
    <meta name="fluidLayout" content="false"/>
    <asset:javascript src="downloads.js" />
    <asset:stylesheet src="downloads.css" />
</head>
<body>
<div class="container">
<div class="row">
    <div class="col-md-12" id="doiTitle">
        <h2><a href="https://doi.org/${doi.doi}" type="button" class="doi"><span><g:message code="biocache.doi.title" /></span><span>${doi.doi}</span></a></h2>
        <h3><g:message code="doi.page.download.subtitle" args="[g.formatDate(date:doi.dateCreated, format:message(code:'doi.page.date.format'))]"/></h3>
    </div>
    <div class="col-md-12 text-right">
        <a href="${request.contextPath}/doi/${doi.uuid}/download" class="btn btn-primary"><i class="glyphicon glyphicon-download-alt"></i>&nbsp; <g:message code="doi.page.download.link" /></a>
    </div>
    <div class="col-md-12"><b><g:message code="doi.page.file" /></b> <a href="${request.contextPath}/doi/${doi.uuid}/download"> ${doi.filename?:message(code:"doi.page.download.file.not.found")}</a></div><br>
    <div class="col-md-12"><g:message code="doi.page.record.count" args="[g.formatNumber(number:doi.applicationMetadata?.recordCount, type:'number')]"/></div>
    <div class="col-md-8 col-sm-12"><b><g:message code="doi.page.search.query" /></b> <doi:formatSearchQuery searchUrl="${doi.applicationMetadata?.searchUrl}" /> </div>
    <div class="col-md-12"><b><g:message code="doi.page.search.url" /></b> <a href="${doi.applicationMetadata?.searchUrl}"><doi:sanitiseRawContent content="${doi.applicationMetadata?.queryTitle?.encodeAsRaw()}" /></a></div>
    <g:set var="enabled" value="${doi.applicationMetadata?.qualityFilters != null && doi.applicationMetadata?.qualityFilters.size() > 0}"/>
    <div class="col-md-12">
        <g:if test="${enabled}">
            <b>Data profile:</b>&nbsp;${doi.applicationMetadata?.dataProfile}
            <table class="table table-condensed table-striped">
                <thead>
                <tr>
                    <th><g:message code="doi.page.header.name" /></th>
                    <th><g:message code="doi.page.header.filter" /></th>
                </tr>
                </thead>
                <tbody>
                <g:each in="${doi.applicationMetadata?.qualityFilters}" var="qf">
                    <tr>
                        <td>${qf.name}</td>
                        <td>${qf.filter}</td>
                    </tr>
                </g:each>
                </tbody>
            </table>
        </g:if>
        <g:else>
            <b><g:message code="doi.page.data.profile.disabled" /></b>
        </g:else>
    </div>
    <div class="col-md-12"><b><g:message code="doi.page.licence" /></b>
        <g:if test="${doi.licence}">
            <ul>
                <g:each in="${doi.licence}" var="licence" >
                    <li>${licence}</li>
                </g:each>
            </ul>
        </g:if>
    </div>
    <div class="col-md-12"><b><g:message code="doi.page.authors" /></b> ${doi.authors}</div>
    <div class="col-md-12"><b><g:message code="doi.page.date.created" /></b> <g:formatDate date="${doi.dateCreated}" format="yyyy-MM-dd h:mm a"/></div>
    <div class="col-md-12"><b><g:message code="doi.page.citation.url" /></b> <a href="${grailsApplication.config.doi.resolverUrl}${doi.doi}">${grailsApplication.config.doi.resolverUrl}${doi.doi}</a></div><br>

</div>
    <div class="row">
        <div class="fwtable table-responsive col-md-12">
            <p><b><g:message code="doi.page.datasets" /> (<g:formatNumber number="${doi.applicationMetadata?.datasets?.size()}" type="number" />)</b></p>
            <table class="table table-bordered table-striped ">
                <thead>
                <tr>
                    <th><g:message code="doi.page.header.name" /></th>
                    <th><g:message code="doi.page.header.licence" /></th>
                    <th style="text-align: center"><g:message code="doi.page.header.record.count" /></th>
                </tr>
                </thead>
                <tbody>
                <g:each in="${doi.applicationMetadata?.datasets?.sort{a,b -> b.count as Integer <=> a.count as Integer}}" var="dataset">
                    <tr>
                        <td class="col-xs-4"><a href="${grailsApplication?.config.collections.baseUrl}/public/show/${dataset.uid}">${dataset.name}</a></td>
                        <td class="col-xs-3">${dataset.licence}</td>
                        <td class="col-xs-1" align="center"><g:formatNumber number="${dataset.count}" type="number" /></td>
                    </tr>
                </g:each>
                </tbody>
            </table>
        </div>

    </div>
</div>
</body>
</html>
