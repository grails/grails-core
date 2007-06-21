package grails.config/**
 * Class description here.
 
 * @author Graeme Rocher
 * @since 0.4
  *
 * Created: Jun 20, 2007
 * Time: 6:28:30 PM
 * 
 */

class ConfigValue  {
    def value
    ConfigObject co = new ConfigObject()

    ConfigValue(value) {
        this.metaClass = getClass().metaClass
        this.value = value
    }

    def invokeMethod(String name, args) {
        println "invoke method $name and $args"
        value."$name"(args)
    }

    void setProperty(String name, value) {
       if(name == 'metaClass') super.setProperty(name,value)
       else {
          co[name] = value
       }
    }

    def getProperty(String name) {
        println "calling getProperty! $name"
        co[name]
    }
    
    boolean equals(other) {
        println "comparing $other of type ${other.class} with self ${value} and class ${value.class}"
        
        if(other instanceof GString)other = other.toString()

        value.equals(other)
    }

    int hashCode() { value.hashCode() }

    String toString() { value.toString() }

}