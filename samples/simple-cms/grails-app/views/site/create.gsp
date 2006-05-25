
<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
		 <meta name="layout" content="main" />
         <title>Create Site</title>
         <link rel="stylesheet" href="${createLinkTo(dir:'css',file:'main.css')}"></link>
    </head>
    <body>
        <div class="body">
           <h1>Create Site</h1>
           <g:if test="${flash['message']}">
                 <div class="message">${flash['message']}</div>
           </g:if>
           <g:hasErrors bean="${site}">
                <div class="errors">
                    <g:renderErrors bean="${site}" as="list" />
                </div>
           </g:hasErrors>
           <g:form action="save" method="post">
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
                     <span class="formButton">
                        <input type="submit" value="Create"></input>
                     </span>
               </div>
            </g:form>
        </div>
    </body>
</html>
            