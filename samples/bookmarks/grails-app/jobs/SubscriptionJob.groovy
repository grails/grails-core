import org.grails.bookmarks.*

class SubscriptionJob {
	static final EMAIL_SERVER = "localhost"
	
	def cronExpression = "0 0 6 ? * FRI"

	def execute() {	
		def criteria = Subscription.createCriteria()
		def subscribedUsers = criteria.list {
			projections {
				distinct("user")
			}
		}
		subscribedUsers.each { user ->
			def subscribedTags = Subscription.findAllByUser(user).tag
			
			criteria = Bookmark.createCriteria()
			def now = new Date()
			def bookmarks = criteria.list {
						notEqual("user", user)
						between("dateCreated",now-7,now)
						tags {
							inList("tag", subscribedTags)
						}					
			}
			if(bookmarks) {
				sendMail(user,"Latest Bookmarks") {
					p("Hi $user!")
					p("Below are the most recent bookmarks for the tags you're interested in!")
					for(b in bookmarks) {
						p {
							a(href:b.url, b.title)
						}
					}
					p("Enjoy the bookmarks!")
					p("The Bookmarks Team")
				}									
			}
		}
	}
	
	def sendMail(User to,String subject,Closure bodyContent) {
		def sw = new StringWriter()
		def mkp = new groovy.xml.MarkupBuilder(new PrintWriter(sw))
		mkp.html {
			body(bodyContent)
		}
		
		def email = new org.apache.commons.mail.HtmlEmail(
		   hostName: EMAIL_SERVER,
		   subject : subject,
		   htmlMsg     : sw.toString()
		)
		email.setFrom("noreply@bookmarks.grail.org" , "Grails Bookmarks Application")
		email.addTo(to.email, "$to")
		println("Sending email: $sw")
		email.send()		
	}
}
