package com.worldline.openshift.maven;

import com.openshift.client.IApplication;
import com.openshift.client.IOpenShiftConnection;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * force an application to start
 */
@Mojo(name = "start")
public class StartApplicationMojo extends BaseApplicationMojo {
    @Override
    public void doExecute(final IOpenShiftConnection connection, final IApplication app) throws MojoExecutionException {
        app.start();
        getLog().info("Application started");
    }
}
