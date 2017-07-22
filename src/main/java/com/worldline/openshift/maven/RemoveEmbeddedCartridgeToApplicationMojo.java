package com.worldline.openshift.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

import com.openshift.client.IApplication;
import com.openshift.client.IOpenShiftConnection;
import com.openshift.client.cartridge.EmbeddableCartridge;

/**
 * Remove an embedded cartridge to an existing application
 */
@Mojo(name = "remove-cartridge")
public class RemoveEmbeddedCartridgeToApplicationMojo extends EmbeddedCartridgeApplicationMojo {
	@Override
	protected void doExecute(final IOpenShiftConnection connection, final IApplication app)
			throws MojoExecutionException {
		app.removeEmbeddedCartridge(new EmbeddableCartridge(embeddedCartridge));
		getLog().info("Added cartridge '" + embeddedCartridge + "' to application '" + app.getName() + "'");
	}
}
