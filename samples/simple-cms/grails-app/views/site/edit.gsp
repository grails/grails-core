
<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
		 <meta name="layout" content="main" />
         <title>Edit Site</title>
         <link rel="stylesheet" href="${createLinkTo(dir:'css',file:'main.css')}"></link>
    </head>
    <body>
        <div class="body">
           <h1>Edit Site</h1>
           <g:if test="${flash['message']}">
                 <div class="message">${flash['message']}</div>
           </g:if>
           <g:hasErrors bean="${site}">
                <div class="errors">
                    <g:renderErrors bean="${site}" as="list" />
                </div>
           </g:hasErrors>
           <div class="prop">
	      <span class="name">Id:</span>
	      <span class="value">${site?.id}</span>
	      <input type="hidden" name="site.id" value="${site?.id}" />
           </div>           
           <g:form controller="site" method="post">
               <input type="hidden" name="id" value="${site?.id}" />
               <div class="dialog">
                <table>

                       
                      <tr class='prop'>
                      	<td valign='top' style='text-align:left;' width='20%'><label for='name'>Name:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:site,field:'name','errors')}'>
                      		<input type="text" name='name' value="${site?.name}"></input>
                      	</td></tr>
           
                      <tr class='prop'>
                      	<td valign='top' style='text-align:left;' width='20%'>
                      		<label for='domain'>Domain:</label></td>
                      	<td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:site,field:'domain','errors')}'>
                      		<input type="text" name='domain' value="${site?.domain}"></input></td></tr>
                       
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
            