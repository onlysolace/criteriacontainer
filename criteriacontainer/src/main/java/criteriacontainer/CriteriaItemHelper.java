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
package criteriacontainer;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.List;

import org.vaadin.addons.beantuplecontainer.BeanTupleItemHelper;
import org.vaadin.addons.beantuplecontainer.BeanTupleQueryDefinition;
import org.vaadin.addons.lazyquerycontainer.CompositeItem;

import com.vaadin.data.Item;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.ObjectProperty;

/**
 * Entity query implementation which dynamically injects missing query
 * definition properties to CompositeItems.
 * 
 * @author Jean-François Lamy
 * 
 * @param <T> The type of entity underneath the items.
 */
public final class CriteriaItemHelper<T> extends BeanTupleItemHelper {

    private Class<?> entityClass;

    /**
     * @param criteriaQueryDefinition the definition for the query
     */
    public CriteriaItemHelper(BeanTupleQueryDefinition criteriaQueryDefinition) {
        super(criteriaQueryDefinition);
        entityClass = queryDefinition.getEntityClass();
    }


    /**
     * Constructs new item based on QueryDefinition.
     * @return new item.
     */
    @Override
	public Item constructItem() {
        try {
            Object entity = entityClass.newInstance();
            BeanInfo info = Introspector.getBeanInfo(entityClass);
            for (PropertyDescriptor pd : info.getPropertyDescriptors()) {
                for (Object propertyId : queryDefinition.getPropertyIds()) {
                    if (pd.getName().equals(propertyId)) {
                        pd.getWriteMethod().invoke(entity, queryDefinition.getPropertyDefaultValue(propertyId));
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
        for (Item item : addedItems) {
            entityManager.persist(fromItem(item));
        }
        for (Item item : modifiedItems) {
            entityManager.persist(fromItem(item));
        }
        for (Item item : removedItems) {
            entityManager.remove(fromItem(item));
        }
        if (applicationTransactionManagement) {
            entityManager.getTransaction().commit();
        }
    }

    /**
     * Converts bean to Item. Implemented by encapsulating the Bean
     * first to BeanItem and then to CompositeItem.
     * @param entity bean to be converted.
     * @return item converted from bean.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
	private Item toItem(final Object entity) {
        BeanItem<?> beanItem = new BeanItem<Object>(entity);

        CompositeItem compositeItem = new CompositeItem();
        compositeItem.addItem("bean", beanItem);

        for (Object propertyId : queryDefinition.getPropertyIds()) {
            if (compositeItem.getItemProperty(propertyId) == null) {
                compositeItem.addItemProperty(
                        propertyId,
                        new ObjectProperty(queryDefinition.getPropertyDefaultValue(propertyId), queryDefinition
                                .getPropertyType(propertyId), queryDefinition.isPropertyReadOnly(propertyId)));
            }
        }

        return compositeItem;
    }

    /**
     * Converts item back to bean.
     * @param item Item to be converted to bean.
     * @return Resulting bean.
     */
    @SuppressWarnings({ "rawtypes" })
    private Object fromItem(final Item item) {
        return (Object) ((BeanItem) (((CompositeItem) item).getItem("bean"))).getBean();
    }
}