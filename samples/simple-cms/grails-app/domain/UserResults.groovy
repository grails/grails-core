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
 * Stores results that users have answered in a questionnaire (a Page
 * of type Page.QUESTIONNAIRE)
 *
 * @author Graeme Rocher
 * @since 10-May-2006
 */
class UserResults { 
	@Property Long id
	@Property Long version
	
	@Property belongsTo = [User,Answer,Question,Page]
	
	@Property User user
	@Property Page questionnaire
	@Property Question question
	@Property Answer answer
	@Property Date created = new Date()
	

    String toString() { "${this.class.name} :  $id" }
	
	boolean equals(other) {
		if(other?.is(this))return true
		if(!(other instanceof UserResults)) return false
		
		if(!id || !other?.id || id!=other?.id) return false
		
		return true
	}
	
	int hashCode() {
		int hashCode = 0
		hashCode = 29 * (hashCode + ( !id ? 0 : id ^ (id >>> 32) ) )
	}
}	
