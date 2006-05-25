<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
		 <title>${forum} / ${topic}</title>
		 <g:render template="/pagemeta" model="[page:forum,levels:levels]" />	 

    </head>
    <body>
        <div class="body">
           <g:if test="${flash['message']}">
                 <div class="message">${flash['message']}</div>
           </g:if>
		   <g:if test="${total}">
			   <div class="paginateButtons">
				<g:paginate action="show" id="${params.id}" total="${total}" />
			   </div>
		   </g:if>
		   
		   <g:if test="${!posts}">
		   		There are no posts in this topic
		   </g:if>
		   <g:render template="/post" collection="${posts}" />
		   		   
           <div class="buttons">
		   		<g:if test="${total}">
					<div class="paginateButtons">
						<g:paginate action="show" id="${params.id}" total="${total}" />
					</div>
				</g:if>
               <g:form controller="topic">
                 <input type="hidden" name="id" value="${topic?.id}" />
				 <input type="hidden" name="forum.id" value="${topic?.forum?.id}" />
				 <g:isContentEditor>
                 	<span class="button"><g:actionSubmit value="Edit" /></span>
				 </g:isContentEditor>
				 <span class="button"><g:actionSubmit value="Add Post" /></span>
               </g:form>
           </div>
        </div>
    </body>
</html>
            