package com.example.javaloggingframeworktest;

import java.util.Date;

import com.example.javaloggingframeworktest.runners.BaseRunner;
import com.example.javaloggingframeworktest.runners.JUtilRunner;
import com.example.javaloggingframeworktest.runners.Log4j2Runner;
import com.example.javaloggingframeworktest.runners.LogbackRunner;

public class BenchMark {

	public void run(String[] args) {
		int numIterations = Integer.parseInt(args[0]);
		int runsPerIteration = Integer.parseInt(args[1]);
		String framework = args[2];
		String output = args[3];
		boolean async = isAsync(args);

		System.out.println(String.format("Framework: %2$s\nOutput: %3$s\nAsync: %4$s\nIterations: %5$s\nRuns per Iteration: %1$s\n", runsPerIteration, framework, output, async, numIterations));

		BaseRunner runner = initializeRunner(framework, output, async);
		runner.warmup(runsPerIteration);
		runTest(runner, numIterations, runsPerIteration);
	}

	private boolean isAsync(String[] args) {
		return args.length > 4 && args[4].toLowerCase().contains("async");
	}

	private BaseRunner initializeRunner(String framework, String output, boolean async) {
		BaseRunner runner;
		String configFile = System.getProperty("user.dir") + "/build/resources/main";
		// Use async loggers (Log4j2 only)
		if (framework.toLowerCase().contains("log4j2") && framework.toLowerCase().contains("async")) {
			System.setProperty("Log4jContextSelector", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");

			// Increase asynchronous logger buffer size to 1M messages
			System.setProperty("AsyncLogger.RingBufferSize", "1048576");

			// Use a config file with location enabled?
			if (framework.toLowerCase().contains("location")) {
				configFile += "/async-loggers/location";
			}
			else {
				configFile += "/async-loggers/nolocation";
			}
		} else {
			// Use traditional loggers with sync or async appenders
			if (async) {
				configFile += "/async";
			} else {
				configFile += "/sync";
			}
		}

		if (framework.toLowerCase().contains("log4j2")) {
			configFile += String.format("/log4j2-%s.xml", output);
			Log4j2Runner.setConfigurationFile(configFile);
			runner = new Log4j2Runner();
		}
		else if (framework.toLowerCase().contains("logback")) {
			configFile += String.format("/logback-%s.xml", output);
			LogbackRunner.setConfigurationFile(configFile);
			runner = new LogbackRunner();
		}
		else {	// Assume java.util.logging
			configFile += String.format("/java.util.logging-%2$s.properties", framework, output);
			JUtilRunner.setConfigurationFile(configFile);
			runner = new JUtilRunner();
		}

		System.out.println(String.format("Configured framework using file: %s", configFile));
		return runner;
	}

	private void runTest(BaseRunner runner, int numIterations, int runsPerIteration) {
		Date start, end;

		// Start background stress test
		int stressThreads = Runtime.getRuntime().availableProcessors();
		Stresser stresser = new Stresser(stressThreads);
		stresser.start();

		start = new Date();
		System.out.println(String.format("Starting test at %1$tT.%1$tL", start));

		// Run logging calls
		for (int iteration = 1; iteration <= numIterations; iteration++) {
			System.out.println(String.format("Starting iteration %1$s.", iteration));
			runner.run(iteration, runsPerIteration);
			System.out.println(String.format("Finished iteration %1$s.", iteration));
		}

		// End the test
		stresser.stop();
		end = new Date();
		System.out.println(String.format("Test finished at %1$tT.%1$tL", end));

		// Print runtime
		long runtime = end.getTime() - start.getTime();
		System.out.println(String.format("Total runtime: %1$sms", runtime));
	}
}
