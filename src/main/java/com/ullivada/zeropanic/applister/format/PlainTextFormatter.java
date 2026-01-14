package com.ullivada.zeropanic.applister.format;

import com.ullivada.zeropanic.applister.model.InstalledApp;

import java.util.List;
import java.util.Map;

public final class PlainTextFormatter {
	private PlainTextFormatter() {
	}

	public static String format(Map<String, List<InstalledApp>> bySource) {
		StringBuilder sb = new StringBuilder();
		int printedSources = 0;
		for (Map.Entry<String, List<InstalledApp>> entry : bySource.entrySet()) {
			String source = entry.getKey();
			List<InstalledApp> apps = entry.getValue();
			if (apps.isEmpty()) {
				continue;
			}
			printedSources++;

			sb.append(source).append(" (").append(apps.size()).append(")\n");
			for (InstalledApp app : apps) {
				sb.append("- ").append(app.name());
				app.version().ifPresent(v -> sb.append(" ").append(v));
				sb.append("\n");
			}
			sb.append("\n");
		}
		if (printedSources == 0) {
			sb.append("No apps found.\n");
		}
		return sb.toString();
	}
}
