package com.ullivada.zeropanic.applister.collectors;

import com.ullivada.zeropanic.applister.model.InstalledApp;
import com.ullivada.zeropanic.applister.util.CommandRunner;
import com.ullivada.zeropanic.applister.util.OSDetector;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class FlatpakCollector implements InstalledAppCollector {
	@Override
	public String sourceName() {
		return "flatpak";
	}

	@Override
	public List<InstalledApp> collect() {
		if (OSDetector.isWindows() || !CommandRunner.isCommandAvailable("flatpak")) {
			return List.of();
		}

		List<String> lines;
		try {
			// tab-separated output by default
			lines = CommandRunner.runLines(
					List.of("flatpak", "list", "--app", "--columns=application,name,version,origin"),
					Duration.ofSeconds(10)
			);
		} catch (Exception e) {
			throw new RuntimeException("Unable to run 'flatpak list'", e);
		}

		List<InstalledApp> out = new ArrayList<>();
		for (String line : lines) {
			String trimmed = line.trim();
			if (trimmed.isEmpty()) {
				continue;
			}

			String[] parts = trimmed.split("\\t");
			if (parts.length < 2) {
				// Some installations may not be tab-separated; fall back to multiple spaces.
				parts = trimmed.split("\\s{2,}");
			}
			if (parts.length < 2) {
				continue;
			}

			String appId = parts[0].trim();
			String name = parts[1].trim();
			Optional<String> version = parts.length >= 3 && !parts[2].trim().isEmpty() ? Optional.of(parts[2].trim()) : Optional.empty();
			Optional<String> origin = parts.length >= 4 && !parts[3].trim().isEmpty() ? Optional.of(parts[3].trim()) : Optional.empty();

			out.add(new InstalledApp(name.isEmpty() ? appId : name, sourceName(), Optional.of(appId), version, origin));
		}

		return out;
	}
}
