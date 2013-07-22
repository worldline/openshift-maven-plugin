package com.worldline.openshift.maven;

import com.openshift.client.IApplication;
import com.openshift.client.IOpenShiftConnection;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * print all information about an application
 */
@Mojo(name = "application")
public class ApplicationMojo extends BaseApplicationMojo {
    @Override
    public void doExecute(final IOpenShiftConnection connection, final IApplication app) throws MojoExecutionException {
        dumpApplication("", app, app.getEmbeddedCartridges());
        emptyLine();
    }
}
