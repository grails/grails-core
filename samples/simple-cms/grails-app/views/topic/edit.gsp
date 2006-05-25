
<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
		 <g:render template="/pagemeta" model="[page:forum,levels:levels]" />
    </head>
    <body>
        <div class="body">
           <h1>Edit Topic</h1>
           <g:if test="${flash['message']}">
                 <div class="message">${flash['message']}</div>
           </g:if>
           <g:hasErrors bean="${topic}">
                <div class="errors">
                    <g:renderErrors bean="${topic}" as="list" />
                </div>
           </g:hasErrors>
           <div class="prop">
	      <span class="name">Id:</span>
	      <span class="value">${topic?.id}</span>
	      <input type="hidden" name="topic.id" value="${topic?.id}" />
           </div>           
           <g:form controller="topic" method="post">
               <input type="hidden" name="id" value="${topic?.id}" />
               <div class="dialog">
                <table>

				<tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='title'>Title:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:topic,field:'title','errors')}'><input type='text' name='title' value='${topic?.title}' /></td></tr>
				
				<tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='description'>Description:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:topic,field:'description','errors')}'><textarea name="description" columns="1" rows="1">${topic?.description}</textarea></td></tr>
                       
				<input type="hidden" name="forum.id" value="${topic?.forum?.id}" />
                       				
                       
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
            