package com.aircas;

import com.aircas.onemodel.service.OneModelNativeLibrarySupport;
import com.aircas.onemodel.service.OneModelProjectService;
import com.supermap.desktop.core.AbstractPlugin;
import com.supermap.desktop.core.PluginInfo;
import com.supermap.desktop.core.license.LicenseException;

/**
  * @version 1.0
 */
public class DevelopPlugin extends AbstractPlugin {

	public DevelopPlugin(String name, PluginInfo pluginInfo) throws LicenseException {
		super(name, pluginInfo);
		OneModelNativeLibrarySupport.preloadBundledSpatialiteStackQuietly();
		new OneModelProjectService().clearCurrentProjectSelection();
	}

	@Override
	public boolean isGranted() {
		return true;
	}

	@Override
	public String getPluginTitle() {
		return "GIM Pro";
	}

	@Override
	public String getPluginName() {
		return "SuperMap.Desktop.Develop";
	}
}

