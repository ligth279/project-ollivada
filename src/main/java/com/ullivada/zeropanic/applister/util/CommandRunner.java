package com.ullivada.zeropanic.applister.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class CommandRunner {
	private CommandRunner() {
	}

	public static boolean isCommandAvailable(String command) {
		try {
			// On Windows, use 'where' command; on Unix, use 'command -v'
			if (OSDetector.isWindows()) {
				Process p = new ProcessBuilder("cmd.exe", "/c", "where", command)
						.redirectErrorStream(true)
						.start();
				return p.waitFor() == 0;
			} else {
				Process p = new ProcessBuilder("sh", "-lc", "command -v " + escapeSh(command) + " >/dev/null 2>&1")
						.redirectErrorStream(true)
						.start();
				return p.waitFor() == 0;
			}
		} catch (Exception e) {
			return false;
		}
	}

	public static List<String> runLines(List<String> command, Duration timeout) throws IOException, InterruptedException {
		ProcessBuilder pb = new ProcessBuilder(command);
		pb.redirectErrorStream(true);
		Process process = pb.start();

		List<String> lines = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				lines.add(line);
			}
		}

		boolean finished = process.waitFor(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
		if (!finished) {
			process.destroyForcibly();
			throw new IOException("Command timed out: " + String.join(" ", command));
		}

		int exit = process.exitValue();
		if (exit != 0) {
			throw new IOException("Command failed (exit " + exit + "): " + String.join(" ", command));
		}

		return lines;
	}

	private static String escapeSh(String s) {
		return s.replace("\"", "\\\"");
	}
}
