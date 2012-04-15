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
package org.gradle.build.docs.dsl.docbook

import org.gradle.build.docs.XIncludeAwareXmlProvider
import org.gradle.build.docs.dsl.TypeNameResolver
import org.gradle.build.docs.dsl.model.ClassExtensionMetaData
import org.gradle.build.docs.dsl.model.ClassMetaData
import org.gradle.build.docs.model.ClassMetaDataRepository
import org.w3c.dom.Document

class DslDocModel {
    private final File classDocbookDir
    private final Document document
    private final Map<String, ClassDoc> classes = [:]
    private final ClassMetaDataRepository<ClassMetaData> classMetaData
    private final Map<String, ClassExtensionMetaData> extensionMetaData
    private final JavadocConverter javadocConverter

    DslDocModel(File classDocbookDir, Document document, ClassMetaDataRepository<ClassMetaData> classMetaData, Map<String, ClassExtensionMetaData> extensionMetaData) {
        this.classDocbookDir = classDocbookDir
        this.document = document
        this.classMetaData = classMetaData
        this.extensionMetaData = extensionMetaData
        javadocConverter = new JavadocConverter(document, new JavadocLinkConverter(document, new TypeNameResolver(classMetaData), new LinkRenderer(document, this), classMetaData))
    }

    boolean isKnownType(String className) {
        return classMetaData.find(className) != null
    }

    ClassDoc getClassDoc(String className) {
        ClassDoc classDoc = classes[className]
        if (classDoc == null) {
            classDoc = loadClassDoc(className)
            classes[className] = classDoc
        }
        return classDoc
    }

    private ClassDoc loadClassDoc(String className) {
        ClassMetaData classMetaData = classMetaData.find(className)
        if (!classMetaData) {
            if (!className.contains('.internal.')) {
                throw new RuntimeException("No meta-data found for class '$className'.")
            }
            classMetaData = new ClassMetaData(className)
        }
        try {
            ClassExtensionMetaData extensionMetaData = extensionMetaData[className]
            if (!extensionMetaData) {
                extensionMetaData = new ClassExtensionMetaData(className)
            }
            File classFile = new File(classDocbookDir, "${className}.xml")
            if (!classFile.isFile()) {
                throw new RuntimeException("Docbook source file not found for class '$className' in $classDocbookDir.")
            }
            XIncludeAwareXmlProvider provider = new XIncludeAwareXmlProvider()
            def doc = new ClassDoc(className, provider.parse(classFile), document, classMetaData, extensionMetaData, this, javadocConverter)
            doc.mergeContent()
            return doc
        } catch (ClassDocGenerationException e) {
            throw e
        } catch (Exception e) {
            throw new ClassDocGenerationException("Could not load the class documentation for class '$className'.", e)
        }
    }
}
