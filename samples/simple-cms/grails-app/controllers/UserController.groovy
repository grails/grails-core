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
 * A controller that handles login, registration and management of
 * users within the system
 *
 * @author Graeme Rocher
 * @since 10-May-2006
 */
class UserController extends BaseController {
	
	def Page page
	def Map levels
	
	def afterInterceptor = { model ->
		// check for a link
		if(session.user) {
			def link = null
			if(flash.url && (actionName == 'profile' && !params.id)) {
				link = Page.findByContentAndSite( "${request.contextPath}${actionUri}".toString(), session.site )				
			}
			else if(flash.url) {
				link = Page.findByContentAndSite( flash.url.toString(), session.site )
			}
			else {
				link = Page.findByContentAndSite( "${request.contextPath}${actionUri}".toString(), session.site )
			}
			
			if(link) {
				model.page = link
				def calcLevels = ApplicationConfig.CALCULATE_LEVELS
				model.levels  = calcLevels(link)			
			}			
		}		
	}
	
    def index = {
        redirect(controller:'page')
    }

	def register = {
		def regUri = getViewUri("register_${session.site.domain}")
		if(servletContext.getResource(regUri)) {
			render(view:"register_${session.site.domain}")
		}		
	}
	
	def contact = {
		if(!params.id) {
			redirect(controller:'page')	
		}
		else {
			return [ contactUser : User.get(params.id) ]
		}
	}
	
	def editProfile = {
		if(!params.id) {
			redirect(controller:'page')	
		}
		else {
			if(session.user.id == params.id.toInteger()) {
				return [user:session.user]
			}
			else {
				redirect(controller:'page')				
			}
		}
	}
	
    def updateProfile = {
		if(!params.id) {
			redirect(controller:'page')	
		}
		else {
			def user = User.get( params.id )
			if(user) {
				 user.properties = params
				if(user.save()) {
					session.user = user
					redirect(action:profile,id:user.id)
				}
				else {
					render(view:'editProfile',model:[user:user])
				}
			}
			else {
				redirect(controller:'page')
			}			
		}
    }	
	
	def send = {
		if(!params.id) {
			redirect(controller:'page')	
		}
		else if(!params.msg) {
			flash.message = "You must enter the body of the message"
			flash.subject = params.subject
			redirect( action: contact, id:params.id)
		}
		else if(!params.subject) {
			flash.message = "You must enter the subject of the message"
			flash.msg = params.msg	
			redirect( action: contact, id:params.id)			
		}
		else {
			def to = User.get(params.id)
			if(!to) {
				flash.message = "User not found on system"
				redirect( action: contact, id:params.id)		
			}
			else {
				Notifications.sendHtmlNotification(	to,
														"${session.site?.name}: ${params.subject}",
														"<html><body><p>A message from a user of the ${session.site?.name} site:</p> ${params.msg}</body></html>" )
				flash.message = "Your message has been sent to $to"
				redirect( action:contact, id:params.id)
			}
		}
	}
	
	def remind = { }
	
	def password = {
		if(!params.email) {
			flash.message = "You need to specify the email with which you registered"
			redirect(action:remind)
		}
		else {
			def u = User.findByEmail(params.email)
			if(!u) {
				flash.message = "No user found for specified email address"
				redirect(action:remind)
				
			}
			else {
				flash.message = "You password has been retrieved and sent to your email account. Please check your email to retrieve your password"
				redirect(action:remind)				
				Notifications.sendNotification(u,"Password Reminder", """
					Dear $u,
					
					You password for the ${u.role.site} is ${u.pwd}.
					
					Kind Regards,
					System Support
					${u.role.site}
				""")
			}
		}
	}
	
	def handleRegistration = {
		def u = new User()
		u.properties = params
			
		if(!params.key) {
			flash.message = "Access key not specified"
			render(view:'register',model:[user:u])
		}	
		else if(!params.terms) {
			flash.message = "You have not agreed to the Terms & Conditions"
			render(view:'register',model:[user:u])
		}
		else if(u.pwd != params.confirm) {
			flash.message = "The passwords you entered do not match"
			render(view:'register',model:[user:u])			
		}
		else {
			flash.key = params.key
			flash.terms = params.terms
			def ak = AccessKey.findByCode(params.key)
			if(!ak) {
				flash.message = "Access key <b>${params.code}</b> does not exist"
				render(view:'register',model:[user:u])				
			}
			else if(ak.expiryDate?.before(new Date())) {
				
				flash.message = "The specified access key has expired"
				render(view:'register',model:[user:u])
			}
			else {
				if(ak.usages > 0) {
					ak.usages = ak.usages - 1
				
					u.accessKey = ak
					u.role = ak.role
					if(User.findByLogin(params.login)) {
						flash.message = "User already exists for login ${u.email}"
						render(view:'register',model:[user:u])
					}
					else if(User.findByEmail(u.email)) {
						flash.message = "User already exists for email ${u.email}"
						render(view:'register',model:[user:u])				
					}											
					if(u.save()) {
						session.user = u
						ak.save()
						redirect(controller:'page')
					}
					else {
						render(view:'register',model:[user:u])		
					}					
				}
				else {
					flash.message = "The specified access key does not have any uses left"
					render(view:'register',model:[user:u])					
				}
			}				
		}
	}
	
    def login = { 
		if(flash.admin) {
			flash.admin = true
			if(session.user) {
				redirect(controller:'site',action:'list')	
			}
		}
		else if(session.user){
			redirect(controller:'page',action:'show',id:session.site.homePage.id)	
		}		
	}

    def handleLogin = {
		if(flash.admin)flash.admin=true
        if(params.login && params.pwd) {
            def u = User.findByLogin(params.login)
            if(u) {
				if(!u.active) {
					flash.message = "Your account is no longer active"
					redirect(action:login)					
				}
               else if(u.pwd == params.pwd) {
					def now = new Date()
					if(u.accessKey?.endDate?.before(now)) {
						flash.message = "Your account has expired"
						redirect(action:login)						
					}
					else if(u.accessKey?.startDate?.after(now)) {
						flash.message = "Your account has not been activited yet"	
					}
					else {
					   session.user = u
					   session.site = (u.role?.site ? u.role.site : Site.findByName(Site.DEFAULT))
					   if(flash.admin) {
							redirect(controller:'site',action:'list')                   
					   }
					   else {
						   redirect(controller:'page',action:'show',id:u.role.site.homePage.id)                   
					   }						
					}
                }
                else {
                    flash.message = "Incorrect password for login '${params.login}'"
                    render(view:'login')
                }
            }
            else {
                flash.message = "User not found for login '${params.login}'"
                render(view:'login')
            }
        }
        else {
            flash.message = 'Login and/or Password not specified'
            render(view:'login')
        }
    }

	def profile = {
		if(!params.id) {
			return [ user: session.user ]	
		}
		else {
			flash.url = "${request.contextPath}/user/network"
			return [ user : User.get( params.id ) ]
		}
	}
	
    def list = {
        if(!params['max']) params['max'] = 10
        [ userList: User.list( params ) ]
    }

    def show = {
        return [ user : User.get( params['id'] ) ]
    }

/*     def delete = {
        def user = User.get( params['id'] )
        if(user) {
            user.delete()
            flash['message'] = "User ${params['id']} deleted."
            redirect(action:list)
        }
        else {
            flash['message'] = "User not found with id ${params['id']}"
            redirect(action:list)
        }
    } */

    def edit = {
        def user = User.get( params['id'] )

        if(!user) {
                flash['message'] = "User not found with id ${params['id']}"
                redirect(action:list)
        }
        else {
            return [ user : user ]
        }
    }

    def update = {
        def user = User.get( params['id'] )
        if(user) {
             user.properties = params
            if(user.save()) {
                redirect(action:show,id:user.id)
            }
            else {
                render(view:'edit',model:[user:user])
            }
        }
        else {
            flash['message'] = "User not found with id ${params['id']}"
            redirect(action:edit,id:params['id'])
        }
    }

    def create = {
        def user = new User()
        user.properties = params
        return ['user':user]
    }

    def save = {
        def user = new User()
        user.properties = params
        if(user.save()) {
            redirect(action:show,id:user.id)
        }
        else {
            render(view:'create',model:[user:user]) 
        }
    }

}