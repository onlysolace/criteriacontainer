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

import java.util.ArrayList;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.addons.criteriacontainersample.data.Person;
import org.vaadin.addons.criteriacontainersample.data.Person_;
import org.vaadin.addons.criteriacontainersample.data.Task;
import org.vaadin.addons.criteriacontainersample.data.Task_;
import org.vaadin.addons.tuplecontainer.TupleContainer;

import com.vaadin.Application;
import com.vaadin.data.Container;
import com.vaadin.ui.AbstractSelect.MultiSelectMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.Runo;

/**
 * Shared core for applications demonstrating the TupleContainer features.
 * (Lazy Query Container extended to work with JPA 2.0 Tuple queries,
 * in which columns from several tables are shown together).
 * 
 * TupleContainer cannot be used in editing mode.
 * 
 * @author Tommi S.E. Laukkanen
 * @author Modified by Jean-François Lamy
 */
public abstract class AbstractTupleApplication extends Application implements ClickListener {
	private static final long serialVersionUID = 1L;

	public static final String PERSISTENCE_UNIT = "vaadin-lazyquerycontainer-example";
	protected static final EntityManagerFactory ENTITY_MANAGER_FACTORY = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT);
	protected EntityManager entityManager;

	protected TextField nameFilterField;

	protected Button refreshButton;

	protected Table table;
	protected TupleContainer criteriaContainer;

	protected ArrayList<Object> visibleColumnIds = new ArrayList<Object>();
	protected ArrayList<String> visibleColumnLabels = new ArrayList<String>();

	protected Logger logger = LoggerFactory.getLogger(AbstractTupleApplication.class);


	@Override
	public void init() {

		Window mainWindow = new Window("Tuple Application");
		VerticalLayout mainLayout = new VerticalLayout();
		mainLayout.setMargin(true);
		mainLayout.setSpacing(true);
		mainWindow.setContent(mainLayout);

		Panel filterPanel = new Panel();
		createTopPanel(mainWindow, filterPanel);

		entityManager = ENTITY_MANAGER_FACTORY.createEntityManager();
		criteriaContainer =  (TupleContainer) createTupleContainer();
		int size = criteriaContainer.size();
		if (size == 0) {
			createEntities();
		}

		createTable(criteriaContainer);
		mainWindow.addComponent(table);
		setMainWindow(mainWindow);
	}


	/**
	 * @return
	 */
	protected abstract Container createTupleContainer();
	
	/**
	 * 
	 */
	protected abstract void doFiltering();


	
	/**
	 * @param mainWindow
	 * @param filterPanel
	 */
	private void createTopPanel(Window mainWindow, Panel filterPanel) {
		filterPanel.addStyleName(Runo.PANEL_LIGHT);
		HorizontalLayout filterLayout = new HorizontalLayout();
		filterLayout.setMargin(false);
		filterLayout.setSpacing(true);
		filterPanel.setContent(filterLayout);
		mainWindow.addComponent(filterPanel);

		Panel buttonPanel = new Panel();
		buttonPanel.addStyleName(Runo.PANEL_LIGHT);
		HorizontalLayout buttonLayout = new HorizontalLayout();
		buttonLayout.setMargin(false);
		buttonLayout.setSpacing(true);
		buttonPanel.setContent(buttonLayout);
		mainWindow.addComponent(buttonPanel);

		Panel buttonPanel2 = new Panel();
		buttonPanel2.addStyleName(Runo.PANEL_LIGHT);
		HorizontalLayout buttonLayout2 = new HorizontalLayout();
		buttonLayout2.setMargin(false);
		buttonLayout2.setSpacing(true);
		buttonPanel2.setContent(buttonLayout2);
		mainWindow.addComponent(buttonPanel2);

		nameFilterField = new TextField("Name");
		filterPanel.addComponent(nameFilterField);

		refreshButton = new Button("Refresh");
		refreshButton.addListener(this);
		buttonPanel.addComponent(refreshButton);
	}


	/**
	 * 
	 */
	private void createEntities() {
		EntityTransaction transaction = entityManager.getTransaction();
		transaction.begin();
		
		Person person1 = new Person();
		person1.setFirstName("Jean-François");
		person1.setLastName("Lamy");
		entityManager.persist(person1);

		Person person2 = new Person();
		person2.setFirstName("Alain");
		person2.setLastName("Robitaille");
		entityManager.persist(person2);
		
		for (int i=0; i<150; i++) {
			Task task = new Task();
			task.setName("task-"+Integer.toString(i));
			task.setAssignee("assignee-"+Integer.toString(i));
			task.setReporter("reporter-"+Integer.toString(i));
			task.setAlpha(Integer.toString(i));
			task.setBeta(Integer.toString(i));
			task.setGamma(Integer.toString(i));
			task.setDelta(Integer.toString(i));
			if (i == 0 || i == 3) {
				task.setAssignedTo(person1);
			} else if (i == 1) {
				task.setAssignedTo(person2);
			}
			entityManager.persist(task);
		}
		transaction.commit();
	}

	@Override
	public void buttonClick(ClickEvent event) {
		if (event.getButton() == refreshButton) {
			doFiltering();
		}
	}

	private void createTable(TupleContainer criteriaContainer2) {
		criteriaContainer2.refresh();
		table = new Table();

		table.setCaption("Tuple Query");
		table.setPageLength(40);

		table.setContainerDataSource(criteriaContainer2);
		defineTableColumns();
		
		table.setColumnWidth("taskName", 135);
		table.setColumnWidth("lastName", 135);
		table.setColumnWidth("firstName", 135);

		table.setImmediate(true);
		table.setEditable(false);
		table.setMultiSelect(true);
		table.setMultiSelectMode(MultiSelectMode.DEFAULT);
		table.setSelectable(true);
		table.setWriteThrough(true);
	}
	
	protected void defineTableColumns() {
		visibleColumnIds.add(Task_.taskId.getName());
		visibleColumnIds.add("taskName");
		visibleColumnIds.add(Person_.firstName.getName());
		visibleColumnIds.add(Person_.lastName.getName());
		
		visibleColumnLabels.add("Task ID");
		visibleColumnLabels.add("Name");
		visibleColumnLabels.add("Assignee First Name");
		visibleColumnLabels.add("Assignee Last Name");
		
		table.setVisibleColumns(visibleColumnIds.toArray());
		table.setColumnHeaders(visibleColumnLabels.toArray(new String[0]));
	}

}
