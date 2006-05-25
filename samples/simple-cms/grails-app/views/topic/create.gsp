
<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
		 <g:render template="/pagemeta" model="[page:forum,levels:levels]" />        		 
    </head>
    <body>
        <div class="body">
           <g:if test="${flash['message']}">
                 <div class="message">${flash['message']}</div>
           </g:if>
           <g:hasErrors bean="${topic}">
                <div class="errors">
                    <g:renderErrors bean="${topic}" as="list" />
                </div>
           </g:hasErrors>
           <g:form action="save" method="post">
               <div class="dialog">
                <table>

                       
                      				<tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='title'>Title:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:topic,field:'title','errors')}'><input type='text' name='title' value='${topic?.title}' /></td></tr>
					   
                                  <tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='description'>Description:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:topic,field:'description','errors')}'><textarea name="description" columns="1" rows="1">${topic?.description}</textarea></td></tr>
                       
                                 <input type="hidden" name="forum.id" value="${forum?.id}" />
                       
                                  
                       
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
            