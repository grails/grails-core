/*
 * Copyright 2011 SpringSource.
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
package org.codehaus.groovy.grails.plugins.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Used for secondary datasources that are read-only and not pooled.
 *
 * @author Burt Beckwith
 */
public class ReadOnlyDriverManagerDataSource extends DriverManagerDataSource {

    @Override
    protected Connection getConnectionFromDriverManager(final String url, final Properties props) throws SQLException {
        Connection connection = super.getConnectionFromDriverManager(url, props);
        connection.setReadOnly(true);
        return connection;
    }
}
