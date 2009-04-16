package org.grails.samples;

import com.google.appengine.api.datastore.*

class PetController {

	def persistenceManager
	
	def add = {
		if(request.method == 'GET') {
			def p = new Pet()
			if(params['owner']?.id) {
			 	Key k = KeyFactory.createKey(Owner.simpleName, params['owner']?.id.toInteger())
				p.owner = persistenceManager.getObjectById(Owner, k)				
			}
			[pet: p, types: persistenceManager.newQuery(PetType).execute() ]
		}
		else {
			def pet = new Pet(params['pet'])
		 	Key k = KeyFactory.createKey(Owner.simpleName, params.'pet.owner.id'.toInteger())
			def owner = persistenceManager.getObjectById(Owner, k)				
			pet.owner = owner
			if(pet.validate()) {
				owner.pets << pet
				persistenceManager.makePersistent(pet)
				redirect controller:'owner',action:'show', id:pet.owner.key.id				
			}
			else {
				render view:'add', model: [pet: pet, types: persistenceManager.newQuery(PetType).execute() ]
			}				
		}
	}

	def edit = {
	 	Key k = KeyFactory.createKey(Pet.simpleName, params.id.toInteger())		
		if(request.method == 'GET') {
			render view:'add',model:[pet: persistenceManager.getObjectById(Pet, k), 
									 types: persistenceManager.newQuery(PetType).execute()  ]
		}
		else {
			def pet = persistenceManager.getObjectById(Pet, k)
			pet.properties = params['pet']
			if(pet.validate()) {
				persistenceManager.makePersistent pet
				redirect controller:'owner', action:'show', id:pet.owner.key.id				
			}
			else {
				persistenceManager.evict(pet)
				render view:'add', model:[pet: pet, types: persistenceManager.newQuery(PetType).execute() ]
			}
				
		}
	}
	
	def addVisit = {
	 	Key k = KeyFactory.createKey(Pet.simpleName, params.'visit.pet.id' ? params.'visit.pet.id'.toInteger() : params.id.toInteger())				
		if(request.method == 'GET') {
			[visit: new Visit(pet: persistenceManager.getObjectById(Pet, k))]
		}
		else {
			def visit = new Visit(params['visit'])
			def pet = persistenceManager.getObjectById(Pet, k)
			visit.pet = pet
			if(visit.validate()) {
				persistenceManager.makePersistent visit								
				pet.visits << visit
				redirect controller:'owner', action:'show', id:visit.pet.owner.key.id				
			}
			else {
				render view:'addVisit', model:[visit:visit]
			}
				
		}
	}
}