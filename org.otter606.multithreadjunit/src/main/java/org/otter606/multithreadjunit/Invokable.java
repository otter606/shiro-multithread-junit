package org.otter606.multithreadjunit;
/**
 * Simple callback interface for supplying to a {@link SequencedRunnableRunner}.
 * @author radams
 *
 */
public interface Invokable {
	
		/**
		 * Runs any code.
		 * @throws Exception
		 */
		void invoke() throws Exception;
	

}
