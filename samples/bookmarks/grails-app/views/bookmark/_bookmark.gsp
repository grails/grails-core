<div id="bookmark${bookmark.id}"> 
	<div id="link${bookmark.id}">
		<a href="${bookmark.url}">${bookmark.title}</a>
		<g:if test="${bookmark.id}"> - 
		<g:remoteLink onComplete="Effect.BlindDown('bookmark${bookmark.id}');" action="edit" id="${bookmark.id}" update="bookmark${bookmark.id}">edit</g:remoteLink> | 
			<g:link action="delete" id="${bookmark.id}" 
			                   onclick="return confirm('Are you sure you want to delete this bookmark?')">delete</g:link> | 
			<a href="#" onmouseover="${remoteFunction(action:'preview',
			                                          id:bookmark.id,
			                                          update:'preview' + bookmark.id,
                                                	  onComplete:'Effect.Appear(preview'+bookmark.id+')')};"              
                         onmouseout="Effect.Fade('preview${bookmark.id}');">preview</a>
		</g:if>
	</div>     
		<div id="notes">
			<g:editInPlace id="notes${bookmark.id}" 
			               url="[action:'updateNotes',id:bookmark.id]" 
						   rows="5"
						   cols= "10"
			               paramName="notes">${bookmark.notes}</g:editInPlace>
		
		</div>	
	<div id="preview${bookmark.id}">
	</div>	
</div>
