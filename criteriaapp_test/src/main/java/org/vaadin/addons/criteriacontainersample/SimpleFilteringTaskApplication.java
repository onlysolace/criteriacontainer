/**
 * Copyright 2011 Tommi S.E. Laukkanen
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

import java.util.LinkedList;

import org.vaadin.addons.criteriacontainer.CritQueryDefinition;
import org.vaadin.addons.criteriacontainer.CritRestriction;
import org.vaadin.addons.criteriacontainer.CriteriaContainer;
import org.vaadin.addons.criteriacontainersample.data.Task;
import org.vaadin.addons.criteriacontainersample.data.Task_;

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
public class SimpleFilteringTaskApplication extends AbstractTaskApplication  {
	
	protected CritQueryDefinition<Task> cd;
	
	
	/**
	 * @return the container used to feed the table
	 */
	@Override
	protected CriteriaContainer<Task> createTaskContainer() {
		cd = new CritQueryDefinition<Task>(entityManager,true,Task.class,100);
		
		CriteriaContainer<Task> taskContainer = new CriteriaContainer<Task>(cd);
		addContainerProperties(taskContainer);
		return taskContainer;
	}
	
	/**
	 * Get a query definition with filtering activated
	 * 
	 * In this version, we use the generic filtering mechanism provided
	 * by CriteriaContainer.
	 * 
	 * @see {@link CustomFilteringTaskApplication} and {@link CustomFilteringQueryDefinition} for
	 * an alternate approach where arbitrary complex filtering can be done through methods.
	 */
	@Override
	protected void doFiltering() {
		// get filtering string from the user interface
		final String nameFilterValue = (String) nameFilterField.getValue();
		
		// if value define add the filtering conditions, else remove them.
		if (nameFilterValue != null && nameFilterValue.length() != 0) {
			// filtering style #2
			// simple conditions are added to a list and passed to the filter mechanism.
			final LinkedList<CritRestriction> restrictions = new LinkedList<CritRestriction>();
			restrictions.add(new CritRestriction(Task_.name.getName(), CritRestriction.Operation.LIKE, nameFilterValue));
			criteriaContainer.filter(restrictions);
		} else {
			criteriaContainer.filter((LinkedList<CritRestriction>)null);          
		}
	}

}
