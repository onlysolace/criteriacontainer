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

import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.addons.beantuplecontainer.BeanTupleQueryDefinition;

/**
 * Definition of a JPA 2.0 Criteria query.
 * The query can be arbitrary, but the first item in the tuple returned must
 * be an entity of type T, which will be used to create the items returned
 * by the container.
 * 
 * @author jflamy
 * 
 * @param <ItemEntity> the Entity type for the BeanItems
 */
public class CriteriaQueryDefinition<ItemEntity> extends BeanTupleQueryDefinition {
    
    @SuppressWarnings("unused")
    private static Logger logger = LoggerFactory.getLogger(CriteriaQueryDefinition.class);
    private Class<ItemEntity> itemEntityClass;

    /**
     * @param entityManager the EntityManager to reach the database that contains T objects
     * @param applicationManagedTransactions true unless using J2EE container-managed transactions
     * @param batchSize how many items are retrieved at once
     * @param itemEntityClass class for the entities that underlie the items (T)
     */
    public CriteriaQueryDefinition(EntityManager entityManager,
            boolean applicationManagedTransactions, int batchSize, Class<ItemEntity> itemEntityClass) {
        super(entityManager, applicationManagedTransactions, batchSize);
        this.itemEntityClass = itemEntityClass;
    }
    
    /**
     * @param entityManager the EntityManager to reach the database that contains T objects
     * @param detachedEntities if true, entities will be detached from the persistence context and merged as needed.
     * @param applicationManagedTransactions true unless using J2EE container-managed transactions
     * @param batchSize how many items are retrieved at once
     * @param itemEntityClass class for the entities that underlie the items (T)
     */
    public CriteriaQueryDefinition(
            EntityManager entityManager,
            boolean detachedEntities,
            boolean applicationManagedTransactions,
            int batchSize,
            Class<ItemEntity> itemEntityClass) {
        super(entityManager, applicationManagedTransactions, batchSize);
        this.itemEntityClass = itemEntityClass;
        setDetachedEntities(detachedEntities);
    }
    
    
    /**
     * Get the class of the entity being managed.
     * 
     * @return the Class for the Entity being returned.
     */
    @Override
    public Class<ItemEntity> getEntityClass() {
        return (Class<ItemEntity>) itemEntityClass;
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
        Root<ItemEntity> t = criteriaQuery.from(getEntityClass());
        // select all root objects by default.
        criteriaQuery.multiselect(t);
        return t;
    }
    
	/* (non-Javadoc)
	 * @see org.vaadin.addons.beantuplecontainer.BeanTupleQueryDefinition#addPropertyForEntity(java.util.Map, javax.persistence.criteria.Path, boolean)
	 */
	@Override
	protected void addPropertyForEntity(Map<Object, Expression<?>> expressionMap,
			Path<?> entityPath, boolean defineProperties) {
		// nothing.  We only add columns for the attributes, not for the whole table.
	}

	/**
     * This method must be consistent with {@link #getPropertyId(Class, SingularAttribute)} and
     * {@link #getPropertyId(String, SingularAttribute)}
     * 
     * @param entityPath path (Root or Join) that designates an entity
	 * @param column the expression for the column
	 * @return the name to use
	 */
	@Override
	protected String columnName(Path<?> entityPath, SingularAttribute<?, ?> column) {
		// return a name qualified by the alias of the table/entity
		return column.getName();
	}


    /**
     * Return the attribute name as created in the BeanItem.
     * This override is needed because we return a BeanItem for the entity and not a BeanTupleItem.
     * Therefore the type name or alias must not be present.
     * 
     * @see org.vaadin.addons.beantuplecontainer.BeanTupleQueryDefinition#getPropertyId(java.lang.Class, javax.persistence.metamodel.SingularAttribute)
     */
    @Override
    public String getPropertyId(Class<?> metamodelType, SingularAttribute<?, ?> attr) {
        init();
        String propertyId = attr.getName();
//        if (getPropertyIds().contains(propertyId)) {
//            return propertyId;
//        } else {
//            return null;
//        }
       return propertyId;
    }

    
    /**
     * Compute the property id from information available in the static metamodel.
     * This override is needed because we return a BeanItem for the entity and not a BeanTupleItem.
     * In this case, we simply return the property id, and not the qualified name.
     * 
     * @param alias alias used in the query definition.
     * @param attr a singular attribute defined in the model
     * @return a propertyId if it has been found in the defined properties, null if not.
     */
    @Override
    public String getPropertyId(String alias, SingularAttribute<?, ?> attr) {
        init();
        String propertyId = attr.getName();
//        if (propertyIds.contains(propertyId)) {
//            return propertyId;
//        } else {
//            return null;
//        }
        return propertyId;
    }
    
    
    

}
