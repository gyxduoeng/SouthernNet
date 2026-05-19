package com.aircas.gimpro.action;

import com.aircas.gimpro.dialog.DialogGpTextResult;
import com.aircas.gimpro.service.GpDataIntegrationService;
import com.supermap.desktop.core.Interface.IBaseItem;

/**
 * GIM Pro 多源数据整合诊断入口。
 */
public class CtrlActionGpDiagnoseSources extends AbstractGpAction {

	private final GpDataIntegrationService integrationService = new GpDataIntegrationService();

	public CtrlActionGpDiagnoseSources(IBaseItem caller) {
		super(caller);
	}

	@Override
	public void run() {
		showDialog(new DialogGpTextResult("GIM Pro 多源数据整合", integrationService.buildHumanReadableReport()));
	}
}

