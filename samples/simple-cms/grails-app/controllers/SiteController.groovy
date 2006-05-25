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
 * A controller that manages site instances
 *
 * @author Graeme Rocher
 * @since 10-May-2006
 */
class SiteController extends BaseController {
    @Property index = {
        redirect(action:list)
	}
	
	@Property admin = { 
		flash.admin = true 
		redirect(controller:'user',action:'login')
	}
	
    @Property loadSubPages = {
    	def pages
    	if(params.id) {
    		def parent = Page.get(params.id)
    		if(parent)
				pages = Page.findAllByParent(parent)
								.findAll {
					if(!it.revisions) 
						return true
					else
						return it.revisions?.last().state != Revision.DELETED					
				}
    	}
    	render(template:'subPageJSON',model:[pages: pages.sort{ it.position }  ])
    }

	@Property displayPage = {
		if(params.id) {
			def p = Page.get(params.id)
			if(p) { 
				render(template:'/pagexml',model:[page:p])
			}
		}
	}
	
    @Property list = {
        if(!params.max) params.max = 10
        [ siteList: Site.list( params ) ]
    }

    @Property show = {
    	def s = Site.get( params.id )
        [ site : s ]
    }

    @Property delete = {
        def site = Site.get( params['id'] )
        if(site) {
            site.delete()
            flash['message'] = "Site ${params['id']} deleted."
            redirect(action:list)
        }
        else {
            flash['message'] = "Site not found with id ${params['id']}"
            redirect(action:list)
        }
    }

    @Property edit = {
        def site = Site.get( params['id'] )

        if(!site) {
                flash['message'] = "Site not found with id ${params['id']}"
                redirect(action:list)
        }
        else {
            return [ site : site ]
        }
    }

    @Property update = {
        def site = Site.get( params['id'] )
        if(site) {
             site.properties = params
            if(site.save()) {
                redirect(action:show,id:site.id)
            }
            else {
                render(view:'edit',model:[site:site])
            }
        }
        else {
            flash['message'] = "Site not found with id ${params['id']}"
            redirect(action:edit,id:params['id'])
        }
    }

    @Property create = {
        def site = new Site()
        site.properties = params
        return ['site':site]
    }

    @Property save = {
        def site = new Site()
        site.properties = params
        
        def homePage = new Page( site:site,
        						 template:'default')
        site.homePage = homePage
        
        if(site.validate()) {
            homePage.title = "${site.name} Home Page"
            homePage.content = 'Initial Text: To be edited'
            site.homePage = homePage
            site.save()
            redirect(action:show,id:site.id)            
        }
        else {
            render(view:'create',model:[site:site])
        }
    }

}