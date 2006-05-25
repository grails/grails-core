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
 * A controller that handles CRUD operations for Roles
 *
 * @author Graeme Rocher
 * @since 10-May-2006
 */
class RoleController {
    @Property index = { redirect(action:list,params:params) }

    @Property list = {
        if(!params['max']) params['max'] = 10
        [ roleList: Role.list( params ) ]
    }

    @Property show = {
        [ role : Role.get( params['id'] ) ]
    }

    @Property delete = {
        def role = Role.get( params['id'] )
        if(role) {
            role.delete()
            flash['message'] = "Role ${params['id']} deleted."
            redirect(action:list)
        }
        else {
            flash['message'] = "Role not found with id ${params['id']}"
            redirect(action:list)
        }
    }

    @Property edit = {
        def role = Role.get( params['id'] )

        if(!role) {
                flash['message'] = "Role not found with id ${params['id']}"
                redirect(action:list)
        }
        else {
            return [ role : role ]
        }
    }

    @Property update = {
        def role = Role.get( params['id'] )
        if(role) {
             role.properties = params
            if(role.save()) {
                redirect(action:show,id:role.id)
            }
            else {
                render(view:'edit',model:[role:role])
            }
        }
        else {
            flash['message'] = "Role not found with id ${params['id']}"
            redirect(action:edit,id:params['id'])
        }
    }

    @Property create = {
        def role = new Role()
        role.properties = params
        return ['role':role]
    }

    @Property save = {
        def role = new Role()
        role.properties = params
        if(role.save()) {
            redirect(action:show,id:role.id)
        }
        else {
            render(view:'create',model:[role:role])
        }
    }

}