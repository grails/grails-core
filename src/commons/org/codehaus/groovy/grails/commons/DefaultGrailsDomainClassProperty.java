/* Copyright 2004-2005 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 
package org.codehaus.groovy.grails.commons;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.validation.ConstrainedProperty;
import org.hibernate.MappingException;
import org.springframework.validation.Validator;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.util.*;

/**
 *
 * A class that represents a property of a domain class and contains meta information about the properties relationships, naming conventions and type
 *
 * @author Graeme Rocher
 * @since 0.1
 *
 * Created: 05-Jul-2005
 */
public class DefaultGrailsDomainClassProperty implements GrailsDomainClassProperty {


    private static final Log LOG  = LogFactory.getLog(DefaultGrailsDomainClassProperty.class);

    private GrailsDomainClass domainClass;
    private boolean persistant;
	private boolean identity;
	private boolean oneToMany;
	private String name;
	private Class type;
	private boolean manyToMany;
	private boolean manyToOne;
	private boolean oneToOne;

	private boolean bidirectional;

	private Class referencedPropertyType;
	private GrailsDomainClass referencedDomainClass;
	private GrailsDomainClassProperty otherSide;
    private String naturalName;
	private boolean inherited;
	private int fetchMode = FETCH_LAZY;
	private boolean owningSide;
	private String referencePropertyName;
    private boolean embedded;
    private GrailsDomainClass component;


    public DefaultGrailsDomainClassProperty(GrailsDomainClass domainClass, PropertyDescriptor descriptor)  {
        this.domainClass = domainClass;
        // persistant by default
        this.persistant = true;
        this.name = descriptor.getName();
        this.naturalName = GrailsClassUtils.getNaturalName(descriptor.getName());
        this.type = descriptor.getPropertyType();
        this.identity = descriptor.getName().equals( IDENTITY );

        // establish if property is persistant
        if(domainClass != null) {
            // figure out if this property is inherited
            if(!domainClass.isRoot()) {
                this.inherited = GrailsClassUtils.isPropertyInherited(domainClass.getClazz(), this.name);
            }
            List transientProps = getTransients(domainClass);
            checkIfTransient(transientProps);

            establishFetchMode();

        }
    }

    /**
     * Evaluates the fetchmode 
     *
     */
	private void establishFetchMode() {

        Map fetchMap = (Map) domainClass.getPropertyValue(GrailsDomainClassProperty.FETCH_MODE, Map.class);
		if(fetchMap != null && fetchMap.containsKey(this.name)) {
			if("eager".equals(fetchMap.get(this.name))) {
				this.fetchMode = FETCH_EAGER;				
			}
		}
	}

	/**
	 * Checks whether this property is transient
	 * 
	 * @param transientProps The transient properties
	 */
	private void checkIfTransient(List transientProps) {
		if(transientProps != null) {
            for(Iterator i = transientProps.iterator();i.hasNext();) {

                // make sure its a string otherwise ignore. Note: Again maybe a warning?
                Object currentObj = i.next();
                if(currentObj instanceof String) {
                    String propertyName = (String)currentObj;
                    // if the property name is on the not persistant list
                    // then set persistant to false
                    if(propertyName.equals( this.name )) {
                        this.persistant = false;
                        break;
                    }
                }
            }
        }
	}


	/**
	 * Retrieves the transient properties
	 * 
	 * @param domainClass The owning domain class
	 * @return A list of transient properties
	 */
	private List getTransients(GrailsDomainClass domainClass) {
		List transientProps;
		transientProps= (List)domainClass.getPropertyValue( TRANSIENT, List.class );

        // Undocumented feature alert! Steve insisted on this :-)
        List evanescent = (List)domainClass.getPropertyValue(EVANESCENT, List.class );
        if(evanescent != null) {
            if(transientProps == null)
                transientProps = new ArrayList();

            transientProps.addAll(evanescent);
        }
		return transientProps;
	}


	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.domain.GrailsDomainClassProperty#getName()
	 */
	public String getName() {
		return this.name;
	}

	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.domain.GrailsDomainClassProperty#getType()
	 */
	public Class getType() {
		return this.type;
	}

	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.domain.GrailsDomainClassProperty#isPersistant()
	 */
	public boolean isPersistent() {
		return this.persistant;
	}

	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.domain.GrailsDomainClassProperty#isRequired()
	 */
	public boolean isOptional() {
		ConstrainedProperty constrainedProperty = (ConstrainedProperty) domainClass.getConstrainedProperties().get(name);
		return ( constrainedProperty != null ) && constrainedProperty.isNullable();
	}

	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.domain.GrailsDomainClassProperty#isIdentity()
	 */
	public boolean isIdentity() {
		return this.identity;
	}
	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.domain.GrailsDomainClassProperty#isOneToMany()
	 */
	public boolean isOneToMany() {
		return this.oneToMany;
	}
	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.domain.GrailsDomainClassProperty#isManyToOne()
	 */
	public boolean isManyToOne() {
		return this.manyToOne;
	}
	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.domain.GrailsDomainClassProperty#getFieldName()
	 */
	public String getFieldName() {
		return getName().toUpperCase();
	}
	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.domain.GrailsDomainClassProperty#isOneToOne()
	 */
	public boolean isOneToOne() {
		return this.oneToOne;
	}
	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.commons.GrailsDomainClassProperty#getDomainClass()
	 */
	public GrailsDomainClass getDomainClass() {
		return this.domainClass;
	}
	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.commons.GrailsDomainClassProperty#isManyToMany()
	 */
	public boolean isManyToMany() {
		return this.manyToMany;
	}

	/**
	 * @param manyToMany The manyToMany to set.
	 */
	protected void setManyToMany(boolean manyToMany) {
		this.manyToMany = manyToMany;
	}

	/**
	 * @param oneToMany The oneToMany to set.
	 */
	protected void setOneToMany(boolean oneToMany) {
		this.oneToMany = oneToMany;
	}


	/**
	 * @param manyToOne The manyToOne to set.
	 */
	protected void setManyToOne(boolean manyToOne) {
		this.manyToOne = manyToOne;
	}


	/**
	 * @param oneToOne The oneToOne to set.
	 */
	protected void setOneToOne(boolean oneToOne) {
		this.oneToOne = oneToOne;
	}


	/**
	 * @param persistant The persistant to set.
	 */
	protected void setPersistant(boolean persistant) {
		this.persistant = persistant;
	}

	
	/**
	 * Sets whether the relationship is bidirectional or not
	 */
	protected void setBidirectional(boolean bidirectional) {
		this.bidirectional = bidirectional;
	}


	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.commons.GrailsDomainClassProperty#getTypePropertyName()
	 */
	public String getTypePropertyName() {	
		String shortTypeName = ClassUtils.getShortClassName( this.type );
		return shortTypeName.substring(0,1).toLowerCase(Locale.ENGLISH) + shortTypeName.substring(1);
	}

	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.commons.GrailsDomainClassProperty#getReferencedPropertyType()
	 */
	public Class getReferencedPropertyType() {
		if(isDomainAssociation()) {
			return this.referencedPropertyType;
		}
		else {
			return getType();
		}
	}

    private boolean isDomainAssociation() {
        return (Collection.class.isAssignableFrom(this.type) || Map.class.isAssignableFrom(this.type)) && this.referencedPropertyType != null;
    }

    /* (non-Javadoc)
      * @see org.codehaus.groovy.grails.commons.GrailsDomainClassProperty#isBidirectional()
      */
	public boolean isBidirectional() {
		return this.bidirectional;
	}

	/**
	 * Sets the referenced property type of this property
	 */
	protected void setReferencedPropertyType(Class referencedPropertyType) {
		this.referencedPropertyType = referencedPropertyType;
	}

	
	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.commons.GrailsDomainClassProperty#isAssociation()
	 */

	public GrailsDomainClass getReferencedDomainClass() {
		return this.referencedDomainClass;
	}


	public void setReferencedDomainClass(GrailsDomainClass referencedDomainClass) {
		if(referencedDomainClass != null) {
			this.referencedDomainClass = referencedDomainClass;
			this.referencedPropertyType = referencedDomainClass.getClazz();
		}
	}


	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.commons.GrailsDomainClassProperty#isAssociation()
	 */
	public boolean isAssociation() {
        return isOneToMany() ||
                isOneToOne() ||
                isManyToOne() ||
                isManyToMany() ||
                isEmbedded();
	}

    public boolean isEnum() {
        return GrailsClassUtils.isJdk5Enum(getType());
    }

    public String getNaturalName() {
        return this.naturalName;
    }


    /* (non-Javadoc)
      * @see java.lang.Object#toString()
      */
    public String toString() {
        String assType = null;
        if(isManyToMany()) {
            assType = "many-to-many";
        }
        else if(isOneToMany()) {
            assType = "one-to-many";
        }
        else if(isOneToOne()) {
            assType = "one-to-one";
        }
        else if(isManyToOne()) {
            assType = "many-to-one";
        }
        else if(isEmbedded()) {
            assType = "embedded";
        }
        return new ToStringBuilder(this)
                        .append("name", this.name)
                        .append("type", this.type)
                        .append("persistent", isPersistent())
                        .append("optional", isOptional())
                        .append("association", isAssociation())
                        .append("bidirectional", isBidirectional())
                        .append("association-type", assType)
                        .toString();
    }

	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.commons.GrailsDomainClassProperty#getOtherSide()
	 */
	public GrailsDomainClassProperty getOtherSide() {
		return this.otherSide;
	}


	public void setOtherSide(GrailsDomainClassProperty property) {
        if(!this.equals(property)) {
            this.bidirectional = true;
            if(isOneToOne() && property.isOneToMany()) {
                this.oneToOne = false;
                this.manyToOne = true;
            }
        }
        this.otherSide = property;
	}


	public boolean isInherited() {
		return this.inherited;
	}


	public int getFetchMode() {
		return this.fetchMode ;
	}

	public boolean isOwningSide() {
		return this.owningSide;
	}

	public void setOwningSide(boolean b) {
		this.owningSide = b;
	}

    public boolean isCircular() {
		if(this.otherSide != null) {
            if(this.otherSide.getDomainClass().getClazz().isAssignableFrom(this.domainClass.getClazz()))            
				return true;
		}
		return false;
	}

	public void setReferencePropertyName(String name) {
		this.referencePropertyName = name;
	}

	public String getReferencedPropertyName() {
		return this.referencePropertyName;
	}


    public boolean isEmbedded() {
        return embedded;
    }

    public GrailsDomainClass getComponent() {
        return this.component;
    }

    public void setEmbedded(boolean isEmbedded) {
        this.embedded = isEmbedded;
        if(isEmbedded) {
            this.component = new ComponentDomainClass(getType());
        }
    }

  /**
     * Overriddent equals to take into account inherited properties
     * e.g. childClass.propertyName is equal to parentClass.propertyName if the types match and
     * childClass.property.isInherited
     *
     * @param o the Object to compare this property to
     * @return boolean indicating equality of the two objects
     */
    public boolean equals(Object o) {
        if(o == null){
            return false;
        }
        if(o instanceof GrailsDomainClassProperty){
            if(!super.equals(o)){
                GrailsDomainClassProperty otherProp = (GrailsDomainClassProperty) o;
                boolean namesMatch = otherProp.getName().equals(getName());
                boolean typesMatch = otherProp.getReferencedPropertyType().equals(getReferencedPropertyType());
                Class myActualClass = getDomainClass().getClazz();
                Class otherActualClass = otherProp.getDomainClass().getClazz() ;
                boolean classMatch = otherActualClass.isAssignableFrom(myActualClass) ||
                        myActualClass.isAssignableFrom(otherActualClass);
                return namesMatch && typesMatch && classMatch;
            }else{
                return true;
            }
        }else{
          return false;
        }
    }

    private class ComponentDomainClass extends AbstractGrailsClass implements GrailsDomainClass {
        private GrailsDomainClassProperty[] properties;
        private Map constraints = Collections.EMPTY_MAP;
        private List transients = Collections.EMPTY_LIST;

        public ComponentDomainClass(Class type) {
            super(type, "");

            PropertyDescriptor[] descriptors;

            try {
                descriptors = java.beans.Introspector.getBeanInfo(type).getPropertyDescriptors();
            } catch (IntrospectionException e) {
                throw new MappingException("Failed to use class ["+type+"] as a component. Cannot introspect! " + e.getMessage());
            }

            List tmp = (List)getPropertyOrStaticPropertyOrFieldValue(GrailsDomainClassProperty.TRANSIENT, List.class);
            if(tmp!=null) this.transients = tmp;
            this.properties = createDomainClassProperties(this,descriptors);
            try {
                this.constraints = GrailsDomainConfigurationUtil.evaluateConstraints(getReference().getWrappedInstance(), properties);
            } catch (IntrospectionException e) {
                LOG.error("Error reading embedded component ["+getClazz()+"] constraints: " +e .getMessage(), e);
            }
        }

        private GrailsDomainClassProperty[] createDomainClassProperties(ComponentDomainClass type, PropertyDescriptor[] descriptors) {
            List properties = new ArrayList();
            for (int i = 0; i < descriptors.length; i++) {
                PropertyDescriptor descriptor = descriptors[i];
                if(isPersistentProperty(descriptor)) {
                    properties.add(new DefaultGrailsDomainClassProperty(type,descriptor));
                }
            }
            return (GrailsDomainClassProperty[])properties.toArray(new GrailsDomainClassProperty[properties.size()]);
        }

        private boolean isPersistentProperty(PropertyDescriptor descriptor) {
            String propertyName = descriptor.getName();
            return GrailsDomainConfigurationUtil.isNotConfigurational(descriptor) && !transients.contains(propertyName);
        }


        public boolean isOwningClass(Class domainClass) {
            return domainClass != null && domainClass.equals(getDomainClass().getClazz());
        }

        public GrailsDomainClassProperty[] getProperties() {
            return properties;
        }

        /** 
          * @deprecated Use #getPersistentProperties instead
        	*/
        public GrailsDomainClassProperty[] getPersistantProperties() {
            return properties;
        }

        public GrailsDomainClassProperty[] getPersistentProperties() {
            return properties;
        }

        public GrailsDomainClassProperty getIdentifier() {
            return null;  // no identifier for embedded component
        }

        public GrailsDomainClassProperty getVersion() {
            return null;  // no version for embedded component
        }

        public Map getAssociationMap() {
            return Collections.EMPTY_MAP;
        }

        public GrailsDomainClassProperty getPropertyByName(String name) {
            for (int i = 0; i < properties.length; i++) {
                GrailsDomainClassProperty property = properties[i];
                if(property.getName().equals(name)) return property;
            }
            return null;
        }

        public String getFieldName(String propertyName) {
            return null;
        }


        public boolean isOneToMany(String propertyName) {
            return false;
        }

        public boolean isManyToOne(String propertyName) {
            return false;
        }

        public boolean isBidirectional(String propertyName) {
            return false;
        }

        public Class getRelatedClassType(String propertyName) {
            return getPropertyByName(propertyName).getReferencedPropertyType();
        }

        public Map getConstrainedProperties() {
            return this.constraints;
        }

        public Validator getValidator() {
            return null;
        }

        public void setValidator(Validator validator) {
        }

        public String getMappingStrategy() {
            return GrailsDomainClass.GORM;
        }

        public boolean isRoot() {
            return true;
        }

        public Set getSubClasses() {
            return Collections.EMPTY_SET;
        }

        public void refreshConstraints() {
            // do nothing
        }

        public boolean hasSubClasses() {
            return false;
        }

        public Map getMappedBy() {
            return Collections.EMPTY_MAP;
        }

        public boolean hasPersistentProperty(String propertyName) {
            for (int i = 0; i < properties.length; i++) {
                GrailsDomainClassProperty persistantProperty = properties[i];
                if(persistantProperty.getName().equals(propertyName)) return true;
            }
            return false;
        }

        public void setMappingStrategy(String strategy) {
            // do nothing
        }
    }

}
