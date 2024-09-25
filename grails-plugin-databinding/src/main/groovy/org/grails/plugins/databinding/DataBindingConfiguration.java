package org.grails.plugins.databinding;

import grails.core.GrailsApplication;
import grails.databinding.TypedStructuredBindingEditor;
import grails.databinding.converters.FormattedValueConverter;
import grails.databinding.converters.ValueConverter;
import grails.databinding.events.DataBindingListener;
import grails.util.GrailsArrayUtils;
import grails.web.databinding.GrailsWebDataBinder;
import org.grails.databinding.bindingsource.DataBindingSourceCreator;
import org.grails.web.databinding.bindingsource.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

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
            TypedStructuredBindingEditor[] structuredBindingEditors,
            DataBindingListener[] dataBindingListeners) {

        GrailsWebDataBinder dataBinder = new GrailsWebDataBinder(grailsApplication);
        dataBinder.setConvertEmptyStringsToNull(configurationProperties.isConvertEmptyStringsToNull());
        dataBinder.setTrimStrings(configurationProperties.isTrimStrings());
        dataBinder.setAutoGrowCollectionLimit(configurationProperties.getAutoGrowCollectionLimit());
        final ApplicationContext mainContext = grailsApplication.getMainContext();
        final ValueConverter[] mainContextConverters = mainContext
                .getBeansOfType(ValueConverter.class).values().toArray(new ValueConverter[0]);
        final ValueConverter[] allValueConverters = GrailsArrayUtils.concat(valueConverters, mainContextConverters);
        AnnotationAwareOrderComparator.sort(allValueConverters);
        dataBinder.setValueConverters(allValueConverters);

        final FormattedValueConverter[] mainContextFormattedValueConverters = mainContext
                .getBeansOfType(FormattedValueConverter.class).values().toArray(new FormattedValueConverter[0]);
        dataBinder.setFormattedValueConverters(GrailsArrayUtils.concat(formattedValueConverters, mainContextFormattedValueConverters));
        final TypedStructuredBindingEditor[] mainContextStructuredBindingEditors = mainContext
                .getBeansOfType(TypedStructuredBindingEditor.class).values().toArray(new TypedStructuredBindingEditor[0]);
        dataBinder.setStructuredBindingEditors(GrailsArrayUtils.concat(structuredBindingEditors, mainContextStructuredBindingEditors));
        final DataBindingListener[] mainContextDataBindingListeners = mainContext
                .getBeansOfType(DataBindingListener.class).values().toArray(new DataBindingListener[0]);
        dataBinder.setDataBindingListeners(GrailsArrayUtils.concat(dataBindingListeners,mainContextDataBindingListeners));
        dataBinder.setMessageSource(mainContext.getBean("messageSource", MessageSource.class));
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
