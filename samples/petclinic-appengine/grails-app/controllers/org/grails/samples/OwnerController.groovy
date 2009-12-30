package org.grails.samples

import com.google.appengine.api.datastore.*

class OwnerController {
	
	def add = {
		if(request.method == 'GET') {
			[ownerBean: new Owner()]
		}
		else {
			def owner = new Owner(params['owner'])
			if(owner.validate()) {
				Owner.withTransaction {
					owner.save()
				}
				redirect action:'show', id:owner.id.id
			}
			else {
				render view:'add', model:[ownerBean:owner]
			}
		}
	}
	
	def show = {
		def owner = Owner.get(params.id.toLong())
		if(owner)
			[ownerBean:owner]
		else
			response.sendError 404
	}
	
	def edit = {
		def owner = Owner.get(params.id)
		if(request.method == 'GET') {
			render view:'add',model:[ownerBean: owner]
		}
		else {
			Owner.withTransaction { status ->
				owner.properties = params['owner']
				if(owner.validate()) {
					owner.save()
					redirect action:'show', id:owner.key.id
				}
				else {
					status.setRollbackOnly()
					render view:'add', model:[ownerBean: owner]
				}
			}
			
		}
	}
	
	def find = {
		println "LIST = ${Owner.list().lastName}"
		if(request.method == 'POST') {
			
		 	def owners =  Owner.findAllByLastName(params.lastName)
			if(owners) {
				if(owners.size()>1) 
					render view:'selection', model:[owners:owners]
				else
					redirect action:'show', id:owners[0].id.id
			}
			else {
				[message:'owners.not.found']
			}			
		}	
	}

}