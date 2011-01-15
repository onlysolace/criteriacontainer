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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.TupleElement;

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
				value = new BeanItem<Object>(value);
				entities.add(value);
			}
			
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

}
