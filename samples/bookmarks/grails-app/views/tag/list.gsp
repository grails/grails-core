
<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
         <meta name="layout" content="main" />
         <title>Tag List</title>
    </head>
    <body>
        <div class="nav">
            <span class="menuButton"><a href="${createLinkTo(dir:'')}">Home</a></span>
            <span class="menuButton"><g:link action="create">New Tag</g:link></span>
        </div>
        <div class="body">
           <h1>Tag List</h1>
            <g:if test="${flash.message}">
                 <div class="message">
                       ${flash.message}
                 </div>
            </g:if>
           <table>
               <tr>
                   
                                      
                        <th>Id</th>
                                      
                        <th>Bookmark</th>
                                      
                        <th>Name</th>
                   
                   <th></th>
               </tr>
               <g:each in="${tagList}">
                    <tr>
                       
                            <td>${it.id}</td>
                       
                            <td>${it.bookmark}</td>
                       
                            <td>${it.name}</td>
                       
                       <td class="actionButtons">
                            <span class="actionButton"><g:link action="show" id="${it.id}">Show</g:link></span>
                       </td>
                    </tr>
               </g:each>
           </table>
        </div>
    </body>
</html>
            