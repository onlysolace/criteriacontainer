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

import java.util.Collection;
import java.util.List;

import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.ObjectProperty;
import com.vaadin.data.util.PropertysetItem;

/**
 * TupleItem allows accessing a Tuple's values through the Item interface.
 * 
 * @author jflamy
 * 
 */
@SuppressWarnings("serial")
public final class TupleItem extends PropertysetItem {
    
    /** The default item. */
    private Item item = new PropertysetItem();
    
	/** The backing tuple */
	private Object tuple;

    /**
     * Default constructor initializes default Item.
     */
    public TupleItem() {
    }

	/**
	 * Set the backing tuple.
	 * Each value in the tuple is assigned to the corresponding property
	 * in the item (respecting the order)
	 * 
	 * @param tuple   from which the item properties are extracted.
	 */
	public void setTuple(Tuple tuple) {
		this.tuple = tuple;
		final List<TupleElement<?>> elements = tuple.getElements();
		Collection<?> propertyIds = item.getItemPropertyIds();
		
		if (elements.size() != propertyIds.size()) {
			throw new RuntimeException("Tuple must have same number of elements as Item");
		}
		
		int i = 0;
		for (Object curPropertyId : propertyIds) {
			final Object value = tuple.get(i);
			Property property = new ObjectProperty<Object>(value);
			this.addItemProperty(curPropertyId, property);
		}
	}

	/**
	 * @return get the backing tuple
	 */
	public Object getTuple() {
		return tuple;
	}
	
	/**
	 * persist all entities in the tuple.
	 */
	public void persist() {
	}

}
