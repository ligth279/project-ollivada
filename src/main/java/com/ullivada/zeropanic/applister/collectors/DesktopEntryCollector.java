package com.ullivada.zeropanic.applister.collectors;

import com.ullivada.zeropanic.applister.model.InstalledApp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public final class DesktopEntryCollector implements InstalledAppCollector {
	@Override
	public String sourceName() {
		return "desktop";
	}

	@Override
	public List<InstalledApp> collect() {
		if (isWindows()) {
			return List.of();
		}
		
		List<Path> roots = new ArrayList<>();
		
		// Use XDG_DATA_DIRS standard (portable across all Linux distros)
		String xdgDataDirs = System.getenv("XDG_DATA_DIRS");
		if (xdgDataDirs != null && !xdgDataDirs.isBlank()) {
			for (String dir : xdgDataDirs.split(":")) {
				if (!dir.isBlank()) {
					roots.add(Path.of(dir.trim(), "applications"));
				}
			}
		} else {
			// Fallback if XDG_DATA_DIRS not set
			roots.add(Path.of("/usr/share/applications"));
			roots.add(Path.of("/usr/local/share/applications"));
		}

		// Flatpak sandbox: host apps are at /run/host/usr/share (not /run/host/share)
		roots.add(Path.of("/run/host/usr/share/applications"));

		// User-specific directories
		String home = System.getenv("HOME");
		if (home != null && !home.isBlank()) {
			roots.add(Path.of(home, ".local", "share", "applications"));
		}

		// Snap/Flatpak export locations
		roots.add(Path.of("/var/lib/snapd/desktop/applications"));
		roots.add(Path.of("/var/lib/flatpak/exports/share/applications"));
		if (home != null && !home.isBlank()) {
			roots.add(Path.of(home, ".local", "share", "flatpak", "exports", "share", "applications"));
		}

		List<InstalledApp> out = new ArrayList<>();
		for (Path root : roots) {
			if (!Files.isDirectory(root)) {
				continue;
			}

			try (Stream<Path> files = Files.walk(root)) {
				files
						.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".desktop"))
						.forEach(p -> parseDesktopFile(p).ifPresent(out::add));
			} catch (IOException e) {
				// best-effort
			}
		}

		return out;
	}

	private boolean isWindows() {
		String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		return os.contains("win");
	}

	private Optional<InstalledApp> parseDesktopFile(Path path) {
		Map<String, String> kv;
		try {
			kv = readDesktopEntrySection(path);
		} catch (IOException e) {
			return Optional.empty();
		}

		String type = kv.getOrDefault("Type", "");
		if (!type.equalsIgnoreCase("Application")) {
			return Optional.empty();
		}
		// For "all installed apps" use-cases, include NoDisplay entries too.
		// Hidden=true usually means the entry is explicitly disabled.
		if (isTrue(kv.get("Hidden"))) {
			return Optional.empty();
		}

		String name = kv.getOrDefault("Name", "").trim();
		if (name.isEmpty()) {
			return Optional.empty();
		}

		String id = path.getFileName().toString();
		String exec = kv.getOrDefault("Exec", "").trim();
		return Optional.of(new InstalledApp(name, sourceName(), Optional.of(id), Optional.empty(), exec.isEmpty() ? Optional.empty() : Optional.of(exec)));
	}

	private static boolean isTrue(String value) {
		if (value == null) return false;
		String v = value.trim().toLowerCase(Locale.ROOT);
		return v.equals("true") || v.equals("1") || v.equals("yes");
	}

	private static Map<String, String> readDesktopEntrySection(Path path) throws IOException {
		List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
		Map<String, String> out = new HashMap<>();

		boolean inSection = false;
		for (String raw : lines) {
			String line = raw.trim();
			if (line.isEmpty() || line.startsWith("#")) {
				continue;
			}
			if (line.startsWith("[") && line.endsWith("]")) {
				inSection = line.equalsIgnoreCase("[Desktop Entry]");
				continue;
			}
			if (!inSection) {
				continue;
			}

			int eq = line.indexOf('=');
			if (eq <= 0) {
				continue;
			}

			String key = line.substring(0, eq).trim();
			// Ignore localized keys like Name[en_US]
			if (key.contains("[")) {
				continue;
			}
			String value = line.substring(eq + 1).trim();
			out.putIfAbsent(key, value);
		}

		return out;
	}
}
