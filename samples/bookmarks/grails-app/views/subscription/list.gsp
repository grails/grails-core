<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
         <meta name="layout" content="main" />
         <title>Subscription List</title>
    </head>
    <body>
        <div class="nav">
            <span class="menuButton"><a href="${createLinkTo(dir:'')}">Home</a></span>
            <span class="menuButton"><g:link controller="bookmark" action="list">View Bookmarks</g:link></span>
        </div>
        <div class="body">
           <h1 id="title">Subscription List</h1>
            <g:if test="${flash.message}">
                 <div class="message">
                       ${flash.message}
                 </div>
            </g:if>
			<p>Below is a list of tags you are subscribed to. You will receive weekly notifications of 
				new bookmarks that have been added by other users that have been tagged with the below:</p>
			<ul id="subscriptions">
				<g:render template="subscription" var="subscription" collection="${subscriptionList}" />
			</ul>
			<p>
				<g:formRemote name="addTag" url="[action:'save']" update="subscriptions">
					Add Tag: <input type="text" name="tagName" /> <input type="submit" value="Add" />
				</g:formRemote>
			</p>
        </div>
    </body>
</html>
            