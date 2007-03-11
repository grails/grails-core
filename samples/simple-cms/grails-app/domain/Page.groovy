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
 * Defines a page class. There are different types of Pages (standard, forums,links)
 * Each has many revisions.
 *
 * @author Graeme Rocher
 * @since 10-May-2006
 */
class Page implements Comparable {
	static final STANDARD = 'standard'
	static final FORUM = 'forum'	
	static final QUESTIONNAIRE = 'questionnaire'
	static final LINK = 'link'	
	
	 static belongsTo = [Site,Page,Revision]
	 static hasMany = [ children:Page,
								revisions:Revision]
	
	 String type = STANDARD
	 Site site
	 Page parent
	 User createdBy
	 String title
	 String content = ''
	 SortedSet children
	 SortedSet revisions
	 Integer pos = 1
	 Boolean enabled = true
	
	def addPage(page) {
		if(!children)children = new TreeSet()		
		page.parent = this
		page.pos = children.size() + 1
		children.add(page)
		return this
	}
	
	def addNewRevision(rev) {
		if(!revisions)revisions = new TreeSet()
		rev.page = this
		rev.content= this.content
		if(revisions.size() > 0) {			
			rev.number = revisions.last().number +1
		}
		else {
			rev.number = 1
		}
		revisions.add(rev)
		return this
	}

	int compareTo(other) {
		if(pos < other.pos)
			return -1
		else if(pos == other.pos)
			return 0
		else
			return 1
	}
	
    String toString() { title }

	 static constraints = {
		title(blank:false,size:1..150)
		site(nullable:false)
		type(inList:[STANDARD,FORUM,QUESTIONNAIRE,LINK])
		revisions(minSize:1)
	}
}	
