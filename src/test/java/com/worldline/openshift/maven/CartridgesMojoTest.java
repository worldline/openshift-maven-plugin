package com.worldline.openshift.maven;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.openshift.client.IOpenShiftConnection;

@RunWith(JUnit4.class)
public class CartridgesMojoTest {
	// mock OS connection
	IOpenShiftConnection openShiftConnection = mock(IOpenShiftConnection.class);

	// spy mojo
	CartridgesMojo mojo = spy(new CartridgesMojo());

	@Before
	public void setup() {
		doReturn(openShiftConnection).when(mojo).getOpenShiftConnection();
	}

	@Test
	public void should_get_standalone_and_embeddable_cartridges() {
		try {
			mojo.execute();
			
			verify(openShiftConnection).getStandaloneCartridges();
			verify(openShiftConnection).getEmbeddableCartridges();
		} catch (MojoExecutionException e) {
			e.printStackTrace();
		} catch (MojoFailureException e) {
			e.printStackTrace();
		}
	}
}
