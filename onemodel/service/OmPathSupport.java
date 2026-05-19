package com.aircas.onemodel.service;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * OneModel 路径支持。
 */
public class OmPathSupport {

	public Path resolveProjectRoot() {
		return Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
	}

	public Path resolveRuntimeDir() {
		return resolveProjectRoot().resolve("data").resolve("_onemodel");
	}

	public Path resolveGlobalConfigDir() {
		return resolveRuntimeDir().resolve("global");
	}
}

