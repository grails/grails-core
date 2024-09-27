/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.grails.cli.compiler.autoconfigure;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

import org.grails.cli.compiler.AstUtils;
import org.grails.cli.compiler.CompilerAutoConfiguration;
import org.grails.cli.compiler.DependencyCustomizer;

/**
 * {@link CompilerAutoConfiguration} for Spring JDBC.
 *
 * @author Dave Syer
 * @since 1.0.0
 */
public class JdbcCompilerAutoConfiguration extends CompilerAutoConfiguration {

	@Override
	public boolean matches(ClassNode classNode) {
		return AstUtils.hasAtLeastOneFieldOrMethod(classNode, "JdbcTemplate", "NamedParameterJdbcTemplate",
				"DataSource");
	}

	@Override
	public void applyDependencies(DependencyCustomizer dependencies) {
		dependencies.ifAnyMissingClasses("org.springframework.jdbc.core.JdbcTemplate").add("spring-boot-starter-jdbc");
	}

	@Override
	public void applyImports(ImportCustomizer imports) {
		imports.addStarImports("org.springframework.jdbc.core", "org.springframework.jdbc.core.namedparam");
		imports.addImports("javax.sql.DataSource");
	}

}
