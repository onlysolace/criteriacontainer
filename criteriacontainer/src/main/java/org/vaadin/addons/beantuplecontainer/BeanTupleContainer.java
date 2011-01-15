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
package org.vaadin.addons.beantuplecontainer;

import java.util.LinkedList;

import javax.persistence.metamodel.SingularAttribute;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.addons.criteriacontainer.CritQueryDefinition;
import org.vaadin.addons.criteriacontainer.CritRestriction;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;

/**
 * CriteriaContainer enables using JPA 2.0 Criteria type-safe queries with lazy batch loading, filter, sort
 * and buffered writes.
 * 
 * @author Jean-François Lamy
 */
@SuppressWarnings("serial")
public final class BeanTupleContainer extends LazyQueryContainer {
	final static Logger logger = LoggerFactory.getLogger(BeanTupleContainer.class);
    
    /**
     * Standard constructor for type-safe queries.
     * @param cd the definition of the query
     */
    public BeanTupleContainer(BeanTupleQueryDefinition cd){
    	super(new BeanTupleQueryView(cd,new BeanTupleQueryFactory()));
    }


	/**
	 * @param entityAlias the alias under which the parent entity is retrieved
	 * @param attribute the attribute being retrieved 
	 * @param defaultValue value to set for the item
	 * @param readOnly the property cannot be set
	 */
	public void addContainerSortableProperty(
			String entityAlias, 
			SingularAttribute<?, ?> attribute,
			Object defaultValue, 
			boolean readOnly) {
		String propertyId = entityAlias+"."+attribute.getName();
		Class<?> javaType = instantatiableType(attribute.getJavaType());
		addContainerProperty(propertyId, javaType, defaultValue, readOnly, true);
	}


	/**
	 * @param javaType
	 * @return the corresponding class for which newInstance can be called.
	 */
	private Class<?> instantatiableType(Class<?> javaType) {
		if (javaType == long.class) {
			javaType = Long.class;
		} else if (javaType == int.class) {
			javaType = Integer.class;
		} else if (javaType == boolean.class) {
			javaType = Boolean.class;
		}
		return javaType;
	}


	/**
	/**
	 * Filters the container content by setting "where" criteria in the JPA Criteria.
	 * @param restrictions  restrictions to set to JPA query or null to clear.
	 */
	public void filter(LinkedList<CritRestriction> restrictions) {
        ((CritQueryDefinition<?>) getQueryView().getQueryDefinition()).setRestrictions(restrictions);
        refresh();
	}



}
