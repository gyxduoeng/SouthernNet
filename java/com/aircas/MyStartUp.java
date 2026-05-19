package com.aircas;

import com.aircas.onemodel.service.OneModelNativeLibrarySupport;
import com.aircas.onemodel.service.OneModelProjectService;
import com.supermap.desktop.core.*;

/**
  * @version 1.0
 */
public class MyStartUp {
	public static void main(String[] args) {
		OneModelNativeLibrarySupport.preloadBundledSpatialiteStackQuietly();
		new OneModelProjectService().clearCurrentProjectSelection();
		if (!Application.getActiveApplication().initialize()) {
			System.exit(0);
		}
	}
}
