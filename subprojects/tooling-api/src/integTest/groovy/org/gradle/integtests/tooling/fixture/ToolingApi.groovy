/*
 * Copyright 2011 the original author or authors.
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
package org.gradle.integtests.tooling.fixture

import java.util.concurrent.TimeUnit
import org.gradle.integtests.fixtures.BasicGradleDistribution
import org.gradle.integtests.fixtures.GradleDistribution
import org.gradle.integtests.fixtures.GradleDistributionExecuter
import org.gradle.integtests.fixtures.IntegrationTestHint
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.UnsupportedVersionException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ToolingApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(ToolingApi)

    private File projectDir
    private BasicGradleDistribution dist
    private Closure getProjectDir
    private File userHomeDir

    private final List<Closure> connectorConfigurers = []
    boolean isEmbedded
    boolean verboseLogging = true

    ToolingApi(GradleDistribution dist) {
        this(dist, dist.userHomeDir, { dist.testDir }, GradleDistributionExecuter.systemPropertyExecuter == GradleDistributionExecuter.Executer.embedded)
    }

    ToolingApi(BasicGradleDistribution dist, File userHomeDir, Closure getProjectDir, boolean isEmbedded) {
        this.dist = dist
        this.userHomeDir = userHomeDir
        this.getProjectDir = getProjectDir
        this.isEmbedded = isEmbedded
    }

    void withConnector(Closure cl) {
        connectorConfigurers << cl
    }

    public <T> T withConnection(Closure<T> cl) {
        GradleConnector connector = connector()
        withConnection(connector, cl)
    }

    public <T> T withConnection(GradleConnector connector, Closure<T> cl) {
        try {
            return withConnectionRaw(connector, cl)
        } catch (UnsupportedVersionException e) {
            throw new IntegrationTestHint(e);
        }
    }

    public Throwable maybeFailWithConnection(Closure cl) {
        GradleConnector connector = connector()
        try {
            withConnectionRaw(connector, cl)
            return null
        } catch (Throwable e) {
            return e
        }
    }

    private <T> T withConnectionRaw(GradleConnector connector, Closure<T> cl) {
        ProjectConnection connection = connector.connect()
        try {
            return cl.call(connection)
        } finally {
            connection.close()
        }
    }

    GradleConnector connector() {
        GradleConnector connector = GradleConnector.newConnector()
        connector.useGradleUserHomeDir(userHomeDir)
        connector.forProjectDirectory(getProjectDir().absoluteFile)
        connector.searchUpwards(false)
        connector.daemonMaxIdleTime(60, TimeUnit.SECONDS)
        if (connector.metaClass.hasProperty(connector, 'verboseLogging')) {
            connector.verboseLogging = verboseLogging
        }
        if (isEmbedded) {
            LOGGER.info("Using embedded tooling API provider");
            connector.useClasspathDistribution()
            connector.embedded(true)
        } else {
            LOGGER.info("Using daemon tooling API provider");
            connector.useInstallation(dist.gradleHomeDir.absoluteFile)
            connector.embedded(false)
        }
        connectorConfigurers.each {
            it.call(connector)
        }
        return connector
    }
}
