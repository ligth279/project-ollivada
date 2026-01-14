package com.ullivada.zeropanic.applister;

import com.ullivada.zeropanic.applister.collectors.DesktopEntryCollector;
import com.ullivada.zeropanic.applister.collectors.FlatpakCollector;
import com.ullivada.zeropanic.applister.collectors.InstalledAppCollector;
import com.ullivada.zeropanic.applister.collectors.AptCollector;
import com.ullivada.zeropanic.applister.collectors.SnapCollector;
import com.ullivada.zeropanic.applister.collectors.WindowsRegistryCollector;
import com.ullivada.zeropanic.applister.database.DatabaseService;
import com.ullivada.zeropanic.applister.format.PlainTextFormatter;
import com.ullivada.zeropanic.applister.model.InstalledApp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class Main {
	private static final int SCAN_INTERVAL_HOURS = 20;

	public static void main(String[] args) {
		try {
			ensureDatabaseDirectory();
			
			try (DatabaseService db = new DatabaseService()) {
				// Check if we scanned recently
				if (db.wasScannedWithinHours(SCAN_INTERVAL_HOURS)) {
					System.out.println("[Info] Using cached data (last scan < " + SCAN_INTERVAL_HOURS + " hours ago)\n");
					List<InstalledApp> cachedApps = db.getAllApps();
					Map<String, List<InstalledApp>> bySource = groupBySource(cachedApps);
					System.out.print(PlainTextFormatter.format(bySource));
					return;
				}
				
				// Perform fresh scan
				System.out.println("[Info] Performing fresh scan...\n");
				List<InstalledApp> freshApps = scanSystem();
				
				// Update database
				db.updateApps(freshApps);
				
				// Print results
				Map<String, List<InstalledApp>> bySource = groupBySource(freshApps);
				System.out.print(PlainTextFormatter.format(bySource));
			}
		} catch (SQLException e) {
			System.err.println("[Error] Database error: " + e.getMessage());
			System.exit(1);
		} catch (IOException e) {
			System.err.println("[Error] I/O error: " + e.getMessage());
			System.exit(1);
		}
	}

	private static void ensureDatabaseDirectory() throws IOException {
		String userHome = System.getProperty("user.home");
		Path dbDir = Paths.get(userHome, ".applister");
		if (!Files.exists(dbDir)) {
			Files.createDirectories(dbDir);
		}
	}

	private static List<InstalledApp> scanSystem() {
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
		return allApps;
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
