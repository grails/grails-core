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
 * Handles the CRUD operations for messages. A message is created
 * within a topic
 *
 * @author Graeme Rocher
 */
class MessageController extends BaseController {
    @Property index = { redirect(action:list,params:params) }
	@Property Map levels
	@Property Page page
	@Property Page forum
	
	@Property afterInterceptor = { model ->
		def forum = null
		if(params.'forum.id' || flash.'forum.id') {
			flash.'forum.id' = flash.'forum.id'
			def forumId = (params.'forum.id'?params.'forum.id':flash.'forum.id')
			forum = Page.get(forumId)
			if(forum) {
				def calcLevels = ApplicationConfig.CALCULATE_LEVELS
				model.levels  = (flash.admin? null : calcLevels(forum))
			}
		}		
		if(!forum && params.id) {
			def message = Message.get(params.id)
			forum = message?.topic?.forum
			if(forum) {
				def calcLevels = ApplicationConfig.CALCULATE_LEVELS
				model.levels  = (flash.admin? null : calcLevels(forum))				
			}
		}		
		if(forum) {
				model.page = forum	
				model.forum = forum
				flash.forum = forum			
		}
	}
	

    @Property delete = {
        def message = Message.get( params['id'] )
        if(message) {
			def topic = message.topic
			flash.'forum.id' = topic.forum.id
            message.delete()
            flash.message = "Message ${params['id']} deleted."
            redirect(controller:'topic',action:'show',id:topic.id)
        }
        else {
            flash['message'] = "Message not found with id ${params['id']}"
            redirect(action:list)
        }
    }

    @Property edit = {
        def message = Message.get( params['id'] )

        if(!message) {
                flash['message'] = "Message not found with id ${params['id']}"
                redirect(action:list)
        }
        else {
            return [ message : message ]
        }
    }

    @Property update = {
        def message = Message.get( params['id'] )
        if(message) {
             message.properties = params
            if(message.save()) {
                redirect(controller:'topic',action:'show',id:message.topic.id)
            }
            else {
                render(view:'edit',model:[message:message])
            }
        }
    }

    @Property create = {
		if(!flash.'topic.id' || !flash.'forum.id') {
			redirect(controller:'page')			
		}
		else {
			def topic = Topic.get(flash.'topic.id')
			def posts = Message.findAllByTopic(topic,[sort:'datePosted'])
			def lastPost = null
			if(posts) {
				lastPost = posts[posts.size()-1]	
			}
			if(!topic) {
				redirect(controller:'page')				
			}
			else {
				def message = new Message()
				
				message.properties = params
				message.properties = flash
				return ['message':message,lastPost:lastPost, topic:topic]				
			}			
		}			
    }

    @Property save = {
        def message = new Message()
        message.properties = params
		message.by = session.user
        if(message.save()) {
            redirect(controller:'topic',action:'show',id:params.'topic.id')
        }
        else {
			def t = Topic.get(params.'topic.id')
            render(view:'create',model:[message:message,topic:t])
        }
    }

}