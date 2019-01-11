package org.grails.plugins.databinding;

import grails.core.GrailsApplication;
import grails.databinding.TypedStructuredBindingEditor;
import grails.databinding.converters.FormattedValueConverter;
import grails.databinding.converters.ValueConverter;
import grails.web.databinding.DataBindingUtils;
import grails.web.databinding.GrailsWebDataBinder;
import io.micronaut.core.util.ArrayUtils;
import org.grails.databinding.bindingsource.DataBindingSourceCreator;
import org.grails.databinding.converters.DateConversionHelper;
import org.grails.web.databinding.bindingsource.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataBindingConfiguration {

    private final DataBindingConfigurationProperties configurationProperties;

    public DataBindingConfiguration(DataBindingConfigurationProperties configurationProperties) {
        this.configurationProperties = configurationProperties;
    }

    @Bean("grailsWebDataBinder")
    protected GrailsWebDataBinder grailsWebDataBinder(
            GrailsApplication grailsApplication,
            ValueConverter[] valueConverters,
            FormattedValueConverter[] formattedValueConverters,
            TypedStructuredBindingEditor[] structuredBindingEditors) {
        GrailsWebDataBinder dataBinder = new GrailsWebDataBinder(grailsApplication);
        dataBinder.setConvertEmptyStringsToNull(configurationProperties.isConvertEmptyStringsToNull());
        dataBinder.setTrimStrings(configurationProperties.isTrimStrings());
        dataBinder.setAutoGrowCollectionLimit(configurationProperties.getAutoGrowCollectionLimit());
        dataBinder.setStructuredBindingEditors(structuredBindingEditors);
        final ApplicationContext mainContext = grailsApplication.getMainContext();
        final ValueConverter[] mainContextConverters = mainContext
                .getBeansOfType(ValueConverter.class).values().toArray(new ValueConverter[0]);
        dataBinder.setValueConverters(ArrayUtils.concat(valueConverters, mainContextConverters));
        dataBinder.setFormattedValueConverters(formattedValueConverters);
        return dataBinder;
    }

    @Bean("xmlDataBindingSourceCreator")
    protected XmlDataBindingSourceCreator xmlDataBindingSourceCreator() {
        return new XmlDataBindingSourceCreator();
    }

    @Bean("jsonDataBindingSourceCreator")
    protected JsonDataBindingSourceCreator jsonDataBindingSourceCreator() {
        return new JsonDataBindingSourceCreator();
    }

    @Bean("halJsonDataBindingSourceCreator")
    protected HalJsonDataBindingSourceCreator halJsonDataBindingSourceCreator() {
        return new HalJsonDataBindingSourceCreator();
    }

    @Bean("halXmlDataBindingSourceCreator")
    protected HalXmlDataBindingSourceCreator halXmlDataBindingSourceCreator() {
        return new HalXmlDataBindingSourceCreator();
    }

    @Bean("jsonApiDataBindingSourceCreator")
    protected JsonApiDataBindingSourceCreator jsonApiDataBindingSourceCreator() {
        return new JsonApiDataBindingSourceCreator();
    }

    @Bean("dataBindingSourceRegistry")
    protected DataBindingSourceRegistry dataBindingSourceRegistry(DataBindingSourceCreator... creators) {
        final DefaultDataBindingSourceRegistry registry = new DefaultDataBindingSourceRegistry();
        registry.setDataBindingSourceCreators(creators);
        registry.initialize();
        return registry;
    }

}
