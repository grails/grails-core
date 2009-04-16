package org.grails.samples

import com.google.appengine.api.datastore.*

class OwnerController {

	def persistenceManager
	
	def add = {
		if(request.method == 'GET') {
			[ownerBean: new Owner()]
		}
		else {
			def owner = new Owner(params['owner'])
			if(owner.validate()) {
				persistenceManager.makePersistent(owner)
				redirect action:'show', id:owner.key.id
			}
			else {
				render view:'add', model:[ownerBean:owner]
			}
		}
	}
	
	def show = {
	 	Key k = KeyFactory.createKey(Owner.simpleName, params.id.toInteger())
		def owner = persistenceManager.getObjectById(Owner, k)
		if(owner)
			[ownerBean:owner]
		else
			response.sendError 404
	}
	
	def edit = {
	 	Key k = KeyFactory.createKey(Owner.simpleName, params.id.toInteger())
		def owner = persistenceManager.getObjectById(Owner, k)
		owner = persistenceManager.detachCopy(owner)
		if(request.method == 'GET') {
			render view:'add',model:[ownerBean: owner]
		}
		else {
			owner.properties = params['owner']
			if(owner.validate()) {
				persistenceManager.makePersistent owner
				redirect action:'show', id:owner.key.id
			}
			else {
				render view:'add', model:[ownerBean: owner]
			}
		}
	}
	
	def find = {
		if(request.method == 'POST') {
			def query = 	persistenceManager.newQuery(Owner)
			query.setFilter("lastName == lastNameParam")
		 	query.declareParameters("String lastNameParam")
		 	def owners =  query.execute(params.lastName?.trim())
			if(owners) {
				if(owners.size()>1) 
					render view:'selection', model:[owners:owners]
				else
					redirect action:'show', id:owners[0].key.id
			}
			else {
				[message:'owners.not.found']
			}			
		}	
	}

}