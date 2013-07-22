package com.worldline.openshift.maven;

import org.apache.maven.plugins.annotations.Parameter;

public abstract class EmbeddedCartridgeApplicationMojo extends BaseApplicationMojo {
    /**
     * embedded cartridge
     */
    @Parameter(property = PREFIX + "embedded-cartridge", required = true)
    protected String embeddedCartridge;
}
