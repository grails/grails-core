/*
 * Copyright 2004-2005 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 
 
/**
 * A controller that displays tests and calculates the results of a submitted
 * test
 *
 * @author Graeme Rocher
 * @since 10-May-2006
 */
class TestController extends BaseController {

	def index = { 
		redirect(action:list)
	}
	
	def list = {
		[tests: Page.findAllByType(Page.QUESTIONNAIRE) ]	
	}
	
	def show = {
		def quest = Page.get(params.id)
		
		if(!quest) {
			flash.message = "Questionnaire ${params.id} not found"
			redirect(action:list)			
		}
		else if(quest.type != Page.QUESTIONNAIRE) {
			flash.message = "Page ${params.id} is not a Questionnaire"
			redirect(action:list)
		}
		else {
			def questions = Question.findAllByQuestionnaire(quest,[sort:'number'])
			return [quest:quest,questions:questions]	
		}
	}
	
	def submit = {
		if(!params.id) {
			flash.message = "Questionnaire ${params.id} not found"
			redirect(controller:'page')	
		}
		else {
			def p = Page.get(params.id)
			def questions = Question.findAllByQuestionnaire(p)
			def results = []
			
			println( "Number of questions: ${questions.size()}" )
			
			for(q in questions) {
				// if there is only one correct answer
				println "processing question $q"
				if(q.answers.correct.count(true) == 1) {
					def aId = params["singleAnswer${q.id}"]
					
					println "got answer id $aId"
					if(!aId) {
						flash.message = "Question '${q.text}' was not answered"
						redirect(action:show,id:params.id)
						break
					}
					else {
						// store user answer check if it exists first
						def a = Answer.get(aId)
						def allResults = UserResults.findAllByUserAndQuestion(session.user,q)
						def UserResults ur
						
						if(allResults) {
							ur = allResults[0]
							// this would only occur if the question had been changed
							// from a many-from-many to a one-from-many
							if(allResults.size() > 1) {
								for(i in 1..allResuts.size()) {
									allResults[i].delete()	
								}
							}
							ur.answer = a						
						}
						else {
							ur = new UserResults(	user:session.user,
															questionnaire:p,
															question:q,
															answer:a )							
						}
						results << ur					
					}
				}
				else {
					// otherwise we have to figure out which answers
					// the user did select from a many-from-many
					def allResults = UserResults.findAllByUserAndQuestion(session.user,q)
					// if they have existing results we need to go through
					// and delete those that are no longer selected and add
					// those that are
					q.answers.each { a ->
						def r = allResults.find { it.answer == a }
						if(!params["multiAnswer${a.id}"]) {								
							if(r) r.delete()
						}
						else {
							if(!r) {
								results << 	new UserResults(	user:session.user,
																questionnaire:p,
																question:q,
																answer:a )
							}
						}
					}
				}
			}
			
			results.each { r ->
				r.save()	
			}
			flash.message = "Your results have been saved and will be available to you the next time you visit."
			redirect(action:show,id:params.id)
		}
	}
}

