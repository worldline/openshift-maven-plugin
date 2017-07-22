package com.worldline.openshift.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

import com.openshift.client.IApplication;
import com.openshift.client.IOpenShiftConnection;

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
