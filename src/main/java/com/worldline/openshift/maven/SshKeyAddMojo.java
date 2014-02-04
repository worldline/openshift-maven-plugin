package com.worldline.openshift.maven;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.openshift.client.IOpenShiftConnection;
import com.openshift.client.IUser;
import com.openshift.client.SSHPublicKey;

/**
 * add an ssh keys
 */
@Mojo(name = "sshkeyadd")
public class SshKeyAddMojo extends BaseSshkeyMojo {
	/**
	 * ssh key name (required)
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
	public String getSshpubkeyfile() {
		return sshpubkeyfile;
	}
	public void setSshpubkeyfile(String sshpubkeyfile) {
		this.sshpubkeyfile = sshpubkeyfile;
	}
	public void setSshkeyname(String sshkeyname) {
		this.sshkeyname = sshkeyname;
	}

	@Override
	public void doExecute(final IOpenShiftConnection connection)
			throws MojoExecutionException {
		String keyname = getSshkeyname();
		if (keyname == null) {
			throw new MojoExecutionException(
					"sshkeyname can't be null");
		}
		String keypub = getSshpubkeyfile();
		// get openshift user
		IUser openshiftUser = connection.getUser();
		// add an ssh key
		addSshKeyForOpenshiftUser(keyname, keypub, openshiftUser);
	}

}