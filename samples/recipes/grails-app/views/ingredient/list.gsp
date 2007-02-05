<%@ page import="com.recipes.Ingredient" %>  
<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
         <meta name="layout" content="main" />
         <title>Ingredient List</title>
    </head>
    <body>
        <div class="nav">
            <span class="menuButton"><a href="${createLinkTo(dir:'')}">Home</a></span>
            <span class="menuButton"><g:link action="create">New Ingredient</g:link></span>
        </div>
        <div class="body">
           <h1>Ingredient List</h1>
            <g:if test="${flash.message}">
                 <div class="message">
                       ${flash.message}
                 </div>
            </g:if>
           <table>
             <thead>
               <tr>
                   
                                      
                        <th>Id</th>
                                      
                        <th>Name</th>
                                      
                        <th>Quantity</th>
                                      
                        <th>Recipe</th>
                   
                   <th></th>
               </tr>
             </thead>
             <tbody>
               <g:each in="${ingredientList}">
                    <tr>
                       
                            <td>${it.id?.encodeAsHTML()}</td>
                       
                            <td>${it.name?.encodeAsHTML()}</td>
                       
                            <td>${it.quantity?.encodeAsHTML()}</td>
                       
                            <td>${it.recipe?.encodeAsHTML()}</td>
                       
                       <td class="actionButtons">
                            <span class="actionButton"><g:link action="show" id="${it.id}">Show</g:link></span>
                       </td>
                    </tr>
               </g:each>
             </tbody>
           </table>
               <div class="paginateButtons">
                   <g:paginate total="${Ingredient.count()}" />
               </div>
        </div>
    </body>
</html>
