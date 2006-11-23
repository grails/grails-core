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
 * A class that defines system Roles within a CMS for editors, approvers etc.
 *
 * @author Graeme Rocher
 * @since 10-May-2006
 */
class Role {
    static final SYSTEM_ADMINISTRATOR = 'System Administrator'
    static final CONTENT_EDITOR = 'Content Editor'
    static final CONTENT_APPROVER = 'Content Approver'
    static final GENERAL_USER = 'General User'

	 static belongsTo = Site
	 static optionals = ['site']

	 String title
     String name
     Site site
	

    String toString() { title }

    static constraints = {
		title(blank:false)
        name(inList:[SYSTEM_ADMINISTRATOR,CONTENT_EDITOR,CONTENT_APPROVER,GENERAL_USER])		
    }

	def isSysAdmin(){
		if(!name)return false
		if(name==SYSTEM_ADMINISTRATOR) return true
		return false
	}
	
	def isContentEditor() {
		if(!name)return false
		if(name==SYSTEM_ADMINISTRATOR || name == CONTENT_EDITOR) return true
		return false	
	}
	
	def isContentApprover() {
		if(!name)return false
		if(name==SYSTEM_ADMINISTRATOR || name == CONTENT_APPROVER) return true
		return false	
	}
}	
