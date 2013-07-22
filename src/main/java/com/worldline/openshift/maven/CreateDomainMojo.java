package com.worldline.openshift.maven;

import com.openshift.client.IDomain;
import com.openshift.client.IOpenShiftConnection;
import com.openshift.client.IUser;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Create a new domain.
 */
@Mojo(name = "create-domain")
public class CreateDomainMojo extends BaseOpenshift {
    /**
     * domain name
     */
    @Parameter(property = PREFIX + "domain", required = true)
    protected String domain;

    @Override
    protected void doExecute(final IOpenShiftConnection connection) {
        final IUser user = connection.getUser();
        final IDomain created = user.createDomain(domain);
        getLog().info("Created domain '" + created.getId() + "'");
    }
}
