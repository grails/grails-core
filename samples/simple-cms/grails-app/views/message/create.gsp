
<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
		 <script type="text/javascript" src="${createLinkTo(dir:'fckeditor',file:'fckeditor.js')}"></script>
		 <title>Add Post</title>
		 <g:render template="/pagemeta" model="[page:forum,levels:levels]" />	 	
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
		    <h2>Create post in ${topic}</h2>
           <g:form action="save" method="post">
               <div class="dialog">
                <table>

                       
                       
                                <tr class='prop'>
								  	<td valign='top' style='text-align:left;' width='20%'>
										<label for='title'>Title:</label>
									</td>
									<td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:message,field:'title','errors')}'>
										<input type="text" maxlength='50' name='title' value='${lastPost ? (lastPost.title.startsWith("RE:")? lastPost.title : "RE: " + lastPost.title) : message?.title}'></input>
									</td>
								  </tr>
                       
								  <tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='message'>Message:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:message,field:'message','errors')}'>
								    <g:if test="${[Role.CONTENT_EDITOR,Role.SYSTEM_ADMINISTRATOR].contains(session.user?.role.name)}">
										<g:richTextEditor name="message" height="300" toolbar="EditorBasic" value="${message?.message}"/>
									</g:if>
									<g:else>
								  		<g:richTextEditor name="message" height="300" toolbar="Basic" value="${message?.message}"/>
									</g:else>
								  </td></tr>
                       
                       <input type="hidden" name="topic.id" value="${topic?.id}" />
                       
                       
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
            