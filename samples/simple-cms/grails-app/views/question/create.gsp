
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
				
		<meta name="layout" content="main" />
        <title>Create Question</title>
		<script type="text/javascript" src="${createLinkTo(dir:'fckeditor',file:'fckeditor.js')}"></script>
		<g:javascript library="yahoo"></g:javascript>
		<g:javascript>			
			var answerIndex = 2;
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
           <h1>Create Question</h1>
           <g:if test="${flash['message']}">
                 <div class="message">${flash['message']}</div>
           </g:if>
           <g:hasErrors bean="${question}">
                <div class="errors">
                    <g:renderErrors bean="${question}" as="list" />
                </div>
           </g:hasErrors>
           <g:form action="save" method="post">
               <div class="dialog">
                <table>
					<tr class='prop'>
						<td valign='top' style='text-align:left;' width='20%'>
							<label for='text'>Question:</label>
						</td>
						<td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:question,field:'title','errors')}'>
							<g:richTextEditor name="text" toolbar="Basic" height="75" />
						</td>
					</tr>
					<tr class='prop'>
						<td valign='top' style='text-align:left;' width='20%'>
							<label for='text'>Answers:</label>
						</td>
						<td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:question,field:'title','errors')}'>
							<ol id="answers">
								<li>
									<input class="answer" type="text" name="answer" ></input>
									<input class="answer" type="checkbox" name="correct1" ></input>
								</li>
								<li>
									<input class="answer" type="text" name="answer"></input>
									<input class="answer" type="checkbox" name="correct2" ></input>
								</li>
							</ol>
							<a href="javascript:void(0);" onclick="addAnswer()">+ Add Answer</a>
						</td>
					</tr>								  	
<tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='score'>Score:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:question,field:'score','errors')}'><g:select from='${1..100}' name='score' value='${question?.score}'></g:select></td></tr>                       
								  	<input type="hidden" name="quest.id" value="${quest?.id}" /> 
               </table>
               </div>
               <div class="buttons">
                     <span class="formButton">
                        <input type="submit" value="Create"></input>
                     </span>
               </div>
            </g:form>
        </div>
    </body>
</html>
            