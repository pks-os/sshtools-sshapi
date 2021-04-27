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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import net.sf.sshapi.SshConfiguration;

class Util {
	public static File TEST_FILE = new File("target/test-file");

	protected static void createTempFile() throws FileNotFoundException, IOException {
		long requiredFileSize = Long.parseLong(AbstractConnectionTest.PROPERTIES.getProperty("fileSize", "65536"));
		if (requiredFileSize != TEST_FILE.length()) {
			SshConfiguration.getLogger().info("Generating test file {0} of {1} bytes", TEST_FILE, requiredFileSize);
			FileOutputStream fos = new FileOutputStream(TEST_FILE);
			try {
				for (int i = 0; i < requiredFileSize; i++) {
					fos.write((int) (Math.random() * 256));
				}
			} finally {
				fos.close();
			}

			SshConfiguration.getLogger().info("Generated test file {0}", TEST_FILE);
		}
	}

}
