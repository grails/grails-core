<%@page contentType="text/xml" %>
<ajax-response>
	<g:if test="${alert}">
		<alert message="${alert}" />	
	</g:if>	
	<g:javascript library="yahoo"></g:javascript>
	<page id="${page?.id}" parentId="${page?.parent?.id}" type="${page?.type}">	
		<title>${page?.title}</title>
		<g:if  test="${page?.type == Page.LINK}">
			<g:if test="${page?.content ==~ /\d+/}">
				<g:def var="linked" value="${Page.get(page?.content)}" />
				<content><![CDATA[Link to Page: <b>${linked.title}</b>, on Site:<b>${linked.site.name}</b>]]></content>			
			</g:if>
			<g:else>
				<content><![CDATA[Link to : ${page?.content}]]></content>
			</g:else>					
		</g:if>
		<g:else>
			<content><![CDATA[${page?.content}]]></content>
		</g:else>
		<details><![CDATA[
			<div class="pageControls">
				<g:form id="pageControls" name="pageControls" url="[controller:'page']" onsubmit="return false;">
					<input type="hidden" id="pageId" name="id" value="${page?.id}"></input>
					<g:approveButton page="${page}"
									url="[controller:'page',action:'approve']"></g:approveButton>
					<g:rejectButton page="${page}"
									url="[controller:'page',action:'reject']"></g:rejectButton>
					<g:rollbackButton 	page="${page}" 
									message="Are you sure you want to rollback the last change?" 
									url="[controller:'page',action:'undo']"></g:rollbackButton>
					<g:publishButton page="${page}"
									url="[controller:'page',action:'publish']"></g:publishButton>
									
				</g:form>
			</div>
			<table class="pageDetails">
				<tr>
					<th>Type</th>
					<th>Last Updated</th>
					<th>Current Revsion</th>
					<th>State</th>
				</tr>
				<g:if test="${page?.revisions?.size() > 0}">
					<tr>
						<td>${page?.type}</td>
						<td>${page?.revisions?.last().lastUpdated} - by <a href="mailto:${page?.revisions?.last().updatedBy?.email}">${page?.revisions?.last().updatedBy}</a></td>
						<td>${page?.revisions?.last().number}</td>
						<td>${page?.revisions?.last().state}</td>
					</tr>
				</g:if>
			</table>			
			]]>
		</details>
		<comments><![CDATA[
			<table class="comments">
				<tr><th>Latest Comment</th></tr>
				<g:if test="${page?.revisions?.size() > 0}">
					<g:if test="${page?.revisions?.last().comments}">
						<tr><td style="text-align:left;">
							<div class="comments">
								<g:render template="/comment" model="[comment:page?.revisions?.last().comments?.last()]" />
							</div>							
						</td></tr>
					</g:if>
				</g:if>
			</table>
			<g:link target="_blank" controller="page" action="comments" id="${page?.id}">Show All</g:link>
			<a href="javascript:void(0);" id="addCommentLink">Add Comment</a>
			<div class="commentBox" id="commentBox" style="display:none;">
				<textarea name="comment" id="commentField"></textarea>
			</div>]]>			
		</comments>
	</page>
</ajax-response>
