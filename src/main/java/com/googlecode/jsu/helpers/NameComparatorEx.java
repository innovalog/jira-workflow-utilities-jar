package com.googlecode.jsu.helpers;

import java.util.Comparator;

import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.util.I18nHelper;

/**
 * @author Gustavo Martin
 *
 * This Comparator is used to compare two fields by its internationalized name.
 *
 */
public class NameComparatorEx implements Comparator<Field> {
    private final I18nHelper i18nHelper;

    public NameComparatorEx(I18nHelper i18nHelper) {
        this.i18nHelper = i18nHelper;
    }

    public int compare(Field o1, Field o2) {
        if (o1 == null)
            throw new IllegalArgumentException("The first parameter is null");
        if (o2 == null)
            throw new IllegalArgumentException("The second parameter is null");

        String name1 = i18nHelper.getText(o1.getName());
        String name2 = i18nHelper.getText(o2.getName());

        int result = name1.compareTo(name2);
        if (result == 0)
            return o1.getId().compareTo(o2.getId());
        else
            return result;
    }
}
