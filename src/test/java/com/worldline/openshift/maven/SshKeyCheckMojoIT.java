package com.worldline.openshift.maven;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openshift.client.ISSHPublicKey;

@RunWith(JUnit4.class)
public class SshKeyCheckMojoIT extends BaseIT {
	protected static Logger LOG = LoggerFactory.getLogger(SshKeyCheckMojoIT.class);		
	SshKeyCheckMojo mojo = spy(new SshKeyCheckMojo());
	
	String sshKeyName = "test_sshkeyadd";
	String curPath = SshKeyCheckMojoIT.class.getProtectionDomain().getCodeSource().getLocation().getPath();
	
	String sep = java.io.File.pathSeparator;
	String sshPublicKeyFilepath = curPath + "fake_test_key.pub";
	
	@Before
	public void setup() {
    	
		// mock sshkeyname parameter
		doReturn(sshKeyName).when(mojo).getSshkeyname();
		// mock sshkey public file path
		doReturn(sshPublicKeyFilepath).when(mojo).getSshpubkeyfile();
		// spy openshift connection
		doReturn(openShiftConnection).when(mojo).getOpenShiftConnection();
		// spy openshift user
		doReturn(openshiftUser).when(openShiftConnection).getUser();

		LOG.info("setup OK sshkeyname:filepath={}", sshKeyName + ":" + sshPublicKeyFilepath);
	}

    @After
    public void after() {
    	LOG.info("after : remove sshkeyname: " + sshKeyName); 
    	openshiftUser.deleteKey(sshKeyName);
    }

	@Test
	public void testDoExecute() {
		try {
			LOG.info("first execution : the key should be added");
			
			mojo.execute();
			verify(openShiftConnection, atLeast(1)).getUser();
			verify(openshiftUser, atLeast(1)).getSSHKeyByName(sshKeyName);
			verify(openshiftUser).putSSHKey(anyString(), any(ISSHPublicKey.class));
			
			LOG.info("second execution : the key should not be added but checked only");

			mojo.execute();
			verify(openShiftConnection, atLeast(1)).getUser();
			verify(openshiftUser, atLeast(1)).getSSHKeyByName(sshKeyName);
		} catch (MojoExecutionException e) {
			e.printStackTrace();
		} catch (MojoFailureException e) {
			e.printStackTrace();
		}
	}
}
