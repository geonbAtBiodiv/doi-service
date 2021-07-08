<g:set var="orgNameShort" value="${grailsApplication.config.skin.orgNameShort}"/>
<!DOCTYPE html>
<html>
<head>
	<meta name="layout" content="${grailsApplication.config.skin.layout}"/>
    <title><g:message code="error.page.title" args="[orgNameShort]"/></title>

	<!-- HTML5 shim and Respond.js for IE8 support of HTML5 elements and media queries -->
	<!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
	<!--[if lt IE 9]>
        <script src="https://oss.maxcdn.com/html5shiv/3.7.2/html5shiv.min.js"></script>
        <script src="https://oss.maxcdn.com/respond/1.4.2/respond.min.js"></script>
    <![endif]-->

	<asset:stylesheet src="doi.css"/>
</head>

<body>

<div class="col-sm-12 col-md-9 col-lg-9">

	<h2 class="heading-medium">Error</h2>

	<div class="row">
		<div class="col-sm-12">
			<div class="alert alert-danger">
				Not found
			</div>
		</div>
	</div>
</div>

</body>
</html>
