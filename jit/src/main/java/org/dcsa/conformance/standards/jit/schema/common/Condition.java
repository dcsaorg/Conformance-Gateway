package org.dcsa.conformance.standards.jit.schema.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Condition {
  String[] description() default "";

  String[] oneOf() default "";

  String[] anyOf() default "";

  String[] mandatory() default "";
}
