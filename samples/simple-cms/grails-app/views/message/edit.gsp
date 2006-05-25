
<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
		 <g:render template="/pagemeta" model="[page:forum,levels:levels]" />
		 <script type="text/javascript" src="${createLinkTo(dir:'fckeditor',file:'fckeditor.js')}"></script>
    </head>
    <body>
        <div class="body">
           <h1>Edit Message</h1>
           <g:if test="${flash['message']}">
                 <div class="message">${flash['message']}</div>
           </g:if>
           <g:hasErrors bean="${message}">
                <div class="errors">
                    <g:renderErrors bean="${message}" as="list" />
                </div>
           </g:hasErrors>
           <div class="prop">
	      <span class="name">Id:</span>
	      <span class="value">${message?.id}</span>
	      <input type="hidden" name="message.id" value="${message?.id}" />
           </div>           
           <g:form controller="message" method="post">
               <input type="hidden" name="id" value="${message?.id}" />
               <div class="dialog">
                <table>

                       
				<tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='title'>Title:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:message,field:'title','errors')}'><input type="text" maxlength='50' name='title' value='${message?.title}'></input></td></tr>
                       
				<tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='message'>Message:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:message,field:'message','errors')}'>
					<g:richTextEditor name="message" height="300" toolbar="Basic" value="${message?.message}"/>
				</td></tr>
                       
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
            