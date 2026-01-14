package com.ullivada.zeropanic.applister.collectors;

import com.ullivada.zeropanic.applister.model.InstalledApp;
import com.ullivada.zeropanic.applister.util.CommandRunner;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Lists Debian/Ubuntu-installed packages.
 *
 * Notes:
 * - If available, uses "apt-mark showmanual" to approximate user-installed apps.
 * - Falls back to listing all installed packages via dpkg-query.
 */
public final class AptCollector implements InstalledAppCollector {
	@Override
	public String sourceName() {
		return "apt";
	}

	@Override
	public List<InstalledApp> collect() {
		if (!CommandRunner.isCommandAvailable("dpkg-query")) {
			return List.of();
		}

		Set<String> manualPackages = getManualPackagesOrEmpty();

		List<String> lines;
		try {
			// Format: package\tversion
			lines = CommandRunner.runLines(
					List.of("dpkg-query", "-W", "-f=${Package}\\t${Version}\\n"),
					Duration.ofSeconds(20)
			);
		} catch (Exception e) {
			throw new RuntimeException("Unable to run 'dpkg-query -W'", e);
		}

		List<InstalledApp> out = new ArrayList<>();
		for (String line : lines) {
			String trimmed = line.trim();
			if (trimmed.isEmpty()) {
				continue;
			}
			String[] parts = trimmed.split("\\t", 2);
			String pkg = parts[0].trim();
			if (pkg.isEmpty()) {
				continue;
			}

			if (!manualPackages.isEmpty() && !manualPackages.contains(pkg)) {
				continue;
			}

			Optional<String> version = Optional.empty();
			if (parts.length == 2) {
				String v = parts[1].trim();
				if (!v.isEmpty()) {
					version = Optional.of(v);
				}
			}

			out.add(new InstalledApp(pkg, sourceName(), Optional.of(pkg), version, Optional.empty()));
		}

		return out;
	}

	private Set<String> getManualPackagesOrEmpty() {
		if (!CommandRunner.isCommandAvailable("apt-mark")) {
			return Set.of();
		}

		try {
			List<String> lines = CommandRunner.runLines(List.of("apt-mark", "showmanual"), Duration.ofSeconds(10));
			Set<String> pkgs = new HashSet<>();
			for (String line : lines) {
				String pkg = line.trim().toLowerCase(Locale.ROOT);
				if (!pkg.isEmpty()) {
					pkgs.add(pkg);
				}
			}
			return pkgs;
		} catch (Exception e) {
			// best-effort; fall back to all installed packages
			return Set.of();
		}
	}
}
