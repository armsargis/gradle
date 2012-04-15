/*
 * Copyright 2009 the original author or authors.
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

package org.gradle.peformance.fixture

import org.gradle.api.logging.Logging
import org.gradle.integtests.fixtures.*

public class PerformanceTestRunner {
    
    private final static LOGGER = Logging.getLogger(PerformanceTestRunner.class)

    def current = new GradleDistribution()
    def previous = new ReleasedVersions(current).last

    String testProject
    int runs
    int warmUpRuns
    int accuracyMs
    List<String> gradleOpts

    def results

    PerformanceResults run() {
        results = new PerformanceResults(accuracyMs: accuracyMs, displayName: "Results for test project '$testProject'")
        LOGGER.lifecycle("Running performance tests for test project '{}', no. # runs: {}", testProject, runs)
        warmUpRuns.times {
            LOGGER.info("Executing warm-up run #${it+1}")
            runOnce()
        }
        results.clear()
        runs.times {
            LOGGER.info("Executing test run #${it+1}")
            runOnce()
        }
        results
    }

    void runOnce() {
        def previousExecuter = executer(previous, testProject)
        def previousResult = MeasuredOperation.measure {
            previousExecuter.run()
        }

        def currentExecuter = executer(current, testProject)
        def currentResult = MeasuredOperation.measure {
            currentExecuter.run()
        }

        results.addResult(previousResult, currentResult)
    }

    GradleExecuter executer(BasicGradleDistribution dist, String testProjectName) {
        def projectDir = new TestProjectLocator().findProjectDir(testProjectName)
        def executer
        if (dist instanceof GradleDistribution) {
            executer = new GradleDistributionExecuter(GradleDistributionExecuter.Executer.forking, dist)
        } else {
            executer = dist.executer()
        }
        if (gradleOpts) {
            executer.withGradleOpts(gradleOpts as String[])
        }
        return executer.withArguments('-u').inDirectory(projectDir).withTasks('clean', 'build')
    }
}
