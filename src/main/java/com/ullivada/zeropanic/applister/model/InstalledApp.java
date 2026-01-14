package com.ullivada.zeropanic.applister.model;

import java.util.Optional;

public record InstalledApp(
		String name,
		String source,
		Optional<String> id,
		Optional<String> version,
		Optional<String> origin
) {
	public InstalledApp {
		name = name == null ? "" : name;
		source = source == null ? "" : source;
		id = id == null ? Optional.empty() : id;
		version = version == null ? Optional.empty() : version;
		origin = origin == null ? Optional.empty() : origin;
	}

	public static InstalledApp simple(String name, String source) {
		return new InstalledApp(name, source, Optional.empty(), Optional.empty(), Optional.empty());
	}
}
