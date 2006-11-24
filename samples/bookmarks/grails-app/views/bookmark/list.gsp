
<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
         <meta name="layout" content="main"  />
         <title>Bookmark List</title>
    </head>
    <body>
        <div class="nav">
            <span class="menuButton"><a href="${createLinkTo(dir:'')}">Home</a></span>
            <span class="menuButton"><g:link action="create">New Bookmark</g:link></span>
            <span class="menuButton"><g:link controller="subscription" action="list">Subscriptions</g:link></span>	
        </div>
        <div class="body">
           <h1 id="title">Bookmark List</h1>
            <g:if test="${flash.message}">
                 <div class="message">
                       ${flash.message}
                 </div>
            </g:if>
			
			<g:render template="bookmark" var="bookmark" collection="${bookmarkList}" />
		
			<g:if test="${deliciousList}"> 
				<h2>Latest from <a href="http://del.icio.us/${session.user.login}" target="_blank">del.icio.us</a></h2>
				<g:set var="edit" value="${false}" />
				<g:render template="bookmark" var="bookmark" collection="${deliciousList}" />				
			</g:if>
			<g:if test="${deliciousResults}">
				<h2>Results from <a href="http://del.icio.us/${session.user.login}" target="_blank">del.icio.us</a></h2>
				<g:render template="bookmark" var="bookmark" collection="${deliciousResults}" />
			</g:if>
        </div>
    </body>
</html>
            