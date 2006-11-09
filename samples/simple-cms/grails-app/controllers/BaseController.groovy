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
 * Abstract base controller that defines a before interceptor to deal
 * with authentication
 *
 * @author Graeme Rocher
 */
abstract class BaseController {
	def beforeInterceptor = this.&auth
	
	static def EXCLUDES = ['remind','password','admin','login','handleLogin','register','handleRegistration','terms']
	static def GENERAL_ALLOWED = 	[
											page: ['show','index'],
											message: ['create','save','edit','update'],
											forum: ['show'],
											topic: ['show','addPost','create','save'],
											test: ['show','submit'],
											user: ['profile','contact','send','network','search']
										]
	def auth() {	
		if(!session.site) {
			def s = Site.findByDomain(request.serverName)
			if(!s) s= Site.findByName(Site.DEFAULT)
			session.site = s
		}	
		if(!EXCLUDES.contains(actionName)) {
			if(!session.user) {
				redirect(controller:'user',action:'login')
				return false
			}
			else {
				def role = session.user.role
				if(role.name == Role.GENERAL_USER) {
					println "Checking allowed to access $controllerName and $actionName"
					if(!GENERAL_ALLOWED[controllerName]) {
							redirect(controller:'page')
							return false
					}	
					else if(!GENERAL_ALLOWED[controllerName].contains(actionName)) {
							redirect(controller:'page')
							return false						
					}
				}
			}
		}
	}
	
	def testSysAdmin() {
		def u = User.findByLogin('admin')
		session.user = u
	}
	
	def testEditor() {
			def u = User.findByLogin('contentedt')
			if(!u) {
				u = new User( title: 'Mr',
								  firstName: 'Content',
								  lastName: 'Editor',
								  email: 'graeme.rocher@gmail.com',
								  company: 'None',
								  login: 'contentedt',
								  pwd: 'letmein',
								  )
				
				u.role = Role.findByName(Role.CONTENT_EDITOR)
				u.role.site = Site.findByName('default')
				u.save()				
			}
			
			session.user = u			
	}
	def testApprover() {
			def u = User.findByLogin('contentapp')
			if(!u) {
				u = new User( title: 'Mr',
								  firstName: 'Content',
								  lastName: 'Approver',
								  email: 'graeme.rocher@gmail.com',
								  company: 'None',
								  login: 'contentapp',
								  pwd: 'letmein',
								  )
				
				u.role = Role.findByName(Role.CONTENT_APPROVER)
				u.role.site = Site.findByName('default')
				u.save()				
			}
			
			session.user = u			
	}	
}

