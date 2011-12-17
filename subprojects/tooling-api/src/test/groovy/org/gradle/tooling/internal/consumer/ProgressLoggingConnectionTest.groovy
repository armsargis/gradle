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
package org.gradle.tooling.internal.consumer

import org.gradle.listener.ListenerManager
import org.gradle.logging.ProgressLogger
import org.gradle.logging.ProgressLoggerFactory
import spock.lang.Specification
import org.gradle.tooling.internal.protocol.*

class ProgressLoggingConnectionTest extends Specification {
    final ConnectionVersion4 target = Mock()
    final BuildOperationParametersVersion1 params = Mock()
    final ProgressListenerVersion1 listener = Mock()
    final ProgressLogger progressLogger = Mock()
    final ProgressLoggerFactory progressLoggerFactory = Mock()
    final ListenerManager listenerManager = Mock()
    final LoggingProvider loggingProvider = Mock()
    final ProgressLoggingConnection connection = new ProgressLoggingConnection(target, loggingProvider)

    def notifiesProgressListenerOfStartAndEndOfFetchingModel() {
        when:
        connection.getModel(ProjectVersion3, params)

        then:
        1 * loggingProvider.getListenerManager() >> listenerManager
        1 * loggingProvider.getProgressLoggerFactory() >> progressLoggerFactory
        1 * listenerManager.addListener(!null)
        1 * progressLoggerFactory.newOperation(ProgressLoggingConnection.class) >> progressLogger
        1 * progressLogger.setDescription('Load projects')
        1 * progressLogger.started()
        1 * target.getModel(ProjectVersion3, params)
        1 * progressLogger.completed()
        1 * listenerManager.removeListener(!null)
        _ * params.progressListener >> listener
        0 * _._
    }

    def notifiesProgressListenerOfStartAndEndOfExecutingBuild() {
        BuildParametersVersion1 buildParams = Mock()

        when:
        connection.executeBuild(buildParams, params)

        then:
        1 * loggingProvider.getListenerManager() >> listenerManager
        1 * loggingProvider.getProgressLoggerFactory() >> progressLoggerFactory
        1 * listenerManager.addListener(!null)
        1 * progressLoggerFactory.newOperation(ProgressLoggingConnection.class) >> progressLogger
        1 * progressLogger.setDescription('Execute build')
        1 * progressLogger.started()
        1 * target.executeBuild(buildParams, params)
        1 * progressLogger.completed()
        1 * listenerManager.removeListener(!null)
        _ * params.progressListener >> listener
        0 * _._
    }
}
