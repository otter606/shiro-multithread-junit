package org.otter606.multithreadjunit;

import java.util.concurrent.CountDownLatch;

import org.apache.log4j.Logger;

/**
 * Runs an {@link Invokable} action in a thread.
 * @author radams
 *
 */
class SequencedRunnable implements Runnable {
	Logger log = Logger.getLogger(SequencedRunnable.class);
	private final String name;

	private final CountDownLatch[] toWaitFor;
	private final Invokable[] actions;
	final Integer[] sequence;

	/**
	 * 
	 * @param name
	 *            The name of the thread
	 * @param conds
	 *            An array of {@link CountDownLatch}, all with count of 1
	 * @param actions
	 *            An array of invokable actions
	 * @param sequence
	 *            An integer array of indices of actions to run in this thread.
	 * @throws IllegalArgumentException
	 *             if: <code>conds</code> and <code>actions</code> differ in length, if values in
	 *             <code>sequence</code> aren't valid array indices into <code>conds</code> and
	 *             <code>actions</code>, or if array indices aren't sorted in increasing order.
	 */
	public SequencedRunnable(String name, CountDownLatch[] conds, Invokable[] actions,
			Integer[] sequence) {
	
		validateArgs(name, conds, actions, sequence);
		this.toWaitFor = conds;
		this.actions = actions;
		this.name = name;
		this.sequence = sequence;
	}

	private void validateArgs(String name, CountDownLatch[] latches, Invokable[] actions,
			Integer[] sequence) {
		// must be an action for each latch
		if (latches.length != actions.length) {
			throw new IllegalArgumentException("In thread " + name
					+ " there must be the same number of conditions as actions, but was "
					+ latches.length + " and " + actions.length + " respectively");
		}
		for (int index = 0; index < sequence.length; index++) {
			// array indices must be valid
			if (sequence[index] < 0 || sequence[index] >= actions.length) {
				throw new IllegalArgumentException(
						"In thread "
								+ name
								+ ", values  in  sequence [] must be array indices for Conditions []. But ["
								+ index + "] lies outside the range [0," + latches.length + "]");
			}
			if (index >= 1) {
				// can't be consecutive
				if (sequence[index] - sequence[index - 1] == 1) {
					throw new IllegalArgumentException("Thread " + name
							+ " can't have consecutive indices [" + sequence[index - 1] + ","
							+ sequence[index] + "]. Combine into a single action.");
				}
				// sequence of invokables must be ordered.
				if (sequence[index] - sequence[index - 1] < 1) {
					throw new IllegalArgumentException("Thread " + name
							+ " invocation sequence must be an increasing sequence but contains ["
							+ sequence[index - 1] + "," + sequence[index] + "]");
				}
			}
		}
	}

    
	public void run() {
		try {
			for (int i = 0; i < sequence.length; i++) {
				int toWaitForIndx = sequence[i];
				try {
					log.debug(name + ": waiting for event " + toWaitForIndx);
					toWaitFor[toWaitForIndx].await();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				log.debug(name + ": invoking action " + toWaitForIndx);
				actions[toWaitForIndx].invoke();
				if (toWaitForIndx < toWaitFor.length - 1) {
					log.debug(name + "counting down for next latch " + (toWaitForIndx + 1));
					toWaitFor[++toWaitForIndx].countDown();
				} else
					log.debug(name + " executed last invokable!");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}