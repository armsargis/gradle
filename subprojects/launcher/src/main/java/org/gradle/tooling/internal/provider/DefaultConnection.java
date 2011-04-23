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
package org.gradle.tooling.internal.provider;

import org.gradle.GradleLauncher;
import org.gradle.api.internal.project.ServiceRegistry;
import org.gradle.initialization.DefaultGradleLauncherFactory;
import org.gradle.initialization.GradleLauncherAction;
import org.gradle.initialization.GradleLauncherFactory;
import org.gradle.launcher.GradleLauncherActionExecuter;
import org.gradle.logging.LoggingServiceRegistry;
import org.gradle.logging.internal.LoggingOutputInternal;
import org.gradle.tooling.internal.protocol.*;
import org.gradle.util.GradleVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultConnection implements ConnectionVersion4 {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultConnection.class);
    private final ServiceRegistry loggingServices;
    private final GradleLauncherFactory gradleLauncherFactory;

    public DefaultConnection() {
        LOGGER.debug("Using tooling API provider version {}.", GradleVersion.current().getVersion());
        loggingServices = new LoggingServiceRegistry(false);
        gradleLauncherFactory = new DefaultGradleLauncherFactory(loggingServices);
        GradleLauncher.injectCustomFactory(gradleLauncherFactory);
    }

    public ConnectionMetaDataVersion1 getMetaData() {
        return new ConnectionMetaDataVersion1() {
            public String getVersion() {
                return GradleVersion.current().getVersion();
            }

            public String getDisplayName() {
                return String.format("Gradle %s", getVersion());
            }
        };
    }

    public void stop() {
    }

    public void executeBuild(final BuildParametersVersion1 buildParameters, BuildOperationParametersVersion1 operationParameters) {
        run(operationParameters, new ExecuteBuildAction(buildParameters));
    }

    public ProjectVersion3 getModel(Class<? extends ProjectVersion3> type, BuildOperationParametersVersion1 operationParameters) {
        GradleLauncherAction<ProjectVersion3> action = new DelegatingBuildModelAction(type);
        return run(operationParameters, action);
    }

    private <T> T run(BuildOperationParametersVersion1 operationParameters, GradleLauncherAction<T> action) {
        GradleLauncherActionExecuter<BuildOperationParametersVersion1> executer = createExecuter(operationParameters);
        return executer.execute(action, operationParameters);
    }

    private GradleLauncherActionExecuter<BuildOperationParametersVersion1> createExecuter(BuildOperationParametersVersion1 operationParameters) {
        return new LocalGradleLauncherActionExecuter(gradleLauncherFactory, loggingServices.get(LoggingOutputInternal.class));
    }
}