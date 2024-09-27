package org.grails.cli.compiler.autoconfigure;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

import org.grails.cli.compiler.CompilerAutoConfiguration;
import org.grails.cli.compiler.DependencyCustomizer;

public class GrailsCompilerAutoConfiguration extends CompilerAutoConfiguration {

    @Override
    public boolean matches(ClassNode classNode) {
        return true;
    }

    @Override
    public void applyDependencies(DependencyCustomizer dependencies) {
        dependencies.ifAnyMissingClasses("org.springframework.web.servlet.mvc.Controller").add("spring-boot-starter-web");
        dependencies.ifAnyMissingClasses("grails.boot.config.GrailsAutoConfiguration").add("grails-boot");
        dependencies.ifAnyMissingClasses("grails.core.DefaultGrailsApplication").add("grails-core");
        dependencies.add("grails-web");
        dependencies.add("grails-plugin-i18n", "grails-plugin-codecs", "grails-plugin-controllers",
                "grails-plugin-converters", "grails-plugin-databinding", "grails-plugin-interceptors", "grails-plugin-mimetypes",
                "grails-plugin-rest", "grails-plugin-url-mappings");
        dependencies.add("gsp", "plain", "jar", true);
    }

    @Override
    public void applyImports(ImportCustomizer imports) {
        imports.addImports("groovy.transform.CompileStatic");
    }

}