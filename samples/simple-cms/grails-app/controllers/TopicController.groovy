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
 * A controller that handles CRUD operations for the manipulation of Topics
 * within a Forum
 *
 * @author Graeme Rocher
 * @since 10-May-2006
 */
class TopicController extends BaseController {
	@Property Map levels
	@Property Page page
	@Property Page forum
	
	def afterInterceptor = { model ->
		def forum = null
		if(params.'forum.id' || flash.'forum.id') {
			def forumId = (params.'forum.id'?params.'forum.id':flash.'forum.id')
			forum = Page.get(forumId)
			if(forum) {
				def calcLevels = ApplicationConfig.CALCULATE_LEVELS
				model.levels  = (flash.admin? null : calcLevels(forum))
			}
		}	
		
		if(!forum && params.id) {
			def topic = Topic.get(params.id)
			forum = topic?.forum
			if(forum) {
				def calcLevels = ApplicationConfig.CALCULATE_LEVELS
				model.levels  = (flash.admin? null : calcLevels(topic.forum))				
			}
		}		

		if(forum) {
				model.page = forum	
				model.forum = forum
				flash.forum = forum			
		}		
	}
	
    def index = { redirect(controller:'page') }


    def show = {
		def topic = Topic.get( params.id )
		if(!topic) {
			flash.message = "Topic not found for id ${params.id}"
			redirect(controller:'forum',action:'show',id:params.'forum.id')
		}
		else {
			def max = (params.max ? params.max.toInteger() : 10)
			def offset = (params.offset ? params.offset.toInteger() : 0)
			def posts = Message.findAllByTopic(topic,[max:max,offset:offset,sort:'datePosted'])
			
			// this is sub-optimal until I implement countBy
			def total = Message.findAllByTopic(topic).size()
			
			return [  forum:topic.forum,
					  topic:topic,
					  posts:posts,
					  total:total]
		}        
    }

	def addPost = {
		def topic = Topic.get(params.id)
		flash.'forum.id' = topic?.forum?.id
		flash.'topic.id' = topic?.id
		redirect(controller:'message',action:'create')	
	}
	
    def delete = {
        def topic = Topic.get( params['id'] )
		def forum = topic.forum
        if(topic) {
            topic.delete()
            flash['message'] = "Topic ${params['id']} deleted."
            redirect(controller:'forum',action:'show',id:forum.id)
        }
        else {
            flash['message'] = "Topic not found with id ${params['id']}"
            redirect(controller:'forum',action:'show',id:forum.id)
        }
    }

    def edit = {
        def topic = Topic.get( params.id )

        if(!topic) {
                flash['message'] = "Topic not found with id ${params['id']}"
                redirect(action:list)
        }
        else {
            return [ topic : topic ]
        }
    }

    def update = {
        def topic = Topic.get( params['id'] )
        if(topic) {
             topic.properties = params
            if(topic.save()) {
                redirect(action:show,id:topic.id)
            }
            else {
                render(view:'edit',model:[topic:topic])
            }
        }
        else {
            flash['message'] = "Topic not found with id ${params['id']}"
            redirect(action:edit,id:params['id'])
        }
    }

    def create = {
        def topic = new Topic()
        topic.properties = params
        return ['topic':topic]
    }

    def save = {
        def topic = new Topic()
        topic.properties = params
		topic.createdBy = session.user
        if(topic.save()) {
            redirect(controller:'forum',action:'show',id:topic.forum.id)
        }
        else {
            render(view:'create',model:[topic:topic])
        }
    }

}