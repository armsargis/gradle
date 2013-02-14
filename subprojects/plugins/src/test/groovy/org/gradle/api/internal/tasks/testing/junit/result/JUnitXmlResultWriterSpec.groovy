/*
 * Copyright 2012 the original author or authors.
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

package org.gradle.api.internal.tasks.testing.junit.result

import org.gradle.api.internal.tasks.testing.results.DefaultTestResult
import org.gradle.integtests.fixtures.JUnitTestClassExecutionResult
import spock.lang.Specification

import static java.util.Arrays.asList
import static java.util.Collections.emptyList
import static org.gradle.api.tasks.testing.TestOutputEvent.Destination.StdErr
import static org.gradle.api.tasks.testing.TestOutputEvent.Destination.StdOut
import static org.gradle.api.tasks.testing.TestResult.ResultType.*
import static org.hamcrest.Matchers.equalTo
import org.gradle.internal.SystemProperties

/**
 * by Szczepan Faber, created at: 11/16/12
 */
class JUnitXmlResultWriterSpec extends Specification {

    private provider = Mock(TestResultsProvider)
    private generator = new JUnitXmlResultWriter("localhost", provider)

    private startTime = 1353344968049

    def "writes xml JUnit result"() {
        TestClassResult result = new TestClassResult("com.foo.FooTest", startTime)
        result.add(new TestMethodResult("some test", new DefaultTestResult(SUCCESS, startTime + 10, startTime + 25, 1, 1, 0, emptyList())))
        result.add(new TestMethodResult("some test two", new DefaultTestResult(SUCCESS, startTime + 15, startTime + 30, 1, 1, 0, emptyList())))
        result.add(new TestMethodResult("some failing test", new DefaultTestResult(FAILURE, startTime + 30, startTime + 40, 1, 0, 1, [new RuntimeException("Boo!")])))
        result.add(new TestMethodResult("some skipped test", new DefaultTestResult(SKIPPED, startTime + 35, startTime + 45, 1, 0, 1, asList())))

        provider.writeOutputs("com.foo.FooTest", StdOut, _) >> { args -> args[2].write("1st output message\n2nd output message\n") }
        provider.writeOutputs("com.foo.FooTest", StdErr, _) >> { args -> args[2].write("err") }

        when:
        def xml = getXml(result)

        then:
        new JUnitTestClassExecutionResult(xml, "com.foo.FooTest")
            .assertTestCount(4, 1, 0)
            .assertTestFailed("some failing test", equalTo('java.lang.RuntimeException: Boo!'))
            .assertTestsSkipped("some skipped test")
            .assertTestsExecuted("some test", "some test two", "some failing test")
            .assertStdout(equalTo("""1st output message
2nd output message
"""))
            .assertStderr(equalTo("err"))

        and:
        xml.startsWith """<?xml version="1.1" encoding="UTF-8"?>
<testsuite name="com.foo.FooTest" tests="4" failures="1" errors="0" timestamp="2012-11-19T17:09:28" hostname="localhost" time="0.045">
  <properties/>
  <testcase name="some test" classname="com.foo.FooTest" time="0.015"/>
  <testcase name="some test two" classname="com.foo.FooTest" time="0.015"/>
  <testcase name="some failing test" classname="com.foo.FooTest" time="0.01">
    <failure message="java.lang.RuntimeException: Boo!" type="java.lang.RuntimeException">java.lang.RuntimeException: Boo!"""

        xml.endsWith """</failure>
  </testcase>
  <ignored-testcase name="some skipped test" classname="com.foo.FooTest" time="0.01"/>
  <system-out><![CDATA[1st output message
2nd output message
]]></system-out>
  <system-err><![CDATA[err]]></system-err>
</testsuite>
"""
    }

    def "writes results with empty outputs"() {
        TestClassResult result = new TestClassResult("com.foo.FooTest", startTime)
        result.add(new TestMethodResult("some test", new DefaultTestResult(SUCCESS, startTime + 100, startTime + 300, 1, 1, 0, emptyList())))
        _ * provider.writeOutputs(_, _, _)

        when:
        def xml = getXml(result)

        then:
        xml == """<?xml version="1.1" encoding="UTF-8"?>
<testsuite name="com.foo.FooTest" tests="1" failures="0" errors="0" timestamp="2012-11-19T17:09:28" hostname="localhost" time="0.3">
  <properties/>
  <testcase name="some test" classname="com.foo.FooTest" time="0.2"/>
  <system-out><![CDATA[]]></system-out>
  <system-err><![CDATA[]]></system-err>
</testsuite>
"""
    }

    def "encodes xml"() {
        TestClassResult result = new TestClassResult("com.foo.FooTest", startTime)
        result.add(new TestMethodResult("some test", new DefaultTestResult(FAILURE, 100, 300, 1, 1, 0, [new RuntimeException("<> encoded!")])))
        provider.writeOutputs(_, StdErr, _) >> { args -> args[2].write("with CDATA end token: ]]> some ascii: ż") }
        provider.writeOutputs(_, StdOut, _) >> { args -> args[2].write("with CDATA end token: ]]> some ascii: ż") }

        when:
        def xml = getXml(result)

        then:
        //attribute and text is encoded:
        xml.contains('message="java.lang.RuntimeException: &lt;&gt; encoded!" type="java.lang.RuntimeException">java.lang.RuntimeException: &lt;&gt; encoded!')
        //output encoded:
        xml.contains('<system-out><![CDATA[with CDATA end token: ]]]]><![CDATA[> some ascii: ż]]></system-out>')
        xml.contains('<system-err><![CDATA[with CDATA end token: ]]]]><![CDATA[> some ascii: ż]]></system-err>')
    }

    def "writes results with no tests"() {
        TestClassResult result = new TestClassResult("com.foo.IgnoredTest", startTime)

        when:
        def xml = getXml(result)

        then:
        xml == """<?xml version="1.1" encoding="UTF-8"?>
<testsuite name="com.foo.IgnoredTest" tests="0" failures="0" errors="0" timestamp="2012-11-19T17:09:28" hostname="localhost" time="0.0">
  <properties/>
  <system-out><![CDATA[]]></system-out>
  <system-err><![CDATA[]]></system-err>
</testsuite>
"""
    }

    def getXml(TestClassResult result) {
        def text = new ByteArrayOutputStream()
        generator.write(result, text)
        return text.toString("UTF-8").replace(SystemProperties.lineSeparator, "\n")
    }
}