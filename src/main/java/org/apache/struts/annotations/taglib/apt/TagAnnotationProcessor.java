/*
 * $Id:  $
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.struts.annotations.taglib.apt;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.NoType;
import javax.lang.model.util.ElementFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.*;
import java.util.Map.Entry;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({TagAnnotationProcessor.TAG, TagAnnotationProcessor.TAG_ATTRIBUTE, TagAnnotationProcessor.TAG_SKIP_HIERARCHY})
public class TagAnnotationProcessor extends AbstractProcessor {
    public static final String TAG = "org.apache.struts2.views.annotations.StrutsTag";
    public static final String TAG_ATTRIBUTE = "org.apache.struts2.views.annotations.StrutsTagAttribute";
    public static final String TAG_SKIP_HIERARCHY = "org.apache.struts2.views.annotations.StrutsTagSkipInheritance";

    private Map<String, Tag> tags = new TreeMap<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // make sure all paramters were set
        checkOptions();

        // tags
        TypeElement tagAnnotationType = processingEnv.getElementUtils().getTypeElement(TAG);
        TypeElement attributeAnnotationType = processingEnv.getElementUtils().getTypeElement(TAG_ATTRIBUTE);
        TypeElement skipAnnotationType = processingEnv.getElementUtils().getTypeElement(TAG_SKIP_HIERARCHY);
        Set<? extends javax.lang.model.element.Element> tagDeclarations = roundEnv.getElementsAnnotatedWith(tagAnnotationType);
        Set<? extends javax.lang.model.element.Element> attributesDeclarations = roundEnv.getElementsAnnotatedWith(attributeAnnotationType);
        Set<? extends javax.lang.model.element.Element> skipDeclarations = roundEnv.getElementsAnnotatedWith(skipAnnotationType);

        // find Tags
        for (javax.lang.model.element.Element element : tagDeclarations) {
            Map<String, Object> values = getValues(element, tagAnnotationType);
            TypeElement type = (TypeElement) element;
            Tag tag = new Tag();
            tag.setDescription((String) values.get("description"));
            tag.setName((String) values.get("name"));
            tag.setTldBodyContent((String) values.get("tldBodyContent"));
            tag.setTldTagClass((String) values.get("tldTagClass"));
            tag.setDeclaredType(type.getQualifiedName().toString());
            tag.setAllowDynamicAttributes((Boolean) values.get("allowDynamicAttributes"));
            // add to map
            tags.put(type.getQualifiedName().toString(), tag);
        }

        //find attributes to be skipped
        for (javax.lang.model.element.Element declaration : skipDeclarations) {
            //types will be ignored when hierarchy is scanned
            if (declaration instanceof ExecutableElement) {
                ExecutableElement methodDeclaration = (ExecutableElement) declaration;
                String typeName = ((TypeElement) methodDeclaration.getEnclosingElement()).getQualifiedName().toString();
                String methodName = methodDeclaration.getSimpleName().toString();
                String name = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
                Tag tag = tags.get(typeName);
                if (tag != null) {
                    //if it is on an abstract class, there is not tag for it at this point
                    tags.get(typeName).addSkipAttribute(name);
                }
            }
        }

        // find Tags Attributes
        for (javax.lang.model.element.Element declaration : attributesDeclarations) {
            // type
            ExecutableElement methodDeclaration = (ExecutableElement) declaration;
            String typeName = ((TypeElement) methodDeclaration.getEnclosingElement()).getQualifiedName().toString();
            Map<String, Object> values = getValues(methodDeclaration,
                    attributeAnnotationType);
            // create Attribute and apply values found
            TagAttribute attribute = new TagAttribute();
            String name = (String) values.get("name");
            if (name == null || name.length() == 0) {
                // get name from method
                String methodName = methodDeclaration.getSimpleName().toString();
                name = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
            }
            values.put("name", name);
            populateTagAttributes(attribute, values);
            // add to map
            Tag parentTag = tags.get(typeName);
            if (parentTag != null) {
                tags.get(typeName).addTagAttribute(attribute);
            } else {
                // an abstract or base class
                parentTag = new Tag();
                parentTag.setDeclaredType(typeName);
                parentTag.setInclude(false);
                parentTag.addTagAttribute(attribute);
                tags.put(typeName, parentTag);
            }
        }

        // we can't process the hierarchy on the first pass because
        // apt does not guarantee that the base classes will be processed
        // before their subclasses
        for (Map.Entry<String, Tag> entry : tags.entrySet()) {
            processHierarchy(entry.getValue());
        }

        // save
        saveAsXml();
        saveTemplates();
        return true;
    }

    private static void populateTagAttributes(TagAttribute attribute, Map<String, Object> values) {
        attribute.setRequired((Boolean) values.get("required"));
        attribute.setRtexprvalue((Boolean) values.get("rtexprvalue"));
        attribute.setDefaultValue((String) values.get("defaultValue"));
        attribute.setType((String) values.get("type"));
        attribute.setDescription((String) values.get("description"));
        attribute.setName((String) values.get("name"));
    }

    private void processHierarchy(Tag tag) {
        TypeElement type = processingEnv.getElementUtils().getTypeElement(tag.getDeclaredType());
        List<String> skipAttributes = tag.getSkipAttributes();
        while (type != null && !(type instanceof NoType) && getAnnotation(type, TAG_SKIP_HIERARCHY) == null) {
            Tag parentTag = tags.get(type.getQualifiedName().toString());
            // copy parent annotations to this tag
            if (parentTag != null) {
                for (TagAttribute attribute : parentTag.getAttributes()) {
                    if (!skipAttributes.contains(attribute.getName())) {
                        tag.addTagAttribute(attribute);
                    }
                }
            } else {
                // Maybe the parent class is already compiled
                addTagAttributesFromParent(tag, type);
            }
            type = (TypeElement) processingEnv.getTypeUtils().asElement(type.getSuperclass());
        }
    }

    private void addTagAttributesFromParent(Tag tag, TypeElement type) {
        for (ExecutableElement method : ElementFilter.methodsIn(processingEnv.getElementUtils().getAllMembers(type))) {
            AnnotationMirror annotation = getAnnotation(method, TAG_ATTRIBUTE);
            if (method.getModifiers().contains(Modifier.PUBLIC) && annotation != null) {
                String name = String.valueOf(Character.toLowerCase(method.getSimpleName()
                        .charAt(3)))
                        + method.getSimpleName().subSequence(4, method.getSimpleName().length());
                if (!tag.getSkipAttributes().contains(name)) {
                    Map<String, Object> values = new HashMap<>();
                    for (Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : processingEnv.getElementUtils().getElementValuesWithDefaults(annotation).entrySet()) {
                        values.put(entry.getKey().getSimpleName().toString(), entry.getValue().getValue());
                    }
                    TagAttribute attribute = new TagAttribute();
                    populateTagAttributes(attribute, values);
                    attribute.setName(name);
                    tag.addTagAttribute(attribute);
                }
            }
        }
    }

    private AnnotationMirror getAnnotation(javax.lang.model.element.Element element, String annotationName) {
        TypeElement annotation = processingEnv.getElementUtils().getTypeElement(annotationName);
        if (element != null && element.getAnnotationMirrors() != null) {
            for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
                if (mirror.getAnnotationType().asElement().equals(annotation)) {
                    return mirror;
                }
            }
        }
        return null;
    }

    private void checkOptions() {
        if (getOption("tlibVersion") == null)
            throw new IllegalArgumentException("'tlibVersion' is missing");
        if (getOption("jspVersion") == null)
            throw new IllegalArgumentException("'jspVersion' is missing");
        if (getOption("shortName") == null)
            throw new IllegalArgumentException("'shortName' is missing");
        if (getOption("description") == null)
            throw new IllegalArgumentException("'description' is missing");
        if (getOption("displayName") == null)
            throw new IllegalArgumentException("'displayName' is missing");
        if (getOption("uri") == null)
            throw new IllegalArgumentException("'uri' is missing");
        if (getOption("outTemplatesDir") == null)
            throw new IllegalArgumentException("'outTemplatesDir' is missing");
        if (getOption("outFile") == null)
            throw new IllegalArgumentException("'outFile' is missing");
    }

    private void saveTemplates() {
        // freemarker configuration
        Configuration config = new Configuration();
        config.setClassForTemplateLoading(getClass(), "");
        config.setObjectWrapper(new DefaultObjectWrapper());

        try {
            // load template
            Template tagDescription = config.getTemplate("tag-description.ftl");
            Template tagAttributes = config.getTemplate("tag-attributes.ftl");
            String outTemplatesDir = getOption("outTemplatesDir");
            if (outTemplatesDir == null) {
                throw new IllegalArgumentException("outTemplatesDir was not defined!");
            }
            String rootDir = (new File(outTemplatesDir)).getAbsolutePath();
            for (Tag tag : tags.values()) {
                if (tag.isInclude()) {
                    // model
                    HashMap<String, Tag> root = new HashMap<>();
                    root.put("tag", tag);

                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(
                            new File(rootDir, tag.getName() + "-description.html")))) {
                        tagDescription.process(root, writer);
                    }
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(
                            new File(rootDir, tag.getName() + "-attributes.html")))) {
                        tagAttributes.process(root, writer);
                    }
                }
            }
        } catch (Exception e) {
            // oops we cannot throw checked exceptions
            throw new RuntimeException(e);
        }
    }

    private void saveAsXml() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;

        try {
            // create xml document
            builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();
            document.setXmlVersion("1.0");

            // taglib
            Element tagLib = document.createElement("taglib");
            tagLib.setAttribute("xmlns", "http://java.sun.com/xml/ns/j2ee");
            tagLib.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            tagLib.setAttribute("xsi:schemaLocation", "http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-jsptaglibrary_2_0.xsd");
            tagLib.setAttribute("version", getOption("jspVersion"));
            document.appendChild(tagLib);
            // tag lib attributes
            appendTextNode(document, tagLib, "description",
                    getOption("description"), true);
            appendTextNode(document, tagLib, "display-name",
                    getOption("displayName"), false);
            appendTextNode(document, tagLib, "tlib-version",
                    getOption("tlibVersion"), false);
            appendTextNode(document, tagLib, "short-name",
                    getOption("shortName"), false);
            appendTextNode(document, tagLib, "uri", getOption("uri"), false);

            // create tags
            for (Map.Entry<String, Tag> entry : tags.entrySet()) {
                Tag tag = entry.getValue();
                if (tag.isInclude())
                    createElement(document, tagLib, tag);
            }

            // save to file
            TransformerFactory tf = TransformerFactory.newInstance();
            tf.setAttribute("indent-number", 2);
            Transformer transformer = tf.newTransformer();
            // if tiger would just format it :(
            // formatting bug in tiger
            // (http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6296446)

            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            //create output directory if it does not exists
            String outFile = getOption("outFile");
            if (outFile == null) {
                throw new IllegalArgumentException("outFile was not defined!");
            }
            File outputFile = new File(outFile);
            File parentDir = outputFile.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            Source source = new DOMSource(document);
            Result result = new StreamResult(new OutputStreamWriter(
                    new FileOutputStream(outputFile)));
            transformer.transform(source, result);
        } catch (Exception e) {
            // oops we cannot throw checked exceptions
            throw new RuntimeException(e);
        }
    }

    private String getOption(String name) {
        // there is a bug in the 1.5 apt implementation:
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6258929
        // this is a hack-around
        if (processingEnv.getOptions().containsKey(name)) {
            return processingEnv.getOptions().get(name);
        }
        for (Map.Entry<String, String> entry : processingEnv.getOptions()
                .entrySet()) {
            String key = entry.getKey();
            String[] splitted = key.split("=");
            if (splitted[0].equals("-A" + name))
                return splitted[1];
        }
        return null;
    }

    private static void createElement(Document doc, Element tagLibElement, Tag tag) {
        Element tagElement = doc.createElement("tag");
        tagLibElement.appendChild(tagElement);
        appendTextNode(doc, tagElement, "description", tag.getDescription(),
                true);
        appendTextNode(doc, tagElement, "name", tag.getName(), false);
        appendTextNode(doc, tagElement, "tag-class", tag.getTldTagClass(),
                false);
        appendTextNode(doc, tagElement, "body-content",
                tag.getTldBodyContent(), false);

        // save attributes
        for (TagAttribute attribute : tag.getAttributes()) {
            createElement(doc, tagElement, attribute);
        }

        appendTextNode(doc, tagElement, "dynamic-attributes", String.valueOf(tag.isAllowDynamicAttributes()), false);
    }

    private static void createElement(Document doc, Element tagElement, TagAttribute attribute) {
        Element attributeElement = doc.createElement("attribute");
        tagElement.appendChild(attributeElement);
        appendTextNode(doc, attributeElement, "description", attribute
                .getDescription(), true);
        appendTextNode(doc, attributeElement, "name", attribute.getName(),
                false);
        appendTextNode(doc, attributeElement, "required", String
                .valueOf(attribute.isRequired()), false);
        appendTextNode(doc, attributeElement, "rtexprvalue", String
                .valueOf(attribute.isRtexprvalue()), false);
    }

    private static void appendTextNode(Document doc, Element element, String name,
                                       String text, boolean cdata) {
        Text textNode = cdata ? doc.createCDATASection(text) : doc
                .createTextNode(text);
        Element newElement = doc.createElement(name);
        newElement.appendChild(textNode);
        element.appendChild(newElement);
    }

    /**
     * Get values of annotation
     *
     * @param element The annotation declaration
     * @param type    The type of the annotation
     * @return name->value map of annotation values
     */
    private Map<String, Object> getValues(javax.lang.model.element.Element element, TypeElement type) {
        Map<String, Object> values = new TreeMap<>();
        Collection<? extends AnnotationMirror> annotations = element
                .getAnnotationMirrors();
        // iterate over the mirrors.

        for (AnnotationMirror mirror : annotations) {
            // if the mirror in this iteration is for our note declaration...
            if (mirror.getAnnotationType().asElement().equals(type)) {
                for (Entry<? extends ExecutableElement, ? extends AnnotationValue> elementEntry : processingEnv.getElementUtils().getElementValuesWithDefaults(mirror).entrySet()) {
                    values.put(elementEntry.getKey().getSimpleName().toString(), elementEntry.getValue().getValue());
                }
            }
        }

        return values;
    }

}
