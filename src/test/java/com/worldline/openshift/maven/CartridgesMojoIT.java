package com.worldline.openshift.maven;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.openshift.client.IOpenShiftConnection;

@RunWith(JUnit4.class)
public class CartridgesMojoIT extends BaseIT {
	CartridgesMojo mojo = spy(new CartridgesMojo());

	@Before
	public void setup() {
		IOpenShiftConnection openShiftConnection = getOpenShiftConnection();
		doReturn(openShiftConnection).when(mojo).getOpenShiftConnection();
	}

	@Test
	public void testDoExecute() {
		try {
			mojo.execute();
		} catch (MojoExecutionException e) {
			e.printStackTrace();
		} catch (MojoFailureException e) {
			e.printStackTrace();
		}
	}
}
