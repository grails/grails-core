package org.codehaus.groovy.grails.commons;

/**
 * @author Marc Palmer (marc@anyware.co.uk)
*/
public class DataSourceArtefactHandler extends ArtefactHandlerAdapter {

    public static final String TYPE = "DataSource";


    public DataSourceArtefactHandler() {
        super(TYPE, GrailsDataSource.class, DefaultGrailsDataSource.class, DefaultGrailsDataSource.DATA_SOURCE);
    }
}
