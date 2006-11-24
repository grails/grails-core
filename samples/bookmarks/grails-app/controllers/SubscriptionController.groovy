import org.grails.bookmarks.*

class SubscriptionController extends SecureController {
    def list = {
        [ subscriptionList: Subscription.findAllByUser( session.user,params ) ]
    }
    def delete = {
        def subscription = Subscription.get( params.id )
        subscription?.delete()
		render(template:"subscription", var:"subscription",collection:Subscription.findAllByUser( session.user ))
    }
    def save = {
		def t = Tag.findByName(params.tagName)
		if(!t)t= new Tag(name:params.tagName).save()
		if(!Subscription.findByUserAndTag(session.user,t))
			new Subscription(user:session.user, tag:t).save()
     	render(template:"subscription", var:"subscription",collection:Subscription.findAllByUser( session.user ))
    }
}