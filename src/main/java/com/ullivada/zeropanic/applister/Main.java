package com.ullivada.zeropanic.applister;

import com.ullivada.zeropanic.applister.collectors.DesktopEntryCollector;
import com.ullivada.zeropanic.applister.collectors.FlatpakCollector;
import com.ullivada.zeropanic.applister.collectors.InstalledAppCollector;
import com.ullivada.zeropanic.applister.collectors.AptCollector;
import com.ullivada.zeropanic.applister.collectors.SnapCollector;
import com.ullivada.zeropanic.applister.collectors.WindowsRegistryCollector;
import com.ullivada.zeropanic.applister.format.PlainTextFormatter;
import com.ullivada.zeropanic.applister.model.InstalledApp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class Main {
	public static void main(String[] args) {
		// All collectors - they auto-detect OS and skip if not applicable
		List<InstalledAppCollector> collectors = List.of(
				new SnapCollector(),
				new FlatpakCollector(),
				new AptCollector(),
				new DesktopEntryCollector(),
				new WindowsRegistryCollector()
		);

		List<InstalledApp> allApps = new ArrayList<>();
		for (InstalledAppCollector collector : collectors) {
			try {
				allApps.addAll(collector.collect());
			} catch (Exception e) {
				// Keep best-effort behavior for CLI usage.
				System.err.println("[warn] " + collector.sourceName() + ": " + e.getMessage());
			}
		}

		Map<String, List<InstalledApp>> bySource = groupBySource(allApps);
		System.out.print(PlainTextFormatter.format(bySource));
	}

	private static Map<String, List<InstalledApp>> groupBySource(List<InstalledApp> apps) {
		Map<String, List<InstalledApp>> bySource = new LinkedHashMap<>();
		for (InstalledApp app : apps) {
			bySource.computeIfAbsent(app.source(), _k -> new ArrayList<>()).add(app);
		}

		Comparator<InstalledApp> byName = Comparator.comparing(
				app -> app.name().toLowerCase(Locale.ROOT)
		);

		for (Map.Entry<String, List<InstalledApp>> entry : bySource.entrySet()) {
			List<InstalledApp> sorted = entry.getValue().stream().sorted(byName).toList();
			entry.setValue(dedupByName(sorted));
		}

		return bySource;
	}

	private static List<InstalledApp> dedupByName(List<InstalledApp> sortedApps) {
		Set<String> seen = new LinkedHashSet<>();
		List<InstalledApp> out = new ArrayList<>();
		for (InstalledApp app : sortedApps) {
			String key = app.name().trim().toLowerCase(Locale.ROOT);
			if (key.isEmpty()) {
				continue;
			}
			if (seen.add(key)) {
				out.add(app);
			}
		}
		return out;
	}
}
