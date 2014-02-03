package com.worldline.openshift.maven;

import static org.mockito.Mockito.spy;

import org.junit.Assume;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openshift.client.IOpenShiftConnection;
import com.openshift.client.IUser;
import com.openshift.client.OpenShiftConnectionFactory;

/**
 * common base integration tests
 * pre-requisites : cf ::baseItNotice()
 */
public class BaseIT {
	// logguer
	protected static Logger LOG = LoggerFactory.getLogger(BaseIT.class);	
	
	// spy OS connection
	IOpenShiftConnection openShiftConnection = spy(getOpenShiftConnection());
	// spy OS user
	IUser openshiftUser = spy(openShiftConnection.getUser());

	// mini how to
	public static String baseItNotice() {
		StringBuilder sb = new StringBuilder();
		sb.append("To run openshift Integration Tests, you should set the following systm properties :\n")
		  .append("- openshift.serverUrl (required)\n")
		  .append("- openshift.user      (default: demo)\n")
	      .append("- openshift.password  (default: demo)\n");
		return sb.toString();
	}
	
	// log helper for OS connection
	public static String getOpenshiftConnectionInfo(IOpenShiftConnection openShiftConnection) {
		String login = openShiftConnection.getUser() != null ? openShiftConnection.getUser().getRhlogin() : "(null)"; 
		String server = openShiftConnection.getServer();
		return login + "@" + server;
	}

	/**
	 * construct a local ITdedicated openshift connection
	 * @return
	 */
    protected IOpenShiftConnection getOpenShiftConnection() {
    	final String OS_PREFIX = BaseOpenshift.PREFIX;
    	final String SERVER_URL_KEY = OS_PREFIX + "serverUrl";
    	String systUsr = System.getProperty(OS_PREFIX + "user");
    	String systPwd = System.getProperty(OS_PREFIX + "password");
    	String systSvr = System.getProperty(SERVER_URL_KEY);
    	boolean assumeItTests = (systSvr != null);
    	if (!assumeItTests) {
    		LOG.warn(baseItNotice());
    	}
    	Assume.assumeTrue("skipping IT test (" + SERVER_URL_KEY + " is  null)", 
    					  assumeItTests);
    	
    	String clientId = "OpenShift";
    	String user = (systUsr != null ? systUsr : "demo");
    	String password = (systPwd != null ? systPwd : "demo");
    	String authKey = null;
    	String authVI = null;
        return new OpenShiftConnectionFactory()
        		.getConnection(clientId, user, password, authKey, authVI, systSvr);
    }
}
