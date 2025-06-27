/*
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TagAnnotationProcessorTest {

    private TagAnnotationProcessor processor;
    
    @Mock
    private ProcessingEnvironment processingEnv;
    
    @Mock
    private RoundEnvironment roundEnv;
    
    @Mock
    private Elements elementUtils;
    
    @Mock
    private Types typeUtils;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        processor = new TagAnnotationProcessor();
        
        when(processingEnv.getElementUtils()).thenReturn(elementUtils);
        when(processingEnv.getTypeUtils()).thenReturn(typeUtils);
        when(processingEnv.getSourceVersion()).thenReturn(SourceVersion.RELEASE_17);
        
        processor.init(processingEnv);
    }

    @Test
    void testGetSupportedAnnotationTypes() {
        Set<String> supportedTypes = processor.getSupportedAnnotationTypes();
        
        assertEquals(3, supportedTypes.size());
        assertTrue(supportedTypes.contains(TagAnnotationProcessor.TAG));
        assertTrue(supportedTypes.contains(TagAnnotationProcessor.TAG_ATTRIBUTE));
        assertTrue(supportedTypes.contains(TagAnnotationProcessor.TAG_SKIP_HIERARCHY));
    }

    @Test
    void testGetSupportedSourceVersion() {
        SourceVersion version = processor.getSupportedSourceVersion();
        assertEquals(SourceVersion.RELEASE_17, version);
    }

    @Test
    void testAnnotationConstants() {
        assertEquals("org.apache.struts2.views.annotations.StrutsTag", TagAnnotationProcessor.TAG);
        assertEquals("org.apache.struts2.views.annotations.StrutsTagAttribute", TagAnnotationProcessor.TAG_ATTRIBUTE);
        assertEquals("org.apache.struts2.views.annotations.StrutsTagSkipInheritance", TagAnnotationProcessor.TAG_SKIP_HIERARCHY);
    }

    @Test
    void testPopulateTagAttributes() {
        TagAttribute attribute = new TagAttribute();
        java.util.Map<String, Object> values = new java.util.HashMap<>();
        values.put("required", true);
        values.put("rtexprvalue", false);
        values.put("defaultValue", "default");
        values.put("type", "String");
        values.put("description", "Test attribute");
        values.put("name", "testAttr");
        
        // Use reflection to access the private static method
        try {
            java.lang.reflect.Method method = TagAnnotationProcessor.class.getDeclaredMethod(
                "populateTagAttributes", TagAttribute.class, java.util.Map.class);
            method.setAccessible(true);
            method.invoke(null, attribute, values);
            
            assertTrue(attribute.isRequired());
            assertFalse(attribute.isRtexprvalue());
            assertEquals("default", attribute.getDefaultValue());
            assertEquals("String", attribute.getType());
            assertEquals("Test attribute", attribute.getDescription());
            assertEquals("testAttr", attribute.getName());
        } catch (Exception e) {
            fail("Failed to test populateTagAttributes method: " + e.getMessage());
        }
    }
}