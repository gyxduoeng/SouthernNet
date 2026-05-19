package com.aircas.onemodel.service;

import com.supermap.data.DatasetVector;
import com.supermap.data.DatasourceConnectionInfo;
import com.supermap.data.PrjCoordSys;

import java.lang.reflect.Method;

/**
 * OneModel 坐标系支持。
 */
public class OneModelCoordinateSystemSupport {

	private final OneModelSessionStore sessionStore = OneModelSessionStore.getInstance();

	public String resolveCurrentEpsgCode() {
		String value = sessionStore.getParameters().getCoordinateSystemCode();
		return value == null || value.trim().isEmpty() ? "4490" : value.trim();
	}

	public void applyToDatasourceConnectionInfoQuietly(DatasourceConnectionInfo info, String epsgCode) {
		if (info == null) {
			return;
		}
		PrjCoordSys prjCoordSys = createPrjCoordSys(epsgCode);
		if (prjCoordSys == null) {
			return;
		}
		invokeQuietly(info, "setPrjCoordSys", new Class<?>[]{PrjCoordSys.class}, prjCoordSys);
		invokeQuietly(info, "setCoordSys", new Class<?>[]{PrjCoordSys.class}, prjCoordSys);
	}

	public void applyToDatasourceQuietly(Object datasource, String epsgCode) {
		PrjCoordSys prjCoordSys = createPrjCoordSys(epsgCode);
		if (datasource == null || prjCoordSys == null) {
			return;
		}
		invokeQuietly(datasource, "setPrjCoordSys", new Class<?>[]{PrjCoordSys.class}, prjCoordSys);
	}

	public void applyToDatasetQuietly(DatasetVector dataset, String epsgCode) {
		if (dataset == null) {
			return;
		}
		PrjCoordSys prjCoordSys = createPrjCoordSys(epsgCode);
		if (prjCoordSys == null) {
			return;
		}
		try {
			dataset.setPrjCoordSys(prjCoordSys);
		} catch (Exception ignored) {
			// 不阻塞数据集创建。
		}
	}

	public void applyToDatasetInfoQuietly(Object datasetInfo, String epsgCode) {
		PrjCoordSys prjCoordSys = createPrjCoordSys(epsgCode);
		if (datasetInfo == null || prjCoordSys == null) {
			return;
		}
		invokeQuietly(datasetInfo, "setPrjCoordSys", new Class<?>[]{PrjCoordSys.class}, prjCoordSys);
	}

	public void applyToMapQuietly(Object mapObject, String epsgCode) {
		PrjCoordSys prjCoordSys = createPrjCoordSys(epsgCode);
		if (mapObject == null || prjCoordSys == null) {
			return;
		}
		invokeQuietly(mapObject, "setPrjCoordSys", new Class<?>[]{PrjCoordSys.class}, prjCoordSys);
	}

	private PrjCoordSys createPrjCoordSys(String epsgCode) {
		String value = epsgCode == null || epsgCode.trim().isEmpty() ? "4490" : epsgCode.trim();
		try {
			int code = Integer.parseInt(value);
			PrjCoordSys prjCoordSys = new PrjCoordSys();
			prjCoordSys.setEPSGCode(code);
			invokeQuietly(prjCoordSys, "fromEPSGCode", new Class<?>[]{int.class}, code);
			return prjCoordSys;
		} catch (Exception ignored) {
			return null;
		}
	}

	private Object invokeQuietly(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
		if (target == null) {
			return null;
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

