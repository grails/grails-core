<%@ page import="com.recipes.Ingredient" %>  
<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
          <meta name="layout" content="main" />
         <title>Show Ingredient</title>
    </head>
    <body>
        <div class="nav">
            <span class="menuButton"><a href="${createLinkTo(dir:'')}">Home</a></span>
            <span class="menuButton"><g:link action="list">Ingredient List</g:link></span>
            <span class="menuButton"><g:link action="create">New Ingredient</g:link></span>
        </div>
        <div class="body">
           <h1>Show Ingredient</h1>
           <g:if test="${flash.message}">
                 <div class="message">${flash.message}</div>
           </g:if>
           <div class="dialog">
                 <table>
                   
                   <tbody>
                   
                        <tr class="prop">
                              <td valign="top" class="name">Id:</td>
                              
                                    <td valign="top" class="value">${ingredient.id}</td>
                              
                        </tr>
                   
                        <tr class="prop">
                              <td valign="top" class="name">Name:</td>
                              
                                    <td valign="top" class="value">${ingredient.name}</td>
                              
                        </tr>
                   
                        <tr class="prop">
                              <td valign="top" class="name">Quantity:</td>
                              
                                    <td valign="top" class="value">${ingredient.quantity}</td>
                              
                        </tr>
                   
                        <tr class="prop">
                              <td valign="top" class="name">Recipe:</td>
                              
                                    <td valign="top" class="value"><g:link controller="recipe" action="show" id="${ingredient?.recipe?.id}">${ingredient?.recipe}</g:link></td>
                              
                        </tr>
                   
                   </tbody>
                 </table>
           </div>
           <div class="buttons">
               <g:form controller="ingredient">
                 <input type="hidden" name="id" value="${ingredient?.id}" />
                 <span class="button"><g:actionSubmit value="Edit" /></span>
                 <span class="button"><g:actionSubmit value="Delete" /></span>
               </g:form>
           </div>
        </div>
    </body>
</html>
