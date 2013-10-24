package com.googlecode.jsu.util;

import com.atlassian.crowd.embedded.api.CrowdService;
import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.bc.project.component.ProjectComponentManager;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueConstant;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.IssueRelationConstants;
import com.atlassian.jira.issue.ModifiedValue;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.customfields.MultipleCustomFieldType;
import com.atlassian.jira.issue.customfields.MultipleSettableCustomFieldType;
import com.atlassian.jira.issue.customfields.impl.AbstractMultiCFType;
import com.atlassian.jira.issue.customfields.impl.CascadingSelectCFType;
import com.atlassian.jira.issue.customfields.impl.FieldValidationException;
import com.atlassian.jira.issue.customfields.impl.LabelsCFType;
import com.atlassian.jira.issue.customfields.impl.MultiSelectCFType;
import com.atlassian.jira.issue.customfields.impl.MultiUserCFType;
import com.atlassian.jira.issue.customfields.impl.ProjectCFType;
import com.atlassian.jira.issue.customfields.impl.UserCFType;
import com.atlassian.jira.issue.customfields.impl.VersionCFType;
import com.atlassian.jira.issue.customfields.manager.OptionsManager;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.customfields.view.CustomFieldParams;
import com.atlassian.jira.issue.customfields.view.CustomFieldParamsImpl;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.fields.screen.FieldScreen;
import com.atlassian.jira.issue.label.Label;
import com.atlassian.jira.issue.label.LabelManager;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.issue.resolution.Resolution;
import com.atlassian.jira.issue.security.IssueSecurityLevelManager;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.issue.util.AggregateTimeTrackingCalculatorFactory;
import com.atlassian.jira.issue.util.IssueChangeHolder;
import com.atlassian.jira.issue.watchers.WatcherManager;
import com.atlassian.jira.issue.worklog.WorkRatio;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.usercompatibility.UserCompatibilityHelper;
import com.atlassian.jira.util.BuildUtilsInfo;
import com.atlassian.jira.util.ObjectUtils;
import com.atlassian.jira.workflow.WorkflowActionsBean;
import com.googlecode.jsu.helpers.checkers.ConverterString;
import com.opensymphony.workflow.loader.AbstractDescriptor;
import com.opensymphony.workflow.loader.ActionDescriptor;
import com.opensymphony.workflow.loader.FunctionDescriptor;
import org.apache.commons.lang.StringUtils;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.GenericValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This utils class exposes common methods to custom workflow objects.
 */
public class WorkflowUtils {
  public static final String SPLITTER = "@@";

  private final WorkflowActionsBean workflowActionsBean = new WorkflowActionsBean();
  private final Logger log = LoggerFactory.getLogger(WorkflowUtils.class);

  private final FieldManager fieldManager;
  private final IssueManager issueManager;
  private final ProjectComponentManager projectComponentManager;
  private final VersionManager versionManager;
  private final IssueSecurityLevelManager issueSecurityLevelManager;
  private final ApplicationProperties applicationProperties;
  private final FieldCollectionsUtils fieldCollectionsUtils;
  private final IssueLinkManager issueLinkManager;
  private final UserManager userManager;
  private final CrowdService crowdService;
  private final OptionsManager optionsManager;
  private final ProjectManager projectManager;
  private final LabelManager labelManager;
  private final AggregateTimeTrackingCalculatorFactory aggregateTimeTrackingCalculatorFactory;
  private final ConstantsManager constantsManager;
  private final BuildUtilsInfo buildUtilsInfo;
  private final WatcherManager watcherManager;
  private final JiraAuthenticationContext authenticationContext;

  /**
   * @param fieldManager
   * @param issueManager
   * @param projectComponentManager
   * @param versionManager
   * @param issueSecurityLevelManager
   * @param applicationProperties
   * @param fieldCollectionsUtils
   * @param issueLinkManager
   * @param labelManager
   * @param aggregateTimeTrackingCalculatorFactory
   * @param constantsManager
   * @param buildUtilsInfo
   * @param watcherManager
   * @param authenticationContext
   * */
  public WorkflowUtils(
    FieldManager fieldManager, IssueManager issueManager,
    ProjectComponentManager projectComponentManager, VersionManager versionManager,
    IssueSecurityLevelManager issueSecurityLevelManager, ApplicationProperties applicationProperties,
    FieldCollectionsUtils fieldCollectionsUtils, IssueLinkManager issueLinkManager,
    UserManager userManager, CrowdService crowdService, OptionsManager optionsManager,
    ProjectManager projectManager, LabelManager labelManager, AggregateTimeTrackingCalculatorFactory aggregateTimeTrackingCalculatorFactory, ConstantsManager constantsManager, BuildUtilsInfo buildUtilsInfo, WatcherManager watcherManager, JiraAuthenticationContext authenticationContext) {
    this.fieldManager = fieldManager;
    this.issueManager = issueManager;
    this.projectComponentManager = projectComponentManager;
    this.versionManager = versionManager;
    this.issueSecurityLevelManager = issueSecurityLevelManager;
    this.applicationProperties = applicationProperties;
    this.fieldCollectionsUtils = fieldCollectionsUtils;
    this.issueLinkManager = issueLinkManager;
    this.userManager = userManager;
    this.crowdService = crowdService;
    this.optionsManager = optionsManager;
    this.projectManager = projectManager;
    this.labelManager = labelManager;
    this.aggregateTimeTrackingCalculatorFactory = aggregateTimeTrackingCalculatorFactory;
    this.constantsManager = constantsManager;
    this.buildUtilsInfo = buildUtilsInfo;
    this.watcherManager = watcherManager;
    this.authenticationContext = authenticationContext;
  }

  /**
   * @param key
   * @return a String with the field name from given key.
   */
  public String getFieldNameFromKey(String key) {
    return getFieldFromKey(key).getName();
  }

  /**
   * @param key
   * @return a Field object from given key. (Field or Custom Field).
   */
  public Field getFieldFromKey(String key) {
    Field field;

    if (fieldManager.isCustomField(key)) {
      field = fieldManager.getCustomField(key);
    } else {
      field = fieldManager.getField(key);
    }

    if (field == null) {
      throw new IllegalArgumentException("Unable to find field '" + key + "'");
    }

    return field;
  }

  public Field getFieldFromDescriptor(AbstractDescriptor descriptor, String name) {
    FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
    Map args = functionDescriptor.getArgs();
    String fieldKey = (String) args.get(name);

    try {
      return getFieldFromKey(fieldKey);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * @param issue           an issue object.
   * @param field           a field object. (May be a Custom Field)
   * @param catenateCascade
   * @return an Object
   *         <p/>
   *         It returns the value of a field within issue object. May be a Collection,
   *         a List, a Strong, or any FildType within JIRA.
   */
  public Object getFieldValueFromIssue(Issue issue, Field field, boolean catenateCascade) {
    Object retVal = null;

    try {
      if (fieldManager.isCustomField(field)) {
        // Return the CustomField value. It could be any object.
        CustomField customField = (CustomField) field;
        Object value = issue.getCustomFieldValue(customField);

        if (customField.getCustomFieldType() instanceof CascadingSelectCFType) {
          HashMap<String, Option> hashMapEntries = (HashMap<String, Option>) value;

          if (hashMapEntries != null) {
            Option parent = hashMapEntries.get(CascadingSelectCFType.PARENT_KEY);
            Option child = hashMapEntries.get(CascadingSelectCFType.CHILD_KEY);

            if (parent != null) {
              if (ObjectUtils.isValueSelected(child)) {
                if (catenateCascade)
                  retVal = parent.toString() + " - " + child.toString();
                else
                  retVal = child.toString();
              } else if (catenateCascade)
                retVal = parent.toString();
              else {
                final List<Option> childOptions = parent.getChildOptions();

                if ((childOptions == null) || (childOptions.isEmpty())) {
                  retVal = parent.toString();
                }
              }
            }
          }
        } else if (value instanceof Option) {
          retVal = ((Option) value).toString();
        } else {
          retVal = value;
        }

        if (log.isDebugEnabled()) {
          log.debug(
            String.format(
              "Got field value [object=%s;class=%s]",
              retVal, ((retVal != null) ? retVal.getClass() : "")
            )
          );
        }
      } else {
        String fieldId = field.getId();
        Collection<?> retCollection = null;

        // Special treatment of fields.
        if (fieldId.equals(IssueFieldConstants.ATTACHMENT)) {
          // return a collection with the attachments associated to given issue.
          retCollection = issue.getAttachments();

          if (retCollection != null && !retCollection.isEmpty()) {
            retVal = retCollection;
          }
        } else if (fieldId.equals(IssueFieldConstants.AFFECTED_VERSIONS)) {
          retCollection = issue.getAffectedVersions();

          if (retCollection != null && !retCollection.isEmpty()) {
            retVal = retCollection;
          }
        } else if (fieldId.equals(IssueFieldConstants.COMMENT)) {
          // return a list with the comments of a given issue.
          try {
            retCollection = issueManager.getEntitiesByIssueObject(
              IssueRelationConstants.COMMENTS, issue
            );

            if (retCollection != null && !retCollection.isEmpty()) {
              retVal = retCollection;
            }
          } catch (GenericEntityException e) {
            retVal = null;
          }
        } else if (fieldId.equals(IssueFieldConstants.COMPONENTS)) {
          retCollection = issue.getComponents();

          if (retCollection != null && !retCollection.isEmpty()) {
            retVal = retCollection;
          }
        } else if (fieldId.equals(IssueFieldConstants.FIX_FOR_VERSIONS)) {
          retCollection = issue.getFixVersions();

          if (retCollection != null && !retCollection.isEmpty()) {
            retVal = retCollection;
          }
        } else if (fieldId.equals(IssueFieldConstants.THUMBNAIL)) {
          // Not implemented, yet.
        } else if (fieldId.equals(IssueFieldConstants.ISSUE_TYPE)) {
          retVal = issue.getIssueTypeObject();
        } else if (fieldId.equals(IssueFieldConstants.TIMETRACKING)) {
          // Not implemented, yet.
        } else if (fieldId.equals(IssueFieldConstants.ISSUE_LINKS)) {
          retVal = issueLinkManager.getOutwardLinks(issue.getId());
          ((Collection) retVal).addAll(issueLinkManager.getInwardLinks(issue.getId()));
        } else if (fieldId.equals(IssueFieldConstants.WORKRATIO)) {
          retVal = String.valueOf(WorkRatio.getWorkRatio(issue));
        } else if (fieldId.equals(IssueFieldConstants.ISSUE_KEY)) {
          retVal = issue.getKey();
        } else if (fieldId.equals(IssueFieldConstants.SUBTASKS)) {
          retCollection = issue.getSubTaskObjects();

          if (retCollection != null && !retCollection.isEmpty()) {
            retVal = retCollection;
          }
        } else if (fieldId.equals(IssueFieldConstants.PRIORITY)) {
          retVal = issue.getPriorityObject();
        } else if (fieldId.equals(IssueFieldConstants.RESOLUTION)) {
          retVal = issue.getResolutionObject();
        } else if (fieldId.equals(IssueFieldConstants.STATUS)) {
          retVal = issue.getStatusObject();
        } else if (fieldId.equals(IssueFieldConstants.PROJECT)) {
          retVal = issue.getProjectObject();
        } else if (fieldId.equals(IssueFieldConstants.SECURITY)) {
          retVal = issue.getSecurityLevel();
        } else if (fieldId.equals(IssueFieldConstants.TIME_ESTIMATE)) {
          retVal = issue.getEstimate();
        } else if (fieldId.equals(IssueFieldConstants.TIME_ORIGINAL_ESTIMATE)) {
          retVal = issue.getOriginalEstimate();
        } else if (fieldId.equals(IssueFieldConstants.TIME_SPENT)) {
          retVal = issue.getTimeSpent();
        } else if (fieldId.equals(IssueFieldConstants.AGGREGATE_TIME_SPENT)) {
          retVal = aggregateTimeTrackingCalculatorFactory.getCalculator(issue).getAggregates(issue).getTimeSpent();
        } else if (fieldId.equals(IssueFieldConstants.AGGREGATE_TIME_ESTIMATE)) {
          retVal = aggregateTimeTrackingCalculatorFactory.getCalculator(issue).getAggregates(issue).getRemainingEstimate();
        } else if (fieldId.equals(IssueFieldConstants.AGGREGATE_TIME_ORIGINAL_ESTIMATE)) {
          retVal = aggregateTimeTrackingCalculatorFactory.getCalculator(issue).getAggregates(issue).getOriginalEstimate();
        } else if (fieldId.equals(IssueFieldConstants.ASSIGNEE)) {
          retVal = issue.getAssigneeUser();
        } else if (fieldId.equals(IssueFieldConstants.REPORTER)) {
          retVal = issue.getReporterUser();
        } else if (fieldId.equals(IssueFieldConstants.DESCRIPTION)) {
          retVal = issue.getDescription();
        } else if (fieldId.equals(IssueFieldConstants.ENVIRONMENT)) {
          retVal = issue.getEnvironment();
        } else if (fieldId.equals(IssueFieldConstants.SUMMARY)) {
          retVal = issue.getSummary();
        } else if (fieldId.equals(IssueFieldConstants.DUE_DATE)) {
          retVal = issue.getDueDate();
        } else if (fieldId.equals(IssueFieldConstants.UPDATED)) {
          retVal = issue.getUpdated();
        } else if (fieldId.equals(IssueFieldConstants.CREATED)) {
          retVal = issue.getCreated();
        } else if (fieldId.equals(IssueFieldConstants.RESOLUTION_DATE)) {
          retVal = issue.getResolutionDate();
        } else if (fieldId.equals(IssueFieldConstants.LABELS)) {
          retVal = issue.getLabels();
        } else if (fieldId.equals(IssueFieldConstants.WATCHES)) { //this is actually shown by JIRA as the "watchers" field
          retVal = watcherManager.getCurrentWatchList(issue, authenticationContext.getLocale());
        } else {
          log.warn("Issue field \"" + fieldId + "\" is not supported.");

          GenericValue gvIssue = issue.getGenericValue();

          if (gvIssue != null) {
            retVal = gvIssue.get(fieldId);
          }
        }
      }
    } catch (NullPointerException e) {
      retVal = null;

      log.error("Unable to get field \"" + field.getId() + "\" value", e);
    }

    return retVal;
  }

  /**
   * Sets specified value to the field for the issue.
   *
   * @param issue
   * @param field
   * @param value
   */
  public void setFieldValue(MutableIssue issue, Field field, Object value, IssueChangeHolder changeHolder) {
    if (fieldManager.isCustomField(field)) {
      CustomField customField = (CustomField) field;
      Object oldValue = issue.getCustomFieldValue(customField);
      FieldLayoutItem fieldLayoutItem;
      CustomFieldType cfType = customField.getCustomFieldType();

      if (log.isDebugEnabled()) {
        log.debug(
          String.format(
            "Set custom field value " +
              "[field=%s,type=%s,oldValue=%s,newValueClass=%s,newValue=%s]",
            customField,
            cfType,
            oldValue,
            (value != null) ? value.getClass().getName() : "null",
            value
          )
        );
      }

      fieldLayoutItem = fieldCollectionsUtils.getFieldLayoutItem(issue, field);
      Object newValue = value;

      if (value instanceof IssueConstant) {
        newValue = ((IssueConstant) value).getName();
      } else if (value instanceof GenericValue) {
        final GenericValue gv = (GenericValue) value;

        if ("SchemeIssueSecurityLevels".equals(gv.getEntityName())) { // We got security level
          newValue = gv.getString("name");
        }
      } else if (value instanceof Option && !(cfType instanceof MultipleSettableCustomFieldType)) {
        newValue = ((Option) newValue).getValue();
      } else if (value instanceof Timestamp && !fieldCollectionsUtils.getAllDateFields().contains(field)) {
        String format = applicationProperties.getDefaultBackedString(APKeys.JIRA_DATE_TIME_PICKER_JAVA_FORMAT);
        DateFormat dateFormat = new SimpleDateFormat(format);
        newValue = dateFormat.format(value);
      }

      if (cfType instanceof VersionCFType) {
        newValue = convertValueToVersions(issue, newValue);
      } else if (cfType instanceof ProjectCFType) {
        newValue = convertValueToProject(newValue);
      } else if (newValue instanceof String) {
        if (cfType instanceof MultipleSettableCustomFieldType) {
          Option option = convertStringToOption(issue, customField, (String) newValue);
          if (cfType instanceof MultiSelectCFType) {
            newValue = asArrayList(option);
          } else if (cfType instanceof CascadingSelectCFType) {
            newValue = convertOptionToCustomFieldParamsImpl(customField, option);
          } else {
            newValue = option;
          }
        } else if (cfType instanceof LabelsCFType && ((String) newValue).contains(" ")) {
          String[] labels = ((String) newValue).split(" +");
          CustomFieldParams fieldParams = new CustomFieldParamsImpl(customField, labels);
          newValue = cfType.getValueFromCustomFieldParams(fieldParams);
        } else {
          //convert from string to Object
          CustomFieldParams fieldParams = new CustomFieldParamsImpl(customField, newValue);
          newValue = cfType.getValueFromCustomFieldParams(fieldParams);
        }
      } else if (newValue instanceof Collection<?>) {
        if (customField.getCustomFieldType() instanceof MultiUserCFType) {
          newValue = convertValueToAppUser(newValue);
        } else if (((customField.getCustomFieldType() instanceof AbstractMultiCFType) ||
          (customField.getCustomFieldType() instanceof MultipleCustomFieldType))) {
          // format already correct
        } else if (customField.getCustomFieldType() instanceof LabelsCFType) {
          if (!(newValue instanceof Set)) {
            newValue = new HashSet((Collection) newValue);
          }
        } else {
          //convert from string to Object
          CustomFieldParams fieldParams = new CustomFieldParamsImpl(
            customField,
            StringUtils.join((Collection<?>) newValue, ",")
          );

          newValue = cfType.getValueFromCustomFieldParams(fieldParams);
        }
      } else if (newValue instanceof Option && !(cfType instanceof MultipleSettableCustomFieldType)) {
        newValue = ((Option) newValue).getValue();
      } else if (cfType instanceof UserCFType) {
        newValue = convertValueToAppUser(newValue);
      } else if (cfType instanceof AbstractMultiCFType) {
        if (cfType instanceof MultiUserCFType) {
          newValue = convertValueToAppUser(newValue);
        }
        if (newValue != null) {
          newValue = asArrayList(newValue);
        }
      } else if (UserCompatibilityHelper.isUserObject(newValue)) {
        newValue = UserCompatibilityHelper.convertUserObject(newValue).getKey();
      }

      if (log.isDebugEnabled()) {
        log.debug("Got new value [class=" +
          ((newValue != null) ? newValue.getClass().getName() : "null") +
          ",value=" +
          newValue +
          "]"
        );
      }

      // Updating internal custom field value
      issue.setCustomFieldValue(customField, newValue);

      customField.updateValue(
        fieldLayoutItem, issue,
        new ModifiedValue(oldValue, newValue), changeHolder
      );

      if (log.isDebugEnabled()) {
        log.debug(
          "Issue [" +
            issue +
            "] got modfied fields - [" +
            issue.getModifiedFields() +
            "]"
        );
      }

      // Not new
/*            if (issue.getKey() != null) {
                // Remove duplicated issue update
                if (issue.getModifiedFields().containsKey(field.getId())) {
                    issue.getModifiedFields().remove(field.getId());
                }
            }
*/
    } else { //----- System Fields -----
      final String fieldId = field.getId();

      // Special treatment of fields.
      if (fieldId.equals(IssueFieldConstants.ATTACHMENT)) {
        throw new UnsupportedOperationException("Not implemented");
        //				// return a collection with the attachments associated to given issue.
        //				retCollection = (Collection)issue.getExternalFieldValue(fieldId);
        //				if(retCollection==null || retCollection.isEmpty()){
        //					isEmpty = true;
        //				}else{
        //					retVal = retCollection;
        //				}
      } else if (fieldId.equals(IssueFieldConstants.AFFECTED_VERSIONS)) {
        Collection<Version> versions = convertValueToVersions(issue, value);
        issue.setAffectedVersions(versions);
      } else if (fieldId.equals(IssueFieldConstants.COMMENT)) {
        throw new UnsupportedOperationException("Not implemented");

        //				// return a list with the comments of a given issue.
        //				try {
        //					retCollection = ManagerFactory.getIssueManager().getEntitiesByIssue(IssueRelationConstants.COMMENTS, issue.getGenericValue());
        //					if(retCollection==null || retCollection.isEmpty()){
        //						isEmpty = true;
        //					}else{
        //						retVal = retCollection;
        //					}
        //				} catch (GenericEntityException e) {
        //					retVal = null;
        //				}
      } else if (fieldId.equals(IssueFieldConstants.COMPONENTS)) {
        Collection<GenericValue> components = convertValueToComponents(issue, value);
        issue.setComponents(components);
      } else if (fieldId.equals(IssueFieldConstants.FIX_FOR_VERSIONS)) {
        Collection<Version> versions = convertValueToVersions(issue, value);
        issue.setFixVersions(versions);
      } else if (fieldId.equals(IssueFieldConstants.THUMBNAIL)) {
        throw new UnsupportedOperationException("Not implemented");

        //				// Not implemented, yet.
        //				isEmpty = true;
      } else if (fieldId.equals(IssueFieldConstants.ISSUE_TYPE)) {
        throw new UnsupportedOperationException("Not implemented");
        //
        //				retVal = issue.getIssueTypeObject();
      } else if (fieldId.equals(IssueFieldConstants.TIMETRACKING)) {
        throw new UnsupportedOperationException("Not implemented");
        //
        //				// Not implemented, yet.
        //				isEmpty = true;
      } else if (fieldId.equals(IssueFieldConstants.ISSUE_LINKS)) {
        throw new UnsupportedOperationException("Not implemented");
        //
        //				retVal = ComponentManager.getInstance().getIssueLinkManager().getIssueLinks(issue.getId());
      } else if (fieldId.equals(IssueFieldConstants.WORKRATIO)) {
        throw new UnsupportedOperationException("Not implemented");
        //
        //				retVal = String.valueOf(WorkRatio.getWorkRatio(issue));
      } else if (fieldId.equals(IssueFieldConstants.ISSUE_KEY)) {
        throw new UnsupportedOperationException("Not implemented");
        //
        //				retVal = issue.getKey();
      } else if (fieldId.equals(IssueFieldConstants.SUBTASKS)) {
        throw new UnsupportedOperationException("Not implemented");
        //
        //				retCollection = issue.getSubTasks();
        //				if(retCollection==null || retCollection.isEmpty()){
        //					isEmpty = true;
        //				}else{
        //					retVal = retCollection;
        //				}
      } else if (fieldId.equals(IssueFieldConstants.PRIORITY)) {
        if (value == null) {
          issue.setPriority(null);
        } else if (value instanceof GenericValue) {
          issue.setStatus((GenericValue) value);
        } else if (value instanceof Priority) {
          issue.setPriorityId(((Priority) value).getId());
        } else {
          Priority priority = constantsManager.getPriorityObject(value.toString());

          if (priority != null) {
            issue.setPriorityId(priority.getId());
          } else {
            throw new IllegalArgumentException("Unable to find priority with name \"" + value + "\"");
          }
        }
      } else if (fieldId.equals(IssueFieldConstants.RESOLUTION)) {
        if (value == null) {
          issue.setResolution(null);
        } else if (value instanceof GenericValue) {
          issue.setResolution((GenericValue) value);
        } else if (value instanceof Resolution) {
          issue.setResolutionId(((Resolution) value).getId());
        } else {
          Collection<Resolution> resolutions = ComponentManager.getInstance().getConstantsManager().getResolutionObjects();
          Resolution resolution = null;
          String s = value.toString().trim();

          for (Resolution r : resolutions) {
            if (r.getName().equalsIgnoreCase(s)) {
              resolution = r;

              break;
            }
          }

          if (resolution != null) {
            issue.setResolutionId(resolution.getId());
          } else {
            throw new IllegalArgumentException("Unable to find resolution with name \"" + value + "\"");
          }
        }
      } else if (fieldId.equals(IssueFieldConstants.STATUS)) {
        if (value == null) {
          issue.setStatus(null);
        } else if (value instanceof GenericValue) {
          issue.setStatus((GenericValue) value);
        } else if (value instanceof Status) {
          issue.setStatusId(((Status) value).getId());
        } else {
          Status status = ComponentManager.getInstance().getConstantsManager().getStatusByName(value.toString());

          if (status != null) {
            issue.setStatusId(status.getId());
          } else {
            throw new IllegalArgumentException("Unable to find status with name \"" + value + "\"");
          }
        }
      } else if (fieldId.equals(IssueFieldConstants.SECURITY)) {
        if (value == null) {
          issue.setSecurityLevel(null);
        } else if (value instanceof GenericValue) {
          issue.setSecurityLevel((GenericValue) value);
        } else if (value instanceof Long) {
          issue.setSecurityLevelId((Long) value);
        } else {
          Collection<GenericValue> levels;

          try {
            Long l = Long.decode(value.toString());
            issue.setSecurityLevelId(l);
          } catch (NumberFormatException ignore) {
            //try looking for the security level name instead
            try {
              levels = issueSecurityLevelManager.getSecurityLevelsByName(value.toString());
            } catch (GenericEntityException e) {
              throw new IllegalArgumentException("Unable to find security level \"" + value + "\"");
            }

            if (levels == null || levels.size() == 0) {
              throw new IllegalArgumentException("Unable to find security level \"" + value + "\"");
            }

            if (levels.size() > 1) {
              throw new IllegalArgumentException("More that one security level with name \"" + value + "\"");
            }

            issue.setSecurityLevel(levels.iterator().next());
          }
        }
      } else if (fieldId.equals(IssueFieldConstants.ASSIGNEE)) {
        User user = convertValueToUser(value);
        issue.setAssignee(user);
      } else if (fieldId.equals(IssueFieldConstants.DUE_DATE)) {
        if (value == null) {
          issue.setDueDate(null);
        }

        if (value instanceof Timestamp) {
          issue.setDueDate((Timestamp) value);
        } else if (value instanceof String) {
          SimpleDateFormat formatter = new SimpleDateFormat(
            applicationProperties.getDefaultString(APKeys.JIRA_DATE_TIME_PICKER_JAVA_FORMAT)
          );

          try {
            Date date = formatter.parse((String) value);

            if (date != null) {
              issue.setDueDate(new Timestamp(date.getTime()));
            } else {
              issue.setDueDate(null);
            }
          } catch (ParseException e) {
            throw new IllegalArgumentException("Wrong date format exception for \"" + value + "\"");
          }
        }
      } else if (fieldId.equals(IssueFieldConstants.REPORTER)) {
        User user = convertValueToUser(value);
        issue.setReporter(user);
      } else if (fieldId.equals(IssueFieldConstants.SUMMARY)) {
        if ((value == null) || (value instanceof String)) {
          issue.setSummary((String) value);
        } else {
          issue.setSummary(value.toString());
        }
      } else if (fieldId.equals(IssueFieldConstants.DESCRIPTION)) {
        if ((value == null) || (value instanceof String)) {
          issue.setDescription((String) value);
        } else {
          issue.setDescription(value.toString());
        }
      } else if (fieldId.equals(IssueFieldConstants.WATCHES)) { //this is the watchers field
        if (value instanceof Collection) {
          for (Object v : ((Collection)value)) {
            User u = convertValueToUser(v);
            if (u != null && !watcherManager.isWatching(u,issue))
              watcherManager.startWatching(u,issue);
          }
        } else {
          User u = convertValueToUser(value);
          if (u != null && !watcherManager.isWatching(u,issue))
            watcherManager.startWatching(u,issue);
        }
      } else if (fieldId.equals(IssueFieldConstants.LABELS)) {
        if ((value == null) || (value instanceof Set)) {
          issue.setLabels((Set<Label>) value);
        } else if (value instanceof Collection) {
            issue.setLabels(new HashSet<Label>((Collection)value));
        } else {
          throw new UnsupportedOperationException("Wrong value type for setting 'Labels'");
        }
      } else if (fieldId.equals(IssueFieldConstants.TIME_SPENT)) {
        if ((value == null) || (value instanceof Long)) {
          issue.setTimeSpent((Long) value);
        } else {
          throw new UnsupportedOperationException("Wrong value type for setting 'Time Spent' (Long expected)");
        }
      } else if (fieldId.equals(IssueFieldConstants.TIME_ESTIMATE)) {
        if ((value == null) || (value instanceof Long)) {
          issue.setEstimate((Long) value);
        } else {
          throw new UnsupportedOperationException("Wrong value type for setting 'Time Estimate' (Long expected)");
        }
      } else if (fieldId.equals(IssueFieldConstants.TIME_ORIGINAL_ESTIMATE)) {
        if ((value == null) || (value instanceof Long)) {
          issue.setOriginalEstimate((Long) value);
        } else {
          throw new UnsupportedOperationException("Wrong value type for setting 'Original Estimate' (Long expected)");
        }
      } else if (fieldId.equals(IssueFieldConstants.ENVIRONMENT)) {
        if ((value == null) || (value instanceof String)) {
          issue.setEnvironment((String) value);
        } else {
          issue.setEnvironment(value.toString());
        }
      } else {
        log.error("Issue field \"" + fieldId + "\" is not supported for setting.");
      }
    }
  }

  private static final ConverterString CONVERTER_STRING = new ConverterString();

  public String convertToString(Object value) {
    return CONVERTER_STRING.convert(value);
  }

  private Option convertStringToOption(Issue issue, CustomField customField, String value) {
    FieldConfig relevantConfig = customField.getRelevantConfig(issue);
    List<Option> options = optionsManager.findByOptionValue(value);
    if (options.size() == 0) {
      try {
        Long optionId = Long.parseLong(value);
        Option option = optionsManager.findByOptionId(optionId);
        options = Collections.singletonList(option);
      } catch (NumberFormatException e) { /* IllegalArgumentException will be thrown at end of this method. */ }
    }
    for (Option option : options) {
      FieldConfig fieldConfig = option.getRelatedCustomField();
      if (relevantConfig != null && relevantConfig.equals(fieldConfig)) {
        return option;
      }
    }
    throw new IllegalArgumentException("No option found with value '" + value + "' for custom field " + customField.getName() + " on issue " + issue.getKey() + ".");
  }

  private Collection<GenericValue> convertValueToComponents(Issue issue, Object value) {
    if (value == null) {
      return Collections.<GenericValue>emptySet();
    } else if (value instanceof GenericValue) {
      return Arrays.asList((GenericValue) value);
    } else if (value instanceof Collection) {
      return (Collection<GenericValue>) value;
    } else {
      ProjectComponent v = projectComponentManager.findByComponentName(
        issue.getProjectObject().getId(), convertToString(value)
      );

      if (v != null) {
        return Arrays.asList(v.getGenericValue());
      }
      throw new IllegalArgumentException("Wrong component value '" + value + "'.");
    }
  }

  private Collection<Version> convertValueToVersions(Issue issue, Object value) {
    if (value == null) {
      return Collections.emptySet();
    } else if (value instanceof Version) {
      return (Arrays.asList(adaptVersionToProject((Version) value, issue.getProjectObject())));
    } else if (value instanceof Collection) {
      List<Version> versions = new ArrayList<Version>(((Collection)value).size());
      for (Object v : (Collection)value)
        versions.add(adaptVersionToProject((Version) v, issue.getProjectObject()));
      return versions;
    } else {
      Version v = versionManager.getVersion(issue.getProjectObject().getId(), convertToString(value));
      if (v != null) {
        return Arrays.asList(v);
      }
      throw new IllegalArgumentException("Wrong version value '" + value + "'.");
    }
  }

  private Version adaptVersionToProject(Version version, Project project) {
    if (version.getProjectObject().equals(project))
      return version;
    Version v = versionManager.getVersion(project.getId(), version.getName());
    if (v==null)
      throw new IllegalArgumentException("Version '" + version.getName() + "' does not exist in project '" + project.getName() +"'.");
    return v;
  }

  public Object convertValueToAppUser(Object value) {
    if (value instanceof Collection<?>) {
      Collection list = new ArrayList(((Collection) value).size());
      for (Object obj : (Collection) value) {
        list.add(convertValueToAppUser(obj));
      }
      return list;
    }

    if (value == null)
      return null;

    if (buildUtilsInfo.getVersionNumbers()[0] < 6)
      return convertValueToUser(value);

    Object user;
    try {
      Method getUserByKeyMethod = userManager.getClass().getMethod("getUserByKey", String.class);
      user = getUserByKeyMethod.invoke(userManager, convertToString(value));
      if (user==null) {
        Method getUserByNameMethod = userManager.getClass().getMethod("getUserByName", String.class);
        user = getUserByNameMethod.invoke(userManager, convertToString(value));
      }
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    }
    if (user != null) {
      return user;
    }
    throw new IllegalArgumentException("User '" + value + "' not found.");
  }

  private User convertValueToUser(Object value) {
    if (value instanceof Collection<?>) {
      value = firstValue((Collection) value);
    }
    if (value == null || value instanceof User) {
      return (User) value;
    } else {
      User user = UserCompatibilityHelper.getUserForKey(convertToString(value));
      if (user != null)
        return user;
      if (buildUtilsInfo.getVersionNumbers()[0] >= 6) {
        try {
          Method getUserByNameMethod = userManager.getClass().getMethod("getUserByName", String.class);
          user = UserCompatibilityHelper.convertUserObject(getUserByNameMethod.invoke(userManager, convertToString(value))).getUser();
        } catch (IllegalAccessException e) {
          throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
          throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
          throw new RuntimeException(e);
        }
      }
      if (user != null)
        return user;

      throw new IllegalArgumentException("User '" + value + "' not found.");
    }
  }

  //Custom fields still (JIRA 5.0) expect GenericValue. They cannot yet handle ProjectObj. - So this implementation is for later. For now we keep using convertValueToProject(...)
  private Project convertValueToProjectObj(Object value) {
    Project project;
    if (value == null || value instanceof Project) {
      return (Project) value;
    } else if (value instanceof GenericValue) {
      value = ((GenericValue) value).get("id");
    }

    if (value instanceof Long) {
      project = projectManager.getProjectObj((Long) value);
      if (project != null) return project;
    } else {
      String s = convertToString(value);
      try {
        Long id = Long.parseLong(s);
        project = projectManager.getProjectObj(id);
        if (project != null) return project;
      } catch (NumberFormatException e) {
        project = projectManager.getProjectObjByKey(s);
        if (project == null) {
          project = projectManager.getProjectObjByName(s);
        }
        if (project != null) return project;
      }
    }
    throw new IllegalArgumentException("Wrong project value '" + value + "'.");
  }

  private Project convertValueToProject(Object value) {
    Project project;
    if (value instanceof Project) {
      return (Project) value;
    } else if (value instanceof Long) {
      project = projectManager.getProjectObj((Long) value);
      if (project != null) return project;
    } else {
      String s = convertToString(value);
      try {
        Long id = Long.parseLong(s);
        project = projectManager.getProjectObj(id);
        if (project != null) return project;
      } catch (NumberFormatException e) {
        project = projectManager.getProjectObjByKey(s);
        if (project == null) {
          project = projectManager.getProjectObjByName(s);
        }
        if (project != null) return project;
      }
    }
    throw new IllegalArgumentException("Wrong project value '" + value + "'.");
  }

  private Map convertOptionToCustomFieldParamsImpl(CustomField customField, Option option) {
    Map params = new HashMap();
    Option upperOption = option.getParentOption();
    Collection<String> val;
    if (upperOption != null) {
      params.put(CascadingSelectCFType.PARENT_KEY, upperOption);
      params.put(CascadingSelectCFType.CHILD_KEY, option);
    } else {
      params.put(CascadingSelectCFType.PARENT_KEY, option);
    }
    return params;
  }

  private <T> ArrayList<T> asArrayList(T value) {
    ArrayList<T> list = new ArrayList<T>(1);
    list.add(value);
    return list;
  }

  private Object firstValue(Collection col) {
    int s = col.size();
    if (s == 0) {
      return null;
    } else {
      if (s > 1) {
        log.debug("Got multiple values: " + col.toString() + ". Using only one of them.");
      }
      return col.iterator().next();
    }
  }

  /**
   * Method sets value for issue field. Field was defined as string
   *
   * @param issue    Muttable issue for changing
   * @param fieldKey Field name
   * @param value    Value for setting
   */
  public void setFieldValue(
    MutableIssue issue, String fieldKey, Object value,
    IssueChangeHolder changeHolder
  ) {
    final Field field = getFieldFromKey(fieldKey);

    setFieldValue(issue, field, value, changeHolder);
  }

  /**
   * @param strGroups
   * @param splitter
   * @return a List of Group
   *         <p/>
   *         Get Groups from a string.
   */
  public List<Group> getGroups(String strGroups, String splitter) {
    String[] groups = strGroups.split("\\Q" + splitter + "\\E");
    List<Group> groupList = new ArrayList<Group>(groups.length);

    for (String s : groups) {
      Group group = crowdService.getGroup(s);

      //JMWE-30
      if (group != null)
        groupList.add(group);
    }

    return groupList;
  }

  /**
   * @param groups
   * @param splitter
   * @return a String with the groups selected.
   *         <p/>
   *         Get Groups as String.
   */
  public String getStringGroup(Collection<Group> groups, String splitter) {
    StringBuilder sb = new StringBuilder();

    for (Group g : groups) {
      sb.append(g.getName()).append(splitter);
    }

    return sb.toString();
  }

  /**
   * @param strFields
   * @param splitter
   * @return a List of Field
   *         <p/>
   *         Get Fields from a string.
   */
  public List<Field> getFields(String strFields, String splitter) {
    String[] fields = strFields.split("\\Q" + splitter + "\\E");
    List<Field> fieldList = new ArrayList<Field>(fields.length);

    for (String s : fields) {
      final Field field = fieldManager.getField(s);

      if (field != null) {
        fieldList.add(field);
      }
    }

    return fieldCollectionsUtils.sortFields(fieldList);
  }

  /**
   * @param fields
   * @param splitter
   * @return a String with the fields selected.
   *         <p/>
   *         Get Fields as String.
   */
  public String getStringField(Collection<Field> fields, String splitter) {
    StringBuilder sb = new StringBuilder();

    for (Field f : fields) {
      sb.append(f.getId()).append(splitter);
    }

    return sb.toString();
  }

  /**
   * @param actionDescriptor
   * @return the FieldScreen of the transition. Or null, if the transition
   *         hasn't a screen asociated.
   *         <p/>
   *         It obtains the fieldscreen for a transition, if it have one.
   */
  public FieldScreen getFieldScreen(ActionDescriptor actionDescriptor) {
    return workflowActionsBean.getFieldScreenForView(actionDescriptor);
  }

  public Object convertValue(Field field, Object value, Issue issue) {

    if (value instanceof String[]) {
      return stringsToObject(field, (String[]) value, issue);
    }
    if (value instanceof ArrayList && (((ArrayList) value).size()>0)
      && ((ArrayList)value).get(0) instanceof String) {
      return stringsToObject(field, ((ArrayList<String>)value).toArray(new String[0]), issue);
    }
    if (value instanceof String) {
      return stringToObject(field, (String)value, issue);
    }

    return value;
  }

  private Object stringsToObject(Field field, String[] value, Issue issue) {
    ArrayList values = new ArrayList(((String[])value).length);
    for (String v : (String[])value) {
      Object o = stringToObject(field, v, issue);
      if (o instanceof Collection)
        values.addAll((Collection)o);
      else
        values.add(o);
    }
    return values;
  }

  private Object stringToObject(Field field, String string, Issue issue) {
    boolean wrapInCollection = false;
    if (field instanceof CustomField && ((CustomField)field).getCustomFieldType() instanceof AbstractMultiCFType ) {
      Collection<String> strings = MultiSelectCFType.extractTransferObjectFromString(string);
      if (strings.size() > 1)
        return stringsToObject(field, strings.toArray(new String[0]), issue);
      else
        wrapInCollection = true;
    }
    if (field instanceof OrderableField) {
      OrderableField orderableField = (OrderableField) field;
      Map values = new HashMap();
      if (field instanceof CustomField) {
        try {
          Object singleObject = ((CustomField) field).getCustomFieldType().getSingularObjectFromString(string);
          if (singleObject==null)
            return string;
          if (wrapInCollection)
            return Collections.singleton(singleObject);
          else
            return singleObject;
        } catch (FieldValidationException e) {
          return string;
        }
      } else {
        orderableField.populateParamsFromString(values, string, issue);
        return orderableField.getValueFromParams(values);
      }
    }
    else
      return string;
  }
}
