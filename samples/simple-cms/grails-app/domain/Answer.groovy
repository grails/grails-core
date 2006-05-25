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
 * An answer to a Question instance
 *
 * @author Graeme Rocher
 * @since 10-May-2006
 */
class Answer { 
	@Property Long id
	@Property Long version

	@Property belongsTo = Question
	
	@Property boolean correct
	@Property String text
	@Property Question question	
	
	@Property constraints = {
		text(blank:false)
		question(nullable:false)
	}	
	
    String toString() { "$text" }
	
	boolean equals(other) {
		if(other?.is(this))return true
		if(!(other instanceof Answer)) return false
		
		if(!id || !other?.id || id!=other?.id) return false
		
		return true
	}
	
	int hashCode() {
		int hashCode = 0
		hashCode = 29 * (hashCode + ( !id ? 0 : id ^ (id >>> 32) ) )
	}
}	
