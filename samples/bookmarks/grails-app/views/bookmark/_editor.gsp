<div id="editor">
	<p>Title: <input type="text" name="title" value="${bookmark?.title}"/></p>
	<p>URL: <g:remoteField action="suggestTag" update="suggestions"  name="url" value="${bookmark?.url}"/></p>	
	<p>Notes:</p>
	<p><textarea name="notes" col="1" row="1">${bookmark?.notes}</textarea></p>
	<p>Tags:</p>
	<p><input type="text" name="tagTokens" id="tags${bookmark?.id}" /></p>
	
	<div id="suggestions${bookmark?.id}">
	     <g:each in="${suggestions?}">
     		<span class="suggestion">
	           <a href="#" onclick="$('tags${bookmark.id}.value += this.value+" "');this.style.display='none';">${it}</a>
			</span> 
   		</g:each>
	</div>
</div>
