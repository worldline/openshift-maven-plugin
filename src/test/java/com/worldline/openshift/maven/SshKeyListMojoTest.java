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

import com.openshift.client.IOpenShiftConnection;
import com.openshift.client.IUser;

@RunWith(JUnit4.class)
public class SshKeyListMojoTest {
	protected static Logger LOG = LoggerFactory.getLogger(SshKeyListMojoTest.class);		
	
	// mock OS connection
	IOpenShiftConnection openShiftConnection = mock(IOpenShiftConnection.class);
	// mock OS user
	IUser openshiftUser = mock(IUser.class);
	
	// spy mojo
	SshKeyListMojo mojo = spy(new SshKeyListMojo());
	
    @Before
	public void setup() {
		// spy openshift connection
		doReturn(openShiftConnection).when(mojo).getOpenShiftConnection();
		// spy openshift user
		doReturn(openshiftUser).when(openShiftConnection).getUser();
		doReturn("OS_TESTU").when(openshiftUser).getRhlogin();
	}
    
	@Test
	public void should_list_all_sshkeys_when_asterisk_is_used() {
		// GIVEN
		String sshkeyName = "*";
		// mock sshkeyname parameter
		doReturn(sshkeyName).when(mojo).getSshkeyname();
		
		try {
			// WHEN
			mojo.execute();
			// THEN
			verify(openShiftConnection, atLeast(1)).getUser();
			verify(openshiftUser).getSSHKeys();
		} catch (MojoExecutionException e) {
			e.printStackTrace();
		} catch (MojoFailureException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void should_get_one_sshkey_when_litteral_name_is_used() {
		// GIVEN
		String sshkeyName = "my_ssh_key";
		// mock sshkeyname parameter
		doReturn(sshkeyName).when(mojo).getSshkeyname();
		
		try {
			// WHEN
			mojo.execute();
			// THEN
			verify(openShiftConnection, atLeast(1)).getUser();
			verify(openshiftUser).getSSHKeyByName(sshkeyName);
		} catch (MojoExecutionException e) {
			e.printStackTrace();
		} catch (MojoFailureException e) {
			e.printStackTrace();
		}
	}
}
