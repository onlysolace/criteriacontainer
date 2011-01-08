package org.vaadin.addons.criteriacontainersample;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.addons.criteriacontainer.CritRestriction;
import org.vaadin.addons.criteriacontainer.CriteriaContainer;
import org.vaadin.addons.criteriacontainersample.data.Task;
import org.vaadin.addons.criteriacontainersample.data.Task_;
import org.vaadin.addons.lazyquerycontainer.LazyQueryView;
import org.vaadin.addons.lazyquerycontainer.QueryItemStatus;
import org.vaadin.addons.lazyquerycontainer.QueryItemStatusColumnGenerator;

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
 */
@SuppressWarnings("rawtypes")
public class MyVaadinApplication extends Application implements ClickListener {
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
	private CriteriaContainer<Task> criteriaContainer;

	private ArrayList<Object> visibleColumnIds = new ArrayList<Object>();
	private ArrayList<String> visibleColumnLabels = new ArrayList<String>();

	private Logger logger = LoggerFactory.getLogger(MyVaadinApplication.class);

	private SimpleTaskQueryDefinition cd;

	@Override
	public void init() {

		Window mainWindow = new Window("Lazycontainer Application");

		VerticalLayout mainLayout = new VerticalLayout();
		mainLayout.setMargin(true);
		mainLayout.setSpacing(true);
		mainWindow.setContent(mainLayout);

		Panel filterPanel = new Panel();
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

		visibleColumnIds.add(LazyQueryView.PROPERTY_ID_ITEM_STATUS);
		visibleColumnIds.add(Task_.taskId.getName());
		visibleColumnIds.add(Task_.name.getName());
		visibleColumnIds.add(Task_.reporter.getName());
		visibleColumnIds.add(Task_.assignee.getName());
		visibleColumnIds.add(Task_.alpha.getName());
		visibleColumnIds.add(Task_.beta.getName());
		visibleColumnIds.add(Task_.gamma.getName());
		visibleColumnIds.add(Task_.delta.getName());
		visibleColumnIds.add(LazyQueryView.DEBUG_PROPERTY_ID_QUERY_INDEX);
		visibleColumnIds.add(LazyQueryView.DEBUG_PROPERTY_ID_BATCH_INDEX);
		visibleColumnIds.add(LazyQueryView.DEBUG_PROPERTY_ID_BATCH_QUERY_TIME);

		visibleColumnLabels.add("");
		visibleColumnLabels.add("Task ID");
		visibleColumnLabels.add("Name");
		visibleColumnLabels.add("Reporter");
		visibleColumnLabels.add("Assignee");
		visibleColumnLabels.add("Alpha");
		visibleColumnLabels.add("Beta");
		visibleColumnLabels.add("Gamma");
		visibleColumnLabels.add("Delta");
		visibleColumnLabels.add("Query");
		visibleColumnLabels.add("Batch");
		visibleColumnLabels.add("Time [ms]");

		entityManager = ENTITY_MANAGER_FACTORY.createEntityManager();

		cd = new SimpleTaskQueryDefinition(entityManager,true,100);
		criteriaContainer = new CriteriaContainer<Task>(cd);

		criteriaContainer.addContainerProperty(LazyQueryView.PROPERTY_ID_ITEM_STATUS, QueryItemStatus.class,
				QueryItemStatus.None, true, false);
		criteriaContainer.addContainerProperty(Task_.taskId.getName(), Long.class, new Long(0), true, true);
		criteriaContainer.addContainerProperty(Task_.name.getName(), String.class, "", true, true);
		criteriaContainer.addContainerProperty(Task_.reporter.getName(), String.class, "", true, true);
		criteriaContainer.addContainerProperty(Task_.assignee.getName(), String.class, "", true, true);
		criteriaContainer.addContainerProperty(Task_.alpha.getName(), String.class, "", false, true);
		criteriaContainer.addContainerProperty(Task_.beta.getName(), String.class, "", false, true);
		criteriaContainer.addContainerProperty(Task_.gamma.getName(), String.class, "", false, true);
		criteriaContainer.addContainerProperty(Task_.delta.getName(), String.class, "", false, true);
		criteriaContainer.addContainerProperty(LazyQueryView.DEBUG_PROPERTY_ID_QUERY_INDEX, Integer.class, 0, true, false);
		criteriaContainer.addContainerProperty(LazyQueryView.DEBUG_PROPERTY_ID_BATCH_INDEX, Integer.class, 0, true, false);
		criteriaContainer.addContainerProperty(LazyQueryView.DEBUG_PROPERTY_ID_BATCH_QUERY_TIME, Integer.class, 0, true,
				false);

		int size = criteriaContainer.size();

		if (size == 0) {
			for (int i=0; i<10000; i++) {
				Task task = criteriaContainer.addEntity();
				task.setName("task-"+Integer.toString(i));
				task.setAssignee("assignee-"+Integer.toString(i));
				task.setReporter("reporter-"+Integer.toString(i));
				task.setAlpha(Integer.toString(i));
				task.setBeta(Integer.toString(i));
				task.setGamma(Integer.toString(i));
				task.setDelta(Integer.toString(i));
			}
		}

		criteriaContainer.commit();

		table = new Table();

		table.setCaption("JpaQuery");
		table.setPageLength(40);

		table.setContainerDataSource(criteriaContainer);

		table.setColumnWidth("name", 135);
		table.setColumnWidth("reporter", 135);
		table.setColumnWidth("assignee", 135);

		table.setVisibleColumns(visibleColumnIds.toArray());
		table.setColumnHeaders(visibleColumnLabels.toArray(new String[0]));

		table.setColumnWidth(LazyQueryView.PROPERTY_ID_ITEM_STATUS, 16);
		table.addGeneratedColumn(LazyQueryView.PROPERTY_ID_ITEM_STATUS, new QueryItemStatusColumnGenerator(this));

		table.setImmediate(true);
		table.setEditable(false);
		table.setMultiSelect(true);
		table.setMultiSelectMode(MultiSelectMode.DEFAULT);
		table.setSelectable(true);
		table.setWriteThrough(true);

		mainWindow.addComponent(table);

		setMainWindow(mainWindow);
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

	@SuppressWarnings("unused")
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
				
				logger.warn(Task_.name.getName());
				// filtering style #2
				// simple conditions are added to a list and passed to the filter mechanism.
				if (false) {
					final LinkedList<CritRestriction> restrictions = new LinkedList<CritRestriction>();
					restrictions.add(new CritRestriction(Task_.beta.getName(), CritRestriction.Operation.GE, 9996));
					criteriaContainer.filter(restrictions);
				}
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

}
