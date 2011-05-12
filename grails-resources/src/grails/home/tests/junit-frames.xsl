<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0" xmlns:lxslt="http://xml.apache.org/xslt" xmlns:redirect="http://xml.apache.org/xalan/redirect" xmlns:stringutils="xalan://org.apache.tools.ant.util.StringUtils" extension-element-prefixes="redirect">
    <xsl:output method="html" encoding="utf-8" indent="yes" />
    <xsl:decimal-format decimal-separator="." grouping-separator="," />
    <!-- Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); 
        you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES 
        OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License. -->

    <!-- Sample stylesheet to be used with Ant JUnitReport output. It creates a set of HTML files a la javadoc where you can browse easily through all packages and classes. -->
    <xsl:param name="output.dir" select="'.'" />
    <xsl:param name="TITLE">
        Unit Test Results
    </xsl:param>


    <xsl:template match="testsuites">

        <!-- create the all.html -->
        <redirect:write file="{$output.dir}/all.html">
            <xsl:call-template name="all.html" />
        </redirect:write>

        <!-- create the failed.html this will include tests with errors as well as failures -->
        <redirect:write file="{$output.dir}/failed.html">
            <xsl:call-template name="failed.html" />
        </redirect:write>

        <!-- create the overview.html -->
        <redirect:write file="{$output.dir}/index.html">
            <xsl:call-template name="index.html" />
        </redirect:write>

        <!-- generate individual reports per test case -->
        <xsl:for-each select="./testsuite[not(./@package = preceding-sibling::testsuite/@package)]">
            <xsl:call-template name="package">
                <xsl:with-param name="name" select="@package" />
            </xsl:call-template>
        </xsl:for-each>

        <!-- create the stylesheet.css -->
        <redirect:write file="{$output.dir}/stylesheet.css">
            <xsl:call-template name="stylesheet.css" />
        </redirect:write>

        <!-- create the boilerplate.css -->
        <redirect:write file="{$output.dir}/boilerplate.css">
            <xsl:call-template name="boilerplate.css" />
        </redirect:write>

        <!-- create the jquery.js -->
        <redirect:write file="{$output.dir}/jquery.js">
            <xsl:call-template name="jquery.js" />
        </redirect:write>

    </xsl:template>


    <!-- Process each package -->
    <xsl:template name="package">
        <xsl:param name="name" />
        <xsl:variable name="package.dir">
            <xsl:if test="not($name = '')">
                <xsl:value-of select="translate($name,'.','/')" />
            </xsl:if>
            <xsl:if test="$name = ''">
                <xsl:value-of select="$name" />
            </xsl:if>
        </xsl:variable>



        <xsl:for-each select="/testsuites/testsuite[@package = $name]">
            <xsl:if test="$package.dir = ''">
                <redirect:write file="{$output.dir}/{@id}_{@name}.html">
                    <xsl:apply-templates select="." mode="testsuite.page" />
                </redirect:write>
            </xsl:if>
            <xsl:if test="not($package.dir = '')">
                <redirect:write file="{$output.dir}/{$package.dir}/{@id}_{@name}.html">
                    <xsl:apply-templates select="." mode="testsuite.page" />
                </redirect:write>
            </xsl:if>
        </xsl:for-each>
    </xsl:template>

    <!-- One file per test suite / class -->
    <xsl:template match="testsuite" name="testsuite" mode="testsuite.page">
        <xsl:text disable-output-escaping='yes'>&lt;!DOCTYPE html></xsl:text>
        <html>
            <head>
                <title>
                    <xsl:value-of select="@name" />
                </title>
                <xsl:call-template name="create.resource.links">
                    <xsl:with-param name="package.name" select="@package" />
                </xsl:call-template>
            </head>
            <body>

                <div id="report">
                    <xsl:call-template name="navigation.links">
                        <xsl:with-param name="package.name" select="@package" />
                    </xsl:call-template>

                    <hgroup>
                        <xsl:call-template name="create.logo.link">
                            <xsl:with-param name="package.name" select="@package" />
                        </xsl:call-template>

                        <h1>
                            <xsl:value-of select="@name" />
                        </h1>
                        <h2>
                            Package:
                            <xsl:value-of select="@package" />
                        </h2>
                    </hgroup>

                    <xsl:apply-templates select="." mode="summary">
                        <xsl:sort select="@errors + @failures" data-type="number" order="descending" />
                        <xsl:sort select="@name" />
                    </xsl:apply-templates>
                </div>

                <xsl:call-template name="output.parser.js" />

            </body>
        </html>
    </xsl:template>


    <!-- This will produce a large file containing failed (including errors) tests -->
    <xsl:template name="failed.html" match="testsuites" mode="all.tests">
        <xsl:text disable-output-escaping='yes'>&lt;!DOCTYPE html></xsl:text>
        <html>
            <head>
                <title>
                    <xsl:value-of select="$TITLE" />
                    - Failed tests
                </title>
                <link href="boilerplate.css" rel="stylesheet" type="text/css" />
                <link href="stylesheet.css" rel="stylesheet" type="text/css" />
            </head>
            <body>

                <div id="report">
                    <xsl:call-template name="navigation.links">
                        <xsl:with-param name="package.name" select="''" />
                    </xsl:call-template>

                    <div class="grailslogo"></div>
                    <hgroup class="clearfix">
                        <h1>
                            <xsl:value-of select="$TITLE" />
                            - Failed tests
                        </h1>

                        <p class="intro">
                            <xsl:call-template name="test.count.summary">
                                <xsl:with-param name="tests" select="sum(testsuite/@tests)" />
                                <xsl:with-param name="errors" select="sum(testsuite/@errors)" />
                                <xsl:with-param name="failures" select="sum(testsuite/@failures)" />
                            </xsl:call-template>
                        </p>
                    </hgroup>

                    <xsl:apply-templates select="testsuite[@errors &gt; 0 or @failures &gt; 0]" mode="summary">
                        <xsl:sort select="@errors + @failures" data-type="number" order="descending" />
                        <xsl:sort select="@name" />
                    </xsl:apply-templates>
                </div>

                <script language="javascript" src="jquery.js"></script>
                <xsl:call-template name="output.parser.js" />

            </body>
        </html>
    </xsl:template>

    <xsl:template name="all.html" match="testsuites" mode="all.tests">
        <xsl:text disable-output-escaping='yes'>&lt;!DOCTYPE html></xsl:text>
        <html>
            <head>
                <title>
                    <xsl:value-of select="$TITLE" />
                    - All tests
                </title>
                <link href="boilerplate.css" rel="stylesheet" type="text/css" />
                <link href="stylesheet.css" rel="stylesheet" type="text/css" />
            </head>
            <body>

                <div id="report">
                    <xsl:call-template name="navigation.links">
                        <xsl:with-param name="package.name" select="''" />
                    </xsl:call-template>

                    <div class="grailslogo"></div>

                    <hgroup class="clearfix">
                        <h1>
                            <xsl:value-of select="$TITLE" />
                            - All tests
                        </h1>

                        <p class="intro">
                            <xsl:call-template name="test.count.summary">
                                <xsl:with-param name="tests" select="sum(testsuite/@tests)" />
                                <xsl:with-param name="errors" select="sum(testsuite/@errors)" />
                                <xsl:with-param name="failures" select="sum(testsuite/@failures)" />
                            </xsl:call-template>
                        </p>
                    </hgroup>

                    <xsl:apply-templates select="testsuite" mode="summary">
                        <xsl:sort select="@errors + @failures" data-type="number" order="descending" />
                        <xsl:sort select="@name" />
                    </xsl:apply-templates>
                </div>

                <script language="javascript" src="jquery.js"></script>
                <xsl:call-template name="output.parser.js" />

            </body>
        </html>
    </xsl:template>


    <!-- Produces a file with a package / test case summary with links to more detailed per-test case reports. -->
    <xsl:template name="index.html" match="testsuites" mode="all.tests">
        <xsl:text disable-output-escaping='yes'>&lt;!DOCTYPE html></xsl:text>
        <html>
            <head>
                <title>
                    <xsl:value-of select="$TITLE" />
                    - Package summary
                </title>
                <link href="boilerplate.css" rel="stylesheet" type="text/css" />
                <link href="stylesheet.css" rel="stylesheet" type="text/css" />
            </head>
            <body>

                <div id="report">
                    <xsl:call-template name="navigation.links">
                        <xsl:with-param name="package.name" select="''" />
                    </xsl:call-template>

                    <div class="grailslogo"></div>

                    <hgroup class="clearfix">
                        <h1>
                            <xsl:value-of select="$TITLE" />
                            - Summary
                        </h1>

                        <p class="intro">
                            <xsl:call-template name="test.count.summary">
                                <xsl:with-param name="tests" select="sum(testsuite/@tests)" />
                                <xsl:with-param name="errors" select="sum(testsuite/@errors)" />
                                <xsl:with-param name="failures" select="sum(testsuite/@failures)" />
                            </xsl:call-template>
                        </p>
                    </hgroup>

                    <xsl:for-each select="./testsuite[not(./@package = preceding-sibling::testsuite/@package)]">
                        <xsl:sort select="@errors + @failures" data-type="number" order="descending" />
                        <xsl:sort select="../@name" />

                        <xsl:call-template name="packages.overview">
                            <xsl:with-param name="packageName" select="@package" />
                        </xsl:call-template>
                    </xsl:for-each>
                </div>

            </body>
        </html>
    </xsl:template>


    <!-- A list of all packages and their test cases -->
    <xsl:template name="packages.overview">
        <xsl:param name="packageName" />

        <xsl:variable name="sumTime" select="sum(/testsuites/testsuite[@package = $packageName]/@time)" />
        <xsl:variable name="testCount" select="sum(/testsuites/testsuite[@package = $packageName]/@tests)" />
        <xsl:variable name="errorCount" select="sum(/testsuites/testsuite[@package = $packageName]/@errors)" />
        <xsl:variable name="failureCount" select="sum(/testsuites/testsuite[@package = $packageName]/@failures)" />
        <xsl:variable name="successCount" select="$testCount - $errorCount - $failureCount" />

        <xsl:variable name="cssclass">
            <xsl:choose>
                <xsl:when test="$failureCount &gt; 0 and $errorCount = 0">
                    failure
                </xsl:when>
                <xsl:when test="$errorCount &gt; 0">
                    error
                </xsl:when>
                <xsl:otherwise>
                    success
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <div>
            <xsl:attribute name="class">testsuite <xsl:value-of select="$cssclass" /></xsl:attribute>

            <header>
                <h2>
                    <xsl:value-of select="$packageName" />
                </h2>
                <h3>
                    <xsl:call-template name="test.count.summary">
                        <xsl:with-param name="tests" select="$testCount" />
                        <xsl:with-param name="errors" select="$errorCount" />
                        <xsl:with-param name="failures" select="$failureCount" />
                    </xsl:call-template>
                </h3>
            </header>

            <ul class="clearfix">
                <xsl:for-each select="/testsuites/testsuite[@package = $packageName]">
                    <xsl:sort select="@name" />

                    <xsl:variable name="testcaseCssClass">
                        <xsl:choose>
                            <xsl:when test="count(testcase/error) &gt; 0">
                                error
                            </xsl:when>
                            <xsl:when test="count(testcase/failure) &gt; 0">
                                failure
                            </xsl:when>
                            <xsl:otherwise>
                                success
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:variable>

                    <li>
                        <xsl:attribute name="class">packagelink <xsl:value-of select="$testcaseCssClass" /></xsl:attribute>

                        <a>
                            <xsl:variable name="package.name" select="@package" />

                            <xsl:attribute name="href">
                            <xsl:if test="not($package.name='')">
                                <xsl:value-of select="translate($package.name,'.','/')" /><xsl:text>/</xsl:text>
                            </xsl:if><xsl:value-of select="@id" />_<xsl:value-of select="@name" /><xsl:text>.html</xsl:text>
                        </xsl:attribute>

                            <xsl:attribute name="title"><xsl:value-of select="@tests" /> tests executed in <xsl:value-of select="@time" /> seconds.</xsl:attribute>

                            <span>
                                <xsl:attribute name="class">icon <xsl:value-of select="$testcaseCssClass" /></xsl:attribute>
                            </span>
                            <xsl:value-of select="@name" />
                        </a>
                    </li>
                </xsl:for-each>
            </ul>
        </div>
    </xsl:template>


    <!-- Writes the test summary -->
    <xsl:template match="testsuite" mode="summary">
        <xsl:variable name="cssclass">
            <xsl:choose>
                <xsl:when test="@failures &gt; 0 and @errors = 0">
                    failure
                </xsl:when>
                <xsl:when test="@errors &gt; 0">
                    error
                </xsl:when>
                <xsl:otherwise>
                    success
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <div>
            <xsl:attribute name="class">testsuite <xsl:value-of select="$cssclass" /></xsl:attribute>

            <header>
                <h2>
                    <xsl:value-of select="@name" />
                </h2>
                <h3>
                    <xsl:call-template name="test.count.summary">
                        <xsl:with-param name="tests" select="@tests" />
                        <xsl:with-param name="errors" select="@errors" />
                        <xsl:with-param name="failures" select="@failures" />
                    </xsl:call-template>
                </h3>
            </header>

            <xsl:apply-templates select="testcase" mode="tableline">
            </xsl:apply-templates>

            <footer class="clearfix output">
                <div class="sysout">
                    <h2>Standard output</h2>
                    <pre>
                        <xsl:value-of select="system-out" />
                    </pre>
                </div>
                <div class="syserr">
                    <h2>System error</h2>
                    <pre>
                        <xsl:value-of select="system-err" />
                    </pre>
                </div>
            </footer>
        </div>
    </xsl:template>
    
    <!-- Test method -->
    <xsl:template match="testcase" mode="tableline">
        <xsl:variable name="cssclass">
            <xsl:choose>
                <xsl:when test="count(error) &gt; 0">
                    error
                </xsl:when>
                <xsl:when test="count(failure) &gt; 0">
                    failure
                </xsl:when>
                <xsl:otherwise>
                    success
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <div>
            <xsl:attribute name="class">testcase clearfix <xsl:value-of select="$cssclass" /></xsl:attribute>

            <div class="metadata">
                <p>
                    <span>
                        <xsl:attribute name="class">icon <xsl:value-of select="$cssclass" /></xsl:attribute>
                    </span>
                    <b>
                        <xsl:attribute name="class">testname message <xsl:value-of select="$cssclass" /></xsl:attribute>
                        <xsl:value-of select="@name" />
                    </b>
                </p>

                <p class="summary">
                    Executed in
                    <xsl:value-of select="@time" />
                    seconds.
                </p>
            </div>


            <div class="outputinfo">
                <xsl:apply-templates select="failure | error" mode="testcase.details" />
            </div>
        </div>
    </xsl:template>

    <!-- Test failure -->
    <xsl:template match="failure | error" mode="testcase.details">
        <div class="details">
            <p>
                <b class="message">
                    <xsl:value-of select="@message" />
                </b>
            </p>
            <pre>
                <xsl:value-of select="." />
            </pre>
        </div>
    </xsl:template>


    <!-- Test count summary, the number of executed tests, errors and failures -->
    <xsl:template name="test.count.summary">
        <xsl:param name="tests" />
        <xsl:param name="errors" />
        <xsl:param name="failures" />

        <xsl:choose>
            <xsl:when test="$tests = 0">
                No tests executed.
            </xsl:when>
            <xsl:otherwise>

                <!-- Test count -->
                <xsl:choose>
                    <xsl:when test="$tests = 1">
                        A single test executed
                    </xsl:when>
                    <xsl:otherwise>
                        Executed
                        <xsl:value-of select="$tests" />
                        tests
                    </xsl:otherwise>
                </xsl:choose>

                <!-- Error / failure count -->
                <xsl:choose>
                    <xsl:when test="$errors = 0 and $failures = 0">
                        without a single error or failure!
                    </xsl:when>
                    <xsl:when test="$errors &gt; 0 and $failures = 0">
                        with
                        <xsl:call-template name="plural.singular">
                            <xsl:with-param name="number" select="$errors" />
                            <xsl:with-param name="word" select="'error'" />
                        </xsl:call-template>
                    </xsl:when>
                    <xsl:when test="$errors = 0 and $failures &gt; 0">
                        with
                        <xsl:call-template name="plural.singular">
                            <xsl:with-param name="number" select="$failures" />
                            <xsl:with-param name="word" select="'failure'" />
                        </xsl:call-template>
                    </xsl:when>
                    <xsl:otherwise>
                        with
                        <xsl:call-template name="plural.singular">
                            <xsl:with-param name="number" select="$errors" />
                            <xsl:with-param name="word" select="'error'" />
                        </xsl:call-template>

                        and
                        <xsl:call-template name="plural.singular">
                            <xsl:with-param name="number" select="$failures" />
                            <xsl:with-param name="word" select="'failure'" />
                        </xsl:call-template>
                    </xsl:otherwise>
                </xsl:choose>

            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="plural.singular">
        <xsl:param name="number" />
        <xsl:param name="word" />

        <xsl:choose>
            <xsl:when test="$number = 0">
                zero <xsl:value-of select="$word" />s
            </xsl:when>
            <xsl:when test="$number = 1">
                one <xsl:value-of select="$word" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$number" /><xsl:text> </xsl:text><xsl:value-of select="$word" />s
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- this is the stylesheet css to use for nearly everything -->
    <xsl:template name="stylesheet.css">
        <![CDATA[
        #report {
            -o-border-radius: 8px;
            -moz-border-radius: 8px;
            -webkit-border-radius: 8px;
            border-radius: 8px;
    
            -moz-box-shadow: 0 0 8px #F5F5F5;
            -webkit-box-shadow: 0 0 8px #F5F5F5;
            box-shadow: 0 0 8px #F5F5F5;
    
            background-color: white;
            margin: 10px auto;
            max-width: 1200px;
            padding: 10px 15px;
            width: 70%;
        }
        
        @media screen and (max-width: 1200px) {
            #report {
                width: 90% !important;
            }
        }

        /* Navigation links between the various views
        - - - - - - - - - - - - - - - - - - - - - - */
        #navigationlinks {
            width: 300px;
            float: right;
            text-align: right;
        }

        #navigationlinks p {
            padding: 2px;
        }

        #navigationlinks a {
            font-size: 1.1em;
            color: #464F38;
        }

        #navigationlinks a:hover {
            color: #333;
        }

        /* Test suites
        - - - - - - */

        .testsuite {
            -moz-border-radius: 5px;
            -webkit-border-radius: 5px;
            border-radius: 5px;
    
            -moz-box-shadow: 0 0 4px #F8F8F8;
            -webkit-box-shadow: 0 0 4px #F8F8F8;
            box-shadow: 0 0 4px #F8F8F8;
    
            background: -moz-linear-gradient(center top , #F7F7F7, #FEFEFE);
            background: -webkit-gradient(linear, left top, left bottom, from(#F7F7F7), to(#FEFEFE));
            background: linear-gradient(center top , #F7F7F7, #FEFEFE);
    
            border: 1px solid #EEEEEE;
            margin: 20px 0;
            text-align: left;
            width: 100%;
        }

        .testsuite header {
            color: white;
            padding: 5px 7px;
            text-shadow: 0 0 4px rgba(0, 0, 0, 0.2);
            font-size: 1.3em;
    
            -moz-border-radius: 5px 5px 0 0;
            -webkit-border-radius: 5px 5px 0 0;
            border-radius: 5px 5px 0 0;
    
            -moz-box-shadow: 0 0 13px rgba(255, 255, 255, 0.3) inset;
            -webkit-box-shadow: 0 0 13px rgba(255, 255, 255, 0.3) inset;
            box-shadow: 0 0 13px rgba(255, 255, 255, 0.3) inset;
        }

        .testsuite.error header {
            background-color: #BC2F2F;
            background: -moz-linear-gradient(center top , #BC2F2F, #C96952);
            background: -webkit-gradient(linear, left top, left bottom, from(#BC2F2F), to(#C96952));
            background: linear-gradient(center top , #BC2F2F, #C96952);
    
            border-bottom: 1px solid #BE5B5B;
        }

        .testsuite.failure header {
            background-color: #FFB75B;
            background: -moz-linear-gradient(center top , #FFB75B, #E69814);
            background: -webkit-gradient(linear, left top, left bottom, from(#FFB75B), to(#E69814));
            background: linear-gradient(center top , #FFB75B, #E69814);
    
            border-bottom: 1px solid #CD912B;
        }

        .testsuite.success header {
            background-color: #A6CC3B;
            background: -moz-linear-gradient(center top , #A6CC3B, #CBD53B);
            background: -webkit-gradient(linear, left top, left bottom, from(#A6CC3B), to(#CBD53B));
            background: linear-gradient(center top , #A6CC3B, #CBD53B);
    
            border-bottom: 1px solid #C4D5B6;
        }

        .testsuite header h2, h3 {
            margin: 0;
            padding: 0;
        }

        .testsuite header h3 {
            font-size: 0.8em;
        }

        .testsuite .name {
            width: 50%;
        }

        .testsuite .time {
            width: 10%;
        }

        .testsuite .testcase {
            padding: 5px 0;
        }

        /* Link to individual test cases
        - - - - - - - - - - - - - - - - - */

        .packagelink {
            border: 1px solid transparent;
            float: left;
            font-size: 1.1em;
            list-style: none outside none;
            padding: 2px 7px 4px 7px;
            margin: 3px;
        }

        .packagelink:hover {
            -moz-border-radius: 3px 3px 3px 3px;
            background-color: #f9f9f9;
            border: 1px solid #ddd;
        }

        .packagelink a {
            color: blue;
            text-decoration: none;
            display: inline-block;
        }

        .packagelink.failure a {
            color: #FB6C00 !important;
        }

        .packagelink.error a {
            color: #DD0707 !important;
        }

        .packagelink.success a {
            color: #344804 !important;
        }

        .message {
            word-wrap: break-word; /* force line break for long test names wihtout white-space */
        }

        .testcase.error .message {
            color: #AA0E0E;
        }

        .testcase.failure .message {
            color: #FB6C00 !important;
        }

        .testsuite .testcase:nth-of-type(2n) {
            background-color: #F4F4F4;
            border-bottom: 1px solid #EEEEEE;
            border-top: 1px solid #EEEEEE;
        }

        .testcase .metadata {
            float: left;
            width: 30%;
        }

        .testcase .message {
            font-size: 1.1em;
            font-weight: bold;
        }

        .testcase p.summary {
            margin-left: 5px;
            font-size: 1em;
            color: #444;
        }

        .testcase .outputinfo {
            float: left;
            width: 69%;
        }

        .outputinfo p { margin-top: 9px; }

        /* output is parsed using javascript and not visible by default.
        I don't think that having a non-javascript fallback is important
        as most Grails developers won't be using IE 6 :D */

        .testsuite footer { display: none; }
        p { padding: 4px; }

        footer.output {
            -moz-border-radius: 0 0 5px 5px;
            -webkit-border-radius: 0 0 5px 5px;
            border-radius: 0 0 5px 5px;
    
            background: -moz-linear-gradient(center top , #F8F8F8, #F2F2F2);
            background: -webkit-gradient(linear, left top, left bottom, from(#F8F8F8), to(#F2F2F2));
            background: linear-gradient(center top , #F8F8F8, #F2F2F2);
    
            border-top: 1px solid #EEEEEE;
            margin-top: 10px;
        }

        footer.output h2 { padding: 5px 0 0 5px; }

        footer.output .sysout, .syserr {
            float: left;
            width: 50%;
        }

        footer.output pre { margin: 5px; }

        .errorMessage {
            color: #AA0E0E;
            font-size: 1em;
            font-weight: bold;
        }

        .errorMessage.failure { color: #FB6C00 !important; }

        body {
            background-color: #F8F8F8;
            color: #333333;
            font: 85% ubuntu,helvetica,sans-serif;
        }

        p.intro { font-size: 1.5em; }
        h1 { font-size: 2.5em; }

        a { color: #1A4491; text-decoration: none; }
        a:hover { }

        pre {
            -moz-border-radius: 5px;
            -webkit-border-radius: 5px;
            border-radius: 5px;
    
            margin-bottom: 8px;
            background-color: #FFFFFF;
            border: 1px solid #DEDEDE;
            font-family: Consolas, Monaco, monospace;
            font-size: 0.9em;
        }

        .grailslogo {
            background-image:
            url("data:image/png;base64,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");
            float: left;
            height: 66px;
            margin: 0 10px 5px 0;
            width: 63px;
        }

        /* icons
        - - - - */

        .icon {
            width: 16px;
            height: 16px;
            margin: 6px 4px 0;
            display: inline-block;
        }

        .icon.failure {
            background-image:
            url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAC0ElEQVQ4jUWTTWtcZRiGr+d93zPfZ2aSNtPUCUnJaGjNxjZZFAXF4EK6EveC4A8Q3Aj+A3+HS0FRKbTQkIXVTULBL2wcDUlsM1PnM2dmzpzMnPO+XZwWF/fq4b4XF88l7hMFAM6BOFioQe+/FfzCxxTLN1EiDPu/EVx8RSH/N9EU5wCT1uT/AQsOqPh3ady+w+a7sLIJgxF0juBwD37f+5mYt52QvBwwCGkRwGQCals+b30Em7fAacj14eoavLYF2ctvur2vYylQcMIUByaZWQD0jH1b8n21fB0eH0C7CdkcKA3+AlQXcYsNXH2NSfPkcanOGgIKQISt0YDtxL+MSwJQA9BtyLch34LeI5h3EAIoX2EWsRqOuTMZgdIZGAd8GiWg4gBxQ8gOoNKHageqbSiewfhPWC5hgzZo6PX4PFcCg4LzMTueATlvwWQRliuQtVDyQDyYWbAh5BSqd4rzYBKwrTXKAEwj8VQ1B3YKrT+gXofiBlTroEuQA3qz9CaQeB7RfJ5t98iYn76Dyqo7VapYs26aQskXIQygN0ohToCOhVkGu+ATBZpZOGx98y2R8Qwkwv1BN9qu1VYo2z782oSnDi4BAgTAM2Ba5aLcoNv/h1DYNQZUrg7Kcq/bHfPvsEgU1+HEwRFw+CJ/AUcQPSvSaoYctSMU6ofXNzXyYwOwcFE2j/pT7+arb2yw3ihTTlqIOwccVlWYqGVOT0J+OTjEGP20Ek1uoGQk+xvpJ7qCuX0cF+6dT21l6colXlm/SrmSB2AcRJwdtzh7MsDLmXjViz8wUXAXEeQzX3AOrHW8v+5v9yX/5WBqd+Zzi9IKBySJA2XwC3q/zOyLh8fDB1opEDBZlYoYA0bJQac7fq/q+zsul39njLrmQIrWPXFx/HDYCx8sLpkLT4HRKV+jJHVJ5GXEaS273zfbu7fyGRDhfjjjw/UllAIRQb1wQASeA7DsLsm5I4PqAAAAAElFTkSuQmCC");
        }
        .icon.error {
            background-image:
            url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAADKklEQVQ4jW2T208cdRxHz292Z3aGhQK7LBcxmkDLpYKNtqEJrrQl8aFqo6kmXtun/gO+a4x4SRsf1BBsTZqo6ENNTDS2WjGa2lIwVlqsEMrFsrUBuwvLpTA7y+zszNenGkXP8/mct48SETZzqvKuttqW5mTplrIG8QO1nsnMplM3Lj5vL45tdtU/Ax9FYy0d3Xvf2Lqn64DR2mxwKw3FIgQ++YWsNzU0fGbkwsWXj7hrE/8JfFpZu+/Rx/efitXUVOM4gAIJQABNga6DofNn6o/sN98OPHfEXv7+70BfZMvWg8nO4TrTStiLi6SBezSFbpWgEIqBMGeY1KytYlXF+X1lZfn0pZGHXirmJjWA+xJVb5VnFhOzV0aZaWoifLyXqXictau/sXZ1jGuRCOpoD9PNTcxdHqVu9XasMVZ5DIBjymqZqKx1fwlbMtjUJrZti4jIcjotlx/YLSNtD0p2bl5ERGzHkUst98uvYVN+Lot5PcpsC+0P6QeNYvHJMc/FWV/F0sIk9u7BLC3FPPAYFS88S+zuegRIHX2bma++IOV5BMVAyyIToWRIf7ogkrwZBCjfZ/H8OTQ7h7lzF6FoCZppks/lmHn1NUbf7OG2H7CEhg/kUBPhHCilYBofF0UCGD9zlujhQ+gVFSBC0baZODvACmCjmCMgD7iElFYQuZ5QijggBJjb22k60YcTCuHk8zgbG+QEGt/vRd+xkxw+BYQSpfAgpeX8YFDXNK8NofXeBna9+w45Q8dxC7h9xyn09pH3CqwpofPkSXbv6GAbQrnSio7Iee0DcSev+/6X7bpFeXaByE/DhEwTq/8T1nvfY72vF+vDjzGipXg/fIebmqRd05kXvj4h7rgSEQ6rSGOXFRnuKBSql0SIdT0MP54jCmiAg8C+brKDQ5QVi1zR9aVBz+/sF3caEUFEeFEZyf6S8ls3jajMgzh6iUisRqSqTlyzTDIgN0KW9BvRzCFldN/Z/etMzyhjW2sk8vojlvXE9upqc0t9PQQB6wsLTKczGwM5+/R4wXvlM3Gn/veNd3hKRVriYT0ZN60GEZ+VQmF2yfOGPhf32mb3L9WplnZ5n/+WAAAAAElFTkSuQmCC");
        }
        .icon.success {
            background-image:
            url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAMAAAAoLQ9TAAABMlBMVEX///8IkwBn+FkitRMGSAArvRxy9mQGRgAIZgARmQJJ2jqc/5AIlgAJmwAJnQANnwAZrQoLSAQUpQUHkwAVhQkUpAYXqwcKnwAHUgUBRQECTQARXQ8CTwEHWAQITwcEVAENXwoYdhIjgB8XeBAUdw4XdxAZfxEehRcFSgIFTAMidCAGVwMLYggQbA0SUxAWchIlfSBJj0Q6jzQogyM1jy9AvzYwoSQwsSREkj9Xy05mtF0UngYXlQsgoxMltxUvsCI3ySg9wjFBmTpJ2DlM1j9Rk0lW50pZ50pan1JaslFdnlZe6U9f11NgxFVl8Fhn9Vhq0GBv/mB022l4/2l50W9853GB7neE73iE8nmR5IiW5Y6f35ig5Zmj352l6p+q4qWu2qm33LS347O517a63bW917qYo5MyAAAAO3RSTlMABAUGCAgICQkJCQkLCwsLDBATFBY1PEGksrS3u7u8vb7NzdDR0dHR29zc3d3d3d3i7O309vj5+fn5/rsIIGkAAACPSURBVBgZfcHTEkIBAAXAk23btm3btv7/F6ppmlsPtQv8Q8I3vkmDTzzLra0GgWu7NMs1FQCxDA8C66mVyfpFgCScVABC+7mXzzkZgDR0PUblFMdhWCy4qACU6eluEwlux5WSh4knfby7Wu9njaqXhRdtorNYDuo+Gt4MsdGkH2CDoEvN3XR8Mpo5+EbGT3eZ2hQgcUQ3SwAAAABJRU5ErkJggg==");
        }

        ]]>
    </xsl:template>
    

    <!-- transform string like a.b.c to ../../../ @param path the path to transform into a descending directory path -->
    <xsl:template name="path">
        <xsl:param name="path" />
        <xsl:if test="contains($path,'.')">
            <xsl:text>../</xsl:text>
            <xsl:call-template name="path">
                <xsl:with-param name="path">
                    <xsl:value-of select="substring-after($path,'.')" />
                </xsl:with-param>
            </xsl:call-template>
        </xsl:if>
        <xsl:if test="not(contains($path,'.')) and not($path = '')">
            <xsl:text>../</xsl:text>
        </xsl:if>
    </xsl:template>


    <!-- create the link to the stylesheet based on the package name -->
    <xsl:template name="create.resource.links">
        <xsl:param name="package.name" />
        <link rel="shortcut icon" href="http://grails.org/images/favicon.ico" type="image/x-icon"></link>
        <link rel="stylesheet" type="text/css" title="Style">
            <xsl:attribute name="href"><xsl:if test="not($package.name = 'unnamed package')"><xsl:call-template name="path"><xsl:with-param name="path" select="$package.name" /></xsl:call-template></xsl:if>boilerplate.css</xsl:attribute>
        </link>
        <link rel="stylesheet" type="text/css" title="Style">
            <xsl:attribute name="href"><xsl:if test="not($package.name = 'unnamed package')"><xsl:call-template name="path"><xsl:with-param name="path" select="$package.name" /></xsl:call-template></xsl:if>stylesheet.css</xsl:attribute>
        </link>
        
        <script type="text/javascript">
            <xsl:attribute name="src"><xsl:if test="not($package.name = 'unnamed package')"><xsl:call-template name="path"><xsl:with-param name="path" select="$package.name" /></xsl:call-template></xsl:if>jquery.js</xsl:attribute>        
        </script>
    </xsl:template>

    <!-- create the link to the home page wrapped around the grails logo -->
    <xsl:template name="create.logo.link">
        <xsl:param name="package.name" />
        <a title="Home">
            <xsl:attribute name="href"><xsl:if test="not($package.name = 'unnamed package')"><xsl:call-template name="path"><xsl:with-param name="path" select="$package.name" /></xsl:call-template></xsl:if>index.html</xsl:attribute>
            <div class="grailslogo"></div>
        </a>
    </xsl:template>

    <!-- create the links for the various views -->
    <xsl:template name="navigation.links">
        <xsl:param name="package.name" />
        <nav id="navigationlinks">
            <p>
                <a>
                    <xsl:attribute name="href"><xsl:if test="not($package.name = 'unnamed package')"><xsl:call-template name="path"><xsl:with-param name="path" select="$package.name" /></xsl:call-template></xsl:if>failed.html</xsl:attribute>
                    Tests with failure and errors
                </a>
            </p>
            <p>
                <a>
                    <xsl:attribute name="href"><xsl:if test="not($package.name = 'unnamed package')"><xsl:call-template name="path"><xsl:with-param name="path" select="$package.name" /></xsl:call-template></xsl:if>index.html</xsl:attribute>
                    Package summary
                </a>
            </p>
            <p>
                <a>
                    <xsl:attribute name="href"><xsl:if test="not($package.name = 'unnamed package')"><xsl:call-template name="path"><xsl:with-param name="path" select="$package.name" /></xsl:call-template></xsl:if>all.html</xsl:attribute>
                    Show all tests
                </a>
            </p>
        </nav>
    </xsl:template>

    <!-- template that will convert a carriage return into a br tag @param word the text from which to convert CR to BR tag -->
    <xsl:template name="br-replace">
        <xsl:param name="word" />
        <xsl:value-of disable-output-escaping="yes" select='stringutils:replace(string($word),"&#xA;","&lt;br/>")' />
    </xsl:template>

    <xsl:template name="display-time">
        <xsl:param name="value" />
        <xsl:value-of select="format-number($value,'0.000')" />
    </xsl:template>

    <xsl:template name="display-percent">
        <xsl:param name="value" />
        <xsl:value-of select="format-number($value,'0.00%')" />
    </xsl:template>


    <xsl:template name="output.parser.js">
        <xsl:comment>
            Parses JUnit output and associates it with the corresponding test case
        </xsl:comment>
        <script language="javascript">
<![CDATA[
    $(document).ready(function() {
    
        var addOutputToTest = function(header, name, output) {
            output = $.trim(output);
            if (output.length == 0) {
                return;
            }
        
            $(".testname").each(function() {
                if (name == $(this).text()) {
                    var testcase = $(this).parents(".testcase");
                    var outputInfo = $(testcase).find(".outputinfo");
                    $(outputInfo).append('<p><b class="message">' + header + '</b></p>');
                    
                    var preNode = document.createElement("pre");
                    preNode.append(document.createTextNode(output));
                    $(outputInfo).append(preNode);
                }
            });
        }
    
        $(".output").find("pre").each(function() {
            var header = $(this).parent().hasClass("sysout") ? "Output to standard out" : "Output to system error"
            var output = $(this).text().split("\n");
            
            var testName = null;
            var testOutput = "";
            for (var i=0; i < output.length; i++) {
                var line = output[i];
                var matches = line.match(/^--Output from (.*)--$/);
                if (matches !== null && matches.length == 2) {
                    if (testName !== null && testOutput.length > 0) {
                        addOutputToTest(header, testName, testOutput);
                        testOutput = "";
                    }
                    
                    testName = matches[1];
                } else {
                    if (testName !== null) {
                        testOutput += line + "\n";
                    }
                }
            }
            
            if (testName !== null && testOutput.length > 0) {
                addOutputToTest(header, testName, testOutput);
                testOutput = "";
            }
        });
        
    });
]]>
        </script>
    </xsl:template>


    <!-- HTML5 ✰ Boilerplate -->
    <xsl:template name="boilerplate.css">
        html, body, div, span, object, iframe,
        h1, h2, h3, h4, h5, h6, p, blockquote, pre,
        abbr, address, cite, code, del, dfn, em, img, ins, kbd, q, samp,
        small, strong, sub, sup, var, b, i, dl, dt, dd, ol, ul, li,
        fieldset, form, label, legend,
        table, caption, tbody, tfoot, thead, tr, th, td,
        article,
        aside, canvas, details, figcaption, figure,
        footer, header, hgroup, menu, nav, section, summary,
        time, mark, audio, video {
        margin: 0;
        padding: 0;
        border: 0;
        font-size: 100%;
        font: inherit;
        vertical-align: baseline;
        }

        article, aside, details, figcaption, figure,
        footer, header, hgroup, menu, nav,
        section {
        display: block;
        }

        blockquote, q { quotes: none; }
        blockquote:before, blockquote:after,
        q:before, q:after { content: ''; content: none; }
        ins { background-color: #ff9; color: #000; text-decoration: none; }
        mark { background-color: #ff9; color: #000; font-style: italic; font-weight: bold; }
        del { text-decoration: line-through; }
        abbr[title], dfn[title] { border-bottom: 1px dotted; cursor: help; }
        table { border-collapse: collapse; border-spacing: 0; }
        hr { display: block; height: 1px; border: 0; border-top: 1px solid #ccc; margin: 1em 0; padding: 0; }
        input, select {
        vertical-align: middle; }

        body { font:13px/1.231 sans-serif; *font-size:small; }
        select, input, textarea, button { font:99% sans-serif; }
        pre, code, kbd, samp { font-family: monospace, sans-serif; }

        html { overflow-y: scroll; }
        a:hover, a:active { outline: none; }
        ul, ol { }
        ol { list-style-type:
        decimal; }
        nav ul, nav li { margin: 0; list-style:none; list-style-image: none; }
        small { font-size: 85%; }
        strong, th { font-weight: bold; }
        td { vertical-align: top; }

        sub, sup { font-size: 75%; line-height: 0; position: relative; }
        sup { top: -0.5em; }
        sub { bottom: -0.25em; }

        pre { white-space:
        pre; white-space: pre-wrap; word-wrap: break-word; padding: 15px; }
        textarea { overflow: auto; }
        .ie6 legend, .ie7 legend { margin-left: -7px; }
        input[type="radio"] { vertical-align: text-bottom; }
        input[type="checkbox"] { vertical-align: bottom; }
        .ie7 input[type="checkbox"] { vertical-align:
        baseline; }
        .ie6 input { vertical-align: text-bottom; }
        label, input[type="button"], input[type="submit"], input[type="image"], button { cursor: pointer; }
        button, input, select, textarea { margin: 0; }
        input:valid, textarea:valid { }
        input:invalid, textarea:invalid { border-radius: 1px;
        -moz-box-shadow: 0px 0px 5px red; -webkit-box-shadow: 0px 0px 5px red; box-shadow: 0px 0px 5px red; }
        .no-boxshadow input:invalid, .no-boxshadow textarea:invalid { background-color: #f0dddd; }

        ::-moz-selection{ background: #FF9800; color:#fff; text-shadow: none; }
        ::selection { background:
        #FF9800; color:#fff; text-shadow: none; }
        a:link { -webkit-tap-highlight-color: #FF9800; }

        button { width: auto; overflow: visible; }
        .ie7 img { -ms-interpolation-mode: bicubic; }

        body, select, input, textarea { color: #444; }
        h1, h2, h3, h4, h5, h6 { font-weight: bold; }
        a, a:active, a:visited {
        color: #2C3545; }
        a:hover { color: #036; }


        /**
        * Primary styles
        *
        * Author:
        */
        .ir { display: block; text-indent: -999em; overflow: hidden; background-repeat: no-repeat; text-align: left; direction: ltr; }
        .hidden { display: none; visibility: hidden; }
        .visuallyhidden { border: 0; clip: rect(0 0 0
        0); height: 1px; margin: -1px; overflow: hidden; padding: 0; position: absolute; width: 1px; }
        .visuallyhidden.focusable:active,
        .visuallyhidden.focusable:focus { clip: auto; height: auto; margin: 0; overflow: visible; position: static; width: auto; }
        .invisible { visibility: hidden; }
        .clearfix:before, .clearfix:after { content: "\0020"; display: block; height: 0; overflow: hidden; }
        .clearfix:after { clear: both; }
        .clearfix { zoom: 1; }
    </xsl:template>

    <!-- Embedding jquery seems to make the xsl too large in some cases,
    this is a stripped down version of jQuery (without ajax support and so on) -->
    <xsl:template name="jquery.js">
        <xsl:text disable-output-escaping='yes'>
<![CDATA[
/*!
 * jQuery JavaScript Library v1.6.1pre
 * http://jquery.com/
 *
 * Copyright 2011, John Resig
 * Dual licensed under the MIT or GPL Version 2 licenses.
 * http://jquery.org/license
 *
 * Includes Sizzle.js
 * http://sizzlejs.com/
 * Copyright 2011, The Dojo Foundation
 * Released under the MIT, BSD, and GPL Licenses.
 *
 * Date: Wed May 11 14:12:19 2011 +0200
 */
(function(a,b){function bl(a,b,c){var d=b==="width"?bf:bg,e=b==="width"?a.offsetWidth:a.offsetHeight;if(c==="border")return e;f.each(d,function(){c||(e-=parseFloat(f.css(a,"padding"+this))||0),c==="margin"?e+=parseFloat(f.css(a,"margin"+this))||0:e-=parseFloat(f.css(a,"border"+this+"Width"))||0});return e}function X(a,b){b.src?f.ajax({url:b.src,async:!1,dataType:"script"}):f.globalEval((b.text||b.textContent||b.innerHTML||"").replace(P,"/*$0*/")),b.parentNode&&b.parentNode.removeChild(b)}function W(a){f.nodeName(a,"input")?V(a):a.getElementsByTagName&&f.grep(a.getElementsByTagName("input"),V)}function V(a){if(a.type==="checkbox"||a.type==="radio")a.defaultChecked=a.checked}function U(a){return"getElementsByTagName"in a?a.getElementsByTagName("*"):"querySelectorAll"in a?a.querySelectorAll("*"):[]}function T(a,b){var c;if(b.nodeType===1){b.clearAttributes&&b.clearAttributes(),b.mergeAttributes&&b.mergeAttributes(a),c=b.nodeName.toLowerCase();if(c==="object")b.outerHTML=a.outerHTML;else if(c!=="input"||a.type!=="checkbox"&&a.type!=="radio"){if(c==="option")b.selected=a.defaultSelected;else if(c==="input"||c==="textarea")b.defaultValue=a.defaultValue}else a.checked&&(b.defaultChecked=b.checked=a.checked),b.value!==a.value&&(b.value=a.value);b.removeAttribute(f.expando)}}function S(a,b){if(b.nodeType===1&&!!f.hasData(a)){var c=f.expando,d=f.data(a),e=f.data(b,d);if(d=d[c]){var g=d.events;e=e[c]=f.extend({},d);if(g){delete e.handle,e.events={};for(var h in g)for(var i=0,j=g[h].length;i<j;i++)f.event.add(b,h+(g[h][i].namespace?".":"")+g[h][i].namespace,g[h][i],g[h][i].data)}}}}function R(a,b){return f.nodeName(a,"table")?a.getElementsByTagName("tbody")[0]||a.appendChild(a.ownerDocument.createElement("tbody")):a}function F(a,b,c){b=b||0;if(f.isFunction(b))return f.grep(a,function(a,d){var e=!!b.call(a,d,a);return e===c});if(b.nodeType)return f.grep(a,function(a,d){return a===b===c});if(typeof b=="string"){var d=f.grep(a,function(a){return a.nodeType===1});if(A.test(b))return f.filter(b,d,!c);b=f.filter(b,d)}return f.grep(a,function(a,d){return f.inArray(a,b)>=0===c})}function E(a){return!a||!a.parentNode||a.parentNode.nodeType===11}function m(a,c,d){var e=c+"defer",g=c+"queue",h=c+"mark",i=f.data(a,e,b,!0);i&&(d==="queue"||!f.data(a,g,b,!0))&&(d==="mark"||!f.data(a,h,b,!0))&&setTimeout(function(){!f.data(a,g,b,!0)&&!f.data(a,h,b,!0)&&(f.removeData(a,e,!0),i.resolve())},0)}function l(a){for(var b in a)if(b!=="toJSON")return!1;return!0}function k(a,c,d){if(d===b&&a.nodeType===1){var e="data-"+c.replace(j,"$1-$2").toLowerCase();d=a.getAttribute(e);if(typeof d=="string"){try{d=d==="true"?!0:d==="false"?!1:d==="null"?null:f.isNaN(d)?i.test(d)?f.parseJSON(d):d:parseFloat(d)}catch(g){}f.data(a,c,d)}else d=b}return d}var c=a.document,d=a.navigator,e=a.location,f=function(){function H(){if(!e.isReady){try{c.documentElement.doScroll("left")}catch(a){setTimeout(H,1);return}e.ready()}}var e=function(a,b){return new e.fn.init(a,b,h)},f=a.jQuery,g=a.$,h,i=/^(?:[^<]*(<[\w\W]+>)[^>]*$|#([\w\-]*)$)/,j=/\S/,k=/^\s+/,l=/\s+$/,m=/\d/,n=/^<(\w+)\s*\/?>(?:<\/\1>)?$/,o=/^[\],:{}\s]*$/,p=/\\(?:["\\\/bfnrt]|u[0-9a-fA-F]{4})/g,q=/"[^"\\\n\r]*"|true|false|null|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?/g,r=/(?:^|:|,)(?:\s*\[)+/g,s=/(webkit)[ \/]([\w.]+)/,t=/(opera)(?:.*version)?[ \/]([\w.]+)/,u=/(msie) ([\w.]+)/,v=/(mozilla)(?:.*? rv:([\w.]+))?/,w=d.userAgent,x,y,z,A=Object.prototype.toString,B=Object.prototype.hasOwnProperty,C=Array.prototype.push,D=Array.prototype.slice,E=String.prototype.trim,F=Array.prototype.indexOf,G={};e.fn=e.prototype={constructor:e,init:function(a,d,f){var g,h,j,k;if(!a)return this;if(a.nodeType){this.context=this[0]=a,this.length=1;return this}if(a==="body"&&!d&&c.body){this.context=c,this[0]=c.body,this.selector=a,this.length=1;return this}if(typeof a=="string"){a.charAt(0)!=="<"||a.charAt(a.length-1)!==">"||a.length<3?g=i.exec(a):g=[null,a,null];if(g&&(g[1]||!d)){if(g[1]){d=d instanceof e?d[0]:d,k=d?d.ownerDocument||d:c,j=n.exec(a),j?e.isPlainObject(d)?(a=[c.createElement(j[1])],e.fn.attr.call(a,d,!0)):a=[k.createElement(j[1])]:(j=e.buildFragment([g[1]],[k]),a=(j.cacheable?e.clone(j.fragment):j.fragment).childNodes);return e.merge(this,a)}h=c.getElementById(g[2]);if(h&&h.parentNode){if(h.id!==g[2])return f.find(a);this.length=1,this[0]=h}this.context=c,this.selector=a;return this}return!d||d.jquery?(d||f).find(a):this.constructor(d).find(a)}if(e.isFunction(a))return f.ready(a);a.selector!==b&&(this.selector=a.selector,this.context=a.context);return e.makeArray(a,this)},selector:"",jquery:"1.6.1pre",length:0,size:function(){return this.length},toArray:function(){return D.call(this,0)},get:function(a){return a==null?this.toArray():a<0?this[this.length+a]:this[a]},pushStack:function(a,b,c){var d=this.constructor();e.isArray(a)?C.apply(d,a):e.merge(d,a),d.prevObject=this,d.context=this.context,b==="find"?d.selector=this.selector+(this.selector?" ":"")+c:b&&(d.selector=this.selector+"."+b+"("+c+")");return d},each:function(a,b){return e.each(this,a,b)},ready:function(a){e.bindReady(),y.done(a);return this},eq:function(a){return a===-1?this.slice(a):this.slice(a,+a+1)},first:function(){return this.eq(0)},last:function(){return this.eq(-1)},slice:function(){return this.pushStack(D.apply(this,arguments),"slice",D.call(arguments).join(","))},map:function(a){return this.pushStack(e.map(this,function(b,c){return a.call(b,c,b)}))},end:function(){return this.prevObject||this.constructor(null)},push:C,sort:[].sort,splice:[].splice},e.fn.init.prototype=e.fn,e.extend=e.fn.extend=function(){var a,c,d,f,g,h,i=arguments[0]||{},j=1,k=arguments.length,l=!1;typeof i=="boolean"&&(l=i,i=arguments[1]||{},j=2),typeof i!="object"&&!e.isFunction(i)&&(i={}),k===j&&(i=this,--j);for(;j<k;j++)if((a=arguments[j])!=null)for(c in a){d=i[c],f=a[c];if(i===f)continue;l&&f&&(e.isPlainObject(f)||(g=e.isArray(f)))?(g?(g=!1,h=d&&e.isArray(d)?d:[]):h=d&&e.isPlainObject(d)?d:{},i[c]=e.extend(l,h,f)):f!==b&&(i[c]=f)}return i},e.extend({noConflict:function(b){a.$===e&&(a.$=g),b&&a.jQuery===e&&(a.jQuery=f);return e},isReady:!1,readyWait:1,holdReady:function(a){a?e.readyWait++:e.ready(!0)},ready:function(a){if(a===!0&&!--e.readyWait||a!==!0&&!e.isReady){if(!c.body)return setTimeout(e.ready,1);e.isReady=!0;if(a!==!0&&--e.readyWait>0)return;y.resolveWith(c,[e]),e.fn.trigger&&e(c).trigger("ready").unbind("ready")}},bindReady:function(){if(!y){y=e._Deferred();if(c.readyState==="complete")return setTimeout(e.ready,1);if(c.addEventListener)c.addEventListener("DOMContentLoaded",z,!1),a.addEventListener("load",e.ready,!1);else if(c.attachEvent){c.attachEvent("onreadystatechange",z),a.attachEvent("onload",e.ready);var b=!1;try{b=a.frameElement==null}catch(d){}c.documentElement.doScroll&&b&&H()}}},isFunction:function(a){return e.type(a)==="function"},isArray:Array.isArray||function(a){return e.type(a)==="array"},isWindow:function(a){return a&&typeof a=="object"&&"setInterval"in a},isNaN:function(a){return a==null||!m.test(a)||isNaN(a)},type:function(a){return a==null?String(a):G[A.call(a)]||"object"},isPlainObject:function(a){if(!a||e.type(a)!=="object"||a.nodeType||e.isWindow(a))return!1;if(a.constructor&&!B.call(a,"constructor")&&!B.call(a.constructor.prototype,"isPrototypeOf"))return!1;var c;for(c in a);return c===b||B.call(a,c)},isEmptyObject:function(a){for(var b in a)return!1;return!0},error:function(a){throw a},parseJSON:function(b){if(typeof b!="string"||!b)return null;b=e.trim(b);if(a.JSON&&a.JSON.parse)return a.JSON.parse(b);if(o.test(b.replace(p,"@").replace(q,"]").replace(r,"")))return(new Function("return "+b))();e.error("Invalid JSON: "+b)},parseXML:function(b,c,d){a.DOMParser?(d=new DOMParser,c=d.parseFromString(b,"text/xml")):(c=new ActiveXObject("Microsoft.XMLDOM"),c.async="false",c.loadXML(b)),d=c.documentElement,(!d||!d.nodeName||d.nodeName==="parsererror")&&e.error("Invalid XML: "+b);return c},noop:function(){},globalEval:function(b){b&&j.test(b)&&(a.execScript||function(b){a.eval.call(a,b)})(b)},nodeName:function(a,b){return a.nodeName&&a.nodeName.toUpperCase()===b.toUpperCase()},each:function(a,c,d){var f,g=0,h=a.length,i=h===b||e.isFunction(a);if(d){if(i){for(f in a)if(c.apply(a[f],d)===!1)break}else for(;g<h;)if(c.apply(a[g++],d)===!1)break}else if(i){for(f in a)if(c.call(a[f],f,a[f])===!1)break}else for(;g<h;)if(c.call(a[g],g,a[g++])===!1)break;return a},trim:E?function(a){return a==null?"":E.call(a)}:function(a){return a==null?"":(a+"").replace(k,"").replace(l,"")},makeArray:function(a,b){var c=b||[];if(a!=null){var d=e.type(a);a.length==null||d==="string"||d==="function"||d==="regexp"||e.isWindow(a)?C.call(c,a):e.merge(c,a)}return c},inArray:function(a,b){if(F)return F.call(b,a);for(var c=0,d=b.length;c<d;c++)if(b[c]===a)return c;return-1},merge:function(a,c){var d=a.length,e=0;if(typeof c.length=="number")for(var f=c.length;e<f;e++)a[d++]=c[e];else while(c[e]!==b)a[d++]=c[e++];a.length=d;return a},grep:function(a,b,c){var d=[],e;c=!!c;for(var f=0,g=a.length;f<g;f++)e=!!b(a[f],f),c!==e&&d.push(a[f]);return d},map:function(a,c,d){var f,g,h=[],i=0,j=a.length,k=a instanceof e||j!==b&&typeof j=="number"&&(j>0&&a[0]&&a[j-1]||j===0||e.isArray(a));if(k)for(;i<j;i++)f=c(a[i],i,d),f!=null&&(h[h.length]=f);else for(g in a)f=c(a[g],g,d),f!=null&&(h[h.length]=f);return h.concat.apply([],h)},guid:1,proxy:function(a,c){if(typeof c=="string"){var d=a[c];c=a,a=d}if(!e.isFunction(a))return b;var f=D.call(arguments,2),g=function(){return a.apply(c,f.concat(D.call(arguments)))};g.guid=a.guid=a.guid||g.guid||e.guid++;return g},access:function(a,c,d,f,g,h){var i=a.length;if(typeof c=="object"){for(var j in c)e.access(a,j,c[j],f,g,d);return a}if(d!==b){f=!h&&f&&e.isFunction(d);for(var k=0;k<i;k++)g(a[k],c,f?d.call(a[k],k,g(a[k],c)):d,h);return a}return i?g(a[0],c):b},now:function(){return(new Date).getTime()},uaMatch:function(a){a=a.toLowerCase();var b=s.exec(a)||t.exec(a)||u.exec(a)||a.indexOf("compatible")<0&&v.exec(a)||[];return{browser:b[1]||"",version:b[2]||"0"}},sub:function(){function a(b,c){return new a.fn.init(b,c)}e.extend(!0,a,this),a.superclass=this,a.fn=a.prototype=this(),a.fn.constructor=a,a.sub=this.sub,a.fn.init=function(d,f){f&&f instanceof e&&!(f instanceof a)&&(f=a(f));return e.fn.init.call(this,d,f,b)},a.fn.init.prototype=a.fn;var b=a(c);return a},browser:{}}),e.each("Boolean Number String Function Array Date RegExp Object".split(" "),function(a,b){G["[object "+b+"]"]=b.toLowerCase()}),x=e.uaMatch(w),x.browser&&(e.browser[x.browser]=!0,e.browser.version=x.version),e.browser.webkit&&(e.browser.safari=!0),j.test(" ")&&(k=/^[\s\xA0]+/,l=/[\s\xA0]+$/),h=e(c),c.addEventListener?z=function(){c.removeEventListener("DOMContentLoaded",z,!1),e.ready()}:c.attachEvent&&(z=function(){c.readyState==="complete"&&(c.detachEvent("onreadystatechange",z),e.ready())});return e}(),g="done fail isResolved isRejected promise then always pipe".split(" "),h=[].slice;f.extend({_Deferred:function(){var a=[],b,c,d,e={done:function(){if(!d){var c=arguments,g,h,i,j,k;b&&(k=b,b=0);for(g=0,h=c.length;g<h;g++)i=c[g],j=f.type(i),j==="array"?e.done.apply(e,i):j==="function"&&a.push(i);k&&e.resolveWith(k[0],k[1])}return this},resolveWith:function(e,f){if(!d&&!b&&!c){f=f||[],c=1;try{while(a[0])a.shift().apply(e,f)}finally{b=[e,f],c=0}}return this},resolve:function(){e.resolveWith(this,arguments);return this},isResolved:function(){return!!c||!!b},cancel:function(){d=1,a=[];return this}};return e},Deferred:function(a){var b=f._Deferred(),c=f._Deferred(),d;f.extend(b,{then:function(a,c){b.done(a).fail(c);return this},always:function(){return b.done.apply(b,arguments).fail.apply(this,arguments)},fail:c.done,rejectWith:c.resolveWith,reject:c.resolve,isRejected:c.isResolved,pipe:function(a,c){return f.Deferred(function(d){f.each({done:[a,"resolve"],fail:[c,"reject"]},function(a,c){var e=c[0],g=c[1],h;f.isFunction(e)?b[a](function(){h=e.apply(this,arguments),h&&f.isFunction(h.promise)?h.promise().then(d.resolve,d.reject):d[g](h)}):b[a](d[g])})}).promise()},promise:function(a){if(a==null){if(d)return d;d=a={}}var c=g.length;while(c--)a[g[c]]=b[g[c]];return a}}),b.done(c.cancel).fail(b.cancel),delete b.cancel,a&&a.call(b,b);return b},when:function(a){function i(a){return function(c){b[a]=arguments.length>1?h.call(arguments,0):c,--e||g.resolveWith(g,h.call(b,0))}}var b=arguments,c=0,d=b.length,e=d,g=d<=1&&a&&f.isFunction(a.promise)?a:f.Deferred();if(d>1){for(;c<d;c++)b[c]&&f.isFunction(b[c].promise)?b[c].promise().then(i(c),g.reject):--e;e||g.resolveWith(g,b)}else g!==a&&g.resolveWith(g,d?[a]:[]);return g.promise()}}),f.support=function(){var a=c.createElement("div"),b=c.documentElement,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r;a.setAttribute("className","t"),a.innerHTML="   <link/><table></table><a href='/a' style='top:1px;float:left;opacity:.55;'>a</a><input type='checkbox'/>",d=a.getElementsByTagName("*"),e=a.getElementsByTagName("a")[0];if(!d||!d.length||!e)return{};f=c.createElement("select"),g=f.appendChild(c.createElement("option")),h=a.getElementsByTagName("input")[0],j={leadingWhitespace:a.firstChild.nodeType===3,tbody:!a.getElementsByTagName("tbody").length,htmlSerialize:!!a.getElementsByTagName("link").length,style:/top/.test(e.getAttribute("style")),hrefNormalized:e.getAttribute("href")==="/a",opacity:/^0.55$/.test(e.style.opacity),cssFloat:!!e.style.cssFloat,checkOn:h.value==="on",optSelected:g.selected,getSetAttribute:a.className!=="t",submitBubbles:!0,changeBubbles:!0,focusinBubbles:!1,deleteExpando:!0,noCloneEvent:!0,inlineBlockNeedsLayout:!1,shrinkWrapBlocks:!1,reliableMarginRight:!0},h.checked=!0,j.noCloneChecked=h.cloneNode(!0).checked,f.disabled=!0,j.optDisabled=!g.disabled;try{delete a.test}catch(s){j.deleteExpando=!1}!a.addEventListener&&a.attachEvent&&a.fireEvent&&(a.attachEvent("onclick",function b(){j.noCloneEvent=!1,a.detachEvent("onclick",b)}),a.cloneNode(!0).fireEvent("onclick")),h=c.createElement("input"),h.value="t",h.setAttribute("type","radio"),j.radioValue=h.value==="t",h.setAttribute("checked","checked"),a.appendChild(h),k=c.createDocumentFragment(),k.appendChild(a.firstChild),j.checkClone=k.cloneNode(!0).cloneNode(!0).lastChild.checked,a.innerHTML="",a.style.width=a.style.paddingLeft="1px",l=c.createElement("body"),m={visibility:"hidden",width:0,height:0,border:0,margin:0,background:"none"};for(q in m)l.style[q]=m[q];l.appendChild(a),b.insertBefore(l,b.firstChild),j.appendChecked=h.checked,j.boxModel=a.offsetWidth===2,"zoom"in a.style&&(a.style.display="inline",a.style.zoom=1,j.inlineBlockNeedsLayout=a.offsetWidth===2,a.style.display="",a.innerHTML="<div style='width:4px;'></div>",j.shrinkWrapBlocks=a.offsetWidth!==2),a.innerHTML="<table><tr><td style='padding:0;border:0;display:none'></td><td>t</td></tr></table>",n=a.getElementsByTagName("td"),r=n[0].offsetHeight===0,n[0].style.display="",n[1].style.display="none",j.reliableHiddenOffsets=r&&n[0].offsetHeight===0,a.innerHTML="",c.defaultView&&c.defaultView.getComputedStyle&&(i=c.createElement("div"),i.style.width="0",i.style.marginRight="0",a.appendChild(i),j.reliableMarginRight=(parseInt((c.defaultView.getComputedStyle(i,null)||{marginRight:0}).marginRight,10)||0)===0),l.innerHTML="",b.removeChild(l);if(a.attachEvent)for(q in{submit:1,change:1,focusin:1})p="on"+q,r=p in a,r||(a.setAttribute(p,"return;"),r=typeof a[p]=="function"),j[q+"Bubbles"]=r;return j}(),f.boxModel=f.support.boxModel;var i=/^(?:\{.*\}|\[.*\])$/,j=/([a-z])([A-Z])/g;f.extend({cache:{},uuid:0,expando:"jQuery"+(f.fn.jquery+Math.random()).replace(/\D/g,""),noData:{embed:!0,object:"clsid:D27CDB6E-AE6D-11cf-96B8-444553540000",applet:!0},hasData:function(a){a=a.nodeType?f.cache[a[f.expando]]:a[f.expando];return!!a&&!l(a)},data:function(a,c,d,e){if(!!f.acceptData(a)){var g=f.expando,h=typeof c=="string",i,j=a.nodeType,k=j?f.cache:a,l=j?a[f.expando]:a[f.expando]&&f.expando;if((!l||e&&l&&!k[l][g])&&h&&d===b)return;l||(j?a[f.expando]=l=++f.uuid:l=f.expando),k[l]||(k[l]={},j||(k[l].toJSON=f.noop));if(typeof c=="object"||typeof c=="function")e?k[l][g]=f.extend(k[l][g],c):k[l]=f.extend(k[l],c);i=k[l],e&&(i[g]||(i[g]={}),i=i[g]),d!==b&&(i[f.camelCase(c)]=d);if(c==="events"&&!i[c])return i[g]&&i[g].events;return h?i[f.camelCase(c)]:i}},removeData:function(b,c,d){if(!!f.acceptData(b)){var e=f.expando,g=b.nodeType,h=g?f.cache:b,i=g?b[f.expando]:f.expando;if(!h[i])return;if(c){var j=d?h[i][e]:h[i];if(j){delete j[c];if(!l(j))return}}if(d){delete h[i][e];if(!l(h[i]))return}var k=h[i][e];f.support.deleteExpando||h!=a?delete h[i]:h[i]=null,k?(h[i]={},g||(h[i].toJSON=f.noop),h[i][e]=k):g&&(f.support.deleteExpando?delete b[f.expando]:b.removeAttribute?b.removeAttribute(f.expando):b[f.expando]=null)}},_data:function(a,b,c){return f.data(a,b,c,!0)},acceptData:function(a){if(a.nodeName){var b=f.noData[a.nodeName.toLowerCase()];if(b)return b!==!0&&a.getAttribute("classid")===b}return!0}}),f.fn.extend({data:function(a,c){var d=null;if(typeof a=="undefined"){if(this.length){d=f.data(this[0]);if(this[0].nodeType===1){var e=this[0].attributes,g;for(var h=0,i=e.length;h<i;h++)g=e[h].name,g.indexOf("data-")===0&&(g=f.camelCase(g.substring(5)),k(this[0],g,d[g]))}}return d}if(typeof a=="object")return this.each(function(){f.data(this,a)});var j=a.split(".");j[1]=j[1]?"."+j[1]:"";if(c===b){d=this.triggerHandler("getData"+j[1]+"!",[j[0]]),d===b&&this.length&&(d=f.data(this[0],a),d=k(this[0],a,d));return d===b&&j[1]?this.data(j[0]):d}return this.each(function(){var b=f(this),d=[j[0],c];b.triggerHandler("setData"+j[1]+"!",d),f.data(this,a,c),b.triggerHandler("changeData"+j[1]+"!",d)})},removeData:function(a){return this.each(function(){f.removeData(this,a)})}}),f.extend({_mark:function(a,c){a&&(c=(c||"fx")+"mark",f.data(a,c,(f.data(a,c,b,!0)||0)+1,!0))},_unmark:function(a,c,d){a!==!0&&(d=c,c=a,a=!1);if(c){d=d||"fx";var e=d+"mark",g=a?0:(f.data(c,e,b,!0)||1)-1;g?f.data(c,e,g,!0):(f.removeData(c,e,!0),m(c,d,"mark"))}},queue:function(a,c,d){if(a){c=(c||"fx")+"queue";var e=f.data(a,c,b,!0);d&&(!e||f.isArray(d)?e=f.data(a,c,f.makeArray(d),!0):e.push(d));return e||[]}},dequeue:function(a,b){b=b||"fx";var c=f.queue(a,b),d=c.shift(),e;d==="inprogress"&&(d=c.shift()),d&&(b==="fx"&&c.unshift("inprogress"),d.call(a,function(){f.dequeue(a,b)})),c.length||(f.removeData(a,b+"queue",!0),m(a,b,"queue"))}}),f.fn.extend({queue:function(a,c){typeof a!="string"&&(c=a,a="fx");if(c===b)return f.queue(this[0],a);return this.each(function(){var b=f.queue(this,a,c);a==="fx"&&b[0]!=="inprogress"&&f.dequeue(this,a)})},dequeue:function(a){return this.each(function(){f.dequeue(this,a)})},delay:function(a,b){a=f.fx?f.fx.speeds[a]||a:a,b=b||"fx";return this.queue(b,function(){var c=this;setTimeout(function(){f.dequeue(c,b)},a)})},clearQueue:function(a){return this.queue(a||"fx",[])},promise:function(a,c){function m(){--h||d.resolveWith(e,[e])}typeof a!="string"&&(c=a,a=b),a=a||"fx";var d=f.Deferred(),e=this,g=e.length,h=1,i=a+"defer",j=a+"queue",k=a+"mark",l;while(g--)if(l=f.data(e[g],i,b,!0)||(f.data(e[g],j,b,!0)||f.data(e[g],k,b,!0))&&f.data(e[g],i,f._Deferred(),!0))h++,l.done(m);m();return d.promise()}});var n=/[\n\t\r]/g,o=/\s+/,p=/\r/g,q=/^(?:button|input)$/i,r=/^(?:button|input|object|select|textarea)$/i,s=/^a(?:rea)?$/i,t=/^(?:autofocus|autoplay|async|checked|controls|defer|disabled|hidden|loop|multiple|open|readonly|required|scoped|selected)$/i,u=/\:/,v,w;f.fn.extend({attr:function(a,b){return f.access(this,a,b,!0,f.attr)},removeAttr:function(a){return this.each(function(){f.removeAttr(this,a)})},prop:function(a,b){return f.access(this,a,b,!0,f.prop)},removeProp:function(a){a=f.propFix[a]||a;return this.each(function(){try{this[a]=b,delete this[a]}catch(c){}})},addClass:function(a){if(f.isFunction(a))return this.each(function(b){var c=f(this);c.addClass(a.call(this,b,c.attr("class")||""))});if(a&&typeof a=="string"){var b=(a||"").split(o);for(var c=0,d=this.length;c<d;c++){var e=this[c];if(e.nodeType===1)if(!e.className)e.className=a;else{var g=" "+e.className+" ",h=e.className;for(var i=0,j=b.length;i<j;i++)g.indexOf(" "+b[i]+" ")<0&&(h+=" "+b[i]);e.className=f.trim(h)}}}return this},removeClass:function(a){if(f.isFunction(a))return this.each(function(b){var c=f(this);c.removeClass(a.call(this,b,c.attr("class")))});if(a&&typeof a=="string"||a===b){var c=(a||"").split(o);for(var d=0,e=this.length;d<e;d++){var g=this[d];if(g.nodeType===1&&g.className)if(a){var h=(" "+g.className+" ").replace(n," ");for(var i=0,j=c.length;i<j;i++)h=h.replace(" "+c[i]+" "," ");g.className=f.trim(h)}else g.className=""}}return this},toggleClass:function(a,b){var c=typeof a,d=typeof b=="boolean";if(f.isFunction(a))return this.each(function(c){var d=f(this);d.toggleClass(a.call(this,c,d.attr("class"),b),b)});return this.each(function(){if(c==="string"){var e,g=0,h=f(this),i=b,j=a.split(o);while(e=j[g++])i=d?i:!h.hasClass(e),h[i?"addClass":"removeClass"](e)}else if(c==="undefined"||c==="boolean")this.className&&f._data(this,"__className__",this.className),this.className=this.className||a===!1?"":f._data(this,"__className__")||""})},hasClass:function(a){var b=" "+a+" ";for(var c=0,d=this.length;c<d;c++)if((" "+this[c].className+" ").replace(n," ").indexOf(b)>-1)return!0;return!1},val:function(a){var c,d,e=this[0];if(!arguments.length){if(e){c=f.valHooks[e.nodeName.toLowerCase()]||f.valHooks[e.type];if(c&&"get"in c&&(d=c.get(e,"value"))!==b)return d;return(e.value||"").replace(p,"")}return b}var g=f.isFunction(a);return this.each(function(d){var e=f(this),h;if(this.nodeType===1){g?h=a.call(this,d,e.val()):h=a,h==null?h="":typeof h=="number"?h+="":f.isArray(h)&&(h=f.map(h,function(a){return a==null?"":a+""})),c=f.valHooks[this.nodeName.toLowerCase()]||f.valHooks[this.type];if(!c||!("set"in c)||c.set(this,h,"value")===b)this.value=h}})}}),f.extend({valHooks:{option:{get:function(a){var b=a.attributes.value;return!b||b.specified?a.value:a.text}},select:{get:function(a){var b,c=a.selectedIndex,d=[],e=a.options,g=a.type==="select-one";if(c<0)return null;for(var h=g?c:0,i=g?c+1:e.length;h<i;h++){var j=e[h];if(j.selected&&(f.support.optDisabled?!j.disabled:j.getAttribute("disabled")===null)&&(!j.parentNode.disabled||!f.nodeName(j.parentNode,"optgroup"))){b=f(j).val();if(g)return b;d.push(b)}}if(g&&!d.length&&e.length)return f(e[c]).val();return d},set:function(a,b){var c=f.makeArray(b);f(a).find("option").each(function(){this.selected=f.inArray(f(this).val(),c)>=0}),c.length||(a.selectedIndex=-1);return c}}},attrFn:{val:!0,css:!0,html:!0,text:!0,data:!0,width:!0,height:!0,offset:!0},attrFix:{tabindex:"tabIndex"},attr:function(a,c,d,e){var g=a.nodeType;if(!a||g===3||g===8||g===2)return b;if(e&&c in f.attrFn)return f(a)[c](d);if(!("getAttribute"in a))return f.prop(a,c,d);var h,i,j=g!==1||!f.isXMLDoc(a);c=j&&f.attrFix[c]||c,i=f.attrHooks[c],i||(!t.test(c)||typeof d!="boolean"&&d!==b&&d.toLowerCase()!==c.toLowerCase()?v&&(f.nodeName(a,"form")||u.test(c))&&(i=v):i=w);if(d!==b){if(d===null){f.removeAttr(a,c);return b}if(i&&"set"in i&&j&&(h=i.set(a,d,c))!==b)return h;a.setAttribute(c,""+d);return d}if(i&&"get"in i&&j)return i.get(a,c);h=a.getAttribute(c);return h===null?b:h},removeAttr:function(a,b){var c;a.nodeType===1&&(b=f.attrFix[b]||b,f.support.getSetAttribute?a.removeAttribute(b):(f.attr(a,b,""),a.removeAttributeNode(a.getAttributeNode(b))),t.test(b)&&(c=f.propFix[b]||b)in a&&(a[c]=!1))},attrHooks:{type:{set:function(a,b){if(q.test(a.nodeName)&&a.parentNode)f.error("type property can't be changed");else if(!f.support.radioValue&&b==="radio"&&f.nodeName(a,"input")){var c=a.value;a.setAttribute("type",b),c&&(a.value=c);return b}}},tabIndex:{get:function(a){var c=a.getAttributeNode("tabIndex");return c&&c.specified?parseInt(c.value,10):r.test(a.nodeName)||s.test(a.nodeName)&&a.href?0:b}}},propFix:{tabindex:"tabIndex",readonly:"readOnly","for":"htmlFor","class":"className",maxlength:"maxLength",cellspacing:"cellSpacing",cellpadding:"cellPadding",rowspan:"rowSpan",colspan:"colSpan",usemap:"useMap",frameborder:"frameBorder",contenteditable:"contentEditable"},prop:function(a,c,d){var e=a.nodeType;if(!a||e===3||e===8||e===2)return b;var g,h,i=e!==1||!f.isXMLDoc(a);c=i&&f.propFix[c]||c,h=f.propHooks[c];return d!==b?h&&"set"in h&&(g=h.set(a,d,c))!==b?g:a[c]=d:h&&"get"in h&&(g=h.get(a,c))!==b?g:a[c]},propHooks:{}}),w={get:function(a,c){return a[f.propFix[c]||c]?c.toLowerCase():b},set:function(a,b,c){var d;b===!1?f.removeAttr(a,c):(d=f.propFix[c]||c,d in a&&(a[d]=b),a.setAttribute(c,c.toLowerCase()));return c}},f.attrHooks.value={get:function(a,b){if(v&&f.nodeName(a,"button"))return v.get(a,b);return a.value},set:function(a,b,c){if(v&&f.nodeName(a,"button"))return v.set(a,b,c);a.value=b}},f.support.getSetAttribute||(f.attrFix=f.propFix,v=f.attrHooks.name=f.valHooks.button={get:function(a,c){var d;d=a.getAttributeNode(c);return d&&d.nodeValue!==""?d.nodeValue:b},set:function(a,b,c){var d=a.getAttributeNode(c);if(d){d.nodeValue=b;return b}}},f.each(["width","height"],function(a,b){f.attrHooks[b]=f.extend(f.attrHooks[b],{set:function(a,c){if(c===""){a.setAttribute(b,"auto");return c}}})})),f.support.hrefNormalized||f.each(["href","src","width","height"],function(a,c){f.attrHooks[c]=f.extend(f.attrHooks[c],{get:function(a){var d=a.getAttribute(c,2);return d===null?b:d}})}),f.support.style||(f.attrHooks.style={get:function(a){return a.style.cssText.toLowerCase()||b},set:function(a,b){return a.style.cssText=""+b}}),f.support.optSelected||(f.propHooks.selected=f.extend(f.propHooks.selected,{get:function(a){var b=a.parentNode;b&&(b.selectedIndex,b.parentNode&&b.parentNode.selectedIndex)}})),f.support.checkOn||f.each(["radio","checkbox"],function(){f.valHooks[this]={get:function(a){return a.getAttribute("value")===null?"on":a.value}}}),f.each(["radio","checkbox"],function(){f.valHooks[this]=f.extend(f.valHooks[this],{set:function(a,b){if(f.isArray(b))return a.checked=f.inArray(f(a).val(),b)>=0}})}),function(){function u(a,b,c,d,e,f){for(var g=0,h=d.length;g<h;g++){var i=d[g];if(i){var j=!1;i=i[a];while(i){if(i.sizcache===c){j=d[i.sizset];break}if(i.nodeType===1){f||(i.sizcache=c,i.sizset=g);if(typeof b!="string"){if(i===b){j=!0;break}}else if(k.filter(b,[i]).length>0){j=i;break}}i=i[a]}d[g]=j}}}function t(a,b,c,d,e,f){for(var g=0,h=d.length;g<h;g++){var i=d[g];if(i){var j=!1;i=i[a];while(i){if(i.sizcache===c){j=d[i.sizset];break}i.nodeType===1&&!f&&(i.sizcache=c,i.sizset=g);if(i.nodeName.toLowerCase()===b){j=i;break}i=i[a]}d[g]=j}}}var a=/((?:\((?:\([^()]+\)|[^()]+)+\)|\[(?:\[[^\[\]]*\]|['"][^'"]*['"]|[^\[\]'"]+)+\]|\\.|[^ >+~,(\[\\]+)+|[>+~])(\s*,\s*)?((?:.|\r|\n)*)/g,d=0,e=Object.prototype.toString,g=!1,h=!0,i=/\\/g,j=/\W/;[0,0].sort(function(){h=!1;return 0});var k=function(b,d,f,g){f=f||[],d=d||c;var h=d;if(d.nodeType!==1&&d.nodeType!==9)return[];if(!b||typeof b!="string")return f;var i,j,n,o,q,r,s,t,u=!0,w=k.isXML(d),x=[],y=b;do{a.exec(""),i=a.exec(y);if(i){y=i[3],x.push(i[1]);if(i[2]){o=i[3];break}}}while(i);if(x.length>1&&m.exec(b))if(x.length===2&&l.relative[x[0]])j=v(x[0]+x[1],d);else{j=l.relative[x[0]]?[d]:k(x.shift(),d);while(x.length)b=x.shift(),l.relative[b]&&(b+=x.shift()),j=v(b,j)}else{!g&&x.length>1&&d.nodeType===9&&!w&&l.match.ID.test(x[0])&&!l.match.ID.test(x[x.length-1])&&(q=k.find(x.shift(),d,w),d=q.expr?k.filter(q.expr,q.set)[0]:q.set[0]);if(d){q=g?{expr:x.pop(),set:p(g)}:k.find(x.pop(),x.length===1&&(x[0]==="~"||x[0]==="+")&&d.parentNode?d.parentNode:d,w),j=q.expr?k.filter(q.expr,q.set):q.set,x.length>0?n=p(j):u=!1;while(x.length)r=x.pop(),s=r,l.relative[r]?s=x.pop():r="",s==null&&(s=d),l.relative[r](n,s,w)}else n=x=[]}n||(n=j),n||k.error(r||b);if(e.call(n)==="[object Array]")if(!u)f.push.apply(f,n);else if(d&&d.nodeType===1)for(t=0;n[t]!=null;t++)n[t]&&(n[t]===!0||n[t].nodeType===1&&k.contains(d,n[t]))&&f.push(j[t]);else for(t=0;n[t]!=null;t++)n[t]&&n[t].nodeType===1&&f.push(j[t]);else p(n,f);o&&(k(o,h,f,g),k.uniqueSort(f));return f};k.uniqueSort=function(a){if(r){g=h,a.sort(r);if(g)for(var b=1;b<a.length;b++)a[b]===a[b-1]&&a.splice(b--,1)}return a},k.matches=function(a,b){return k(a,null,null,b)},k.matchesSelector=function(a,b){return k(b,null,null,[a]).length>0},k.find=function(a,b,c){var d;if(!a)return[];for(var e=0,f=l.order.length;e<f;e++){var g,h=l.order[e];if(g=l.leftMatch[h].exec(a)){var j=g[1];g.splice(1,1);if(j.substr(j.length-1)!=="\\"){g[1]=(g[1]||"").replace(i,""),d=l.find[h](g,b,c);if(d!=null){a=a.replace(l.match[h],"");break}}}}d||(d=typeof b.getElementsByTagName!="undefined"?b.getElementsByTagName("*"):[]);return{set:d,expr:a}},k.filter=function(a,c,d,e){var f,g,h=a,i=[],j=c,m=c&&c[0]&&k.isXML(c[0]);while(a&&c.length){for(var n in l.filter)if((f=l.leftMatch[n].exec(a))!=null&&f[2]){var o,p,q=l.filter[n],r=f[1];g=!1,f.splice(1,1);if(r.substr(r.length-1)==="\\")continue;j===i&&(i=[]);if(l.preFilter[n]){f=l.preFilter[n](f,j,d,i,e,m);if(!f)g=o=!0;else if(f===!0)continue}if(f)for(var s=0;(p=j[s])!=null;s++)if(p){o=q(p,f,s,j);var t=e^!!o;d&&o!=null?t?g=!0:j[s]=!1:t&&(i.push(p),g=!0)}if(o!==b){d||(j=i),a=a.replace(l.match[n],"");if(!g)return[];break}}if(a===h)if(g==null)k.error(a);else break;h=a}return j},k.error=function(a){throw"Syntax error, unrecognized expression: "+a};var l=k.selectors={order:["ID","NAME","TAG"],match:{ID:/#((?:[\w\u00c0-\uFFFF\-]|\\.)+)/,CLASS:/\.((?:[\w\u00c0-\uFFFF\-]|\\.)+)/,NAME:/\[name=['"]*((?:[\w\u00c0-\uFFFF\-]|\\.)+)['"]*\]/,ATTR:/\[\s*((?:[\w\u00c0-\uFFFF\-]|\\.)+)\s*(?:(\S?=)\s*(?:(['"])(.*?)\3|(#?(?:[\w\u00c0-\uFFFF\-]|\\.)*)|)|)\s*\]/,TAG:/^((?:[\w\u00c0-\uFFFF\*\-]|\\.)+)/,CHILD:/:(only|nth|last|first)-child(?:\(\s*(even|odd|(?:[+\-]?\d+|(?:[+\-]?\d*)?n\s*(?:[+\-]\s*\d+)?))\s*\))?/,POS:/:(nth|eq|gt|lt|first|last|even|odd)(?:\((\d*)\))?(?=[^\-]|$)/,PSEUDO:/:((?:[\w\u00c0-\uFFFF\-]|\\.)+)(?:\((['"]?)((?:\([^\)]+\)|[^\(\)]*)+)\2\))?/},leftMatch:{},attrMap:{"class":"className","for":"htmlFor"},attrHandle:{href:function(a){return a.getAttribute("href")},type:function(a){return a.getAttribute("type")}},relative:{"+":function(a,b){var c=typeof b=="string",d=c&&!j.test(b),e=c&&!d;d&&(b=b.toLowerCase());for(var f=0,g=a.length,h;f<g;f++)if(h=a[f]){while((h=h.previousSibling)&&h.nodeType!==1);a[f]=e||h&&h.nodeName.toLowerCase()===b?h||!1:h===b}e&&k.filter(b,a,!0)},">":function(a,b){var c,d=typeof b=="string",e=0,f=a.length;if(d&&!j.test(b)){b=b.toLowerCase();for(;e<f;e++){c=a[e];if(c){var g=c.parentNode;a[e]=g.nodeName.toLowerCase()===b?g:!1}}}else{for(;e<f;e++)c=a[e],c&&(a[e]=d?c.parentNode:c.parentNode===b);d&&k.filter(b,a,!0)}},"":function(a,b,c){var e,f=d++,g=u;typeof b=="string"&&!j.test(b)&&(b=b.toLowerCase(),e=b,g=t),g("parentNode",b,f,a,e,c)},"~":function(a,b,c){var e,f=d++,g=u;typeof b=="string"&&!j.test(b)&&(b=b.toLowerCase(),e=b,g=t),g("previousSibling",b,f,a,e,c)}},find:{ID:function(a,b,c){if(typeof b.getElementById!="undefined"&&!c){var d=b.getElementById(a[1]);return d&&d.parentNode?[d]:[]}},NAME:function(a,b){if(typeof b.getElementsByName!="undefined"){var c=[],d=b.getElementsByName(a[1]);for(var e=0,f=d.length;e<f;e++)d[e].getAttribute("name")===a[1]&&c.push(d[e]);return c.length===0?null:c}},TAG:function(a,b){if(typeof b.getElementsByTagName!="undefined")return b.getElementsByTagName(a[1])}},preFilter:{CLASS:function(a,b,c,d,e,f){a=" "+a[1].replace(i,"")+" ";if(f)return a;for(var g=0,h;(h=b[g])!=null;g++)h&&(e^(h.className&&(" "+h.className+" ").replace(/[\t\n\r]/g," ").indexOf(a)>=0)?c||d.push(h):c&&(b[g]=!1));return!1},ID:function(a){return a[1].replace(i,"")},TAG:function(a,b){return a[1].replace(i,"").toLowerCase()},CHILD:function(a){if(a[1]==="nth"){a[2]||k.error(a[0]),a[2]=a[2].replace(/^\+|\s*/g,"");var b=/(-?)(\d*)(?:n([+\-]?\d*))?/.exec(a[2]==="even"&&"2n"||a[2]==="odd"&&"2n+1"||!/\D/.test(a[2])&&"0n+"+a[2]||a[2]);a[2]=b[1]+(b[2]||1)-0,a[3]=b[3]-0}else a[2]&&k.error(a[0]);a[0]=d++;return a},ATTR:function(a,b,c,d,e,f){var g=a[1]=a[1].replace(i,"");!f&&l.attrMap[g]&&(a[1]=l.attrMap[g]),a[4]=(a[4]||a[5]||"").replace(i,""),a[2]==="~="&&(a[4]=" "+a[4]+" ");return a},PSEUDO:function(b,c,d,e,f){if(b[1]==="not")if((a.exec(b[3])||"").length>1||/^\w/.test(b[3]))b[3]=k(b[3],null,null,c);else{var g=k.filter(b[3],c,d,!0^f);d||e.push.apply(e,g);return!1}else if(l.match.POS.test(b[0])||l.match.CHILD.test(b[0]))return!0;return b},POS:function(a){a.unshift(!0);return a}},filters:{enabled:function(a){return a.disabled===!1&&a.type!=="hidden"},disabled:function(a){return a.disabled===!0},checked:function(a){return a.checked===!0},selected:function(a){a.parentNode&&a.parentNode
.selectedIndex;return a.selected===!0},parent:function(a){return!!a.firstChild},empty:function(a){return!a.firstChild},has:function(a,b,c){return!!k(c[3],a).length},header:function(a){return/h\d/i.test(a.nodeName)},text:function(a){var b=a.getAttribute("type"),c=a.type;return a.nodeName.toLowerCase()==="input"&&"text"===c&&(b===c||b===null)},radio:function(a){return a.nodeName.toLowerCase()==="input"&&"radio"===a.type},checkbox:function(a){return a.nodeName.toLowerCase()==="input"&&"checkbox"===a.type},file:function(a){return a.nodeName.toLowerCase()==="input"&&"file"===a.type},password:function(a){return a.nodeName.toLowerCase()==="input"&&"password"===a.type},submit:function(a){var b=a.nodeName.toLowerCase();return(b==="input"||b==="button")&&"submit"===a.type},image:function(a){return a.nodeName.toLowerCase()==="input"&&"image"===a.type},reset:function(a){var b=a.nodeName.toLowerCase();return(b==="input"||b==="button")&&"reset"===a.type},button:function(a){var b=a.nodeName.toLowerCase();return b==="input"&&"button"===a.type||b==="button"},input:function(a){return/input|select|textarea|button/i.test(a.nodeName)},focus:function(a){return a===a.ownerDocument.activeElement}},setFilters:{first:function(a,b){return b===0},last:function(a,b,c,d){return b===d.length-1},even:function(a,b){return b%2===0},odd:function(a,b){return b%2===1},lt:function(a,b,c){return b<c[3]-0},gt:function(a,b,c){return b>c[3]-0},nth:function(a,b,c){return c[3]-0===b},eq:function(a,b,c){return c[3]-0===b}},filter:{PSEUDO:function(a,b,c,d){var e=b[1],f=l.filters[e];if(f)return f(a,c,b,d);if(e==="contains")return(a.textContent||a.innerText||k.getText([a])||"").indexOf(b[3])>=0;if(e==="not"){var g=b[3];for(var h=0,i=g.length;h<i;h++)if(g[h]===a)return!1;return!0}k.error(e)},CHILD:function(a,b){var c=b[1],d=a;switch(c){case"only":case"first":while(d=d.previousSibling)if(d.nodeType===1)return!1;if(c==="first")return!0;d=a;case"last":while(d=d.nextSibling)if(d.nodeType===1)return!1;return!0;case"nth":var e=b[2],f=b[3];if(e===1&&f===0)return!0;var g=b[0],h=a.parentNode;if(h&&(h.sizcache!==g||!a.nodeIndex)){var i=0;for(d=h.firstChild;d;d=d.nextSibling)d.nodeType===1&&(d.nodeIndex=++i);h.sizcache=g}var j=a.nodeIndex-f;return e===0?j===0:j%e===0&&j/e>=0}},ID:function(a,b){return a.nodeType===1&&a.getAttribute("id")===b},TAG:function(a,b){return b==="*"&&a.nodeType===1||a.nodeName.toLowerCase()===b},CLASS:function(a,b){return(" "+(a.className||a.getAttribute("class"))+" ").indexOf(b)>-1},ATTR:function(a,b){var c=b[1],d=l.attrHandle[c]?l.attrHandle[c](a):a[c]!=null?a[c]:a.getAttribute(c),e=d+"",f=b[2],g=b[4];return d==null?f==="!=":f==="="?e===g:f==="*="?e.indexOf(g)>=0:f==="~="?(" "+e+" ").indexOf(g)>=0:g?f==="!="?e!==g:f==="^="?e.indexOf(g)===0:f==="$="?e.substr(e.length-g.length)===g:f==="|="?e===g||e.substr(0,g.length+1)===g+"-":!1:e&&d!==!1},POS:function(a,b,c,d){var e=b[2],f=l.setFilters[e];if(f)return f(a,c,b,d)}}},m=l.match.POS,n=function(a,b){return"\\"+(b-0+1)};for(var o in l.match)l.match[o]=new RegExp(l.match[o].source+/(?![^\[]*\])(?![^\(]*\))/.source),l.leftMatch[o]=new RegExp(/(^(?:.|\r|\n)*?)/.source+l.match[o].source.replace(/\\(\d+)/g,n));var p=function(a,b){a=Array.prototype.slice.call(a,0);if(b){b.push.apply(b,a);return b}return a};try{Array.prototype.slice.call(c.documentElement.childNodes,0)[0].nodeType}catch(q){p=function(a,b){var c=0,d=b||[];if(e.call(a)==="[object Array]")Array.prototype.push.apply(d,a);else if(typeof a.length=="number")for(var f=a.length;c<f;c++)d.push(a[c]);else for(;a[c];c++)d.push(a[c]);return d}}var r,s;c.documentElement.compareDocumentPosition?r=function(a,b){if(a===b){g=!0;return 0}if(!a.compareDocumentPosition||!b.compareDocumentPosition)return a.compareDocumentPosition?-1:1;return a.compareDocumentPosition(b)&4?-1:1}:(r=function(a,b){if(a===b){g=!0;return 0}if(a.sourceIndex&&b.sourceIndex)return a.sourceIndex-b.sourceIndex;var c,d,e=[],f=[],h=a.parentNode,i=b.parentNode,j=h;if(h===i)return s(a,b);if(!h)return-1;if(!i)return 1;while(j)e.unshift(j),j=j.parentNode;j=i;while(j)f.unshift(j),j=j.parentNode;c=e.length,d=f.length;for(var k=0;k<c&&k<d;k++)if(e[k]!==f[k])return s(e[k],f[k]);return k===c?s(a,f[k],-1):s(e[k],b,1)},s=function(a,b,c){if(a===b)return c;var d=a.nextSibling;while(d){if(d===b)return-1;d=d.nextSibling}return 1}),k.getText=function(a){var b="",c;for(var d=0;a[d];d++)c=a[d],c.nodeType===3||c.nodeType===4?b+=c.nodeValue:c.nodeType!==8&&(b+=k.getText(c.childNodes));return b},function(){var a=c.createElement("div"),d="script"+(new Date).getTime(),e=c.documentElement;a.innerHTML="<a name='"+d+"'/>",e.insertBefore(a,e.firstChild),c.getElementById(d)&&(l.find.ID=function(a,c,d){if(typeof c.getElementById!="undefined"&&!d){var e=c.getElementById(a[1]);return e?e.id===a[1]||typeof e.getAttributeNode!="undefined"&&e.getAttributeNode("id").nodeValue===a[1]?[e]:b:[]}},l.filter.ID=function(a,b){var c=typeof a.getAttributeNode!="undefined"&&a.getAttributeNode("id");return a.nodeType===1&&c&&c.nodeValue===b}),e.removeChild(a),e=a=null}(),function(){var a=c.createElement("div");a.appendChild(c.createComment("")),a.getElementsByTagName("*").length>0&&(l.find.TAG=function(a,b){var c=b.getElementsByTagName(a[1]);if(a[1]==="*"){var d=[];for(var e=0;c[e];e++)c[e].nodeType===1&&d.push(c[e]);c=d}return c}),a.innerHTML="<a href='#'></a>",a.firstChild&&typeof a.firstChild.getAttribute!="undefined"&&a.firstChild.getAttribute("href")!=="#"&&(l.attrHandle.href=function(a){return a.getAttribute("href",2)}),a=null}(),c.querySelectorAll&&function(){var a=k,b=c.createElement("div"),d="__sizzle__";b.innerHTML="<p class='TEST'></p>";if(!b.querySelectorAll||b.querySelectorAll(".TEST").length!==0){k=function(b,e,f,g){e=e||c;if(!g&&!k.isXML(e)){var h=/^(\w+$)|^\.([\w\-]+$)|^#([\w\-]+$)/.exec(b);if(h&&(e.nodeType===1||e.nodeType===9)){if(h[1])return p(e.getElementsByTagName(b),f);if(h[2]&&l.find.CLASS&&e.getElementsByClassName)return p(e.getElementsByClassName(h[2]),f)}if(e.nodeType===9){if(b==="body"&&e.body)return p([e.body],f);if(h&&h[3]){var i=e.getElementById(h[3]);if(!i||!i.parentNode)return p([],f);if(i.id===h[3])return p([i],f)}try{return p(e.querySelectorAll(b),f)}catch(j){}}else if(e.nodeType===1&&e.nodeName.toLowerCase()!=="object"){var m=e,n=e.getAttribute("id"),o=n||d,q=e.parentNode,r=/^\s*[+~]/.test(b);n?o=o.replace(/'/g,"\\$&"):e.setAttribute("id",o),r&&q&&(e=e.parentNode);try{if(!r||q)return p(e.querySelectorAll("[id='"+o+"'] "+b),f)}catch(s){}finally{n||m.removeAttribute("id")}}}return a(b,e,f,g)};for(var e in a)k[e]=a[e];b=null}}(),function(){var a=c.documentElement,b=a.matchesSelector||a.mozMatchesSelector||a.webkitMatchesSelector||a.msMatchesSelector;if(b){var d=!b.call(c.createElement("div"),"div"),e=!1;try{b.call(c.documentElement,"[test!='']:sizzle")}catch(f){e=!0}k.matchesSelector=function(a,c){c=c.replace(/\=\s*([^'"\]]*)\s*\]/g,"='$1']");if(!k.isXML(a))try{if(e||!l.match.PSEUDO.test(c)&&!/!=/.test(c)){var f=b.call(a,c);if(f||!d||a.document&&a.document.nodeType!==11)return f}}catch(g){}return k(c,null,null,[a]).length>0}}}(),function(){var a=c.createElement("div");a.innerHTML="<div class='test e'></div><div class='test'></div>";if(!!a.getElementsByClassName&&a.getElementsByClassName("e").length!==0){a.lastChild.className="e";if(a.getElementsByClassName("e").length===1)return;l.order.splice(1,0,"CLASS"),l.find.CLASS=function(a,b,c){if(typeof b.getElementsByClassName!="undefined"&&!c)return b.getElementsByClassName(a[1])},a=null}}(),c.documentElement.contains?k.contains=function(a,b){return a!==b&&(a.contains?a.contains(b):!0)}:c.documentElement.compareDocumentPosition?k.contains=function(a,b){return!!(a.compareDocumentPosition(b)&16)}:k.contains=function(){return!1},k.isXML=function(a){var b=(a?a.ownerDocument||a:0).documentElement;return b?b.nodeName!=="HTML":!1};var v=function(a,b){var c,d=[],e="",f=b.nodeType?[b]:b;while(c=l.match.PSEUDO.exec(a))e+=c[0],a=a.replace(l.match.PSEUDO,"");a=l.relative[a]?a+"*":a;for(var g=0,h=f.length;g<h;g++)k(a,f[g],d);return k.filter(e,d)};f.find=k,f.expr=k.selectors,f.expr[":"]=f.expr.filters,f.unique=k.uniqueSort,f.text=k.getText,f.isXMLDoc=k.isXML,f.contains=k.contains}();var x=/Until$/,y=/^(?:parents|prevUntil|prevAll)/,z=/,/,A=/^.[^:#\[\.,]*$/,B=Array.prototype.slice,C=f.expr.match.POS,D={children:!0,contents:!0,next:!0,prev:!0};f.fn.extend({find:function(a){var b=this,c,d;if(typeof a!="string")return f(a).filter(function(){for(c=0,d=b.length;c<d;c++)if(f.contains(b[c],this))return!0});var e=this.pushStack("","find",a),g,h,i;for(c=0,d=this.length;c<d;c++){g=e.length,f.find(a,this[c],e);if(c>0)for(h=g;h<e.length;h++)for(i=0;i<g;i++)if(e[i]===e[h]){e.splice(h--,1);break}}return e},has:function(a){var b=f(a);return this.filter(function(){for(var a=0,c=b.length;a<c;a++)if(f.contains(this,b[a]))return!0})},not:function(a){return this.pushStack(F(this,a,!1),"not",a)},filter:function(a){return this.pushStack(F(this,a,!0),"filter",a)},is:function(a){return!!a&&(typeof a=="string"?f.filter(a,this).length>0:this.filter(a).length>0)},closest:function(a,b){var c=[],d,e,g=this[0];if(f.isArray(a)){var h,i,j={},k=1;if(g&&a.length){for(d=0,e=a.length;d<e;d++)i=a[d],j[i]||(j[i]=C.test(i)?f(i,b||this.context):i);while(g&&g.ownerDocument&&g!==b){for(i in j)h=j[i],(h.jquery?h.index(g)>-1:f(g).is(h))&&c.push({selector:i,elem:g,level:k});g=g.parentNode,k++}}return c}var l=C.test(a)||typeof a!="string"?f(a,b||this.context):0;for(d=0,e=this.length;d<e;d++){g=this[d];while(g){if(l?l.index(g)>-1:f.find.matchesSelector(g,a)){c.push(g);break}g=g.parentNode;if(!g||!g.ownerDocument||g===b||g.nodeType===11)break}}c=c.length>1?f.unique(c):c;return this.pushStack(c,"closest",a)},index:function(a){if(!a||typeof a=="string")return f.inArray(this[0],a?f(a):this.parent().children());return f.inArray(a.jquery?a[0]:a,this)},add:function(a,b){var c=typeof a=="string"?f(a,b):f.makeArray(a&&a.nodeType?[a]:a),d=f.merge(this.get(),c);return this.pushStack(E(c[0])||E(d[0])?d:f.unique(d))},andSelf:function(){return this.add(this.prevObject)}}),f.each({parent:function(a){var b=a.parentNode;return b&&b.nodeType!==11?b:null},parents:function(a){return f.dir(a,"parentNode")},parentsUntil:function(a,b,c){return f.dir(a,"parentNode",c)},next:function(a){return f.nth(a,2,"nextSibling")},prev:function(a){return f.nth(a,2,"previousSibling")},nextAll:function(a){return f.dir(a,"nextSibling")},prevAll:function(a){return f.dir(a,"previousSibling")},nextUntil:function(a,b,c){return f.dir(a,"nextSibling",c)},prevUntil:function(a,b,c){return f.dir(a,"previousSibling",c)},siblings:function(a){return f.sibling(a.parentNode.firstChild,a)},children:function(a){return f.sibling(a.firstChild)},contents:function(a){return f.nodeName(a,"iframe")?a.contentDocument||a.contentWindow.document:f.makeArray(a.childNodes)}},function(a,b){f.fn[a]=function(c,d){var e=f.map(this,b,c),g=B.call(arguments);x.test(a)||(d=c),d&&typeof d=="string"&&(e=f.filter(d,e)),e=this.length>1&&!D[a]?f.unique(e):e,(this.length>1||z.test(d))&&y.test(a)&&(e=e.reverse());return this.pushStack(e,a,g.join(","))}}),f.extend({filter:function(a,b,c){c&&(a=":not("+a+")");return b.length===1?f.find.matchesSelector(b[0],a)?[b[0]]:[]:f.find.matches(a,b)},dir:function(a,c,d){var e=[],g=a[c];while(g&&g.nodeType!==9&&(d===b||g.nodeType!==1||!f(g).is(d)))g.nodeType===1&&e.push(g),g=g[c];return e},nth:function(a,b,c,d){b=b||1;var e=0;for(;a;a=a[c])if(a.nodeType===1&&++e===b)break;return a},sibling:function(a,b){var c=[];for(;a;a=a.nextSibling)a.nodeType===1&&a!==b&&c.push(a);return c}});var G=/ jQuery\d+="(?:\d+|null)"/g,H=/^\s+/,I=/<(?!area|br|col|embed|hr|img|input|link|meta|param)(([\w:]+)[^>]*)\/>/ig,J=/<([\w:]+)/,K=/<tbody/i,L=/<|&#?\w+;/,M=/<(?:script|object|embed|option|style)/i,N=/checked\s*(?:[^=]|=\s*.checked.)/i,O=/\/(java|ecma)script/i,P=/^\s*<!(?:\[CDATA\[|\-\-)/,Q={option:[1,"<select multiple='multiple'>","</select>"],legend:[1,"<fieldset>","</fieldset>"],thead:[1,"<table>","</table>"],tr:[2,"<table><tbody>","</tbody></table>"],td:[3,"<table><tbody><tr>","</tr></tbody></table>"],col:[2,"<table><tbody></tbody><colgroup>","</colgroup></table>"],area:[1,"<map>","</map>"],_default:[0,"",""]};Q.optgroup=Q.option,Q.tbody=Q.tfoot=Q.colgroup=Q.caption=Q.thead,Q.th=Q.td,f.support.htmlSerialize||(Q._default=[1,"div<div>","</div>"]),f.fn.extend({text:function(a){if(f.isFunction(a))return this.each(function(b){var c=f(this);c.text(a.call(this,b,c.text()))});if(typeof a!="object"&&a!==b)return this.empty().append((this[0]&&this[0].ownerDocument||c).createTextNode(a));return f.text(this)},wrapAll:function(a){if(f.isFunction(a))return this.each(function(b){f(this).wrapAll(a.call(this,b))});if(this[0]){var b=f(a,this[0].ownerDocument).eq(0).clone(!0);this[0].parentNode&&b.insertBefore(this[0]),b.map(function(){var a=this;while(a.firstChild&&a.firstChild.nodeType===1)a=a.firstChild;return a}).append(this)}return this},wrapInner:function(a){if(f.isFunction(a))return this.each(function(b){f(this).wrapInner(a.call(this,b))});return this.each(function(){var b=f(this),c=b.contents();c.length?c.wrapAll(a):b.append(a)})},wrap:function(a){return this.each(function(){f(this).wrapAll(a)})},unwrap:function(){return this.parent().each(function(){f.nodeName(this,"body")||f(this).replaceWith(this.childNodes)}).end()},append:function(){return this.domManip(arguments,!0,function(a){this.nodeType===1&&this.appendChild(a)})},prepend:function(){return this.domManip(arguments,!0,function(a){this.nodeType===1&&this.insertBefore(a,this.firstChild)})},before:function(){if(this[0]&&this[0].parentNode)return this.domManip(arguments,!1,function(a){this.parentNode.insertBefore(a,this)});if(arguments.length){var a=f(arguments[0]);a.push.apply(a,this.toArray());return this.pushStack(a,"before",arguments)}},after:function(){if(this[0]&&this[0].parentNode)return this.domManip(arguments,!1,function(a){this.parentNode.insertBefore(a,this.nextSibling)});if(arguments.length){var a=this.pushStack(this,"after",arguments);a.push.apply(a,f(arguments[0]).toArray());return a}},remove:function(a,b){for(var c=0,d;(d=this[c])!=null;c++)if(!a||f.filter(a,[d]).length)!b&&d.nodeType===1&&(f.cleanData(d.getElementsByTagName("*")),f.cleanData([d])),d.parentNode&&d.parentNode.removeChild(d);return this},empty:function(){for(var a=0,b;(b=this[a])!=null;a++){b.nodeType===1&&f.cleanData(b.getElementsByTagName("*"));while(b.firstChild)b.removeChild(b.firstChild)}return this},clone:function(a,b){a=a==null?!1:a,b=b==null?a:b;return this.map(function(){return f.clone(this,a,b)})},html:function(a){if(a===b)return this[0]&&this[0].nodeType===1?this[0].innerHTML.replace(G,""):null;if(typeof a=="string"&&!M.test(a)&&(f.support.leadingWhitespace||!H.test(a))&&!Q[(J.exec(a)||["",""])[1].toLowerCase()]){a=a.replace(I,"<$1></$2>");try{for(var c=0,d=this.length;c<d;c++)this[c].nodeType===1&&(f.cleanData(this[c].getElementsByTagName("*")),this[c].innerHTML=a)}catch(e){this.empty().append(a)}}else f.isFunction(a)?this.each(function(b){var c=f(this);c.html(a.call(this,b,c.html()))}):this.empty().append(a);return this},replaceWith:function(a){if(this[0]&&this[0].parentNode){if(f.isFunction(a))return this.each(function(b){var c=f(this),d=c.html();c.replaceWith(a.call(this,b,d))});typeof a!="string"&&(a=f(a).detach());return this.each(function(){var b=this.nextSibling,c=this.parentNode;f(this).remove(),b?f(b).before(a):f(c).append(a)})}return this.length?this.pushStack(f(f.isFunction(a)?a():a),"replaceWith",a):this},detach:function(a){return this.remove(a,!0)},domManip:function(a,c,d){var e,g,h,i,j=a[0],k=[];if(!f.support.checkClone&&arguments.length===3&&typeof j=="string"&&N.test(j))return this.each(function(){f(this).domManip(a,c,d,!0)});if(f.isFunction(j))return this.each(function(e){var g=f(this);a[0]=j.call(this,e,c?g.html():b),g.domManip(a,c,d)});if(this[0]){i=j&&j.parentNode,f.support.parentNode&&i&&i.nodeType===11&&i.childNodes.length===this.length?e={fragment:i}:e=f.buildFragment(a,this,k),h=e.fragment,h.childNodes.length===1?g=h=h.firstChild:g=h.firstChild;if(g){c=c&&f.nodeName(g,"tr");for(var l=0,m=this.length,n=m-1;l<m;l++)d.call(c?R(this[l],g):this[l],e.cacheable||m>1&&l<n?f.clone(h,!0,!0):h)}k.length&&f.each(k,X)}return this}}),f.buildFragment=function(a,b,d){var e,g,h,i=b&&b[0]?b[0].ownerDocument||b[0]:c;a.length===1&&typeof a[0]=="string"&&a[0].length<512&&i===c&&a[0].charAt(0)==="<"&&!M.test(a[0])&&(f.support.checkClone||!N.test(a[0]))&&(g=!0,h=f.fragments[a[0]],h&&h!==1&&(e=h)),e||(e=i.createDocumentFragment(),f.clean(a,i,e,d)),g&&(f.fragments[a[0]]=h?e:1);return{fragment:e,cacheable:g}},f.fragments={},f.each({appendTo:"append",prependTo:"prepend",insertBefore:"before",insertAfter:"after",replaceAll:"replaceWith"},function(a,b){f.fn[a]=function(c){var d=[],e=f(c),g=this.length===1&&this[0].parentNode;if(g&&g.nodeType===11&&g.childNodes.length===1&&e.length===1){e[b](this[0]);return this}for(var h=0,i=e.length;h<i;h++){var j=(h>0?this.clone(!0):this).get();f(e[h])[b](j),d=d.concat(j)}return this.pushStack(d,a,e.selector)}}),f.extend({clone:function(a,b,c){var d=a.cloneNode(!0),e,g,h;if((!f.support.noCloneEvent||!f.support.noCloneChecked)&&(a.nodeType===1||a.nodeType===11)&&!f.isXMLDoc(a)){T(a,d),e=U(a),g=U(d);for(h=0;e[h];++h)T(e[h],g[h])}if(b){S(a,d);if(c){e=U(a),g=U(d);for(h=0;e[h];++h)S(e[h],g[h])}}return d},clean:function(a,b,d,e){var g;b=b||c,typeof b.createElement=="undefined"&&(b=b.ownerDocument||b[0]&&b[0].ownerDocument||c);var h=[],i;for(var j=0,k;(k=a[j])!=null;j++){typeof k=="number"&&(k+="");if(!k)continue;if(typeof k=="string")if(!L.test(k))k=b.createTextNode(k);else{k=k.replace(I,"<$1></$2>");var l=(J.exec(k)||["",""])[1].toLowerCase(),m=Q[l]||Q._default,n=m[0],o=b.createElement("div");o.innerHTML=m[1]+k+m[2];while(n--)o=o.lastChild;if(!f.support.tbody){var p=K.test(k),q=l==="table"&&!p?o.firstChild&&o.firstChild.childNodes:m[1]==="<table>"&&!p?o.childNodes:[];for(i=q.length-1;i>=0;--i)f.nodeName(q[i],"tbody")&&!q[i].childNodes.length&&q[i].parentNode.removeChild(q[i])}!f.support.leadingWhitespace&&H.test(k)&&o.insertBefore(b.createTextNode(H.exec(k)[0]),o.firstChild),k=o.childNodes}var r;if(!f.support.appendChecked)if(k[0]&&typeof (r=k.length)=="number")for(i=0;i<r;i++)W(k[i]);else W(k);k.nodeType?h.push(k):h=f.merge(h,k)}if(d){g=function(a){return!a.type||O.test(a.type)};for(j=0;h[j];j++)if(e&&f.nodeName(h[j],"script")&&(!h[j].type||h[j].type.toLowerCase()==="text/javascript"))e.push(h[j].parentNode?h[j].parentNode.removeChild(h[j]):h[j]);else{if(h[j].nodeType===1){var s=f.grep(h[j].getElementsByTagName("script"),g);h.splice.apply(h,[j+1,0].concat(s))}d.appendChild(h[j])}}return h},cleanData:function(a){var b,c,d=f.cache,e=f.expando,g=f.event.special,h=f.support.deleteExpando;for(var i=0,j;(j=a[i])!=null;i++){if(j.nodeName&&f.noData[j.nodeName.toLowerCase()])continue;c=j[f.expando];if(c){b=d[c]&&d[c][e];if(b&&b.events){for(var k in b.events)g[k]?f.event.remove(j,k):f.removeEvent(j,k,b.handle);b.handle&&(b.handle.elem=null)}h?delete j[f.expando]:j.removeAttribute&&j.removeAttribute(f.expando),delete d[c]}}}});var Y=/alpha\([^)]*\)/i,Z=/opacity=([^)]*)/,$=/-([a-z])/ig,_=/([A-Z]|^ms)/g,ba=/^-?\d+(?:px)?$/i,bb=/^-?\d/,bc=/^[+\-]=/,bd=/[^+\-\.\de]+/g,be={position:"absolute",visibility:"hidden",display:"block"},bf=["Left","Right"],bg=["Top","Bottom"],bh,bi,bj,bk=function(a,b){return b.toUpperCase()};f.fn.css=function(a,c){if(arguments.length===2&&c===b)return this;return f.access(this,a,c,!0,function(a,c,d){return d!==b?f.style(a,c,d):f.css(a,c)})},f.extend({cssHooks:{opacity:{get:function(a,b){if(b){var c=bh(a,"opacity","opacity");return c===""?"1":c}return a.style.opacity}}},cssNumber:{zIndex:!0,fontWeight:!0,opacity:!0,zoom:!0,lineHeight:!0,widows:!0,orphans:!0},cssProps:{"float":f.support.cssFloat?"cssFloat":"styleFloat"},style:function(a,c,d,e){if(!!a&&a.nodeType!==3&&a.nodeType!==8&&!!a.style){var g,h,i=f.camelCase(c),j=a.style,k=f.cssHooks[i];c=f.cssProps[i]||i;if(d===b){if(k&&"get"in k&&(g=k.get(a,!1,e))!==b)return g;return j[c]}h=typeof d;if(h==="number"&&isNaN(d)||d==null)return;h==="string"&&bc.test(d)&&(d=+d.replace(bd,"")+parseFloat(f.css(a,c))),h==="number"&&!f.cssNumber[i]&&(d+="px");if(!k||!("set"in k)||(d=k.set(a,d))!==b)try{j[c]=d}catch(l){}}},css:function(a,c,d){var e,g;c=f.camelCase(c),g=f.cssHooks[c],c=f.cssProps[c]||c,c==="cssFloat"&&(c="float");if(g&&"get"in g&&(e=g.get(a,!0,d))!==b)return e;if(bh)return bh(a,c)},swap:function(a,b,c){var d={};for(var e in b)d[e]=a.style[e],a.style[e]=b[e];c.call(a);for(e in b)a.style[e]=d[e]},camelCase:function(a){return a.replace($,bk)}}),f.curCSS=f.css,f.each(["height","width"],function(a,b){f.cssHooks[b]={get:function(a,c,d){var e;if(c){a.offsetWidth!==0?e=bl(a,b,d):f.swap(a,be,function(){e=bl(a,b,d)});if(e<=0){e=bh(a,b,b),e==="0px"&&bj&&(e=bj(a,b,b));if(e!=null)return e===""||e==="auto"?"0px":e}if(e<0||e==null){e=a.style[b];return e===""||e==="auto"?"0px":e}return typeof e=="string"?e:e+"px"}},set:function(a,b){if(!ba.test(b))return b;b=parseFloat(b);if(b>=0)return b+"px"}}}),f.support.opacity||(f.cssHooks.opacity={get:function(a,b){return Z.test((b&&a.currentStyle?a.currentStyle.filter:a.style.filter)||"")?parseFloat(RegExp.$1)/100+"":b?"1":""},set:function(a,b){var c=a.style,d=a.currentStyle;c.zoom=1;var e=f.isNaN(b)?"":"alpha(opacity="+b*100+")",g=d&&d.filter||c.filter||"";c.filter=Y.test(g)?g.replace(Y,e):g+" "+e}}),f(function(){f.support.reliableMarginRight||(f.cssHooks.marginRight={get:function(a,b){var c;f.swap(a,{display:"inline-block"},function(){b?c=bh(a,"margin-right","marginRight"):c=a.style.marginRight});return c}})}),c.defaultView&&c.defaultView.getComputedStyle&&(bi=function(a,c){var d,e,g;c=c.replace(_,"-$1").toLowerCase();if(!(e=a.ownerDocument.defaultView))return b;if(g=e.getComputedStyle(a,null))d=g.getPropertyValue(c),d===""&&!f.contains(a.ownerDocument.documentElement,a)&&(d=f.style(a,c));return d}),c.documentElement.currentStyle&&(bj=function(a,b){var c,d=a.currentStyle&&a.currentStyle[b],e=a.runtimeStyle&&a.runtimeStyle[b],f=a.style;!ba.test(d)&&bb.test(d)&&(c=f.left,e&&(a.runtimeStyle.left=a.currentStyle.left),f.left=b==="fontSize"?"1em":d||0,d=f.pixelLeft+"px",f.left=c,e&&(a.runtimeStyle.left=e));return d===""?"auto":d}),bh=bi||bj,f.expr&&f.expr.filters&&(f.expr.filters.hidden=function(a){var b=a.offsetWidth,c=a.offsetHeight;return b===0&&c===0||!f.support.reliableHiddenOffsets&&(a.style.display||f.css(a,"display"))==="none"},f.expr.filters.visible=function(a){return!f.expr.filters.hidden(a)}),a.jQuery=a.$=f})(window);
]]></xsl:text>
    </xsl:template>



</xsl:stylesheet>
