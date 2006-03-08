class @domain.class.name@ { 
	@Property Long id
	@Property Long version

    String toString() { "${this.class.name} :  $id" }	
}	
