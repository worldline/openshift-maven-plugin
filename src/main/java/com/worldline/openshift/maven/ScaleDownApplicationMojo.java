package com.worldline.openshift.maven;

import com.openshift.client.IApplication;
import com.openshift.client.IOpenShiftConnection;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * force an application to scale down
 */
@Mojo(name = "scale-down")
public class ScaleDownApplicationMojo extends BaseApplicationMojo {
    @Override
    public void doExecute(final IOpenShiftConnection connection, final IApplication app) throws MojoExecutionException {
        app.scaleDown();
        getLog().info("Application started");
    }
}
