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
package net.sf.sshapi.impl.nassh;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import org.snf4j.core.SelectorLoop;
import org.snf4j.core.StreamSession;

public class EngineClient {
	static final String PREFIX = "org.snf4j.";
	static final String HOST = System.getProperty(PREFIX+"Host", "127.0.0.1");
	static final int PORT = Integer.getInteger(PREFIX+"Port", 8003);
	static final int ENGINE = Integer.getInteger(PREFIX+"Engine", 2);
	static final int OFFSET = Integer.getInteger(PREFIX+"Offset", 66);
	
	public static void main(String[] args) throws Exception {
		SelectorLoop loop = new SelectorLoop();
		
		try {
			loop.start();
			
			// Initialize the connection
			SocketChannel channel = SocketChannel.open();
			channel.configureBlocking(false);
			channel.connect(new InetSocketAddress(InetAddress.getByName(HOST), PORT));
			
			//Create the engine session
			StreamSession session = new EngineSession(EngineFactory.create(ENGINE, OFFSET, true), new EngineClientHandler());
			
			// Register the channel
			loop.register(channel, session).sync().getSession();
			
			// Confirm that the connection was successful
			session.getReadyFuture().sync();

			session.write("Hello, World!".getBytes()).sync();
			
			session.quickClose();

			session.getCloseFuture().sync();
		}
		
		finally {

			// Gently stop the loop
			loop.stop();
		}
	}
}