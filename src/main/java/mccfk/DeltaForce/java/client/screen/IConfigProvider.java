package mccfk.DeltaForce.java.client.screen;

import java.util.List;

/**
 * 配置提供者接口
 * 白名单模组实现此接口并在主类中注册，即可在 DF 设置界面中显示和编辑配置
 */
public interface IConfigProvider {

    /**
     * 获取此模组的所有配置项
     */
    List<ConfigEntry> getConfigEntries();

    /**
     * 获取所有主分类名（去重）
     */
    default List<String> getCategories() {
        return getConfigEntries().stream()
                .map(ConfigEntry::category)
                .distinct()
                .toList();
    }

    /**
     * 获取指定主分类下的所有子分类名（去重）
     */
    default List<String> getSubCategories(String category) {
        return getConfigEntries().stream()
                .filter(e -> e.category().equals(category))
                .map(ConfigEntry::subCategory)
                .distinct()
                .toList();
    }

    /**
     * 获取指定主分类和子分类下的配置项
     */
    default List<ConfigEntry> getEntries(String category, String subCategory) {
        return getConfigEntries().stream()
                .filter(e -> e.category().equals(category) && e.subCategory().equals(subCategory))
                .toList();
    }

    /**
     * 获取指定路径的当前值（字符串形式）
     */
    String getValue(String path);

    /**
     * 设置指定路径的值（字符串形式），返回是否成功
     */
    boolean setValue(String path, String value);

    /**
     * 保存所有配置变更
     */
    void save();

    default boolean validate() { return true; }
    default boolean isBusy() { return false; }
    default String status() { return ""; }

    /**
     * 取消所有未保存的变更（恢复到上次保存的状态）
     */
    void cancel();

    /**
     * 是否有未保存的变更
     */
    boolean isDirty();

    /**
     * 判断指定路径的配置项是客户端本地（C）还是服务端同步（S）
     * 默认返回 true（客户端），DfsConfigProvider 会覆盖此方法区分
     */
    default boolean isClientLocal(String path) { return true; }

    /**
     * 判断当前用户是否有权编辑指定路径的配置项
     * 默认：客户端本地配置（C）始终可编辑，服务端配置（S）需 OP 或本地世界
     */
    default boolean canEdit(String path) { return isClientLocal(path); }

    /**
     * 配置项数据类
     * nestLevel: 嵌套层级，0=顶层，1=一级子项，2=二级子项
     * parentPath: 父配置项路径，null=无父项（顶层）
     */
    record ConfigEntry(String path, String key, String currentValue, String defaultValue,
                       String description, ConfigValueKind kind, String category, String subCategory,
                       int nestLevel, String parentPath, boolean wip) {

        public ConfigEntry(String path, String key, String currentValue, String defaultValue,
                           String description, ConfigValueKind kind, String category, String subCategory) {
            this(path, key, currentValue, defaultValue, description, kind, category, subCategory, 0, null, false);
        }

        public ConfigEntry(String path, String key, String currentValue, String defaultValue, String description) {
            this(path, key, currentValue, defaultValue, description, ConfigValueKind.STRING, "", "", 0, null, false);
        }

        public boolean isModified() {
            return !currentValue.equals(defaultValue);
        }

        public boolean isBoolean() {
            return kind == ConfigValueKind.BOOLEAN;
        }

        public boolean isNumeric() {
            return kind == ConfigValueKind.INTEGER || kind == ConfigValueKind.LONG
                    || kind == ConfigValueKind.FLOAT || kind == ConfigValueKind.DOUBLE;
        }
    }

    /**
     * 配置值类型
     */
    enum ConfigValueKind {
        BOOLEAN, INTEGER, LONG, FLOAT, DOUBLE, STRING
    }
}