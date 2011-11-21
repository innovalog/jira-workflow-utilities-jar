package com.googlecode.jsu.util;

import static com.atlassian.jira.issue.IssueFieldConstants.AGGREGATE_PROGRESS;
import static com.atlassian.jira.issue.IssueFieldConstants.AGGREGATE_TIME_ESTIMATE;
import static com.atlassian.jira.issue.IssueFieldConstants.AGGREGATE_TIME_ORIGINAL_ESTIMATE;
import static com.atlassian.jira.issue.IssueFieldConstants.ATTACHMENT;
import static com.atlassian.jira.issue.IssueFieldConstants.COMMENT;
import static com.atlassian.jira.issue.IssueFieldConstants.COMPONENTS;
import static com.atlassian.jira.issue.IssueFieldConstants.CREATED;
import static com.atlassian.jira.issue.IssueFieldConstants.ISSUE_KEY;
import static com.atlassian.jira.issue.IssueFieldConstants.ISSUE_LINKS;
import static com.atlassian.jira.issue.IssueFieldConstants.ISSUE_TYPE;
import static com.atlassian.jira.issue.IssueFieldConstants.PRIORITY;
import static com.atlassian.jira.issue.IssueFieldConstants.PROGRESS;
import static com.atlassian.jira.issue.IssueFieldConstants.PROJECT;
import static com.atlassian.jira.issue.IssueFieldConstants.STATUS;
import static com.atlassian.jira.issue.IssueFieldConstants.SUBTASKS;
import static com.atlassian.jira.issue.IssueFieldConstants.THUMBNAIL;
import static com.atlassian.jira.issue.IssueFieldConstants.TIMETRACKING;
import static com.atlassian.jira.issue.IssueFieldConstants.TIME_ESTIMATE;
import static com.atlassian.jira.issue.IssueFieldConstants.TIME_ORIGINAL_ESTIMATE;
import static com.atlassian.jira.issue.IssueFieldConstants.TIME_SPENT;
import static com.atlassian.jira.issue.IssueFieldConstants.UPDATED;
import static com.atlassian.jira.issue.IssueFieldConstants.VOTES;
import static com.atlassian.jira.issue.IssueFieldConstants.WORKRATIO;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.ofbiz.core.entity.model.ModelEntity;
import org.ofbiz.core.entity.model.ModelField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.core.ofbiz.CoreFactory;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.customfields.impl.DateCFType;
import com.atlassian.jira.issue.customfields.impl.DateTimeCFType;
import com.atlassian.jira.issue.customfields.impl.ImportIdLinkCFType;
import com.atlassian.jira.issue.customfields.impl.ReadOnlyCFType;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.fields.FieldException;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.NavigableField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.layout.field.FieldLayout;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutStorageException;
import com.atlassian.jira.issue.fields.screen.FieldScreen;
import com.atlassian.jira.issue.fields.screen.FieldScreenLayoutItem;
import com.atlassian.jira.issue.fields.screen.FieldScreenTab;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.I18nHelper.BeanFactory;
import com.atlassian.jira.web.FieldVisibilityManager;
import com.atlassian.jira.web.util.OutlookDateManager;
import com.googlecode.jsu.helpers.NameComparatorEx;

/**
 * This utils class exposes common methods to get field collections.
 *
 * @author <A href="mailto:abashev at gmail dot com">Alexey Abashev</A>
 * @version $Id$
 */
public class FieldCollectionsUtils {
    private static final Logger log = LoggerFactory.getLogger(FieldCollectionsUtils.class);

    private static final Collection<String> TIME_TRACKING_FIELDS = Arrays.asList(
            IssueFieldConstants.TIME_ESTIMATE,
            IssueFieldConstants.TIME_ORIGINAL_ESTIMATE,
            IssueFieldConstants.TIME_SPENT,
            IssueFieldConstants.TIMETRACKING,
            IssueFieldConstants.WORKLOG
    );

    private final I18nHelper.BeanFactory i18nHelper;
    private final ApplicationProperties applicationProperties;
    private final OutlookDateManager outlookDateManager;
    private final FieldManager fieldManager;
    private final FieldLayoutManager fieldLayoutManager;
    private final CustomFieldManager customFieldManager;
    private final FieldVisibilityManager fieldVisibilityManager;

    /**
     * @param i18nHelper
     * @param applicationProperties
     * @param outlookDateManager
     * @param fieldManager
     * @param fieldLayoutManager
     * @param customFieldManager
     * @param fieldVisibilityManager
     */
    public FieldCollectionsUtils(
            BeanFactory i18nHelper, ApplicationProperties applicationProperties,
            OutlookDateManager outlookDateManager, FieldManager fieldManager,
            FieldLayoutManager fieldLayoutManager,
            CustomFieldManager customFieldManager,
            FieldVisibilityManager fieldVisibilityManager
    ) {
        this.i18nHelper = i18nHelper;
        this.applicationProperties = applicationProperties;
        this.outlookDateManager = outlookDateManager;
        this.fieldManager = fieldManager;
        this.fieldLayoutManager = fieldLayoutManager;
        this.customFieldManager = customFieldManager;
        this.fieldVisibilityManager = fieldVisibilityManager;
    }

    /**
     * @return a complete list of fields, including custom fields.
     */
    public List<Field> getAllFields() {
        Set<Field> allFieldsSet = new TreeSet<Field>(getComparator());

        allFieldsSet.addAll(fieldManager.getOrderableFields());

        try {
            allFieldsSet.addAll(fieldManager.getAllAvailableNavigableFields());
        } catch (FieldException e) {
            log.error("Unable to load navigable fields", e);
        }

        return new ArrayList<Field>(allFieldsSet);
    }

    /**
     * @return a list of fields, including custom fields, which could be modified.
     */
    public List<Field> getAllEditableFields(){
        Set<Field> allFields = new TreeSet<Field>(getComparator());

        try {
            final Set<NavigableField> fields = fieldManager.getAllAvailableNavigableFields();

            for (Field f : fields) {
                allFields.add(f);
            }
        } catch (FieldException e) {
            log.error("Unable to load navigable fields", e);
        }

        return new ArrayList<Field>(allFields);
    }

    /**
     * @param allFields list of fields to be sorted.
     * @return a list with fields sorted by name.
     */
    public List<Field> sortFields(List<Field> allFields) {
        Collections.sort(allFields, getComparator());

        return allFields;
    }

    /**
     * @return a list of all fields of type date and datetime.
     */
    public List<Field> getAllDateFields() {
        List<Field> allDateFields = new ArrayList<Field>();

        List<CustomField> fields = customFieldManager.getCustomFieldObjects();

        for (CustomField cfDate : fields) {
            CustomFieldType customFieldType = cfDate.getCustomFieldType();

            if ((customFieldType instanceof DateCFType) || (customFieldType instanceof DateTimeCFType)){
                allDateFields.add(cfDate);
            }
        }

        // Obtain all fields type date from model.
        ModelEntity modelIssue = CoreFactory.getGenericDelegator().getModelEntity("Issue");
        Iterator<ModelField> modelFields = modelIssue.getFieldsIterator();

        while (modelFields.hasNext()) {
            ModelField modelField = modelFields.next();

            if(modelField.getType().equals("date-time")){
                Field fldDate = fieldManager.getField(modelField.getName());
                allDateFields.add(fldDate);
            }
        }

        return sortFields(allDateFields);
    }

    /**
     * @param issue: issue to which the field belongs
     * @param field wished field
     * @param fieldScreen wished screen
     * @return if a field is displayed in a screen.
     */
    public boolean isFieldOnScreen(Issue issue, Field field, FieldScreen fieldScreen){
        if (IssueFieldConstants.COMMENT.equals(field.getId())) { //Always present but cannot be detected.
            return true;
        }
        if (fieldManager.isCustomField(field)) {
            CustomFieldType type = ((CustomField) field).getCustomFieldType();

            if ((type instanceof ReadOnlyCFType) ||
                    (type instanceof ImportIdLinkCFType)) {
                return false;
            }
        }

        boolean retVal = false;
        Iterator<FieldScreenTab> itTabs = fieldScreen.getTabs().iterator();

        while(itTabs.hasNext() && !retVal){
            FieldScreenTab tab = itTabs.next();
            Iterator<FieldScreenLayoutItem> itFields = tab.getFieldScreenLayoutItems().iterator();

            while(itFields.hasNext() && !retVal){
                FieldScreenLayoutItem fieldScreenLayoutItem = itFields.next();

                if ( (field.getId().equals(fieldScreenLayoutItem.getFieldId()) && isIssueHasField(issue, field)) ||
                     (TIME_TRACKING_FIELDS.contains(field.getId()) && TIME_TRACKING_FIELDS.contains(fieldScreenLayoutItem.getFieldId()) ) //time tracking fields are not really clear...
                   ) {
                    retVal = true;
                }
            }
        }

        return retVal;
    }

    /*
    It's not possible to put a validation message on a timetracking field.
     */
    public boolean cannotSetValidationMessageToField(Field field) {
        return TIME_TRACKING_FIELDS.contains(field.getId());
    }

    /**
     * Check is the issue has the field.
     *
     * @param issue: issue to which the field belongs
     * @param field: wished field
     * @return if a field is available.
     */
    public boolean isIssueHasField(Issue issue, Field field) {
        final String fieldId = field.getId();

        boolean isHidden = false;

        if (TIME_TRACKING_FIELDS.contains(fieldId)) {
            isHidden = !fieldManager.isTimeTrackingOn();
        } else {
            isHidden = fieldVisibilityManager.isFieldHidden(field.getId(), issue);
        }

        if (isHidden) {
            // Looks like we found hidden field
            return false;
        }

        if (fieldManager.isCustomField(field)) {
            CustomField customField = (CustomField) field;
            FieldConfig config = customField.getRelevantConfig(issue);

            return (config != null);
        }

        return true;
    }

    public FieldLayoutItem getFieldLayoutItem(Issue issue, Field field) throws FieldLayoutStorageException {
                FieldLayout layout = fieldLayoutManager.getFieldLayout(
                issue.getProjectObject(),
                issue.getIssueTypeObject().getId()
        );

        if (layout.getId() == null) {
            layout = fieldLayoutManager.getEditableDefaultFieldLayout();
        }

        return layout.getFieldLayoutItem(field.getId());
    }

    /**
     * @param issue: issue to which the field belongs
     * @param field: wished field
     * @return if a field is required.
     */
    public boolean isFieldRequired(Issue issue, Field field) {
        boolean retVal = false;

        try {
            FieldLayoutItem fieldLayoutItem = getFieldLayoutItem(issue, field);

            if (fieldLayoutItem != null) {
                retVal = fieldLayoutItem.isRequired();
            }
        } catch (FieldLayoutStorageException e) {
            log.error("Unable to check is field required", e);
        }

        return retVal;
    }

    /**
     * @return a list of fields that could be chosen to copy their value.
     */
    public List<Field> getCopyFromFields(){
        List<Field> allFields = getAllFields();

        allFields.removeAll(getNonCopyFromFields());

        return allFields;
    }

    /**
     * @return a list of fields that will be eliminated from getCopyFromFields().
     */
    private List<Field> getNonCopyFromFields(){
        return asFields(
                ATTACHMENT,
                COMMENT,
                COMPONENTS,
                ISSUE_LINKS,
                SUBTASKS,
                THUMBNAIL,
                TIMETRACKING
        );
    }

    /**
     * @return a list of fields that could be chosen to copy their value.
     */
    public List<Field> getCopyToFields(){
        List<Field> allFields = getAllEditableFields();
        allFields.removeAll(getNonCopyToFields());

        return allFields;
    }

    /**
     * @return a list of fields that will be eliminated from getCopyFromFields().
     */
    private List<Field> getNonCopyToFields(){
        return asFields(
                ATTACHMENT,
                COMMENT,
                COMPONENTS,
                CREATED,
                TIMETRACKING,
                TIME_ORIGINAL_ESTIMATE,
                TIME_ESTIMATE,
                TIME_SPENT,
                AGGREGATE_TIME_ORIGINAL_ESTIMATE,
                AGGREGATE_TIME_ESTIMATE,
                AGGREGATE_PROGRESS,
                ISSUE_KEY,
                ISSUE_LINKS,
                ISSUE_TYPE,
                PRIORITY,
                PROJECT,
                SUBTASKS,
                THUMBNAIL,
                UPDATED,
                VOTES,
                WORKRATIO
        );
    }

    /**
     * @return a list of fields that could be chosen like required.
     */
    public List<Field> getRequirableFields(){
        List<Field> allFields = getAllFields();

        allFields.removeAll(getNonRequirableFields());

        return allFields;
    }

    /**
     * @return a list of fields that will be eliminated from getRequirableFields().
     */
    private List<Field> getNonRequirableFields(){
        return asFields(
                CREATED,
                TIMETRACKING,
                PROGRESS,
                AGGREGATE_TIME_ORIGINAL_ESTIMATE,
                AGGREGATE_PROGRESS,
                ISSUE_KEY,
                ISSUE_LINKS,
                ISSUE_TYPE,
                PROJECT,
                STATUS,
                SUBTASKS,
                THUMBNAIL,
                UPDATED,
                VOTES,
                WORKRATIO,
                "worklog",
                "aggregatetimeestimate",
                "aggregatetimespent"
        );
    }

    /**
     * @return a list of fields that could be chosen in Value-Field Condition.
     */
    public List<Field> getValueFieldConditionFields(){
        List<Field> allFields = getAllFields();

        allFields.removeAll(getNonValueFieldConditionFields());
        // Date fields are removed, because date comparison is not implemented yet. - See also ConditionCheckerFactory.
        allFields.removeAll(getAllDateFields());

        return allFields;
    }

    /**
     * @return a list of fields that will be eliminated from getValueFieldConditionFields().
     */
    private List<Field> getNonValueFieldConditionFields(){
        return asFields(
                ATTACHMENT,
                COMMENT,
                CREATED,
                ISSUE_KEY,
                ISSUE_LINKS,
                SUBTASKS,
                THUMBNAIL,
                TIMETRACKING,
                UPDATED,
                WORKRATIO
        );
    }

    /**
     * @param cal
     *
     * Clear the time part from a given Calendar.
     *
     */
    public void clearCalendarTimePart(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    /**
     * @param tsDate
     * @return a String.
     *
     * It formats to a date nice.
     */
    public String getNiceDate(Timestamp tsDate){
        Date timePerformed = new Date(tsDate.getTime());
        Locale defaultLocale = applicationProperties.getDefaultLocale();

        return outlookDateManager.getOutlookDate(defaultLocale).formatDMYHMS(timePerformed);
    }

    /**
     * Get comparator for sorting fields.
     * @return
     */
    private Comparator<Field> getComparator() {
        I18nHelper i18n = i18nHelper.getInstance(applicationProperties.getDefaultLocale());

        return new NameComparatorEx(i18n);
    }

    /**
     * Convert array of names into list of fields
     * @param names
     * @return
     */
    private List<Field> asFields(String ... names) {
        List<Field> result = new ArrayList<Field>(names.length);

        for (String name : names) {
            result.add(fieldManager.getField(name));
        }

        return result;
    }
}
