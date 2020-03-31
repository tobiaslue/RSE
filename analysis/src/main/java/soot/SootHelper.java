package soot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.rse.utils.Configuration;
import ch.ethz.rse.verify.ClassToVerify;
import soot.options.Options;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;

/**
 * Helper for Soot-related tasks
 *
 * DO NOT MODIFY THIS FILE
 */
public class SootHelper {

	private static final Logger logger = LoggerFactory.getLogger(SootHelper.class);

	/**
	 * Load the referenced class and all related classes
	 * 
	 * @param c the class to test
	 * @return the Soot representation of c
	 */
	public static SootClass loadClass(ClassToVerify c) {
		// reset previously loaded classes (important for consecutive analysis)
		logger.info("Resetting Soot.");
		G.reset();

		// Loading classes follows this example:
		// https://github.com/Sable/heros/wiki/Example:-Using-Heros-with-Soot

		// construct classpath to use when loading the examples
		String javaHome = Configuration.props.getSootJavaHome();
		if (javaHome == null) {
			logger.error("JAVA_HOME not set: " + javaHome);
		}
		String rt = javaHome + "/jre/lib/rt.jar";
		String jce = javaHome + "/jre/lib/jce.jar";
		String classpath = c.getClassPath().toString() + ":" + rt + ":" + jce;
		// set classpath
		logger.debug("Soot classpath:" + classpath);
		Options.v().set_soot_classpath(classpath);

		// Enable whole-program mode
		Options.v().set_whole_program(true);
		Options.v().set_app(true);

		// produce more detailed output (helpful for debugging purposes)
		Options.v().set_verbose(true);

		// Enable SPARK call-graph construction
		// Documentation of options:
		// https://soot-build.cs.uni-paderborn.de/public/origin/master/soot/soot-master/3.0.0/options/soot_options.htm#phase_5_2
		Options.v().setPhaseOption("cg", "on");
		Options.v().setPhaseOption("cg.spark", "on");
		Options.v().setPhaseOption("cg.spark", "enabled:true");
		Options.v().setPhaseOption("cg.spark", "verbose:true");
		Options.v().setPhaseOption("cg.spark", "on-fly-cg:true");
		Options.v().setPhaseOption("cg.spark", "apponly:true");

		// SPARK requires jimple format
		// Helpful source: https://github.com/Sable/soot/issues/332
		Options.v().set_output_format(Options.output_format_jimple);

		// load the class
		logger.info("Loading {} into Soot", c.getPackageName());
		SootClass sc = Scene.v().loadClassAndSupport(c.getPackageName());
		sc.setApplicationClass();

		Scene.v().loadNecessaryClasses();

		Scene.v().setEntryPoints(sc.getMethods());

		// run SPARK call-graph construction
		logger.info("Running call-graph construction");
		PackManager.v().runPacks();
		logger.info("Finished call-graph construction");

		return sc;
	}

	public static final boolean isIntValue(Value val) {
		// sometimes, Soot represents integers as short or byte
		// For example: "int i = 10"
		return val.getType().toString().equals("int") || val.getType().toString().equals("short")
				|| val.getType().toString().equals("byte");
	}

	public final static UnitGraph getUnitGraph(SootMethod method) {
		Body b = method.retrieveActiveBody();
		logger.debug("Analysing:\n" + b);
		UnitGraph g = new BriefUnitGraph(b);
		return g;
	}
}
