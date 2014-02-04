package com.worldline.openshift.maven;

import static org.mockito.Mockito.*;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(JUnit4.class)
public class SshKeyListMojoIT extends BaseIT {
	protected static Logger LOG = LoggerFactory.getLogger(SshKeyListMojoIT.class);
	
	// spy mojo
	SshKeyListMojo mojo = spy(new SshKeyListMojo());
	
    @Before
	public void setup() {
    	
		String sshkeyName = "*";
		// mock sshkeyname parameter
		doReturn("*").when(mojo).getSshkeyname();
		// spy openshift connection
		doReturn(openShiftConnection).when(mojo).getOpenShiftConnection();
		// spy openshift user
		doReturn(openshiftUser).when(openShiftConnection).getUser();

		LOG.debug("setup OK sshkeyname={}", sshkeyName);
	}
    
	@Test
	public void testDoExecute() {
		try {
			mojo.execute();
			// openshiftUser = connection.getSSHKeys();
			verify(openShiftConnection, atLeast(1)).getUser();
			verify(openshiftUser).getSSHKeys();
		} catch (MojoExecutionException e) {
			e.printStackTrace();
		} catch (MojoFailureException e) {
			e.printStackTrace();
		}
	}
}
