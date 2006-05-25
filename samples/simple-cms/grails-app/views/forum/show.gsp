<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>		 
		  <g:render template="/pagemeta" model="[page:forum,levels:levels]" />	 	
    </head>
    <body>
        <div class="body">
            <g:if test="${flash.message}">
                 <div class="message">
                       ${flash.message}
                 </div>
            </g:if>
			<div class="intro">
				${forum.content}
			</div>			
			<h2>Topics:</h2>
			<div class="topics">
				<g:if test="${!topics}">
					There are currently no topics in this forum
				</g:if>
				
				<g:render template="/topic" collection="${topics}" />
			</div>
			<div class="buttons">
				<g:form url="[controller:'topic',action:'create']">
					<input type="hidden" name="forum.id" value="${forum?.id}" />
					<input type="submit" value="Add Topic" />
				</g:form>
			</div>
			
			<g:paginate action="show" total="${Page.findAllByType(Page.FORUM).size()}" />
        </div>
    </body>
</html>
            
