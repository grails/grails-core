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
 * A class that defines static methods for sending email
 *
 * @author Graeme Rocher
 * @since 10-May-2006
 */
import org.apache.commons.mail.SimpleEmail
import org.apache.commons.mail.HtmlEmail

class Notifications {


	
	static def sendNotification(from,to,subject,message) {
		def email = new SimpleEmail(
		   hostName: ApplicationConfig.EMAIL_SERVER,
		   subject : subject,
		   msg     : message
		)
		email.setFrom(from.email , from.toString())
		email.addTo(to.email, to.toString())
		email.send()		
	}
	
	static def sendNotification(to,subject,message) {
		def email = new SimpleEmail(
		   hostName: ApplicationConfig.EMAIL_SERVER,
		   subject : subject,
		   msg     : message
		)
		email.setFrom(ApplicationConfig.SYSTEM_EMAIL , ApplicationConfig.SYSTEM_NAME)
		email.addTo(to.email, to.toString())
		email.send()		
	}

	static def sendHtmlNotification(from,to,subject,message) {
		def email = new HtmlEmail(
		   hostName: ApplicationConfig.EMAIL_SERVER,
		   subject : subject,
		   htmlMsg     : message,
		   textMsg : message
		)
		email.setFrom(from.email , from.toString())
		email.addTo(to.email, to.toString())
		email.send()			
	}
	
	static def sendHtmlNotification(to,subject,message) {
		def email = new HtmlEmail(
		   hostName: ApplicationConfig.EMAIL_SERVER,
		   subject : subject,
		   htmlMsg     : message,
		   textMsg : message
		)
		email.setFrom(ApplicationConfig.SYSTEM_EMAIL , ApplicationConfig.SYSTEM_NAME)
		email.addTo(to.email, to.toString())
		email.send()			
	}	
}
