package org.codehaus.groovy.grails.orm.hibernate.support;

import java.lang.ref.SoftReference;

/**
 * 
 * SoftReference key to be used with ConcurrentHashMap 
 * 
 * @author Lari Hotari
 */
class SoftKey<T> extends SoftReference<T>{
	int hash;
	
	public SoftKey(T referent) {
		super(referent);
		this.hash=referent.hashCode();
	}

	@Override
	public int hashCode() {
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SoftKey<T> other = (SoftKey<T>) obj;
		if (hash != other.hash)
			return false;
		T referent=this.get();
		T otherReferent=other.get();
		if (referent == null) {
			if (otherReferent != null)
				return false;
		} else if (!referent.equals(otherReferent))
			return false;
		return true;
	}
}
