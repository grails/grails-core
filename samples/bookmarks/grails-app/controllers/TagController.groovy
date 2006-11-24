import org.grails.bookmarks.*
            
class TagController extends SecureController {
    def index = { redirect(action:list,params:params) }

    def list = {
        [ tagList: Tag.list( params ) ]
    }

    def show = {
        [ tag : Tag.get( params.id ) ]
    }

    def delete = {
        def tag = Tag.get( params.id )
        if(tag) {
            tag.delete()
            flash.message = "Tag ${params.id} deleted."
            redirect(action:list)
        }
        else {
            flash.message = "Tag not found with id ${params.id}"
            redirect(action:list)
        }
    }

    def edit = {
        def tag = Tag.get( params.id )

        if(!tag) {
                flash.message = "Tag not found with id ${params.id}"
                redirect(action:list)
        }
        else {
            return [ tag : tag ]
        }
    }

    def update = {
        def tag = Tag.get( params.id )
        if(tag) {
             tag.properties = params
            if(tag.save()) {
                redirect(action:show,id:tag.id)
            }
            else {
                render(view:'edit',model:[tag:tag])
            }
        }
        else {
            flash.message = "Tag not found with id ${params.id}"
            redirect(action:edit,id:params.id)
        }
    }

    def create = {
        def tag = new Tag()
        tag.properties = params
        return ['tag':tag]
    }

    def save = {
        def tag = new Tag()
        tag.properties = params
        if(tag.save()) {
            redirect(action:show,id:tag.id)
        }
        else {
            render(view:'create',model:[tag:tag])
        }
    }

}