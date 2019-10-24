package com.maverick.ssh.tests;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.FileUtils;

import net.sf.sshapi.SshConfiguration;
import net.sf.sshapi.Logger.Level;

public class RandomFilesGenerator {

	protected static long[] FILE_SIZES = new long[] { 256, 1024, 16384, 65536,
			300, 0, 600, 1000000, 25000000 };
	
	protected File localFilesDir;
	protected File testFilesDir;
	protected List<File> testFiles;
	String name;
	
	public RandomFilesGenerator() throws IOException {
		this(false, "sftp-integration-tests");
	}
	
	public RandomFilesGenerator(boolean multilevel, String name) throws IOException {
		this.name = name;
		testFilesDir = new File(new File(System.getProperty("java.io.tmpdir")),
				name);
		testFiles = new ArrayList<File>();
		if (testFilesDir.exists()) {
			FileUtils.deleteDirectory(testFilesDir);
		}
		if (!testFilesDir.mkdirs()) {
			throw new IOException("Failed to create temporary directory");
		}

		generateFiles(testFilesDir);
		
		if(multilevel) {
			File p = new File(testFilesDir, "1");
			p.mkdir();
			
			generateFiles(p);
			
			p = new File(testFilesDir, "2");
			p.mkdir();
			
			generateFiles(p);
			
			p = new File(p, "3");
			p.mkdir();
			
			generateFiles(p);
		
		}
		
		localFilesDir = new File(
				new File(System.getProperty("java.io.tmpdir")),
				"files-integration-tests-local");
		localFilesDir.mkdirs();
	}
	
	public String getName() {
		return name;
	}
	
	private void generateFiles(File parent) throws IOException {
		for (int i = 0; i < FILE_SIZES.length; i++) {
			SshConfiguration.getLogger().log(Level.INFO, "Creating test file " + i + " for size of "
					+ FILE_SIZES[i] + " bytes");
			File f = new File(parent, "testfile" + i);
			genFile(FILE_SIZES[i], f);
			testFiles.add(f);
			f = new File(parent, "alt" + i);
			genFile(FILE_SIZES[i], f);
			testFiles.add(f);
		}
	}
	public void resetLocal() throws IOException {
		// Create a space to work
		FileUtils.deleteDirectory(localFilesDir);
		if (!localFilesDir.mkdirs()) {
			throw new IOException("Failed to create temporary directory");
		}
	}

	public File getLocalFilesDir() {
		return localFilesDir;
	}

	public File getTestFilesDir() {
		return testFilesDir;
	}

	public void genFile(long length, File file) throws IOException {
		FileOutputStream fos = new FileOutputStream(file);
		byte[] b = generateChars(65535);
		try {
			long w = length;
			while (w > 0) {
				int s = (int) Math.min(w, b.length);
				fos.write(b, 0, s);
				w -= s;
			}
		} finally {
			fos.close();
		}
	}
	
	private byte[] generateChars(int length) {
		char[] chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789?#!@�$%^&*()=+-;:\\|/?".toCharArray();
		Random random = new Random();
		byte[] buf = new byte[length];
		for (int i = 0; i < length; i++) {
		    buf[i] = (byte) chars[random.nextInt(chars.length)];
		}
		return buf;
	}

	public void cleanup() throws IOException {
		if(localFilesDir != null) {
			FileUtils.deleteDirectory(localFilesDir);
		}
	}

	public int getSize() {
		return testFiles.size();
	}

	public File getRandomTestFile() {
		return testFiles.get((int)(Math.random() * (double)getSize()));
	}

	public List<File> getTestFiles() {
		return testFiles;
	}
}
