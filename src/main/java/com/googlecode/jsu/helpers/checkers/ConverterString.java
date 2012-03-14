package com.googlecode.jsu.helpers.checkers;

import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.customfields.option.Option;
import org.apache.commons.lang.StringUtils;
import org.ofbiz.core.entity.GenericValue;

import com.atlassian.jira.issue.IssueConstant;
import com.atlassian.jira.project.Project;

import java.util.Collection;

/**
 * @author <A href="mailto:abashev at gmail dot com">Alexey Abashev</A>
 * @version $Id$
 */
class ConverterString implements ValueConverter {
    /* (non-Javadoc)
     * @see com.googlecode.jsu.helpers.checkers.ValueConverter#getComparable(java.lang.Object)
     */
    public Comparable<?> getComparable(Object object) {
        if (object == null) {
            return null;
        }

        String result;

        if (object instanceof IssueConstant) {
            result = ((IssueConstant) object).getName();
        } else if (object instanceof User) {
            result = ((User) object).getName();
        } else if (object instanceof Group) {
            result = ((Group) object).getName();
        } else if (object instanceof Project) {
            result = ((Project) object).getKey();
        } else if (object instanceof GenericValue) {
            final GenericValue gv = (GenericValue) object;

            result = gv.getString("name");
            if (result == null) {
                result = object.toString();
            }
        } else if (object instanceof Option) {
            result = ((Option) object).getValue();
        } else if (object instanceof com.atlassian.jira.issue.fields.option.Option) {
            result = ((com.atlassian.jira.issue.fields.option.Option) object).getName();
        } else if (object instanceof Collection && ((Collection) object).size() == 1) {
            result = (String) this.getComparable(((Collection) object).iterator().next());
        } else {
            result = object.toString();
        }

        if (StringUtils.isBlank(result)) {
            return null;
        }

        return result;
    }
}
