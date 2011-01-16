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
import java.util.Collection;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.addons.beantuplecontainer.BeanTupleContainer;
import org.vaadin.addons.criteriacontainersample.data.Person;
import org.vaadin.addons.criteriacontainersample.data.Person_;
import org.vaadin.addons.criteriacontainersample.data.Task;
import org.vaadin.addons.criteriacontainersample.data.Task_;

import com.vaadin.Application;
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
 * Example application demonstrating the Lazy Query Container features.
 * @author Tommi S.E. Laukkanen
 * @author Modified by Jean-François Lamy
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractBeanTupleApplication extends Application implements ClickListener {
	private static final long serialVersionUID = 1L;

	public static final String PERSISTENCE_UNIT = "vaadin-lazyquerycontainer-example";
	protected static final EntityManagerFactory ENTITY_MANAGER_FACTORY = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT);
	protected EntityManager entityManager;

	protected TextField nameFilterField;

	protected Button refreshButton;
	protected Button editButton;
	protected Button saveButton;
	protected Button cancelButton;
	protected Button addItemButton;
	protected Button removeItemButton;

	protected Table table;
	protected BeanTupleContainer criteriaContainer;

	protected ArrayList<Object> visibleColumnIds = new ArrayList<Object>();
	protected ArrayList<String> visibleColumnLabels = new ArrayList<String>();

	protected Logger logger = LoggerFactory.getLogger(AbstractBeanTupleApplication.class);


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
		criteriaContainer =  createTupleContainer();
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
	protected abstract BeanTupleContainer createTupleContainer();
	
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

		editButton = new Button("Edit");
		editButton.addListener(this);
		buttonPanel.addComponent(editButton);

		saveButton = new Button("Save");
		saveButton.addListener(this);
		saveButton.setEnabled(false);
		buttonPanel2.addComponent(saveButton);

		cancelButton = new Button("Cancel");
		cancelButton.addListener(this);
		cancelButton.setEnabled(false);
		buttonPanel2.addComponent(cancelButton);

// are tricky to implement in a table when dealing with a join

//		addItemButton = new Button("Add Row");
//		addItemButton.addListener(this);
//		addItemButton.setEnabled(false);
//		buttonPanel2.addComponent(addItemButton);
//
//		removeItemButton = new Button("Remove Row");
//		removeItemButton.addListener(this);
//		removeItemButton.setEnabled(false);
//		buttonPanel2.addComponent(removeItemButton);
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


	private void setEditMode(boolean editMode) {
		if (editMode) {
			table.setEditable(true);
			table.setSortDisabled(true);
			refreshButton.setEnabled(false);
			editButton.setEnabled(false);
			saveButton.setEnabled(true);
			cancelButton.setEnabled(true);
//			addItemButton.setEnabled(true);
//			removeItemButton.setEnabled(true);
			nameFilterField.setEnabled(false);
		} else {
			table.setEditable(false);
			table.setSortDisabled(false);
			refreshButton.setEnabled(true);
			editButton.setEnabled(true);
			saveButton.setEnabled(false);
			cancelButton.setEnabled(false);
//			addItemButton.setEnabled(false);
//			removeItemButton.setEnabled(false);
			nameFilterField.setEnabled(true);
		}
	}

	@Override
	public void buttonClick(ClickEvent event) {
		if (event.getButton() == refreshButton) {
			doFiltering();
		}
		if (event.getButton() == editButton) {
			setEditMode(true);
		}
		if (event.getButton() == saveButton) {
			criteriaContainer.commit();
			criteriaContainer.refresh();
			setEditMode(false);
		}
		if (event.getButton() == cancelButton) {
			criteriaContainer.discard();
			criteriaContainer.refresh();
			setEditMode(false);
		}
		if (event.getButton() == addItemButton) {
			criteriaContainer.addItem();
		}
		if (event.getButton() == removeItemButton) {
			Object selection = table.getValue();
			if (selection == null) {
				return;
			}
			if (selection instanceof Integer) {
				Integer selectedIndex = (Integer) selection;
				if (selectedIndex != null) {
					criteriaContainer.removeItem(selectedIndex);
				}
			}
			if (selection instanceof Collection) {
				Collection selectionIndexes = (Collection) selection;
				for (Object selectedIndex : selectionIndexes) {
					criteriaContainer.removeItem((Integer) selectedIndex);
				}
			}
		}
	}
	




	/**
	 * 
	 */
	private void createTable(BeanTupleContainer criteriaContainer2) {
		criteriaContainer2.refresh();
		table = new Table();

		table.setCaption("JpaQuery");
		table.setPageLength(40);

		table.setContainerDataSource(criteriaContainer2);
		defineTableColumns();
		
		table.setColumnWidth("taskName", 135);
		table.setColumnWidth("lastName", 135);
		table.setColumnWidth("firstName", 135);

//		table.setColumnWidth(LazyQueryView.PROPERTY_ID_ITEM_STATUS, 16);
//		table.addGeneratedColumn(LazyQueryView.PROPERTY_ID_ITEM_STATUS, new QueryItemStatusColumnGenerator(this));

		table.setImmediate(true);
		table.setEditable(false);
		table.setMultiSelect(true);
		table.setMultiSelectMode(MultiSelectMode.DEFAULT);
		table.setSelectable(true);
		table.setWriteThrough(true);
	}
	
	/**
	 * 
	 */
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
