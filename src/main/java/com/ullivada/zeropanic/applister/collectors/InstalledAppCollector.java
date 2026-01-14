package com.ullivada.zeropanic.applister.collectors;

import com.ullivada.zeropanic.applister.model.InstalledApp;

import java.util.List;

public interface InstalledAppCollector {
	String sourceName();

	List<InstalledApp> collect();
}
