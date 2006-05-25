
<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
		 <script type="text/javascript" src="${createLinkTo(dir:'fckeditor',file:'fckeditor.js')}"></script>
		 <title>Contact User</title>
		 <g:render template="/pagemeta" model="[:]" />	 	
    </head>
    <body>
        <div class="body">
           <g:if test="${flash['message']}">
                 <div class="message">${flash['message']}</div>
           </g:if>
           <g:hasErrors bean="${message}">
                <div class="errors">
                    <g:renderErrors bean="${message}" as="list" />
                </div>
           </g:hasErrors>
		   <h2>Send message to ${contactUser}</h2>
           <g:form action="send" method="post">
               <div class="dialog">
                <table>

                       
                                  <tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='subject'>Subject:</label></td><td valign='top' style='text-align:left;' width='80%' ><input type="text" maxlength='50' name='subject' value='${flash.title}'></input></td></tr>
                       
								  <tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='msg'>Body:</label></td><td valign='top' style='text-align:left;' width='80%' >
								  	<g:richTextEditor name="msg" height="300" toolbar="Basic" value="${flash.msg}"/>
								  </td></tr>
                       
                       <input type="hidden" name="id" value="${contactUser?.id}" />
                       
                       
               </table>
               </div>
               <div class="buttons">
                     <span class="formButton">
                        <input type="submit" value="Send"></input>
                     </span>
               </div>
            </g:form>
        </div>
    </body>
</html>
            
