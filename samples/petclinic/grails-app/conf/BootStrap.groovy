import org.grails.samples.*

class BootStrap {

     def init = { servletContext ->
	    if(!Speciality.count()) {
			def rad =	new Speciality(name:"radiology").save()
			def sur =	new Speciality(name:"surgery").save()
			def den =	new Speciality(name:"dentistry").save()						
			
			new Vet(firstName:"James", lastName:"Carter").save()
			new Vet(firstName:"Helen", lastName:"Leary")
					.addToSpecialities(rad)
					.save()
			new Vet(firstName:"Linda", lastName:"Douglas")
					.addToSpecialities(sur)
					.addToSpecialities(den)
					.save()
			new Vet(firstName:"Rafael", lastName:"Ortega")
					.addToSpecialities(sur)
					.save()									
			new Vet(firstName:"Henry", lastName:"Stevens")
					.addToSpecialities(rad)
					.save()												
			new Vet(firstName:"Sharon", lastName:"Jenkins").save()															
		
			['dog', 'lizard','cat', 'snake','bird', 'hamster'].each {
							new PetType(name:it).save()
			}

		}

     }
     def destroy = {
     }
} 