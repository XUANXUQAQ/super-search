package file.engine.configs;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.intellijthemes.*;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialDarkerIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialLighterIJTheme;
import file.engine.IsDebug;
import file.engine.annotation.EventRegister;
import file.engine.dllInterface.IsLocalDisk;
import file.engine.event.handler.Event;
import file.engine.event.handler.EventManagement;
import file.engine.event.handler.impl.ReadConfigsAndBootSystemEvent;
import file.engine.event.handler.impl.SetSwingLaf;
import file.engine.event.handler.impl.configs.AddCmdEvent;
import file.engine.event.handler.impl.configs.DeleteCmdEvent;
import file.engine.event.handler.impl.configs.SaveConfigsEvent;
import file.engine.event.handler.impl.configs.SetConfigsEvent;
import file.engine.event.handler.impl.daemon.StartDaemonEvent;
import file.engine.event.handler.impl.download.StartDownloadEvent;
import file.engine.event.handler.impl.frame.searchBar.*;
import file.engine.event.handler.impl.frame.settingsFrame.GetExcludeComponentEvent;
import file.engine.event.handler.impl.hotkey.RegisterHotKeyEvent;
import file.engine.event.handler.impl.hotkey.ResponseCtrlEvent;
import file.engine.event.handler.impl.monitor.disk.StartMonitorDiskEvent;
import file.engine.event.handler.impl.plugin.LoadAllPluginsEvent;
import file.engine.event.handler.impl.plugin.SetPluginsCurrentThemeEvent;
import file.engine.event.handler.impl.taskbar.ShowTrayIconEvent;
import file.engine.services.download.DownloadManager;
import file.engine.services.download.DownloadService;
import file.engine.utils.RegexUtil;
import file.engine.utils.TranslateUtil;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 保存软件运行时的所有配置信息
 */
public class AllConfigs {
    public static final String version = "3.3"; //TODO 更改版本号

    public static final String FILE_NAME = "File-Engine-x64.exe";

    public static final int defaultLabelColor = 0x9999ff;
    public static final int defaultWindowBackgroundColor = 0xcccccc;
    public static final int defaultBorderColor = 0xcccccc;
    public static final int defaultFontColor = 0;
    public static final int defaultFontColorWithCoverage = 0;
    public static final int defaultSearchbarColor = 0xcccccc;
    public static final int defaultSearchbarFontColor = 0;
    private volatile ConfigEntity configEntity;
    private final LinkedHashMap<String, AddressUrl> updateAddressMap = new LinkedHashMap<>();
    private final LinkedHashSet<String> cmdSet = new LinkedHashSet<>();
    private boolean isFirstRunApp = false;

    private static volatile AllConfigs instance = null;

    private AllConfigs() {
    }

    public static AllConfigs getInstance() {
        if (instance == null) {
            synchronized (AllConfigs.class) {
                if (instance == null) {
                    instance = new AllConfigs();
                }
            }
        }
        return instance;
    }

    /**
     * 将swingTheme字符串映射到枚举类
     *
     * @param swingTheme swingTheme名称
     * @return swingTheme枚举类实例
     */
    private Enums.SwingThemes swingThemesMapper(String swingTheme) {
        if ("current".equals(swingTheme)) {
            return swingThemesMapper(configEntity.getSwingTheme());
        }
        for (Enums.SwingThemes each : Enums.SwingThemes.values()) {
            if (each.toString().equals(swingTheme)) {
                return each;
            }
        }
        return Enums.SwingThemes.MaterialLighter;
    }

    /**
     * 是否在将文件拖出时提示已创建快捷方式
     *
     * @return boolean
     */
    public boolean isShowTipOnCreatingLnk() {
        return configEntity.isShowTipCreatingLnk();
    }

    /**
     * 检测是不是第一次运行
     *
     * @return boolean
     */
    public boolean isFirstRun() {
        return isFirstRunApp;
    }

    /**
     * 获取网络代理端口
     *
     * @return proxy port
     */
    public int getProxyPort() {
        return configEntity.getProxyPort();
    }

    /**
     * 获取网络代理用户名
     *
     * @return proxy username
     */
    public String getProxyUserName() {
        return configEntity.getProxyUserName();
    }

    /**
     * 获取网络代理密码
     *
     * @return proxy password
     */
    public String getProxyPassword() {
        return configEntity.getProxyPassword();
    }

    /**
     * 获取网络代理的类型
     *
     * @return proxyType int 返回值的类型由Enums.ProxyType中定义
     * @see Enums.ProxyType
     */
    public int getProxyType() {
        return configEntity.getProxyType();
    }

    /**
     * 获取网络代理地址
     *
     * @return proxy address
     */
    public String getProxyAddress() {
        return configEntity.getProxyAddress();
    }

    /**
     * 获取搜索框默认字体颜色RGB值
     *
     * @return rgb hex
     */
    public int getSearchBarFontColor() {
        return configEntity.getSearchBarFontColor();
    }

    /**
     * 获取搜索框显示颜色
     *
     * @return rgb hex
     */
    public int getSearchBarColor() {
        return configEntity.getSearchBarColor();
    }

    /**
     * 获取热键
     *
     * @return hotkey 每个案件以 + 分开
     */
    public String getHotkey() {
        return configEntity.getHotkey();
    }

    /**
     * 获取最大cache数量
     *
     * @return cache max size
     */
    public int getCacheNumLimit() {
        return configEntity.getCacheNumLimit();
    }

    /**
     * 获取检测一次系统文件更改的时间
     *
     * @return int 单位 秒
     */
    public int getUpdateTimeLimit() {
        return configEntity.getUpdateTimeLimit();
    }

    /**
     * 获取忽略的文件夹
     *
     * @return ignored path 由 ,（逗号）分开
     */
    public String getIgnorePath() {
        return configEntity.getIgnorePath();
    }

    /**
     * 获取优先搜索文件夹
     *
     * @return priority dir
     */
    public String getPriorityFolder() {
        return configEntity.getPriorityFolder();
    }

    /**
     * 是否在打开文件时默认以管理员身份运行，绕过UAC（危险）
     *
     * @return boolean
     */
    public boolean isDefaultAdmin() {
        return configEntity.isDefaultAdmin();
    }

    /**
     * 是否在窗口失去焦点后自动关闭
     *
     * @return boolean
     */
    public boolean isLoseFocusClose() {
        return configEntity.isLoseFocusClose();
    }

    /**
     * 获取swing皮肤包名称，可由swingThemesMapper转换为Enums.SwingThemes
     *
     * @return swing name
     * @see Enums.SwingThemes
     * @see #swingThemesMapper(String)
     */
    public String getSwingTheme() {
        return configEntity.getSwingTheme();
    }

    /**
     * 获取打开上级文件夹的键盘快捷键code
     *
     * @return keycode
     */
    public int getOpenLastFolderKeyCode() {
        return configEntity.getOpenLastFolderKeyCode();
    }

    /**
     * 获取以管理员身份运行程序快捷键code
     *
     * @return keycode
     */
    public int getRunAsAdminKeyCode() {
        return configEntity.getRunAsAdminKeyCode();
    }

    /**
     * 获取复制文件路径code
     *
     * @return keycode
     */
    public int getCopyPathKeyCode() {
        return configEntity.getCopyPathKeyCode();
    }

    /**
     * 获取不透明度
     *
     * @return opacity
     */
    public float getOpacity() {
        return configEntity.getTransparency();
    }

    /**
     * 获取cmdSet的一个复制
     *
     * @return cmdSet clone
     */
    public LinkedHashSet<String> getCmdSet() {
        return new LinkedHashSet<>(cmdSet);
    }

    /**
     * 获取搜索下拉框的默认颜色
     *
     * @return rgb hex
     */
    public int getLabelColor() {
        return configEntity.getLabelColor();
    }

    /**
     * 获取更新地址
     *
     * @return url
     */
    public String getUpdateAddress() {
        return configEntity.getUpdateAddress();
    }

    /**
     * 获取下拉框默认背景颜色
     *
     * @return rgb hex
     */
    public int getDefaultBackgroundColor() {
        return configEntity.getDefaultBackgroundColor();
    }

    /**
     * 获取下拉框被选中的背景颜色
     *
     * @return rgb hex
     */
    public int getLabelFontColorWithCoverage() {
        return configEntity.getFontColorWithCoverage();
    }

    /**
     * 获取下拉框被选中的字体颜色
     *
     * @return rgb hex
     */
    public int getLabelFontColor() {
        return configEntity.getFontColor();
    }

    /**
     * 获取边框颜色
     *
     * @return rgb hex
     */
    public int getBorderColor() {
        return configEntity.getBorderColor();
    }

    /**
     * 获取边框类型
     *
     * @return 边框类型
     * @see Enums.BorderType
     */
    public Enums.BorderType getBorderType() {
        String borderType = configEntity.getBorderType();
        for (Enums.BorderType each : Enums.BorderType.values()) {
            if (each.toString().equals(borderType)) {
                return each;
            }
        }
        return Enums.BorderType.AROUND;
    }

    /**
     * 是否贴靠在explorer窗口
     *
     * @return true or false
     */
    public boolean isAttachExplorer() {
        return configEntity.isAttachExplorer();
    }

    /**
     * 获取边框厚度
     *
     * @return 厚度
     */
    public int getBorderThickness() {
        return configEntity.getBorderThickness();
    }

    public boolean isCheckUpdateStartup() {
        return configEntity.isCheckUpdateStartup();
    }

    public ProxyInfo getProxy() {
        return new ProxyInfo(
                configEntity.getProxyAddress(),
                configEntity.getProxyPort(),
                configEntity.getProxyUserName(),
                configEntity.getProxyPassword(),
                configEntity.getProxyType());
    }

    public String getDisks() {
        return configEntity.getDisks();
    }

    public boolean isResponseCtrl() {
        return configEntity.isDoubleClickCtrlOpen();
    }

    /**
     * 初始化cmdSet
     */
    private void initCmdSetSettings() {
        //获取所有自定义命令
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("user/cmds.txt"), StandardCharsets.UTF_8))) {
            String each;
            while ((each = br.readLine()) != null) {
                cmdSet.add(each);
            }
        } catch (IOException ignored) {
        }
    }

    private void initUpdateAddress() {
        //todo 添加更新服务器地址
        updateAddressMap.put("jsdelivr CDN",
                new AddressUrl(
                        "https://cdn.jsdelivr.net/gh/XUANXUQAQ/File-Engine-Version/version.json",
                        "https://cdn.jsdelivr.net/gh/XUANXUQAQ/File-Engine-Version/plugins.json"
                ));
        updateAddressMap.put("GitHub",
                new AddressUrl(
                        "https://raw.githubusercontent.com/XUANXUQAQ/File-Engine-Version/master/version.json",
                        "https://raw.githubusercontent.com/XUANXUQAQ/File-Engine-Version/master/plugins.json"
                ));
        updateAddressMap.put("GitHack",
                new AddressUrl(
                        "https://raw.githack.com/XUANXUQAQ/File-Engine-Version/master/version.json",
                        "https://raw.githack.com/XUANXUQAQ/File-Engine-Version/master/plugins.json"
                ));
        updateAddressMap.put("Gitee",
                new AddressUrl(
                        "https://gitee.com/XUANXUQAQ/file-engine-version/raw/master/version.json",
                        "https://gitee.com/XUANXUQAQ/file-engine-version/raw/master/plugins.json"
                ));
    }

    public AddressUrl getUpdateUrlFromMap() {
        return getUpdateUrlFromMap(getUpdateAddress());
    }

    public Set<String> getAllUpdateAddress() {
        return updateAddressMap.keySet();
    }

    public AddressUrl getUpdateUrlFromMap(String updateAddress) {
        return updateAddressMap.get(updateAddress);
    }

    private String getLocalDisks() {
        File[] files = File.listRoots();
        if (files == null || files.length == 0) {
            return "";
        }
        String diskName;
        StringBuilder stringBuilder = new StringBuilder();
        for (File each : files) {
            diskName = each.getAbsolutePath();
            if (IsLocalDisk.INSTANCE.isDiskNTFS(diskName) && IsLocalDisk.INSTANCE.isLocalDisk(diskName)) {
                stringBuilder.append(each.getAbsolutePath()).append(",");
            }
        }
        return stringBuilder.toString();
    }

    private void readDisks(JSONObject settingsInJson) {
        String disks = (String) getFromJson(settingsInJson, "disks", getLocalDisks());
        String[] stringDisk = RegexUtil.comma.split(disks);
        StringBuilder stringBuilder = new StringBuilder();
        for (String each : stringDisk) {
            if (new File(each).exists()) {
                stringBuilder.append(each).append(",");
            }
        }
        configEntity.setDisks(stringBuilder.toString());
    }

    private void readIsAttachExplorer(JSONObject settingsInJson) {
        configEntity.setAttachExplorer((boolean) getFromJson(settingsInJson, "isAttachExplorer", true));
    }

    private void readResponseCtrl(JSONObject settingsInJson) {
        configEntity.setDoubleClickCtrlOpen((boolean) getFromJson(settingsInJson, "doubleClickCtrlOpen", true));
    }

    private void readUpdateAddress(JSONObject settingsInJson) {
        configEntity.setUpdateAddress((String) getFromJson(settingsInJson, "updateAddress", "jsdelivr CDN"));
    }

    private void readCacheNumLimit(JSONObject settingsInJson) {
        configEntity.setCacheNumLimit((int) getFromJson(settingsInJson, "cacheNumLimit", 1000));
    }

    private void readHotKey(JSONObject settingsInJson) {
        configEntity.setHotkey((String) getFromJson(settingsInJson, "hotkey", "Ctrl + Alt + K"));
    }

    private void readPriorityFolder(JSONObject settingsInJson) {
        configEntity.setPriorityFolder((String) getFromJson(settingsInJson, "priorityFolder", ""));
    }

    private void readIgnorePath(JSONObject settingsInJson) {
        configEntity.setIgnorePath((String) getFromJson(settingsInJson, "ignorePath", "C:\\Windows,"));
    }

    private void readUpdateTimeLimit(JSONObject settingsInJson) {
        configEntity.setUpdateTimeLimit((int) getFromJson(settingsInJson, "updateTimeLimit", 5));
    }

    private void readIsDefaultAdmin(JSONObject settingsInJson) {
        configEntity.setDefaultAdmin((boolean) getFromJson(settingsInJson, "isDefaultAdmin", false));
    }

    private void readIsLoseFocusClose(JSONObject settingsInJson) {
        configEntity.setLoseFocusClose((boolean) getFromJson(settingsInJson, "isLoseFocusClose", true));
    }

    private void readOpenLastFolderKeyCode(JSONObject settingsInJson) {
        configEntity.setOpenLastFolderKeyCode((int) getFromJson(settingsInJson, "openLastFolderKeyCode", 17));
    }

    private void readRunAsAdminKeyCode(JSONObject settingsInJson) {
        configEntity.setRunAsAdminKeyCode((int) getFromJson(settingsInJson, "runAsAdminKeyCode", 16));
    }

    private void readCopyPathKeyCode(JSONObject settingsInJson) {
        configEntity.setCopyPathKeyCode((int) getFromJson(settingsInJson, "copyPathKeyCode", 18));
    }

    private void readTransparency(JSONObject settingsInJson) {
        configEntity.setTransparency(Float.parseFloat(getFromJson(settingsInJson, "transparency", 0.8f).toString()));
    }

    private void readSearchBarColor(JSONObject settingsInJson) {
        configEntity.setSearchBarColor((int) getFromJson(settingsInJson, "searchBarColor", defaultSearchbarColor));
    }

    private void readDefaultBackground(JSONObject settingsInJson) {
        configEntity.setDefaultBackgroundColor((int) getFromJson(settingsInJson, "defaultBackground", defaultWindowBackgroundColor));
    }

    private void readBorderType(JSONObject settingsInJson) {
        configEntity.setBorderType((String) getFromJson(settingsInJson, "borderType", Enums.BorderType.AROUND.toString()));
    }

    private void readBorderColor(JSONObject settingsInJson) {
        configEntity.setBorderColor((int) getFromJson(settingsInJson, "borderColor", defaultBorderColor));
    }

    private void readFontColorWithCoverage(JSONObject settingsInJson) {
        configEntity.setFontColorWithCoverage((int) getFromJson(settingsInJson, "fontColorWithCoverage", defaultFontColorWithCoverage));
    }

    private void readLabelColor(JSONObject settingsInJson) {
        configEntity.setLabelColor((int) getFromJson(settingsInJson, "labelColor", defaultLabelColor));
    }

    private void readFontColor(JSONObject settingsInJson) {
        configEntity.setFontColor((int) getFromJson(settingsInJson, "fontColor", defaultFontColor));
    }

    private void readSearchBarFontColor(JSONObject settingsInJson) {
        configEntity.setSearchBarFontColor((int) getFromJson(settingsInJson, "searchBarFontColor", defaultSearchbarFontColor));
    }

    private void readBorderThickness(JSONObject settingsInJson) {
        configEntity.setBorderThickness((int) getFromJson(settingsInJson, "borderThickness", 1));
    }

    private void readLanguage(JSONObject settingsInJson) {
        TranslateUtil translateUtil = TranslateUtil.getInstance();
        String language = (String) getFromJson(settingsInJson, "language", translateUtil.getDefaultLang());
        configEntity.setLanguage(language);
        translateUtil.setLanguage(language);
    }

    private void readProxy(JSONObject settingsInJson) {
        configEntity.setProxyAddress((String) getFromJson(settingsInJson, "proxyAddress", ""));
        configEntity.setProxyPort((int) getFromJson(settingsInJson, "proxyPort", 0));
        configEntity.setProxyUserName((String) getFromJson(settingsInJson, "proxyUserName", ""));
        configEntity.setProxyPassword((String) getFromJson(settingsInJson, "proxyPassword", ""));
        configEntity.setProxyType((int) getFromJson(settingsInJson, "proxyType", Enums.ProxyType.PROXY_DIRECT));
    }

    private void readCheckUpdateStartup(JSONObject settings) {
        configEntity.setCheckUpdateStartup((Boolean) getFromJson(settings, "isCheckUpdateStartup", true));
    }

    private void readSwingTheme(JSONObject settingsInJson) {
        configEntity.setSwingTheme((String) getFromJson(settingsInJson, "swingTheme", "CoreFlatDarculaLaf"));
    }

    private void readShowTipOnCreatingLnk(JSONObject settingsInJson) {
        configEntity.setShowTipCreatingLnk((boolean) getFromJson(settingsInJson, "isShowTipOnCreatingLnk", true));
    }

    private JSONObject getSettingsJSON() {
        File settings = new File("user/settings.json");
        if (settings.exists()) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(settings), StandardCharsets.UTF_8))) {
                String line;
                StringBuilder result = new StringBuilder();
                while (null != (line = br.readLine())) {
                    result.append(line);
                }
                return JSONObject.parseObject(result.toString());
            } catch (IOException e) {
                return null;
            }
        } else {
            isFirstRunApp = true;
            return null;
        }
    }

    private Object getFromJson(JSONObject json, String key, Object defaultObj) {
        if (json == null) {
            return defaultObj;
        }
        Object tmp = json.get(key);
        if (tmp == null) {
            if (IsDebug.isDebug()) {
                System.err.println("配置文件读取到null值   key : " + key);
            }
            return defaultObj;
        }
        return tmp;
    }

    private void readAllSettings() {
        configEntity = new ConfigEntity();
        JSONObject settingsInJson = getSettingsJSON();
        readProxy(settingsInJson);
        readLabelColor(settingsInJson);
        readLanguage(settingsInJson);
        readBorderColor(settingsInJson);
        readBorderType(settingsInJson);
        readSearchBarColor(settingsInJson);
        readSearchBarFontColor(settingsInJson);
        readFontColor(settingsInJson);
        readFontColorWithCoverage(settingsInJson);
        readDefaultBackground(settingsInJson);
        readTransparency(settingsInJson);
        readCopyPathKeyCode(settingsInJson);
        readRunAsAdminKeyCode(settingsInJson);
        readOpenLastFolderKeyCode(settingsInJson);
        readIsLoseFocusClose(settingsInJson);
        readIsDefaultAdmin(settingsInJson);
        readUpdateTimeLimit(settingsInJson);
        readIgnorePath(settingsInJson);
        readPriorityFolder(settingsInJson);
        readCacheNumLimit(settingsInJson);
        readUpdateAddress(settingsInJson);
        readHotKey(settingsInJson);
        readResponseCtrl(settingsInJson);
        readIsAttachExplorer(settingsInJson);
        readShowTipOnCreatingLnk(settingsInJson);
        readSwingTheme(settingsInJson);
        readDisks(settingsInJson);
        readCheckUpdateStartup(settingsInJson);
        readBorderThickness(settingsInJson);
        initUpdateAddress();
        initCmdSetSettings();
    }

    private void setAllSettings() {
        EventManagement eventManagement = EventManagement.getInstance();
        AllConfigs allConfigs = AllConfigs.getInstance();
        eventManagement.putEvent(new SetPluginsCurrentThemeEvent(
                allConfigs.getDefaultBackgroundColor(),
                allConfigs.getLabelColor(),
                allConfigs.getBorderColor()));
        eventManagement.putEvent(new RegisterHotKeyEvent(configEntity.getHotkey()));
        eventManagement.putEvent(new ResponseCtrlEvent(configEntity.isDoubleClickCtrlOpen()));
        eventManagement.putEvent(new SetSearchBarTransparencyEvent(configEntity.getTransparency()));
        eventManagement.putEvent(new SetSearchBarDefaultBackgroundEvent(configEntity.getDefaultBackgroundColor()));
        eventManagement.putEvent(new SetSearchBarLabelColorEvent(configEntity.getLabelColor()));
        eventManagement.putEvent(new SetSearchBarFontColorWithCoverageEvent(configEntity.getFontColorWithCoverage()));
        eventManagement.putEvent(new SetSearchBarLabelFontColorEvent(configEntity.getFontColor()));
        eventManagement.putEvent(new SetSearchBarColorEvent(configEntity.getSearchBarColor()));
        eventManagement.putEvent(new SetSearchBarFontColorEvent(configEntity.getSearchBarFontColor()));
        eventManagement.putEvent(new SetBorderEvent(allConfigs.getBorderType(), configEntity.getBorderColor(), configEntity.getBorderThickness()));
        eventManagement.putEvent(new SetSwingLaf("current"));
    }

    private void setSwingLaf(Enums.SwingThemes theme) {
        SwingUtilities.invokeLater(() -> {
            if (theme == Enums.SwingThemes.CoreFlatIntelliJLaf) {
                FlatIntelliJLaf.install();
            } else if (theme == Enums.SwingThemes.CoreFlatLightLaf) {
                FlatLightLaf.install();
            } else if (theme == Enums.SwingThemes.CoreFlatDarkLaf) {
                FlatDarkLaf.install();
            } else if (theme == Enums.SwingThemes.Arc) {
                FlatArcIJTheme.install();
            } else if (theme == Enums.SwingThemes.ArcDark) {
                FlatArcDarkIJTheme.install();
            } else if (theme == Enums.SwingThemes.DarkFlat) {
                FlatDarkFlatIJTheme.install();
            } else if (theme == Enums.SwingThemes.Carbon) {
                FlatCarbonIJTheme.install();
            } else if (theme == Enums.SwingThemes.CyanLight) {
                FlatCyanLightIJTheme.install();
            } else if (theme == Enums.SwingThemes.DarkPurple) {
                FlatDarkPurpleIJTheme.install();
            } else if (theme == Enums.SwingThemes.LightFlat) {
                FlatLightFlatIJTheme.install();
            } else if (theme == Enums.SwingThemes.Monocai) {
                FlatMonocaiIJTheme.install();
            } else if (theme == Enums.SwingThemes.OneDark) {
                FlatOneDarkIJTheme.install();
            } else if (theme == Enums.SwingThemes.Gray) {
                FlatGrayIJTheme.install();
            } else if (theme == Enums.SwingThemes.MaterialDesignDark) {
                FlatMaterialDesignDarkIJTheme.install();
            } else if (theme == Enums.SwingThemes.MaterialLighter) {
                FlatMaterialLighterIJTheme.install();
            } else if (theme == Enums.SwingThemes.MaterialDarker) {
                FlatMaterialDarkerIJTheme.install();
            } else if (theme == Enums.SwingThemes.ArcDarkOrange) {
                FlatArcDarkOrangeIJTheme.install();
            } else if (theme == Enums.SwingThemes.Dracula) {
                FlatDraculaIJTheme.install();
            } else if (theme == Enums.SwingThemes.Nord) {
                FlatNordIJTheme.install();
            } else {
                FlatDarculaLaf.install();
            }
            ArrayList<Component> components = new ArrayList<>(Arrays.asList(JFrame.getFrames()));
            EventManagement eventManagement = EventManagement.getInstance();
            GetExcludeComponentEvent event = new GetExcludeComponentEvent();
            eventManagement.putEvent(event);
            eventManagement.waitForEvent(event);
            components.addAll(event.getReturnValue());
            for (Component frame : components) {
                SwingUtilities.updateComponentTreeUI(frame);
            }
        });
    }

    private void saveAllSettings() {
        try (BufferedWriter buffW = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("user/settings.json"), StandardCharsets.UTF_8))) {
            String format = JSON.toJSONString(
                    configEntity,
                    SerializerFeature.PrettyFormat,
                    SerializerFeature.WriteMapNullValue,
                    SerializerFeature.WriteDateUseDateFormat
            );
            buffW.write(format);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean hasStartup() {
        String command = "cmd.exe /c chcp 65001 & schtasks /query /tn \"File-Engine\"";
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(command);
            StringBuilder strBuilder = new StringBuilder();
            String line;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                while ((line = reader.readLine()) != null) {
                    strBuilder.append(line);
                }
            }
            return strBuilder.toString().contains("File-Engine");
        } catch (IOException e) {
            return false;
        } finally {
            if (p != null) {
                p.destroy();
            }
        }
    }

    private boolean noNullValue(ConfigEntity config) {
        try {
            for (Field field : config.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object o = field.get(config);
                if (o == null) {
                    return false;
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private String getUpdateUrl() {
        return getUpdateUrlFromMap().fileEngineVersionUrl;
    }

    public JSONObject getUpdateInfo() throws IOException {
        DownloadService downloadService = DownloadService.getInstance();
        String url = getUpdateUrl();
        DownloadManager downloadManager = new DownloadManager(
                url,
                "version.json",
                new File("tmp").getAbsolutePath()
        );
        EventManagement eventManagement = EventManagement.getInstance();
        eventManagement.putEvent(new StartDownloadEvent(downloadManager));
        downloadService.waitForDownloadTask(downloadManager, 10000);
        String eachLine;
        StringBuilder strBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("tmp/version.json"), StandardCharsets.UTF_8))) {
            while ((eachLine = br.readLine()) != null) {
                strBuilder.append(eachLine);
            }
        }
        return JSONObject.parseObject(strBuilder.toString());
    }

    @EventRegister(registerClass = AddCmdEvent.class)
    private static void addCmdEvent(Event event) {
        AllConfigs allConfigs = getInstance();
        AddCmdEvent event1 = (AddCmdEvent) event;
        allConfigs.cmdSet.add(event1.cmd);
    }

    @EventRegister(registerClass = DeleteCmdEvent.class)
    private static void DeleteCmdEvent(Event event) {
        AllConfigs allConfigs = AllConfigs.getInstance();
        DeleteCmdEvent deleteCmdEvent = (DeleteCmdEvent) event;
        allConfigs.cmdSet.remove(deleteCmdEvent.cmd);
    }

    @EventRegister(registerClass = ReadConfigsAndBootSystemEvent.class)
    private static void ReadConfigsAndBootSystemEvent(Event event) {
        Event tmpEvent;
        EventManagement eventManagement = EventManagement.getInstance();
        AllConfigs allConfigs = AllConfigs.getInstance();

        allConfigs.readAllSettings();
        allConfigs.saveAllSettings();

        tmpEvent = new LoadAllPluginsEvent("plugins");
        eventManagement.putEvent(tmpEvent);
        eventManagement.waitForEvent(tmpEvent);

        eventManagement.putEvent(new StartMonitorDiskEvent());
        eventManagement.putEvent(new ShowTrayIconEvent());

        tmpEvent = new SetConfigsEvent();
        eventManagement.putEvent(tmpEvent);
        eventManagement.waitForEvent(tmpEvent);

        if (!IsDebug.isDebug()) {
            eventManagement.putEvent(new StartDaemonEvent(new File("").getAbsolutePath()));
        }
    }

    @EventRegister(registerClass = SetConfigsEvent.class)
    public static void SetAllConfigsEvent(Event event) {
        getInstance().setAllSettings();
    }

    @EventRegister(registerClass = SetSwingLaf.class)
    public static void setSwingLafEvent(Event event) {
        try {
            AllConfigs instance = getInstance();
            String theme = ((SetSwingLaf) event).theme;
            instance.setSwingLaf(instance.swingThemesMapper(theme));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventRegister(registerClass = SaveConfigsEvent.class)
    public static void saveConfigsEvent(Event event) {
        AllConfigs allConfigs = getInstance();
        ConfigEntity tempConfigEntity = ((SaveConfigsEvent) event).configEntity;
        if (allConfigs.noNullValue(tempConfigEntity)) {
            allConfigs.configEntity = tempConfigEntity;
            allConfigs.saveAllSettings();
        } else {
            throw new NullPointerException("configEntity中有Null值");
        }
    }

    public static class AddressUrl {
        public final String fileEngineVersionUrl;
        public final String pluginListUrl;

        private AddressUrl(String fileEngineVersionUrl, String pluginListUrl) {
            this.fileEngineVersionUrl = fileEngineVersionUrl;
            this.pluginListUrl = pluginListUrl;
        }
    }
}
