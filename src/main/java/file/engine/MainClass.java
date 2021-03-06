package file.engine;

import com.alibaba.fastjson.JSONObject;
import file.engine.configs.AllConfigs;
import file.engine.configs.Enums;
import file.engine.constant.Constants;
import file.engine.event.handler.Event;
import file.engine.event.handler.EventManagement;
import file.engine.event.handler.impl.ReadConfigsAndBootSystemEvent;
import file.engine.event.handler.impl.database.UpdateDatabaseEvent;
import file.engine.event.handler.impl.frame.settingsFrame.ShowSettingsFrameEvent;
import file.engine.event.handler.impl.stop.RestartEvent;
import file.engine.event.handler.impl.taskbar.ShowTaskBarMessageEvent;
import file.engine.services.DatabaseService;
import file.engine.services.plugin.system.PluginService;
import file.engine.utils.CachedThreadPoolUtil;
import file.engine.utils.Md5Util;
import file.engine.utils.SQLiteUtil;
import file.engine.utils.TranslateUtil;
import file.engine.utils.file.CopyFileUtil;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public class MainClass {
    private static final String FILE_MONITOR_64_MD_5 = "5a8c123397c8e89614d4f9b91c2fa8f9";
    private static final String GET_ASC_II_64_MD_5 = "62a56c26e1afa7c4fa3f441aadb9d515";
    private static final String HOTKEY_LISTENER_64_MD_5 = "a212cc427a89a614402e59897c82e50d";
    private static final String IS_LOCAL_DISK_64_MD_5 = "f8a71d3496d8cc188713d521e6dfa2b2";
    private static final String FILE_SEARCHER_USN_64_MD_5 = "1fef7fdf8ebde3846afe0e442f47f6ef";
    private static final String SQLITE3_64_MD_5 = "703bd51c19755db49c9070ceb255dfe5";
    private static final String UPDATER_BAT_64_MD_5 = "357d7cc1cf023cb6c90f73926c6f2f55";
    private static final String GET_HANDLE_64_MD_5 = "ee14698d5c8c8b55110d53012f8b7739";
    private static final String DAEMON_PROCESS_64_MD_5 = "cf6052fa108b0d19a98d2422c1ea853f";
    private static final String SHORTCUT_GEN_MD_5 = "fa4e26f99f3dcd58d827828c411ea5d7";

    private static void initializeDllInterface() throws ClassNotFoundException {
        Class.forName("file.engine.dllInterface.FileMonitor");
        Class.forName("file.engine.dllInterface.IsLocalDisk");
        Class.forName("file.engine.dllInterface.HotkeyListener");
        Class.forName("file.engine.dllInterface.GetAscII");
        Class.forName("file.engine.dllInterface.GetHandle");
    }

    private static void updatePlugins() throws FileNotFoundException {
        File sign = new File("user/updatePlugin");
        File tmpPlugins = new File("tmp/pluginsUpdate");
        if (sign.exists()) {
            if (IsDebug.isDebug()) {
                System.out.println("正在更新插件");
            }
            boolean isUpdatePluginSignDeleted = sign.delete();
            if (!isUpdatePluginSignDeleted) {
                System.err.println("删除插件更新标志失败");
            }
            File[] files = tmpPlugins.listFiles();
            if (files == null || files.length == 0) {
                return;
            }
            for (File eachPlugin : files) {
                String pluginName = eachPlugin.getName();
                File targetPlugin = new File("plugins" + File.separator + pluginName);
                CopyFileUtil.copyFile(new FileInputStream(eachPlugin), targetPlugin);
            }
        }
    }

    private static boolean isTableExist(ArrayList<String> tableNames) {
        try (Statement stmt = SQLiteUtil.getStatement()) {
            for (String tableName : tableNames) {
                String sql = String.format("SELECT ASCII, PATH FROM %s;", tableName);
                stmt.executeQuery(sql);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isAtDiskC() {
        return new File("").getAbsolutePath().startsWith("C:");
    }

    private static boolean isDatabaseDamaged() {
        ArrayList<String> list = new ArrayList<>();
        for (int i = 0; i <= Constants.ALL_TABLE_NUM; i++) {
            list.add("list" + i);
        }
        return !isTableExist(list);
    }

    private static void deleteDir(File file) {
        if (!file.exists()) {
            return;
        }
        File[] content = file.listFiles();//取得当前目录下所有文件和文件夹
        if (content == null || content.length == 0) {
            return;
        }
        for (File temp : content) {
            //直接删除文件
            if (temp.isDirectory()) {//判断是否是目录
                deleteDir(temp);//递归调用，删除目录里的内容
            }
            //删除空目录
            if (!temp.delete()) {
                System.err.println("Failed to delete " + temp.getAbsolutePath());
            }
        }
    }

    private static void deleteUpdater() throws InterruptedException {
        boolean ret = false;
        int count = 0;
        File updater = new File("updater.bat");
        if (updater.exists()) {
            while (!ret) {
                ret = updater.delete();
                Thread.sleep(1000);
                count++;
                if (count > 3) {
                    break;
                }
            }
        }
    }

    private static String generateFormattedSql(String suffix, int priority) {
        return String.format("INSERT OR IGNORE INTO priority VALUES(\"%s\", %d)", suffix, priority);
    }

    private static void insertAllSuffixPriority(HashMap<String, Integer> suffixMap, Statement statement) {
        try {
            statement.execute("BEGIN;");
            suffixMap.forEach((suffix, priority) -> {
                String generateFormattedSql = generateFormattedSql(suffix, priority);
                try {
                    statement.execute(generateFormattedSql);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
        } catch (SQLException throwable) {
            throwable.printStackTrace();
        } finally {
            try {
                statement.execute("COMMIT;");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private static void createPriorityTable() throws SQLException {
        try (Statement statement = SQLiteUtil.getStatement()) {
            int row = statement.executeUpdate("CREATE TABLE IF NOT EXISTS priority(SUFFIX text unique, PRIORITY INT)");
            if (row == 0) {
                int count = 10;
                HashMap<String, Integer> map = new HashMap<>();
                map.put("lnk", count--);
                map.put("exe", count--);
                map.put("bat", count--);
                map.put("cmd", count--);
                map.put("txt", count--);
                map.put("docx", count--);
                map.put("zip", count--);
                map.put("rar", count--);
                map.put("7z", count--);
                map.put("html", count);
                map.put("defaultPriority", 0);
                insertAllSuffixPriority(map, statement);
            }
        }
    }

    private static void createCacheTable() throws SQLException {
        try (PreparedStatement pStmt = SQLiteUtil.getPreparedStatement("CREATE TABLE IF NOT EXISTS cache(PATH text unique);")) {
            pStmt.executeUpdate();
        }
    }

    private static void checkVersion() {
        if (AllConfigs.getInstance().isCheckUpdateStartup()) {
            EventManagement eventManagement = EventManagement.getInstance();
            TranslateUtil translateUtil = TranslateUtil.getInstance();
            if (!isLatest()) {
                eventManagement.putEvent(new ShowTaskBarMessageEvent(
                        translateUtil.getTranslation("Info"),
                        translateUtil.getTranslation("New version can be updated"),
                        new ShowSettingsFrameEvent("tabAbout")));
            }
        }
    }

    private static void checkOldApiPlugin() {
        EventManagement eventManagement = EventManagement.getInstance();
        TranslateUtil translateUtil = TranslateUtil.getInstance();
        if (PluginService.getInstance().isPluginTooOld()) {
            String oldPlugins = PluginService.getInstance().getAllOldPluginsName();
            eventManagement.putEvent(new ShowTaskBarMessageEvent(
                    translateUtil.getTranslation("Warning"),
                    oldPlugins + "\n" + translateUtil.getTranslation("Plugin Api is too old"),
                    new ShowSettingsFrameEvent("tabPlugin")));
        }
    }

    private static void checkRepeatPlugin() {
        EventManagement eventManagement = EventManagement.getInstance();
        TranslateUtil translateUtil = TranslateUtil.getInstance();
        if (PluginService.getInstance().isPluginRepeat()) {
            String repeatPlugins = PluginService.getInstance().getRepeatPlugins();
            eventManagement.putEvent(new ShowTaskBarMessageEvent(
                    translateUtil.getTranslation("Warning"),
                    repeatPlugins + "\n" + translateUtil.getTranslation("Duplicate plugin, please delete it in plugins folder"),
                    new ShowSettingsFrameEvent("tabPlugin")));
        }
    }

    private static void checkErrorPlugin() {
        EventManagement eventManagement = EventManagement.getInstance();
        TranslateUtil translateUtil = TranslateUtil.getInstance();
        if (PluginService.getInstance().isPluginLoadError()) {
            String errorPlugins = PluginService.getInstance().getLoadingErrorPlugins();
            eventManagement.putEvent(new ShowTaskBarMessageEvent(
                    translateUtil.getTranslation("Warning"),
                    errorPlugins + "\n" + translateUtil.getTranslation("Loading plugins error"),
                    new ShowSettingsFrameEvent("tabPlugin")));
        }
    }

    private static void initDatabase() throws SQLException {
        SQLiteUtil.initConnection("jdbc:sqlite:data.db");
        createCacheTable();
        createPriorityTable();
    }

    private static void checkPluginInfo() {
        checkOldApiPlugin();
        checkRepeatPlugin();
        checkErrorPlugin();
        checkPluginVersion();
    }

    private static void checkPluginVersion() {
        if (AllConfigs.getInstance().isCheckUpdateStartup()) {
            CachedThreadPoolUtil cachedThreadPoolUtil = CachedThreadPoolUtil.getInstance();
            cachedThreadPoolUtil.executeTask(() -> {
                StringBuilder notLatestPluginsBuilder = new StringBuilder();
                PluginService pluginService = PluginService.getInstance();
                pluginService.checkAllPluginsVersion(notLatestPluginsBuilder);
                EventManagement eventManagement = EventManagement.getInstance();
                TranslateUtil translateUtil = TranslateUtil.getInstance();
                String notLatestPlugins = notLatestPluginsBuilder.toString();
                if (!notLatestPlugins.isEmpty()) {
                    eventManagement.putEvent(
                            new ShowTaskBarMessageEvent(translateUtil.getTranslation("Info"),
                                    notLatestPlugins + "\n" +
                                            translateUtil.getTranslation("New versions of these plugins can be updated"),
                                    new ShowSettingsFrameEvent("tabPlugin")));
                }
            });
        }
    }

    private static void checkRunningDirAtDiskC() {
        EventManagement eventManagement = EventManagement.getInstance();
        TranslateUtil translateUtil = TranslateUtil.getInstance();
        if (isAtDiskC()) {
            eventManagement.putEvent(new ShowTaskBarMessageEvent(
                    translateUtil.getTranslation("Warning"),
                    translateUtil.getTranslation("Putting the software on the C drive may cause index failure issue")));
        }
    }

    /**
     * -Dfile.encoding=UTF-8
     * -Dsun.java2d.noddraw=true
     * -Djna.library.path=user
     * -Dswing.aatext=true
     * -Djna.debug_load=false
     * -DFile_Engine_Debug=true
     */
    private static void setSystemProperties() {
        //todo Debug在发布时设置为false
        System.setProperty("File_Engine_Debug", "true");
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("sun.java2d.noddraw", "true");
        System.setProperty("jna.library.path", "user");
        System.setProperty("swing.aatext", "true");
        System.setProperty("jna.debug_load", "false");
    }

    public static void main(String[] args) {
        try {
            setSystemProperties();

            if (!System.getProperty("os.arch").contains("64")) {
                JOptionPane.showMessageDialog(null, "Not 64 Bit", "ERROR", JOptionPane.ERROR_MESSAGE);
                return;
            }

            startOrIgnoreUpdateAndExit(isUpdateSignExist());

            Class.forName("org.sqlite.JDBC");

            initDatabase();

            updatePlugins();

            //清空tmp
            deleteDir(new File("tmp"));

            initFoldersAndFiles();

            initializeDllInterface();

            EventManagement eventManagement = EventManagement.getInstance();

            eventManagement.registerAllHandler();

            eventManagement.registerAllListener();

            sendStartSignal();

            checkRunningDirAtDiskC();

            checkVersion();

            checkPluginInfo();

            mainLoop();

            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    static class CursorCount {
        private static final AtomicInteger count = new AtomicInteger();
    }

    private static void mainLoop() throws InterruptedException {
        Date startTime = new Date();
        Date endTime;
        long timeDiff;
        long div = 24 * 60 * 60 * 1000;
        int restartCount = 0;

        boolean isDatabaseDamaged = isDatabaseDamaged();
        boolean isCheckIndex = false;
        boolean isNeedUpdate = false;

        if (!IsDebug.isDebug()) {
            isCheckIndex = checkIndex();
        }

        EventManagement eventManagement = EventManagement.getInstance();
        TranslateUtil translateUtil = TranslateUtil.getInstance();

        if (isDatabaseDamaged || isCheckIndex) {
            eventManagement.putEvent(new ShowTaskBarMessageEvent(
                    translateUtil.getTranslation("Info"),
                    translateUtil.getTranslation("Updating file index")));
            eventManagement.putEvent(new UpdateDatabaseEvent());
        }

        startGetCursorPosTimer();

        while (eventManagement.isNotMainExit()) {
            // 主循环开始
            if (CursorCount.count.get() < 200) {
                CursorCount.count.incrementAndGet();
            }
            //检查已工作时间
            endTime = new Date();
            timeDiff = endTime.getTime() - startTime.getTime();
            long diffDays = timeDiff / div;
            if (diffDays > 2) {
                restartCount++;
                startTime = endTime;
                //启动时间已经超过2天,更新索引
                isNeedUpdate = true;
            }
            if (IsDebug.isDebug()) {
                if (isCursorLongTimeNotMove()) {
                    if (IsDebug.isDebug()) {
                        System.out.println("长时间未移动鼠标");
                    }
                }
            }
            //开始检测鼠标移动，若鼠标长时间未移动，且更新标志isNeedUpdate为true，则更新
            if (isNeedUpdate && isCursorLongTimeNotMove()) {
                isNeedUpdate = false;
                eventManagement.putEvent(new ShowTaskBarMessageEvent(
                        translateUtil.getTranslation("Info"),
                        translateUtil.getTranslation("Updating file index")));
                eventManagement.putEvent(new UpdateDatabaseEvent());
            }
            if (restartCount > 2) {
                restartCount = 0;
                //超过限定时间未重启
                eventManagement.putEvent(new RestartEvent());
            }
            TimeUnit.SECONDS.sleep(1);
        }
    }

    /**
     * 检测鼠标是否在两分钟内都未移动
     *
     * @return true if cursor not move in 2 minutes
     */
    private static boolean isCursorLongTimeNotMove() {
        return CursorCount.count.get() > 120;
    }

    /**
     * 持续检测鼠标位置，如果在一秒内移动过，则重置CursorCount.count
     */
    private static void startGetCursorPosTimer() {
        Point lastPoint = new Point();
        Timer timer = new Timer(1000, (event) -> {
            Point point = getCursorPoint();
            if (!point.equals(lastPoint)) {
                CursorCount.count.set(0);
            }
            lastPoint.setLocation(point);
        });
        timer.start();
    }

    private static Point getCursorPoint() {
        return java.awt.MouseInfo.getPointerInfo().getLocation();
    }

    private static boolean checkIndex() {
        int startTimes = 0;
        File startTimeCount = new File("user/startTimeCount.dat");
        boolean isFileCreated;
        boolean ret = false;
        if (startTimeCount.exists()) {
            isFileCreated = true;
        } else {
            try {
                isFileCreated = startTimeCount.createNewFile();
            } catch (IOException e) {
                isFileCreated = false;
                e.printStackTrace();
            }
        }
        if (isFileCreated) {
            try (BufferedReader reader =
                         new BufferedReader(new InputStreamReader(new FileInputStream(startTimeCount), StandardCharsets.UTF_8))) {
                //读取启动次数
                String times = reader.readLine();
                if (!(times == null || times.isEmpty())) {
                    try {
                        startTimes = Integer.parseInt(times);
                        //使用次数大于3次，优化数据库
                        if (startTimes >= Constants.UPDATE_DATABASE_THRESHOLD) {
                            startTimes = 0;
                            if (DatabaseService.getInstance().getStatus() == Enums.DatabaseStatus.NORMAL) {
                                ret = true;
                            }
                        }
                    } catch (NumberFormatException e) {
                        ret = true;
                    }
                }
                //自增后写入
                startTimes++;
                try (BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(new FileOutputStream(startTimeCount), StandardCharsets.UTF_8))) {
                    writer.write(String.valueOf(startTimes));
                }
            } catch (Exception throwables) {
                throwables.printStackTrace();
            }
        }
        return ret;
    }

    private static void sendStartSignal() {
        EventManagement eventManagement = EventManagement.getInstance();

        Event event = new ReadConfigsAndBootSystemEvent();
        eventManagement.putEvent(event);
        if (eventManagement.waitForEvent(event)) {
            throw new RuntimeException("初始化失败");
        }
    }

    private static void releaseAllDependence() throws IOException {
        copyOrIgnoreFile("user/fileMonitor.dll", "/win32-native/fileMonitor.dll", FILE_MONITOR_64_MD_5);
        copyOrIgnoreFile("user/getAscII.dll", "/win32-native/getAscII.dll", GET_ASC_II_64_MD_5);
        copyOrIgnoreFile("user/hotkeyListener.dll", "/win32-native/hotkeyListener.dll", HOTKEY_LISTENER_64_MD_5);
        copyOrIgnoreFile("user/isLocalDisk.dll", "/win32-native/isLocalDisk.dll", IS_LOCAL_DISK_64_MD_5);
        copyOrIgnoreFile("user/fileSearcherUSN.exe", "/win32-native/fileSearcherUSN.exe", FILE_SEARCHER_USN_64_MD_5);
        copyOrIgnoreFile("user/sqlite3.dll", "/win32-native/sqlite3.dll", SQLITE3_64_MD_5);
        copyOrIgnoreFile("user/getHandle.dll", "/win32-native/getHandle.dll", GET_HANDLE_64_MD_5);
        copyOrIgnoreFile("user/daemonProcess.exe", "/win32-native/daemonProcess.exe", DAEMON_PROCESS_64_MD_5);
        copyOrIgnoreFile("user/shortcutGenerator.vbs", "/shortcutGenerator.vbs", SHORTCUT_GEN_MD_5);
    }

    private static void copyOrIgnoreFile(String path, String rootPath, String md5) throws IOException {
        File target = new File(path);
        String fileMd5 = Md5Util.getMD5(target.getAbsolutePath());
        if (!target.exists() || !md5.equals(fileMd5)) {
            if (IsDebug.isDebug()) {
                System.out.println("正在重新释放文件：" + path);
            }
            try (InputStream resource = MainClass.class.getResourceAsStream(rootPath)) {
                CopyFileUtil.copyFile(resource, target);
            }
        }
    }

    private static void startOrIgnoreUpdateAndExit(boolean isUpdate) throws InterruptedException, IOException {
        if (isUpdate) {
            File closeSignal = new File("tmp/closeDaemon");
            if (closeSignal.createNewFile()) {
                System.err.println("添加退出守护进程标志失败");
            }
            File update = new File("user/update");
            if (update.delete()) {
                System.err.println("删除更新标志失败");
            }
            File updaterBat = new File("updater.bat");
            copyOrIgnoreFile("updater.bat", "/updater.bat", UPDATER_BAT_64_MD_5);
            Desktop desktop;
            if (Desktop.isDesktopSupported()) {
                desktop = Desktop.getDesktop();
                desktop.open(updaterBat);
                Thread.sleep(500);
                System.exit(0);
            }
        } else {
            deleteUpdater();
        }
    }

    private static void initFoldersAndFiles() throws IOException {
        boolean isSucceeded;
        //user
        isSucceeded = createFileOrFolder("user", false, false);
        //plugins
        isSucceeded &= createFileOrFolder("plugins", false, false);
        //tmp
        File tmp = new File("tmp");
        String tempPath = tmp.getAbsolutePath();
        isSucceeded &= createFileOrFolder(tmp, false, false);
        isSucceeded &= createFileOrFolder(tempPath + File.separator + "fileAdded.txt", true, true);
        isSucceeded &= createFileOrFolder(tempPath + File.separator + "fileRemoved.txt", true, true);
        //cmd.txt
        isSucceeded &= createFileOrFolder("user/cmds.txt", true, false);
        releaseAllDependence();
        if (!isSucceeded) {
            throw new RuntimeException("初始化依赖项失败");
        }
    }

    private static boolean createFileOrFolder(File file, boolean isFile, boolean isDeleteOnExit) {
        boolean result;
        try {
            if (!file.exists()) {
                if (isFile) {
                    result = file.createNewFile();
                } else {
                    result = file.mkdirs();
                }
                if (isDeleteOnExit) {
                    file.deleteOnExit();
                }
            } else {
                result = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
            result = false;
        }
        return result;
    }

    private static boolean createFileOrFolder(String path, boolean isFile, boolean isDeleteOnExit) {
        File file = new File(path);
        return createFileOrFolder(file, isFile, isDeleteOnExit);
    }

    private static boolean isLatest() {
        //检测是否为最新版本
        try {
            JSONObject info = AllConfigs.getInstance().getUpdateInfo();
            if (info != null) {
                String latestVersion = info.getString("version");
                if (Double.parseDouble(latestVersion) > Double.parseDouble(AllConfigs.version)) {
                    return false;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    private static boolean isUpdateSignExist() {
        File user = new File("user/update");
        return user.exists();
    }
}
