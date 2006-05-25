
<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
         <title>Question List</title>
         <link rel="stylesheet" href="${createLinkTo(dir:'css',file:'main.css')}"></link>
    </head>
    <body>
        <div class="logo"><img src="${createLinkTo(dir:'images',file:'grails_logo.jpg')}" alt="Grails" /></div>
        <div class="nav">
            <span class="menuButton"><a href="${createLinkTo(dir:'')}">Home</a></span>
            <span class="menuButton"><g:link action="create">New Question</g:link></span>
        </div>
        <div class="body">
           <h1>Question List</h1>
            <g:if test="flash['message']">
                 <div class="message">
                       ${flash['message']}
                 </div>
            </g:if>
           <table>
               <tr>
                   
                                      
                        <th>Id</th>
                                      
                        <th>Title</th>
                                      
                        <th>Text</th>
                                      
                        <th>Score</th>
                                      
                        <th>Number</th>
                                      
                        <th>Questionnaire</th>
                   
                   <th></th>
               </tr>
               <g:each in="${questionList}">
                    <tr>
                       
                            <td>${it.id}</td>
                       
                            <td>${it.title}</td>
                       
                            <td>${it.text}</td>
                       
                            <td>${it.score}</td>
                       
                            <td>${it.number}</td>
                       
                            <td>${it.questionnaire}</td>
                       
                       <td class="actionButtons">
                            <span class="actionButton"><g:link action="show" id="${it.id}">Show</g:link></span>
                       </td>
                    </tr>
               </g:each>
           </table>
        </div>
    </body>
</html>
            