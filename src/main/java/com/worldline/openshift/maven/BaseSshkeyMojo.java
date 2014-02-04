package com.worldline.openshift.maven;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;

import com.openshift.client.IUser;
import com.openshift.client.SSHPublicKey;

public abstract class BaseSshkeyMojo extends BaseOpenshift {
	
	abstract public String getSshkeyname();
	abstract public void setSshkeyname(String sshkeyname);

	
	protected void addSshKeyForOpenshiftUser(String keyname, String keypub,
			IUser openshiftUser) throws MojoExecutionException {
		if (keypub == null) {
			throw new MojoExecutionException(
					"sshpubkeyfile can't be null");
		}
		File pubKeyFile = new File(keypub);
		if (!pubKeyFile.exists() || !pubKeyFile.canRead()) {
			throw new MojoExecutionException(
					keypub + " does'nt exists or not readable !");
		}


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
					+ "\n" + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
		}
		emptyLine();
		getLog().info("key " + keyname + " added");
		emptyLine();
	}

}
