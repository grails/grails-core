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
 * Defines tags to controller workflow and access to the CMS system
 *
 * @author Graeme Rocher
 * @since 10-May-2006
 */
class CMSTagLib {
	
	/**
	 * Only invokes the body if the user within the session is a system administrator
	 */
	def isSysAdmin = { attrs, body ->
		if(session.user) {
			def u = session.user
			if(u.role.isSysAdmin()) {
				out << body()	
			}
		}
	}
	
	/**
	 * Only invokes the body if the user within the session is a content editor
	 */
	def isContentEditor = { attrs, body ->
		if(session.user) {
			def u = session.user
			if(u.role.isContentEditor()) {
				out << body()	
			}
		}
	}
	
	/**
	 * Only invokes the body if the user within the session is a content approver
	 */
	def isContentApprover = { attrs, body ->
		if(session.user) {
			def u = session.user
			if(u.role.isContentApprover()) {
			   out << body()	
			}
		}
	}
	
	/**
	 * Creates a re-usable dialog with a close button in the top right and
	 * appropriate CSS styles. Could extend to make the dialog movable
	 */
	def dialog = { attrs,body ->
		
		def mkp = new groovy.xml.MarkupBuilder(out)
		
		mkp.div(id:attrs.id,'class':'dialog smallDialog',style:'position:absolute;display:none;') {
			div(style:'float:right;') {
				a(	style:'text-decoration:none;',
					href:'#',
					onclick:"\$('${attrs.id}').style.display='none';", "X" )	
			}
			if(attrs.title) {
				div('class':'title',attrs.title)
			}
			out << body()
		}
	}
	
	/**
	 * The below are specific buttons that are only rendered if the write
	 * permissions and page state are available on the session and model
	 */
	 def approveButton = { attrs ->
		 createWorkFlowButton('Approve',attrs)
	 }
	 
	 def rejectButton = { attrs ->
		 createWorkFlowButton('Reject',attrs)
	 }	 
	 
	 def createWorkFlowButton(name,attrs) {
		def role = session.user?.role
		def page = attrs.page
		println "creating workflow button for ${page} and ${role}"
		if(role && page) {
			println role.site == attrs.page.site
			println role.name == Role.CONTENT_APPROVER
			println role.name
			if( (role.name == Role.CONTENT_APPROVER || role.name == Role.SYSTEM_ADMINISTRATOR)
				&& role.site == attrs.page.site) {
				println "checking if there are revs"	
				if(page.revisions) {
					def last = page.revisions.last()
					println "last rev state ${last.state}"
					if(	last.state == Revision.EDITED || 
						last.state == Revision.DELETE_REQUESTED || 
						last.state == Revision.ADDED) {
						def mkp = new groovy.xml.MarkupBuilder(out)
						
						mkp.input(	type:"submit",
									name:"${name}Button",
									id:"${name}Button",
									value:"${name}",
									)												
					}					
				}									
			}								
		}		 
	 }
		
	 /**
	  * A button that undoes the last change
	  */
	 def rollbackButton = { attrs ->
		 def page = attrs.page
		 if(page) {			
			if(page.revisions) {
				def last = page.revisions.last()	
				if(	last.state == Revision.EDITED || 
					last.state == Revision.DELETE_REQUESTED || 
					last.state == Revision.REJECTED) {
						def mkp = new groovy.xml.MarkupBuilder(out)
						
						mkp.input(	type:'submit',
									name:'UndoButton',
									id:'UndoButton',
									value:"Rollback")
				}
			}
		 }		 
	 }
	 
	 def publishButton = { attrs ->
		def page = attrs.page
		if(page) {
			if(page.revisions) {
				def last = page.revisions.last()
				if(last.state == Revision.APPROVED) {
						def mkp = new groovy.xml.MarkupBuilder(out)
						
						mkp.input(	type:'submit',
									name:'PublishButton',
									id:'PublishButton',
									value:"Publish")											
				}
			}
		}
	 }
}
