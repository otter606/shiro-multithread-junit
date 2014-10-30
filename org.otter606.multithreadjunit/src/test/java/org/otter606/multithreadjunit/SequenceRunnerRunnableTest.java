package org.otter606.multithreadjunit;

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.Factory;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.apache.shiro.mgt.SecurityManager;

/**
 * Example test cases for how to run multiple stepwise tests.
 * 
 * @author radams
 * 
 */

public class SequenceRunnerRunnableTest extends ShiroTestUtils {

	// ShiroTestUtils shiroUtils;
	static final Logger log = Logger.getLogger(SequencedRunnable.class);

	@BeforeClass
	public static void readShiroConfiguration() {
		Factory<SecurityManager> factory = new IniSecurityManagerFactory("classpath:shiro.ini");
		SecurityManager securityManager = factory.getInstance();
		SecurityUtils.setSecurityManager(securityManager);
		log.setLevel(Level.INFO);
	}

	@Before
	public void setUp() throws Exception {
		// shiroUtils = new ShiroTestUtils();

	}

	@After
	public void tearDown() throws Exception {

	}

	static Invokable createInvokable(final int i) {
		return new Invokable() {
			public void invoke() throws Exception {
				log.info(" invoking " + i);
			}
		};
	}

	private Subject doLogin(User user) {
		Subject subjectUnderTest = new Subject.Builder(SecurityUtils.getSecurityManager())
				.buildSubject();
		subjectUnderTest.login(new UsernamePasswordToken(user.getUsername(), user.getPassword()));
		setSubject(subjectUnderTest);
		return subjectUnderTest;
	}

	@Test
	public void testSequenceRunner() throws Exception {
		log.info("This test doesn't use Shiro for authentication, but shows a more " +
	      "complicated example of coordinating the threads.");
		Invokable[] invokables = new Invokable[20];
		for (int i = 0; i < 20; i++) {
			invokables[i] = createInvokable(i);
		}
		Map<String, Integer[]> config = new TreeMap<>();
		config.put("t1", new Integer[] { 0, 3, 5, 9, 11, 14, 17, 19 });
		config.put("t2", new Integer[] { 1, 4, 7, 10, 13, 15 });
		config.put("t3", new Integer[] { 2, 6, 8, 12, 16, 18 });
		SequencedRunnableRunner runner = new SequencedRunnableRunner(config, invokables);
		runner.runSequence();

	}

	@Test
	public void testUserActions() throws Exception {
		// initialise all the users we need before starting tests
		// shiro.ini file is used as simple Realm to authenticate.
		final User u1 = new User("user1", "password1");
		final User u2 = new User("user2", "password2");
		final User u3 = new User("user3", "password3");

		// Each invokable is just a callback function that will execute an atomic sequence of
		// operations
		// in a single thread. While an invokable is running, all other threads are stopped.
		// We define all the invokables up front, before running any.
		// The first invokable for a user should log them in.
		Invokable[] invokables = new Invokable[6];
		invokables[0] = new Invokable() {
			// annotate these inner class methods with @Test
			@Test
			public void invoke() throws Exception {
				log.info("logging in user1");
				doLogin(u1);
				log.info(" user1 doing some actions..");
				log.info(" user1 pausing but still logged in.");
			}
		};
		invokables[1] = new Invokable() {
			public void invoke() throws Exception {
				log.info("logging in user2");
				doLogin(u2);
				log.info(" user2 doing some actions..");
				// some action
				log.info(" user2 pausing but still logged in.");

			}
		};
		invokables[2] = new Invokable() {
			public void invoke() throws Exception {
				log.info("logging in user3");
				doLogin(u3);
				// some action
				log.info(" user3 doing some actions..");
				log.info(" user3 pausing but still logged in.");
			}
		};

		// the last invokables for a user should remember to log them out.
		invokables[3] = new Invokable() {
			public void invoke() throws Exception {
				SecurityUtils.getSubject().logout();
				log.info(" user1 logged out .");
			}
		};
		invokables[4] = new Invokable() {
			public void invoke() throws Exception {

				SecurityUtils.getSubject().logout();
				log.info(" user3 logged out .");

			}
		};
		invokables[5] = new Invokable() {
			public void invoke() throws Exception {
				SecurityUtils.getSubject().logout();
				log.info(" user2 logged out .");
			}
		};
		// the configuration defines how many threads we want.
		// All operations performed by a user should occur in a single thread
		Map<String, Integer[]> config = new TreeMap<>();
		// these are the array indices of the Invokable [].
		config.put("t1", new Integer[] { 0, 3 });
		config.put("t2", new Integer[] { 1, 5, });
		config.put("t3", new Integer[] { 2, 4 });
		SequencedRunnableRunner runner = new SequencedRunnableRunner(config, invokables);
		runner.runSequence();

		log.info("Finished");

	}

}
