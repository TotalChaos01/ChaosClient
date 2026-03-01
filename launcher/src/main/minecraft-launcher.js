const { EventEmitter } = require('events');
const fs = require('fs');
const path = require('path');
const https = require('https');
const http = require('http');
const { spawn, execSync } = require('child_process');
const { createWriteStream, mkdirSync } = require('fs');
const crypto = require('crypto');

const MINECRAFT_VERSION = '1.21.11';
const FABRIC_LOADER_VERSION = '0.18.4';
const FABRIC_API_VERSION = '0.141.3+1.21.11';
const VERSION_MANIFEST_URL = 'https://piston-meta.mojang.com/mc/game/version_manifest_v2.json';
const FABRIC_META_URL = 'https://meta.fabricmc.net';
const MODRINTH_API = 'https://api.modrinth.com/v2';

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
                const pageSize = 4096; // typical page size
                const rssPages = parseInt(statm[1]) || 0;
                stats.memoryMB = Math.round((rssPages * pageSize) / (1024 * 1024));

                // CPU from /proc/PID/stat
                const stat = fs.readFileSync(`/proc/${pid}/stat`, 'utf8').trim();
                // Find the closing paren of comm field, then parse remaining
                const afterComm = stat.substring(stat.lastIndexOf(')') + 2);
                const fields = afterComm.split(' ');
                const utime = parseInt(fields[11]) || 0;
                const stime = parseInt(fields[12]) || 0;
                const starttime = parseInt(fields[19]) || 0;
                const clkTck = 100; // sysconf(_SC_CLK_TCK) typically 100
                const uptime = parseFloat(fs.readFileSync('/proc/uptime', 'utf8').split(' ')[0]);
                const totalTime = (utime + stime) / clkTck;
                const elapsed = uptime - (starttime / clkTck);
                if (elapsed > 0) stats.cpu = Math.min(Math.round((totalTime / elapsed) * 100), 100);
            } catch (e) { /* ignore proc read errors */ }
        } else if (process.platform === 'win32') {
            try {
                const out = execSync(`wmic process where ProcessId=${pid} get WorkingSetSize /format:value`, { encoding: 'utf8', timeout: 3000 });
                const match = out.match(/WorkingSetSize=(\d+)/);
                if (match) stats.memoryMB = Math.round(parseInt(match[1]) / (1024 * 1024));
            } catch (e) { /* ignore */ }
        } else if (process.platform === 'darwin') {
            try {
                const out = execSync(`ps -o rss= -p ${pid}`, { encoding: 'utf8', timeout: 3000 });
                const kb = parseInt(out.trim());
                if (kb) stats.memoryMB = Math.round(kb / 1024);
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

        // Step 3: Download libraries
        this.emit('status', 'Загрузка библиотек...');
        await this._downloadLibraries(versionData);
        this.emit('progress', { percent: 35, stage: 'libraries' });

        // Step 4: Download assets
        this.emit('status', 'Загрузка ресурсов...');
        await this._downloadAssets(versionData);
        this.emit('progress', { percent: 55, stage: 'assets' });

        // Step 5: Download client jar
        this.emit('status', 'Загрузка клиента...');
        await this._downloadClientJar(versionData);
        this.emit('progress', { percent: 60, stage: 'client' });

        // Step 6: Install Fabric
        this.emit('status', 'Установка Fabric...');
        await this._installFabric();
        this.emit('progress', { percent: 70, stage: 'fabric' });

        // Step 7: Install mods
        this.emit('status', 'Установка модов...');
        await this._installAllMods();
        this.emit('progress', { percent: 85, stage: 'mods' });

        // Step 8: Apply default game settings
        this._applyDefaultGameSettings();

        // Step 9: Launch
        this.emit('status', 'Запуск ChaosClient...');
        this.emit('progress', { percent: 90, stage: 'launching' });
        await this._launchGame(javaPath, versionData);
        this.emit('progress', { percent: 100, stage: 'done' });
        this.emit('status', 'ChaosClient запущен!');
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
        const libraries = versionData.libraries || [];
        let downloaded = 0;

        for (const lib of libraries) {
            if (!this._isLibraryAllowed(lib)) continue;

            const artifact = lib.downloads?.artifact;
            if (!artifact) continue;

            const libPath = path.join(libsDir, artifact.path);
            if (fs.existsSync(libPath)) { downloaded++; continue; }

            try {
                mkdirSync(path.dirname(libPath), { recursive: true });
                await this._downloadFile(artifact.url, libPath);
                downloaded++;
            } catch (e) {
                this._log('warn', `Не удалось скачать библиотеку: ${lib.name}: ${e.message}`);
            }

            const progress = 10 + Math.round((downloaded / libraries.length) * 25);
            this.emit('progress', { percent: Math.min(progress, 35), stage: 'libraries' });
        }

        this._log('info', `Загружено библиотек: ${downloaded}`);
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
        const batchSize = 30;

        for (let i = 0; i < entries.length; i += batchSize) {
            const batch = entries.slice(i, i + batchSize);
            const promises = batch.map(async ([name, obj]) => {
                const hash = obj.hash;
                const prefix = hash.substring(0, 2);
                const objectPath = path.join(this.gameDir, 'assets', 'objects', prefix, hash);

                if (fs.existsSync(objectPath)) { skipped++; return; }

                try {
                    mkdirSync(path.dirname(objectPath), { recursive: true });
                    await this._downloadFile(`https://resources.download.minecraft.net/${prefix}/${hash}`, objectPath);
                    downloaded++;
                } catch (e) {
                    // Non-critical - try again later
                }
            });

            await Promise.all(promises);
            const progress = 35 + Math.round(((i + batch.length) / entries.length) * 20);
            this.emit('progress', { percent: Math.min(progress, 55), stage: 'assets' });
        }

        this._log('info', `Ресурсы: загружено ${downloaded}, кешировано ${skipped}`);
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

        // Download Fabric libraries
        const libsDir = path.join(this.gameDir, 'libraries');
        for (const lib of (profile.libraries || [])) {
            const nameParts = lib.name.split(':');
            const [group, artifact, version] = nameParts;
            const groupPath = group.replace(/\./g, '/');
            const relPath = `${groupPath}/${artifact}/${version}/${artifact}-${version}.jar`;
            const libPath = path.join(libsDir, relPath);

            if (fs.existsSync(libPath)) continue;

            const url = (lib.url || 'https://maven.fabricmc.net/') + relPath;
            try {
                mkdirSync(path.dirname(libPath), { recursive: true });
                await this._downloadFile(url, libPath);
            } catch (e) {
                this._log('warn', `Не удалось скачать Fabric библиотеку: ${lib.name}`);
            }
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
        const modFileName = 'ChaosClient-1.0.0.jar';
        const destPath = path.join(modsDir, modFileName);
        if (fs.existsSync(destPath)) return;

        // Check bundled resource first
        const bundledPaths = [
            path.join(process.resourcesPath || '', modFileName),
            path.join(__dirname, '..', '..', '..', 'build', 'libs', modFileName),
            path.join(__dirname, '..', '..', 'build', 'libs', modFileName),
        ];

        for (const srcPath of bundledPaths) {
            if (fs.existsSync(srcPath)) {
                fs.copyFileSync(srcPath, destPath);
                this._log('info', 'ChaosClient мод установлен (из ресурсов)');
                return;
            }
        }

        // Try downloading from GitHub releases
        try {
            const releasesUrl = 'https://api.github.com/repos/totalchaos01/ChaosClient-releases/releases/latest';
            const release = await this._fetchJson(releasesUrl);
            if (release?.assets) {
                const modAsset = release.assets.find(a => a.name.endsWith('.jar') && a.name.includes('ChaosClient'));
                if (modAsset) {
                    await this._downloadFile(modAsset.browser_download_url, destPath);
                    this._log('info', 'ChaosClient мод скачан с GitHub');
                    return;
                }
            }
        } catch (e) {
            this._log('warn', `Не удалось скачать мод с GitHub: ${e.message}`);
        }

        this._log('warn', 'ChaosClient мод не найден — будет скопирован при сборке');
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
                if (!file.endsWith('.jar')) continue;
                const lower = file.toLowerCase();
                const shouldKeep = keepPatterns.some(p => lower.includes(p));
                if (!shouldKeep) {
                    // Check if this is a known mod that is now deselected
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
        if (fs.existsSync(optionsPath)) return; // Don't overwrite

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

                proc.stdout.on('data', (data) => {
                    const lines = data.toString().split('\n');
                    for (const line of lines) {
                        const trimmed = line.trim();
                        if (trimmed) this._log('game', trimmed);
                    }
                });

                proc.stderr.on('data', (data) => {
                    const lines = data.toString().split('\n');
                    for (const line of lines) {
                        const trimmed = line.trim();
                        if (trimmed) this._log('game-err', trimmed);
                    }
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
        if (fs.existsSync(nativesDir) && fs.readdirSync(nativesDir).length > 0) return;

        mkdirSync(nativesDir, { recursive: true });
        const libsDir = path.join(this.gameDir, 'libraries');

        for (const lib of (versionData.libraries || [])) {
            if (!this._isLibraryAllowed(lib)) continue;

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

            // Extract .so / .dll / .dylib files
            try {
                if (process.platform === 'win32') {
                    execSync(`powershell -command "Expand-Archive -Path '${nativePath}' -DestinationPath '${nativesDir}' -Force"`, { timeout: 30000 });
                } else {
                    execSync(`unzip -o -q "${nativePath}" -d "${nativesDir}" 2>/dev/null || true`, { timeout: 30000 });
                }
            } catch (e) { /* ignore extraction errors */ }
        }

        // Clean META-INF from natives dir
        const metaInf = path.join(nativesDir, 'META-INF');
        if (fs.existsSync(metaInf)) {
            try { fs.rmSync(metaInf, { recursive: true, force: true }); } catch (e) { /* ignore */ }
        }
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

    // ===== Network utilities =====

    _fetchJson(url) {
        return new Promise((resolve, reject) => {
            const doRequest = (url, redirects = 0) => {
                if (redirects > 5) { reject(new Error('Слишком много перенаправлений')); return; }
                const mod = url.startsWith('https') ? https : http;
                mod.get(url, {
                    headers: { 'User-Agent': 'ChaosClient-Launcher/1.0', 'Accept': 'application/json' },
                    timeout: 15000
                }, (res) => {
                    if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
                        doRequest(res.headers.location, redirects + 1);
                        return;
                    }
                    if (res.statusCode !== 200) {
                        reject(new Error(`HTTP ${res.statusCode} для ${url}`));
                        return;
                    }
                    let data = '';
                    res.on('data', (chunk) => data += chunk);
                    res.on('end', () => {
                        try { resolve(JSON.parse(data)); }
                        catch (e) { reject(new Error(`JSON parse error: ${e.message}`)); }
                    });
                }).on('error', reject).on('timeout', () => reject(new Error('Таймаут загрузки')));
            };
            doRequest(url);
        });
    }

    _downloadFile(url, destPath, expectedSize) {
        return new Promise((resolve, reject) => {
            const doRequest = (url, redirects = 0) => {
                if (redirects > 5) { reject(new Error('Слишком много перенаправлений')); return; }
                const mod = url.startsWith('https') ? https : http;
                mod.get(url, {
                    headers: { 'User-Agent': 'ChaosClient-Launcher/1.0' },
                    timeout: 30000
                }, (res) => {
                    if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
                        doRequest(res.headers.location, redirects + 1);
                        return;
                    }
                    if (res.statusCode !== 200) {
                        reject(new Error(`HTTP ${res.statusCode}`));
                        return;
                    }
                    mkdirSync(path.dirname(destPath), { recursive: true });
                    const file = createWriteStream(destPath);
                    res.pipe(file);
                    file.on('finish', () => { file.close(); resolve(); });
                    file.on('error', (err) => {
                        file.close();
                        try { fs.unlinkSync(destPath); } catch (e) { /* ignore */ }
                        reject(err);
                    });
                }).on('error', (err) => {
                    try { fs.unlinkSync(destPath); } catch (e) { /* ignore */ }
                    reject(err);
                }).on('timeout', () => reject(new Error('Таймаут загрузки')));
            };
            doRequest(url);
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
