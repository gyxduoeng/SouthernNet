package com.aircas.onemodel.action;

import com.aircas.onemodel.dock.OneModelEquipmentPropertyDockPanel;
import com.aircas.onemodel.dock.OneModelWorkspaceTreeDockPanel;
import com.aircas.onemodel.service.OneModelGimTreeService;
import com.aircas.onemodel.service.OneModelModelExplorerService;
import com.aircas.onemodel.service.OneModelSelectedEquipmentService;
import com.supermap.desktop.core.Application;
import com.supermap.desktop.core.Interface.IBaseItem;

import javax.swing.JOptionPane;
import java.lang.reflect.Method;

/**
 * 设备模型编辑入口。
 */
public class CtrlActionOmEquipmentPropertyEdit extends AbstractOmAction {

	private final OneModelSelectedEquipmentService selectedEquipmentService = new OneModelSelectedEquipmentService();
	private final OneModelGimTreeService gimTreeService = new OneModelGimTreeService();
	private final OneModelModelExplorerService modelExplorerService = OneModelModelExplorerService.getInstance();

	public CtrlActionOmEquipmentPropertyEdit(IBaseItem caller) {
		super(caller);
	}

	@Override
	public void run() {
		OneModelSelectedEquipmentService.LookupResult result = selectedEquipmentService.resolveCurrentSelection();
		if (result.isSuccess()) {
			modelExplorerService.showContext(result.getContext(), gimTreeService.parse(result.getContext().getModelResource()));
			refreshPanels();
			showDock(OneModelWorkspaceTreeDockPanel.class);
			showDock(OneModelEquipmentPropertyDockPanel.class);
		} else {
			modelExplorerService.clear(result.getMessage());
			refreshPanels();
		}
		if (!result.isSuccess()) {
			JOptionPane.showMessageDialog(null, result.getMessage(), "模型编辑", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	private void refreshPanels() {
		OneModelWorkspaceTreeDockPanel treePanel = resolveDockPanel(OneModelWorkspaceTreeDockPanel.class);
		if (treePanel != null) {
			treePanel.render(modelExplorerService.getCurrentContext(), modelExplorerService.getCurrentParseResult(),
					modelExplorerService.getCurrentMessage());
		}
		OneModelEquipmentPropertyDockPanel propertyPanel = resolveDockPanel(OneModelEquipmentPropertyDockPanel.class);
		if (propertyPanel != null) {
			if (modelExplorerService.getCurrentContext() == null) {
				propertyPanel.clearSelection(modelExplorerService.getCurrentMessage());
			} else {
				propertyPanel.renderSelection(modelExplorerService.getCurrentContext(),
						modelExplorerService.getCurrentNodeLabel(), modelExplorerService.getCurrentNodeProperties());
			}
		}
	}

	private <T> T resolveDockPanel(Class<T> componentClass) {
		Object dockbar = resolveDockbar(componentClass);
		if (dockbar == null) {
			return null;
		}
		Object component = invokeQuietly(dockbar, "getInnerComponent");
		return componentClass.isInstance(component) ? componentClass.cast(component) : null;
	}

	private void showDock(Class<?> componentClass) {
		Object dockbar = resolveDockbar(componentClass);
		if (dockbar != null) {
			invokeQuietly(dockbar, "setVisible", true);
		}
	}

	private Object resolveDockbar(Class<?> componentClass) {
		try {
			return Application.getActiveApplication().getMainFrame()
					.getDockbarManager().get(componentClass.getName());
		} catch (Exception ignored) {
			return null;
		}
	}

	private Object invokeQuietly(Object target, String methodName, Object... args) {
		if (target == null) {
			return null;
		}
		Class<?>[] parameterTypes = new Class<?>[args.length];
		for (int i = 0; i < args.length; i++) {
			if (args[i] instanceof Boolean) {
				parameterTypes[i] = boolean.class;
			} else {
				parameterTypes[i] = args[i] == null ? Object.class : args[i].getClass();
			}
		}
		try {
			Method method = target.getClass().getMethod(methodName, parameterTypes);
			method.setAccessible(true);
			return method.invoke(target, args);
		} catch (Exception ignored) {
			return null;
		}
	}
}


