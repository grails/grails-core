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
 * A Revision defines the current state of Page, when it was last updated and by whom
 * This allows workflow control within the CMS (edit/approve/publish)
 *
 * @author Graeme Rocher
 * @since 10-May-2006
 */
class Revision implements Comparable { 
	
	static final PUBLISHED = "Live"	
	static final EDITED = "Edited"
	static final APPROVED = "Approved"
	static final REJECTED = "Rejected"
	static final DELETED = "Deleted"
	static final DELETE_REQUESTED = "Delete Requested"
	static final ADDED = "Added"
	
	@Property belongsTo = [Page,User]
	@Property relatesToMany = [ comments : Comment ]
	
	@Property Long id
	@Property Long version
    
    @Property String content
    @Property Integer number = 1
    @Property Page page
    @Property User updatedBy
	@Property Date lastUpdated = new Date()
	@Property String state = ADDED
	@Property SortedSet comments
	
	def addComment(user,msg) {
		if(!comments)comments = new TreeSet()
			
		comments.add(new Comment(leftBy:user,message:msg,revision:this))
		return this
	}
	
	@Property constraints = {
		state(inList:[PUBLISHED,REJECTED,EDITED,APPROVED,ADDED,DELETED,DELETE_REQUESTED])		
	}
    
    String toString() { "${this.class.name} :  $id" }    
   
    int compareTo(other) {
    	if(number < other.number) {
    		return -1
    	}
    	else if(number == other.number) {
    		return 0
    	}
    	return 1
    }
}	
