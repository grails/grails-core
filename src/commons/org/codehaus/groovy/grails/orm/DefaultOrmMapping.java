package org.codehaus.groovy.grails.orm;

import groovy.lang.GroovyObjectSupport;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;

import java.util.HashMap;
import java.util.Map;

public class DefaultOrmMapping extends GroovyObjectSupport implements OrmMapping {
    private String tableName;
    private final GrailsDomainClass domainClass;
    private final Map columnNames = new HashMap();

    public DefaultOrmMapping(final GrailsDomainClass domainClass) {
        this.domainClass = domainClass;
    }

    public String getTableName() {
        return tableName;
    }

    void table(final String name) {
        tableName = name;
    }

    public Object invokeMethod(final String name, final Object args) {
        final GrailsDomainClassProperty property = domainClass.getPropertyByName(name);
        if (property != null) {
            if (((Object[]) args)[0] instanceof Map) {
                final Map map = (Map) ((Object[]) args)[0];
                final String columnName = (String) map.get("column");
                if (columnName != null) {
                    columnNames.put(property, columnName);
                }
            }
            return property;
        }
        return super.invokeMethod(name, args);
    }

    public String getColumnName(final GrailsDomainClassProperty property) {
        return (String) columnNames.get(property);
    }
}
