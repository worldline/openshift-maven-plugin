package com.worldline.openshift.maven;

import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.IEmbeddedCartridge;
import com.openshift.client.IOpenShiftConnection;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.List;

public abstract class BaseApplicationMojo extends BaseOpenshift {
    protected static final String SPACE = "  ";

    /**
     * domain name
     */
    @Parameter(property = PREFIX + "domain", required = true)
    protected String domain;

    /**
     * application name
     */
    @Parameter(property = PREFIX + "application", required = true)
    protected String application;

    @Override
    protected void doExecute(final IOpenShiftConnection connection) throws MojoExecutionException {
        doExecute(connection, findApplication(connection));
    }

    protected abstract void doExecute(IOpenShiftConnection connection, IApplication application) throws MojoExecutionException;

    protected IApplication findApplication(final IOpenShiftConnection connection) throws MojoExecutionException {
        final IDomain userDomain = connection.getUser().getDomain(domain);
        if (userDomain == null) {
            throw new MojoExecutionException("Can't find domain'" + domain + "'");
        }

        final IApplication app = userDomain.getApplicationByName(application);
        if (app == null) {
            throw new MojoExecutionException("Can't find application'" + application + "'");
        }
        return app;
    }

    protected void dumpApplication(final String prefix, final IApplication application, final List<IEmbeddedCartridge> embeddedCartridgesByApp) {
        getLog().info(prefix + "Application: " + application.getName());
        getLog().info(prefix + SPACE + "Url: " + application.getApplicationUrl());
        getLog().info(prefix + SPACE + "Git: " + application.getGitUrl());
        getLog().info(prefix + SPACE + "Aliases: " + application.getAliases());
        getLog().info(prefix + SPACE + "Scale: " + application.getApplicationScale().name());
        getLog().info(prefix + SPACE + "Cartridge: " + application.getCartridge().getName());
        getLog().info(prefix + SPACE + "Created: " + application.getCreationTime());
        getLog().info(prefix + SPACE + "Embedded cartridges:");
        for (final IEmbeddedCartridge embeddedCartridge : embeddedCartridgesByApp) {
            getLog().info(prefix + SPACE + SPACE + " " + embeddedCartridge.getName());
        }
    }
}
