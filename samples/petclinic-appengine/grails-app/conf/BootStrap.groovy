import org.grails.samples.*

class BootStrap {

     def init = { servletContext ->
	
		def count = Speciality.count()
	    if(!count) {						
			new Vet(firstName:"James", lastName:"Carter").save()
			def v1 = new Vet(firstName:"Helen", lastName:"Leary") 
			v1.specialities << new Speciality(name:"radiology")
			v1.save()
			def v2 = new Vet(firstName:"Linda", lastName:"Douglas")
			v2.specialities << new Speciality(name:"surgery")
			v2.specialities << new Speciality(name:"dentistry")
			v2.save()
			def v3 = new Vet(firstName:"Rafael", lastName:"Ortega")
			v3.specialities << new Speciality(name:"surgery")
			v3.save()
			def v4 = new Vet(firstName:"Henry", lastName:"Stevens")
			v4.specialities << new Speciality(name:"radiology")
			v4.save()
			new Vet(firstName:"Sharon", lastName:"Jenkins").save()
		
			['dog', 'lizard','cat', 'snake','bird', 'hamster'].each {
							new PetType(name:it).save(flush:true)
			}

		}

     }
     def destroy = {
     }
} 