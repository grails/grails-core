import org.grails.bookmarks.*
import groovy.mock.interceptor.StubFor

class SubscriptionTests extends GroovyTestCase {

	void testExecute() {
		
		// create a user
		def userA = new User(login:"graeme",password:"graeme",firstName:"Graeme",lastName:"Rocher",email:"graeme@grails.org").save()
		def t = new Tag(name:"grails").save()
		def s = new Subscription(user:userA,tag:t).save()
		
		// create another user
		def userB = new User(login:"other",password:"other",firstName:"other",lastName:"user",email:"other@grails.org").save()
		// create a new bookmark tagged with Grails.
		def b = new Bookmark(title:"Grails Download Page", url:new URI("http://grails.org/Download"),notes:"",user:userB)
		b.addTagReference(new TagReference(bookmark:b,user:userB,tag:t))
		b.save()

		

		// now test the correct code is called for an email to be sent
		def emailStub = new groovy.mock.interceptor.StubFor(org.apache.commons.mail.HtmlEmail)
		emailStub.demand.setFrom { String addr, String name -> 
			println("Checking from address")
			assert addr == "noreply@bookmarks.grail.org"
			assert name == "Grails Bookmarks Application"
		}
		emailStub.demand.addTo {  String addr, String name ->
			println("Checking to address")			
			assert addr == userA.email
			assert name == userA.toString()
		}
		emailStub.demand.send {
			return true
		}
		
		def job = new SubscriptionJob()
		emailStub.use {
			job.execute()
		}
		// test the contents of the email
		def sendMailStub = new StubFor(SubscriptionJob)
		sendMailStub.demand.sendMail { user, subject,bodyContent ->
			println("Checking generated email")
			
			def sw = new StringWriter()
			def mkp = new groovy.xml.MarkupBuilder(new PrintWriter(sw))
			mkp.html {
				body(bodyContent)
			}
			assert sw.toString().indexOf("<a href=\"http://grails.org/Download\">Grails Download Page</a>")
		}
		sendMailStub.use {
			job.execute()
		}		

	}
}
