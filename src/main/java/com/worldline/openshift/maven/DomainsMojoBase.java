package com.worldline.openshift.maven;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.IOpenShiftConnection;
import com.openshift.client.cartridge.IEmbeddedCartridge;

/**
 * list all defined domains and applications
 */
@Mojo(name = "domains")
public class DomainsMojoBase extends BaseApplicationMojo {
	@Override
	public void doExecute(final IOpenShiftConnection connection) {
		final List<IDomain> domains = connection.getDomains();
		final Map<IDomain, List<IApplication>> applicationByDomains = new HashMap<IDomain, List<IApplication>>();
		final Map<IApplication, List<IEmbeddedCartridge>> embeddedCartridgesByApp = new HashMap<IApplication, List<IEmbeddedCartridge>>();

		for (final IDomain domain : domains) {
			final List<IApplication> applications = domain.getApplications();
			applicationByDomains.put(domain, applications);
			for (final IApplication app : applications) {
				embeddedCartridgesByApp.put(app, app.getEmbeddedCartridges());
			}
		}

		emptyLine();
		getLog().info("Standalone domains:");
		emptyLine();
		for (final IDomain domain : domains) {
			getLog().info("Id: " + domain.getId());
			getLog().info("Suffix: " + domain.getSuffix());
			for (final IApplication application : applicationByDomains.get(domain)) {
				dumpApplication(SPACE + SPACE, application, embeddedCartridgesByApp.get(application));
				emptyLine();
			}
			emptyLine();
		}
	}

	@Override
	protected void doExecute(final IOpenShiftConnection connection, final IApplication application)
			throws MojoExecutionException {
		// no-op: not called since we override doExecute(IOpenShiftConnection)
	}
}
