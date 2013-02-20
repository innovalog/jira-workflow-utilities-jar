package com.googlecode.jsu.helpers.checkers;

import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.usercompatibility.UserCompatibilityHelper;
import org.apache.commons.lang.StringUtils;
import org.ofbiz.core.entity.GenericEntity;

import com.atlassian.jira.issue.IssueConstant;
import com.atlassian.jira.project.Project;


import java.lang.reflect.Method;
import java.util.Collection;

/**
 * @author <A href="mailto:abashev at gmail dot com">Alexey Abashev</A>
 * @version $Id$
 */
public class ConverterString implements ValueConverter {
    /* (non-Javadoc)
     * @see com.googlecode.jsu.helpers.checkers.ValueConverter#getComparable(java.lang.Object)
     */
    public Comparable<?> getComparable(Object object) {
        if (object == null) {
            return null;
        }

        String result = convert(object);

        if (StringUtils.isBlank(result)) {
            return null;
        }

        return result;
    }


    public String convert(Object value) {
        if (value == null || value instanceof String) {
            return (String) value;
        } else if (value instanceof IssueConstant) {
            return ((IssueConstant) value).getName();
        } else if (value instanceof Project) {
            return ((Project)value).getKey();
        } else if (value instanceof Collection && ((Collection) value).size() == 1) {
            return convert(((Collection) value).iterator().next());
        } else if (value instanceof Option) {
            return ((Option) value).getValue();
        } else if (value instanceof com.atlassian.jira.issue.fields.option.Option) {
            return ((com.atlassian.jira.issue.fields.option.Option) value).getName();
        } else if (UserCompatibilityHelper.isUserObject(value)) {
          return UserCompatibilityHelper.convertUserObject(value).getKey();
        } else if (value instanceof Group) {
            return ((Group) value).getName();
        } else if (value instanceof GenericEntity) {
            String s = ((GenericEntity) value).getString("name");
            if (StringUtils.isEmpty(s)) {
                s = ((GenericEntity) value).getString("id");
                if (StringUtils.isEmpty(s)) {
                    s = value.toString();
                }
            }
            return s;
        } else {
            try {
                Method getName = value.getClass().getMethod("getName");
                return getName.invoke(value).toString();
            } catch (Exception e) { /* try getId() ... */ }
            try {
                Method getId = value.getClass().getMethod("getId");
                return getId.invoke(value).toString();
            } catch (Exception e) { /* use toString() ... */ }
            return value.toString();
        }
    }
}
