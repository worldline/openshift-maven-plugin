package com.worldline.openshift.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.Base64;
import org.codehaus.plexus.util.IOUtil;

import com.openshift.client.ConnectionBuilder;
import com.openshift.client.ConnectionBuilder.AbstractConnectionBuilder;
import com.openshift.client.ConnectionBuilder.CredentialsConnectionBuilder;
import com.openshift.client.IOpenShiftConnection;

public abstract class BaseOpenshift extends AbstractMojo {
	protected static final String PREFIX = "openshift.";

	/**
	 * user to use to log in, if not provided express.conf will be read
	 */
	@Parameter(property = PREFIX + "user") // should be required but can be
											// guessed from express.conf
	protected String user;

	/**
	 * password to log in (a good practise is to put it in settings.xml or as
	 * system property)
	 */
	@Parameter(property = PREFIX + "password")
	protected String password;

	/**
	 * openshift instance URL
	 */
	@Parameter(property = PREFIX + "serverUrl", defaultValue = "openshift.redhat.com")
	protected String serverUrl;

	@Parameter(property = PREFIX + "authKey")
	protected String authKey;

	@Parameter(property = PREFIX + "authVI")
	protected String authIV;

	@Parameter(property = PREFIX + "sslChecks", defaultValue = "false")
	protected boolean sslChecks;

	@Parameter(property = PREFIX + "proxyHost")
	protected String proxyHost;

	@Parameter(property = PREFIX + "proxyPort")
	protected String proxyPort;

	@Parameter(property = PREFIX + "proxySet")
	protected String proxySet;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		tryToGuessNotDefinedCredentials();
		try {
			try {
				ConnectionBuilder connectionBuilder = new ConnectionBuilder(serverUrl);
				AbstractConnectionBuilder abstractConnectionBuilder = null;
				if (user != null && password != null)
					abstractConnectionBuilder = connectionBuilder.credentials(user, password);
				else if (authIV != null && authKey != null)
					abstractConnectionBuilder = connectionBuilder.key(authIV, authKey);
				final IOpenShiftConnection connection = abstractConnectionBuilder.create();
				doExecute(connection);
			} catch (final IllegalArgumentException iae) {
				if (serverUrl.startsWith("http:")) {
					ConnectionBuilder connectionBuilder = new ConnectionBuilder(serverUrl.replace("http:", "https:"));
					CredentialsConnectionBuilder credentialsConnectionBuilder = connectionBuilder.credentials(user,
							password);
					final IOpenShiftConnection connection = credentialsConnectionBuilder.create();
					doExecute(connection);
				} else {
					throw iae;
				}
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	protected abstract void doExecute(IOpenShiftConnection connection) throws MojoExecutionException;

	protected void emptyLine() {
		getLog().info("");
	}

	protected void tryToGuessNotDefinedCredentials() throws MojoExecutionException {
		final File expressConfig = new File(System.getProperty("user.home"), ".openshift/express.conf");
		final Properties config = new Properties();
		if (expressConfig.exists()) {
			InputStream inputStream = null;
			try {
				inputStream = new FileInputStream(expressConfig);
				config.load(inputStream);
			} catch (final IOException e) {
				throw new MojoExecutionException(e.getMessage(), e);
			} finally {
				IOUtil.close(inputStream);
			}
		}

		if (user == null) {
			user = config.getProperty("default_rhlogin");
		}
		if (password == null) { // not in the default file but would be common
								// when using this plugin
			password = config.getProperty("maven_plugin_password");
			if (password != null) {
				password = new String(Base64.decodeBase64(password.getBytes()));
			}
		}
		if (serverUrl == null || "openshift.redhat.com".equals(serverUrl)) {
			serverUrl = config.getProperty("libra_server", serverUrl);
		}
	}
}
