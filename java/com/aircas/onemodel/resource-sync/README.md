# resource-sync

当前 Git 仓库根目录在 `src/main/java/com/aircas`，看不到 `src/main/resources`。

本目录保存 OneModel 插件菜单配置的同步副本。若重新拉取代码后菜单没有更新，请将 `SuperMap.Desktop.GIM Pro.config` 复制到工程的：

`src/main/resources/SuperMap.Desktop.GIM Pro.config`

长期建议把 Git 仓库根目录调整到 `GIM Pro` 工程根目录，让 Java 源码和 resources 一起纳入版本控制。