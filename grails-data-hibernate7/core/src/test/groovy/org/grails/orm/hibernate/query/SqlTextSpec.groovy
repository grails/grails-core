/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.grails.orm.hibernate.query

import spock.lang.Specification
import spock.lang.Unroll

import static org.grails.orm.hibernate.query.SqlSyntax.HQL
import static org.grails.orm.hibernate.query.SqlSyntax.SQL
import static org.grails.orm.hibernate.query.SqlSyntax.TRANSACT_SQL

/**
 * The one table of awkward inputs every scanner of the package is measured against: what a string literal, a quoted
 * or bracket identifier, a dollar-quoted string and a comment hide, what parentheses and array brackets nest, and
 * how the same text reads in each syntax.
 */
class SqlTextSpec extends Specification {

    @Unroll
    void "the SQL #sql splits at the top-level commas into #parts in every SQL syntax"() {
        expect:
        SqlText.of(sql, SQL).splitTopLevel(',' as char) == parts
        SqlText.of(sql, TRANSACT_SQL).splitTopLevel(',' as char) == parts

        where:
        sql                                       | parts
        'width'                                   | ['width']
        'width, sum(height) as total'             | ['width', 'sum(height) as total']
        "coalesce(label, 'a,b'), label"           | ["coalesce(label, 'a,b')", 'label']
        "'it''s, not', c"                         | ["'it''s, not'", 'c']
        '"odd,name", other'                       | ['"odd,name"', 'other']
        '`odd,name`, other'                       | ['`odd,name`', 'other']
        'a /* b, c */, d'                         | ['a /* b, c */', 'd']
        'a -- b, c\n, d'                          | ['a -- b, c', 'd']
        'concat(a, concat(b, c)), d'              | ['concat(a, concat(b, c))', 'd']
        'a$b, $1'                                 | ['a$b', '$1']
    }

    @Unroll
    void "the SQL #sql splits into #parts where brackets hold arrays"() {
        expect:
        SqlText.of(sql, SQL).splitTopLevel(',' as char) == parts

        where:
        sql                                       | parts
        'arr[f(a, b)], ARRAY[ARRAY[1, 2]], c'     | ['arr[f(a, b)]', 'ARRAY[ARRAY[1, 2]]', 'c']
        "ARRAY['['], height"                      | ["ARRAY['[']", 'height']
        "ARRAY[']', ','], [a,b]"                  | ["ARRAY[']', ',']", '[a,b]']
        'ARRAY [a, b], c'                         | ['ARRAY [a, b]', 'c']
        '"items"[1], c'                           | ['"items"[1]', 'c']
        "[a'b], c"                                | ["[a'b], c"]
        '$$a,b$$, c'                              | ['$$a,b$$', 'c']
        '$tag$a,$$,b$tag$, c'                     | ['$tag$a,$$,b$tag$', 'c']
    }

    @Unroll
    void "the Transact-SQL #sql splits into #parts where brackets quote an identifier"() {
        expect:
        SqlText.of(sql, TRANSACT_SQL).splitTopLevel(',' as char) == parts

        where:
        sql                                       | parts
        "width as [box's width], height as total" | ["width as [box's width]", 'height as total']
        'width as [a"b], c'                       | ['width as [a"b]', 'c']
        'sum(height) as [total (cm)], c'          | ['sum(height) as [total (cm)]', 'c']
        'sum(height) as [a]]b], c'                | ['sum(height) as [a]]b]', 'c']
        '[a[b], c'                                | ['[a[b]', 'c']
        '[a,b], c'                                | ['[a,b]', 'c']
        '$$a,b$$, c'                              | ['$$a', 'b$$', 'c']
    }

    void "HQL has no bracket identifiers or dollar quotes: brackets group an index expression, the rest is plain"() {
        expect:
        SqlText.hql('a[f(1, 2)], b').splitTopLevel(',' as char) == ['a[f(1, 2)]', 'b']
        SqlText.hql("[a'b], c").splitTopLevel(',' as char) == ["[a'b], c"]
        SqlText.hql('$$a,b$$, c').splitTopLevel(',' as char) == ['$$a', 'b$$', 'c']
        SqlText.of("[a'b], c", HQL).splitTopLevel(',' as char) == SqlText.hql("[a'b], c").splitTopLevel(',' as char)
    }

    @Unroll
    void "the keyword '#keyword' is found at the top level of '#text' at #index"() {
        expect:
        SqlText.hql(text).indexOfTopLevel(keyword, 0) == index

        where:
        text                                                  | keyword  | index
        'select a from B'                                     | 'select '| 0
        'select a from B'                                     | ' from ' | 8
        'from B where x in (select y from C)'                 | 'select '| -1
        "from B where name = 'select '"                       | 'select '| -1
        'from B where name = "select "'                       | 'select '| -1
        'from B where name = `select `'                       | 'select '| -1
        'from B /* select a */ where x = 1'                   | 'select '| -1
        'from B -- select a\nwhere x = 1'                     | 'select '| -1
        'from B where preselect = 1'                          | 'select '| -1
        'from B where b.select = 1'                           | 'select '| -1
        "from B where name = 'it''s' and 1 = (select count(c) from C) select"  | 'select' | 61
        'with x as (select 1) select a from B'                | 'select '| 21
    }

    void "a search starts at the given index"() {
        expect:
        SqlText.hql('select a from B where b in (select c from D) and e = 1').indexOfTopLevel(' from ', 9) == -1
        SqlText.hql('select a from B order by a').indexOfTopLevel(' from ', 0) == 8
    }

    @Unroll
    void "'#text' without parentheses enclosing all of it is '#unwrapped'"() {
        expect:
        SqlText.hql(text).unwrapOuterParentheses() == unwrapped

        where:
        text                                                        | unwrapped
        'select b from B'                                           | 'select b from B'
        '(select b from B)'                                         | 'select b from B'
        '((select b from B))'                                       | 'select b from B'
        '( select b from B )'                                       | 'select b from B'
        "(select b from B where b.name <> ')')"                     | "select b from B where b.name <> ')'"
        '(/* ignore ) and ( */ select b from B)'                    | '/* ignore ) and ( */ select b from B'
        '(select b from B where b.a in (select a from A))'          | 'select b from B where b.a in (select a from A)'
        '(select b from B) union (select a from A)'                 | '(select b from B) union (select a from A)'
        '(select b from B'                                          | '(select b from B'
        '(select b from B))'                                        | '(select b from B))'
        '()'                                                        | ''
    }

    @Unroll
    void "the '?' placeholders of '#sql' are at #indexes in every SQL syntax"() {
        expect:
        SqlText.of(sql, SQL).plainIndexes('?' as char) == indexes
        SqlText.of(sql, TRANSACT_SQL).plainIndexes('?' as char) == indexes

        where:
        sql                                   | indexes
        'a = ?'                               | [4]
        "a = ? and b = '?' and c = ?"         | [4, 26]
        'a = ? /* ? */ and b = ?'             | [4, 22]
        'a = ? -- ?\nand b = ?'               | [4, 19]
        'a = (?)'                             | [5]
        'a = "?" and b = ?'                   | [16]
    }

    @Unroll
    void "the '?' placeholders of '#sql' are at #indexes where brackets hold arrays"() {
        expect:
        SqlText.of(sql, SQL).plainIndexes('?' as char) == indexes

        where:
        sql                                   | indexes
        'id = ANY(ARRAY[?, ?])'               | [15, 18]
        'id = ANY(ARRAY [CAST(? AS INTEGER)])'| [21]
        '"items"[?] = ?'                      | [8, 13]
        "a = ARRAY['?', ?][1]"                | [15]
        'a = ARRAY[1, 2][?]'                  | [16]
        'a = [?] and b = ?'                   | [5, 16]
        'a = $$?$$ and b = ?'                 | [18]
    }

    @Unroll
    void "the '?' placeholders of the Transact-SQL '#sql' are at #indexes, brackets quoting an identifier"() {
        expect:
        SqlText.of(sql, TRANSACT_SQL).plainIndexes('?' as char) == indexes

        where:
        sql                                   | indexes
        'a = [?] and b = ?'                   | [16]
        '[a[b] = ?'                           | [8]
        '[a?b] = ?'                           | [8]
        '[a]]?] = ?'                          | [9]
        '[a?'                                 | []
        'a = $$?$$ and b = ?'                 | [6, 18]
    }

    @Unroll
    void "'#sql' #description"() {
        expect:
        SqlText.of(sql, SQL).endsInLineComment() == ends

        where:
        sql                      | ends  | description
        'a = 1 -- done'          | true  | 'ends in a line comment'
        'a = 1 -- done\n'        | false | 'has its line comment closed by the newline'
        'a = 1 /* done */'       | false | 'ends in a block comment, which is closed'
        "a = '-- not'"           | false | 'has the dashes inside a literal'
        'a = 1'                  | false | 'has no comment'
    }

    @Unroll
    void "'#sql' without its trailing comments is '#expected'"() {
        expect:
        SqlText.of(sql, SQL).withoutTrailingComments() == expected

        where:
        sql                                   | expected
        'width as w /* a comment */'          | 'width as w'
        'width as w -- another comment\n'     | 'width as w'
        'width /* kept */ as w'               | 'width /* kept */ as w'
        "width as 'w -- not a comment'"       | "width as 'w -- not a comment'"
        'width as w   '                       | 'width as w'
    }

    @Unroll
    void "'#column' without the alias '#alias' is '#expected' in every SQL syntax"() {
        expect:
        SqlText.of(column, SQL).withoutTrailingAlias(alias) == expected
        SqlText.of(column, TRANSACT_SQL).withoutTrailingAlias(alias) == expected

        where:
        column                          | alias          | expected
        'sum(height) as total'          | 'total'        | 'sum(height)'
        'sum(height) AS Total'          | 'total'        | 'sum(height)'
        'sum(height)  as  total  '      | 'total'        | 'sum(height)'
        'sum(height) as "total"'        | 'total'        | 'sum(height)'
        'sum(height) as `total`'        | 'total'        | 'sum(height)'
        'sum(height) as "total height"' | 'total height' | 'sum(height)'
        'sum(height) as "say ""hi"""'   | 'say "hi"'     | 'sum(height)'
        'sum(height) as total /* c */'  | 'total'        | 'sum(height)'
        'sum(height) as total -- c'     | 'total'        | 'sum(height)'
        'cast(width as integer) as w'   | 'w'            | 'cast(width as integer)'
        'cast(width as integer)'        | 'integer'      | 'cast(width as integer)'
        'sum(height) as total'          | 'other'        | 'sum(height) as total'
        'sum(height) as "total height"' | 'total'        | 'sum(height) as "total height"'
        'sum(height) as "total]'        | 'total'        | 'sum(height) as "total]'
        "sum(height) as 'total'"        | 'total'        | "sum(height) as 'total'"
        'sum(height) has total'         | 'total'        | 'sum(height) has total'
        'as total'                      | 'total'        | 'as total'
        'width'                         | 'width'        | 'width'
        ''                              | 'width'        | ''
    }

    void "a bracket alias is removed only where brackets quote an identifier"() {
        expect:
        SqlText.of('sum(height) as [total height]', TRANSACT_SQL).withoutTrailingAlias('total height') == 'sum(height)'
        SqlText.of('sum(height) as [a]]b]', TRANSACT_SQL).withoutTrailingAlias('a]b') == 'sum(height)'
        SqlText.of('sum(height) as [total height]', SQL).withoutTrailingAlias('total height') == 'sum(height) as [total height]'
        SqlText.of('sum(height) as [total]', SQL).withoutTrailingAlias('total') == 'sum(height) as [total]'
    }

    @Unroll
    void "'#identifier' unquoted is '#expected' in every SQL syntax"() {
        expect:
        SqlText.of(identifier, SQL).unquoted() == expected
        SqlText.of(identifier, TRANSACT_SQL).unquoted() == expected

        where:
        identifier        | expected
        '"total"'         | 'total'
        '`total`'         | 'total'
        '"say ""hi"""'    | 'say "hi"'
        'total'           | 'total'
        "'total'"         | "'total'"
        '"total'          | '"total'
        '"a" "b"'         | '"a" "b"'
        '""'              | ''
        '"'               | '"'
        ''                | ''
    }

    void "a bracket identifier is unquoted only where brackets quote an identifier"() {
        expect:
        SqlText.of('[total]', TRANSACT_SQL).unquoted() == 'total'
        SqlText.of('[a]]b]', TRANSACT_SQL).unquoted() == 'a]b'
        SqlText.of('[total', TRANSACT_SQL).unquoted() == '[total'
        SqlText.of('[total]', SQL).unquoted() == '[total]'
    }
}
