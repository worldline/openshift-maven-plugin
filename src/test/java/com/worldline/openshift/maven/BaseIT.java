package com.worldline.openshift.maven;

import static org.mockito.Mockito.spy;

import org.junit.Assume;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openshift.client.IOpenShiftConnection;
import com.openshift.client.IUser;
import com.openshift.client.OpenShiftConnectionFactory;

/**
 * [common base] integration tests
 * 
 * To skip ITs : 
 * 	add " -DskipITs=true" to the maven command line
 * To run openshift Integration Tests :
 * 	set the following sysem properties :
 * 	-Dopenshift.serverUrl (required)
 *  -Dopenshift.user      (default: demo)
 *  -Dopenshift.password  (default: demo)
 */
public class BaseIT {
	// logguer
	protected static Logger LOG = LoggerFactory.getLogger(BaseIT.class);	
	
	// spy OS connection
	IOpenShiftConnection openShiftConnection = spy(getOpenShiftConnection());
	// spy OS user
	IUser openshiftUser = spy(openShiftConnection.getUser());

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
    		LOG.info(" *WARNING* : skip IT test ('{}' is  null)", SERVER_URL_KEY);
    	}
    	Assume.assumeTrue(assumeItTests);
    	
    	String clientId = "OpenShift";
    	String user = (systUsr != null ? systUsr : "demo");
    	String password = (systPwd != null ? systPwd : "demo");
    	String authKey = null;
    	String authVI = null;
        IOpenShiftConnection osConnection = new OpenShiftConnectionFactory()
        		.getConnection(clientId, user, password, authKey, authVI, systSvr);
    	LOG.debug("using openshift connection : {}", 
    			  getOpenshiftConnectionInfo(osConnection));
		return osConnection;
    }
}
