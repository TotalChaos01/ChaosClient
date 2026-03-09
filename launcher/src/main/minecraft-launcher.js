const { EventEmitter } = require('events');
const fs = require('fs');
const path = require('path');
const https = require('https');
const http = require('http');
const { spawn, execSync } = require('child_process');
const { createWriteStream, mkdirSync } = require('fs');
const crypto = require('crypto');

// ── Persistent HTTP agents with connection pooling ──
// Reuses TCP+TLS connections → eliminates ~200ms handshake per request
const httpsAgent = new https.Agent({
    keepAlive: true,
    keepAliveMsecs: 15000,
    maxSockets: 32,         // max parallel connections per host
    maxTotalSockets: 64,    // max total across all hosts
    timeout: 30000,
});
const httpAgent = new http.Agent({
    keepAlive: true,
    keepAliveMsecs: 15000,
    maxSockets: 32,
    maxTotalSockets: 64,
    timeout: 30000,
});

const MINECRAFT_VERSION = '1.21.11';
const FABRIC_LOADER_VERSION = '0.18.4';
const FABRIC_API_VERSION = '0.141.3+1.21.11';
const VERSION_MANIFEST_URL = 'https://piston-meta.mojang.com/mc/game/version_manifest_v2.json';
const FABRIC_META_URL = 'https://meta.fabricmc.net';
const MODRINTH_API = 'https://api.modrinth.com/v2';

// ── Pre-built bundle: 1 ZIP instead of ~1000 individual downloads ──
const BUNDLE_TAG = `bundle-${MINECRAFT_VERSION}`;
const BUNDLE_FILENAME = `chaosclient-bundle-${MINECRAFT_VERSION}.zip`;
const BUNDLE_URL = `https://github.com/TotalChaos01/ChaosClient/releases/download/${BUNDLE_TAG}/${BUNDLE_FILENAME}`;

// Modrinth project IDs для модов
const MODRINTH_MODS = {
    sodium: 'AANobbMI',
    lithium: 'gvQqBUqZ',
    iris: 'YL57xq9U',
    lambdynamiclights: 'yBW8D80W',
    modmenu: 'mOgUt4GM'
};

class MinecraftLauncher extends EventEmitter {
    constructor(store, javaManager, gameDir) {
        super();
        this.store = store;
        this.javaManager = javaManager;
        this.gameDir = gameDir;
        this._gameProcess = null;
        this._launchTime = null;
    }

    getGamePid() {
        return this._gameProcess?.pid || null;
    }

    getProcessStats() {
        const pid = this.getGamePid();
        if (!pid) return null;

        try {
            // Check if process is still alive
            process.kill(pid, 0);
        } catch (e) {
            return null;
        }

        const stats = { pid, memoryMB: 0, cpu: 0, uptime: 0 };

        if (this._launchTime) {
            stats.uptime = Math.floor((Date.now() - this._launchTime) / 1000);
        }

        if (process.platform === 'linux') {
            try {
                // Memory from /proc/PID/statm (pages)
                const statm = fs.readFileSync(`/proc/${pid}/statm`, 'utf8').trim().split(' ');
                const pageSize = 4096;
                const rssPages = parseInt(statm[1]) || 0;
                stats.memoryMB = Math.round((rssPages * pageSize) / (1024 * 1024));
            } catch (e) {
                // Fallback: ps command for memory
                try {
                    const out = execSync(`ps -o rss= -p ${pid}`, { encoding: 'utf8', timeout: 3000 });
                    const kb = parseInt(out.trim());
                    if (kb) stats.memoryMB = Math.round(kb / 1024);
                } catch (e2) { /* ignore */ }
            }

            try {
                // CPU via ps -o %cpu for simplicity and reliability
                const cpuOut = execSync(`ps -o %cpu= -p ${pid}`, { encoding: 'utf8', timeout: 3000 });
                const cpuVal = parseFloat(cpuOut.trim());
                if (!isNaN(cpuVal)) stats.cpu = Math.round(cpuVal);
            } catch (e) {
                // Fallback: /proc/PID/stat calculation
                try {
                    const stat = fs.readFileSync(`/proc/${pid}/stat`, 'utf8').trim();
                    const afterComm = stat.substring(stat.lastIndexOf(')') + 2);
                    const fields = afterComm.split(' ');
                    const utime = parseInt(fields[11]) || 0;
                    const stime = parseInt(fields[12]) || 0;
                    const starttime = parseInt(fields[19]) || 0;
                    const clkTck = 100;
                    const uptime = parseFloat(fs.readFileSync('/proc/uptime', 'utf8').split(' ')[0]);
                    const totalTime = (utime + stime) / clkTck;
                    const elapsed = uptime - (starttime / clkTck);
                    if (elapsed > 0) stats.cpu = Math.min(Math.round((totalTime / elapsed) * 100), 100);
                } catch (e2) { /* ignore */ }
            }
        } else if (process.platform === 'win32') {
            try {
                const out = execSync(`wmic process where ProcessId=${pid} get WorkingSetSize /format:value`, { encoding: 'utf8', timeout: 3000 });
                const match = out.match(/WorkingSetSize=(\d+)/);
                if (match) stats.memoryMB = Math.round(parseInt(match[1]) / (1024 * 1024));
            } catch (e) { /* ignore */ }
        } else if (process.platform === 'darwin') {
            try {
                const out = execSync(`ps -o rss=,%cpu= -p ${pid}`, { encoding: 'utf8', timeout: 3000 });
                const parts = out.trim().split(/\s+/);
                const kb = parseInt(parts[0]);
                if (kb) stats.memoryMB = Math.round(kb / 1024);
                const cpu = parseFloat(parts[1]);
                if (!isNaN(cpu)) stats.cpu = Math.round(cpu);
            } catch (e) { /* ignore */ }
        }

        return stats;
    }

    async getInstallState() {
        const versionsDir = path.join(this.gameDir, 'versions');
        const fabricDir = path.join(versionsDir, `fabric-loader-${FABRIC_LOADER_VERSION}-${MINECRAFT_VERSION}`);
        const modsDir = path.join(this.gameDir, 'mods');
        const assetsDir = path.join(this.gameDir, 'assets');
        const libsDir = path.join(this.gameDir, 'libraries');

        return {
            versionInstalled: fs.existsSync(fabricDir),
            modsInstalled: fs.existsSync(modsDir) && fs.readdirSync(modsDir).length > 0,
            assetsDownloaded: fs.existsSync(assetsDir),
            librariesReady: fs.existsSync(libsDir)
        };
    }

    async launch() {
        this.emit('status', 'Подготовка к запуску...');
        this.emit('progress', { percent: 0, stage: 'init' });

        const username = this.store.get('username');
        if (!username) throw new Error('Укажите никнейм!');

        // Step 1: Ensure Java
        this.emit('status', 'Проверка Java...');
        const javaPath = await this._resolveJavaPath();
        this.emit('progress', { percent: 5, stage: 'java' });

        // Step 2: Download version manifest
        this.emit('status', 'Загрузка метаданных...');
        const versionData = await this._downloadVersionData();
        this.emit('progress', { percent: 10, stage: 'metadata' });

        // Step 3: Try bundle (1 ZIP with everything) — FAST PATH
        if (!this._isBundleInstalled()) {
            try {
                this.emit('status', 'Загрузка игровых файлов (бандл)...');
                await this._downloadAndExtractBundle();
                this.emit('progress', { percent: 60, stage: 'bundle' });
            } catch (e) {
                this._log('warn', `Бандл недоступен (${e.message}), загрузка по отдельности...`);
                // Fallback: download everything individually
                this.emit('status', 'Загрузка библиотек...');
                await this._downloadLibraries(versionData);
                this.emit('progress', { percent: 35, stage: 'libraries' });

                this.emit('status', 'Загрузка ресурсов...');
                await this._downloadAssets(versionData);
                this.emit('progress', { percent: 55, stage: 'assets' });

                this.emit('status', 'Загрузка клиента...');
                await this._downloadClientJar(versionData);
                this.emit('progress', { percent: 60, stage: 'client' });

                this.emit('status', 'Установка Fabric...');
                await this._installFabric();
                this.emit('progress', { percent: 65, stage: 'fabric' });
            }
        } else {
            this._log('info', 'Бандл уже установлен, пропуск загрузки');
            this.emit('progress', { percent: 60, stage: 'bundle-cached' });
        }

        // Step 3.5: Verify all platform libraries exist (safety net after bundle)
        // The bundle may not include every library; _downloadLibraries skips existing files
        this.emit('status', 'Проверка библиотек...');
        await this._downloadLibraries(versionData);
        this.emit('progress', { percent: 65, stage: 'libraries-verified' });

        // Step 4: Ensure Fabric is installed (may already be from bundle)
        this.emit('status', 'Проверка Fabric...');
        await this._installFabric();
        this.emit('progress', { percent: 70, stage: 'fabric' });

        // Step 5: Install mods
        this.emit('status', 'Установка модов...');
        await this._installAllMods();
        this.emit('progress', { percent: 85, stage: 'mods' });

        // Step 6: Apply default game settings
        this._applyDefaultGameSettings();

        // Step 7: Launch
        this.emit('status', 'Запуск ChaosClient...');
        this.emit('progress', { percent: 90, stage: 'launching' });
        await this._launchGame(javaPath, versionData);
        this.emit('progress', { percent: 100, stage: 'done' });
        this.emit('status', 'ChaosClient запущен!');
    }

    /**
     * Check if the bundle has already been extracted (libraries + assets + client jar exist).
     */
    _isBundleInstalled() {
        const clientJar = path.join(this.gameDir, 'versions', MINECRAFT_VERSION, `${MINECRAFT_VERSION}.jar`);
        const libsDir = path.join(this.gameDir, 'libraries');
        const assetsDir = path.join(this.gameDir, 'assets', 'objects');
        const fabricDir = path.join(this.gameDir, 'versions', `fabric-loader-${FABRIC_LOADER_VERSION}-${MINECRAFT_VERSION}`);

        if (!fs.existsSync(clientJar)) return false;
        if (!fs.existsSync(fabricDir)) return false;

        // Check that at least some libraries exist
        try {
            const libCount = this._countFiles(libsDir, '.jar');
            if (libCount < 50) return false; // should be ~69
        } catch (e) { return false; }

        // Check that at least some assets exist
        try {
            if (!fs.existsSync(assetsDir)) return false;
            const assetCount = this._countFilesRecursive(assetsDir);
            if (assetCount < 500) return false; // should be ~957
        } catch (e) { return false; }

        return true;
    }

    _countFiles(dir, ext) {
        if (!fs.existsSync(dir)) return 0;
        let count = 0;
        const walk = (d) => {
            for (const entry of fs.readdirSync(d, { withFileTypes: true })) {
                if (entry.isDirectory()) walk(path.join(d, entry.name));
                else if (!ext || entry.name.endsWith(ext)) count++;
            }
        };
        walk(dir);
        return count;
    }

    _countFilesRecursive(dir) {
        return this._countFiles(dir, null);
    }

    /**
     * Download the pre-built bundle ZIP from GitHub and extract it.
     * This replaces ~1000 individual HTTP requests with 1 download + unzip.
     */
    async _downloadAndExtractBundle() {
        const bundlePath = path.join(this.gameDir, BUNDLE_FILENAME);
        mkdirSync(this.gameDir, { recursive: true });

        // Download the ZIP
        this._log('info', `Загрузка бандла: ${BUNDLE_URL}`);
        this.emit('status', 'Загрузка игровых файлов (~437 МБ)...');

        await this._downloadFileWithProgress(BUNDLE_URL, bundlePath, (percent) => {
            this.emit('progress', { percent: 10 + Math.round(percent * 0.45), stage: 'bundle-download' });
            if (percent % 10 === 0) {
                this.emit('status', `Загрузка бандла... ${percent}%`);
            }
        });

        // Verify the file exists and has reasonable size
        const stat = fs.statSync(bundlePath);
        if (stat.size < 200 * 1024 * 1024) {
            fs.unlinkSync(bundlePath);
            throw new Error(`Бандл слишком маленький (${this._formatBytes(stat.size)}), возможно повреждён`);
        }

        this._log('info', `Бандл загружен: ${this._formatBytes(stat.size)}`);
        this.emit('status', 'Распаковка игровых файлов...');
        this.emit('progress', { percent: 58, stage: 'bundle-extract' });

        // Extract ZIP
        try {
            if (process.platform === 'win32') {
                execSync(`powershell -command "Expand-Archive -Path '${bundlePath}' -DestinationPath '${this.gameDir}' -Force"`, { timeout: 120000 });
            } else {
                execSync(`unzip -o -q "${bundlePath}" -d "${this.gameDir}"`, { timeout: 120000 });
            }
        } catch (e) {
            // Try node-based extraction as fallback
            this._log('warn', `unzip failed (${e.message}), trying manual extraction...`);
            throw new Error(`Не удалось распаковать бандл: ${e.message}`);
        }

        // Clean up ZIP to save disk space
        try { fs.unlinkSync(bundlePath); } catch (e) { /* ignore */ }

        this._log('info', 'Бандл успешно распакован');
        this.emit('progress', { percent: 65, stage: 'bundle-done' });
    }

    /**
     * Download a file with progress callback (percent 0-100).
     */
    _downloadFileWithProgress(url, destPath, onProgress) {
        return new Promise((resolve, reject) => {
            let settled = false;
            const settle = (fn, val) => { if (!settled) { settled = true; fn(val); } };
            const doRequest = (reqUrl, redirects = 0) => {
                if (redirects > 10) { settle(reject, new Error('Too many redirects')); return; }
                const mod = reqUrl.startsWith('https') ? https : http;
                const req = mod.get(reqUrl, {
                    agent: this._getAgent(reqUrl),
                    headers: { 'User-Agent': 'ChaosClient-Launcher/1.0', 'Connection': 'keep-alive' },
                    timeout: 300000 // 5 min for large bundle
                }, (res) => {
                    if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
                        let loc = res.headers.location;
                        if (loc.startsWith('/')) { const u = new URL(reqUrl); loc = u.origin + loc; }
                        res.resume();
                        doRequest(loc, redirects + 1);
                        return;
                    }
                    if (res.statusCode !== 200) {
                        res.resume();
                        settle(reject, new Error(`HTTP ${res.statusCode}`));
                        return;
                    }

                    const totalSize = parseInt(res.headers['content-length'] || '0', 10);
                    let downloaded = 0;
                    let lastPercent = -1;

                    mkdirSync(path.dirname(destPath), { recursive: true });
                    const file = createWriteStream(destPath);

                    res.on('data', (chunk) => {
                        downloaded += chunk.length;
                        if (totalSize > 0 && onProgress) {
                            const percent = Math.round((downloaded / totalSize) * 100);
                            if (percent !== lastPercent) {
                                lastPercent = percent;
                                onProgress(percent);
                            }
                        }
                    });

                    res.pipe(file);
                    file.on('finish', () => { file.close(); settle(resolve); });
                    file.on('error', (err) => {
                        file.close();
                        try { fs.unlinkSync(destPath); } catch (e) { /* ignore */ }
                        settle(reject, err);
                    });
                    res.on('error', (e) => { file.close(); settle(reject, e); });
                });
                req.on('error', (err) => settle(reject, err));
                req.on('timeout', () => { req.destroy(); settle(reject, new Error('Timeout')); });
            };
            doRequest(url);
        });
    }

    async _resolveJavaPath() {
        const javaMode = this.store.get('javaMode') || 'bundled';
        if (javaMode === 'custom') {
            const customPath = this.store.get('customJavaPath');
            if (customPath && fs.existsSync(customPath)) return customPath;
            this._log('warn', 'Пользовательская Java не найдена, используется встроенная');
        }

        if (this.javaManager.isBundledInstalled()) return this.javaManager.getBundledJavaPath();

        // Try to install bundled java
        this._log('info', 'Установка Java 21...');
        this.javaManager.removeAllListeners();
        this.javaManager.on('progress', (d) => this.emit('progress', d));
        this.javaManager.on('status', (m) => this.emit('status', m));
        await this.javaManager.downloadAndInstall();

        if (this.javaManager.isBundledInstalled()) return this.javaManager.getBundledJavaPath();

        // Fallback to system java
        try {
            const which = process.platform === 'win32' ? 'where java' : 'which java';
            const sysJava = execSync(which, { encoding: 'utf8', timeout: 5000 }).trim().split('\n')[0];
            if (sysJava) return sysJava;
        } catch (e) { /* no system java */ }

        throw new Error('Java 21 не найдена! Установите Java или используйте встроенную.');
    }

    async _downloadVersionData() {
        // Check cached
        const versionDir = path.join(this.gameDir, 'versions', MINECRAFT_VERSION);
        const versionJsonPath = path.join(versionDir, `${MINECRAFT_VERSION}.json`);

        if (fs.existsSync(versionJsonPath)) {
            return JSON.parse(fs.readFileSync(versionJsonPath, 'utf8'));
        }

        const manifest = await this._fetchJson(VERSION_MANIFEST_URL);
        const version = manifest.versions.find(v => v.id === MINECRAFT_VERSION);
        if (!version) throw new Error(`Версия ${MINECRAFT_VERSION} не найдена`);

        const versionData = await this._fetchJson(version.url);
        mkdirSync(versionDir, { recursive: true });
        fs.writeFileSync(versionJsonPath, JSON.stringify(versionData, null, 2));
        return versionData;
    }

    async _downloadLibraries(versionData) {
        const libsDir = path.join(this.gameDir, 'libraries');
        const libraries = (versionData.libraries || []).filter(lib => {
            if (!this._isLibraryAllowed(lib)) return false;
            return !!lib.downloads?.artifact;
        });
        let completed = 0;
        let failed = 0;
        const total = libraries.length;
        const batchSize = 15; // keep-alive позволяет больше параллельных загрузок
        const failedLibs = [];

        // Fallback Maven-репозитории
        const MAVEN_FALLBACKS = [
            'https://libraries.minecraft.net/',
            'https://repo1.maven.org/maven2/',
            'https://maven.neoforged.net/releases/',
            'https://maven.fabricmc.net/',
            'https://maven.google.com/',
            'https://repo.maven.apache.org/maven2/',
        ];

        for (let i = 0; i < total; i += batchSize) {
            const batch = libraries.slice(i, i + batchSize);
            await Promise.all(batch.map(async (lib) => {
                const artifact = lib.downloads.artifact;
                const libPath = path.join(libsDir, artifact.path);
                if (fs.existsSync(libPath) && fs.statSync(libPath).size > 0) { completed++; return; }
                mkdirSync(path.dirname(libPath), { recursive: true });

                // Попытка 1: основной URL из манифеста
                try {
                    await this._downloadFile(artifact.url, libPath);
                    completed++;
                    return;
                } catch (e) { /* fallback */ }

                // Попытка 2: гонка fallback'ов — кто первый ответит, тот и победил
                try {
                    await this._downloadWithFallbackRace(artifact.path, libPath, MAVEN_FALLBACKS);
                    this._log('info', `Библиотека ${lib.name}: скачана из fallback`);
                    completed++;
                    return;
                } catch (e) { /* все провалились */ }

                this._log('error', `Библиотека НЕ скачана: ${lib.name}`);
                failedLibs.push({ name: lib.name, artifact });
                failed++;
                completed++;
            }));

            const progress = 10 + Math.round((completed / total) * 25);
            this.emit('progress', { percent: Math.min(progress, 35), stage: 'libraries' });
        }

        // ── Повторная попытка для неудавшихся ──
        if (failedLibs.length > 0) {
            this._log('info', `Повторная попытка для ${failedLibs.length} библиотек...`);
            this.emit('status', `Повтор загрузки ${failedLibs.length} библиотек...`);

            const stillFailed = [];
            // Повтор батчами по 5 параллельно
            for (let i = 0; i < failedLibs.length; i += 5) {
                const retryBatch = failedLibs.slice(i, i + 5);
                await Promise.all(retryBatch.map(async ({ name, artifact }) => {
                    const libPath = path.join(libsDir, artifact.path);
                    if (fs.existsSync(libPath) && fs.statSync(libPath).size > 0) return;

                    // Собираем все URL для гонки
                    const allUrls = [artifact.url, ...MAVEN_FALLBACKS.map(b => b + artifact.path)];
                    try {
                        await this._downloadFirstAvailable(allUrls, libPath);
                        this._log('info', `Библиотека ${name}: скачана при повторе`);
                        failed--;
                    } catch (e) {
                        stillFailed.push(name);
                    }
                }));
            }

            if (stillFailed.length > 0) {
                this._log('error', `Не удалось загрузить ${stillFailed.length} библиотек: ${stillFailed.join(', ')}`);
                const critical = stillFailed.filter(n =>
                    n.includes('guava') || n.includes('gson') || n.includes('netty') ||
                    n.includes('lwjgl') || n.includes('log4j') || n.includes('jna') ||
                    n.includes('commons-codec') || n.includes('commons-io')
                );
                if (critical.length > 0) {
                    throw new Error(`Критические библиотеки не загружены: ${critical.join(', ')}. Проверьте интернет-соединение.`);
                }
            }
        }

        this._log('info', `Загружено библиотек: ${completed - failed}, пропущено: ${failed}`);
    }

    _isLibraryAllowed(lib) {
        if (!lib.rules) return true;
        let allowed = false;
        for (const rule of lib.rules) {
            const action = rule.action === 'allow';
            if (rule.os) {
                const osName = { win32: 'windows', darwin: 'osx', linux: 'linux' }[process.platform];
                if (rule.os.name === osName) allowed = action;
            } else {
                allowed = action;
            }
        }
        return allowed;
    }

    async _downloadAssets(versionData) {
        const assetIndex = versionData.assetIndex;
        if (!assetIndex) return;

        const indexDir = path.join(this.gameDir, 'assets', 'indexes');
        const indexPath = path.join(indexDir, `${assetIndex.id}.json`);

        let indexData;
        if (fs.existsSync(indexPath)) {
            indexData = JSON.parse(fs.readFileSync(indexPath, 'utf8'));
        } else {
            mkdirSync(indexDir, { recursive: true });
            indexData = await this._fetchJson(assetIndex.url);
            fs.writeFileSync(indexPath, JSON.stringify(indexData, null, 2));
        }

        const objects = indexData.objects || {};
        const entries = Object.entries(objects);
        let downloaded = 0;
        let skipped = 0;
        let failed = 0;
        const totalEntries = entries.length;

        // Считаем сколько нужно скачать
        const toDownload = entries.filter(([, obj]) => {
            const prefix = obj.hash.substring(0, 2);
            const objPath = path.join(this.gameDir, 'assets', 'objects', prefix, obj.hash);
            return !fs.existsSync(objPath) || fs.statSync(objPath).size === 0;
        });
        const needDownload = toDownload.length;

        if (needDownload > 0) {
            this._log('info', `Ресурсы: нужно загрузить ${needDownload} из ${totalEntries}`);
        } else {
            this._log('info', `Ресурсы: все ${totalEntries} уже загружены`);
            return;
        }

        // Скачиваем батчами по 80 — keep-alive переиспользует соединения
        const assetBatchSize = 80;
        const failedAssets = [];
        for (let i = 0; i < needDownload; i += assetBatchSize) {
            const batch = toDownload.slice(i, i + assetBatchSize);
            await Promise.all(batch.map(async ([name, obj]) => {
                const hash = obj.hash;
                const prefix = hash.substring(0, 2);
                const objectPath = path.join(this.gameDir, 'assets', 'objects', prefix, hash);

                try {
                    mkdirSync(path.dirname(objectPath), { recursive: true });
                    await this._downloadFile(`https://resources.download.minecraft.net/${prefix}/${hash}`, objectPath);
                    downloaded++;
                } catch (e) {
                    failedAssets.push([name, obj]);
                }
            }));
            const progress = 35 + Math.round(((i + batch.length) / needDownload) * 20);
            this.emit('progress', { percent: Math.min(progress, 55), stage: 'assets' });
            this.emit('status', `Загрузка ресурсов... ${downloaded}/${needDownload}`);
        }

        // Повторная попытка для неудавшихся (батчами по 40)
        if (failedAssets.length > 0) {
            this._log('info', `Ресурсы: повтор для ${failedAssets.length} неудавшихся...`);
            for (let i = 0; i < failedAssets.length; i += 40) {
                const batch = failedAssets.slice(i, i + 40);
                await Promise.all(batch.map(async ([name, obj]) => {
                    const hash = obj.hash;
                    const prefix = hash.substring(0, 2);
                    const objectPath = path.join(this.gameDir, 'assets', 'objects', prefix, hash);
                    try {
                        await this._downloadFile(`https://resources.download.minecraft.net/${prefix}/${hash}`, objectPath);
                        downloaded++;
                    } catch (e) {
                        failed++;
                    }
                }));
                this.emit('status', `Повтор ресурсов... ${downloaded}/${needDownload}`);
            }
        }

        skipped = totalEntries - needDownload;
        this._log('info', `Ресурсы: загружено ${downloaded}, кешировано ${skipped}${failed > 0 ? `, ошибок: ${failed}` : ''}`);
    }

    async _downloadClientJar(versionData) {
        const clientDownload = versionData.downloads?.client;
        if (!clientDownload) throw new Error('Не найдена ссылка для загрузки клиента');

        const versionsDir = path.join(this.gameDir, 'versions', MINECRAFT_VERSION);
        const clientJar = path.join(versionsDir, `${MINECRAFT_VERSION}.jar`);

        if (fs.existsSync(clientJar)) return;

        mkdirSync(versionsDir, { recursive: true });
        this._log('info', `Загрузка клиента (${this._formatBytes(clientDownload.size)})...`);
        await this._downloadFile(clientDownload.url, clientJar);
        this._log('info', 'Клиент загружен');
    }

    async _installFabric() {
        const fabricVersionId = `fabric-loader-${FABRIC_LOADER_VERSION}-${MINECRAFT_VERSION}`;
        const fabricDir = path.join(this.gameDir, 'versions', fabricVersionId);
        const fabricJson = path.join(fabricDir, `${fabricVersionId}.json`);

        if (fs.existsSync(fabricJson)) return;

        // Download Fabric profile
        const profileUrl = `${FABRIC_META_URL}/v2/versions/loader/${MINECRAFT_VERSION}/${FABRIC_LOADER_VERSION}/profile/json`;
        const profile = await this._fetchJson(profileUrl);

        mkdirSync(fabricDir, { recursive: true });
        fs.writeFileSync(fabricJson, JSON.stringify(profile, null, 2));
        this._log('info', 'Профиль Fabric установлен');

        // Download Fabric libraries in parallel
        const libsDir = path.join(this.gameDir, 'libraries');
        const fabricLibs = (profile.libraries || []).map(lib => {
            const [group, artifact, version] = lib.name.split(':');
            const groupPath = group.replace(/\./g, '/');
            const relPath = `${groupPath}/${artifact}/${version}/${artifact}-${version}.jar`;
            return { name: lib.name, url: (lib.url || 'https://maven.fabricmc.net/') + relPath, path: path.join(libsDir, relPath) };
        }).filter(l => !fs.existsSync(l.path));

        if (fabricLibs.length > 0) {
            await Promise.all(fabricLibs.map(async (lib) => {
                try {
                    mkdirSync(path.dirname(lib.path), { recursive: true });
                    await this._downloadFile(lib.url, lib.path);
                } catch (e) {
                    this._log('warn', `Fabric библиотека: ${lib.name}: ${e.message}`);
                }
            }));
        }

        this._log('info', 'Fabric библиотеки установлены');
    }

    async _installAllMods() {
        const modsDir = path.join(this.gameDir, 'mods');
        mkdirSync(modsDir, { recursive: true });

        // Install ChaosClient mod (always)
        await this._installClientMod(modsDir);

        // Install Fabric API (always required)
        await this._installFabricApi(modsDir);

        // Install selected mods from wizard
        const selectedMods = this.store.get('selectedMods') || ['sodium', 'lithium'];

        for (const modId of selectedMods) {
            if (MODRINTH_MODS[modId]) {
                await this._installModrinthMod(modId, MODRINTH_MODS[modId], modsDir);
            }
        }

        // Clean old/unselected mods
        this._cleanOldMods(modsDir, selectedMods);
    }

    async _installClientMod(modsDir) {
        // Find existing ChaosClient jars in mods folder (exclude sources jars)
        let existingMods = [];
        try {
            existingMods = fs.readdirSync(modsDir).filter(f => f.startsWith('ChaosClient') && f.endsWith('.jar') && !f.includes('-sources'));
            // Clean up any -sources.jar that accidentally ended up in mods
            const sourcesJars = fs.readdirSync(modsDir).filter(f => f.startsWith('ChaosClient') && f.includes('-sources'));
            for (const sj of sourcesJars) {
                try { fs.unlinkSync(path.join(modsDir, sj)); this._log('info', `Удалён sources-jar: ${sj}`); } catch (e) { /* ignore */ }
            }
        } catch (e) { /* ignore */ }

        // Helper: extract version from jar filename "ChaosClient-X.Y.Z.jar" → "X.Y.Z"
        const extractVersion = (name) => {
            const m = name.match(/ChaosClient-(.+)\.jar/);
            return m ? m[1] : null;
        };

        // Check if an update was previously applied (stored in config)
        const updatedModFile = this.store.get('updatedModFile') || null;
        if (updatedModFile && !updatedModFile.includes('-sources') && existingMods.includes(updatedModFile)) {
            const updatedPath = path.join(modsDir, updatedModFile);
            if (fs.existsSync(updatedPath) && fs.statSync(updatedPath).size > 1000) {
                this._log('info', `ChaosClient мод актуален: ${updatedModFile}`);
                return;
            }
        }

        // Determine update channel
        const channel = this.store.get('updateChannel') || 'release';
        const selectedDevBuild = this.store.get('selectedDevBuild') || null;

        // DEV channel: use selected commit build if available
        if (channel === 'dev' && selectedDevBuild && selectedDevBuild.downloadUrl) {
            try {
                for (const old of existingMods) {
                    try { fs.unlinkSync(path.join(modsDir, old)); } catch (e) { /* ignore */ }
                }
                const devDest = path.join(modsDir, selectedDevBuild.fileName || 'ChaosClient-dev.jar');
                this._log('info', `Загрузка дев-билда: ${selectedDevBuild.fileName}...`);
                await this._downloadFile(selectedDevBuild.downloadUrl, devDest);
                this.store.set('updatedModFile', selectedDevBuild.fileName || 'ChaosClient-dev.jar');
                this._log('info', `ChaosClient дев-билд установлен: ${selectedDevBuild.fileName}`);
                return;
            } catch (e) {
                this._log('warn', `Не удалось скачать дев-билд: ${e.message}, пробуем release...`);
            }
        }

        // RELEASE channel: try to get latest from GitHub
        try {
            const releasesUrl = 'https://api.github.com/repos/TotalChaos01/ChaosClient/releases/latest';
            const release = await this._fetchJson(releasesUrl);
            if (release?.assets) {
                const modAsset = release.assets.find(a => a.name.endsWith('.jar') && a.name.includes('ChaosClient') && !a.name.includes('-sources'));
                if (modAsset) {
                    const ghDest = path.join(modsDir, modAsset.name);
                    const latestVersion = extractVersion(modAsset.name);
                    const installedVersion = existingMods.length > 0 ? extractVersion(existingMods[0]) : null;

                    // FIX: Compare versions instead of file sizes.
                    // If we already have the same version installed, skip download
                    if (latestVersion && latestVersion === installedVersion && fs.existsSync(path.join(modsDir, existingMods[0]))) {
                        this._log('info', `ChaosClient мод актуален (v${latestVersion})`);
                        this.store.set('updatedModFile', existingMods[0]);
                        return;
                    }

                    // Remove old jars before downloading new
                    for (const old of existingMods) {
                        try { fs.unlinkSync(path.join(modsDir, old)); } catch (e) { /* ignore */ }
                    }
                    this._log('info', `Загрузка ChaosClient ${release.tag_name} с GitHub...`);
                    await this._downloadFile(modAsset.browser_download_url, ghDest);
                    this.store.set('updatedModFile', modAsset.name);
                    this._log('info', `ChaosClient мод обновлён: ${modAsset.name}`);
                    return;
                }
            }
        } catch (e) {
            this._log('warn', `Не удалось скачать мод с GitHub: ${e.message}`);
        }

        // If there's already a ChaosClient jar from previous install, keep it
        if (existingMods.length > 0) {
            this._log('info', `ChaosClient мод уже установлен: ${existingMods[0]} (офлайн)`);
            return;
        }

        // Fallback: install from bundled resources (first launch / no internet)
        const bundledFileName = 'ChaosClient-1.4.0.jar';
        const bundledDest = path.join(modsDir, bundledFileName);
        const bundledPaths = [
            path.join(process.resourcesPath || '', bundledFileName),
            path.join(__dirname, '..', '..', '..', 'build', 'libs', bundledFileName),
            path.join(__dirname, '..', '..', 'build', 'libs', bundledFileName),
        ];

        for (const srcPath of bundledPaths) {
            if (fs.existsSync(srcPath)) {
                fs.copyFileSync(srcPath, bundledDest);
                this.store.set('updatedModFile', bundledFileName);
                this._log('info', `ChaosClient мод установлен из бандла: ${srcPath}`);
                return;
            }
        }

        this._log('warn', 'ChaosClient мод не найден');
    }

    async _installFabricApi(modsDir) {
        // Check if any fabric-api jar exists already
        const existing = fs.readdirSync(modsDir).filter(f => f.includes('fabric-api') || f.includes('fabric_api'));
        if (existing.length > 0) return;

        const projectId = 'P7dR8mSH'; // fabric-api
        try {
            await this._installModrinthMod('fabric-api', projectId, modsDir);
        } catch (e) {
            this._log('warn', `Fabric API: ${e.message} — попытка прямой загрузки`);
            // Fallback: direct modrinth download
            try {
                const url = `https://cdn.modrinth.com/data/${projectId}/versions/latest/fabric-api-${FABRIC_API_VERSION}.jar`;
                const dest = path.join(modsDir, `fabric-api-${FABRIC_API_VERSION}.jar`);
                await this._downloadFile(url, dest);
            } catch (e2) {
                this._log('error', `Не удалось установить Fabric API: ${e2.message}`);
            }
        }
    }

    async _installModrinthMod(modName, projectId, modsDir) {
        // Check if already installed
        const files = fs.readdirSync(modsDir);
        const modNameLower = modName.toLowerCase();
        if (files.some(f => f.toLowerCase().includes(modNameLower) && f.endsWith('.jar'))) {
            return; // Already installed
        }

        try {
            const versionsUrl = `${MODRINTH_API}/project/${projectId}/version?game_versions=["${MINECRAFT_VERSION}"]&loaders=["fabric"]`;
            const versions = await this._fetchJson(versionsUrl);

            if (!versions || versions.length === 0) {
                this._log('warn', `${modName}: нет версии для ${MINECRAFT_VERSION}`);
                return;
            }

            // Pick latest
            const version = versions[0];
            const file = version.files?.find(f => f.primary) || version.files?.[0];
            if (!file) {
                this._log('warn', `${modName}: нет файла для загрузки`);
                return;
            }

            const destPath = path.join(modsDir, file.filename);
            if (fs.existsSync(destPath)) return;

            this._log('info', `Загрузка ${modName}...`);
            await this._downloadFile(file.url, destPath);
            this._log('info', `${modName} установлен`);
        } catch (e) {
            this._log('warn', `Ошибка установки ${modName}: ${e.message}`);
        }
    }

    _cleanOldMods(modsDir, selectedMods) {
        // Remove mods that are no longer selected (but keep ChaosClient and fabric-api)
        const keepPatterns = ['chaosclient', 'fabric-api', 'fabric_api'];
        selectedMods.forEach(m => keepPatterns.push(m.toLowerCase()));

        try {
            const files = fs.readdirSync(modsDir);
            for (const file of files) {
                // Handle both .jar and .jar.disabled files
                if (!file.endsWith('.jar') && !file.endsWith('.jar.disabled')) continue;
                const lower = file.toLowerCase();
                const shouldKeep = keepPatterns.some(p => lower.includes(p));
                if (!shouldKeep) {
                    const allKnownMods = Object.keys(MODRINTH_MODS);
                    const isKnownMod = allKnownMods.some(m => lower.includes(m));
                    if (isKnownMod) {
                        fs.unlinkSync(path.join(modsDir, file));
                        this._log('info', `Удалён мод: ${file}`);
                    }
                }
            }
        } catch (e) { /* ignore */ }
    }

    _applyDefaultGameSettings() {
        const optionsPath = path.join(this.gameDir, 'options.txt');

        // Исправляем повреждённые значения в существующем options.txt
        if (fs.existsSync(optionsPath)) {
            try {
                let content = fs.readFileSync(optionsPath, 'utf8');
                let fixed = false;
                // Исправляем невалидную яркость (gamma > 1.0 вызывает спам ошибок)
                content = content.replace(/^gamma:(.+)$/m, (match, val) => {
                    const num = parseFloat(val);
                    if (isNaN(num) || num > 1.0 || num < 0.0) {
                        fixed = true;
                        return 'gamma:1.0';
                    }
                    return match;
                });
                if (fixed) {
                    fs.writeFileSync(optionsPath, content);
                    this._log('info', 'Исправлены некорректные значения в options.txt');
                }
            } catch (e) { /* ignore */ }
            return;
        }

        mkdirSync(this.gameDir, { recursive: true });
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
        this._log('info', 'Применены стандартные настройки игры');
    }

    async _launchGame(javaPath, versionData) {
        const fabricVersionId = `fabric-loader-${FABRIC_LOADER_VERSION}-${MINECRAFT_VERSION}`;
        const fabricDir = path.join(this.gameDir, 'versions', fabricVersionId);
        const fabricJsonPath = path.join(fabricDir, `${fabricVersionId}.json`);

        if (!fs.existsSync(fabricJsonPath)) throw new Error('Профиль Fabric не найден');

        const fabricProfile = JSON.parse(fs.readFileSync(fabricJsonPath, 'utf8'));

        // Build classpath
        const classpath = this._buildClasspath(versionData, fabricProfile);
        const mainClass = fabricProfile.mainClass || 'net.fabricmc.loader.impl.launch.knot.KnotClient';
        const username = this.store.get('username') || 'Player';
        const memory = this.store.get('memory') || 4096;
        const uuid = this._offlineUUID(username);
        const accessToken = '0';
        const assetsDir = path.join(this.gameDir, 'assets');
        const assetIndex = versionData.assetIndex?.id || MINECRAFT_VERSION;

        const jvmArgs = [
            `-Xmx${memory}M`,
            `-Xms${Math.min(memory, 512)}M`,
            '-XX:+UseG1GC',
            '-XX:+ParallelRefProcEnabled',
            '-XX:MaxGCPauseMillis=200',
            '-XX:+UnlockExperimentalVMOptions',
            '-XX:+DisableExplicitGC',
            '-XX:G1NewSizePercent=30',
            '-XX:G1MaxNewSizePercent=40',
            '-XX:G1HeapRegionSize=8M',
            '-XX:G1ReservePercent=20',
            '-XX:G1HeapWastePercent=5',
            '-XX:G1MixedGCCountTarget=4',
            '-XX:InitiatingHeapOccupancyPercent=15',
            '-XX:G1MixedGCLiveThresholdPercent=90',
            '-XX:G1RSetUpdatingPauseTimePercent=5',
            `-Djava.library.path=${path.join(this.gameDir, 'natives')}`,
            `-Dminecraft.launcher.brand=ChaosClient`,
            `-Dminecraft.launcher.version=1.0.0`,
        ];

        const gameArgs = [
            '--username', username,
            '--version', fabricVersionId,
            '--gameDir', this.gameDir,
            '--assetsDir', assetsDir,
            '--assetIndex', assetIndex,
            '--uuid', uuid,
            '--accessToken', accessToken,
            '--userType', 'legacy',
            '--versionType', 'release'
        ];

        const args = [...jvmArgs, '-cp', classpath, mainClass, ...gameArgs];

        this._log('info', `Запуск: ${path.basename(javaPath)} ... ${mainClass}`);
        this._log('info', `Память: ${memory} МБ, Пользователь: ${username}`);

        // Extract natives
        await this._extractNatives(versionData);

        return new Promise((resolve, reject) => {
            try {
                const proc = spawn(javaPath, args, {
                    cwd: this.gameDir,
                    env: { ...process.env, _JAVA_OPTIONS: '' },
                    detached: false,
                    stdio: ['ignore', 'pipe', 'pipe']
                });

                this._gameProcess = proc;
                this._launchTime = Date.now();

                // Фильтрация спама и дедупликация
                const spamPatterns = [
                    /^Illegal option value .* for Brightness$/,
                    /^Picked up _JAVA_OPTIONS/,
                ];
                let lastMsg = '';
                let repeatCount = 0;
                const MAX_REPEATS = 2; // показать сообщение макс 2 раза, потом свернуть

                const filterAndLog = (level, line) => {
                    const trimmed = line.trim();
                    if (!trimmed) return;
                    // Фильтруем спам-паттерны
                    if (spamPatterns.some(p => p.test(trimmed))) return;
                    // Дедупликация подряд идущих одинаковых сообщений
                    if (trimmed === lastMsg) {
                        repeatCount++;
                        if (repeatCount === MAX_REPEATS) {
                            this._log(level, `... (повторяется)`);
                        }
                        return;
                    }
                    // Если были повторы, логируем итог
                    if (repeatCount > MAX_REPEATS) {
                        this._log(level, `↑ повторилось ${repeatCount} раз`);
                    }
                    lastMsg = trimmed;
                    repeatCount = 0;
                    this._log(level, trimmed);
                };

                proc.stdout.on('data', (data) => {
                    const lines = data.toString().split('\n');
                    for (const line of lines) filterAndLog('game', line);
                });

                proc.stderr.on('data', (data) => {
                    const lines = data.toString().split('\n');
                    for (const line of lines) filterAndLog('game-err', line);
                });

                proc.on('error', (err) => {
                    this._gameProcess = null;
                    this._launchTime = null;
                    reject(new Error(`Ошибка запуска: ${err.message}`));
                });

                proc.on('exit', (code, signal) => {
                    this._gameProcess = null;
                    this._launchTime = null;
                    this.emit('game-exit', { code, signal });
                });

                // Resolve after a short delay (process started)
                setTimeout(() => resolve(), 2000);
            } catch (err) {
                reject(err);
            }
        });
    }

    _buildClasspath(versionData, fabricProfile) {
        const sep = process.platform === 'win32' ? ';' : ':';
        const libsDir = path.join(this.gameDir, 'libraries');
        const paths = new Set();

        // Vanilla libraries
        for (const lib of (versionData.libraries || [])) {
            if (!this._isLibraryAllowed(lib)) continue;
            const artifact = lib.downloads?.artifact;
            if (artifact) {
                const libPath = path.join(libsDir, artifact.path);
                if (fs.existsSync(libPath)) paths.add(libPath);
            }
        }

        // Fabric libraries
        for (const lib of (fabricProfile.libraries || [])) {
            const nameParts = lib.name.split(':');
            const [group, artifact, version] = nameParts;
            const groupPath = group.replace(/\./g, '/');
            const relPath = `${groupPath}/${artifact}/${version}/${artifact}-${version}.jar`;
            const libPath = path.join(libsDir, relPath);
            if (fs.existsSync(libPath)) paths.add(libPath);
        }

        // Vanilla client jar
        const clientJar = path.join(this.gameDir, 'versions', MINECRAFT_VERSION, `${MINECRAFT_VERSION}.jar`);
        if (fs.existsSync(clientJar)) paths.add(clientJar);

        return Array.from(paths).join(sep);
    }

    async _extractNatives(versionData) {
        const nativesDir = path.join(this.gameDir, 'natives');
        // Очищаем natives при каждом запуске для актуальности
        if (fs.existsSync(nativesDir)) {
            try { fs.rmSync(nativesDir, { recursive: true, force: true }); } catch (e) { /* ignore */ }
        }
        mkdirSync(nativesDir, { recursive: true });
        const libsDir = path.join(this.gameDir, 'libraries');
        let extractedCount = 0;

        for (const lib of (versionData.libraries || [])) {
            if (!this._isLibraryAllowed(lib)) continue;

            // === Новый формат MC 1.21+ ===
            // Нативные библиотеки — отдельные артефакты с именем вида "org.lwjgl:lwjgl:3.3.3:natives-linux"
            const libName = lib.name || '';
            const osNative = { win32: 'natives-windows', darwin: 'natives-macos', linux: 'natives-linux' }[process.platform];
            if (osNative && libName.includes(`:${osNative}`)) {
                const artifact = lib.downloads?.artifact;
                if (artifact) {
                    const nativePath = path.join(libsDir, artifact.path);
                    if (!fs.existsSync(nativePath)) {
                        try {
                            mkdirSync(path.dirname(nativePath), { recursive: true });
                            await this._downloadFile(artifact.url, nativePath);
                        } catch (e) {
                            this._log('warn', `Natives: не удалось загрузить ${artifact.path}`);
                            continue;
                        }
                    }
                    try {
                        if (process.platform === 'win32') {
                            execSync(`powershell -command "Expand-Archive -Path '${nativePath}' -DestinationPath '${nativesDir}' -Force"`, { timeout: 30000 });
                        } else {
                            execSync(`unzip -o -q "${nativePath}" -d "${nativesDir}" 2>/dev/null || true`, { timeout: 30000 });
                        }
                        extractedCount++;
                    } catch (e) { /* ignore extraction errors */ }
                }
                continue;
            }

            // === Старый формат (classifiers) для совместимости ===
            const classifiers = lib.downloads?.classifiers;
            if (!classifiers) continue;

            const nativeKey = this._getNativeKey(lib);
            if (!nativeKey || !classifiers[nativeKey]) continue;

            const native = classifiers[nativeKey];
            const nativePath = path.join(libsDir, native.path);

            if (!fs.existsSync(nativePath)) {
                try {
                    mkdirSync(path.dirname(nativePath), { recursive: true });
                    await this._downloadFile(native.url, nativePath);
                } catch (e) {
                    this._log('warn', `Natives: не удалось загрузить ${native.path}`);
                    continue;
                }
            }

            try {
                if (process.platform === 'win32') {
                    execSync(`powershell -command "Expand-Archive -Path '${nativePath}' -DestinationPath '${nativesDir}' -Force"`, { timeout: 30000 });
                } else {
                    execSync(`unzip -o -q "${nativePath}" -d "${nativesDir}" 2>/dev/null || true`, { timeout: 30000 });
                }
                extractedCount++;
            } catch (e) { /* ignore extraction errors */ }
        }

        // Clean META-INF from natives dir
        const metaInf = path.join(nativesDir, 'META-INF');
        if (fs.existsSync(metaInf)) {
            try { fs.rmSync(metaInf, { recursive: true, force: true }); } catch (e) { /* ignore */ }
        }

        this._log('info', `Natives: извлечено ${extractedCount} библиотек`);
    }

    _getNativeKey(lib) {
        const natives = lib.natives;
        if (!natives) return null;
        const osName = { win32: 'windows', darwin: 'osx', linux: 'linux' }[process.platform];
        let key = natives[osName];
        if (!key) return null;
        return key.replace('${arch}', process.arch === 'x64' ? '64' : '32');
    }

    _offlineUUID(name) {
        const md5 = crypto.createHash('md5').update('OfflinePlayer:' + name).digest('hex');
        return [
            md5.substring(0, 8),
            md5.substring(8, 12),
            '3' + md5.substring(13, 16),
            md5.substring(16, 20),
            md5.substring(20, 32)
        ].join('-');
    }

    _log(level, message) {
        const ts = new Date().toLocaleTimeString('ru-RU', { hour12: false });
        this.emit('log', { time: ts, level, message });
    }

    // ===== Network utilities (keep-alive connection pooling) =====

    _getAgent(url) {
        return url.startsWith('https') ? httpsAgent : httpAgent;
    }

    async _fetchJson(url, retries = 3) {
        for (let attempt = 1; attempt <= retries; attempt++) {
            try {
                return await this._fetchJsonOnce(url);
            } catch (e) {
                if (attempt < retries) {
                    await new Promise(r => setTimeout(r, 500 * attempt));
                    continue;
                }
                throw e;
            }
        }
    }

    _fetchJsonOnce(url) {
        return new Promise((resolve, reject) => {
            let settled = false;
            const settle = (fn, val) => { if (!settled) { settled = true; fn(val); } };
            const doRequest = (reqUrl, redirects = 0) => {
                if (redirects > 5) { settle(reject, new Error('Too many redirects')); return; }
                const mod = reqUrl.startsWith('https') ? https : http;
                const req = mod.get(reqUrl, {
                    agent: this._getAgent(reqUrl),
                    headers: { 'User-Agent': 'ChaosClient-Launcher/1.0', 'Accept': 'application/json', 'Connection': 'keep-alive' },
                    timeout: 15000
                }, (res) => {
                    if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
                        let loc = res.headers.location;
                        if (loc.startsWith('/')) { const u = new URL(reqUrl); loc = u.origin + loc; }
                        res.resume();
                        doRequest(loc, redirects + 1);
                        return;
                    }
                    if (res.statusCode !== 200) {
                        res.resume();
                        settle(reject, new Error(`HTTP ${res.statusCode}`));
                        return;
                    }
                    let data = '';
                    res.on('data', (chunk) => data += chunk);
                    res.on('end', () => {
                        try { settle(resolve, JSON.parse(data)); }
                        catch (e) { settle(reject, new Error(`JSON parse error`)); }
                    });
                    res.on('error', (e) => settle(reject, e));
                });
                req.on('error', (e) => settle(reject, e));
                req.on('timeout', () => { req.destroy(); settle(reject, new Error('Timeout')); });
            };
            doRequest(url);
        });
    }

    async _downloadFile(url, destPath, retries = 3) {
        for (let attempt = 1; attempt <= retries; attempt++) {
            try {
                return await this._downloadFileOnce(url, destPath);
            } catch (e) {
                try { fs.unlinkSync(destPath); } catch (_) { /* ignore */ }
                if (attempt < retries) {
                    await new Promise(r => setTimeout(r, 300 * attempt));
                    continue;
                }
                throw e;
            }
        }
    }

    _downloadFileOnce(url, destPath) {
        return new Promise((resolve, reject) => {
            let settled = false;
            const settle = (fn, val) => { if (!settled) { settled = true; fn(val); } };
            const doRequest = (reqUrl, redirects = 0) => {
                if (redirects > 5) { settle(reject, new Error('Too many redirects')); return; }
                const mod = reqUrl.startsWith('https') ? https : http;
                const req = mod.get(reqUrl, {
                    agent: this._getAgent(reqUrl),
                    headers: { 'User-Agent': 'ChaosClient-Launcher/1.0', 'Connection': 'keep-alive' },
                    timeout: 30000
                }, (res) => {
                    if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
                        let loc = res.headers.location;
                        if (loc.startsWith('/')) { const u = new URL(reqUrl); loc = u.origin + loc; }
                        res.resume();
                        doRequest(loc, redirects + 1);
                        return;
                    }
                    if (res.statusCode !== 200) {
                        res.resume();
                        settle(reject, new Error(`HTTP ${res.statusCode}`));
                        return;
                    }
                    mkdirSync(path.dirname(destPath), { recursive: true });
                    const file = createWriteStream(destPath);
                    res.pipe(file);
                    file.on('finish', () => { file.close(); settle(resolve); });
                    file.on('error', (err) => {
                        file.close();
                        try { fs.unlinkSync(destPath); } catch (e) { /* ignore */ }
                        settle(reject, err);
                    });
                    res.on('error', (e) => { file.close(); settle(reject, e); });
                });
                req.on('error', (err) => settle(reject, err));
                req.on('timeout', () => { req.destroy(); settle(reject, new Error('Timeout')); });
            };
            doRequest(url);
        });
    }

    /**
     * Гонка fallback-URL'ов: запускаем 3 параллельных запроса одновременно,
     * первый успешный побеждает, остальные отменяются.
     */
    async _downloadWithFallbackRace(artifactPath, destPath, fallbackBases) {
        // Пробуем fallback'и группами по 3 параллельно
        for (let i = 0; i < fallbackBases.length; i += 3) {
            const group = fallbackBases.slice(i, i + 3);
            const urls = group.map(base => base + artifactPath);
            try {
                await this._downloadFirstAvailable(urls, destPath);
                return;
            } catch (e) { /* следующая группа */ }
        }
        throw new Error('All fallbacks failed');
    }

    /**
     * Запускает загрузку из нескольких URL параллельно.
     * Первый успешно скачанный файл выигрывает.
     */
    _downloadFirstAvailable(urls, destPath) {
        return new Promise((resolve, reject) => {
            let settled = false;
            let remaining = urls.length;
            const tempPaths = urls.map((_, idx) => destPath + `.tmp${idx}`);

            urls.forEach((url, idx) => {
                const tmpPath = tempPaths[idx];
                this._downloadFileOnce(url, tmpPath).then(() => {
                    if (settled) {
                        try { fs.unlinkSync(tmpPath); } catch (e) { /* ignore */ }
                        return;
                    }
                    settled = true;
                    // Переименовываем победителя
                    try { fs.renameSync(tmpPath, destPath); } catch (e) {
                        try { fs.copyFileSync(tmpPath, destPath); fs.unlinkSync(tmpPath); } catch (e2) { /* ignore */ }
                    }
                    // Чистим остальные tmp
                    tempPaths.forEach((tp, i) => { if (i !== idx) try { fs.unlinkSync(tp); } catch (e) { /* ignore */ } });
                    resolve();
                }).catch(() => {
                    try { fs.unlinkSync(tmpPath); } catch (e) { /* ignore */ }
                    remaining--;
                    if (remaining === 0 && !settled) {
                        settled = true;
                        reject(new Error('All URLs failed'));
                    }
                });
            });
        });
    }

    _formatBytes(bytes) {
        if (!bytes) return '0 Б';
        if (bytes >= 1073741824) return (bytes / 1073741824).toFixed(1) + ' ГБ';
        if (bytes >= 1048576) return (bytes / 1048576).toFixed(0) + ' МБ';
        if (bytes >= 1024) return (bytes / 1024).toFixed(0) + ' КБ';
        return bytes + ' Б';
    }
}

module.exports = { MinecraftLauncher };
