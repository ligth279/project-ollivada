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
import com.ullivada.zeropanic.applister.supabase.SupabaseService;

import java.io.Console;
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
import java.util.Scanner;
import java.util.Set;

public final class Main {
	private static final int SCAN_INTERVAL_HOURS = 20;
	private static final int SUPABASE_CHECK_INTERVAL_HOURS = 5;

	public static void main(String[] args) {
		try {
			ensureDatabaseDirectory();
			
			try (DatabaseService db = new DatabaseService()) {
				// Authentication
				if (!authenticate(db)) {
					System.err.println("[Error] Authentication failed");
					System.exit(1);
				}
				
				List<InstalledApp> apps;
				
				// Check if we scanned recently
				if (db.wasScannedWithinHours(SCAN_INTERVAL_HOURS)) {
					System.out.println("[Info] Using cached data (last scan < " + SCAN_INTERVAL_HOURS + " hours ago)\n");
					apps = db.getAllApps();
				} else {
					// Perform fresh scan
					System.out.println("[Info] Performing fresh scan...\n");
					apps = scanSystem();
					db.updateApps(apps);
				}
				
				// Check Supabase for threats (every 5 hours)
				if (!db.wasSupabaseCheckedWithinHours(SUPABASE_CHECK_INTERVAL_HOURS)) {
					checkSupabaseThreats(apps, db);
				}
				
				// Print results
				Map<String, List<InstalledApp>> bySource = groupBySource(apps);
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

	private static boolean authenticate(DatabaseService db) throws SQLException {
		Scanner scanner = new Scanner(System.in);
		Console console = System.console();
		
		System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
		System.out.println("         App Lister - Authentication");
		System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
		System.out.println();
		
		System.out.print("Username: ");
		String username = scanner.nextLine().trim();
		
		if (username.isEmpty()) {
			System.err.println("Username cannot be empty");
			return false;
		}
		
		// Read password (hidden if console available)
		String password;
		if (console != null) {
			char[] passwordChars = console.readPassword("Password: ");
			password = new String(passwordChars);
		} else {
			System.out.print("Password: ");
			password = scanner.nextLine();
		}
		
		if (password.isEmpty()) {
			System.err.println("Password cannot be empty");
			return false;
		}
		
		// Check if user exists
		boolean userExists = db.userExists(username);
		
		if (userExists) {
			// Existing user - authenticate
			if (db.authenticate(username, password)) {
				System.out.println("✓ Authentication successful\n");
				return true;
			} else {
				System.err.println("✗ Invalid password");
				return false;
			}
		} else {
			// New user - register
			System.out.println("[Info] User not found. Creating new account...");
			System.out.print("Confirm password: ");
			String confirmPassword;
			if (console != null) {
				char[] confirmChars = console.readPassword("");
				confirmPassword = new String(confirmChars);
			} else {
				confirmPassword = scanner.nextLine();
			}
			
			if (!password.equals(confirmPassword)) {
				System.err.println("Passwords do not match");
				return false;
			}
			
			db.createUser(username, password);
			System.out.println("✓ Account created successfully\n");
			return true;
		}
	}

	private static void checkSupabaseThreats(List<InstalledApp> localApps, DatabaseService db) {
		try {
			SupabaseService supabase = new SupabaseService();
			
			if (!supabase.isConfigured()) {
				// Supabase not configured, skip silently
				return;
			}
			
			List<SupabaseService.ThreatMatch> threats = supabase.getActiveThreats();
			
			// Match threats to installed apps (case-insensitive)
			Set<String> localAppNames = localApps.stream()
				.map(app -> app.name().toLowerCase(Locale.ROOT))
				.collect(java.util.stream.Collectors.toSet());
			
			boolean foundMatch = false;
			for (SupabaseService.ThreatMatch threat : threats) {
				String targetAppLower = threat.targetApp().toLowerCase(Locale.ROOT);
				if (localAppNames.contains(targetAppLower)) {
					if (!foundMatch) {
						System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
						System.out.println("⚠️  SECURITY ALERTS");
						System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
						foundMatch = true;
					}
					
					System.out.println();
					System.out.println(threat.getSeverityIcon() + " " + threat.title());
					System.out.println("   App: " + threat.targetApp());
					if (threat.severity() != null) {
						System.out.println("   Severity: " + threat.severity());
					}
					if (threat.description() != null && !threat.description().isBlank()) {
						System.out.println("   Details: " + threat.description());
					}
					if (threat.url() != null && !threat.url().isBlank()) {
						System.out.println("   🔗 URL: " + threat.url());
					}
				}
			}
			
			if (foundMatch) {
				System.out.println();
				System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
				System.out.println();
			}
			
			// Update check timestamp
			db.updateSupabaseCheckTimestamp();
			
		} catch (Exception e) {
			System.err.println("[Warn] Supabase check failed: " + e.getMessage());
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
