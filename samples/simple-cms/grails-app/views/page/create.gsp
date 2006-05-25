
<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
         <title>Create Page</title>
         <link rel="stylesheet" href="${createLinkTo(dir:'css',file:'main.css')}"></link>
    </head>
    <body>
        <div class="logo"><img src="${createLinkTo(dir:'images',file:'grails_logo.jpg')}" alt="Grails" /></div>
        <div class="nav">
            <span class="menuButton"><a href="${createLinkTo(dir:'')}">Home</a></span>
            <span class="menuButton"><g:link action="list">Page List</g:link></span>
        </div>
        <div class="body">
           <h1>Create Page</h1>
           <g:if test="${flash['message']}">
                 <div class="message">${flash['message']}</div>
           </g:if>
           <g:hasErrors bean="${page}">
                <div class="errors">
                    <g:renderErrors bean="${page}" as="list" />
                </div>
           </g:hasErrors>
           <g:form action="save" method="post">
               <div class="dialog">
                <table>

                       
                       
                                  <tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='title'>Title:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:page,field:'title','errors')}'><textarea rows='1' cols='1' name='title'>${page?.title}</textarea></td></tr>
                       
                                  <tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='site'>Site:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:page,field:'site','errors')}'><g:select optionKey="id" from="${Site.list()}" name='site.id' value='${page?.site?.id}'></g:select></td></tr>
                       
                                  <tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='template'>Template:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:page,field:'template','errors')}'><textarea rows='1' cols='1' name='template'>${page?.template}</textarea></td></tr>
                       
                                  <tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='type'>Type:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:page,field:'type','errors')}'><select name='type'><option value='standard'>standard</option><option value='forum'>forum</option><option value='questionnaire'>questionnaire</option></select></td></tr>
                       
                                  <tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='content'>Content:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:page,field:'content','errors')}'><input type='text' name='content' value='${page?.content}' /></td></tr>
                       
                                  <tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='parent'>Parent:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:page,field:'parent','errors')}'><g:select optionKey="id" from="${Page.list()}" name='parent.id' value='${page?.parent?.id}'></g:select></td></tr>
                       
                                  <tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='revisions'>Revisions:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:page,field:'revisions','errors')}'><ul>
	                                 
    <g:each var='r' in='${page?.revisions?}'>
        <li><g:link controller='revision' action='show' id='${r.id}'>${r}</g:link></li>
    </g:each>
</ul>
<g:link controller='revision' params='["page.id":page?.id]' action='create'>Add Revision</g:link>
</td></tr>
                       
                                  <tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='state'>State:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:page,field:'state','errors')}'><g:select optionKey="id" from="${State.list()}" name='state.id' value='${page?.state?.id}'></g:select></td></tr>
                       
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
            