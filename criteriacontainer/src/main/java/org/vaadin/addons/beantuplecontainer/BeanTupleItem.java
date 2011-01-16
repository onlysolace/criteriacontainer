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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.ObjectProperty;
import com.vaadin.data.util.PropertysetItem;

/**
 * BeanTupleItem allows accessing and modifying entities retrieved in a Tuple.
 * 
 * For each tuple element that is an entity, a BeanItem is created.
 * The properties inside each beanItem can be accessed through the parent property.
 * 
 * In the following code, assume that the query returns Person entities under the alias
 * "person", and a number under the alias "total".
 * 
 * <pre>
 * BeanTupleItem queryResult = ...
 * queryResult.getTuple().get("person"); // returns an entity of type Person
 * queryResult.getTuple().get("total"); // returns a number
 * 
 * queryResult.getItemProperty("person") // returns a Property with a BeanItem inside
 * queryResult.getItemProperty("person").getValue() // returns a BeanItem
 * queryResult.getItemProperty("person").getValue().getBean() // returns a Person
 * 
 * queryResult.getItemProperty("person.lastName") // returns a Property
 * queryResult.getItemProperty("person.lastName").getValue() // returns a String
 * </pre>
 * 
 * @author jflamy
 * 
 */
@SuppressWarnings("serial")
public final class BeanTupleItem extends PropertysetItem {

	@SuppressWarnings("unused")
	private final static Logger logger = LoggerFactory.getLogger(BeanTupleItem.class);

	/** The backing tuple */
	private Tuple tuple;

	/** 
	 * elements in the tuple that are entities, and can be edited as BeanItem
	 * the alias for the element in the tuple, and the propertyId of the 
	 * corresponding property are the same.
	 */
	private Set<Object> entities = new HashSet<Object>();

	/** Default constructor initializes default Item. */
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
		//logger.warn("item = {}",toString());
	}

	/**
	 * Set item properties based on values returned in tuple.
	 */
	protected void initializeFromTuple() {
		final List<TupleElement<?>> elements = tuple.getElements();

		for (TupleElement<?> curElem  : elements) {
			String curPropertyId = curElem.getAlias();
			if (curPropertyId == null) {
				throw new RuntimeException("Selection element "+curElem.toString()+" does not have an alias");
			}

			Object value = tuple.get(curPropertyId);
			if (value.getClass().isAnnotationPresent(Entity.class)) {
				// the class is an entity, create a bean item
				Item item = new BeanItem<Object>(value);
				entities.add(value);
				Property property = new ObjectProperty<Object>(item);
				this.addItemProperty(curPropertyId, property);
			} else {
				Property property = new ObjectProperty<Object>(value);
				this.addItemProperty(curPropertyId, property);
			}


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
	 * @param entityManager to be used for storing
	 */
	public void persist(EntityManager entityManager) {
		for (Object curEntity: entities){
			entityManager.persist(curEntity);
		}
	}

	/**
	 * merge all entities in the tuple.
	 * @param entityManager to be used for storing
	 */
	public void merge(EntityManager entityManager) {
		for (Object curEntity: entities){
			entityManager.merge(curEntity);
		}
	}

	/**
	 * remove all entities in the tuple.
	 * @param entityManager to be used for storing
	 */
	public void remove(EntityManager entityManager) {
		for (Object curEntity: entities){
			entityManager.remove(curEntity);
		}
	}

	/**
	 * Gets the Property corresponding to the given Property ID stored in the Item.
	 * If the Item does not contain the Property, null is returned.
	 * If the property Id is a string and contains a ".", then 
	 * 
	 * @see com.vaadin.data.util.PropertysetItem#getItemProperty(java.lang.Object)
	 */
	@Override
	public Property getItemProperty(Object id) {
		Property retVal;
		if (!(id instanceof String)) {
			retVal = super.getItemProperty(id);
		} else {
			// check for nested property
			String propertyId = (String) id;
			int dotIndex = propertyId.indexOf('.');
			if (dotIndex == -1) {
				retVal = super.getItemProperty(propertyId);
			} else {
				retVal = retrieveNestedProperty(propertyId, dotIndex);
			}
		}
		//logger.warn("sought id={} property={} value={}",new Object[]{id,retVal,(retVal != null ? retVal.getValue() : "n/a")});
		return retVal;
	}

	/**
	 * Retrieve property "x.y", where "x" is the name of a property that contains an item.
	 * "y" is assumed to be a property of the item contained in property "x".  If that
	 * is not the case, check for the presence of a property called "x.y".
	 * 
	 * @param propertyId
	 * @param dotIndex
	 * @return
	 */
	private Property retrieveNestedProperty(String propertyId, int dotIndex) {
		Property retVal;
		String prefix = propertyId.substring(0,dotIndex);		
		Property itemProperty = super.getItemProperty(prefix);
		if (itemProperty != null) {
			// prefix exists as a property on its own,
			// assume that rest of string is a property id inside

			@SuppressWarnings("unchecked")
			// get the property designated by the prefix
			BeanItem<Object> item = (BeanItem<Object>) itemProperty.getValue();
			propertyId = propertyId.substring(dotIndex+1);
			if (item == null) {
				retVal = null;
			} else {
				// not null, return the nested property
				retVal = item.getItemProperty(propertyId);			
			}
		} else {
			// attempt to retrieve the dotted name as a propertyId
			retVal = super.getItemProperty(propertyId);
		}
		return retVal;
	}

	/* (non-Javadoc)
	 * @see com.vaadin.data.util.PropertysetItem#getItemPropertyIds()
	 */
	@Override
	public Collection<?> getItemPropertyIds() {
		// TODO Auto-generated method stub
		return super.getItemPropertyIds();
	}

	/* (non-Javadoc)
	 * @see com.vaadin.data.util.PropertysetItem#toString()
	 */
	@Override
	public String toString() {
		return dumpItem(this,"",new StringBuffer());
	}

	/**
	 * @return
	 */
	private static String dumpItem(Item item, String indent, StringBuffer retValue) {

		retValue.append("\n");
		for (final Iterator<?> i = item.getItemPropertyIds().iterator(); i.hasNext();) {
			final Object propertyId = i.next();
			retValue.append(indent);
			retValue.append(propertyId);
			retValue.append("=");
			Property itemProperty = item.getItemProperty(propertyId);
			if (itemProperty != null) {
				Object value = itemProperty.getValue();
				if (value instanceof Item) {
					dumpItem((Item) value, "   ", retValue);
				} else {
					retValue.append(itemProperty.toString());
					retValue.append("\n");
				}
			} else {
				retValue.append("null***");
				retValue.append("\n");
			}
		}
		retValue.append("\n");
		return retValue.toString();
	}




}
