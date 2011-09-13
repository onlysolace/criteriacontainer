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
package org.vaadin.addons.criteriacontainersample;

import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.vaadin.addons.criteriacontainer.CriteriaContainer;
import org.vaadin.addons.criteriacontainersample.data.Task;
import org.vaadin.addons.lazyquerycontainer.LazyQueryView;
import org.vaadin.addons.lazyquerycontainer.QueryItemStatus;

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
 * Shared core for applications demonstrating the CriteriaContainer features.
 * (Lazy Query Container extended to work with JPA 2.0 queries returning
 * a single entity type).
 * 
 * @author Tommi S.E. Laukkanen
 * @author Modified by Jean-François Lamy
 */
public abstract class AbstractEntityApplication extends Application implements ClickListener {

	private static final long serialVersionUID = 1L;
	protected static final String PERSISTENCE_UNIT = "vaadin-lazyquerycontainer-example";
	protected static final EntityManagerFactory ENTITY_MANAGER_FACTORY = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT);
	protected static final boolean TESTING = true;
	protected EntityManager entityManager;
	protected TextField nameFilterField;
	protected Button refreshButton;
	protected Button editButton;
	protected Button saveButton;
	protected Button cancelButton;
	protected Button addItemButton;
	protected Button removeItemButton;
	protected Table table;
	protected CriteriaContainer<Task> criteriaContainer;
	protected ArrayList<Object> visibleColumnIds = new ArrayList<Object>();
	protected ArrayList<String> visibleColumnLabels = new ArrayList<String>();
	protected Logger logger = LoggerFactory.getLogger(EntityContainerFilteringApplication.class);


	/**
	 * 
	 */
	public AbstractEntityApplication() {
		super();
		 // call only once during initialization time of your application
		 SLF4JBridgeHandler.install();
	}

	@Override
	public void init() {
	
		Window mainWindow = new Window("Lazycontainer Application");
	
		VerticalLayout mainLayout = new VerticalLayout();
		mainLayout.setSizeFull();
		mainLayout.setMargin(true);
		mainLayout.setSpacing(true);

		createFilterPanel(mainLayout);

		entityManager = ENTITY_MANAGER_FACTORY.createEntityManager();
		criteriaContainer =  createTaskContainer();
		
		int size = criteriaContainer.size();
		if (size == 0 && TESTING) {
			createEntities();
			criteriaContainer.refresh();
			size = criteriaContainer.size();
		}
	
		createTable(criteriaContainer);
		table.setPageLength(0);
		table.setSizeUndefined();
		table.setHeight("100%");
		mainLayout.addComponent(table);
		mainLayout.setExpandRatio(table, 100.0F);
		
	    mainWindow.setContent(mainLayout);
		setMainWindow(mainWindow);
	}
	

	/**
	 * @return the container used to fill the table
	 */
	abstract CriteriaContainer<Task> createTaskContainer();

	/**
	 * React to the user pressing the "Refresh" button by adding the requested filtering
	 * to the query.
	 */
	abstract void doFiltering() ;
	

	/**
	 * Populate the database with test data.
	 */
	private void createEntities() {
		EntityTransaction transaction = entityManager.getTransaction();
		transaction.begin();
		
		for (int i=0; i<150; i++) {
			Task task = new Task();
			String suffix = (char)('a' + (i / 10)) + Integer.toString(i % 10);
            String name = "task-"+suffix;
            task.setName(name);
			task.setAssignee("assignee-"+suffix);
			task.setReporter("reporter-"+suffix);
			task.setAlpha(suffix);
			task.setBeta(suffix);
			task.setGamma(suffix);
			task.setDelta(suffix);
			logger.debug("adding task {}",task);
			entityManager.persist(task);
		}
	
		transaction.commit();
	}

	/**
	 * Create the top panel where the user can add filtering restrictions.
	 * @param mainWindow
	 * @return
	 */
	private Panel createFilterPanel(VerticalLayout mainLayout) {
		Panel filterPanel = new Panel();
		filterPanel.addStyleName(Runo.PANEL_LIGHT);
		HorizontalLayout filterLayout = new HorizontalLayout();
		filterLayout.setMargin(false);
		filterLayout.setSpacing(true);
		filterPanel.setContent(filterLayout);
		mainLayout.addComponent(filterPanel);
	
		Panel buttonPanel = new Panel();
		buttonPanel.addStyleName(Runo.PANEL_LIGHT);
		HorizontalLayout buttonLayout = new HorizontalLayout();
		buttonLayout.setMargin(false);
		buttonLayout.setSpacing(true);
		buttonPanel.setContent(buttonLayout);
		mainLayout.addComponent(buttonPanel);
	
		Panel buttonPanel2 = new Panel();
		buttonPanel2.addStyleName(Runo.PANEL_LIGHT);
		HorizontalLayout buttonLayout2 = new HorizontalLayout();
		buttonLayout2.setMargin(false);
		buttonLayout2.setSpacing(true);
		buttonPanel2.setContent(buttonLayout2);
		mainLayout.addComponent(buttonPanel2);
	
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
	
		addItemButton = new Button("Add Row");
		addItemButton.addListener(this);
		addItemButton.setEnabled(false);
		buttonPanel2.addComponent(addItemButton);
	
		removeItemButton = new Button("Remove Row");
		removeItemButton.addListener(this);
		removeItemButton.setEnabled(false);
		buttonPanel2.addComponent(removeItemButton);
		return buttonPanel2;
	}

	/**
	 * @param taskContainer
	 */
	protected void addSpecialProperties(CriteriaContainer<Task> taskContainer) {
		taskContainer.addContainerProperty(LazyQueryView.PROPERTY_ID_ITEM_STATUS, QueryItemStatus.class,QueryItemStatus.None, true, false);
		taskContainer.addContainerProperty(LazyQueryView.DEBUG_PROPERTY_ID_QUERY_INDEX, Integer.class, 0, true, false);
		taskContainer.addContainerProperty(LazyQueryView.DEBUG_PROPERTY_ID_BATCH_INDEX, Integer.class, 0, true, false);
		taskContainer.addContainerProperty(LazyQueryView.DEBUG_PROPERTY_ID_BATCH_QUERY_TIME, Integer.class, 0, true,false);
	}

	protected void setEditMode(boolean editMode) {
		if (editMode) {
			table.setEditable(true);
			table.setSortDisabled(true);
			refreshButton.setEnabled(false);
			editButton.setEnabled(false);
			saveButton.setEnabled(true);
			cancelButton.setEnabled(true);
			addItemButton.setEnabled(true);
			removeItemButton.setEnabled(true);
			nameFilterField.setEnabled(false);
		} else {
			table.setEditable(false);
			table.setSortDisabled(false);
			refreshButton.setEnabled(true);
			editButton.setEnabled(true);
			saveButton.setEnabled(false);
			cancelButton.setEnabled(false);
			addItemButton.setEnabled(false);
			removeItemButton.setEnabled(false);
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
			criteriaContainer.addEntity(); // addItem() is sufficient; this is used for testing
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
				Collection<?> selectionIndexes = (Collection<?>) selection;
				for (Object selectedIndex : selectionIndexes) {
					criteriaContainer.removeItem((Integer) selectedIndex);
				}
			}
		}
	}


	
	/**
	 * 
	 */
	private void createTable(CriteriaContainer<Task> container) {
		//container.refresh();
		table = new Table();
	
		table.setCaption("JpaQuery");
		table.setPageLength(40);
	
		table.setContainerDataSource(container);
		defineTableColumns();
	
		table.setImmediate(true);
		table.setEditable(false);
		table.setMultiSelect(false);
		table.setMultiSelectMode(MultiSelectMode.DEFAULT);
		table.setSelectable(true);
		table.setWriteThrough(true);
	}

    /**
     * 
     */
    abstract void defineTableColumns();



}