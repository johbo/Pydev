/*
 * Author: atotic
 * Created on Apr 21, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model;


import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.model.IThread;
import org.python.pydev.debug.core.PydevDebugPlugin;

/**
 * ListThreads command.
 * 
 * See protocol for more info
 */
public class ListThreadsCommand extends RemoteDebuggerCommand {

	int sequence;
	boolean done;
	PyDebugTarget target;
	IThread[] threads;
	
	public ListThreadsCommand(RemoteDebugger debugger, PyDebugTarget target) {
		super(debugger);
		this.target = target;
		sequence = debugger.getNextSequence();
		done = false;
	}

	public void waitUntilDone(int timeout) throws InterruptedException {
		while (!done && timeout > 0) {
			timeout -= 100;
			Thread.sleep(100);
		}
		if (timeout < 0)
			throw new InterruptedException();
	}
	
	public IThread[] getThreads() {
		return threads;
	}
	
	public int getSequence() {
		return sequence;
	}
	
	public String getOutgoing() {
		return makeCommand(Integer.toString(CMD_LIST_THREADS), sequence, "");
	}
	
	public boolean needResponse() {
		return true;
	}
	
	/**
	 * The response is a list of threads
	 */
	public void processResponse(int cmdCode, String payload) {
		if (cmdCode != 102) {
			PydevDebugPlugin.log(IStatus.ERROR, "Unexpected response to LIST THREADS"  + payload, null);
			return;
		}
		threads = ModelUtils.ThreadsFromXML(target, payload);
		done = true;
	}

	
	public void processErrorResponse(int cmdCode, String payload) {
		PydevDebugPlugin.log(IStatus.ERROR, "LIST THREADS got an error "  + payload, null);
	}
}
