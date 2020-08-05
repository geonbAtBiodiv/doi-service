<%--
  Created by IntelliJ IDEA.
  User: mol109
  Date: 13/04/2016
  Time: 6:04 PM
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<!doctype html>
<html>
<head>
    <meta name="layout" content="${grailsApplication.config.skin.layout}"/>
    <meta name="breadcrumbParent" content="${grailsApplication.config.grails.serverURL},${message(code:"doi.homepage.title")}"/>
    %{--<meta name="section" content="home"/>--}%
    <title>DOI Service Administration</title>
    <asset:stylesheet src="doi.css"/>
</head>
<body>
<div class="col-sm-12">
    <h2 class="heading-medium">DOI Service Administration</h2>


    <div class="panel panel-default well" id="page-body" role="main">
        <ul>
            <li><g:link controller="admin" action="mintDoi">Mint/Register a DOI</g:link></li>
%{--            The ALA admin plugin was commented out in the config so this doesn't work --}%
%{--            <li><g:link controller="alaAdmin" action="index">ALA Admin</g:link></li>--}%
            <li><g:link controller="admin" action="indexAll">Index all DOIs</g:link></li>
            <li><g:link controller="apiDoc" action="getDocuments">API Documentation</g:link></li>
        </ul>
        <p>
            <span class="label label-default">Using ${storageType}</span>
        </p>
    </div>
    <div class="panel panel-default">
        <div class="panel-heading"><h2 class="panel-title">DOIs</h2></div>
        <table class="table">
            <thead>
                <tr>
                    <th>API (DOI) <small class="badge label-success">JSON</small> </th>
                    <th>API (UUID) <small class="badge label-success">JSON</small></th>
                    <th>Resolve</th>
                    <th>Download</th>
                </tr>
            </thead>
            <tbody>
                <g:each var="doi" in="${doiInstanceList}">
                    <tr>
                        <td>
                            <g:link namespace="v1" method="GET" controller="doi" action="show" id="${doi.doi}">${doi.doi}</g:link>
                        </td>
                        <td>
                            <g:link namespace="v1" method="GET" controller="doi" action="show" id="${doi.uuid}">${doi.uuid}</g:link>
                        </td>
                        <td>
                            <g:link absolute="true" method="GET" controller="doiResolve" action="doi" id="${doi.uuid}">${doi.title}</g:link>
                        </td>
                        <td>
                            <g:link method="GET" controller="doiResolve" action="download" id="${doi.uuid}">${createLink(absolute: true, method: 'GET', controller: 'doiResolve', action: 'download', id: doi.uuid)}</g:link>
                        </td>
                    </tr>
                </g:each>
            </tbody>
        </table>
    </div>
    <g:if test="${doiInstanceList.totalCount > listParams.max}">
        <div class="small pull-right">Showing ${listParams.offset + 1} to ${Math.min(doiInstanceList.totalCount, listParams.offset + listParams.max)} of ${doiInstanceList.totalCount}</div>

        <div class="clear"></div>

        <div class="row">
            <nav class="col-sm-12 col-centered text-center">
                <div class="pagination pagination-lg">
                    <hf:paginate total="${doiInstanceList.totalCount}" controller="admin" action="index"
                                 omitLast="false" omitFirst="false" prev="&laquo;" next="&raquo;"
                                 max="${listParams.max}" offset="${listParams.offset}"/>
                </div>
            </nav>
        </div>
    </g:if>
    %{--<hf:paginate total=""></hf:paginate>--}%
</div>
</body>
</html>
