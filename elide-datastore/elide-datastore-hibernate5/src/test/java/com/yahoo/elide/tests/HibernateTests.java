package com.yahoo.elide.tests;


import com.yahoo.elide.utils.ClassScanner;
import example.Child;
import example.Filtered;
import example.Person;
import example.TestCheckMappings;
import org.hibernate.MappingException;
import org.hibernate.Session;
import org.hibernate.ejb.HibernateEntityManager;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.testng.annotations.Test;

import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HibernateTests {

    @Test
    public void testHibernate() {
        TestCheckMappings.MAPPINGS.put("filterCheck", Filtered.FilterCheck.class);
        TestCheckMappings.MAPPINGS.put("filterCheck3", Filtered.FilterCheck3.class);

        Map<String, Object> options = new HashMap<>();
        ArrayList<Class> bindClasses = new ArrayList<>();

        try {
            bindClasses.addAll(ClassScanner.getAnnotatedClasses(Person.class.getPackage(), Entity.class));
        } catch (MappingException e) {
            throw new IllegalStateException(e);
        }

        options.put("javax.persistence.jdbc.driver", "com.mysql.jdbc.Driver");
        options.put("javax.persistence.jdbc.url", "jdbc:mysql://localhost:3306/test?serverTimezone=UTC");
        options.put("javax.persistence.jdbc.user", "root");
        options.put("javax.persistence.jdbc.password", "root");
        options.put("hibernate.ejb.loaded.classes", bindClasses);
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("elide-tests", options);
        HibernateEntityManager em = (HibernateEntityManager) emf.createEntityManager();
        Session session = em.getSession();
        
        session.beginTransaction();
        Person person = new Person();
        person.setId(1);
        person.setName("old name");
        Child child = new Child();
        child.setParent(person);
        child.setId(1L);
        child.setName("Old Name");
        Set<Child> children = new HashSet<>();
        children.add(child);
        person.setChildren(children);
        session.save(person);
        session.save(child);
        session.getTransaction().commit();

        session.beginTransaction();
        child = session.load(Child.class, 1L);
        child.setName("New Name");
        person = new Person();
        person.setId(2);
        person.setName("Other parent");
        child.setParent(person);
        session.save(child);
        session.save(person);
        session.getTransaction().commit();

        session.beginTransaction();
        child = session.load(Child.class, 1L);
        child.setName("revise again");
        session.save(child);
        session.getTransaction().commit();

        AuditReader reader = AuditReaderFactory.get(em);
        //Child oldVersion = reader.find(Child.class, 1L, 1);
        //Person parent = oldVersion.getParent();
        //Assert.assertEquals(parent.getId(), 2);

        List list1 = reader.createQuery().forEntitiesAtRevision(Person.class, 1).add(AuditEntity.property("name").ilike("%eorge%")).getResultList();
        List list2 = reader.createQuery().forEntitiesAtRevision(Child.class, 2).add(AuditEntity.relatedId("parent").eq(1L)).getResultList();
    }
}
