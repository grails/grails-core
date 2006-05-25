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
 * Handles the CRUD operations for forums. A forum is a Page of type Page.FORUM
 * that has 0-* Topics
 *
 * @author Graeme Rocher
 */
class ForumController extends BaseController {

	@Property index = { 
		redirect(action:list)
	}
	
	@Property list = {
		[forums: Page.findAllByType(Page.FORUM) ]	
	}
	
	@Property show = {
		def forum = Page.get(params.id)
		def max = (params.max ? params.max.toInteger() : 10)
		def offset = (params.offset ? params.offset.toInteger() : 0)
		def calcLevels = ApplicationConfig.CALCULATE_LEVELS
		def levels = (flash.admin? null : calcLevels(forum))
		
		if(!forum) {
			flash.message = "Forum ${params.id} not found"
			redirect(action:list)			
		}
		else if(forum.type != Page.FORUM) {
			flash.message = "Page ${params.id} is not a forum"
			redirect(action:list)
		}
		else {
			def topics = Topic.findAllByForum(forum,[max:max,sort:'dateAdded',offset:offset])
			flash.levels = levels
			return [forum:forum,topics:topics,levels:levels]	
		}
	}

}

