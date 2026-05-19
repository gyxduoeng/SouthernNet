package com.aircas.onemodel.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Preloads OneModel bundled native GIS libraries before SuperMap opens UDBX datasources.
 */
public final class OneModelNativeLibrarySupport {

    private static volatile boolean attempted;

    private OneModelNativeLibrarySupport() {
    }

    public static void preloadBundledSpatialiteStackQuietly() {
        if (attempted) {
            return;
        }
        synchronized (OneModelNativeLibrarySupport.class) {
            if (attempted) {
                return;
            }
            attempted = true;
        }
        Path binDir = resolveBinDir();
        if (binDir == null) {
            return;
        }
        String[] libraries = new String[]{
                "sqlite328.dll",
                "zlib.dll",
                "TD_Zlib.dll",
                "libiconv.dll",
                "libxml2.dll",
                "geos352.dll",
                "geos352_c.dll",
                "proj_9_0.dll",
                "spatialite.dll"
        };
        for (String library : libraries) {
            loadIfExists(binDir.resolve(library));
        }
    }

    private static Path resolveBinDir() {
        Path userDir = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        Path parent = userDir.getParent();
        Path[] candidates = new Path[]{
                userDir.resolve("bin"),
                userDir,
                parent == null ? null : parent.resolve("bin")
        };
        for (Path candidate : candidates) {
            if (candidate != null && Files.isDirectory(candidate) && Files.exists(candidate.resolve("spatialite.dll"))) {
                return candidate;
            }
        }
        return null;
    }

    private static void loadIfExists(Path libraryPath) {
        try {
            if (Files.exists(libraryPath)) {
                System.load(libraryPath.toAbsolutePath().normalize().toString());
            }
        } catch (UnsatisfiedLinkError | SecurityException ignored) {
            // Fall back to SuperMap native loading if a dependency has already been loaded.
        }
    }
}
