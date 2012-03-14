package com.googlecode.jsu.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public abstract class AbstractVisitor {
    public abstract Class<? extends Annotation> getAnnotation();

    public void visitField(Object source, Field field, Annotation annotation) {
    }
}
