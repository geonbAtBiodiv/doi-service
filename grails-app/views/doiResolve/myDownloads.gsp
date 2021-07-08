<%@ page contentType="text/html;charset=UTF-8" %>
<g:set var="orgNameShort" value="${grailsApplication.config.skin.orgNameShort}"/>
<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="${grailsApplication.config.skin.layout}"/>
    <meta name="fluidLayout" content="false"/>
    <title><g:message code="download.page.title" args="[orgNameShort]"/></title>

    <asset:javascript src="downloads.js" />
    <asset:stylesheet src="downloads.css" />
</head>

<body>
<div class="container content">
    <div class="row">
        <h3 class="col-md-12"><g:message code="download.mydownloads.pageTitle"/></h3>
    </div>
    <g:if test="${totalRecords <= 0}">
        <g:message code="download.mydownloads.noRecords"/>
    </g:if>
    <g:else>
        <div class="row" id="table-metadata">
            <g:set var="offset" value="${params.getInt('offset')?:0}"/>
            <g:set var="max" value="${params.getInt('max')?:10}"/>
            <g:set var="start" value="${((params.getInt('offset')?:0) + 1)}"/>
            <g:set var="start" value="${((params.getInt('offset')?:0) + 1)}"/>
            <g:set var="end" value="${(start + ((params.getInt('max')?:10) - 1))}"/>
            <g:set var="end" value="${(end > totalRecords ? totalRecords : end)}"/>
            <div class="col-md-5" id="pagination-details">
                <g:message code="downloads.fields.showing.results" args="[start, end, totalRecords]" />
                <g:if test="${params.filter}">
                    (filter: ${params.filter})
                </g:if>
            </div>
            <div class="col-md-7" id="sort-widgets">
                <g:message code="download.mydownloads.items.per.page" default="items per page"/>:
                <select id="per-page" name="per-page" class="input-small" onchange="location = this.value;">
                    <option value="${g.createLink(action:'myDownloads',params:params + [max:'10'])}" ${params.max == '10' ? 'selected': ''}>10</option>
                    <option value="${g.createLink(action:'myDownloads',params:params + [max:'20'])}" ${params.max == '20' ? 'selected': ''}>20</option>
                    <option value="${g.createLink(action:'myDownloads',params:params + [max:'50'])}" ${params.max == '50' ? 'selected': ''}>50</option>
                    <option value="${g.createLink(action:'myDownloads',params:params + [max:'100'])}" ${params.max == '100' ? 'selected': ''}>100</option>
                </select>&nbsp;

                <g:message code="download.mydownloads.sort" default="sort"/>:
                <select id="sort" name="sort" class="input-small" onchange="location = this.value;">
                    <option value="${g.createLink(action:'myDownloads',params:params + [sort:'dateCreated'])}" ${params.sort == 'dateCreated' ? 'selected': ''}><g:message code="download.mydownloads.date" default="Date"/></option>
                    <option value="${g.createLink(action:'myDownloads',params:params + [sort:'doi'])}" ${params.sort == 'doi' ? 'selected': ''}><g:message code="download.mydownloads.doi" default="DOI"/></option>
                    <option value="${g.createLink(action:'myDownloads',params:params + [sort:'filename'])}" ${params.sort == 'filename' ? 'selected': ''}><g:message code="download.mydownloads.filename" default="Filename"/></option>
                </select>&nbsp;

                <g:message code="download.mydownloads.order" default="order"/>:
                <select id="dir" name="dir" class="input-small" onchange="location = this.value;">
                    <option value="${g.createLink(action:'myDownloads',params:params + [order:'asc'])}" ${params.order == 'asc' ? 'selected': ''}><g:message code="downloads.fields.ascending" default="Ascending"/></option>
                    <option value="${g.createLink(action:'myDownloads',params:params + [order:'desc'])}" ${params.order == 'desc' ? 'selected': ''}><g:message code="downloads.fields.descending" default="Descending"/></option>
                </select>
            </div>
        </div>

        <div class="row">
            <g:if test="${totalRecords > max}">
                <div class="row">
                    <nav class="col-sm-12 col-centered text-center">
                        <div class="pagination pagination-lg">
                            <hf:paginate total="${totalRecords}"  action="myDownloads"
                                         omitLast="false" omitFirst="false" prev="&laquo;" next="&raquo;"
                                         max="${max}" offset="${offset}" params="${[filter:params.filter]}"/>
                        </div>
                    </nav>
                </div>
            </g:if>
            <div class="row">
                <div class="col-md-12 fwtable table-responsive">
                    <table class="table table-bordered table-striped ">
                        <thead>
                            <tr>
                                <th class="col-sm-2"><g:message code="myDownloads.header.download" /></th>
                                <th class="col-sm-2"><g:message code="myDownloads.header.date" /></th>
                                <th class="col-sm-1"><g:message code="myDownloads.header.records" /></th>
                                <th class="col-sm-1"><g:message code="myDownloads.header.datasets" /></th>
                                <th class="col-sm-6"><g:message code="myDownloads.header.search.query" /></th>
                            </tr>
                        </thead>
                        <tbody>
                            <g:each in="${dois}" var="doi">
                               <tr>
                                   <td>
                                       <a href="${g.createLink(uri: '/doi')}/${doi.doi}" type="button" class="doi doi-sm"><span>DOI</span><span>${doi.doi}</span></a>
                                       <br>
                                       <g:message code="myDownloads.field.file" /> <a href="${request.contextPath}/doi/${doi.uuid}/download"> ${doi.filename}</a>
                                   </td>
                                   <td><span class="no-wrap"><g:formatDate date="${doi.dateMinted}" format="yyyy-MM-dd"/></span>
                                       <span class="no-wrap"><g:formatDate date="${doi.dateMinted}" format="h:mm a"/></span></td>
                                   %{--<td>${doi.title}</td>--}%
                                   <td><g:formatNumber number="${doi.applicationMetadata?.recordCount}" type="number" /></td>
                                   <td><g:formatNumber number="${doi.applicationMetadata?.datasets?.size()}" type="number" /></td>
                                   <td><a href="${doi?.applicationMetadata?.searchUrl}"><g:message code="myDownloads.re.run.search" /></a> <doi:formatSearchQuery searchUrl="${doi?.applicationMetadata?.searchUrl}" queryTitle="${doi.applicationMetadata?.queryTitle?.encodeAsRaw()}"/></td>
                               </tr>
                            </g:each>
                        </tbody>
                    </table>
                </div>
            </div>
            <g:if test="${totalRecords > max}">
                <div class="row">
                    <nav class="col-sm-12 col-centered text-center">
                        <div class="pagination pagination-lg">
                            <hf:paginate total="${totalRecords}"  action="myDownloads"
                                         omitLast="false" omitFirst="false" prev="&laquo;" next="&raquo;"
                                         max="${max}" offset="${offset}" params="${[filter:params.filter]}"/>
                        </div>
                    </nav>
                </div>
            </g:if>
        </div>
    </g:else>
</div>
 %{--   </div>
</div>--}%
</body>
%{--        <g:set var="showLongTimeWarning" value="${totalRecords && (totalRecords > grailsApplication.config.downloads.maxRecords)}"/>--}%
