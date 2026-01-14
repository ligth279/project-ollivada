package com.ullivada.zeropanic.applister.collectors;

import com.ullivada.zeropanic.applister.model.InstalledApp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Lists installed applications on Windows by reading the registry uninstall keys.
 * Filters out system components to show only user-facing apps.
 */
public final class WindowsRegistryCollector implements InstalledAppCollector {
	@Override
	public String sourceName() {
		return "windows-registry";
	}

	@Override
	public List<InstalledApp> collect() {
		if (!isWindows()) {
			return List.of();
		}

		List<InstalledApp> apps = new ArrayList<>();
		apps.addAll(readUninstallKey("HKLM\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall"));
		apps.addAll(readUninstallKey("HKLM\\SOFTWARE\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall"));
		apps.addAll(readUninstallKey("HKCU\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall"));
		return apps;
	}

	private boolean isWindows() {
		String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		return os.contains("win");
	}

	private List<InstalledApp> readUninstallKey(String registryPath) {
		List<InstalledApp> apps = new ArrayList<>();
		try {
			List<String> subkeys = execRegistryQuery(registryPath);
			for (String subkey : subkeys) {
				if (subkey.startsWith(registryPath)) {
					readAppFromSubkey(subkey).ifPresent(apps::add);
				}
			}
		} catch (Exception e) {
			// Best-effort collection
		}
		return apps;
	}

	private Optional<InstalledApp> readAppFromSubkey(String subkey) {
		try {
			Optional<String> displayName = getRegistryValue(subkey, "DisplayName");
			if (displayName.isEmpty() || displayName.get().isEmpty()) {
				return Optional.empty();
			}

			String name = displayName.get();
			if (isSystemComponent(subkey) || isSystemAppByName(name)) {
				return Optional.empty();
			}

			Optional<String> version = getRegistryValue(subkey, "DisplayVersion");
			return Optional.of(new InstalledApp(name, sourceName(), Optional.empty(), version, Optional.empty()));
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	private boolean isSystemComponent(String subkey) {
		try {
			Optional<String> value = getRegistryValue(subkey, "SystemComponent");
			return value.isPresent() && value.get().contains("0x1");
		} catch (Exception e) {
			return false;
		}
	}

	private boolean isSystemAppByName(String name) {
		String lower = name.toLowerCase(Locale.ROOT);
		return (lower.startsWith("update for") || lower.startsWith("security update") ||
				(lower.contains("kb") && lower.matches(".*kb\\d+.*")) ||
				(lower.contains("microsoft visual c++") && lower.contains("redistributable")) ||
				lower.contains("microsoft .net") || lower.contains(".net framework") ||
				lower.contains("windows software development kit") ||
				lower.contains("microsoft sdk") || lower.contains("windows driver"));
	}

	private List<String> execRegistryQuery(String path) throws Exception {
		Process proc = Runtime.getRuntime().exec(new String[]{"reg", "query", path});
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
			List<String> lines = new ArrayList<>();
			String line;
			while ((line = reader.readLine()) != null) {
				lines.add(line.trim());
			}
			proc.waitFor();
			return lines;
		}
	}

	private Optional<String> getRegistryValue(String subkey, String valueName) {
		try {
			Process proc = Runtime.getRuntime().exec(new String[]{"reg", "query", subkey, "/v", valueName});
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					if (line.contains(valueName) && line.contains("REG_SZ")) {
						String[] parts = line.split("REG_SZ");
						if (parts.length > 1) {
							String val = parts[1].trim();
							if (!val.isEmpty()) {
								return Optional.of(val);
							}
						}
						break;
					}
				}
				proc.waitFor();
			}
		} catch (Exception e) {
			// Ignore
		}
		return Optional.empty();
	}
}
