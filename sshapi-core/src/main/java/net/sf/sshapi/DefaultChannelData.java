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
package net.sf.sshapi;

import net.sf.sshapi.SshChannel.ChannelData;

public class DefaultChannelData implements ChannelData {
	int windowSize;
	int packetSize;
	long timeout;
	byte[] create;
	byte[] requestData;

	public DefaultChannelData(int windowSize, int packetSize, long timeout, byte[] requestData) {
		super();
		this.windowSize = windowSize;
		this.packetSize = packetSize;
		this.timeout = timeout;
		this.requestData = requestData;
	}

	public DefaultChannelData(int windowSize, int packetSize, long timeout, byte[] create, byte[] requestData) {
		super();
		this.windowSize = windowSize;
		this.packetSize = packetSize;
		this.timeout = timeout;
		this.create = create;
		this.requestData = requestData;
	}

	public int getWindowSize() {
		return windowSize;
	}

	public void setWindowSize(int windowSize) {
		this.windowSize = windowSize;
	}

	public int getPacketSize() {
		return packetSize;
	}

	public void setPacketSize(int packetSize) {
		this.packetSize = packetSize;
	}

	public long getTimeout() {
		return timeout;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	public void setCreate(byte[] create) {
		this.create = create;
	}

	public byte[] getRequestData() {
		return requestData;
	}

	public void setRequestData(byte[] requestData) {
		this.requestData = requestData;
	}

	public byte[] create() {
		return create;
	}

}
