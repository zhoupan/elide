/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.yahoo.elide.annotation.ComputedAttribute;
import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.MappedInterface;
import com.yahoo.elide.annotation.OnUpdatePreSecurity;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.SecurityCheck;
import com.yahoo.elide.functions.LifeCycleHook;
import com.yahoo.elide.models.generics.Employee;
import com.yahoo.elide.models.generics.Manager;
import com.yahoo.elide.security.checks.UserCheck;
import com.yahoo.elide.security.checks.prefab.Collections.AppendOnly;
import com.yahoo.elide.security.checks.prefab.Collections.RemoveOnly;
import com.yahoo.elide.security.checks.prefab.Common.UpdateOnCreate;
import com.yahoo.elide.security.checks.prefab.Role;

import example.Child;
import example.FieldAnnotations;
import example.FunWithPermissions;
import example.Job;
import example.Left;
import example.Parent;
import example.Right;
import example.StringId;
import example.User;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Transient;

public class EntityDictionaryTest extends EntityDictionary {

    //Test class to validate inheritance logic
    @Include(rootLevel = true, type = "friend")
    private class Friend extends Child { }

    public EntityDictionaryTest() {
        super(Collections.EMPTY_MAP);
        init();
    }

    private void init() {
        this.bindEntity(FunWithPermissions.class);
        this.bindEntity(Parent.class);
        this.bindEntity(Child.class);
        this.bindEntity(User.class);
        this.bindEntity(Left.class);
        this.bindEntity(Right.class);
        this.bindEntity(StringId.class);
        this.bindEntity(Friend.class);
        this.bindEntity(FieldAnnotations.class);
        this.bindEntity(Manager.class);
        this.bindEntity(Employee.class);
        this.bindEntity(Job.class);
        this.bindEntity(NoId.class);

        checkNames.forcePut("user has all access", Role.ALL.class);
    }

    @Test
    public void testFindCheckByExpression() {
        assertEquals("user has all access", getCheckIdentifier(Role.ALL.class));
        assertEquals("Prefab.Role.None", getCheckIdentifier(Role.NONE.class));
        assertEquals("Prefab.Collections.AppendOnly", getCheckIdentifier(AppendOnly.class));
        assertEquals("Prefab.Collections.RemoveOnly", getCheckIdentifier(RemoveOnly.class));
        assertEquals("Prefab.Common.UpdateOnCreate", getCheckIdentifier(UpdateOnCreate.class));
    }

    @Test
    public void testCheckScan() {

        @SecurityCheck("User is Admin")
        class Foo extends UserCheck {

            @Override
            public boolean ok(com.yahoo.elide.security.User user) {
                return false;
            }
        }

        EntityDictionary testDictionary = new EntityDictionary(new HashMap<>());
        testDictionary.scanForSecurityChecks();

        assertEquals("User is Admin", testDictionary.getCheckIdentifier(Foo.class));
    }

    @Test
    public void testGetAttributeOrRelationAnnotation() {
        String[] fields = {"field1", "field2", "field3", "relation1", "relation2"};
        Annotation annotation;
        for (String field : fields) {
            annotation = this.getAttributeOrRelationAnnotation(FunWithPermissions.class, ReadPermission.class, field);
            assertTrue(annotation instanceof ReadPermission, "Every field should return a ReadPermission annotation");
        }
    }

    @Test
    public void testBindingInitializerPriorToBindingEntityClass() {
        @Entity
        @Include
        class Foo {
            @Id
            private long id;

            private int bar;
        }

        Initializer<Foo> initializer = mock(Initializer.class);
        this.bindInitializer(initializer, Foo.class);

        assertEquals(1, this.getAllFields(Foo.class).size());
    }

    @Test
    public void testBindingTriggerPriorToBindingEntityClass1() {
        @Entity
        @Include
        class Foo2 {
            @Id
            private long id;

            private int bar;
        }

        LifeCycleHook<Foo2> trigger = mock(LifeCycleHook.class);

        this.bindTrigger(Foo2.class, OnUpdatePreSecurity.class, "bar", trigger);
            assertEquals(1, this.getAllFields(Foo2.class).size());
        }

    @Test
    public void testBindingTriggerPriorToBindingEntityClass2() {
        @Entity
        @Include
        class Foo3 {
            @Id
            private long id;

            private int bar;
        }

        LifeCycleHook<Foo3> trigger = mock(LifeCycleHook.class);

        this.bindTrigger(Foo3.class, OnUpdatePreSecurity.class, trigger, true);
        assertEquals(1, this.getAllFields(Foo3.class).size());
    }

    @Test
    public void testBindingTriggerPriorToBindingEntityClass3() {
        @Entity
        @Include
        class Foo4 {
            @Id
            private long id;

            private int bar;
        }

        LifeCycleHook<Foo4> trigger = mock(LifeCycleHook.class);

        this.bindTrigger(Foo4.class, OnUpdatePreSecurity.class, trigger);
        assertEquals(1, this.getAllFields(Foo4.class).size());
    }

    @Test
    public void testJPAFieldLevelAccess() {
        @Entity
        @Include
        class FieldLevelTest {
            @Id
            private long id;

            private int bar;

            @Exclude
            private int excluded;

            @Transient
            @ComputedAttribute
            private int computedField;

            @Transient
            @ComputedAttribute
            public int getComputedProperty() {
                return 1;
            }

            public void setComputedProperty() {
                //NOOP
            }
        }
        this.bindEntity(FieldLevelTest.class);

        assertEquals(AccessType.FIELD, getAccessType(FieldLevelTest.class));

        List<String> fields = this.getAllFields(FieldLevelTest.class);
        assertEquals(3, fields.size());
        assertTrue(fields.contains("bar"));
        assertTrue(fields.contains("computedField"));
        assertTrue(fields.contains("computedProperty"));
    }

    @Test
    public void testJPAPropertyLevelAccess() {
        @Entity
        @Include
        class PropertyLevelTest {
            private long id;

            private int excluded;
            public int bar;


            @Exclude
            public int getExcluded() {
                return excluded;
            }

            public void setExcluded(int unused) {
                //noop
            }

            @Transient
            @ComputedAttribute
            private int computedField;

            @Transient
            @ComputedAttribute
            public int getComputedProperty() {
                return 1;
            }

            public void setComputedProperty() {
                //NOOP
            }
        }
        this.bindEntity(PropertyLevelTest.class);

        assertEquals(AccessType.PROPERTY, getAccessType(PropertyLevelTest.class));

        List<String> fields = this.getAllFields(PropertyLevelTest.class);
        assertEquals(2, fields.size());
        assertTrue(fields.contains("bar"));
        assertTrue(fields.contains("computedProperty"));
    }


    @Test
    public void testGetParameterizedType() {
        Class<?> type;

        FunWithPermissions fun = new FunWithPermissions();

        type = getParameterizedType(fun, "relation2");
        assertEquals(Child.class, type, "A set of Child objects should return Child.class");

        type = getParameterizedType(fun, "relation3");
        assertEquals(Child.class, type, "A Child object should return Child.class");

        assertEquals(
                FieldAnnotations.class,
                getParameterizedType(FieldAnnotations.class, "children"),
                "getParameterizedType return the type of a private field relationship");

        assertEquals(
                Child.class,
                getParameterizedType(Parent.class, "children"),
            "getParameterizedType returns the type of relationship fields");

        assertEquals(
                Employee.class,
                getParameterizedType(Manager.class, "minions"),
                "getParameterizedType returns the correct generic type of a to-many relationship");
    }

    @Test
    public void testGetInverseRelationshipOwningSide()  {
        assertEquals(
                "parents",
                getRelationInverse(Parent.class, "children"),
                "The inverse relationship of children should be parents");
    }

    @Test
    public void testGetInverseRelationshipOwnedSide()  {
        assertEquals(
                "children",
                getRelationInverse(Child.class, "parents"),
                "The inverse relationship of children should be parents");
    }

    @Test
    public void testComputedAttributeIsExposed() {
        List<String> attributes = getAttributes(User.class);
        assertTrue(attributes.contains("password"));
    }

    @Test
    public void testExcludedAttributeIsNotExposed() {
        List<String> attributes = getAttributes(User.class);
        assertFalse(attributes.contains("reversedPassword"));
    }

    @Test
    public void testDetectCascadeRelations() {
        assertFalse(cascadeDeletes(FunWithPermissions.class, "relation1"));
        assertFalse(cascadeDeletes(FunWithPermissions.class, "relation2"));
        assertTrue(cascadeDeletes(FunWithPermissions.class, "relation3"));
        assertFalse(cascadeDeletes(FunWithPermissions.class, "relation4"));
        assertFalse(cascadeDeletes(FunWithPermissions.class, "relation5"));
    }

    @Test
    public void testGetIdAnnotations() throws Exception {

        Collection<Class> expectedAnnotationClasses = Arrays.asList(new Class[]{Id.class, GeneratedValue.class});
        Collection<Class> actualAnnotationsClasses  = getIdAnnotations(new Parent()).stream()
            .map(Annotation::annotationType)
        .collect(Collectors.toList());

        assertEquals(actualAnnotationsClasses, expectedAnnotationClasses,
                "getIdAnnotations returns annotations on the ID field of the given class");
    }

    @Test
    public void testGetIdAnnotationsNoId() throws Exception {

        Collection<Annotation> expectedAnnotation = Collections.emptyList();
        Collection<Annotation> actualAnnotations  = getIdAnnotations(new NoId());

        assertEquals(actualAnnotations, expectedAnnotation,
                "getIdAnnotations returns an empty collection if there is no ID field for given class");
    }

    @Include
    class NoId {

    }

    @Test
    public void testGetIdAnnotationsSubClass() throws Exception {

        Collection<Class> expectedAnnotationClasses = Arrays.asList(new Class[]{Id.class, GeneratedValue.class});
        Collection<Class> actualAnnotationsClasses  = getIdAnnotations(new Friend()).stream()
                .map(Annotation::annotationType)
        .collect(Collectors.toList());

        assertEquals(actualAnnotationsClasses, expectedAnnotationClasses,
                "getIdAnnotations returns annotations on the ID field when defined in a super class");
    }

    @Test
    public void testIsSharableTrue() throws Exception {
        assertTrue(isShareable(Right.class));
    }

    @Test
    public void testIsSharableFalse() throws Exception {
        assertFalse(isShareable(Left.class));
    }

    @Test
    public void testGetIdType() throws Exception {
        assertEquals(getIdType(Parent.class), long.class,
                "getIdType returns the type of the ID field of the given class");

        assertEquals(getIdType(StringId.class), String.class,
                "getIdType returns the type of the ID field of the given class");

        assertNull(getIdType(NoId.class),
                "getIdType returns null if ID field is missing");

        assertEquals(getIdType(Friend.class), long.class,
                "getIdType returns the type of the ID field when defined in a super class");
    }

    @Test
    public void testGetType() throws Exception {
        assertEquals(
                Long.class,
                getType(FieldAnnotations.class, "id"),
                "getType returns the type of the ID field of the given class");

        assertEquals(
                long.class,
                getType(FieldAnnotations.class, "publicField"),
                "getType returns the type of attribute when Column annotation is on a field");

        assertEquals(
                Boolean.class,
                getType(FieldAnnotations.class, "privateField"),
                "getType returns the type of attribute when Column annotation is on a getter");

        assertNull(getType(FieldAnnotations.class, "missingField"),
                "getId returns null if attribute is missing"
        );

        assertEquals(
                FieldAnnotations.class,
                getType(FieldAnnotations.class, "parent"),
                "getType return the type of a private field relationship");

        assertEquals(
                Set.class,
                getType(FieldAnnotations.class, "children"),
                "getType return the type of a private field relationship");

        assertEquals(
                Set.class,
                getType(Parent.class, "children"),
                "getType returns the type of relationship fields");

        assertEquals(String.class, getType(Friend.class, "name"),
                "getType returns the type of attribute when defined in a super class");

        assertEquals(Manager.class, getType(Employee.class, "boss"),
            "getType returns the correct generic type of a to-one relationship");

        assertEquals(Set.class, getType(Manager.class, "minions"),
            "getType returns the correct generic type of a to-many relationship");

        // ID is "id"
        assertEquals(long.class, getType(Parent.class, "id"),
                "getType returns the type of surrogate key");

        // ID is not "id"
        assertEquals(Long.class, getType(Job.class, "jobId"),
                "getType returns the type of surrogate key");
        assertEquals(Long.class, getType(Job.class, "id"),
                "getType returns the type of surrogate key");
        assertEquals(String.class, getType(StringId.class, "surrogateKey"),
                "getType returns the type of surrogate key");
        assertEquals(String.class, getType(StringId.class, "id"),
                "getType returns the type of surrogate key");
    }

    @Test
    public void testGetTypUnknownEntityException() {
        assertThrows(IllegalArgumentException.class, () -> getType(Object.class, "id"));
    }

    @Test
    public void testNoExcludedFieldsReturned() {
        List<String> attrs = getAttributes(Child.class);
        List<String> rels = getRelationships(Child.class);
        assertTrue(!attrs.contains("excludedEntity") && !attrs.contains("excludedRelationship")
            && !attrs.contains("excludedEntityList"));
        assertTrue(!rels.contains("excludedEntity") && !rels.contains("excludedRelationship")
            && !rels.contains("excludedEntityList"));
    }

    @MappedInterface
    public interface SuitableInterface { }

    public interface BadInterface { }

    @Test
    public void testMappedInterface() {
        assertEquals(EntityBinding.EMPTY_BINDING, getEntityBinding(SuitableInterface.class));
    }

    @Test
    public void testBadInterface() {
        assertThrows(IllegalArgumentException.class, () -> getEntityBinding(BadInterface.class));
    }

    @Test
    public void testEntityInheritanceBinding() {
        @Entity
        @Include
        class SuperclassBinding {
            @Id
            private long id;
        }

        class SubclassBinding extends SuperclassBinding {
        }

        class SubsubclassBinding extends SubclassBinding {
        }

        this.bindEntity(SuperclassBinding.class);
        this.bindEntity(SubclassBinding.class);
        this.bindEntity(SubsubclassBinding.class);

        assertEquals(SubclassBinding.class, getEntityBinding(SubclassBinding.class).entityClass);
        assertEquals(SuperclassBinding.class, getEntityBinding(SuperclassBinding.class).entityClass);

        assertEquals(SuperclassBinding.class, lookupEntityClass(SuperclassBinding.class));
        assertEquals(SuperclassBinding.class, lookupEntityClass(SubclassBinding.class));
        assertEquals(SuperclassBinding.class, lookupEntityClass(SubsubclassBinding.class));
    }

    @Test
    public void testEntityInheritanceBindingOverride() {
        @Entity
        @Include
        class SuperclassBinding {
            @Id
            private long id;
        }

        class SubclassBinding extends SuperclassBinding {
        }

        @Entity
        @Include
        class SubsubclassBinding extends SubclassBinding {
            @Id
            private long id;
        }

        this.bindEntity(SuperclassBinding.class);
        this.bindEntity(SubclassBinding.class);
        this.bindEntity(SubsubclassBinding.class);

        assertEquals(SuperclassBinding.class, getEntityBinding(SuperclassBinding.class).entityClass);
        assertEquals(SubclassBinding.class, getEntityBinding(SubclassBinding.class).entityClass);
        assertEquals(SubsubclassBinding.class, getEntityBinding(SubsubclassBinding.class).entityClass);

        assertEquals(SuperclassBinding.class, lookupEntityClass(SuperclassBinding.class));
        assertEquals(SuperclassBinding.class, lookupEntityClass(SubclassBinding.class));
        assertEquals(SubsubclassBinding.class, lookupEntityClass(SubsubclassBinding.class));
    }

    @Test
    public void testMissingEntityBinding() {
        @Entity
        class SuperclassBinding {
            @Id
            private long id;
        }

        this.bindEntity(SuperclassBinding.class);

        assertEquals(null, getEntityBinding(SuperclassBinding.class).entityClass);
        assertEquals(SuperclassBinding.class, lookupEntityClass(SuperclassBinding.class));
    }

    @Test
    public void testNonEntityInheritanceBinding() {
        @Include
        class SuperclassBinding {
            @Id
            private long id;
        }

        class SubclassBinding extends SuperclassBinding {
        }

        class SubsubclassBinding extends SubclassBinding {
        }

        this.bindEntity(SuperclassBinding.class);
        this.bindEntity(SubclassBinding.class);
        this.bindEntity(SubsubclassBinding.class);

        assertEquals(SubclassBinding.class, getEntityBinding(SubclassBinding.class).entityClass);
        assertEquals(SuperclassBinding.class, getEntityBinding(SuperclassBinding.class).entityClass);

        assertEquals(SuperclassBinding.class, lookupIncludeClass(SuperclassBinding.class));
        assertEquals(SubclassBinding.class, lookupIncludeClass(SubclassBinding.class));
        assertEquals(SubsubclassBinding.class, lookupIncludeClass(SubsubclassBinding.class));
    }

    @Test
    public void testNonEntityInheritanceBindingOverride() {
        @Include
        class SuperclassBinding {
            @Id
            private long id;
        }

        class SubclassBinding extends SuperclassBinding {
        }

        @Include
        class SubsubclassBinding extends SubclassBinding {
            @Id
            private long id;
        }

        this.bindEntity(SuperclassBinding.class);
        this.bindEntity(SubclassBinding.class);
        this.bindEntity(SubsubclassBinding.class);

        assertEquals(SubclassBinding.class, getEntityBinding(SubclassBinding.class).entityClass);
        assertEquals(SuperclassBinding.class, getEntityBinding(SuperclassBinding.class).entityClass);
        assertEquals(SubsubclassBinding.class, getEntityBinding(SubsubclassBinding.class).entityClass);

        assertEquals(SuperclassBinding.class, lookupIncludeClass(SuperclassBinding.class));
        assertEquals(SubclassBinding.class, lookupIncludeClass(SubclassBinding.class));
        assertEquals(SubsubclassBinding.class, lookupIncludeClass(SubsubclassBinding.class));
    }

    @Test
    public void testNonEntityInheritanceBindingExclusion() {
        @Include
        class SuperclassBinding {
            @Id
            private long id;
        }

        class SubclassBinding extends SuperclassBinding {
        }

        @Exclude
        class SubsubclassBinding extends SubclassBinding {
        }

        this.bindEntity(SuperclassBinding.class);
        this.bindEntity(SubclassBinding.class);
        this.bindEntity(SubsubclassBinding.class);

        assertEquals(SubclassBinding.class, getEntityBinding(SubclassBinding.class).entityClass);
        assertEquals(SuperclassBinding.class, getEntityBinding(SuperclassBinding.class).entityClass);
        assertThrows(IllegalArgumentException.class, () -> { getEntityBinding(SubsubclassBinding.class); });

        assertEquals(SuperclassBinding.class, lookupIncludeClass(SuperclassBinding.class));
        assertEquals(SubclassBinding.class, lookupIncludeClass(SubclassBinding.class));
        assertEquals(null, lookupIncludeClass(SubsubclassBinding.class));
    }

    @Test
    public void testGetFirstAnnotation() {
        @Exclude
        class Foo { }

        @Include
        class Bar extends Foo {

        }

        class Baz extends Bar {

        }

        Annotation first = getFirstAnnotation(Baz.class, Arrays.asList(Exclude.class, Include.class));
        assertTrue(first instanceof Include);
    }

    @Test
    public void testGetFirstAnnotationConflict() {
        @Exclude
        @Include
        class Foo { }

        Annotation first = getFirstAnnotation(Foo.class, Arrays.asList(Exclude.class, Include.class));
        assertTrue(first instanceof Exclude);
    }

    @Test
    public void testBadLookupEntityClass() {
        assertThrows(IllegalArgumentException.class, () -> lookupEntityClass(null));
        assertThrows(IllegalArgumentException.class, () -> lookupEntityClass(Object.class));
    }
}
