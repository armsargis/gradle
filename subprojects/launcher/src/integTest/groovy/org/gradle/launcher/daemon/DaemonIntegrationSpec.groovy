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

package org.gradle.launcher.daemon

import ch.qos.logback.classic.Level
import org.gradle.integtests.fixtures.GradleDistribution
import org.gradle.integtests.fixtures.GradleDistributionExecuter
import org.junit.Rule
import org.slf4j.LoggerFactory
import spock.lang.Specification
import static org.gradle.integtests.fixtures.GradleDistributionExecuter.Executer.daemon

/**
 * by Szczepan Faber, created at: 2/1/12
 */
class DaemonIntegrationSpec extends Specification {

    @Rule public final GradleDistribution distribution = new GradleDistribution()
    @Rule public final GradleDistributionExecuter executer = new GradleDistributionExecuter(daemon)

    def setup() {
        distribution.requireIsolatedDaemons()
        LoggerFactory.getLogger("org.gradle.cache.internal.DefaultFileLockManager").level = Level.INFO
    }

    void stopDaemonsNow() {
        executer.withArguments("--stop", "--info").run()
    }

    void buildSucceeds(String script) {
        distribution.file('build.gradle') << script
        executer.withArguments("--info", "-Dorg.gradle.jvmargs=").run()
    }
}
