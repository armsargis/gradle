/*
 * Copyright 2010 the original author or authors.
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

package org.gradle.integtests.samples

import org.gradle.integtests.fixtures.GradleDistribution
import org.gradle.integtests.fixtures.GradleDistributionExecuter
import org.gradle.integtests.fixtures.JUnitTestExecutionResult
import org.gradle.integtests.fixtures.Sample
import org.gradle.util.TestFile
import org.junit.Rule
import org.junit.Test

/**
 * @author Hans Dockter
 */

class SamplesJavaBaseIntegrationTest {
    @Rule public final GradleDistribution dist = new GradleDistribution()
    @Rule public final GradleDistributionExecuter executer = new GradleDistributionExecuter()
    @Rule public final Sample sample = new Sample('java/base')

    @Test
    public void canBuildAndUploadJar() {
        TestFile javaprojectDir = sample.dir

        // Build and test projects
        executer.inDirectory(javaprojectDir).withTasks('clean', 'build').run()

        // Check tests have run
        JUnitTestExecutionResult result = new JUnitTestExecutionResult(javaprojectDir.file('test'))
        result.assertTestClassesExecuted('org.gradle.PersonTest')

        // Check jar exists
        javaprojectDir.file("prod/build/libs/prod-1.0.jar").assertIsFile()
        
        // Check contents of Jar
        TestFile jarContents = dist.testDir.file('jar')
        javaprojectDir.file('prod/build/libs/prod-1.0.jar').unzipTo(jarContents)
        jarContents.assertHasDescendants(
                'META-INF/MANIFEST.MF',
                'org/gradle/Person.class'
        )
    }
}