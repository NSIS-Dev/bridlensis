package bridlensis;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Scanner;

import bridlensis.env.DefaultNameGenerator;
import bridlensis.env.Environment;
import bridlensis.env.NameGenerator;

public class MakeBridleNSIS {

	private static final int EXIT_OUTDIRERROR = 10;
	private static final int EXIT_MAKEBRIDLENSISERROR = 11;
	private static final int EXIT_MAKENSISERROR = 12;

	private static final String VERSION;

	static {
		Scanner versionFileScanner = new Scanner(MakeBridleNSIS.class
				.getClassLoader().getResourceAsStream("bridlensis/VERSION"));
		VERSION = versionFileScanner.nextLine();
		versionFileScanner.close();
	}

	private static PrintStream stdout = System.out;

	private static class Arguments {

		File inputFile = null;
		String encoding = System.getProperty("file.encoding");
		String nsisHome = null;
		File outDir = null;
		ArrayList<String> nsisOptions = new ArrayList<>();
		ArrayList<String> excludeFiles = new ArrayList<>();

		static Arguments parse(String[] args) {
			Arguments arguments = new Arguments();
			for (int i = 0; i < args.length; i++) {
				if (args[i].equals("-n")) {
					arguments.nsisHome = args[++i];
				} else if (args[i].equals("-o")) {
					arguments.outDir = new File(args[++i]);
				} else if (args[i].equals("-e")) {
					arguments.encoding = args[++i];
				} else if (args[i].equals("-x")) {
					arguments.excludeFiles.addAll(Arrays.asList(args[++i]
							.split(":")));
				} else if (args[i].startsWith("/")) {
					arguments.nsisOptions.add(args[i]);
				} else {
					arguments.inputFile = new File(args[i]);
				}
			}
			return arguments;
		}

	}

	public static void main(String[] args) {
		stdout.print("BridleNSIS v");
		stdout.print(VERSION);
		stdout.println(" - Copyright (c) 2014 Contributors");
		stdout.println("See the User Manual for license details and credits.");
		stdout.println();

		if (args.length == 0) {
			usage();
			System.exit(0);
		}

		Arguments arguments = Arguments.parse(args);

		if (arguments.inputFile == null) {
			usage();
			System.exit(0);
			return;
		}

		if (arguments.outDir == null) {
			File parent = arguments.inputFile.getAbsoluteFile().getParentFile();
			if (parent == null) {
				stdout.println("Unable to resolve outdir based on input file "
						+ arguments.inputFile.getAbsolutePath());
				System.exit(EXIT_OUTDIRERROR);
				return;
			}
			arguments.outDir = parent;
		}

		if (!arguments.outDir.mkdir() && !arguments.outDir.exists()
				&& !arguments.outDir.isDirectory()) {
			stdout.println("Unable to create outdir "
					+ arguments.outDir.getAbsolutePath());
			System.exit(EXIT_OUTDIRERROR);
			return;
		}

		File outputFile = new File(arguments.outDir,
				getBridleNSISFileName(arguments.inputFile.getName()));

		try {
			makeBridleNSIS(new DefaultNameGenerator(), arguments.inputFile,
					outputFile, arguments.encoding, arguments.excludeFiles);
		} catch (IOException | ParserException e) {
			stdout.println(e.getMessage());
			System.exit(EXIT_MAKEBRIDLENSISERROR);
			return;
		}

		int exitCode = 0xFFFF;
		try {
			exitCode = makeNSIS(outputFile.getAbsolutePath(),
					arguments.nsisHome, arguments.nsisOptions);
		} catch (IOException | InterruptedException e) {
			stdout.println("Unable to run makensis.exe: " + e.getMessage());
			System.exit(EXIT_MAKENSISERROR);
			return;
		}
		System.exit(exitCode);
	}

	static String getBridleNSISFileName(String inputFileName) {
		String outputFileName;
		int fileExtIndex = inputFileName.lastIndexOf('.');
		if (fileExtIndex != -1) {
			outputFileName = inputFileName.substring(0, fileExtIndex) + ".b"
					+ inputFileName.substring(fileExtIndex + 1);
		} else {
			outputFileName = inputFileName + ".bnsi";
		}
		return outputFileName;
	}

	private static void usage() {
		stdout.println("Usage:");
		stdout.println("  java -jar bridlensis-" + VERSION
				+ ".jar [-n <NSIS home>] [-o <outdir>] <file> [<NSIS options>]");
	}

	protected static void makeBridleNSIS(NameGenerator nameGenerator,
			File inputFile, File outputFile, String encoding,
			Collection<String> excludeFiles) throws IOException,
			ParserException {
		if (outputFile.equals(inputFile)) {
			throw new IOException("Cannot override input file");
		}

		File baseDir = inputFile.getParentFile();
		File outDir = outputFile.getParentFile();

		stdout.println("Output: " + outDir.getAbsolutePath());
		stdout.println("Encoding: " + encoding);
		stdout.println();

		Environment environment = new Environment(nameGenerator);
		environment.loadBuiltinVariables();
		environment.loadBuiltinInstructions();
		Parser parser = new Parser(environment, baseDir, outDir, encoding,
				excludeFiles);

		long time = System.currentTimeMillis();
		parser.parse(inputFile.getName(), outputFile.getName());
		time = System.currentTimeMillis() - time;
		time = time < 1000 ? 1 : time / 1000;

		stdout.println(String.format(
				"\nParsed in %d seconds total of %d lines in %d file(s).\n",
				time, parser.getInputLines(), parser.getFileCount()));
	}

	private static int makeNSIS(String filename, String nsisHome,
			Collection<String> nsisOptions) throws IOException,
			InterruptedException {
		int exitCode;
		String exec = "makensis.exe";
		if (nsisHome != null && !nsisHome.isEmpty()) {
			exec = new File(nsisHome, exec).getAbsolutePath();
		}

		ArrayList<String> cmd = new ArrayList<String>();
		cmd.add(exec);

		stdout.print("Command: ");
		stdout.print("\"" + exec + "\" ");

		for (String option : nsisOptions) {
			cmd.add(option);
			stdout.print("\"" + option + "\" ");
		}

		cmd.add(filename);

		stdout.println("\"" + filename + "\"");
		stdout.println("\nMakeNSIS ---->\n");

		ProcessBuilder builder = new ProcessBuilder(cmd);
		builder.redirectErrorStream(true);
		Process process = builder.start();

		InputStream processInput = process.getInputStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				processInput));
		String line;
		while ((line = reader.readLine()) != null) {
			System.out.println(line);
		}

		exitCode = process.waitFor();
		return exitCode;
	}

}
