package com.github.bazelbuild.rules_jvm_external.resolver.gradle;

public class Repository {
    public final String url;
    public final boolean requiresAuth;
    public final String usernameProperty;
    public final String passwordProperty;

    public Repository(String url) {
        this(url, false, null, null);
    }

    public Repository(String url, boolean requiresAuth, String usernameProperty, String passwordProperty) {
        this.url = url;
        this.requiresAuth = requiresAuth;
        this.usernameProperty = usernameProperty;
        this.passwordProperty = passwordProperty;
    }
}
