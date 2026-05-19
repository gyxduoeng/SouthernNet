package com.aircas.gimpro.action;

import com.aircas.gimpro.dialog.DialogGpTextResult;
import com.aircas.gimpro.service.GpLightweightCapabilityService;
import com.supermap.desktop.core.Interface.IBaseItem;

/**
 * GIM Pro 轻量化能力接入入口。
 */
public class CtrlActionGpPrepareLightweight extends AbstractGpAction {

	private final GpLightweightCapabilityService lightweightCapabilityService = new GpLightweightCapabilityService();

	public CtrlActionGpPrepareLightweight(IBaseItem caller) {
		super(caller);
	}

	@Override
	public void run() {
		showDialog(new DialogGpTextResult("GIM Pro 轻量化能力接入", lightweightCapabilityService.prepareAndDescribe()));
	}
}

