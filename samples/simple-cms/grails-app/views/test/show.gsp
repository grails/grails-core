<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>		 
		  <g:render template="/pagemeta" model="[page:quest,levels:levels]" />
		  <g:javascript library="rico"></g:javascript>
		  <g:javascript>
		  		var questionClasses = [
					<g:def var="i" expr="${0}" />
					<g:each var="q" in="${questions}">
						<g:set var="i" expr="${i+1}" />
						"question${q.id}"<g:if test="${i!=questions.size()}">,</g:if>
					</g:each>
				]
		  		function validateQuestions(form) {
					
					var valid = true;
					for(var i =0; i < questionClasses.length;i++) {
						var qClass = questionClasses[i];
						var answers = document.getElementsByTagAndClassName('input',qClass )
						var foundSelectedAnswer = false
						for(var j =0; j<answers.length;j++) {
							if(answers[j].checked) {
								foundSelectedAnswer = true
								break;
							}
						}
						if(!foundSelectedAnswer) {
							valid = false;
							$(qClass).className = 'errors';
							break;
						}
						else {
							$(qClass).className = 'text';
						}
					}
					if(!valid) {
						alert('All questions must be answered.');
					}
					return valid;
				}
		  </g:javascript>
    </head>
    <body>
        <div class="body">
            <g:if test="${flash.message}">
                 <div class="message">
                       ${flash.message}
                 </div>
            </g:if>
			<div class="intro">
				${quest.content}
			</div>			
			<h2>Answer the questions below:</h2>
			<div class="questions">
				<g:if test="${!questions}">
					There are currently no questions in this questionnaire
				</g:if>
				
				<g:form onsubmit="return validateQuestions(this)" url="[controller:'test',action:'submit']">
					<input type="hidden" name="id" value="${quest?.id}" />
					
					<g:render template="/question" collection="${questions}" />
					<div class="buttons">
						<input type="submit" value="Submit Answers" />
					<div>
			    </g:form>
			</div>
			<g:isContentEditor>
				<div class="buttons">
				
					<g:form url="[controller:'question',action:'create']">
						<input type="hidden" name="quest.id" value="${quest?.id}" />
						<input type="submit" value="Add Question" />
					</g:form>
				</div>
			</g:isContentEditor>
			
        </div>
    </body>
</html>
            
