<g:def var="q" value="${it}" />
<div class="question">
	<div id="question${q.id}" class="text">${q}</div>
	<div class="answers">
			
			<g:each var="a" in="${q.answers}">
				<div class="answer">
					<g:if test="${q.answers.correct.count(true) == 1}">
						<g:radio 	class="question${q.id}" 
									name="singleAnswer${q.id}" 
									value="${a.id}"
									checked="${UserResults.findByUserAndAnswer(session.user,a)}" />
					</g:if>
					<g:else>
						<g:checkBox class="question${q.id}" name="multiAnswer${a.id}" value="${UserResults.findByUserAndAnswer(session.user,a)}" />					
					</g:else>
					${a}
				</div>	
			</g:each>
	</div>
	<div class="buttons">
		<g:isContentEditor>
			<g:link controller="question" action="edit" id="${q.id}">Edit Question</g:link>
		</g:isContentEditor>
	<div>
</div>
