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
package net.sf.sshapi;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract implementation of an {@link SshLifecycleComponent}, providing some
 * command methods.
 */
public abstract class AbstractLifecycleComponent<L extends SshLifecycleListener<C>, C extends SshLifecycleComponent<L, C>> implements SshLifecycleComponent<L, C> {

	private List<L> listeners;

	public final synchronized void addListener(L listener) {
		if (listeners == null) {
			listeners = new ArrayList<>();
		}
		listeners.add(listener);
	}

	public final synchronized void removeListener(L listener) {
		if (listeners != null) {
			listeners.remove(listener);
		}
	}

	@Override
	public void closeQuietly() {
		try {
			close();
		} catch (Exception e) {
		}
	}

	/**
	 * Inform all listeners the component has opened.
	 */
	@SuppressWarnings("unchecked")
	protected void fireOpened() {
		if (listeners != null) {
			for (int i = listeners.size() - 1; i >= 0; i--)
				listeners.get(i).opened((C) this);
		}
	}

	/**
	 * Inform all listeners the component has closed.
	 */
	@SuppressWarnings("unchecked")
	protected void fireClosed() {
		if (listeners != null) {
			for (int i = listeners.size() - 1; i >= 0; i--)
				listeners.get(i).closed((C) this);
		}
	}

	/**
	 * Inform all listeners the component is closing.
	 */
	@SuppressWarnings("unchecked")
	protected void fireClosing() {
		if (listeners != null) {
			for (int i = listeners.size() - 1; i >= 0; i--)
				listeners.get(i).closing((C) this);
		}
	}
	
	protected List<L> getListeners() {
		return listeners;
	}
}
