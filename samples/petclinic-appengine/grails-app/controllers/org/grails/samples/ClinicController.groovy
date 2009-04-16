package org.grails.samples

class ClinicController {

	def index = {}
	
	def persistenceManager
	def vets = {
		def query  = persistenceManager.newQuery(Vet)
		[vets: query.execute() ]
	}

}