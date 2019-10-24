package net.sf.sshapi.impl.libssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.sf.sshapi.AbstractLifecycleComponentWithEvents;
import net.sf.sshapi.SshChannelListener;
import net.sf.sshapi.SshDataListener;
import net.sf.sshapi.SshException;
import net.sf.sshapi.SshCommand;
import ssh.SshLibrary;
import ssh.SshLibrary.ssh_channel;
import ssh.SshLibrary.ssh_session;

class LibsshSshCommand
		extends AbstractLifecycleComponentWithEvents<SshChannelListener<SshCommand>, SshCommand>
		implements SshCommand {

	private InputStream in;
	private InputStream ext;
	private OutputStream out;
	private ssh_channel channel;
	private SshLibrary library;
	private String command;
	private ssh_session libSshSession;
	private String termType;
	private int cols;
	private int rows;

	public LibsshSshCommand(ssh_session libSshSession, SshLibrary library, String command, String termType, int cols, int rows) {
		this.library = library;
		this.command = command;
		this.libSshSession = libSshSession;
		this.termType = termType;
		this.cols = cols;
		this.rows = rows;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return in;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return out;
	}

	@Override
	public InputStream getExtendedInputStream() throws IOException {
		return ext;
	}

	// public void addDataListener(SshDataListener<> listener) {
	// }
	//
	// public void removeDataListener(SshDataListener listener) {
	// }

	@Override
	public void addDataListener(SshDataListener<SshCommand> listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeDataListener(SshDataListener<SshCommand> listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onOpen() throws SshException {

		channel = library.ssh_channel_new(libSshSession);
		if (channel == null) {
			throw new SshException(SshException.FAILED_TO_OPEN_SHELL, "Failed to open channel for command.");
		}

		try {
			int ret = library.ssh_channel_open_session(channel);
			if (ret != SshLibrary.SSH_OK) {
				throw new SshException(SshException.GENERAL, "Failed to open session for command channel.");
			}
			if (termType != null) {
				ret = library.ssh_channel_request_pty_size(channel, termType, cols, rows);
				if (ret != SshLibrary.SSH_OK) {
					throw new SshException(SshException.FAILED_TO_OPEN_SHELL, "Failed to set PTY size");
				}
			}

			try {
				ret = library.ssh_channel_request_exec(channel, command);
				if (ret != SshLibrary.SSH_OK) {
					throw new SshException(SshException.GENERAL, "Failed to execute command.");
				}

				in = new LibsshInputStream(library, channel, false);
				out = new LibsshOutputStream(library, channel);
				ext = new LibsshInputStream(library, channel, true);

			} catch (SshException sshe) {
				library.ssh_channel_close(channel);
				throw sshe;
			}

		} catch (SshException sshe) {
			library.ssh_channel_free(channel);
			throw sshe;
		}

	}

	@Override
	public void onClose() throws SshException {
		library.ssh_channel_send_eof(channel);
		try {
			in.close();
		} catch (IOException e) {
		}
		try {
			out.close();
		} catch (IOException e) {
		}
		if (channel != null) {
			library.ssh_channel_close(channel);
			library.ssh_channel_free(channel);
		}
	}

	@Override
	public int exitCode() throws IOException {
		return library.ssh_channel_get_exit_status(channel);
	}
}