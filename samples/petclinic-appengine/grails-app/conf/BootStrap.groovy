import org.grails.samples.*

class BootStrap {

	 def persistenceManagerFactory
     def init = { servletContext ->
	
		def persistenceManager = persistenceManagerFactory.getPersistenceManager()
		
		def query = persistenceManager.newQuery(Speciality)
		query.setResult("count(name)")
		def count = query.execute()
	    if(!count) {						
			persistenceManager.makePersistent( new Vet(firstName:"James", lastName:"Carter") )
			def v1 = new Vet(firstName:"Helen", lastName:"Leary") 
			v1.specialities << new Speciality(name:"radiology")
			persistenceManager.makePersistent( v1 )
			def v2 = new Vet(firstName:"Linda", lastName:"Douglas")
			v2.specialities << new Speciality(name:"surgery")
			v2.specialities << new Speciality(name:"dentistry")
			persistenceManager.makePersistent(v2)
			def v3 = new Vet(firstName:"Rafael", lastName:"Ortega")
			v3.specialities << new Speciality(name:"surgery")
			persistenceManager.makePersistent(v3)
			def v4 = new Vet(firstName:"Henry", lastName:"Stevens")
			v4.specialities << new Speciality(name:"radiology")
			persistenceManager.makePersistent( v4 )
			persistenceManager.makePersistent( new Vet(firstName:"Sharon", lastName:"Jenkins") )
		
			['dog', 'lizard','cat', 'snake','bird', 'hamster'].each {
							persistenceManager.makePersistent(new PetType(name:it))
			}
			
			persistenceManager.close()

		}

     }
     def destroy = {
     }
} 