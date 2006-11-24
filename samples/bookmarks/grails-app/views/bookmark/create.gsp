
<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
         <meta name="layout" content="main" />
         <title>Create Bookmark</title> 
         <g:javascript library="prototype" />
    </head>
    <body>
        <div class="nav">
            <span class="menuButton"><a href="${createLinkTo(dir:'')}">Home</a></span>
            <span class="menuButton"><g:link action="list">Bookmark List</g:link></span>
        </div>
        <div class="body">
           <h1>Create Bookmark</h1>
           <g:if test="${flash.message}">
                 <div class="message">${flash.message}</div>
           </g:if>
           <g:hasErrors bean="${bookmark}">
                <div class="errors">
                    <g:renderErrors bean="${bookmark}" as="list" />
                </div>
           </g:hasErrors>
           <g:form action="save" method="post" >
               <div class="dialog">
					<g:render template="editor" model="[bookmark:bookmark]" />					
               </div>
               <div class="buttons">
                     <span class="formButton">
                        <input type="submit" value="Create"></input>
                     </span>
               </div>
            </g:form>
        </div>
    </body>
</html>
            