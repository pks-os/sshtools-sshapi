package net.sf.sshapi;

import java.io.IOException;
import java.io.InputStream;

/**
 * Extension of a {@link SshStreamChannel} that adds the Extended Input Stream,
 * used for STDERR.
 * 
 */

public interface SshExtendedChannel<L extends SshLifecycleListener<C>, C extends SshDataProducingComponent<L, C>>
		extends SshStreamChannel<L, C> {
	/**
	 * Get the extended input stream.
	 * 
	 * @return extended input stream
	 * @throws IOException
	 */
	InputStream getExtendedInputStream() throws IOException;

	/**
	 * Get the exit code of this command or shell (when known).
	 * 
	 * @return exist code
	 * @throws IOException
	 */
	int exitCode() throws IOException;
}