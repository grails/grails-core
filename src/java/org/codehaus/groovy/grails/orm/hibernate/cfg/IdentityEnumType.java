/* Copyright 2004-2008 the original author or authors.
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
package org.codehaus.groovy.grails.orm.hibernate.cfg;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.ApplicationHolder;
import org.codehaus.groovy.grails.commons.ConfigurationHolder;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.type.NullableType;
import org.hibernate.type.TypeFactory;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Hibernate Usertype that enum values by their ID
 *
 * @author Siegfried Puchbauer
 * @since 1.1
 */
public class IdentityEnumType implements UserType, ParameterizedType, Serializable {

    private static final Log LOG = LogFactory.getLog(IdentityEnumType.class);

    public static final String ENUM_ID_ACCESSOR = "getId";
    public static final String PARAM_ENUM_CLASS = "enumClass";

    private static final Map<Class<? extends Enum>, BidiEnumMap> ENUM_MAPPINGS = new HashMap<Class<? extends Enum>, BidiEnumMap>();

    public static BidiEnumMap getBidiEnumMap(Class<? extends Enum> cls) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        BidiEnumMap m = ENUM_MAPPINGS.get(cls);
        if (m == null) {
            synchronized (ENUM_MAPPINGS) {
                if (!ENUM_MAPPINGS.containsKey(cls)) {
                    m = new BidiEnumMap(cls);
                    ENUM_MAPPINGS.put(cls, m);
                } else {
                    m = ENUM_MAPPINGS.get(cls);
                }
            }
        }
        return m;
    }

    private Class<? extends Enum> enumClass;

    private BidiEnumMap bidiMap;

    private NullableType type;
    private int[] sqlTypes;

    public static boolean isEnabled() {
        Object disableConfigOption = ConfigurationHolder.getFlatConfig().get("grails.orm.enum.id.mapping");
        return disableConfigOption == null || !(Boolean.FALSE.equals(disableConfigOption));
    }

    public static boolean supports(Class enumClass) {
        if (!isEnabled()) return false;
        if (GrailsClassUtils.isJdk5Enum(enumClass)) {
            try {
                Method idAccessor = enumClass.getMethod(ENUM_ID_ACCESSOR);
                int mods = idAccessor.getModifiers();
                if (Modifier.isPublic(mods) && !Modifier.isStatic(mods)) {
                    Class returnType = idAccessor.getReturnType();
                    return returnType != null && TypeFactory.basic(returnType.getName()) instanceof NullableType;
                }
            } catch (NoSuchMethodException e) {
                // ignore
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public void setParameterValues(Properties properties) {
        try {
            enumClass = (Class<? extends Enum>) ApplicationHolder.getApplication().getClassLoader().loadClass((String) properties.get(PARAM_ENUM_CLASS));
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Building ID-mapping for Enum Class %s", enumClass.getName()));
            }
            bidiMap = getBidiEnumMap(enumClass);
            type = (NullableType) TypeFactory.basic(bidiMap.keyType.getName());
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Mapped Basic Type is %s", type));
            }
            sqlTypes = new int[]{type.sqlType()};
        } catch (Exception e) {
            throw new MappingException("Error mapping Enum Class using IdentifierEnumType", e);
        }
    }

    public int[] sqlTypes() {
        return sqlTypes;
    }

    public Class returnedClass() {
        return enumClass;
    }

    public boolean equals(Object o1, Object o2) throws HibernateException {
        return o1 == o2;
    }

    public int hashCode(Object o) throws HibernateException {
        return o.hashCode();
    }

    public Object nullSafeGet(ResultSet resultSet, String[] names, Object owner) throws HibernateException, SQLException {
        Object id = type.get(resultSet, names[0]);
        if ((!resultSet.wasNull()) && id != null) {
            return bidiMap.getEnumValue(id);
        }
        return null;
    }

    public void nullSafeSet(PreparedStatement pstmt, Object value, int idx) throws HibernateException, SQLException {
        if (value == null) {
            pstmt.setNull(idx, sqlTypes[0]);
        } else {
            type.set(pstmt, bidiMap.getKey(value), idx);
        }
    }

    public Object deepCopy(Object o) throws HibernateException {
        return o;
    }

    public boolean isMutable() {
        return false;
    }

    public Serializable disassemble(Object o) throws HibernateException {
        return (Serializable) o;
    }

    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return cached;
    }

    public Object replace(Object orig, Object target, Object owner) throws HibernateException {
        return orig;
    }


    @SuppressWarnings("unchecked")
    private static class BidiEnumMap implements Serializable {

        private final Map enumToKey;
        private final Map keytoEnum;

        private Class keyType;

        private BidiEnumMap(Class<? extends Enum> enumClass) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Building Bidirectional Enum Map..."));
            }

            EnumMap enumToKey = new EnumMap(enumClass);
            HashMap keytoEnum = new HashMap();

            Method idAccessor = enumClass.getMethod(ENUM_ID_ACCESSOR);

            keyType = idAccessor.getReturnType();

            Method valuesAccessor = enumClass.getMethod("values");
            Object[] values = (Object[]) valuesAccessor.invoke(enumClass);

            for (Object value : values) {
                Object id = idAccessor.invoke(value);
                enumToKey.put(value, id);
                if (keytoEnum.containsKey(id)) {
                    LOG.warn(String.format("Duplicate Enum ID '%s' detected for Enum %s!", id, enumClass.getName()));
                }
                keytoEnum.put(id, value);
            }

            this.enumToKey = Collections.unmodifiableMap(enumToKey);
            this.keytoEnum = Collections.unmodifiableMap(keytoEnum);

        }

        public Object getEnumValue(Object id) {
            return keytoEnum.get(id);
        }

        public Object getKey(Object enumValue) {
            return enumToKey.get(enumValue);
        }
    }
}
