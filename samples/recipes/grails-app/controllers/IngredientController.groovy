
import com.recipes.Ingredient            
class IngredientController {
    @Property index = { redirect(action:list,params:params) }

    @Property list = {
        [ ingredientList: Ingredient.list( params ) ]
    }

    @Property show = {
        [ ingredient : Ingredient.get( params.id ) ]
    }

    @Property delete = {
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

    @Property edit = {
        def ingredient = Ingredient.get( params.id )

        if(!ingredient) {
                flash.message = "Ingredient not found with id ${params.id}"
                redirect(action:list)
        }
        else {
            return [ ingredient : ingredient ]
        }
    }

    @Property update = {
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

    @Property create = {
        def ingredient = new Ingredient()
        ingredient.properties = params
        return ['ingredient':ingredient]
    }

    @Property save = {
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