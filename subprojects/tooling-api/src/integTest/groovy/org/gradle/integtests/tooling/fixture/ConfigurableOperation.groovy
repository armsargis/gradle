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

import org.gradle.tooling.LongRunningOperation
import org.gradle.tooling.ProgressListener

/**
 * by Szczepan Faber, created at: 12/19/11
 */
class ConfigurableOperation {

    LongRunningOperation operation

    def progressMessages = []
    def listener = { event -> progressMessages << event.description } as ProgressListener
    def stdout = new ByteArrayOutputStream()
    def stderr = new ByteArrayOutputStream()

    public ConfigurableOperation(LongRunningOperation operation) {
        init(operation)
    }

    public ConfigurableOperation() {}

    void init(LongRunningOperation operation) {
        this.operation = operation
        this.operation.addProgressListener(listener)
        this.operation.standardOutput = stdout
        this.operation.standardError = stderr
    }

    String getStandardOutput() {
        return stdout.toString()
    }

    String getStandardError() {
        return stderr.toString()
    }

    ConfigurableOperation setStandardInput(String input) {
        this.operation.standardInput = new ByteArrayInputStream(input.toString().bytes)
        return this
    }

    List getProgressMessages() {
        return progressMessages
    }
}
