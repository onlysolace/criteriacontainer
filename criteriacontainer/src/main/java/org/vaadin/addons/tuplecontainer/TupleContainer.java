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
package org.vaadin.addons.tuplecontainer;

import java.util.LinkedList;

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
public class TupleContainer extends LazyQueryContainer {
    
    /**
     * Standard constructor for type-safe queries.
     * @param cd the definition of the query
     */
    public TupleContainer(TupleQueryDefinition cd){
    	super(new TupleQueryView(cd,new TupleQueryFactory()));
    }
    
    /**
     * Standard constructor for type-safe queries.
     * @param cd the definition of the query (independent of its execution context)
     * @param cf the factory that will generate a context in which the query will run.
     */
    public TupleContainer(
    		TupleQueryDefinition cd,
    		TupleQueryFactory cf
            ){
    	super(new TupleQueryView(cd,cf));
    }
    
	/**
	 * @param tupleQueryView an already built view
	 */
	public TupleContainer(TupleQueryView tupleQueryView) {
		super(tupleQueryView);
	}


	/**
	 * Filters the container content by setting "where" criteria in the JPA Criteria.
	 * @param restrictions  restrictions to set to JPA query or null to clear.
	 */
	public void filter(LinkedList<CritRestriction> restrictions) {
        ((CritQueryDefinition<?>) getQueryView().getQueryDefinition()).setRestrictions(restrictions);
        refresh();
	}




}
