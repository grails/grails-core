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
	
	@Property belongsTo = [Site,Page,Revision]
	@Property relatesToMany = [ children:Page,
								revisions:Revision]
	
	@Property Long id
	@Property Long version
	
	@Property String type = STANDARD
	@Property Site site
	@Property Page parent
	@Property User createdBy
	@Property String title
	@Property String content = ''
	@Property SortedSet children
	@Property SortedSet revisions
	@Property Integer position = 1
	@Property Boolean enabled = true
	
	def addPage(page) {
		if(!children)children = new TreeSet()		
		page.parent = this
		page.position = children.size() + 1
		children.add(page)
		return this
	}
	
	def addRevision(rev) {
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

	boolean equals(other) {
		if(this == other) return true
		if(!(other instanceof Page)) return false
		if(!parent && (title == other.title)) return true		
		if(title == other.title && parent.id == other.parent?.id) return true		
		return false
	}
	
	int hashCode() {
		int result = 23 + position
		if(title) {
			result *= 37
			result += title.hashCode()
		}
		if(parent?.id) {
			result *= 37
			result += (parent.id ^ (parent.id >>> 32))
		}
		if(site?.id) {
			result += 37
			result += (site.id ^ (site.id >>> 32))
		}
		return result
	}
	
	int compareTo(other) {
		if(position < other.position)
			return -1
		else if(position == other.position)
			return 0
		else
			return 1
	}
	
    String toString() { title }

	@Property constraints = {
		title(blank:false,length:1..150)
		site(nullable:false)
		type(inList:[STANDARD,FORUM,QUESTIONNAIRE,LINK])
		revisions(minSize:1)
	}
}	
