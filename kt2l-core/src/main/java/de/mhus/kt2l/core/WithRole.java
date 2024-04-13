package de.mhus.kt2l.core;

import de.mhus.kt2l.config.UsersConfiguration;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface WithRole {
    UsersConfiguration.ROLE[] value();
}
