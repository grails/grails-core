
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
				
		<meta name="layout" content="main" />
        <title>Edit Question</title>
		<script type="text/javascript" src="${createLinkTo(dir:'fckeditor',file:'fckeditor.js')}"></script>
		<g:javascript library="yahoo"></g:javascript>
		<g:javascript>			
			var answerIndex = ${question?.answers?.size()};
			function addAnswer() {
				var answers = YAHOO.util.Dom.get('answers')
				var li = document.createElement('li')
				li.innerHTML = "<input type='text' name='answer'></input>\n"+
								"<input class='answer' type='checkbox' name='correct"+ ++answerIndex +"' ></input>";
				
				answers.appendChild(li);
			}
		</g:javascript>
    </head>
    <body>
        <div class="body">
           <h1>Edit Question</h1>
           <g:if test="${flash['message']}">
                 <div class="message">${flash['message']}</div>
           </g:if>
           <g:hasErrors bean="${question}">
                <div class="errors">
                    <g:renderErrors bean="${question}" as="list" />
                </div>
           </g:hasErrors>
           <g:form action="update" method="post">
		   		<input type="hidden" name="id" value="${question?.id}" />
               <div class="dialog">
                <table>
					<tr class='prop'>
						<td valign='top' style='text-align:left;' width='20%'>
							<label for='text'>Question:</label>
						</td>
						<td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:question,field:'title','errors')}'>
							<g:richTextEditor value="${question.text}" name="text" toolbar="Basic" height="75" />
						</td>
					</tr>
					<tr class='prop'>
						<td valign='top' style='text-align:left;' width='20%'>
							<label for='text'>Answers:</label>
						</td>
						<td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:question,field:'title','errors')}'>
							<ol id="answers">
								<g:def var="i" expr="${0}" />
								<g:each var="a" in="${question?.answers?}">
									<g:set var="i" expr="${i+1}" />
									<li>
										<input class="answer" type="text" name="answer" value="${a}"></input>
										<input type="hidden" name="answerId" value="${a.id}"></input>
										<input type="checkbox" name="correct${i}" <%if(a.correct)%>checked="true"<%;%> ></input>
									</li>								
								</g:each>
							</ol>
							<a href="javascript:void(0);" onclick="addAnswer()">+ Add Answer</a>
						</td>
					</tr>								  	
<tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='score'>Score:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:question,field:'score','errors')}'><g:select from='${1..100}' name='score' value='${question?.score}'></g:select></td></tr>                       
								  	<input type="hidden" name="quest.id" value="${question?.questionnaire?.id}" /> 
               </table>
               </div>
               <div class="buttons">
                     <span class="formButton">
                        <input type="submit" value="Save"></input>
                     </span>
               </div>
            </g:form>
        </div>
    </body>
</html>
            