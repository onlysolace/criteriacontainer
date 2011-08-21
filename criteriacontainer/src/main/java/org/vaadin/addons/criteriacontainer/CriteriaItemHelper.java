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

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Tuple;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.addons.beantuplecontainer.BeanTupleItemHelper;
import org.vaadin.addons.beantuplecontainer.BeanTupleQueryDefinition;
import org.vaadin.addons.beantuplecontainer.KeyManager;

import com.vaadin.data.Item;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.ObjectProperty;

/**
 * Load batches of entities and create corresponding items. 
 * 
 * @author Jean-François Lamy
 * 
 * @param <T> The type of entity underneath the items.
 */
public final class CriteriaItemHelper<T> extends BeanTupleItemHelper {
    
    @SuppressWarnings("unused")
    final private static Logger logger = LoggerFactory.getLogger(CriteriaItemHelper.class);

    private Class<?> entityClass;

    /**
     * @param criteriaQueryDefinition the definition for the query
     * @param beanTupleQueryView Holds cache id to key mappings.
     */
    public CriteriaItemHelper(BeanTupleQueryDefinition criteriaQueryDefinition, KeyManager beanTupleQueryView) {
        super(criteriaQueryDefinition, beanTupleQueryView);
        entityClass = queryDefinition.getEntityClass();
    }


    /**
     * Load batch of items.
     * This version aligns the loads according to batch size, in order to enable a significant
     * optimization in getItemIds().
     * 
     * @param startIndex Starting index of the item list.
     * @param count Count of the items to be retrieved.
     * @return List of items.
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Item> loadItems(final int startIndex, final int count) {
        List<Item> items = new ArrayList<Item>();
        if (count <= 0) {
            return items;
        }
        
        adjustRetrievalBoundaries(startIndex, count);
        
        Object keyPropertyId = keyToIdMapper.getKeyPropertyId();
        List<?> entities = getSelectQuery().getResultList();

        int curCount = 0;
        for (Object entity : entities) {
            T curEntity = (T) ((Tuple) entity).get(0);
            Item item = toItem(curEntity);
            if (queryDefinition.isDetachedEntities()) {
                entityManager.detach(curEntity);
            }
            items.add(item);
            addToMapping(item, keyPropertyId, startIndex+curCount);
            //logger.debug("adding {} at index {}",item, startIndex+curCount);
            curCount++;
        }
        return items;
    }


	/**
     * Constructs new item based on QueryDefinition.
     * @return new item.
     */
    @Override
	public Item constructItem() {
        try {
            @SuppressWarnings("unchecked")
            T entity = (T) entityClass.newInstance();
            BeanInfo info = Introspector.getBeanInfo(entityClass);
            for (PropertyDescriptor pd : info.getPropertyDescriptors()) {
                for (Object propertyId : queryDefinition.getPropertyIds()) {
                    if (pd.getName().equals(propertyId)) {
                        Method writeMethod = pd.getWriteMethod();
                        Object propertyDefaultValue = queryDefinition.getPropertyDefaultValue(propertyId);
                        writeMethod.invoke(entity, propertyDefaultValue);
                    }
                }
            }
            return toItem(entity);
        } catch (Exception e) {
            throw new RuntimeException("Error in bean construction or property population with default values.", e);
        }
    }


    /**
     * Saves the modifications done by container to the query result.
     * Query will be discarded after changes have been saved
     * and new query loaded so that changed items are sorted
     * appropriately.
     * @param addedItems Items to be inserted.
     * @param modifiedItems Items to be updated.
     * @param removedItems Items to be deleted.
     */
    @Override
	public void saveItems(final List<Item> addedItems, final List<Item> modifiedItems, final List<Item> removedItems) {
        if (applicationTransactionManagement) {
            entityManager.getTransaction().begin();
        }
        try {
            for (Item item : addedItems) {
                if (!removedItems.contains(item)) {
                    entityManager.persist(fromItem(item));
                }
            }
            for (Item item : modifiedItems) {
                if (!removedItems.contains(item)) {
                    Object entity = fromItem(item);
                    if (queryDefinition.isDetachedEntities()) {
                        entity = entityManager.merge(entity);
                    }
                    entityManager.persist(entity);
                }
            }
            for (Item item : removedItems) {
                if (!addedItems.contains(item)) {
                    Object entity = fromItem(item);
                    if (queryDefinition.isDetachedEntities()) {
                        entity = entityManager.merge(entity);
                    }
                    entityManager.remove(entity);
                }
            }
            if (applicationTransactionManagement) {
                entityManager.getTransaction().commit();
            }
        } catch (Exception e) {
            if (applicationTransactionManagement) {
                if (entityManager.getTransaction().isActive()) {
                    entityManager.getTransaction().rollback();
                }
            }
            throw new RuntimeException(e);            
        }
        
        // invalidate the query size
        setQuerySize(-1);
    }

    
    /**
     * Converts bean to Item.
     * @param entity bean to be converted.
     * @return item converted from bean.
     */

	@SuppressWarnings({ "unchecked", "rawtypes" })
    protected Item toItem(final T entity) {
        BeanItem<T> beanItem = new NestedBeanItem<T>(entity);

        for (Object propertyId : queryDefinition.getPropertyIds()) {
            if (beanItem.getItemProperty(propertyId) == null) {
                beanItem.addItemProperty(
                        propertyId,
                        new ObjectProperty(queryDefinition.getPropertyDefaultValue(propertyId), 
                                queryDefinition.getPropertyType(propertyId),
                                queryDefinition.isPropertyReadOnly(propertyId)));
            }
        }

        return beanItem;
    }
	

    /**
     * Converts item back to bean.
     * @param item Item to be converted to bean.
     * @return Resulting bean.
     */
    @SuppressWarnings("unchecked")
    private Object fromItem(final Item item) {
        return (Object) ((BeanItem<T>) item).getBean();
    }

}