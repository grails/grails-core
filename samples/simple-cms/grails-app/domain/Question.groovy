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
 * A question that forms part of a questionnaire (a Page of type Page.QUESTIONNAIRE)
 *
 * @author Graeme Rocher
 * @since 10-May-2006
 */
class Question { 
	 static belongsTo = Page
	 static hasMany = [answers:Answer]

	 Page questionnaire
	 String text
	 Integer score = 1
	 Integer number
	
     String toString() { "$text" }	
	
	def addAnswer(ans,isCorrect) {
		if(!answers)answers = new HashSet()
		
		def a = new Answer(question:this,text:ans,correct:isCorrect)
		answers.add(a)
		return this
	}
	
	static constraints = {
		text(blank:false)
		score(range:1..100)		
		number(minSize:1)
	}
}	
