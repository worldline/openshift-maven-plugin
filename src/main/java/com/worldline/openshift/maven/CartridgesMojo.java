package com.worldline.openshift.maven;

import java.util.List;

import org.apache.maven.plugins.annotations.Mojo;

import com.openshift.client.IOpenShiftConnection;
import com.openshift.client.cartridge.ICartridge;
import com.openshift.client.cartridge.IEmbeddableCartridge;

/**
 * list all available cartridges
 */
@Mojo(name = "cartridges")
public class CartridgesMojo extends BaseOpenshift {
	@Override
	public void doExecute(final IOpenShiftConnection connection) {
		final List<ICartridge> cartridges = connection.getCartridges();
		final List<IEmbeddableCartridge> embeddableCartridges = connection.getEmbeddableCartridges();

		emptyLine();
		getLog().info("Standalone cartridges:");
		emptyLine();
		for (final ICartridge cartridge : cartridges) {
			getLog().info(cartridge.getName());
		}
		emptyLine();

		emptyLine();
		getLog().info("Embeddable cartridges:");
		emptyLine();
		for (final IEmbeddableCartridge cartridge : embeddableCartridges) {
			getLog().info(cartridge.getName());
		}
		emptyLine();
	}
}
