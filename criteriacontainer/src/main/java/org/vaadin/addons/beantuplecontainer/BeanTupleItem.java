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
package org.vaadin.addons.beantuplecontainer;

import java.util.List;

import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import com.vaadin.data.Property;
import com.vaadin.data.util.ObjectProperty;
import com.vaadin.data.util.PropertysetItem;

/**
 * BeanTupleItem allows accessing a Tuple's values through the Item interface.
 * 
 * @author jflamy
 * 
 */
@SuppressWarnings("serial")
public final class BeanTupleItem extends PropertysetItem {

	/** The backing tuple */
	private Tuple tuple;

    /**
     * Default constructor initializes default Item.
     */
    public BeanTupleItem() {
    }

	/**
	 * Set the backing tuple without assigning values to properties
	 * 
	 * @param tuple   from which the item properties are extracted.
	 */
	public void setTuple(Tuple tuple) {
		this.tuple = tuple;
		initializeFromTuple();
	}

	/**
	 * Set item properties based on values returned in tuple.
	 */
	protected void initializeFromTuple() {
		final List<TupleElement<?>> elements = tuple.getElements();
		
		for (TupleElement<?> curElem  : elements) {
			String curPropertyId = curElem.getAlias();
			if (curPropertyId == null) {
				throw new RuntimeException("Selection element "+curElem.toString()+"does not have an alias");
			}
			final Object value = tuple.get(curPropertyId);
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
