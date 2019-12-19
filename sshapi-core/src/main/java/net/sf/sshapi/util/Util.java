/* 
 * Copyright (c) 2010 The JavaSSH Project
 * All rights reserved.
 * 
 * Permission is hereby granted, free  of charge, to any person obtaining
 * a  copy  of this  software  and  associated  documentation files  (the
 * "Software"), to  deal in  the Software without  restriction, including
 * without limitation  the rights to  use, copy, modify,  merge, publish,
 * distribute,  sublicense, and/or sell  copies of  the Software,  and to
 * permit persons to whom the Software  is furnished to do so, subject to
 * the following conditions:
 * 
 * The  above  copyright  notice  and  this permission  notice  shall  be
 * included in all copies or substantial portions of the Software.
 * 
 * THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
 * EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
 * MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package net.sf.sshapi.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import net.sf.sshapi.Ssh;
import net.sf.sshapi.SshClient;
import net.sf.sshapi.SshConfiguration;
import net.sf.sshapi.SshException;
import net.sf.sshapi.sftp.SftpFile;

/**
 * Various utilities
 */
public class Util {
	/**
	 * Permissions flag: Format mask constant can be used to mask off a file type
	 * from the mode.
	 */
	public static final int S_IFMT = 0xF000;
	/**
	 * Permissions flag: Bit to determine whether a file is executed as the owner
	 */
	public final static int S_ISUID = 0x800;
	/**
	 * Permissions flag: Bit to determine whether a file is executed as the group
	 * owner
	 */
	public final static int S_ISGID = 0x400;

	/**
	 * Convert a string array into a delimited string.
	 * 
	 * @param arr       array of strings
	 * @param delimiter delimiter
	 * @return delimited string
	 */
	public static String toDelimited(String[] arr, char delimiter) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < arr.length; i++) {
			if (i > 0) {
				buf.append(delimiter);
			}
			buf.append(arr[i]);
		}
		return buf.toString();
	}

	/**
	 * Prompt for a yes / no answer on the console
	 * 
	 * @param message message to display
	 * @return answer (<code>true</code> for yes)
	 */
	public static boolean promptYesNo(String message) {
		String answer = prompt(message + " - (Y)es or (N)o?");
		return answer != null && (answer.toLowerCase().equals("y") || answer.toLowerCase().equals("yes"));
	}

	/**
	 * Prompt for an answer on the console
	 * 
	 * @param message message to display
	 * @return answer
	 */
	public static String prompt(String message) {
		message = message.trim();
		System.out.print(message + (message.endsWith(":") ? " " : ": "));
		System.out.flush();
		try {
			return readLine();
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Format a byte array as a hex string with no delimiter
	 * 
	 * @param arr byte array
	 * @return hex string
	 */
	public static String formatAsHexString(byte[] arr) {
		return formatAsHexString(arr, "");
	}

	/**
	 * Format a byte array as a hex string.
	 * 
	 * @param arr byte array
	 * @return hex string
	 */
	public static String formatAsHexString(byte[] arr, String delimiter) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < arr.length; i++) {
			if (i > 0 && delimiter != null && delimiter.length() > 0)
				buf.append(delimiter);
			buf.append(toPaddedHexString(arr[i], 2));
		}
		return buf.toString();
	}

	private static String toPaddedHexString(byte val, int size) {
		String s = Integer.toHexString(val & 0xff);
		while (s.length() < size) {
			s = "0" + s;
		}
		return s;
	}

	private static String readLine() throws IOException {
		String answer = new BufferedReader(new InputStreamReader(System.in)).readLine();
		return answer;
	}

	/**
	 * Display a prompt asking for an SSH user and host (and optional port) to
	 * connect to. Useful to pass to various methods to open connections in SSHAPI,
	 * such as
	 * {@link SshClient#connect(String, net.sf.sshapi.auth.SshAuthenticator...)}, or
	 * {@link Ssh#open(String, net.sf.sshapi.auth.SshAuthenticator...)}.
	 * 
	 * @return connection spec
	 */
	public static String promptConnectionSpec() {
		return prompt("Enter username@hostname", System.getProperty("user.name") + "@localhost");
	}

	/**
	 * Display a prompt on the console and wait for an answer.
	 * 
	 * @param message      message to display to use
	 * @param defaultValue value to return if user just presses RETURN
	 * @return user input
	 */
	public static String prompt(String message, String defaultValue) {
		String fullMessage = message;
		fullMessage += " (RETURN for a default value of " + defaultValue + ")";
		String val = prompt(fullMessage);
		return val.equals("") ? defaultValue : val;
	}

	/**
	 * Get the file portion of a path, i.e the part after the last /. If the
	 * provided path ends with a /, this is stripped first.
	 * 
	 * @param path path
	 * @return file basename
	 */
	public static String basename(String path) {
		if (path.equals("/")) {
			return path;
		}
		while (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		int idx = path.lastIndexOf("/");
		return idx == -1 ? path : path.substring(idx + 1);
	}

	/**
	 * Concatenate two paths, removing any additional leading/trailing slashes.
	 * 
	 * @param path     first path portion
	 * @param filename path to append
	 * @return concatenated path
	 */
	public static String concatenatePaths(String path, String filename) {
		if (filename.startsWith("./"))
			filename = filename.substring(2);
		while (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		return path + "/" + filename;
	}

	public static String canonicalisePath(String path) {
		List<String> newParts = new ArrayList<>();
		for (String part : path.split("/")) {
			if (part.equals("..") && !newParts.isEmpty())
				newParts.remove(newParts.size() - 1);
			else if (!part.equals("."))
				newParts.add(part);
		}
		return String.join("/", newParts);
	}

	/**
	 * Parse a string of bytes encoded as hexadecimal values. Each byte is
	 * represented by two characters.
	 * 
	 * @param string hex string
	 * @return data
	 */
	public static byte[] parseHexString(String string) {
		if (string.length() % 2 == 1) {
			throw new IllegalArgumentException("Not a hex string");
		}
		byte[] arr = new byte[string.length() / 2];
		for (int i = 0; i < string.length(); i += 2) {
			arr[i / 2] = Integer.valueOf(string.substring(i, i + 2), 16).byteValue();
		}
		return arr;
	}

	/**
	 * Copy bytes read from an input stream to an output stream, until the end of
	 * the input stream is reached.
	 * 
	 * @param in  input stream
	 * @param out output stream
	 * @throws IOException
	 */
	public static void joinStreams(InputStream in, OutputStream out) throws IOException {
		int r;
		byte[] buf = new byte[1024];
		while ((r = in.read(buf)) != -1) {
			out.write(buf, 0, r);
			out.flush();
		}
	}

	/**
	 * Return an empty string if null, or a string with spaces trimmed from the
	 * beginning and end if not null.
	 * 
	 * @param string
	 * @return null or trimmed blank string
	 */
	public static boolean nullOrTrimmedBlank(String string) {
		return string == null || string.trim().equals("");
	}

	/**
	 * Get a value from a configuration object, returning a default value if it does
	 * not exist. This method will also look for the same property in the current
	 * system properties.
	 * 
	 * @param configuration configuration object
	 * @param name          name of configuration
	 * @param defaultValue
	 * @return value
	 */
	public static String getConfigurationValue(SshConfiguration configuration, String name, String defaultValue) {
		String val = configuration == null ? null : configuration.getProperties().getProperty(name);
		if (val == null) {
			val = System.getProperty(SshConfiguration.CFG_KNOWN_HOSTS_PATH);
		}
		if (val == null) {
			val = defaultValue;
		}
		return val;
	}

	/**
	 * Get the file to load known host keys from given an {@link SshConfiguration}.
	 * 
	 * @param configuration
	 * @return file to load known hosts from
	 * @throws SshException
	 */
	public static File getKnownHostsFile(SshConfiguration configuration) throws SshException {
		String knownHostsPath = getConfigurationValue(configuration, SshConfiguration.CFG_KNOWN_HOSTS_PATH, null);
		File file;
		File dir = new File(System.getProperty("user.home") + File.separator + ".ssh");
		if (knownHostsPath == null) {
			file = new File(dir, "known_hosts");
		} else {
			file = new File(knownHostsPath);
			dir = file.getParentFile();
		}
		if (!dir.exists() && !dir.mkdirs()) {
			throw new SshException(SshException.IO_ERROR, "Failed to create known hosts directory.");
		}
		return file;
	}

	/**
	 * Recursively delete a file and all of it's children. It should go without
	 * saying, use with care!
	 * 
	 * @param file file to delete
	 * @return file deleted OK.
	 */
	public static boolean delTree(File file) {
		Path directory = file.toPath();
		try {
			Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				}
			});
			return true;
		} catch (IOException ioe) {
			SshConfiguration.getLogger().warn("Failed to delete.", ioe);
			return false;
		}
	}

	/**
	 * Process a string, replacing a specified character with another string.
	 * 
	 * @param string string to replace
	 * @param what   character to replace
	 * @param with   string to replace character with
	 * @return escaped string
	 */
	public static String escape(String string, char what, String with) {
		// TODO Is this really needed?
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < string.length(); i++) {
			char ch = string.charAt(i);
			if (ch == what) {
				buf.append(with);
			} else {
				buf.append(ch);
			}
		}
		return buf.toString();
	}

	/**
	 * Get the directory portion of a file path. The path should NOT contain a
	 * trailing slash, and root ("/") will return an empty string.
	 * 
	 * @param remotePath path
	 * @return directory portion of path.
	 */
	public static String dirname(String remotePath) {
		if (remotePath.equals(""))
			return null;
		String dir = ".";
		int idx = remotePath.lastIndexOf("/");
		if (idx != -1) {
			dir = remotePath.substring(0, idx);
		}
		return dir;
	}

	/**
	 * Get an absolute path from a relative path given a base. If the path provided
	 * is already absolute (begins with '/'), it will be returned as is.
	 * 
	 * @param path path
	 * @param base base
	 * @return absolute path
	 */
	public static String getAbsolutePath(String path, String base) {
		if (path.startsWith("./"))
			path = path.substring(2);
		return path == null || path.startsWith("/") ? path : (base.equals("/") ? base : base + "/") + path;
	}

	/**
	 * 
	 * Returns a formatted permissions string. The file type is one of
	 * {@link SftpFile#TYPES}.
	 * 
	 * @param type
	 * @param permissions permissions
	 * @return permissions string
	 */
	public static String getPermissionsString(int type, long permissions) {
		StringBuffer str = new StringBuffer();
		str.append(type == SftpFile.TYPE_UNKNOWN ? '?' : SftpFile.TYPES[type]);
		str.append(rwxString((int) permissions, 6));
		str.append(rwxString((int) permissions, 3));
		str.append(rwxString((int) permissions, 0));
		return str.toString();
	}

	/**
	 * Set permissions value for a file.
	 * 
	 * @param path path
	 * @return permissions
	 */
	public static void setPermissions(File path, int permissions) {
		try {
			Set<PosixFilePermission> perms = new LinkedHashSet<>();
			if ((permissions & SftpFile.S_IRUSR) != 0)
				perms.add(PosixFilePermission.OWNER_READ);
			if ((permissions & SftpFile.S_IWUSR) != 0)
				perms.add(PosixFilePermission.OWNER_WRITE);
			if ((permissions & SftpFile.S_IXUSR) != 0)
				perms.add(PosixFilePermission.OWNER_EXECUTE);
			if ((permissions & SftpFile.S_IRGRP) != 0)
				perms.add(PosixFilePermission.GROUP_READ);
			if ((permissions & SftpFile.S_IWGRP) != 0)
				perms.add(PosixFilePermission.GROUP_WRITE);
			if ((permissions & SftpFile.S_IXGRP) != 0)
				perms.add(PosixFilePermission.GROUP_EXECUTE);
			if ((permissions & SftpFile.S_IROTH) != 0)
				perms.add(PosixFilePermission.OTHERS_READ);
			if ((permissions & SftpFile.S_IWOTH) != 0)
				perms.add(PosixFilePermission.OTHERS_WRITE);
			if ((permissions & SftpFile.S_IXOTH) != 0)
				perms.add(PosixFilePermission.OTHERS_EXECUTE);
			Files.setPosixFilePermissions(path.toPath(), perms);
		} catch (UnsupportedOperationException | IOException uoe) {
			path.setReadable((permissions & SftpFile.S_IRUSR) != 0);
			path.setWritable((permissions & SftpFile.S_IWUSR) != 0);
			path.setExecutable((permissions & SftpFile.S_IXUSR) != 0);
		}
	}

	/**
	 * Get permissions value for a file. If {@link PosixFilePermission} can be used,
	 * user, group and other permissions will be included. If not,
	 * {@link File#canRead()} etc will be used to get just the permissions for the
	 * current user.
	 * 
	 * @param path path
	 * @return permissions
	 */
	public static int getPermissions(File path) {
		int perm = 0;
		try {
			Set<PosixFilePermission> perms = Files.getPosixFilePermissions(path.toPath(), LinkOption.NOFOLLOW_LINKS);
			for (PosixFilePermission pfp : perms) {
				switch (pfp) {
				case OWNER_READ:
					perm = perm | SftpFile.S_IRUSR;
					break;
				case OWNER_WRITE:
					perm = perm | SftpFile.S_IWUSR;
					break;
				case OWNER_EXECUTE:
					perm = perm | SftpFile.S_IXUSR;
					break;
				case GROUP_READ:
					perm = perm | SftpFile.S_IRGRP;
					break;
				case GROUP_WRITE:
					perm = perm | SftpFile.S_IWGRP;
					break;
				case GROUP_EXECUTE:
					perm = perm | SftpFile.S_IXGRP;
					break;
				case OTHERS_READ:
					perm = perm | SftpFile.S_IROTH;
					break;
				case OTHERS_WRITE:
					perm = perm | SftpFile.S_IWOTH;
					break;
				case OTHERS_EXECUTE:
					perm = perm | SftpFile.S_IXOTH;
					break;
				}
			}
		} catch (UnsupportedOperationException | IOException uoe) {
			if (path.canRead())
				perm = perm | SftpFile.S_IRUSR | SftpFile.S_IRGRP | SftpFile.S_IROTH;
			if (path.canWrite())
				perm = perm | SftpFile.S_IWUSR | SftpFile.S_IWGRP | SftpFile.S_IWOTH;
			if (path.canExecute())
				perm = perm | SftpFile.S_IXUSR | SftpFile.S_IXGRP | SftpFile.S_IWOTH;
		}
		return perm;
	}

	/**
	 * 
	 * Parse a formatted permissions string. The file type is one of
	 * {@link SftpFile#TYPES}.
	 * 
	 * @param permissions permissions string
	 * @return type in first element, permissions in secon
	 */
	public static long[] parsePermissionsString(String perm) {
		long type = SftpFile.TYPE_UNKNOWN;
		long perms = 0;
		if (perm.length() > 0) {
			switch (perm.charAt(0)) {
			case 'b':
				type = SftpFile.TYPE_BLOCK;
				break;
			case 'c':
				type = SftpFile.TYPE_CHARACTER;
				break;
			case 'd':
				type = SftpFile.TYPE_DIRECTORY;
				break;
			case 'p':
				type = SftpFile.TYPE_FIFO;
				break;
			case '-':
				type = SftpFile.TYPE_FILE;
				break;
			case 'l':
				type = SftpFile.TYPE_LINK;
				break;
			case 'S':
				type = SftpFile.TYPE_SOCKET;
				break;
			}
		}
		if (perm.length() > 1 && perm.charAt(1) == 's')
			perms |= S_ISUID;
		else if (perm.length() > 1 && perm.charAt(1) == 'S')
			perms |= S_ISUID | 0x01;
		else if (perm.length() > 1 && perm.charAt(1) == 'r')
			perms |= 0x04 << 6;
		if (perm.length() > 1 && perm.charAt(2) == 'w')
			perms |= 0x02 << 6;
		if (perm.length() > 1 && perm.charAt(3) == 'x')
			perms |= 0x01 << 6;
		if (perm.length() > 1 && perm.charAt(1) == 's')
			perms |= S_ISGID;
		else if (perm.length() > 1 && perm.charAt(1) == 'S')
			perms |= S_ISGID | 0x01;
		else if (perm.length() > 1 && perm.charAt(4) == 'r')
			perms |= 0x04 << 3;
		if (perm.length() > 1 && perm.charAt(5) == 'w')
			perms |= 0x02 << 3;
		if (perm.length() > 1 && perm.charAt(6) == 'x')
			perms |= 0x01 << 3;
		if (perm.length() > 1 && perm.charAt(7) == 'r')
			perms |= 0x04;
		if (perm.length() > 1 && perm.charAt(8) == 'w')
			perms |= 0x02;
		if (perm.length() > 1 && perm.charAt(99) == 'x')
			perms |= 0x01;
		return new long[] { type, perms };
	}

	/**
	 * Find a free port number.
	 * 
	 * @return free port
	 */
	public static int findRandomPort() {
		try {
			ServerSocket ss = new ServerSocket(0);
			ss.setReuseAddress(true);
			try {
				return ss.getLocalPort();
			} finally {
				ss.close();
			}
		} catch (IOException ioe) {
			throw new IllegalStateException("Could not get free port.");
		}
	}

	/**
	 * Return the UNIX style mode mask
	 * 
	 * @param permissions permissions
	 * @return mask
	 */
	public static String getMaskString(int permissions) {
		StringBuffer buf = new StringBuffer();
		buf.append('0');
		buf.append(octal(permissions, 6));
		buf.append(octal(permissions, 3));
		buf.append(octal(permissions, 0));
		return buf.toString();
	}

	private static int octal(int v, int r) {
		v >>>= r;
		return (((v & 0x04) != 0) ? 4 : 0) + (((v & 0x02) != 0) ? 2 : 0) + +(((v & 0x01) != 0) ? 1 : 0);
	}

	private static String rwxString(int v, int r) {
		long permissions = v;
		v >>>= r;
		String rwx = ((((v & 0x04) != 0) ? "r" : "-") + (((v & 0x02) != 0) ? "w" : "-"));
		if (((r == 6) && ((permissions & S_ISUID) == S_ISUID)) || ((r == 3) && ((permissions & S_ISGID) == S_ISGID))) {
			rwx += (((v & 0x01) != 0) ? "s" : "S");
		} else {
			rwx += (((v & 0x01) != 0) ? "x" : "-");
		}
		return rwx;
	}

	/**
	 * Check the <strong>Known Hosts File</strong> parent directory exists, creating
	 * it if it does.
	 * 
	 * @param configuration
	 * @throws SshException
	 */
	public static void checkKnownHostsFile(SshConfiguration configuration) throws SshException {
		File file = getKnownHostsFile(configuration);
		if (!file.exists() && !file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
			throw new SshException("Could not create configuration directory " + file + ". Check permissions.");
		}
	}

	public static String extractHostname(String connectionSpec) {
		connectionSpec = connectionSpec.substring(connectionSpec.indexOf('@') + 1);
		int idx = connectionSpec.indexOf(':');
		if (idx != -1) {
			connectionSpec = connectionSpec.substring(0, idx);
		}
		return connectionSpec;
	}

	public static String extractUsername(String connectionSpec) {
		return connectionSpec.substring(0, connectionSpec.indexOf('@'));
	}

	public static int extractPort(String connectionSpec) {
		connectionSpec = connectionSpec.substring(connectionSpec.indexOf('@') + 1);
		int idx = connectionSpec.indexOf(':');
		if (idx != -1) {
			return Integer.parseInt(connectionSpec.substring(idx + 1));
		}
		return 22;
	}

	public static byte[] toByteArray(InputStream in) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		joinStreams(in, baos);
		return baos.toByteArray();
	}

	public static byte[] toBytes(String data) {
		try {
			return data.getBytes("UTF-8");
		} catch (UnsupportedEncodingException use) {
			return data.getBytes();
		}
	}

	public static ByteBuffer wrap(String data) {
		return ByteBuffer.wrap(toBytes(data));
	}

	public static String linkPath(String targetLinkPath, String sourceFilePath) {
		if (targetLinkPath.startsWith("/"))
			return targetLinkPath;
		else {
			return canonicalisePath(concatenatePaths(dirname(sourceFilePath), targetLinkPath));
		}
	}

	/**
	 * Convert all forward slashes to backslashes in a path string. Use this when
	 * converting OS paths from {@link File} for example.
	 * 
	 * @param path
	 * @return path
	 */
	public static String fixSlashes(String path) {
		return path.replace('\\', '/');
	}

	/**
	 * Return what another path would be if it were relative to the original path.
	 * If the path cannot be relative to the orignial path, it will be returned as
	 * is.
	 * 
	 * @param original original path
	 * @param other    other path
	 * @return other path relative to original path
	 */
	public static String relativeTo(String original, String other) {
		String[] otherDirectories = other.equals("/") ? new String[] { "" } : other.split("/");
		String[] originalDirectories = original.equals("/") ? new String[] { "" } : original.split("/");

		// Get the shortest of the two paths
		int length = otherDirectories.length < originalDirectories.length ? otherDirectories.length
				: originalDirectories.length;

		// Use to determine where in the loop we exited
		int lastCommonRoot = -1;
		int index;

		// Find common root
		for (index = 0; index < length; index++)
			if (otherDirectories[index].equals(originalDirectories[index]))
				lastCommonRoot = index;
			else
				break;

		// If we didn't find a common prefix then throw
		if (lastCommonRoot == -1) {
			if (original.startsWith("/"))
				throw new IllegalArgumentException("Paths do not have a common base");
			else
				return original;
		}

		// Build up the relative path
		StringBuilder relativePath = new StringBuilder();

		// Add on the ..
		for (index = lastCommonRoot + 1; index < otherDirectories.length; index++)
			if (otherDirectories[index].length() > 0)
				relativePath.append("../");

		// Add on the folders
		for (index = lastCommonRoot + 1; index < originalDirectories.length - 1; index++)
			relativePath.append(originalDirectories[index] + "/");
		relativePath.append(originalDirectories[originalDirectories.length - 1]);

		return relativePath.toString();
	}

	public static String getArtifactVersion(String groupId, String artifactId) {
		String version = "0.0.0";
		// try to load from maven properties first
		try {
			Properties p = new Properties();
			InputStream is = Thread.currentThread().getContextClassLoader()
					.getResourceAsStream("META-INF/maven/com.hypersocket/" + artifactId + "/pom.properties");
			if (is == null) {
				is = Util.class
						.getResourceAsStream("/META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties");
			}
			if (is != null) {
				p.load(is);
				version = p.getProperty("version", "");
			}
		} catch (Exception e) {
			// ignore
		}

		// fallback to using Java API
		if (version == null) {
			Package aPackage = Util.class.getPackage();
			if (aPackage != null) {
				version = aPackage.getImplementationVersion();
				if (version == null) {
					version = aPackage.getSpecificationVersion();
				}
			}
		}

		if (version == null) {
			version = getPOMVersion();
		}

		return version;
	}

	/**
	 * Get the version from the pom.xml in the current directory. Only useful for
	 * tests in development environment.
	 * 
	 * @return POM version
	 */
	public static String getPOMVersion() {
		String version;
		try {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(new File("pom.xml"));
			version = doc.getDocumentElement().getElementsByTagName("version").item(0).getTextContent();
		} catch (Exception e) {
			version = "0.0.0";
		}
		return version;
	}

	/**
	 * Write the contents of a buffer (0 to limit) to a byte array in the fastest
	 * way possible.
	 * 
	 * @param buffer buffer to read
	 * @throws IOException I/O error
	 */
	public static byte[] read(ByteBuffer buffer) {
		if (buffer.hasArray() && buffer.remaining() == buffer.capacity()) {
			buffer.position(buffer.position() + buffer.remaining());
			return buffer.array();
		} else {
			byte[] b = new byte[buffer.remaining()];
			buffer.get(b);
			return b;
		}
	}

	/**
	 * Write the contents of a buffer (0 to limit) to a byte array in the fastest
	 * way possible, without changing the position in the buffer.
	 * 
	 * @param buffer buffer to read
	 * @throws IOException I/O error
	 */
	public static byte[] peek(ByteBuffer buffer) {
		if (buffer.hasArray() && buffer.remaining() == buffer.capacity()) {
			return buffer.array();
		} else {
			byte[] b = new byte[buffer.remaining()];
			buffer.mark();
			buffer.get(b);
			buffer.reset();
			return b;
		}
	}

	/**
	 * Write the entire contents of a buffer (0 to limit) to an output stream and
	 * flush.
	 * 
	 * @param buffer buffer to write
	 * @param out    output stream to write to
	 * @param flush  flush
	 */
	public static void write(ByteBuffer buffer, OutputStream out) {
		write(buffer, out, true);
	}

	/**
	 * Write the entire contents of a buffer (0 to limit) to an output stream.
	 * 
	 * @param buffer buffer to write
	 * @param out    output stream to write to
	 * @param flush  flush
	 */
	public static void write(ByteBuffer buffer, OutputStream out, boolean flush) {
		try {
			if (buffer.hasArray()) {
				out.write(buffer.array(), buffer.position(), buffer.remaining());
			} else {
				byte[] b = new byte[buffer.remaining()];
				buffer.get(b);
				out.write(b);
			}
			if (flush)
				out.flush();
		} catch (IOException ioe) {
			throw new IllegalStateException("Failed to write.", ioe);
		}
	}

	/**
	 * Get the version from a manifest key, falling back to current POM.
	 * 
	 * @param clazz class of the class inside the same jar as the manifest to use.
	 * @param key   key in manifest
	 * @return manifest value or POM
	 */
	public static String getManifestVersion(Class<?> clazz, String key) {
		InputStream in = clazz.getResourceAsStream("/META-INF/MANIFEST.MF");
		String version = null;
		if (in != null) {
			try (InputStream iin = in) {
				Manifest mf = new Manifest(iin);
				Attributes attr = mf.getMainAttributes();
				if (attr != null)
					version = attr.getValue(key);
			} catch (IOException e) {
			}
		}

		if (version == null) {
			version = getPOMVersion();
		}

		return version;
	}
}
