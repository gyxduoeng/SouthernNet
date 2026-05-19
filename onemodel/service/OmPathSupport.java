package com.aircas.onemodel.service;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * OneModel 路径支持。
 */
public class OmPathSupport {

	public Path resolveProjectRoot() {
		return Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
	}

	public Path resolveDefaultDataDir() {
		return resolveSourceProjectRoot().resolve("data").toAbsolutePath().normalize();
	}

	public Path resolveRuntimeDir() {
		return resolveProjectRoot().resolve("data").resolve("_onemodel");
	}

	public Path resolveGlobalConfigDir() {
		return resolveRuntimeDir().resolve("global");
	}

	private Path resolveSourceProjectRoot() {
		List<Path> starts = collectCandidateStarts();
		for (Path start : starts) {
			Path root = findProjectRoot(start);
			if (root != null) {
				return root;
			}
			Path siblingRoot = findSiblingProjectRoot(start);
			if (siblingRoot != null) {
				return siblingRoot;
			}
		}
		return resolveProjectRoot();
	}

	private List<Path> collectCandidateStarts() {
		List<Path> starts = new ArrayList<>();
		try {
			URL location = OmPathSupport.class.getProtectionDomain().getCodeSource().getLocation();
			if (location != null) {
				starts.add(Paths.get(location.toURI()));
			}
		} catch (Exception ignored) {
			// 回退到 user.dir。
		}
		starts.add(resolveProjectRoot());
		return starts;
	}

	private Path findProjectRoot(Path start) {
		Path current = directoryOf(start);
		while (current != null) {
			if (isSourceProjectRoot(current)) {
				return current.toAbsolutePath().normalize();
			}
			current = current.getParent();
		}
		return null;
	}

	private Path findSiblingProjectRoot(Path start) {
		Path current = directoryOf(start);
		while (current != null) {
			Path nestedGimProRoot = current.resolve("GIM Pro-2").resolve("GIM Pro");
			if (isSourceProjectRoot(nestedGimProRoot)) {
				return nestedGimProRoot.toAbsolutePath().normalize();
			}
			Path directGimProRoot = current.resolve("GIM Pro");
			if (isSourceProjectRoot(directGimProRoot)) {
				return directGimProRoot.toAbsolutePath().normalize();
			}
			current = current.getParent();
		}
		return null;
	}

	private Path directoryOf(Path path) {
		if (path == null) {
			return null;
		}
		Path normalized = path.toAbsolutePath().normalize();
		if (Files.isRegularFile(normalized)) {
			return normalized.getParent();
		}
		return normalized;
	}

	private boolean isSourceProjectRoot(Path path) {
		return path != null
				&& Files.isDirectory(path)
				&& (Files.isDirectory(path.resolve("src").resolve("main").resolve("java").resolve("com").resolve("aircas"))
						|| Files.exists(path.resolve("src").resolve("main").resolve("resources").resolve("SuperMap.Desktop.GIM Pro.config")));
	}
}