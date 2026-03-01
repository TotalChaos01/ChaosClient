const { EventEmitter } = require('events');
const fs = require('fs');
const path = require('path');
const https = require('https');
const { execFile, execSync } = require('child_process');
const { createWriteStream } = require('fs');

const ADOPTIUM_API = 'https://api.adoptium.net/v3';
const JAVA_VERSION = 21;

class JavaManager extends EventEmitter {
    constructor(gameDir) {
        super();
        this.gameDir = gameDir;
        this.javaDir = path.join(gameDir, 'runtime', 'java');
    }

    isBundledInstalled() {
        const javaPath = this.getBundledJavaPath();
        return fs.existsSync(javaPath);
    }

    getBundledJavaPath() {
        const bin = process.platform === 'win32' ? 'java.exe' : 'java';
        if (fs.existsSync(this.javaDir)) {
            const entries = fs.readdirSync(this.javaDir);
            for (const entry of entries) {
                const candidate = path.join(this.javaDir, entry, 'bin', bin);
                if (fs.existsSync(candidate)) return candidate;
            }
        }
        return path.join(this.javaDir, 'bin', bin);
    }

    getBundledVersion() {
        if (!this.isBundledInstalled()) return null;
        try {
            const { execFileSync } = require('child_process');
            execFileSync(this.getBundledJavaPath(), ['-version'], { encoding: 'utf8', timeout: 10000, stdio: ['pipe', 'pipe', 'pipe'] });
            return 'Java 21 (Adoptium)';
        } catch (e) {
            if (e.stderr) {
                const match = e.stderr.match(/version "([^"]+)"/);
                return match ? `Java ${match[1]}` : 'Java 21 (Adoptium)';
            }
            return 'Java 21 (Adoptium)';
        }
    }

    async verifyJava(javaPath) {
        return new Promise((resolve, reject) => {
            execFile(javaPath, ['-version'], { timeout: 10000 }, (err, stdout, stderr) => {
                const output = stderr || stdout || '';
                const match = output.match(/version "([^"]+)"/);
                if (match) {
                    const major = parseInt(match[1].split('.')[0]);
                    if (major >= 21) resolve(match[1]);
                    else reject(new Error(`Java ${match[1]} найдена, но требуется Java 21+`));
                } else if (err) {
                    reject(new Error(`Не удалось запустить Java: ${err.message}`));
                } else {
                    reject(new Error('Не удалось определить версию Java'));
                }
            });
        });
    }

    /**
     * Автоматический поиск всех установленных Java на компьютере.
     * Возвращает массив {path, version}
     */
    detectAllJava() {
        const found = [];
        const checked = new Set();

        const addCandidate = (javaPath) => {
            if (!javaPath || checked.has(javaPath)) return;
            checked.add(javaPath);
            if (!fs.existsSync(javaPath)) return;
            try {
                const output = execSync(`"${javaPath}" -version 2>&1`, { encoding: 'utf8', timeout: 5000 });
                const match = output.match(/version "([^"]+)"/);
                if (match) {
                    const major = parseInt(match[1].split('.')[0]);
                    found.push({ path: javaPath, version: `Java ${match[1]}`, major });
                }
            } catch (e) {
                if (e.stderr || e.stdout) {
                    const output = (e.stderr || '') + (e.stdout || '');
                    const match = output.match(/version "([^"]+)"/);
                    if (match) {
                        const major = parseInt(match[1].split('.')[0]);
                        found.push({ path: javaPath, version: `Java ${match[1]}`, major });
                    }
                }
            }
        };

        const bin = process.platform === 'win32' ? 'java.exe' : 'java';

        if (process.platform === 'linux' || process.platform === 'darwin') {
            // which/whereis
            try {
                const whichResult = execSync('which java 2>/dev/null || true', { encoding: 'utf8', timeout: 3000 }).trim();
                if (whichResult) {
                    // Resolve symlinks
                    try {
                        const resolved = execSync(`readlink -f "${whichResult}" 2>/dev/null || echo "${whichResult}"`, { encoding: 'utf8', timeout: 3000 }).trim();
                        addCandidate(resolved);
                    } catch (e) { addCandidate(whichResult); }
                }
            } catch (e) { /* ignore */ }

            // Common Linux JVM directories
            const jvmDirs = [
                '/usr/lib/jvm',
                '/usr/java',
                '/usr/local/java',
                '/opt/java',
                '/opt/jdk',
                path.join(process.env.HOME || '', '.sdkman', 'candidates', 'java'),
                path.join(process.env.HOME || '', '.jdks'),
            ];

            for (const dir of jvmDirs) {
                try {
                    if (!fs.existsSync(dir)) continue;
                    const entries = fs.readdirSync(dir);
                    for (const entry of entries) {
                        const candidate = path.join(dir, entry, 'bin', bin);
                        addCandidate(candidate);
                    }
                } catch (e) { /* ignore */ }
            }

            // update-alternatives
            try {
                const altResult = execSync('update-alternatives --list java 2>/dev/null || true', { encoding: 'utf8', timeout: 3000 });
                for (const line of altResult.split('\n')) {
                    const trimmed = line.trim();
                    if (trimmed && trimmed.startsWith('/')) addCandidate(trimmed);
                }
            } catch (e) { /* ignore */ }
        }

        if (process.platform === 'win32') {
            // Windows: check common locations
            const programFiles = [process.env['ProgramFiles'], process.env['ProgramFiles(x86)'], process.env['LOCALAPPDATA']];
            const winDirs = [];
            for (const pf of programFiles) {
                if (!pf) continue;
                winDirs.push(path.join(pf, 'Java'));
                winDirs.push(path.join(pf, 'Eclipse Adoptium'));
                winDirs.push(path.join(pf, 'AdoptOpenJDK'));
                winDirs.push(path.join(pf, 'Microsoft', 'jdk'));
                winDirs.push(path.join(pf, 'Zulu'));
            }
            for (const dir of winDirs) {
                try {
                    if (!fs.existsSync(dir)) continue;
                    const entries = fs.readdirSync(dir);
                    for (const entry of entries) {
                        addCandidate(path.join(dir, entry, 'bin', bin));
                    }
                } catch (e) { /* ignore */ }
            }
            // JAVA_HOME
            if (process.env.JAVA_HOME) {
                addCandidate(path.join(process.env.JAVA_HOME, 'bin', bin));
            }
        }

        // Also check JAVA_HOME on any platform
        if (process.env.JAVA_HOME) {
            addCandidate(path.join(process.env.JAVA_HOME, 'bin', bin));
        }

        // Add bundled java
        if (this.isBundledInstalled()) {
            addCandidate(this.getBundledJavaPath());
        }

        // Sort: Java 21+ first, then by version desc
        found.sort((a, b) => {
            if (a.major >= 21 && b.major < 21) return -1;
            if (a.major < 21 && b.major >= 21) return 1;
            return b.major - a.major;
        });

        return found;
    }

    _getPlatformInfo() {
        const archMap = { 'x64': 'x64', 'arm64': 'aarch64', 'arm': 'arm' };
        const osMap = { 'linux': 'linux', 'darwin': 'mac', 'win32': 'windows' };
        return { os: osMap[process.platform] || 'linux', arch: archMap[process.arch] || 'x64' };
    }

    async downloadAndInstall() {
        const { os, arch } = this._getPlatformInfo();
        this.emit('status', 'Получение информации о Java 21...');
        this.emit('progress', { percent: 0, stage: 'java' });

        const apiUrl = `${ADOPTIUM_API}/assets/latest/${JAVA_VERSION}/hotspot?architecture=${arch}&image_type=jdk&os=${os}&vendor=eclipse`;
        const assets = await this._fetchJson(apiUrl);
        if (!assets || assets.length === 0) throw new Error('Не найдена подходящая версия Java для вашей системы');

        const asset = assets[0];
        const downloadUrl = asset.binary.package.link;
        const fileName = asset.binary.package.name;
        const expectedSize = asset.binary.package.size;

        fs.mkdirSync(this.javaDir, { recursive: true });
        const archivePath = path.join(this.javaDir, fileName);
        this.emit('status', `Загрузка Java 21 (${this._formatBytes(expectedSize)})...`);

        await this._downloadFile(downloadUrl, archivePath, expectedSize);
        this.emit('status', 'Распаковка Java 21...');
        this.emit('progress', { percent: 90, stage: 'java' });

        await this._extract(archivePath, this.javaDir);
        try { fs.unlinkSync(archivePath); } catch (e) { /* ignore */ }

        if (process.platform !== 'win32') {
            const javaPath = this.getBundledJavaPath();
            try { fs.chmodSync(javaPath, 0o755); } catch (e) { /* ignore */ }
        }

        this.emit('status', 'Java 21 установлена!');
        this.emit('progress', { percent: 100, stage: 'java' });
    }

    _fetchJson(url) {
        return new Promise((resolve, reject) => {
            const request = (url) => {
                https.get(url, { headers: { 'User-Agent': 'ChaosClient-Launcher/1.0' } }, (res) => {
                    if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) { request(res.headers.location); return; }
                    let data = '';
                    res.on('data', (chunk) => data += chunk);
                    res.on('end', () => { try { resolve(JSON.parse(data)); } catch (e) { reject(new Error('Ошибка парсинга JSON')); } });
                }).on('error', reject);
            };
            request(url);
        });
    }

    _downloadFile(url, destPath, expectedSize) {
        return new Promise((resolve, reject) => {
            const file = createWriteStream(destPath);
            let downloaded = 0;
            const request = (url) => {
                https.get(url, { headers: { 'User-Agent': 'ChaosClient-Launcher/1.0' } }, (res) => {
                    if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) { request(res.headers.location); return; }
                    if (res.statusCode !== 200) { file.close(); reject(new Error(`HTTP ${res.statusCode}`)); return; }
                    const totalSize = parseInt(res.headers['content-length']) || expectedSize;
                    res.on('data', (chunk) => {
                        downloaded += chunk.length;
                        if (totalSize > 0) this.emit('progress', { percent: Math.min(Math.round((downloaded / totalSize) * 85), 85), stage: 'java' });
                    });
                    res.pipe(file);
                    file.on('finish', () => { file.close(); resolve(); });
                }).on('error', (err) => { file.close(); try { fs.unlinkSync(destPath); } catch (e) { /* ignore */ } reject(err); });
            };
            request(url);
        });
    }

    async _extract(archivePath, destDir) {
        const ext = path.extname(archivePath).toLowerCase();
        const { execSync } = require('child_process');
        if (ext === '.zip') {
            if (process.platform === 'win32') {
                execSync(`powershell -command "Expand-Archive -Path '${archivePath}' -DestinationPath '${destDir}' -Force"`, { timeout: 300000 });
            } else {
                execSync(`unzip -o "${archivePath}" -d "${destDir}"`, { timeout: 300000 });
            }
        } else {
            execSync(`tar -xzf "${archivePath}" -C "${destDir}"`, { timeout: 300000 });
        }
    }

    _formatBytes(bytes) {
        if (bytes >= 1073741824) return (bytes / 1073741824).toFixed(1) + ' ГБ';
        if (bytes >= 1048576) return (bytes / 1048576).toFixed(0) + ' МБ';
        if (bytes >= 1024) return (bytes / 1024).toFixed(0) + ' КБ';
        return bytes + ' Б';
    }
}

module.exports = { JavaManager };
