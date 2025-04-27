package com.github.bazelbuild.rules_jvm_external.resolver.gradle;

import java.net.MalformedURLException;
import java.net.URI;

public class Repository {
    public final URI uri;
    public final boolean requiresAuth;
    public final String usernameProperty;
    public final String passwordProperty;
    private final String password;
    private final String username;

    public Repository(URI uri) {
        this(uri, false, null, null);
    }

    public Repository(URI uri, boolean requiresAuth, String username, String password) {
        this.uri = uri;
        this.requiresAuth = requiresAuth;
        String host = URI.create(getUrl()).getHost();
        this.username = username;
        this.password = password;
        this.usernameProperty = host + "UserName";
        this.passwordProperty = host + "Password";
    }

    public String getUsernameProperty() {
        return this.usernameProperty;
    }

    public String getPasswordProperty() {
        return this.passwordProperty;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    public String getUrl() {
        try {
            return uri.toURL().toString();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
