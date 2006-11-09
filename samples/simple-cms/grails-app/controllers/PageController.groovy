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
 * Core controller than handles manipulating of pages within the site
 * structure
 *
 * @author Graeme Rocher
 * @since 10-May-2006
 */
class PageController extends BaseController {
    def index = { 
		redirect(action:show,id:session.site.homePage.id) 
	}

	def terms = {
		def url = getTemplateUri("pages/${session.site.domain}_terms")
		println "retrieving terms for url $url"
		if(servletContext.getResource(url)) {
			render(template:"pages/${session.site.domain}-terms",model:[:])
		}		
		else {
			// default terms
			render(template:"templates/default-terms",model:[:])
		}
	}
	
	def preview = {
		if(!params.id) {
			render "Page id not specified"	
		}
		else {
			def p = Page.get(params.id)	
			def calcLevels = ApplicationConfig.CALCULATE_LEVELS
			def levels = calcLevels(p)

			return [page:p,levels:levels,site:p.site]			
		}
	}
	
    def show = {
	    if(params.id) {
            def p = Page.get(params.id)
			def criteria = ApplicationConfig.PAGE_DISPLAY_CRITERIA
			if(!p) {
				response.sendError(404)
			}
			else if(criteria(p)) {

				def calcLevels = ApplicationConfig.CALCULATE_LEVELS
				def levels = calcLevels(p)
				if(p.type == Page.FORUM) {
					flash.levels = levels
					redirect(controller:'forum',action:'show',id:p.id)	
					return
				}
				else if(p.type == Page.QUESTIONNAIRE) {
					flash.levels = levels
					redirect(controller:'test',action:'show',id:p.id)	
					return					
				}
				else {
					if(p.type == Page.LINK) {
						if(p.content ==~ /\d+/) {
							def linked = Page.get(p.content)
							if(linked && criteria(linked)) {
								render(template:"pages/${linked.id}",model:[	page:p,
																				levels:levels,
																				site:p.site] )
							}
							else {
								response.sendError(404)
							}
						}
						else {
							if(p.content.startsWith('http://')) {
								redirect(url:p.content)
							}
							else {
								redirect(url:"http://${p.content}")
							}
						}
					}
					else {
						def url = servletContext.getResource(getTemplateUri("pages/${p.id}"))
						if(url) {
							render(template:"pages/${p.id}",model:[page:p,levels:levels,site:p.site])
						}
						else {
							response.sendError(404)
						}
					}					
				}							
			}
			else {
				response.sendError(404)
			}
	    }
    }

    /* def edit = {
        def page = Page.get( params['id'] )

        if(!page) {
                flash['message'] = "Page not found with id ${params['id']}"
                redirect(action:list)
        }
        else {
            return [ page : page ]
        }
    } */

	def addLink = {
		if(params.'parent.id' && params.'site.id') {
			def parent = Page.get(params.'parent.id')
			def site = Site.get(params.'site.id')
			
			if(!parent) {
				flash.message "Unable to create link. Parent not found for id ${params.'parent.id'}"
			}
			else if(!site) {
				flash.message "Unable to create link. Site not found for id ${params.'site.id'}"
			}
			else {
				return [parent:parent,site:site]
			}
		}
		else {
			flash.message = "Unable to create link. No parent or site specified"	
		}
	}
	
	def saveLink = {
		if(params.'parent.id' && params.'link.id' && params.linkText) {
			def parent = Page.get(params.'parent.id')
			def linkId = params.'link.id'
			def link = null
			def linkPage = null
			if(linkId ==~ /\d+/) {
				linkPage = Page.get(linkId)
				if(linkPage) {
					link = linkPage.id	
				}
				else {
					link = linkId
				}
			}
			else {
				link = linkId	
			}
			
			
			if(!parent) {
				renderError "Parent page not found for id ${params.'parent.id'}"
				return
			}
			else if(linkPage && linkPage.type == Page.LINK) {
				renderError "Cannot add link: It is not possible to link to another link"
				return				
			}
			else {
				def title = params.linkText.replaceAll(/&/,'&amp;')
				def existing = Page.findByTitleAndParent(title,parent)
				if(existing?.revisions) {
					def last = existing.revisions.last()
					// if there is an existing page that has been deleted
					// restore it	
					if(last.state == Revision.DELETED) {
						existing.properties = params
						last.state = Revision.ADDED
						last.updatedBy = session.user
						existing.title = title
						existing.type = Page.LINK
						existing.content = link
						existing.save()
						
						render(template:"/pagexml",model:[page:existing])				
					}
					else {
						renderError("Page already exists with specified title")
						return				
					}
				}				
				else if(existing) {
					renderError "Cannot add link, page already exists with title ${params.linkText}"
					return					
				}
				else {
					def p = new Page(	type:Page.LINK,
										title:title,
										site:parent.site,
										content:link,
										createdBy:session.user)
										
					p.addRevision(new Revision( 	content: params.editor,
													updatedBy: session.user,
													state: Revision.ADDED ))
					parent.addPage(p)
					if(p.save()) {
						render(template:"/pagexml",model:[page:p,alert:"Link created."])	
					}
					else {
						renderError( "Unable to save link:" + p.errors.allErrors.defaultMessage.join(',') )
						return
					}					
				}										
			}
		}
		else {
			renderError "Cannot create link. Missing required parameters"
			return
		}
	}
	
	
	def publish = {
		if(!params.id) {
			renderError "Page id not specified"
			return			
		}
		// get page
		def p = Page.get(params.id)
		if(!p) {
			renderError "Page not found for id ${params.id}"	
		}
		
		// get GSP engine
		//def engine = grailsAttributes.getPagesTemplateEngine()
		def engine = new groovy.text.SimpleTemplateEngine()
		
		// calculate template uri
		def uri = getTemplateUri("templates/${p.site.domain}")
		
		if(!servletContext.getResource(uri)) {
			uri = getTemplateUri("templates/default")
		}
		
		// create template for uri
        def t = null		
		if(!servletContext.getAttribute("${p.site.domain}-template")) {
			t = engine.createTemplate(  servletContext.getResource(uri) )
			servletContext.setAttribute("${p.site.domain}-template",t)
		}
		else {
			t = servletContext.getAttribute("${p.site.domain}-template")	
		}
		// publish page
		def pageUri = getTemplateUri("pages/${p.id}")
		 
		new File(servletContext.getRealPath(pageUri)).withWriter { w ->
			w << t.make(page:p)
		}
				
		// mark page as PUBLISHED
		def rev = p.revisions.last()
		rev.state = Revision.PUBLISHED
		rev.updatedBy = session.user
		rev.lastUpdated = new Date()
		if(params.comment) {
			rev.addComment(session.user,params.comment)	
		}
		rev.save()
		
		// check for site layout template
		def structTemplateUri = getTemplateUri("templates/${p.site.domain}-layout")
		if(!servletContext.getResource(structTemplateUri)) {
			structTemplateUri = getTemplateUri("templates/default-layout")
		}
		
		// publish site structure
		def structUri = getViewUri("/layouts/${p.site.domain}")
		
        def st = null		
		if(!servletContext.getAttribute("${p.site.domain}-structure")) {
			st = engine.createTemplate( servletContext.getResource(structTemplateUri) )
			servletContext.setAttribute("${p.site.domain}-structure",st)
		}
		else {
			st = servletContext.getAttribute("${p.site.domain}-structure")	
		}		

		// the first level of items below the home page
		def firstLevel =  Page.findAllByParent(p.site.homePage,[sort:'position'] )
							.findAll(ApplicationConfig.PAGE_DISPLAY_CRITERIA)
			
	    // a closure to be placed in the binding to retrieve child pages
		def getChildPages = { parent ->
			return Page.findAllByParent(parent,[sort:'position'] )
										.findAll(ApplicationConfig.PAGE_DISPLAY_CRITERIA)			
		}
		
		// a that gets a link based on the page type
		def getLinkId = { page ->
			if(page.type == Page.LINK) {
				if(page.content ==~ /\d+/) {
					return page.content.trim().toInteger()		
				}				
			}
			else {
				return page.id
			}	
		}
							
		new File(servletContext.getRealPath(structUri)).withWriter { w ->
			w << st.make(	site:p.site,
							page:p,
							Revision:Revision,
							Page:Page,
							firstLevel:firstLevel,
							getChildPages:getChildPages,
							getLinkId:getLinkId)	
		}				
		// render page response
		render(template:"/pagexml",model:[page:p,alert:"Page published."])
	}
	
    def create = {
        def page = new Page()
        page.properties = params
        return ['page':page]
    }
	
	def delete = {
		if(!params.id) {
			renderError "Page id not specified"
			return
		}
		if(!params.comment) {
			renderError "Deleting a page requires a comment"
			return
		}
		
		def page = Page.get(params.id)
		if(!page.parent) {
			renderError "You cannot delete the home page of a site"
			return
		}
	
		if(!page) {
			renderError "Page not found for id ${params.id}"
			return
		}			

		if(page?.revisions) {
			def last = page.revisions.last()
			if(last.state == Revision.DELETE_REQUESTED || last.state == Revision.DELETED) {
				renderError "Page has already been marked for deletion"
				return				
			}
			last.state = Revision.DELETE_REQUESTED
			last.updatedBy = session.user
			if(params.comment) {
				last.addComment(session.user,params.comment )												
			}
			last.save()
			
			// check if the page has children
			// if so mark them as deleted them too with the same comment
			if(page.children) {
				deleteChildren(page.children)	
			}
			
			render(template:"/pagexml",model:[page:page,alert:"The page has been marked for deletion by a content approver."])			
		}
	}
	
	def deleteChildren(children) {
		children.each { child ->
			def rev = (child.revisions ? child.revisions.last() : new Revision(content:child.content,page:child) )
			rev.state = Revision.DELETE_REQUESTED
			if(params.comment) {
				rev.addComment(session.user,params.comment)	
			}
			rev.save()
			
			if(child.children) {
				//recurse
				deleteChildren(child.children)
			}
		}
	}

	def approve = {
		if(!params.id) {
			renderError "Page id not specified"
			return
		}
			
		def page = Page.get(params.id)
		if(!page) {
			renderError "Page not found for id ${params.id}"
		}
		else {			
			if(page.revisions) {
				def last = page.revisions.last()
				boolean shouldSave = false
				switch(last.state) {
					case Revision.DELETE_REQUESTED:
						last.state = Revision.DELETED
						shouldSave = true														
					break;
					case Revision.EDITED:
						last.state = Revision.APPROVED
						shouldSave = true
					break;
					case Revision.ADDED:
						last.state = Revision.APPROVED
						shouldSave = true
					break;				
				}
				println page.content				
				if(shouldSave) {
					last.updatedBy = session.user	
					last.lastUpdated = new Date()	
					if(params.comment) {
						last.addComment(session.user,params.comment)	
					}
					last.save()
					render(template:"/pagexml",model:[page:page,alert:'Page approved'])					
				}				
			}
			else {
				def r = new Revision(updatedBy:session.user,state:Revision.APPROVED)
				if(params.comment) {
					r.addComment(session.user, params.comment)	
				}
				page.addRevision( r )	
				page.save()
				render(template:"/pagexml",model:[page:page,alert:'Page approved'])
			}
			
		}
	}
	
	def reject = {
		if(!params.id) {
			renderError "Page id not specified"
			return
		}
		if(!params.comment) {
			renderError "Rejecting a change requires a comment. Use the 'Add Comment' button at the bottom of the editor."
			return
		}
			
		def page = Page.get(params.id)
		if(!page) {
			renderError "Page not found for id ${params.id}"
			return
		}
		
		if(page.revisions) {
			def last = page.revisions.last()
			last.state = Revision.REJECTED
			last.updatedBy = session.user	
			last.lastUpdated = new Date()			
			last.addComment( session.user, params.comment )
			last.save()		
			
			render(template:"/pagexml",model:[page:page,alert:'Page change rejected'])
		}		
	}
	
	def comments = {
		if(params.id) {
			def p = Page.get(params.id)
			
			if(p.revisions && p.revisions.size() > 0) {
				def last = p.revisions.last()
				return [
							comments:last.comments,
							revision:last,
							page:p
						]
			}
		}
	}
	
	def moveUp = {
		if(!params.id) {
			renderError("Page id not specified")
			return
		}
		
		def p = Page.get(params.id)
		if(p) {
			def newPos = p.position - 1
			println "updated position to ${newPos}"
			def prevPages = Page
				.findAllByPositionLessThanOrEqualAndParent(newPos,p.parent,[sort:'position'])

			println "Previous pages $prevPages"				
			def existing = null
			
			if(prevPages) {
				existing = prevPages[prevPages.size()-1]	
			}
			if(existing == p && prevPages.size() > 1)
				existing = prevPages[prevPages.size()-2]
			
			
			println "Existing page is $existing with position ${existing.position}" 
			if(existing) {
				while(existing.position <= newPos) {
					existing.position = existing.position + 1
					println "Updating existing position to ${existing.position}"	
				}
				existing.save()					
			}
			p.position = newPos
			p.save()
			render(template:"/pagexml",model:[page:p])			
		}
		else {
			renderError "Page not found for id ${params.id}"
		}	
	}
	
	def moveDown = {
		if(!params.id) {
			renderError("Page id not specified")
			return
		}
		
		def p = Page.get(params.id)
		if(p) {
			def newPos = p.position + 1
			println "updated position to ${newPos}"
			def followingPages = Page
				.findAllByPositionGreaterThanOrEqualAndParent(newPos,p.parent,[sort:'position'])

			println "Following pages $followingPages"				
			def existing = followingPages[0]
			if(existing == p)
				existing = followingPages[1]
			
			
			println "Existing page is $existing with position ${existing.position}" 
			if(existing) {
				while(existing.position >= newPos) {
					existing.position = existing.position - 1
					println "Updating existing position to ${existing.position}"	
				}
				existing.save()					
			}
			p.position = newPos
			p.save()
			render(template:"/pagexml",model:[page:p])			
		}
		else {
			renderError "Page not found for id ${params.id}"
		}		
	}
	
	def add = {										 		
		// retrieve parent and add as child
		if(!params.'parent.id' ) {
			renderError("Page parent id is required")
			return
		}
		if(!params.title) { 
			renderError "Page title is required"
			return
		}
			
		def title = params.title.replaceAll(/&/,'&amp;')
		def parent = Page.get( params.'parent.id' )
		def existing = Page.findByTitleAndParent(title,parent)
		if(existing?.revisions) {
			def last = existing.revisions.last()
			// if there is an existing page that has been deleted
			// restore it	
			if(last.state == Revision.DELETED) {
				existing.properties = params
				last.state = Revision.ADDED
				existing.title = title
				existing.save()
				
				render(template:"/pagexml",model:[page:existing])				
			}
			else {
				renderError("Page already exists with specified title")
				return				
			}
		}
		else if( existing ) {
			renderError("Page already exists with specified title")
			return
		}
		else {
			// create and bind params to page
			def page = new Page()
			page.properties = params
			page.title = title
			page.createdBy = session.user
			println "Creating page with title ${page.title}"
			// create first revision
			def rev = new Revision( page:page, content: page.content,updatedBy:session.user )
			
			if(params.comment) {
				rev.addComment(session.user,params.comment)
			}
			page.addRevision( rev )
			
			if(parent) {
				parent.addPage(page)
				page.site = parent.site
				if( page.save() ) {				
					render(template:"/pagexml",model:[page:page])
				}
				else {
					renderError(parent.errors.allErrors.defaultMessage.join(','))
				}
			}
			else {
				renderError("Parent page not found for id ${params.'parent.id'}")
			}				
		}	
	}
	
	def rollback = {
		if(params.id) {
			def p = Page.get(params.id)			
			if(p.revisions) {
				def last = p.revisions.last()

				def prev = Revision.findByNumberAndPage(last.number - 1,p)
				if(prev) {
					p.content = prev.content
					p.revisions.remove(last)	
					p.save()
					last.delete()
					if(params.comment) {
						prev.addComment(session.user,params.comment)
						prev.save()
					}
					render(template:"/pagexml",model:[page:p, alert:"Page rolled back to version $val"])
				}
				else if(p.revisions.size()==1) {
					last.state = Revision.DELETED;
					last.save()
					if(params.comment) {
						last.addComment(session.user,params.comment)
						last.save()
					}					
					render(template:"/pagexml",model:[page:p, alert:"Page deleted."])					
				}
				else {
					renderError "No previous revision found, if this is the first revision there is nothing to go back to."		
				}
			}
		}
		else {
			renderError("Page id not specified")
		}
	}
	
    def save = {
       def page = Page.get(params.page)
		if(page) {
			// update the content
			page.content = params.editor
			if(!page.revisions) {
				def revision = new Revision( 	content: params.editor,
												updatedBy: session.user,
												state: Revision.EDITED )
				if(params.comment) {
					revision.addComment(session.user,params.comment)	
				}
				page.addRevision(revision)				
			}
			else {
				def rev = page
									.revisions
									.last()
				if(rev.state == Revision.PUBLISHED) {
					rev = new Revision( updatedBy:session.user,
									   					state: Revision.EDITED )
					page.addRevision( rev )
				}
				else {
					rev.content = params.editor
					rev.updatedBy = session.user	
					rev.lastUpdated = new Date()
					rev.state = Revision.EDITED					
				}	
				if(params.comment) {
					rev.addComment(session.user,params.comment)	
				}
			}
			if(page.save()) {
				// notify content administrators for site
				User.findAll('from User as u where u.role.name = ? and u.role.site.id = ?'
								,[Role.CONTENT_EDITOR, page.site.id]).each { user ->
					// send email 
					if(session.user?.email && user.email) {
						Notifications.sendNotification(session.user, user, "Page Updated: ${page.title}", """
							Dear ${user},
							
							The page ${page} on site ${page.site} has been updated and
							requires approval.
							
							Regards,
							${session.user}
						""" )
					}
				}
				render(template:"/pagexml",model:[page:page,alert:"Page saved. A Content approver has been notified."])				
			}
			else {
				render(template:"/pagexml",model:[page:page,alert:"Unable to save page."])					
			}				
		}
		else {
			renderError("Page not found for id ${params.page}")
		}
    }

	def renderError(msg) {
		render(contentType:'text/xml') {
			error(message:msg)
		}		
	}
}