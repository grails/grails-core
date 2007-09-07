package org.codehaus.groovy.grails.orm;

import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;

public interface OrmMapping {
    String getTableName();

    String getColumnName(GrailsDomainClassProperty property);
}
