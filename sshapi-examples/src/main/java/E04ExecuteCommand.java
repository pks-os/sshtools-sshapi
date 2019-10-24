import java.io.BufferedReader;
import java.io.InputStreamReader;

import net.sf.sshapi.Ssh;
import net.sf.sshapi.SshClient;
import net.sf.sshapi.SshCommand;
import net.sf.sshapi.util.ConsolePasswordAuthenticator;
import net.sf.sshapi.util.Util;

/**
 * This example demonstrates executing a command instead of a shell.
 */
public class E04ExecuteCommand {
	/**
	 * Entry point.
	 * 
	 * @param arg command line arguments
	 * @throws Exception
	 */
	public static void main(String[] arg) throws Exception {
		
		// Create the client using that configuration and connect
		try (SshClient client = Ssh.open(Util.promptConnectionSpec(), new ConsolePasswordAuthenticator())) {
			
			// Execute the command and read back its output
			try (SshCommand command = client.command("ls /etc")) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(command.getInputStream()));
				String line;
				while ((line = reader.readLine()) != null)
					System.out.println(line);
			}
		}
	}
}
