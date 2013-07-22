package com.worldline.openshift.maven;

import com.openshift.client.ICartridge;
import com.openshift.client.IEmbeddableCartridge;
import com.openshift.client.IOpenShiftConnection;
import org.apache.maven.plugins.annotations.Mojo;

import java.util.List;

/**
 * list all available cartridges
 */
@Mojo(name = "cartridges")
public class CartridgesMojo extends BaseOpenshift {
    @Override
    public void doExecute(final IOpenShiftConnection connection) {
        final List<ICartridge> cartridges = connection.getStandaloneCartridges();
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
