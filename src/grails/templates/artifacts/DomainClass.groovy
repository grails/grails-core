class @domain.class.name@ { 
	@Property Long id
	@Property Long version

    String toString() { "${this.class.name} :  $id" }

    boolean equals(other) {
        if(other instanceof @domain.class.name@) {
            return (id == other.id)
        }
        return false
    }

    int hashCode() { id }
}	
