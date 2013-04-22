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

package org.gradle.testing.junit

import org.gradle.integtests.fixtures.DefaultTestExecutionResult
import org.gradle.integtests.fixtures.MultiVersionIntegrationSpec
import org.gradle.integtests.fixtures.TargetVersions
import org.gradle.integtests.fixtures.TestResources
import org.junit.Rule
import org.junit.Test

@TargetVersions(['4.0', '4.4', '4.8.2', '4.11'])
class JUnitCrossVersionIntegrationSpec extends MultiVersionIntegrationSpec {
    @Rule
    public final TestResources resources = new TestResources(temporaryFolder)

    String junitDependency = "junit:junit:$version"

    @Test
    public void canRunTestsUsingJUnit() {
        given:
        resources.maybeCopy('JUnitIntegrationTest/junit3Tests')
        resources.maybeCopy('JUnitIntegrationTest/junit4Tests')

        configureJUnit();
        when:
        executer.withTasks('check').run()
        then:
        def result = new DefaultTestExecutionResult(testDirectory)
        result.assertTestClassesExecuted('org.gradle.Junit3Test', 'org.gradle.Junit4Test')
        result.testClass('org.gradle.Junit3Test')
                .assertTestCount(1, 0, 0)
                .assertTestsExecuted('testRenamesItself')
                .assertTestPassed('testRenamesItself')
        result.testClass('org.gradle.Junit4Test')
                .assertTestCount(2, 0, 0)
                .assertTestsExecuted('ok')
                .assertTestPassed('ok')
                .assertTestsSkipped('broken')
    }

    private void configureJUnit() {
        buildFile << """
        dependencies {
        testCompile '${junitDependency.toString()}'
        }"""
    }
}
