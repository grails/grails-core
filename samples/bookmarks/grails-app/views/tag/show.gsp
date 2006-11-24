
<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
          <meta name="layout" content="main" />
         <title>Show Tag</title>
    </head>
    <body>
        <div class="nav">
            <span class="menuButton"><a href="${createLinkTo(dir:'')}">Home</a></span>
            <span class="menuButton"><g:link action="list">Tag List</g:link></span>
            <span class="menuButton"><g:link action="create">New Tag</g:link></span>
        </div>
        <div class="body">
           <h1>Show Tag</h1>
           <g:if test="${flash.message}">
                 <div class="message">${flash.message}</div>
           </g:if>
           <div class="dialog">
                 <table>
                   
                   
                        <tr class="prop">
                              <td valign="top" class="name">Id:</td>
                              
                                    <td valign="top" class="value">${tag.id}</td>
                              
                        </tr>
                   
                        <tr class="prop">
                              <td valign="top" class="name">Bookmark:</td>
                              
                                    <td valign="top" class="value"><g:link controller="bookmark" action="show" id="${tag?.bookmark?.id}">${tag?.bookmark}</g:link></td>
                              
                        </tr>
                   
                        <tr class="prop">
                              <td valign="top" class="name">Name:</td>
                              
                                    <td valign="top" class="value">${tag.name}</td>
                              
                        </tr>
                   
                 </table>
           </div>
           <div class="buttons">
               <g:form controller="tag">
                 <input type="hidden" name="id" value="${tag?.id}" />
                 <span class="button"><g:actionSubmit value="Edit" /></span>
                 <span class="button"><g:actionSubmit value="Delete" /></span>
               </g:form>
           </div>
        </div>
    </body>
</html>
            