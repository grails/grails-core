
<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
         <title>Comments for ${page?.title}</title>
         <link rel="stylesheet" href="${createLinkTo(dir:'css',file:'main.css')}"></link>
    </head>
    <body>
        <div class="nav">
        </div>
        <div class="body">
           <h1>Comments for ${page?.title}</h1>
           <g:if test="${flash['message']}">
                 <div class="message">${flash['message']}</div>
           </g:if>
           <g:hasErrors bean="${page}">
                <div class="errors">
                    <g:renderErrors bean="${page}" as="list" />
                </div>
           </g:hasErrors>
		   <g:each var="c" in="${comments?}">
		   		<g:render template="/comment" model="[comment:c]" />
		   </g:each>
        </div>
    </body>
</html>
            
