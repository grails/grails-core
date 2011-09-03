/*
 * Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.web.util

import java.util.Date;
import java.util.List;

import org.apache.commons.lang.builder.HashCodeBuilder

/**
 * An category for use with maps that want type conversion capabilities
 *
 * Type converting maps have no inherent ordering. Two maps with identical entries
 * but arranged in a different order internally are considered equal.
 *
 * @author Graeme Rocher
 * @since 1.2
 */
class TypeConvertingMap extends AbstractTypeConvertingMap implements Cloneable {
	TypeConvertingMap() {
		super()
	}

	TypeConvertingMap(Map map) {
		super(map)
	}

	Object clone() {
		new TypeConvertingMap(new LinkedHashMap(this.@wrappedMap))
	}

	public Byte 'byte'(String name) {
		return super.getByte(name);
	}

	public Byte 'byte'(String name, Integer defaultValue) {
		return super.getByte(name, defaultValue);
	}

	public Character 'char'(String name) {
		return super.getChar(name);
	}

	public Character 'char'(String name, Character defaultValue) {
		return super.getChar(name, defaultValue?.charValue() as Integer);
	}

	public Character 'char'(String name, Integer defaultValue) {
		return super.getChar(name, defaultValue);
	}

	public Integer 'int'(String name) {
		return super.getInt(name);
	}

	public Integer 'int'(String name, Integer defaultValue) {
		return super.getInt(name, defaultValue);
	}

	public Long 'long'(String name) {
		return super.getLong(name);
	}

	public Long 'long'(String name, Long defaultValue) {
		return super.getLong(name, defaultValue);
	}

	public Short 'short'(String name) {
		return super.getShort(name);
	}

	public Short 'short'(String name, Integer defaultValue) {
		return super.getShort(name, defaultValue);
	}

	public Double 'double'(String name) {
		return super.getDouble(name);
	}

	public Double 'double'(String name, Double defaultValue) {
		return super.getDouble(name, defaultValue);
	}

	public Float 'float'(String name) {
		return super.getFloat(name);
	}

	public Float 'float'(String name, Float defaultValue) {
		return super.getFloat(name, defaultValue);
	}

	public Boolean 'boolean'(String name) {
		return super.getBoolean(name);
	}

	public Boolean 'boolean'(String name, Boolean defaultValue) {
		return super.getBoolean(name, defaultValue);
	}
}
