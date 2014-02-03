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
public class SshKeyAddMojo extends BaseOpenshift {
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
		if (keypub == null) {
			throw new MojoExecutionException(
					"sshpubkeyfile can't be null");
		}
		File pubKeyFile = new File(keypub);
		if (!pubKeyFile.exists() || !pubKeyFile.canRead()) {
			throw new MojoExecutionException(
					keypub + " does'nt exists or not readable !");
		}

		// get openshift user
		IUser openshiftUser = connection.getUser();

		StringBuffer sbSshAdd = new StringBuffer();
		sbSshAdd.append("Add ssh keys for openshiftUser ")
				.append(openshiftUser.getRhlogin())
				.append(" sshkey name:").append(keyname)
				.append(" path:").append(keypub);
		emptyLine();
		getLog().info(sbSshAdd.toString());
		try {
			openshiftUser.putSSHKey(keyname, new SSHPublicKey(pubKeyFile));
		} catch (Exception e) {
			throw new MojoExecutionException(
					"Unable to add public ssh key " + keyname 
					+ e.getClass().getSimpleName() + " : " + e.getMessage(), e);
		}
		emptyLine();
		getLog().info("key " + keyname + " added");
		emptyLine();
	}
}