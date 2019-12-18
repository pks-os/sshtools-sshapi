package net.sf.sshapi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.Future;

public abstract class AbstractSshStreamChannel<L extends SshChannelListener<C>, C extends SshStreamChannel<L, C>>
		extends AbstractDataProducingComponent<L, C> implements SshStreamChannel<L, C> {

	protected SshConfiguration configuration;

	private SshInput input;
	private Thread inputThread;
	private boolean eofFired;

	protected AbstractSshStreamChannel(SshProvider provider, SshConfiguration configuration) {
		super(provider);
		this.configuration = configuration;
	}

	@Override
	protected final void onClose() throws SshException {
		fireEof();
		onCloseStream();
	}

	protected void onCloseStream() throws SshException {
	}

	protected void onOpenStream() throws SshException {
	}

	@Override
	protected final void onOpen() throws SshException {
		onOpenStream();
		if (input != null) {
			try {
				inputThread = pump(input, getInputStream());
			} catch (IOException e) {
				throw new IllegalStateException("Could not get input stream.", e);
			}
		}
	}

	@SuppressWarnings("unchecked")
	protected void fireEof() {
		if (!eofFired) {
			try {
				if (listeners != null)
					for (int i = listeners.size() - 1; i >= 0; i--)
						listeners.get(i).eof((C) this);
			} finally {
				eofFired = true;
			}
		}
	}

	@Override
	public void setInput(SshInput input) {
		if (!Objects.equals(input, this.input)) {
			this.input = input;
			if (input == null) {
				inputThread.interrupt();
			}
		}
	}

	@Override
	public Future<Void> writeLater(ByteBuffer buffer) {
		try {
			return doWriteLater(buffer, getOutputStream());
		} catch (IOException e) {
			throw new IllegalStateException("Could not get output stream.", e);
		}
	}

	protected Thread pump(SshInput errInput, InputStream in) {
		if (in == null)
			throw new IllegalArgumentException("Stream must not be null.");
		ByteBuffer buffer = ByteBuffer.allocate(configuration.getStreamBufferSize());
		byte[] outBuffer = new byte[buffer.capacity()];
		Thread thread = new Thread("Pump" + errInput.toString()) {
			public void run() {
				int r;
				try {
					while ((r = in.read(outBuffer)) != -1) {
						buffer.put(outBuffer, 0, r);
						buffer.flip();
						errInput.read(buffer);
						buffer.clear();
					}
				} catch (IOException ioe) {
					errInput.onError(ioe);
				}
			}
		};
		thread.start();
		return thread;
	}

	protected Future<Void> doWriteLater(ByteBuffer buffer, OutputStream out) {
		int len = buffer.limit() - buffer.position();
		byte[] writeBuffer = new byte[len];
		buffer.get(writeBuffer);
		return new AbstractFuture<Void>() {
			{
				provider.getExecutor().execute(createRunnable());
			}

			@Override
			Void doFuture() throws Exception {
				out.write(writeBuffer);
				out.flush();
				return null;
			}
		};
	}
}