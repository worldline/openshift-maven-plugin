package com.worldline.openshift.maven;

import com.openshift.client.IApplication;
import com.openshift.client.IOpenShiftConnection;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * force an application to scale up
 */
@Mojo(name = "scale-up")
public class ScaleUpApplicationMojo extends BaseApplicationMojo {
    @Override
    public void doExecute(final IOpenShiftConnection connection, final IApplication app) throws MojoExecutionException {
        app.scaleUp();
        getLog().info("Application started");
    }
}
