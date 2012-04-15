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
package org.gradle.cache.internal;

import org.gradle.internal.Factory;

import java.util.concurrent.Callable;

/**
 * Provides synchronization with other processes for a particular file.
 */
public interface FileAccess {
    /**
     * Runs the given action under a shared or exclusive lock on the target file.
     *
     * <p>If an exclusive or shared lock is already held, the lock level is not changed and the action is executed. If no lock is already held,
     * a shared lock is acquired, the action executed, and the lock released. This method blocks until the lock can be acquired.
     *
     * @throws LockTimeoutException On timeout acquiring lock, if required.
     * @throws IllegalStateException When this lock has been closed.
     * @throws FileIntegrityViolationException If the integrity of the file cannot be guaranteed (never thrown if target is a directory)
     */
    <T> T readFile(Callable<? extends T> action) throws LockTimeoutException, FileIntegrityViolationException;

    /**
     * Runs the given action under a shared or exclusive lock on the target file.
     *
     * <p>If an exclusive or shared lock is already held, the lock level is not changed and the action is executed. If no lock is already held,
     * a shared lock is acquired, the action executed, and the lock released. This method blocks until the lock can be acquired.
     *
     * @throws LockTimeoutException On timeout acquiring lock, if required.
     * @throws IllegalStateException When this lock has been closed.
     * @throws FileIntegrityViolationException If the integrity of the file cannot be guaranteed (never thrown if target is a directory)
     */
    <T> T readFile(Factory<? extends T> action) throws LockTimeoutException, FileIntegrityViolationException;

    /**
     * Runs the given action under an exclusive lock on the target file. If the given action fails, the lock is marked as uncleanly unlocked.
     *
     * <p>If an exclusive lock is already held, the lock level is not changed and the action is executed. If a shared lock is already held,
     * the lock is escalated to an exclusive lock, and reverted back to a shared lock when the action completes. If no lock is already held, an
     * exclusive lock is acquired, the action executed, and the lock released.
     *
     * @throws LockTimeoutException On timeout acquiring lock, if required.
     * @throws IllegalStateException When this lock has been closed.
     * @throws FileIntegrityViolationException If the integrity of the file cannot be guaranteed (never thrown if target is a directory)
     */
    void updateFile(Runnable action) throws LockTimeoutException, FileIntegrityViolationException;

    /**
     * Runs the given action under an exclusive lock on the target file, without checking it's integrity. If the given action fails, the lock is marked as uncleanly unlocked.
     *
     * <p>This method should be used when it is of no consequence if the target was not previously unlocked, e.g. the content is being replaced.
     *
     * <p>Besides not performing integrity checking, this method shares the locking semantics of {@link #updateFile(Runnable)}
     *
     * @throws LockTimeoutException On timeout acquiring lock, if required.
     * @throws IllegalStateException When this lock has been closed.
     */
    void writeFile(Runnable action) throws LockTimeoutException;

}
