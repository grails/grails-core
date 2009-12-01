package org.grails.samples;

import com.google.appengine.api.datastore.*

class PetController {
	
	def add = {
		if(request.method == 'GET') {
			def p = new Pet()
			if(params['owner']?.id) {
				p.owner = Owner.get(params['owner']?.id)
			}
			[pet: p, types: PetType.list() ]
		}
		else {

			def args = params['pet']
			def owner = Owner.get(args.remove('owner.id'))
			def type = PetType.get(args.remove('type.id'))
			def pet = new Pet(args)
			
			pet.owner = owner
			pet.type = type
			if(pet.validate()) {
				owner.pets << pet
				pet.save flush:true
				redirect controller:'owner',action:'show', id:pet.owner.id.id				
			}
			else {
				render view:'add', model: [pet: pet, types: PetType.list() ]
			}				
		}
	}

	def edit = {
		if(request.method == 'GET') {
			render view:'add',model:[pet: Pet.get(params.id.toLong()), 
									 types: PetType.list()  ]
		}
		else {
			Pet.withTransaction { status ->
				def pet = Pet.get(params.id)
				pet.properties = params['pet']
				if(pet.validate()) {
					pet.save()
					redirect controller:'owner', action:'show', id:pet.owner.key.id				
				}
				else {
					status.setRollbackOnly()
					render view:'add', model:[pet: pet, types: PetType.list() ]
				}				
			}
				
		}
	}
	
	def addVisit = {
		def id = params.'visit.pet.id' ? params.'visit.pet.id'.toInteger() : params.id.toInteger()
		if(request.method == 'GET') {
			[visit: new Visit(pet: Pet.get(id))]
		}
		else {
			def visit = new Visit(params['visit'])
			def pet = Pet.get(id)
			visit.pet = pet
			if(visit.validate()) {
				visit.save flush:true
				pet.visits << visit
				redirect controller:'owner', action:'show', id:visit.pet.owner.key.id				
			}
			else {
				render view:'addVisit', model:[visit:visit]
			}
				
		}
	}
}