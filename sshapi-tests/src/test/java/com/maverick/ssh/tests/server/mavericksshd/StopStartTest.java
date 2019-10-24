package com.maverick.ssh.tests.server.mavericksshd;

import java.io.IOException;

import org.junit.Test;

import com.maverick.ssh.tests.client.AbstractClientMultipleConnected;

import net.sf.sshapi.SshClient;
import net.sf.sshapi.sftp.SftpClient;

public class StopStartTest extends AbstractClientMultipleConnected {

	@Test
	public void stopStartTest() throws Exception {
		
		for(int i=0;i<10;i++) {
			disconnect();
			server.restart();
			connect();
			
		}
	}
	
	@Override
	protected void onConnectingSetUp() throws net.sf.sshapi.SshException, IOException {
		super.onConnectingSetUp();
		
		for(SshClient ssh : clients) {
			try(SftpClient sftp = ssh.sftp()) {
				sftp.ls(sftp.getDefaultPath());
			}
		}
	}

	@Override
	public int getConnectionCount() {
		return 100;
	}
}
