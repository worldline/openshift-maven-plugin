package com.worldline.openshift.maven;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.openshift.client.IOpenShiftConnection;
import com.openshift.client.IOpenShiftSSHKey;
import com.openshift.client.ISSHPublicKey;
import com.openshift.client.IUser;

/**
 * list all available ssh keys
 */
@Mojo(name = "sshkeylist")
public class SshKeyListMojo extends BaseSshkeyMojo {
    /**
     * ssh key name (default='*' to list all available keys)
     */
    @Parameter(property = PREFIX + "sshkeyname", required = true, defaultValue = "*")
    protected String sshkeyname;

    public String getSshkeyname() {
		return sshkeyname;
	}

	public void setSshkeyname(String sshkeyname) {
		this.sshkeyname = sshkeyname;
	}


	@Override
    public void doExecute(final IOpenShiftConnection connection) throws MojoExecutionException {
		String filter = getSshkeyname();
        if (filter == null) {
            throw new MojoExecutionException("sshkeyname can't be null (use '*' to get all keys)");
        }

        // get openshift user
        IUser openshiftUser = connection.getUser();
        // list openshift user ssh keys
        List<IOpenShiftSSHKey> sshKeys = getOpenshiftUserSshKeys(filter, openshiftUser);
        displayOpenshiftUserSshKeys(filter, openshiftUser, sshKeys);
    }

	private List<IOpenShiftSSHKey> getOpenshiftUserSshKeys(String filter,
			IUser openshiftUser) {
		List<IOpenShiftSSHKey> sshKeys = null;
        if ("*".equals(filter)) {
            // * : get all openshift user ssh keys
        	sshKeys = openshiftUser.getSSHKeys();
        } else {
        	// name : get openshift user ssh keys by name
        	sshKeys = new ArrayList<IOpenShiftSSHKey>();
        	IOpenShiftSSHKey thekey = openshiftUser.getSSHKeyByName(filter);
        	if (thekey != null) {
        		sshKeys.add(thekey);
        	}
        }
		return sshKeys;
	}

	private void displayOpenshiftUserSshKeys(String filter,
			IUser openshiftUser, List<IOpenShiftSSHKey> sshKeys) {
		String logStr = "SSh keys for openshiftUser " 
  			  + openshiftUser.getRhlogin() 
  			  + " (sshkeyname:" + filter + ") :";
        emptyLine();
        getLog().info(logStr);
        emptyLine();
        int i=0;
        for (final ISSHPublicKey pubKey : sshKeys) {
        	printPubKey(pubKey, i++);
        }
        emptyLine();
	}
	
	private void printPubKey(final ISSHPublicKey pubKey, int index) {
		StringBuffer sb = new StringBuffer();
		sb.append(" key n-").append(index);
		sb.append(" (type: ").append(pubKey.getKeyType())
		  .append(") extract : ")
		  .append(pubKey.getPublicKey().substring(0, 15))
		  .append("...");
    	getLog().info(sb.toString());
	}
}