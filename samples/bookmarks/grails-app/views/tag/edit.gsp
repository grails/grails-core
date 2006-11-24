
<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
         <meta name="layout" content="main" />
         <title>Edit Tag</title>
    </head>
    <body>
        <div class="nav">
            <span class="menuButton"><a href="${createLinkTo(dir:'')}">Home</a></span>
            <span class="menuButton"><g:link action="list">Tag List</g:link></span>
            <span class="menuButton"><g:link action="create">New Tag</g:link></span>
        </div>
        <div class="body">
           <h1>Edit Tag</h1>
           <g:if test="${flash.message}">
                 <div class="message">${flash.message}</div>
           </g:if>
           <g:hasErrors bean="${tag}">
                <div class="errors">
                    <g:renderErrors bean="${tag}" as="list" />
                </div>
           </g:hasErrors>
           <div class="prop">
	      <span class="name">Id:</span>
	      <span class="value">${tag?.id}</span>
	      <input type="hidden" name="tag.id" value="${tag?.id}" />
           </div>           
           <g:form controller="tag" method="post" >
               <input type="hidden" name="id" value="${tag?.id}" />
               <div class="dialog">
                <table>

                       
                       
				<tr class='prop'><td valign='top' class='name'><label for='bookmark'>Bookmark:</label></td><td valign='top' class='value ${hasErrors(bean:tag,field:'bookmark','errors')}'><g:select optionKey="id" from="${Bookmark.list()}" name='bookmark.id' value='${tag?.bookmark?.id}'></g:select></td></tr>
                       
				<tr class='prop'><td valign='top' class='name'><label for='name'>Name:</label></td><td valign='top' class='value ${hasErrors(bean:tag,field:'name','errors')}'><input type='text' name='name' value='${tag?.name}' /></td></tr>
                       
                </table>
               </div>

               <div class="buttons">
                     <span class="button"><g:actionSubmit value="Update" /></span>
                     <span class="button"><g:actionSubmit value="Delete" /></span>
               </div>
            </g:form>
        </div>
    </body>
</html>
            