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
            sandbox: false,
            devTools: false
        },
        icon: path.join(__dirname, '../../build/icon.png'),
        titleBarStyle: 'hidden',
        show: false
    });

    mainWindow.loadFile(path.join(__dirname, '../renderer/index.html'));
    mainWindow.once('ready-to-show', () => mainWindow.show());
    mainWindow.on('closed', () => { mainWindow = null; });

    // Блокировка F5/F12/Ctrl+R/Ctrl+Shift+I — запрет обновления и DevTools
    mainWindow.webContents.on('before-input-event', (event, input) => {
        if (input.key === 'F5' || input.key === 'F12') { event.preventDefault(); return; }
        if (input.control && input.key.toLowerCase() === 'r') { event.preventDefault(); return; }
        if (input.control && input.shift && input.key.toLowerCase() === 'i') { event.preventDefault(); return; }
    });
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

// ===== GitHub API helper =====
const GITHUB_REPO = 'TotalChaos01/ChaosClient';
const GITHUB_API = `https://api.github.com/repos/${GITHUB_REPO}`;

function githubFetch(urlPath, timeout = 10000) {
    const https = require('https');
    const fullUrl = urlPath.startsWith('https://') ? urlPath : `${GITHUB_API}${urlPath}`;
    return new Promise((resolve, reject) => {
        let settled = false;
        const settle = (fn, val) => { if (!settled) { settled = true; fn(val); } };
        const doReq = (url, redirects = 0) => {
            if (redirects > 5) { settle(reject, new Error('Too many redirects')); return; }
            const req = https.get(url, {
                headers: { 'User-Agent': 'ChaosClient-Launcher/1.0', 'Accept': 'application/vnd.github.v3+json' },
                timeout
            }, (res) => {
                if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
                    doReq(res.headers.location, redirects + 1); return;
                }
                if (res.statusCode !== 200) { settle(reject, new Error(`HTTP ${res.statusCode}`)); return; }
                let body = '';
                res.on('data', c => body += c);
                res.on('end', () => { try { settle(resolve, JSON.parse(body)); } catch (e) { settle(reject, e); } });
                res.on('error', e => settle(reject, e));
            });
            req.on('error', e => settle(reject, e));
            req.on('timeout', () => { req.destroy(); settle(reject, new Error('Timeout')); });
        };
        doReq(fullUrl);
    });
}

// ===== Обновления и новости =====
ipcMain.handle('app:checkUpdate', async () => {
    try {
        const channel = store.get('updateChannel') || 'release';
        const currentVersion = app.getVersion();

        if (channel === 'release') {
            const release = await githubFetch('/releases/latest');
            const latestTag = release.tag_name || '';
            const latestVersion = latestTag.replace(/^v/, '');
            const modAsset = (release.assets || []).find(a => a.name.endsWith('.jar') && a.name.includes('ChaosClient'));
            const launcherAssets = (release.assets || []).filter(a =>
                a.name.endsWith('.AppImage') || a.name.endsWith('.deb') || a.name.endsWith('.exe')
            );
            const hasModUpdate = !!modAsset;
            const hasLauncherUpdate = latestVersion !== currentVersion && launcherAssets.length > 0;
            return {
                hasUpdate: hasModUpdate || hasLauncherUpdate,
                latestVersion,
                currentVersion,
                releaseTag: latestTag,
                releaseName: release.name || latestTag,
                releaseBody: release.body || '',
                publishedAt: release.published_at || '',
                modAsset: modAsset ? { name: modAsset.name, size: modAsset.size, url: modAsset.browser_download_url } : null,
                launcherAssets: launcherAssets.map(a => ({ name: a.name, size: a.size, url: a.browser_download_url }))
            };
        } else {
            // Dev channel: check pre-releases or latest commits with artifacts
            const releases = await githubFetch('/releases?per_page=10');
            const preRelease = releases.find(r => r.prerelease);
            if (preRelease) {
                const modAsset = (preRelease.assets || []).find(a => a.name.endsWith('.jar') && a.name.includes('ChaosClient'));
                return {
                    hasUpdate: !!modAsset,
                    latestVersion: (preRelease.tag_name || '').replace(/^v/, ''),
                    currentVersion,
                    releaseTag: preRelease.tag_name,
                    releaseName: preRelease.name || preRelease.tag_name,
                    releaseBody: preRelease.body || '',
                    publishedAt: preRelease.published_at || '',
                    isDev: true,
                    modAsset: modAsset ? { name: modAsset.name, size: modAsset.size, url: modAsset.browser_download_url } : null,
                    launcherAssets: []
                };
            }
            return { hasUpdate: false, currentVersion, latestVersion: currentVersion };
        }
    } catch (e) {
        return { hasUpdate: false, latestVersion: app.getVersion(), error: e.message };
    }
});

// Get all releases (for dev builds picker)
ipcMain.handle('app:getReleases', async () => {
    try {
        const releases = await githubFetch('/releases?per_page=30');
        return releases.map(r => ({
            tag: r.tag_name,
            name: r.name || r.tag_name,
            body: r.body || '',
            prerelease: r.prerelease,
            draft: r.draft,
            publishedAt: r.published_at,
            assets: (r.assets || []).map(a => ({
                name: a.name,
                size: a.size,
                url: a.browser_download_url,
                downloadCount: a.download_count
            }))
        }));
    } catch (e) { return []; }
});

// Get recent commits (for dev builds)
ipcMain.handle('app:getCommits', async () => {
    try {
        const commits = await githubFetch('/commits?per_page=20');
        return commits.map(c => ({
            sha: c.sha,
            shortSha: c.sha.substring(0, 7),
            message: c.commit?.message?.split('\n')[0] || '',
            fullMessage: c.commit?.message || '',
            author: c.commit?.author?.name || c.author?.login || 'unknown',
            date: c.commit?.author?.date || '',
            avatarUrl: c.author?.avatar_url || ''
        }));
    } catch (e) { return []; }
});

// Select a dev build for installation
ipcMain.handle('app:selectDevBuild', async (_, build) => {
    // build = { tag, fileName, downloadUrl } or from commit
    store.set('selectedDevBuild', build);
    return { success: true };
});

// Apply mod update (download from release)
ipcMain.handle('app:applyModUpdate', async (event, assetInfo) => {
    // assetInfo = { name, url }
    try {
        const gameDir = store.get('gameDir') || GAME_DIR;
        const modsDir = path.join(gameDir, 'mods');
        if (!fs.existsSync(modsDir)) fs.mkdirSync(modsDir, { recursive: true });

        // Remove old ChaosClient jars
        const existingMods = fs.readdirSync(modsDir).filter(f => f.startsWith('ChaosClient') && f.endsWith('.jar'));
        for (const old of existingMods) {
            fs.unlinkSync(path.join(modsDir, old));
        }

        const destPath = path.join(modsDir, assetInfo.name);
        // Use the mcLauncher downloader for retries
        await mcLauncher._downloadFile(assetInfo.url, destPath);
        return { success: true, fileName: assetInfo.name };
    } catch (e) {
        return { success: false, error: e.message };
    }
});

ipcMain.handle('app:getNews', async () => {
    try {
        const commits = await githubFetch('/commits?per_page=5');
        if (commits && Array.isArray(commits)) {
            return commits.map(c => {
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
    return null;
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
