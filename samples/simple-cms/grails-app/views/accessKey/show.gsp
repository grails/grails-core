
<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
		 <meta name="layout" content="main" />
         <title>Show AccessKey</title>
         <link rel="stylesheet" href="${createLinkTo(dir:'css',file:'main.css')}"></link>
    </head>
    <body>
        <div class="body">
           <h1>Show AccessKey</h1>
           <g:if test="${flash['message']}">
                 <div class="message">${flash['message']}</div>
           </g:if>
           <div class="dialog">
                 <table>
                   
                   
                        <tr class="prop">
                              <td valign="top" style="text-align:left;" width="20%" class="name">Id:</td>
                              
                                    <td valign="top" style="text-align:left;" class="value">${accessKey.id}</td>
                              
                        </tr>
                   
                        <tr class="prop">
                              <td valign="top" style="text-align:left;" width="20%" class="name">Start Date:</td>
                              
                                    <td valign="top" style="text-align:left;" class="value">${accessKey.startDate}</td>
                              
                        </tr>
                   
                        <tr class="prop">
                              <td valign="top" style="text-align:left;" width="20%" class="name">End Date:</td>
                              
                                    <td valign="top" style="text-align:left;" class="value">${accessKey.endDate}</td>
                              
                        </tr>
                   
                        <tr class="prop">
                              <td valign="top" style="text-align:left;" width="20%" class="name">Expiry Date:</td>
                              
                                    <td valign="top" style="text-align:left;" class="value">${accessKey.expiryDate}</td>
                              
                        </tr>
                   
                        <tr class="prop">
                              <td valign="top" style="text-align:left;" width="20%" class="name">Usages:</td>
                              
                                    <td valign="top" style="text-align:left;" class="value">${accessKey.usages}</td>
                              
                        </tr>
                   
                        <tr class="prop">
                              <td valign="top" style="text-align:left;" width="20%" class="name">Code:</td>
                              
                                    <td valign="top" style="text-align:left;" class="value">${accessKey.code}</td>
                              
                        </tr>
                   
                        <tr class="prop">
                              <td valign="top" style="text-align:left;" width="20%" class="name">Role:</td>
                              
                                    <td valign="top" style="text-align:left;" class="value"><g:link controller="role" action="show" id="${accessKey?.role?.id}">${accessKey?.role}</g:link></td>
                              
                        </tr>
                   
                 </table>
           </div>
           <div class="buttons">
               <g:form controller="accessKey">
                 <input type="hidden" name="id" value="${accessKey?.id}" />
                 <span class="button"><g:actionSubmit value="Edit" /></span>
                 <span class="button"><g:actionSubmit value="Delete" /></span>
               </g:form>
           </div>
        </div>
    </body>
</html>
            