/**
 * Copyright 2010 Jean-François Lamy
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

import org.apache.commons.beanutils.PropertyUtils;

import com.vaadin.data.Property;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.ObjectProperty;

/**
 * @author Jean-François Lamy
 * @param <BT> Bean type being wrapped in Items
 *
 */
@SuppressWarnings("serial")
public class NestedBeanItem<BT> extends BeanItem<BT> {

    /**
     * @param bean bean being wrapped
     */
    public NestedBeanItem(BT bean) {
        super(bean);
    }

    /**
     * Return a possibly nested or indexed property value.
     * <p>Any syntax accepted by Jakarta PropertyUtils is acceptable and will be
     * added as a property in the map. For example </p>
     * <ul>
     * <li>person.address.postalCode</li>
     * <li>person.children[0]</li>
     * </ul>
     * @see com.vaadin.data.util.PropertysetItem#getItemProperty(java.lang.Object)
     */
    @Override
    public Property getItemProperty(Object id) {
        
        if (id instanceof String){
            String idString = (String)id;
            if (idString.contains(".")) {
                // treat a String property id with a . as a nested name
                try {
                    Object value = PropertyUtils.getProperty(getBean(), idString);
                    Property property = new ObjectProperty<Object>(value);
                    addItemProperty(idString, property);
                    return property;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                return super.getItemProperty(id);
            }
        } else{
            return super.getItemProperty(id);
        }

    }
    
    

}
