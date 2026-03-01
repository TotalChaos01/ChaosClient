const { app, BrowserWindow, ipcMain, shell, dialog } = require('electron');
const path = require('path');
const fs = require('fs');
const Store = require('electron-store');
const { MinecraftLauncher } = require('./minecraft-launcher');
const { JavaManager } = require('./java-manager');

// Fix sandbox on Linux
app.commandLine.appendSwitch('no-sandbox');

const GAME_DIR = path.join(app.getPath('home'), '.chaosclient');

const store = new Store({
    defaults: {
        username: '',
        memory: 4096,
        javaMode: 'bundled',
        customJavaPath: '',
        gameDir: GAME_DIR,
        updateChannel: 'release',
        wizardDone: false,
        selectedMods: ['sodium', 'lithium']
    }
});

let mainWindow;
const javaManager = new JavaManager(GAME_DIR);
const mcLauncher = new MinecraftLauncher(store, javaManager, GAME_DIR);

function createWindow() {
    mainWindow = new BrowserWindow({
        width: 1100,
        height: 700,
        minWidth: 900,
        minHeight: 600,
        frame: false,
        transparent: false,
        resizable: true,
        backgroundColor: '#0a0a0f',
        webPreferences: {
            preload: path.join(__dirname, '../preload/preload.js'),
            contextIsolation: true,
            nodeIntegration: false,
            sandbox: false
        },
        icon: path.join(__dirname, '../../build/icon.png'),
        titleBarStyle: 'hidden',
        show: false
    });

    mainWindow.loadFile(path.join(__dirname, '../renderer/index.html'));
    mainWindow.once('ready-to-show', () => mainWindow.show());
    mainWindow.on('closed', () => { mainWindow = null; });
}

app.whenReady().then(createWindow);
app.on('window-all-closed', () => app.quit());

// ===== Управление окном =====
ipcMain.on('window:minimize', () => mainWindow?.minimize());
ipcMain.on('window:maximize', () => {
    mainWindow?.isMaximized() ? mainWindow.unmaximize() : mainWindow?.maximize();
});
ipcMain.on('window:close', () => mainWindow?.close());

// ===== Настройки =====
ipcMain.handle('settings:get', (_, key) => store.get(key));
ipcMain.handle('settings:getAll', () => store.store);
ipcMain.on('settings:set', (_, key, value) => store.set(key, value));

// ===== Диалоги =====
ipcMain.handle('dialog:openFile', async () => {
    const result = await dialog.showOpenDialog(mainWindow, {
        properties: ['openFile'],
        filters: [{ name: 'Java Binary', extensions: ['*'] }]
    });
    return result.canceled ? null : result.filePaths[0];
});

ipcMain.handle('dialog:openDirectory', async () => {
    const result = await dialog.showOpenDialog(mainWindow, { properties: ['openDirectory'] });
    return result.canceled ? null : result.filePaths[0];
});

// ===== Внешние ссылки и файлы =====
ipcMain.on('open:external', (_, url) => shell.openExternal(url));
ipcMain.on('open:path', (_, p) => shell.openPath(p));

// ===== Java =====
ipcMain.handle('java:status', async () => ({
    bundledInstalled: javaManager.isBundledInstalled(),
    bundledPath: javaManager.getBundledJavaPath(),
    bundledVersion: javaManager.getBundledVersion()
}));

ipcMain.handle('java:installBundled', async (event) => {
    try {
        javaManager.removeAllListeners();
        javaManager.on('progress', (data) => event.sender.send('java:progress', data));
        javaManager.on('status', (msg) => event.sender.send('java:status-msg', msg));
        await javaManager.downloadAndInstall();
        return { success: true };
    } catch (err) { return { success: false, error: err.message }; }
});

ipcMain.handle('java:verify', async (_, javaPath) => {
    try {
        const ver = await javaManager.verifyJava(javaPath);
        return { success: true, version: ver };
    } catch (err) { return { success: false, error: err.message }; }
});

// Автоопределение Java
ipcMain.handle('java:detect', async () => {
    return javaManager.detectAllJava();
});

// ===== Minecraft =====
ipcMain.handle('minecraft:launch', async (event) => {
    try {
        mcLauncher.removeAllListeners();
        mcLauncher.on('progress', (data) => event.sender.send('launch:progress', data));
        mcLauncher.on('status', (msg) => event.sender.send('launch:status', msg));
        mcLauncher.on('log', (entry) => event.sender.send('launch:log', entry));
        mcLauncher.on('game-exit', (data) => event.sender.send('launch:game-exit', data));
        await mcLauncher.launch();
        return { success: true, pid: mcLauncher.getGamePid() };
    } catch (err) {
        const ts = new Date().toLocaleTimeString('ru-RU', { hour12: false });
        event.sender.send('launch:log', { time: ts, level: 'error', message: `FATAL: ${err.message}` });
        if (err.stack) event.sender.send('launch:log', { time: ts, level: 'error', message: err.stack });
        return { success: false, error: err.message };
    }
});

ipcMain.handle('minecraft:getState', async () => mcLauncher.getInstallState());

// Переустановка
ipcMain.handle('minecraft:reinstall', async () => {
    const gameDir = store.get('gameDir') || GAME_DIR;
    // Удаляем всё кроме runtime (Java) и logs
    const keepDirs = ['runtime', 'logs'];
    try {
        if (fs.existsSync(gameDir)) {
            const entries = fs.readdirSync(gameDir);
            for (const entry of entries) {
                if (keepDirs.includes(entry)) continue;
                const fullPath = path.join(gameDir, entry);
                fs.rmSync(fullPath, { recursive: true, force: true });
            }
        }
        return { success: true };
    } catch (e) { return { success: false, error: e.message }; }
});

// Очистка модов
ipcMain.handle('minecraft:clearMods', async () => {
    const modsDir = path.join(store.get('gameDir') || GAME_DIR, 'mods');
    try {
        if (fs.existsSync(modsDir)) fs.rmSync(modsDir, { recursive: true, force: true });
        return { success: true };
    } catch (e) { return { success: false, error: e.message }; }
});

// Статистика процесса
ipcMain.handle('minecraft:processStats', async () => {
    return mcLauncher.getProcessStats();
});

// ===== Обновления и новости =====
ipcMain.handle('app:checkUpdate', async () => {
    // Placeholder — реальная проверка будет через GitHub API
    return { hasUpdate: false, latestVersion: app.getVersion() };
});

ipcMain.handle('app:getNews', async () => {
    // Попытка загрузить с GitHub
    try {
        const https = require('https');
        const data = await new Promise((resolve, reject) => {
            // Читаем последние 5 коммитов из repo
            const url = 'https://api.github.com/repos/totalchaos01/ChaosClient-releases/commits?per_page=5';
            https.get(url, { headers: { 'User-Agent': 'ChaosClient-Launcher/1.0' }, timeout: 5000 }, (res) => {
                if (res.statusCode !== 200) { resolve(null); return; }
                let body = '';
                res.on('data', c => body += c);
                res.on('end', () => {
                    try { resolve(JSON.parse(body)); } catch (e) { resolve(null); }
                });
            }).on('error', () => resolve(null));
        });

        if (data && Array.isArray(data)) {
            return data.map(c => {
                const msg = c.commit?.message || '';
                const lines = msg.split('\n');
                const title = lines[0] || 'Обновление';
                const text = lines.slice(1).join(' ').trim() || title;
                const date = c.commit?.author?.date ? new Date(c.commit.author.date).toLocaleDateString('ru-RU', { day: 'numeric', month: 'long', year: 'numeric' }) : '';
                const isRelease = title.toLowerCase().includes('release') || title.toLowerCase().includes('релиз');
                const isFix = title.toLowerCase().includes('fix') || title.toLowerCase().includes('исправ');
                return {
                    tag: isRelease ? 'Релиз' : isFix ? 'Исправление' : 'Нововведение',
                    date, title, text
                };
            });
        }
    } catch (e) { /* fallback to static */ }
    return null; // Use static news from HTML
});

// ===== Информация о приложении =====
ipcMain.handle('app:version', () => app.getVersion());
ipcMain.handle('app:platform', () => process.platform);
ipcMain.handle('app:gameDir', () => GAME_DIR);

// ===== Установка стандартных настроек игры при первом запуске =====
function applyDefaultGameSettings() {
    const gameDir = store.get('gameDir') || GAME_DIR;
    const optionsPath = path.join(gameDir, 'options.txt');

    // Применяем только если файл не существует (первый запуск)
    if (!fs.existsSync(optionsPath)) {
        fs.mkdirSync(gameDir, { recursive: true });
        const defaults = [
            'lang:ru_ru',
            'soundCategory_master:1.0',
            'soundCategory_music:0.5',
            'soundCategory_hostile:1.0',
            'soundCategory_neutral:1.0',
            'soundCategory_players:1.0',
            'soundCategory_ambient:1.0',
            'soundCategory_blocks:1.0',
            'soundCategory_records:1.0',
            'soundCategory_weather:1.0',
            'soundCategory_voice:1.0',
            'guiScale:2',
            'autoJump:false',
            'darkMojangStudiosBackground:true',
            'tutorialStep:none',
            'skipMultiplayerWarning:true',
            'joinedFirstServer:true',
            'renderDistance:12',
            'simulationDistance:8',
            'fov:0.0',
            'gamma:1.0',
            'chatScale:1.0',
            'chatWidth:1.0',
            'narrator:0',
            'showSubtitles:false'
        ].join('\n') + '\n';
        fs.writeFileSync(optionsPath, defaults);
    }
}

// Вызываем при старте приложения
try { applyDefaultGameSettings(); } catch (e) { /* ignore */ }
