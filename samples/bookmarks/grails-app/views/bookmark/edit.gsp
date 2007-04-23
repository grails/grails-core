<div id="bookmark${bookmark.id}" style="display:none;">
<g:renderErrors as="list" bean="${bookmark}" />
<g:formRemote name="editorForm" url="[action:'update',id:bookmark.id]" update="bookmark${bookmark.id}">
<g:render template="editor" model="[bookmark:bookmark,suggestions:suggestions]" />
<div id="editButtons">
	<input type="submit" name="save" value="Save" />
	<g:submitToRemote url="[action:'show',id:bookmark.id]" update="bookmark${bookmark.id}" name="cancel" value="Cancel" />	
</div>
</g:formRemote>
</div>