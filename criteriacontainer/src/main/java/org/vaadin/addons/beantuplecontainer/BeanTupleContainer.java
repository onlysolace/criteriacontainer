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

import javax.persistence.metamodel.SingularAttribute;

import org.vaadin.addons.tuplecontainer.TupleContainer;

/**
 * CriteriaContainer enables using JPA 2.0 Criteria type-safe queries with lazy batch loading, filter, sort
 * and buffered writes.
 * 
 * @author Jean-François Lamy
 */
@SuppressWarnings("serial")
public final class BeanTupleContainer extends TupleContainer {
    
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
	 * @param sortable the property can be used for sorting
	 */
	public void addContainerNestedProperty(
			String entityAlias, 
			SingularAttribute<?, ?> attribute,
			Object defaultValue, 
			boolean readOnly, 
			boolean sortable) {
		addContainerProperty(entityAlias+"."+attribute.getName(), attribute.getJavaType(), defaultValue, readOnly, sortable);
	}



}
