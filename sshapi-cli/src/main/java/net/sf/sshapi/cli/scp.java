/**
 * Copyright (c) 2020 The JavaSSH Project
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package net.sf.sshapi.cli;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import net.sf.sshapi.Logger;
import net.sf.sshapi.SshClient;
import net.sf.sshapi.SshException;
import net.sf.sshapi.SshFileTransferListener;
import net.sf.sshapi.SshSCPClient;
import net.sf.sshapi.util.Util;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Java clone of the de-facto standard OpenSSH ssh command.
 */
@Command(name = "scp", mixinStandardHelpOptions = true, description = "Secure file copy.")
public class scp extends AbstractSshCommand implements SshFileTransferListener, Logger, Callable<Integer> {

	private static final long MS_IN_MINUTE = TimeUnit.MINUTES.toMillis(1);
	private static final long MS_IN_HOUR = TimeUnit.HOURS.toMillis(1);
	private long transferLength;
	private String transferPath;
	private long transferProgressed;
	private long transferLastUpdate;
	private int transferSpeed;
	private long transferBlock;

	@Option(names = { "-P", "--port" }, description = "Port number on which the server is listening.")
	private int port = 22;

	@Option(names = { "-B",
			"--batch" }, description = "Selects batch mode (prevents asking for passwords or passphrases).")
	private boolean batch;

	@Option(names = { "-r",
			"--recursive" }, description = " Recursively copy entire directories.  Note that ssh follows symbolic links encountered in the tree traversal.")
	private boolean recursive;

	@Parameters(index = "0", description = "The source file or directory.")
	private String source;

	@Parameters(index = "1", description = "The target file or directory.")
	private String target;

	/**
	 * Constructor.
	 *
	 * @throws SshException
	 */
	public scp() throws SshException {
	}

	@Override
	protected void onStart() throws SshException, IOException {
		if (isRemotePath(source)) {
			if (isRemotePath(target)) {
				// Remote to remote
				remoteToRemote();
			} else {
				// Remote to local
				remoteToLocal();
			}
		} else {
			if (isRemotePath(target)) {
				localToRemote();
				// Local to remote
			} else {
				// Local to local
				throw new UnsupportedOperationException("TODO");
			}
		}
	}

	void remoteToRemote() throws SshException, IOException {
		connect(getConnectionDetails(source));
		connect(getConnectionDetails(target));
	}

	void remoteToLocal() throws SshException, IOException {
		connect(getConnectionDetails(source));
	}

	void localToRemote() throws SshException, IOException {
		String targetPath = getPath(target);
		try (SshClient client = connect(getConnectionDetails(target))) {
			SshSCPClient scp = client.createSCP();
			scp.addFileTransferListener(this);
			scp.open();
			try {
				scp.put(targetPath, "0770", checkPath(source), recursive);
			} finally {
				scp.close();
			}
		}
	}

	File checkPath(String path) throws FileNotFoundException {
		File file = new File(path);
		if (!file.exists()) {
			throw new FileNotFoundException(path);
		}
		return file;
	}

	@Override
	String getPath(String path) {
		int idx = path.indexOf(":");
		if (idx == -1) {
			return null;
		}
		return path.substring(idx + 1);
	}

	@Override
	boolean isRemotePath(String path) {
		int idx = path.indexOf("@");
		int idx2 = path.indexOf(":");
		return idx > -1 && idx2 > -1 && idx2 > idx;
	}

//	void buildOptions(Options options) {
//		options.addOption("1", false, "Forces ssh to use protocol 1");
//		options.addOption("2", false, "Forces ssh to use protocol 2");
//		options.addOption("3", false,
//				"Copies between two remote hosts are transferred through the local host.  Without "
//						+ "this option the data is copied directly between the two remote hosts.  Note that this option disables "
//						+ "the progress meter.");
//		options.addOption("P", true,
//				"Specifies the port to connect to on the remote host.  Note "
//						+ "that this option is written with a capital ‘P’, because -p is already reserved for "
//						+ "preserving the times and modes of the file");
//		options.addOption("c", true, "Selects the cipher to use for encrypting the data transfer.");
//		options.addOption("i", true,
//				"Selects the file from which the identity (private key) for public key authentication" + " is read.");
//		options.addOption("r", false, "Recursively copy entire directories.  Note that ssh follows symbolic links "
//				+ "encountered in the tree traversal.");
//		options.addOption("F", true,
//				"Specifies an alternative per-user configuration file (IGNORED - Only present for OpenSSH compatibility).");
//		options.addOption("o", true, "Additional options (IGNORED - Only present for OpenSSH compatibility).");
//		options.addOption("4", false,
//				"Forces use of IPv4 addresses only (IGNORED - Only present for OpenSSH compatibility).");
//		options.addOption("6", false,
//				"Forces use of IPv6 addresses only (IGNORED - Only present for OpenSSH compatibility).");
//		options.addOption("p", false,
//				"Preserves modification times, access times, and modes from the original file (IGNORED - Only present for "
//						+ "OpenSSH compatibility).");
//		options.addOption("v", false,
//				"Verbose mode.  Print debugging messages about their progress. This is helpful in debugging connection, "
//						+ "authentication, and configuration problems.");
//		options.addOption("l", false,
//				"Limits the used bandwidth, specified in Kbit/s. (IGNORED - Only present for OpenSSH compatibility).");
//		options.addOption("B", false, "Selects batch mode (prevents asking for passwords or passphrases).");
//		options.addOption("C", false,
//				"Compression enable. Will be ignored if the provider does not support compression.");
//		options.addOption("q", false,
//				"Quiet mode: disables the progress meter as well as warning and diagnostic messages.");
//		options.addOption("S", true,
//				"Classname of SSH provider to use (Note, in OpenSSH this option is 'program' which is an executable. In this case, it must be the classname of an SSHAPI provider.");
//	}

	/**
	 * Entry point.
	 *
	 * @param args command line arguments
	 * @throws Exception on error
	 */
	public static void main(String[] args) throws Exception {
		scp cli = new scp();
		System.exit(new CommandLine(cli).execute(args));
	}

	@Override
	public Integer call() throws SshException {
		try {
			start();
		} catch (FileNotFoundException e) {
			if (!isQuiet()) {
				System.err.println(e.getMessage() + ": No such file or directory");
			}
			return 1;
		} catch (SshException sshe) {
			if (sshe.getCode().equals(SshException.HOST_KEY_REJECTED)) {
				// Already displayed a message, just exit
			} else {
				if (!isQuiet()) {
					System.err.println("scp: " + sshe.getMessage());
					if(isLevelEnabled(Level.DEBUG))
						sshe.printStackTrace();
				}
			}
			return 1;
		} catch (Exception e) {
			if (!isQuiet()) {
				System.err.println("scp: " + e.getMessage());
				if(isLevelEnabled(Level.DEBUG))
					e.printStackTrace();
			}
			return 1;
		}
		return 0;
	}

	@Override
	public void startedTransfer(String path, String targetPath, long length) {
		this.transferLength = length;
		this.transferLastUpdate = System.currentTimeMillis();
		this.transferProgressed = 0;
		this.transferSpeed = 0;
		this.transferBlock = 0;
		this.transferPath = Util.basename(path);
		updateProgress(false);
	}

	@Override
	public void transferProgress(String path, String targetPath, long progress) {
		transferBlock += progress;
		if ((System.currentTimeMillis() - this.transferLastUpdate) > 1000) {
			updateBlock(false);
		}
	}

	@Override
	public void finishedTransfer(String path, String targetPath) {
		updateBlock(true);
	}

	private void updateBlock(boolean newline) {
		long now = System.currentTimeMillis();
		long taken = now - this.transferLastUpdate;
		this.transferLastUpdate = now;
		this.transferSpeed = (int) ((taken / 1000.0) * transferBlock);
		transferProgressed += transferBlock;
		transferBlock = 0;
		updateProgress(newline);
	}

	private void updateProgress(boolean newline) {
		int pc = (int) (((double) transferProgressed / (double) transferLength) * 100.0);
		String sizeSoFar = formatSize(transferProgressed);
		// width - ( 5+ 10 + 8 + 3 + 1 + 1 + 1 + 1 )
		int w = reader == null ? 80 : terminal.getWidth();
		int filenameWidth = Math.max(10, w - 38);
		long msRemain = ( transferLength - transferProgressed ) / Math.max(1, transferSpeed / 1000);
		String result = String.format("%-" + filenameWidth + "s %3d%% %-8s %10s %5s",
				new Object[] { transferPath, Integer.valueOf(pc), sizeSoFar, formatSpeed(transferSpeed), formatTime(msRemain) });
		if (terminal == null) {
			System.out.print(result + "\r");
			if (newline) {
				System.out.println();
			}
		} else {
			if(newline)
				terminal.writer().println(result);
			else {
				terminal.writer().print(result + "\r");
			}
			terminal.writer().flush();
//			reader.getBuffer().clear();
//			reader.getBuffer().write(result);
//			reader.getBuffer().atChar(w);
//			if (newline) {
//				reader.getBuffer().down();
//				reader.getBuffer().atChar(0);
//			}
		}

	}

	private String formatSpeed(long bytesPerSecond) {
		String speedText = String.valueOf(bytesPerSecond) + "B/s";
		if (bytesPerSecond > 9999) {
			bytesPerSecond = bytesPerSecond / 1024;
			speedText = bytesPerSecond + "KB/s";
			if (bytesPerSecond > 9999) {
				bytesPerSecond = bytesPerSecond / 1024;
				speedText = bytesPerSecond + "MB/s";
				if (bytesPerSecond > 9999) {
					bytesPerSecond = bytesPerSecond / 1024;
					speedText = bytesPerSecond + "GB/s";
				}
			}
		}
		return speedText;
	}

	private String formatTime(long ms) {
		long hrs = ms / MS_IN_HOUR;
		long mins = (ms % MS_IN_HOUR) / MS_IN_MINUTE;
		long secs = (ms - (hrs * MS_IN_HOUR + (mins * MS_IN_MINUTE))) / 1000;

		if(hrs > 99999) {
			return String.format("%08d ETA", hrs);
		}
		else if(hrs > 99) {
			return String.format("%4d:%02dm ETA", hrs, mins);
		}
		else if(hrs > 0) {
			return String.format("%02d:%02d:%02d ETA", hrs, mins, secs);
		}
		else {
			return String.format("   %02d:%02d ETA", mins, secs);
		}
	}

	private String formatSize(long bytes) {
		String sizeSoFar = String.valueOf(bytes);
		long size = bytes;
		if (size > 9999) {
			size = size / 1024;
			sizeSoFar = size + "KB";
			if (size > 9999) {
				size = size / 1024;
				sizeSoFar = size + "MB";
				if (size > 9999) {
					size = size / 1024;
					sizeSoFar = size + "GB";
				}
			}
		}
		return sizeSoFar;
	}

	@Override
	public boolean isLevelEnabled(Level level) {
		return this.level.compareTo(level) <= 0;
	}

	@Override
	protected boolean isBatchMode() {
		return batch;
	}

	@Override
	protected int getPort() {
		return port;
	}
}
