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

import org.gradle.build.docs.dsl.model.ClassMetaData
import org.gradle.build.docs.dsl.model.PropertyMetaData
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.gradle.build.docs.dsl.model.MethodMetaData
import org.w3c.dom.Text
import org.gradle.build.docs.dsl.model.MixinMetaData
import org.gradle.build.docs.dsl.model.ClassExtensionMetaData
import org.gradle.build.docs.dsl.model.ExtensionMetaData

class ClassDoc {
    private final String className
    private final String id
    private final String simpleName
    final ClassMetaData classMetaData
    private final Element classSection
    private final ClassExtensionMetaData extensionMetaData
    private final List<PropertyDoc> classProperties = []
    private final List<MethodDoc> classMethods = []
    private final List<BlockDoc> classBlocks = []
    private final List<ClassExtensionDoc> classExtensions = []
    private final JavadocConverter javadocConverter
    private final DslDocModel model
    private final Element propertiesTable
    private final Element methodsTable
    private final Element propertiesSection
    private final Element methodsSection
    private List<Element> comment
    private final GenerationListener listener = new DefaultGenerationListener()

    ClassDoc(String className, Element classContent, Document targetDocument, ClassMetaData classMetaData, ClassExtensionMetaData extensionMetaData, DslDocModel model, JavadocConverter javadocConverter) {
        this.className = className
        id = className
        simpleName = className.tokenize('.').last()
        this.classMetaData = classMetaData
        this.javadocConverter = javadocConverter
        this.model = model
        this.extensionMetaData = extensionMetaData

        classSection = targetDocument.createElement('chapter')

        classContent.childNodes.each { Node n ->
            classSection << n
        }

        propertiesTable = getTable('Properties')
        propertiesSection = propertiesTable.parentNode
        methodsTable = getTable('Methods')
        methodsSection = methodsTable.parentNode
    }

    def getId() { return id }

    def getName() { return className }

    def getSimpleName() { return simpleName }

    def getComment() { return comment }

    def getClassProperties() { return classProperties }

    def getClassMethods() { return classMethods }

    def getClassBlocks() { return classBlocks }

    def getClassExtensions() { return classExtensions }

    def getClassSection() { return classSection }

    def getPropertiesTable() { return propertiesTable }

    def getPropertiesSection() { return propertiesSection }

    def getPropertyDetailsSection() { return getSection('Property details') }

    def getMethodsTable() { return methodsTable }

    def getMethodsSection() { return methodsSection }

    def getMethodDetailsSection() { return getSection('Method details') }

    def getBlocksTable() { return getTable('Script blocks') }

    def getBlockDetailsSection() { return getSection('Script block details') }

    ClassDoc mergeContent() {
        buildDescription()
        buildProperties()
        buildMethods()
        buildExtensions()
        return this
    }

    ClassDoc buildDescription() {
        comment = javadocConverter.parse(classMetaData, listener).docbook
        return this
    }

    ClassDoc buildProperties() {
        List<Element> header = propertiesTable.thead.tr[0].td.collect { it }
        if (header.size() < 1) {
            throw new RuntimeException("Expected at least 1 <td> in <thead>/<tr>, found: $header")
        }
        Map<String, Element> inheritedValueTitleMapping = [:]
        List<Element> valueTitles = []
        header.eachWithIndex { element, index ->
            if (index == 0) { return }
            Element override = element.overrides[0]
            if (override) {
                element.removeChild(override)
                inheritedValueTitleMapping.put(override.textContent, element)
            }
            if (element.firstChild instanceof Text) {
                element.firstChild.textContent = element.firstChild.textContent.replaceFirst(/^\s+/, '')
            }
            if (element.lastChild instanceof Text) {
                element.lastChild.textContent = element.lastChild.textContent.replaceFirst(/\s+$/, '')
            }
            valueTitles.add(element)
        }

        ClassDoc superClass = classMetaData.superClassName ? model.getClassDoc(classMetaData.superClassName) : null
        //adding the properties from the super class onto the inheriting class
        Map<String, PropertyDoc> props = new TreeMap<String, PropertyDoc>()
        if (superClass) {
            superClass.getClassProperties().each { propertyDoc ->
                def additionalValues = new LinkedHashMap<String, ExtraAttributeDoc>()
                propertyDoc.additionalValues.each { attributeDoc ->
                    def key = attributeDoc.key
                    if (inheritedValueTitleMapping[key]) {
                        ExtraAttributeDoc newAttribute = new ExtraAttributeDoc(inheritedValueTitleMapping[key], attributeDoc.valueCell)
                        additionalValues.put(newAttribute.key, newAttribute)
                    } else {
                        additionalValues.put(key, attributeDoc)
                    }
                }

                props[propertyDoc.name] = propertyDoc.forClass(classMetaData, additionalValues.values() as List)
            }
        }

        propertiesTable.tr.each { Element tr ->
            def cells = tr.td.collect { it }
            if (cells.size() != header.size()) {
                throw new RuntimeException("Expected ${header.size()} <td> elements in <tr>, found: $tr")
            }
            String propName = cells[0].text().trim()
            PropertyMetaData property = classMetaData.findProperty(propName)
            if (!property) {
                throw new RuntimeException("No metadata for property '$className.$propName'. Available properties: ${classMetaData.propertyNames}")
            }

            def additionalValues = new LinkedHashMap<String, ExtraAttributeDoc>()

            if (superClass) {
                def overriddenProp = props.get(propName)
                if (overriddenProp) {
                    overriddenProp.additionalValues.each { attributeDoc ->
                        additionalValues.put(attributeDoc.key, attributeDoc)
                    }
                }
            }

            header.eachWithIndex { col, i ->
                if (i == 0 || !cells[i].firstChild) { return }
                def attributeDoc = new ExtraAttributeDoc(valueTitles[i-1], cells[i])
                additionalValues.put(attributeDoc.key, attributeDoc)
            }
            PropertyDoc propertyDoc = new PropertyDoc(property, javadocConverter.parse(property, listener).docbook, additionalValues.values() as List)
            if (propertyDoc.description == null) {
                throw new RuntimeException("Docbook content for '$className.$propName' does not contain a description paragraph.")
            }

            props[propName] = propertyDoc
        }

        classProperties.addAll(props.values())

        return this
    }

    ClassDoc buildMethods() {
        Set signatures = [] as Set

        methodsTable.tr.each { Element tr ->
            def cells = tr.td
            if (cells.size() != 1) {
                throw new RuntimeException("Expected 1 cell in <tr>, found: $tr")
            }
            String methodName = cells[0].text().trim()
            Collection<MethodMetaData> methods = classMetaData.declaredMethods.findAll { it.name == methodName }
            if (!methods) {
                throw new RuntimeException("No metadata for method '$className.$methodName()'. Available methods: ${classMetaData.declaredMethods.collect {it.name} as TreeSet}")
            }
            methods.each { method ->
                def methodDoc = new MethodDoc(method, javadocConverter.parse(method, listener).docbook)
                if (!methodDoc.description) {
                    throw new RuntimeException("Docbook content for '$className $method.signature' does not contain a description paragraph.")
                }
                def property = findProperty(method.name)
                def multiValued = false
                if (method.parameters.size() == 1 && method.parameters[0].type.signature == Closure.class.name && property) {
                    def type = property.metaData.type
                    if (type.name == 'java.util.List' || type.name == 'java.util.Collection' || type.name == 'java.util.Set' || type.name == 'java.util.Iterable') {
                        type = type.typeArgs[0]
                        multiValued = true
                    }
                    classBlocks << new BlockDoc(methodDoc, property, type, multiValued)
                } else {
                    classMethods << methodDoc
                    signatures << method.overrideSignature
                }
            }
        }

        if (classMetaData.superClassName) {
            ClassDoc supertype = model.getClassDoc(classMetaData.superClassName)
            supertype.getClassMethods().each { method ->
                if (signatures.add(method.metaData.overrideSignature)) {
                    classMethods << method.forClass(classMetaData)
                }
            }
        }

        classMethods.sort { it.metaData.overrideSignature }
        classBlocks.sort { it.name }

        return this
    }

    ClassDoc buildExtensions() {
        def plugins = [:]
        extensionMetaData.mixinClasses.each { MixinMetaData mixin ->
            def pluginId = mixin.pluginId
            def classExtensionDoc = plugins[pluginId]
            if (!classExtensionDoc) {
                classExtensionDoc = new ClassExtensionDoc(pluginId, classMetaData)
                plugins[pluginId] = classExtensionDoc
            }
            classExtensionDoc.mixinClasses << model.getClassDoc(mixin.mixinClass)
        }
        extensionMetaData.extensionClasses.each { ExtensionMetaData extension ->
            def pluginId = extension.pluginId
            def classExtensionDoc = plugins[pluginId]
            if (!classExtensionDoc) {
                classExtensionDoc = new ClassExtensionDoc(pluginId, classMetaData)
                plugins[pluginId] = classExtensionDoc
            }
            classExtensionDoc.extensionClasses[extension.extensionId] = model.getClassDoc(extension.extensionClass)
        }

        classExtensions.addAll(plugins.values())
        classExtensions.each { extension -> extension.buildMetaData(model) }
        classExtensions.sort { it.pluginId }

        return this
    }

    String getStyle() {
        return classMetaData.groovy ? 'groovydoc' : 'javadoc'
    }

    private Element getTable(String title) {
        def table = getSection(title).table[0]
        if (!table) {
            throw new RuntimeException("Section '$title' does not contain a <table> element.")
        }
        if (!table.thead[0]) {
            throw new RuntimeException("Table '$title' does not contain a <thead> element.")
        }
        if (!table.thead[0].tr[0]) {
            throw new RuntimeException("Table '$title' does not contain a <thead>/<tr> element.")
        }
        return table
    }

    private Element getSection(String title) {
        def sections = classSection.section.findAll {
            it.title[0] && it.title[0].text().trim() == title
        }
        if (sections.size() < 1) {
            throw new RuntimeException("Docbook content for $className does not contain a '$title' section.")
        }
        return sections[0]
    }

    Element getHasDescription() {
        def paras = classSection.para
        return paras.size() > 0 ? paras[0] : null
    }

    Element getDescription() {
        def paras = classSection.para
        if (paras.size() < 1) {
            throw new RuntimeException("Docbook content for $className does not contain a description paragraph.")
        }
        return paras[0]
    }

    PropertyDoc findProperty(String name) {
        return classProperties.find { it.name == name }
    }

    BlockDoc getBlock(String name) {
        def block = classBlocks.find { it.name == name }
        if (block) {
            return block
        }
        for (extensionDoc in classExtensions) {
            block = extensionDoc.extensionBlocks.find { it.name == name }
            if (block) {
                return block
            }
        }
        throw new RuntimeException("Class $className does not have a script block '$name'.")
    }
}
