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
 * Controller that handles CRUD operations for questions
 *
 * @author Graeme Rocher
 * @since 10-May-2006
 */
class QuestionController extends BaseController {
    def index = { redirect(action:list,params:params) }

    def edit = {
        def question = Question.get( params['id'] )

        if(!question) {
                flash['message'] = "Question not found with id ${params['id']}"
                redirect(action:list)
        }
        else {
            return [ question : question ]
        }
    }

    def update = {
		if(!params.'quest.id') {
			flash.message = "Questionnaire not found."
			redirect(controller:'page')			
		}
		else if(!params.text) {
			flash.message = "Question text must be specified"
			redirect(action:create,params:['quest.id':params.'quest.id'])
		}
		else if(!(params.answer instanceof String[])) {
			flash.message = "Question must have at least 2 possible answers"
			redirect(action:create,params:['quest.id':params.'quest.id'])
		}
		else {
			def question = Question.get( params.id )
			if(question) {
     			question.properties = params
				
				// updating existing answers
				params.answerId.eachWithIndex { aid, i ->
					def a = question.answers.find { it.id == aid.toLong() }
					a.text = params.answer[i]
					a.correct = (params["correct${i+1}"] ? true: false)
				}
				
				// compare answerId count to answers to check for new answers
				if(params.answerId.size() < params.answer.size()) {
					for(i in params.answerId.size()..params.answer.size()-1) {
						question.addAnswer(params.answer[i],	(params["correct${i+1}"] ? true: false) )
					}
				}
				// check that there is at least on correct answer
				def corrects = question.answers.findAll { it.correct == true }
				if(!corrects) {
					flash.message = "Question must have at least one correct answer"
					redirect(action:create,params:['quest.id':params.'quest.id'])										
				}
				else {
 					if(question.save()) {
						redirect(controller:'test',action:'show',id:params.'quest.id')	
					}
					else {
						redirect(action:create,params:['quest.id':params.'quest.id'])
					}	 			
				}
			}
			else {
				flash.message = "Question not found with id ${params['id']}"
				redirect(controller:'test',action:'show',id:params.'quest.id')
			}			
		}
    }

    def create = {
		if(!params.'quest.id') {
			flash.message = "Questionnaire not found."
			redirect(controller:'page')			
		}
		else {
			def question = new Question()
			question.properties = params
			return ['question':question,quest:Page.get(params.'quest.id')]			
		}
    }

    def save = {
		if(!params.'quest.id') {
			flash.message = "Questionaire not found"
			redirect(controller:'test',action:'list')
		}
		else if(!params.text) {
			flash.message = "Question text must be specified"
			redirect(action:create,params:['quest.id':params.'quest.id'])
		}
		else if(!(params.answer instanceof String[])) {
			flash.message = "Question must have at least 2 possible answers"
			redirect(action:create,params:['quest.id':params.'quest.id'])
		}
		else {
			def p = Page.get(params.'quest.id')
			if(p.type != Page.QUESTIONNAIRE) {				
				flash.message = "Specified page is not a Questionaire"
				redirect(controller:'test',action:'list')				
			}
			else {
				def question = new Question(questionnaire:p)
				question.properties = params
				// find all questions for questionnaire
				def total = Question.executeQuery('select count(q) from Question q where q.questionnaire.id = ?',p.id)
				question.number = total[0] + 1
				
				def anyCorrect = false
				params.answer.eachWithIndex { a,i ->
					def correct = (params["correct${i+1}"] ? true: false)
					if(correct) anyCorrect = true
					question.addAnswer(a,correct)
				}
				if(!anyCorrect) {
					flash.message = "Question must have at least one correct answer"
					redirect(action:create,params:['quest.id':params.'quest.id'])					
				}
				else {
					if(question.save()) {
						redirect(controller:'test',action:'show',id:params.'quest.id')	
					}
					else {
						redirect(action:create,params:['quest.id':params.'quest.id'])
					}					
				}
			}			
		}
    }

}