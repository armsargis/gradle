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
package org.gradle.api.internal.file

import org.gradle.util.Requires
import org.gradle.util.TemporaryFolder
import org.gradle.util.TestPrecondition
import org.gradle.os.FileSystems

import org.junit.Rule
import spock.lang.Specification

class BaseDirFileResolverSpec extends Specification {
    @Rule TemporaryFolder tmpDir = new TemporaryFolder()

    @Requires(TestPrecondition.SYMLINKS)
    def "normalizes absolute path which points to an absolute link"() {
        def target = createFile(new File(tmpDir.dir, 'target.txt'))
        def file = new File(tmpDir.dir, 'a/other.txt')
        createLink(file, target)
        assert file.exists() && file.file

        expect:
        normalize(file) == file
    }

    @Requires(TestPrecondition.SYMLINKS)
    def "normalizes absolute path which points to a relative link"() {
        def target = createFile(new File(tmpDir.dir, 'target.txt'))
        def file = new File(tmpDir.dir, 'a/other.txt')
        createLink(file, '../target.txt')
        assert file.exists() && file.file

        expect:
        normalize(file) == file
    }

    @Requires(TestPrecondition.CASE_INSENSITIVE_FS)
    def "normalizes absolute path which has mismatched case"() {
        def file = createFile(new File(tmpDir.dir, 'dir/file.txt'))
        def path = new File(tmpDir.dir, 'dir/FILE.txt')
        assert path.exists() && path.file

        expect:
        normalize(path) == file
    }

    @Requires([TestPrecondition.SYMLINKS, TestPrecondition.CASE_INSENSITIVE_FS])
    def "normalizes absolute path which points to a target using mismatched case"() {
        def target = createFile(new File(tmpDir.dir, 'target.txt'))
        def file = new File(tmpDir.dir, 'dir/file.txt')
        createLink(file, target)
        def path = new File(tmpDir.dir, 'dir/FILE.txt')
        assert path.exists() && path.file

        expect:
        normalize(path) == file
    }

    @Requires(TestPrecondition.SYMLINKS)
    def "normalizes path which points to a link to something that does not exist"() {
        def file = new File(tmpDir.dir, 'a/other.txt')
        createLink(file, 'unknown.txt')
        assert !file.exists() && !file.file

        expect:
        normalize(file) == file
    }

    @Requires(TestPrecondition.SYMLINKS)
    def "normalizes path when ancestor is an absolute link"() {
        def target = createFile(new File(tmpDir.dir, 'target/file.txt'))
        def file = new File(tmpDir.dir, 'a/b/file.txt')
        createLink(file.parentFile, target.parentFile)
        assert file.exists() && file.file

        expect:
        normalize(file) == file
    }

    @Requires(TestPrecondition.CASE_INSENSITIVE_FS)
    def "normalizes path when ancestor has mismatched case"() {
        def file = createFile(new File(tmpDir.dir, "a/b/file.txt"))
        def path = new File(tmpDir.dir, "A/b/file.txt")
        assert file.exists() && file.file

        expect:
        normalize(path) == file
    }

    @Requires(TestPrecondition.CASE_INSENSITIVE_FS)
    def "normalizes ancestor with mismatched case when target file does not exist"() {
        tmpDir.createDir("a")
        def file = new File(tmpDir.dir, "a/b/file.txt")
        def path = new File(tmpDir.dir, "A/b/file.txt")

        expect:
        normalize(path) == file
    }

    def "normalizes relative path"() {
        def ancestor = new File(tmpDir.dir, "test")
        def baseDir = new File(ancestor, "base")
        def sibling = new File(ancestor, "sub")
        def child = createFile(new File(baseDir, "a/b/file.txt"))

        expect:
        normalize("a/b/file.txt", baseDir) == child
        normalize("./a/b/file.txt", baseDir) == child
        normalize(".//a/b//file.txt", baseDir) == child
        normalize("sub/../a/b/file.txt", baseDir) == child
        normalize("../sub", baseDir) == sibling
        normalize("..", baseDir) == ancestor
        normalize(".", baseDir) == baseDir
    }

    @Requires(TestPrecondition.SYMLINKS)
    def "normalizes relative path when base dir is a link"() {
        def target = createFile(new File(tmpDir.dir, 'target/file.txt'))
        def baseDir = new File(tmpDir.dir, 'base')
        createLink(baseDir, "target")
        def file = new File(baseDir, 'file.txt')
        assert file.exists() && file.file

        expect:
        normalize('file.txt', baseDir) == file
    }

    @Requires(TestPrecondition.WINDOWS)
    def "normalizes path which uses windows 8.3 name"() {
        def file = createFile(new File(tmpDir.dir, 'dir/file-with-long-name.txt'))
        def path = new File(tmpDir.dir, 'dir/FILE-W~1.TXT')
        assert path.exists() && path.file

        expect:
        normalize(path) == file
    }

    def "normalizes file system roots"() {
        expect:
        normalize(root) == root

        where:
        root << File.listRoots()
    }

    @Requires(TestPrecondition.WINDOWS)
    def "normalizes non-existent file system root"() {
        def file = new File("Q:\\")
        assert !file.exists()
        assert file.absolute

        expect:
        normalize(file) == file
    }

    def "normalizes relative path that refers to ancestor of file system root"() {
        File root = File.listRoots()[0]

        expect:
        normalize("../../..", root) == root
    }

    def createLink(File link, File target) {
        link.parentFile.mkdirs()
        FileSystems.default.createSymbolicLink(link, target)
    }

    def createLink(File link, String target) {
        createLink(link, new File(target))
    }

    def createFile(File file) {
        file.parentFile.mkdirs()
        file.text = 'content'
        file
    }

    def normalize(Object path, File baseDir = tmpDir.dir) {
        new BaseDirFileResolver(baseDir).resolve(path)
    }
}
