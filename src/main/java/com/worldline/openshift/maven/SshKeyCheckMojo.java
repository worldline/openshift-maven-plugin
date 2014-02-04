package com.worldline.openshift.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.openshift.client.IOpenShiftConnection;
import com.openshift.client.IOpenShiftSSHKey;
import com.openshift.client.IUser;

/**
 * check an ssh keys by name
 * and add it if she's missing
 */
@Mojo(name = "sshkeycheck")
public class SshKeyCheckMojo extends BaseSshkeyMojo {
    /**
     * ssh key name
     */
    @Parameter(property = PREFIX + "sshkeyname", required = true)
    protected String sshkeyname;
    
	/**
	 * ssh public key file path (required)
	 */
	@Parameter(property = PREFIX + "sshpubkeyfile", required = true)
	protected String sshpubkeyfile;    

    public String getSshkeyname() {
		return sshkeyname;
	}

	public void setSshkeyname(String sshkeyname) {
		this.sshkeyname = sshkeyname;
	}
	public String getSshpubkeyfile() {
		return sshpubkeyfile;
	}
	public void setSshpubkeyfile(String sshpubkeyfile) {
		this.sshpubkeyfile = sshpubkeyfile;
	}


	@Override
    public void doExecute(final IOpenShiftConnection connection) throws MojoExecutionException {
		String sshKeyName = getSshkeyname();
        if (sshKeyName == null) {
            throw new MojoExecutionException("sshkeyname can't be null");
        }
		String keypub = getSshpubkeyfile();

        // get openshift user
        IUser openshiftUser = connection.getUser();

    	IOpenShiftSSHKey thekey = openshiftUser.getSSHKeyByName(sshKeyName);
    	if (thekey != null) {
    		getLog().info(sshKeyName + " already exists for the user " + openshiftUser.getRhlogin());
    		return;
    	}
    	getLog().info(sshKeyName + " will be added for the user " + openshiftUser.getRhlogin());
    	addSshKeyForOpenshiftUser(sshKeyName, keypub, openshiftUser);
    }
}