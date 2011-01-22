/**
 * Copyright 2011 Jean-François Lamy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.vaadin.addons.criteriacontainer;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.vaadin.addons.beantuplecontainer.BeanTupleQueryDefinition;

/**
 * Definition of a JPA 2.0 Criteria Query.
 * 
 * Extends LazyQueryDefinition instead of EntityQueryDefinition to avoid passing the Entity class.
 * 
 * @author jflamy
 * 
 * @param <T> the Entity type returned by the query being defined
 */
public class CriteriaQueryDefinition<T> extends BeanTupleQueryDefinition {

    /**
     * @param entityManager the EntityManager to reach the database that contains T objects
     * @param applicationManagedTransactions true unless using J2EE container-managed transactions
     * @param batchSize how many items are retrieved at once
     */
    public CriteriaQueryDefinition(EntityManager entityManager,
            boolean applicationManagedTransactions, int batchSize) {
        super(entityManager, applicationManagedTransactions, batchSize);
    }
    
    
    /**
     * Get the class of the entity being managed.
     * 
     * @return the Class for the Entity being returned.
     */
    @SuppressWarnings("unchecked")
    @Override
    public Class<T> getEntityClass() {
        return (Class<T>) entityClass;
    }


    /** 
     * Define the default query that fetches all items of type <T>
     * {@link #getSelectQuery()} and {@link #getCountQuery()} will take into account
     * the filters and sorting orders defined by the query definition by enriching/amending
     * the criteriaQuery
     * 
     * @see org.vaadin.addons.beantuplecontainer.BeanTupleQueryDefinition#defineQuery(javax.persistence.criteria.CriteriaBuilder, javax.persistence.criteria.CriteriaQuery)
     */
    @Override
    protected Root<?> defineQuery (
            CriteriaBuilder criteriaBuilder,
            CriteriaQuery<?> criteriaQuery) {
        Root<T> t = criteriaQuery.from(getEntityClass());
        criteriaQuery.multiselect(t);
        return t;
    }

}
