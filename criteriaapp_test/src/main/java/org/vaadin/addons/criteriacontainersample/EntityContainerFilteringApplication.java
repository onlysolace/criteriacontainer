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
package org.vaadin.addons.criteriacontainersample;

import org.vaadin.addons.criteriacontainer.CriteriaContainer;
import org.vaadin.addons.criteriacontainer.CriteriaQueryDefinition;
import org.vaadin.addons.criteriacontainersample.data.Task;
import org.vaadin.addons.criteriacontainersample.data.Task_;
import org.vaadin.addons.lazyquerycontainer.LazyQueryView;
import org.vaadin.addons.lazyquerycontainer.QueryItemStatusColumnGenerator;

import com.vaadin.data.Container.Filterable;
import com.vaadin.data.util.filter.SimpleStringFilter;

/**
 * Example application demonstrating how to use the generic
 * CritQueryDefinition to fetch entities and to perform filtering
 * on a container.
 * 
 * In this version, we do not need to extend CritQueryDefinition,
 * we only use the built-in features for filtering.
 * 
 * @author Jean-François Lamy
 */
@SuppressWarnings("serial")
public class EntityContainerFilteringApplication extends AbstractEntityApplication  {
	
	protected CriteriaQueryDefinition<Task> cd;
	
	
	/**
	 * @return the container used to feed the table
	 */
	@Override
	protected CriteriaContainer<Task> createTaskContainer() {
		cd = new CriteriaQueryDefinition<Task>(entityManager,true,100,Task.class);
		
		CriteriaContainer<Task> taskContainer = new CriteriaContainer<Task>(cd);
		addContainerProperties(taskContainer);
		return taskContainer;
	}
	
	
	/**
	 * Manipulate the query definition to add/remove filtering.
	 * 
	 * In this version, we use the generic filtering mechanism provided
	 * by {@link Filterable}
	 * 
	 * See {@link EntityCustomFilteringApplication} and {@link EntityCustomFilteringApplication.CustomFilteringQueryDefinition} for
	 * an alternate approach where arbitrary complex filtering can be done through methods.
	 */
	@Override
	protected void doFiltering() {
        // get filtering string from the user interface
        final String nameFilterValue = (String) nameFilterField.getValue();
        // this makes code portable between CriteriaContainer and BeanItemContainer
    	String propertyId = cd.getPropertyId(Task_.class, Task_.name);
        
        // if value define add the filtering conditions, else remove them.
        if (nameFilterValue != null && nameFilterValue.length() != 0) {
            // conditions are added to the container filter mechanism.
        	criteriaContainer.removeAllContainerFilters();
			SimpleStringFilter filter = new SimpleStringFilter(propertyId, nameFilterValue, true, true);
			criteriaContainer.addContainerFilter(filter);
        	criteriaContainer.refresh();
        } else {
            criteriaContainer.removeAllContainerFilters();
            criteriaContainer.refresh();
        }
	}

	/**
     * @param taskContainer
     */
    @Override
    protected void addContainerProperties(CriteriaContainer<Task> taskContainer) {
        // we want assignee.hashcode to be a property (computed as a nested property)
        taskContainer.addContainerProperty(
                Task_.assignee.getName()+".class",
                Class.class,
                null,
                true,
                false);
    }
    
	/**
     * define the columns visible and the order in which they appear.
     */
    @Override
    protected void defineTableColumns() {
        //visibleColumnIds.add(LazyQueryView.PROPERTY_ID_ITEM_STATUS);
        
    	
        visibleColumnIds.add(cd.getPropertyId(Task_.class, Task_.taskId));
        visibleColumnLabels.add("Task ID");
        
        visibleColumnIds.add(cd.getPropertyId(Task_.class, Task_.name));
        visibleColumnLabels.add("Name");
        
        visibleColumnIds.add(cd.getPropertyId(Task_.class, Task_.reporter));
        visibleColumnLabels.add("Reporter");
        
        String assigneeName = cd.getPropertyId(Task_.class, Task_.assignee);
		visibleColumnIds.add(assigneeName);
        visibleColumnLabels.add("Assignee");
        visibleColumnIds.add(assigneeName+".class");
        visibleColumnLabels.add("assignee.class");
        
        //computedColumns();

        table.setColumnWidth("name", 135);
        table.setColumnWidth("reporter", 135);
        table.setColumnWidth("assignee", 135);  

        table.setVisibleColumns(visibleColumnIds.toArray());
        table.setColumnHeaders(visibleColumnLabels.toArray(new String[0]));
    }


    /**
     * additional tests
     */
    @SuppressWarnings("unused")
    private void computedColumns() {
        visibleColumnIds.add(Task_.alpha.getName());
        visibleColumnIds.add(Task_.beta.getName());
        visibleColumnIds.add(Task_.gamma.getName());
        visibleColumnIds.add(Task_.delta.getName());
        
        visibleColumnIds.add(LazyQueryView.DEBUG_PROPERTY_ID_QUERY_INDEX);
        visibleColumnIds.add(LazyQueryView.DEBUG_PROPERTY_ID_BATCH_INDEX);
        visibleColumnIds.add(LazyQueryView.DEBUG_PROPERTY_ID_BATCH_QUERY_TIME);
        
        visibleColumnLabels.add("Alpha");
        visibleColumnLabels.add("Beta");
        visibleColumnLabels.add("Gamma");
        visibleColumnLabels.add("Delta");
        
        visibleColumnLabels.add("Query");
        visibleColumnLabels.add("Batch");
        visibleColumnLabels.add("Time [ms]");
        
        table.setColumnWidth(LazyQueryView.PROPERTY_ID_ITEM_STATUS, 16);
        table.addGeneratedColumn(LazyQueryView.PROPERTY_ID_ITEM_STATUS, new QueryItemStatusColumnGenerator());
    }



}
