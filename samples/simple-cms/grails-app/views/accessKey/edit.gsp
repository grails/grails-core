
<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
		 <meta name="layout" content="main" />
         <title>Edit AccessKey</title>
         <link rel="stylesheet" href="${createLinkTo(dir:'css',file:'main.css')}"></link>
    </head>
    <body>
        <div class="body">
           <h1>Edit AccessKey</h1>
           <g:if test="${flash['message']}">
                 <div class="message">${flash['message']}</div>
           </g:if>
           <g:hasErrors bean="${accessKey}">
                <div class="errors">
                    <g:renderErrors bean="${accessKey}" as="list" />
                </div>
           </g:hasErrors>
           <div class="prop">
	      <span class="name">Id:</span>
	      <span class="value">${accessKey?.id}</span>
	      <input type="hidden" name="accessKey.id" value="${accessKey?.id}" />
           </div>           
           <g:form controller="accessKey" method="post">
               <input type="hidden" name="id" value="${accessKey?.id}" />
               <div class="dialog">
                <table>

                       
                       
				<tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='startDate'>Start Date:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:accessKey,field:'startDate','errors')}'><g:datePicker name='startDate' value='${accessKey?.startDate}'></g:datePicker></td></tr>
                       
				<tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='endDate'>End Date:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:accessKey,field:'endDate','errors')}'><g:datePicker name='endDate' value='${accessKey?.endDate}'></g:datePicker></td></tr>
                       
				<tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='expiryDate'>Expiry Date:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:accessKey,field:'expiryDate','errors')}'><g:datePicker name='expiryDate' value='${accessKey?.expiryDate}'></g:datePicker></td></tr>
                       
				<tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='usages'>Usages:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:accessKey,field:'usages','errors')}'><input type='text' name='usages' value='${accessKey?.usages}'></input></td></tr>
                       
				<tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='code'>Code:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:accessKey,field:'code','errors')}'><input type="text" maxlength='16' name='code' value='${accessKey?.code}'></input></td></tr>
                       
				<tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='role'>Role:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:accessKey,field:'role','errors')}'><g:select optionKey="id" from="${Role.list()}" name='role.id' value='${accessKey?.role?.id}'></g:select></td></tr>
                       
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
            