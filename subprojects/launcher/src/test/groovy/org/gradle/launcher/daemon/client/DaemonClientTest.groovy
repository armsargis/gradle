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
package org.gradle.launcher.daemon.client

import org.gradle.api.specs.Spec
import org.gradle.api.specs.Specs
import org.gradle.initialization.BuildClientMetaData
import org.gradle.initialization.GradleLauncherAction
import org.gradle.launcher.daemon.context.DaemonContext
import org.gradle.launcher.exec.BuildActionParameters
import org.gradle.logging.internal.OutputEventListener
import org.gradle.messaging.remote.internal.Connection
import spock.lang.Specification
import org.gradle.launcher.daemon.protocol.*

class DaemonClientTest extends Specification {
    final DaemonConnector connector = Mock()
    final DaemonConnection daemonConnection = Mock()
    final Connection<Object> connection = Mock()
    final BuildClientMetaData metaData = Mock()
    final OutputEventListener outputEventListener = Mock()
    final Spec<DaemonContext> compatibilitySpec = Mock()
    final DaemonClient client = new DaemonClient(connector, metaData, outputEventListener, compatibilitySpec, System.in)

    def setup() {
        daemonConnection.getConnection() >> connection
    }

    def stopsTheDaemonWhenRunning() {
        when:
        client.stop()

        then:
        2 * connector.maybeConnect(Specs.satisfyAll()) >>> [daemonConnection, null]
        1 * connection.dispatch({it instanceof Stop})
        1 * connection.receive() >> new Success(null)
        1 * connection.stop()
        daemonConnection.getConnection() >> connection // why do I need this? Why doesn't the interaction specified in setup cover me?
        0 * _
    }

    def "stops all daemons"() {
        when:
        client.stop()

        then:
        3 * connector.maybeConnect(Specs.satisfyAll()) >>> [daemonConnection, daemonConnection, null]
        2 * connection.dispatch({it instanceof Stop})
        2 * connection.receive() >> new Success(null)
    }

    def stopsTheDaemonWhenNotRunning() {
        when:
        client.stop()

        then:
        1 * connector.maybeConnect(Specs.satisfyAll()) >> null
        0 * _
    }

    def executesAction() {
        GradleLauncherAction<String> action = Mock()
        BuildActionParameters parameters = Mock()

        when:
        def result = client.execute(action, parameters)

        then:
        result == '[result]'
        1 * connector.connect(compatibilitySpec) >> daemonConnection
        1 * connection.dispatch({it instanceof Build})
        2 * connection.receive() >>> [new BuildStarted(new Build(action, parameters)), new Success('[result]')]
        1 * connection.stop()
    }

    def rethrowsFailureToExecuteAction() {
        GradleLauncherAction<String> action = Mock()
        BuildActionParameters parameters = Mock()
        RuntimeException failure = new RuntimeException()

        when:
        client.execute(action, parameters)

        then:
        RuntimeException e = thrown()
        e == failure
        1 * connector.connect(compatibilitySpec) >> daemonConnection
        1 * connection.dispatch({it instanceof Build})
        2 * connection.receive() >>> [new BuildStarted(new Build(action, parameters)), new CommandFailure(failure)]
        1 * connection.stop()
    }
}
