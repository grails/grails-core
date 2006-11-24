import org.grails.bookmarks.*

class UserController {
	
	def accessDenied = {
		flash.message = "Your account does not have permission to view this page"
		redirect(controller:"bookmark")
	}
		
	def login = {
		if(session.user) {
			redirect(controller:'bookmark')
		}
	}
	
	def register = {}
	
	def handleRegistration = {
		def user = new User()
		if(params.password != params.confirm) {
			flash.message = "The two passwords you entered dont match!"
			redirect(action:register)
		}
		else {
			user.properties = params
			if(user.save()) {       
				redirect(controller:'bookmark')
			}
			else {
				flash.user = user
				redirect(action:register)
			}			
		}
	}
}

