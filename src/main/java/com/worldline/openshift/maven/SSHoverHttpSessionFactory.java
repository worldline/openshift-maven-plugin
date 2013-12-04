package com.worldline.openshift.maven;

import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.JschSession;
import org.eclipse.jgit.transport.RemoteSession;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.FS;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.ProxyHTTP;
import com.jcraft.jsch.Session;

public class SSHoverHttpSessionFactory extends SshSessionFactory {

   private JSch j;
   
   private String proxyHost;
   private int    proxyPort;

    public SSHoverHttpSessionFactory(String sshHttpProxyHost, int sshHttpProxyPort)
    {
        this.j = new JSch();
        this.proxyHost = sshHttpProxyHost;
        this.proxyPort = sshHttpProxyPort;
    }

    public void Initialize()
    {
    	String sshKey = System.getProperty("user.home");
    	sshKey += "/.ssh/id_rsa";
        try {
			this.j.addIdentity(sshKey);
		} catch (JSchException e) {
			throw new RuntimeException("unable to set identity file" + sshKey);
		}
    	// unused // this.j.SetKnownHosts(@"C:/known_hosts");
    }		

	@Override
	public RemoteSession getSession(URIish uri,
			CredentialsProvider credentialsProvider, FS fs, int tms)
			throws org.eclipse.jgit.errors.TransportException {
		Session session;
    	java.util.Properties config = new java.util.Properties(); 
    	config.put("StrictHostKeyChecking", "no");
		try {
			session = this.j.getSession(uri.getUser(), uri.getHost());
			session.setProxy(new ProxyHTTP(this.proxyHost, this.proxyPort));
	    	session.setConfig(config);
			session.connect(); // ssh connect
		} catch (JSchException e) {
			throw new org.eclipse.jgit.errors.TransportException("error getting ssh session", e);
		}

        return new JschSession(session, uri);
	}
}