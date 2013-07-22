package com.worldline.openshift.maven;

import com.openshift.client.IApplication;
import com.openshift.client.IOpenShiftConnection;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * for an application to stop
 */
@Mojo(name = "stop")
public class StopApplicationMojo extends BaseApplicationMojo {
    @Override
    public void doExecute(final IOpenShiftConnection connection, final IApplication app) throws MojoExecutionException {
        app.stop(true);
        getLog().info("Application stopped");
    }
}
