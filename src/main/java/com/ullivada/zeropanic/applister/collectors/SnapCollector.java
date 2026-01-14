package com.ullivada.zeropanic.applister.collectors;

import com.ullivada.zeropanic.applister.model.InstalledApp;
import com.ullivada.zeropanic.applister.util.CommandRunner;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SnapCollector implements InstalledAppCollector {
	@Override
	public String sourceName() {
		return "snap";
	}

	@Override
	public List<InstalledApp> collect() {
		if (!CommandRunner.isCommandAvailable("snap")) {
			return List.of();
		}

		// snap list output columns: Name Version Rev Tracking Publisher Notes
		List<String> lines;
		try {
			lines = CommandRunner.runLines(List.of("snap", "list"), Duration.ofSeconds(10));
		} catch (Exception e) {
			throw new RuntimeException("Unable to run 'snap list'", e);
		}

		List<InstalledApp> out = new ArrayList<>();
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			if (i == 0) {
				continue; // header
			}
			String trimmed = line.trim();
			if (trimmed.isEmpty()) {
				continue;
			}

			String[] parts = trimmed.split("\\s+");
			if (parts.length < 2) {
				continue;
			}

			String name = parts[0];
			String version = parts[1];
			out.add(new InstalledApp(name, sourceName(), Optional.of(name), Optional.of(version), Optional.empty()));
		}

		return out;
	}
}
