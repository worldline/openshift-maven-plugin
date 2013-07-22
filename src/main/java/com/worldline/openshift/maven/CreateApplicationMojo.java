package com.worldline.openshift.maven;

import com.openshift.client.ApplicationScale;
import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.IOpenShiftConnection;
import com.openshift.client.IUser;
import com.openshift.internal.client.Cartridge;
import com.openshift.internal.client.GearProfile;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Create an application. If the domain doesn't exist it will be created.
 */
@Mojo(name = "create-application")
public class CreateApplicationMojo extends BaseApplicationMojo {
    /**
     * cartridge name
     */
    @Parameter(property = PREFIX + "cartridge", required = true)
    protected String cartridge;

    /**
     * NO_SCALE or SCALE
     */
    @Parameter(property = PREFIX + "scale", defaultValue = "NO_SCALE")
    protected String scale;

    /**
     * JUMBO, EXLARGE, LARGE, MEDIUM, MICRO, SMALL
     */
    @Parameter(property = PREFIX + "gearProfile", defaultValue = "SMALL")
    protected String gearProfile;

    @Override
    protected void doExecute(final IOpenShiftConnection connection) {
        final IUser user = connection.getUser();

        IDomain userDomain = user.getDomain(domain);
        if (userDomain == null) {
            userDomain = user.createDomain(domain);
            getLog().info("Created domain '" + userDomain.getId() + "'");
        }

        final IApplication app = userDomain.createApplication(application,
                                                              new Cartridge(cartridge),
                                                              ApplicationScale.valueOf(scale.toUpperCase()),
                                                              new GearProfile(gearProfile));
        getLog().info("Created application '" + app.getName() + "'");
    }

    @Override
    protected void doExecute(final IOpenShiftConnection connection, final IApplication application) throws MojoExecutionException {
        // no-op, not called since we override doExecute(IOpenShiftConnection)
    }
}
