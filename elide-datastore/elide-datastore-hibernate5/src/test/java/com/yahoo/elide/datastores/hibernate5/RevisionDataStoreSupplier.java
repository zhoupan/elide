package com.yahoo.elide.datastores.hibernate5;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.utils.ClassScanner;
import example.Person;
import org.hibernate.MappingException;
import org.hibernate.ejb.HibernateEntityManager;

import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class RevisionDataStoreSupplier implements Supplier<DataStore> {

    @Override
    public DataStore get() {
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
        return new HibernateRevisionsDataStore(em);
    }
}
