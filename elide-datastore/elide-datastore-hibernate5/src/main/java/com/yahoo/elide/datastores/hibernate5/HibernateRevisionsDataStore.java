package com.yahoo.elide.datastores.hibernate5;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import org.hibernate.ScrollMode;
import org.hibernate.Session;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.jpa.HibernateEntityManager;
import org.hibernate.metadata.ClassMetadata;

import javax.persistence.Entity;
import javax.persistence.EntityManager;

public class HibernateRevisionsDataStore implements DataStore {

    protected final HibernateEntityManager entityManager;

    public HibernateRevisionsDataStore(HibernateEntityManager entityManager) {
        this.entityManager = entityManager;

    }

    @Override
    @SuppressWarnings("resource")
    public DataStoreTransaction beginTransaction() {
        return new HibernateRevisionsTransaction(AuditReaderFactory.get(entityManager), entityManager.getSession());
    }

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        for (ClassMetadata meta : entityManager.getSession().getSessionFactory().getAllClassMetadata().values()) {
            if (meta.getMappedClass().getAnnotation(Entity.class) != null) {
                dictionary.bindEntity(meta.getMappedClass());
            }
        }
    }
}
