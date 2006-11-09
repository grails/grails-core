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
 * Handles the CRUD operations for access keys. An access key allows
 * a user access to the site when registering
 *
 * @author Graeme Rocher
 */
class AccessKeyController {
    def index = { redirect(action:list,params:params) }

    def list = {
        if(!params.max) params.max = 10
        [ accessKeyList: AccessKey.list( params ) ]
    }

    def show = {
        [ accessKey : AccessKey.get( params['id'] ) ]
    }

    def delete = {
        def accessKey = AccessKey.get( params['id'] )
        if(accessKey) {
            accessKey.delete()
            flash['message'] = "AccessKey ${params['id']} deleted."
            redirect(action:list)
        }
        else {
            flash['message'] = "AccessKey not found with id ${params['id']}"
            redirect(action:list)
        }
    }

    def edit = {
        def accessKey = AccessKey.get( params['id'] )

        if(!accessKey) {
                flash['message'] = "AccessKey not found with id ${params['id']}"
                redirect(action:list)
        }
        else {
            return [ accessKey : accessKey ]
        }
    }

    def update = {
        def accessKey = AccessKey.get( params['id'] )
        if(accessKey) {
             accessKey.properties = params
            if(accessKey.save()) {
                redirect(action:show,id:accessKey.id)
            }
            else {
                render(view:'edit',model:[accessKey:accessKey])
            }
        }
        else {
            flash['message'] = "AccessKey not found with id ${params['id']}"
            redirect(action:edit,id:params['id'])
        }
    }

    def create = {
        def accessKey = new AccessKey()
        accessKey.properties = params
        return ['accessKey':accessKey]
    }

    def save = {
        def accessKey = new AccessKey()
        accessKey.properties = params
		if(AccessKey.findByCode(accessKey.code)) {
			flash.message = "Access key already exists for code ${accessKey.code}"
			redirect(action:'create',model:['accessKey':accessKey])
		}
        if(accessKey.save()) {
            redirect(action:show,id:accessKey.id)
        }
        else {
            render(view:'create',model:[accessKey:accessKey])
        }
    }

}