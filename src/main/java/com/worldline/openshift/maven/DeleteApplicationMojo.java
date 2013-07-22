package com.worldline.openshift.maven;

import com.openshift.client.IApplication;
import com.openshift.client.IOpenShiftConnection;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Delete an application.
 */
@Mojo(name = "delete-application")
public class DeleteApplicationMojo extends BaseApplicationMojo {
    @Override
    protected void doExecute(final IOpenShiftConnection connection, final IApplication app) throws MojoExecutionException {
        app.destroy();
        getLog().info("Destroyed application '" + app.getName() + "'");
    }
}
