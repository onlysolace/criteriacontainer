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
import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.SetJoin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.addons.criteriacontainer.CritRestriction;
import org.vaadin.addons.criteriacontainersample.data.Person;
import org.vaadin.addons.criteriacontainersample.data.Person_;
import org.vaadin.addons.criteriacontainersample.data.Task;
import org.vaadin.addons.criteriacontainersample.data.Task_;
import org.vaadin.addons.tuplecontainer.TupleContainer;
import org.vaadin.addons.tuplecontainer.TupleQueryDefinition;

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
public class TupleApplication extends Application implements ClickListener {
	private static final long serialVersionUID = 1L;

	public static final String PERSISTENCE_UNIT = "vaadin-lazyquerycontainer-example";
	private static final EntityManagerFactory ENTITY_MANAGER_FACTORY = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT);
	private EntityManager entityManager;

	private TextField nameFilterField;

	private Button refreshButton;
	private Button editButton;
	private Button saveButton;
	private Button cancelButton;
	private Button addItemButton;
	private Button removeItemButton;

	private Table table;
	private TupleContainer criteriaContainer;

	private ArrayList<Object> visibleColumnIds = new ArrayList<Object>();
	private ArrayList<String> visibleColumnLabels = new ArrayList<String>();

	@SuppressWarnings("unused")
	private Logger logger = LoggerFactory.getLogger(TupleApplication.class);

	private TestTupleQueryDefinition cd;

	@Override
	public void init() {

		Window mainWindow = new Window("Lazycontainer Application");
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
	private TupleContainer createTupleContainer() {
		cd = new TestTupleQueryDefinition(entityManager,true,100);
		TupleContainer tupleContainer = new TupleContainer(cd);

		tupleContainer.addContainerProperty(
				Task.class.getSimpleName()+"."+Task_.taskId.getName(), Long.class, new Long(0), true, true);
		tupleContainer.addContainerProperty(Task_.name.getName(), String.class, "", true, true);
		tupleContainer.addContainerProperty(Task_.alpha.getName(), String.class, "", true, true);
		tupleContainer.addContainerProperty(Person_.lastName.getName(), String.class, "", true, true);
		tupleContainer.addContainerProperty(Person_.firstName.getName(), String.class, "", false, true);

		return tupleContainer;
	}

	@SuppressWarnings("serial")
	class TestTupleQueryDefinition extends TupleQueryDefinition {
		
		/** Value assigned to the runtime JPQL parameter(SQL "like" syntax with %) */
		private String nameFilterValue;
		private Root<Person> person;
		private SetJoin<Person, Task> task;

		/**
		 * Constructor.
		 * @param entityManager the entityManager that gives us access to the database and cache
		 * @param applicationManagedTransactions false if running in a J2EE container that provides the entityManager used, true otherwise
		 * @param batchSize how many tuples to retrieve at once.
		 */
		public TestTupleQueryDefinition(EntityManager entityManager, boolean applicationManagedTransactions, int batchSize) {
			// We pass Task.class because the parameterized type of this class is <Task>
			super(entityManager, applicationManagedTransactions, batchSize);
		}
		

		/** 
		 * Define the query to be executed.
		 * 
		 * This class creates the equivalent of
		 * SELECT * FROM Task t WHERE t.name LIKE "..."
		 * 
		 * More precisely, the query by this method should not call cb.select() and cb.setOrdering().
		 * The default implementations of {@link #getCountQuery()} and {@link #getSelectQuery()} both
		 * call this method in order to guarantee that they are consistent with one another.
		 * 
		 * @see org.vaadin.addons.criteriacontainer.CritQueryDefinition#defineQuery(javax.persistence.criteria.CriteriaBuilder, javax.persistence.criteria.CriteriaQuery)
		 */
		@Override
		protected Root<?> defineQuery(CriteriaBuilder cb, CriteriaQuery<?> cq) {
			// select * FROM person 
			person = cq.from(Person.class);
			task = person.join(Person_.tasks); 
				
			if (nameFilterValue != null && !nameFilterValue.isEmpty()) {	
				// WHERE t.name LIKE ...
				cq.where(
						cb.like(
								task.get(Task_.name), // t.name
								nameFilterValue)  // pattern to be matched?
				);
			}
			cq.multiselect(
					task.get(Task_.taskId),
					task.get(Task_.name),
					task.get(Task_.alpha),
					person.get(Person_.lastName),
					person.get(Person_.firstName));

			return person;
		}

		
		/**
		 * Applies the sort state.
		 * A JPA ordering is created based on the saved sort orders.
		 * 
		 * @param t the root or joins from which columns are being selected
		 * @param cb the criteria builder for the query being built
		 * @return a list of Order objects to be added to the query.
		 */
		@Override
		protected final List<Order> getTupleOrdering(CriteriaBuilder cb) {
	        if (sortPropertyIds == null || sortPropertyIds.length == 0) {
	            sortPropertyIds = nativeSortPropertyIds;
	            sortPropertyAscendingStates = nativeSortPropertyAscendingStates;
	        }
	        
	        ordering = new ArrayList<Order>();
	    	if (sortPropertyIds == null || sortPropertyIds.length == 0) return ordering;
	    	
			for (int curItem = 0; curItem < sortPropertyIds.length; curItem++ ) {
		    	final String id = (String)sortPropertyIds[curItem];
				if (sortPropertyAscendingStates[curItem]) {
					ordering.add(cb.asc(person.get(id)));
				} else {
					ordering.add(cb.desc(person.get(id)));
				}
			}
			return ordering;
		}
		
		
		/* getters and setters */
		
		public String getNameFilterValue() {
			return nameFilterValue;
		}

		public void setNameFilterValue(String nameFilterValue) {
			this.nameFilterValue = nameFilterValue;
		}

	}
	
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

		addItemButton = new Button("Add Row");
		addItemButton.addListener(this);
		addItemButton.setEnabled(false);
		buttonPanel2.addComponent(addItemButton);

		removeItemButton = new Button("Remove Row");
		removeItemButton.addListener(this);
		removeItemButton.setEnabled(false);
		buttonPanel2.addComponent(removeItemButton);
	}




	/**
	 * 
	 */
	private void createEntities() {
		EntityTransaction transaction = entityManager.getTransaction();
		Person person1 = new Person();
		person1.setFirstName("Jean-François");
		person1.setLastName("Lamy");
		
		Person person2 = new Person();
		person2.setFirstName("Alain");
		person2.setLastName("Robitaille");
		
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

	@SuppressWarnings({"unchecked" })
	@Override
	public void buttonClick(ClickEvent event) {
		if (event.getButton() == refreshButton) {
			final String nameFilterValue = (String) nameFilterField.getValue();
			if (nameFilterValue != null && nameFilterValue.length() != 0) {
				// filtering style #1: query definition includes type safe filters.
				// the query has its own specific mechanism for setting the filters up.
				cd.setNameFilterValue(nameFilterValue);
				// do not refresh if calling "filter()" later.
				criteriaContainer.refresh();
				
			} else {
				cd.setNameFilterValue(null);
				criteriaContainer.filter((LinkedList<CritRestriction>)null);          
			}
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
	private void createTable(TupleContainer criteriaContainer2) {
		criteriaContainer2.refresh();
		table = new Table();

		table.setCaption("JpaQuery");
		table.setPageLength(40);

		table.setContainerDataSource(criteriaContainer2);
		defineTableColumns();
		
		table.setColumnWidth("name", 135);
		table.setColumnWidth("reporter", 135);
		table.setColumnWidth("assignee", 135);

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
	private void defineTableColumns() {
		visibleColumnIds.add(Task_.taskId.getName());
		visibleColumnIds.add(Task_.name.getName());
		visibleColumnIds.add(Person_.firstName.getName());
		visibleColumnIds.add(Person_.lastName.getName());
		
		visibleColumnLabels.add("Task ID");
		visibleColumnLabels.add("Name");
		visibleColumnLabels.add("Assignee First Name");
		visibleColumnLabels.add("Assignee First Name");
		
		table.setVisibleColumns(visibleColumnIds.toArray());
		table.setColumnHeaders(visibleColumnLabels.toArray(new String[0]));
	}

}
