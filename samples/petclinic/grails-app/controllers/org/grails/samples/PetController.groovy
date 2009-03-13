package org.grails.samples;

class PetController {

	def add = {
		if(request.method == 'GET') {
			[pet: new Pet(owner: Owner.get(params['owner']?.id)), types: PetType.list() ]
		}
		else {
			def pet = new Pet(params['pet'])
			if(pet.save()) 
				redirect controller:'owner',action:'show', id:pet.owner.id
			else
				render view:'add', model: [pet: pet, types: PetType.list() ]
		}
	}

	def edit = {
		if(request.method == 'GET') {
			render view:'add',model:[pet: Pet.get(params.id), types: PetType.list() ]
		}
		else {
			def pet = Pet.get(params.id)
			pet.properties = params['pet']
			if(pet.save())
				redirect controller:'owner', action:'show', id:pet.owner.id
			else
				render view:'add', model:[pet: pet, types: PetType.list() ]
		}
	}
	
	def addVisit = {
		if(request.method == 'GET') {
			[visit: new Visit(pet: Pet.get(params.id))]
		}
		else {
			def visit = new Visit(params['visit'])
			if(visit.save())
				redirect controller:'owner', action:'show', id:visit.pet.owner.id
			else
				render view:'addVisit', model:[visit:visit]
		}
	}
}