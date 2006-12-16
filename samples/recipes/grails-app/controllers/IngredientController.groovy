
import com.recipes.Ingredient            
class IngredientController {
    def index = { redirect(action:list,params:params) }

    def list = {
        [ ingredientList: Ingredient.list( params ) ]
    }

    def show = {
        [ ingredient : Ingredient.get( params.id ) ]
    }

    def delete = {
        def ingredient = Ingredient.get( params.id )
        if(ingredient) {
            ingredient.delete()
            flash.message = "Ingredient ${params.id} deleted."
            redirect(action:list)
        }
        else {
            flash.message = "Ingredient not found with id ${params.id}"
            redirect(action:list)
        }
    }

    def edit = {
        def ingredient = Ingredient.get( params.id )

        if(!ingredient) {
                flash.message = "Ingredient not found with id ${params.id}"
                redirect(action:list)
        }
        else {
            return [ ingredient : ingredient ]
        }
    }

    def update = {
        def ingredient = Ingredient.get( params.id )
        if(ingredient) {
             ingredient.properties = params
            if(ingredient.save()) {
                redirect(action:show,id:ingredient.id)
            }
            else {
                render(view:'edit',model:[ingredient:ingredient])
            }
        }
        else {
            flash['message'] = "Ingredient not found with id ${params.id}"
            redirect(action:edit,id:params.id)
        }
    }

    def create = {
        def ingredient = new Ingredient()
        ingredient.properties = params
        return ['ingredient':ingredient]
    }

    def save = {
        def ingredient = new Ingredient()
        ingredient.properties = params
        if(ingredient.save()) {
            redirect(action:show,id:ingredient.id)
        }
        else {
            render(view:'create',model:[ingredient:ingredient])
        }
    }

}