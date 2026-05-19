package com.aircas.gimpro.service;

import com.aircas.gimpro.model.GpDataCategory;
import com.aircas.gimpro.model.GpSupportedTypeEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GIM Pro 平台允许接入类型目录。
 *
 * <p>当前版本按产品规划输出二维/三维分类目录，作为基于 iDesktopX 能力进行二次开发的首版类型矩阵。</p>
 */
public class GpDesktopSupportedTypeCatalogService {

	public List<GpSupportedTypeEntry> listSupportedTypes() {
		List<GpSupportedTypeEntry> result = new ArrayList<>();
		result.add(new GpSupportedTypeEntry(GpDataCategory.THREE_D, "BIM", "BIM 模型", "建筑/站房/设备相关 BIM 数据接入"));
		result.add(new GpSupportedTypeEntry(GpDataCategory.THREE_D, "OBLIQUE", "倾斜摄影模型", "倾斜摄影或类似实景三维模型"));
		result.add(new GpSupportedTypeEntry(GpDataCategory.THREE_D, "MODEL_LIBRARY", "三维模型库", "`.gim` 等模型库资源"));
		result.add(new GpSupportedTypeEntry(GpDataCategory.THREE_D, "POINT_CLOUD", "点云数据", "激光点云或其他三维点集"));
		result.add(new GpSupportedTypeEntry(GpDataCategory.THREE_D, "THREE_D_SCENE", "三维场景缓存", "由平台直接承载的三维场景或缓存成果"));
		result.add(new GpSupportedTypeEntry(GpDataCategory.TWO_D, "SATELLITE", "卫星影像", "遥感影像、卫星底图等二维影像"));
		result.add(new GpSupportedTypeEntry(GpDataCategory.TWO_D, "RASTER", "栅格影像", "普通栅格影像或正射影像"));
		result.add(new GpSupportedTypeEntry(GpDataCategory.TWO_D, "CAD", "CAD 图纸", "CAD/DWG/DXF 等二维图纸成果"));
		result.add(new GpSupportedTypeEntry(GpDataCategory.TWO_D, "VECTOR", "二维矢量", "点/线/面矢量数据"));
		result.add(new GpSupportedTypeEntry(GpDataCategory.TWO_D, "WEB_MAP", "在线地图服务", "WMS/WMTS/瓦片等二维底图服务"));
		return Collections.unmodifiableList(result);
	}
}

