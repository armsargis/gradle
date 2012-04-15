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
package org.gradle.launcher.daemon.context

import org.gradle.internal.nativeplatform.ProcessEnvironment
import org.gradle.util.ConfigureUtil
import org.gradle.util.TemporaryFolder
import org.junit.Rule
import spock.lang.Specification

class DaemonCompatibilitySpecSpec extends Specification {

    @Rule TemporaryFolder tmp = new TemporaryFolder()

    def clientConfigure = {}
    def serverConfigure = {}

    def client(Closure c) {
        clientConfigure = c
    }

    def server(Closure c) {
        serverConfigure = c
    }

    def createContext(Closure config) {
        def builder = new DaemonContextBuilder({ 12L } as ProcessEnvironment)
        builder.daemonRegistryDir = new File("dir")
        ConfigureUtil.configure(config, builder).create()
    }

    def getClientContext() {
        createContext(clientConfigure)
    }

    def getServerContext() {
        createContext(serverConfigure)
    }

    boolean isCompatible() {
        new DaemonCompatibilitySpec(clientContext).isSatisfiedBy(serverContext)
    }

    def "default contexts are compatible"() {
        expect:
        compatible
    }

    def "contexts with different java homes are incompatible"() {
        client { javaHome = tmp.createDir("a") }
        server { javaHome = tmp.createDir("b") }

        expect:
        !compatible
    }

    def "contexts with same daemon opts are compatible"() {
        client { daemonOpts = ["-Xmx256m", "-Dfoo=foo"] }
        server { daemonOpts = ["-Xmx256m", "-Dfoo=foo"] }

        expect:
        compatible
    }

    def "contexts with same daemon opts but different order are compatible"() {
        client { daemonOpts = ["-Xmx256m", "-Dfoo=foo"] }
        server { daemonOpts = ["-Dfoo=foo", "-Xmx256m"] }

        expect:
        compatible
    }

    def "contexts with different quantity of opts are not compatible"() {
        client { daemonOpts = ["-Xmx256m", "-Dfoo=foo"] }
        server { daemonOpts = ["-Xmx256m"] }

        expect:
        !compatible
    }

    def "contexts with different daemon opts are incompatible"() {
        client { daemonOpts = ["-Xmx256m", "-Dfoo=foo"] }
        server { daemonOpts = ["-Xmx256m", "-Dfoo=bar"] }

        expect:
        !compatible
    }
}