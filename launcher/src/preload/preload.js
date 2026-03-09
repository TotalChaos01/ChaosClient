const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('launcher', {
    // Управление окном
    minimize: () => ipcRenderer.send('window:minimize'),
    maximize: () => ipcRenderer.send('window:maximize'),
    close: () => ipcRenderer.send('window:close'),

    // Настройки
    getSetting: (key) => ipcRenderer.invoke('settings:get', key),
    getAllSettings: () => ipcRenderer.invoke('settings:getAll'),
    setSetting: (key, value) => ipcRenderer.send('settings:set', key, value),

    // Minecraft
    launch: () => ipcRenderer.invoke('minecraft:launch'),
    getState: () => ipcRenderer.invoke('minecraft:getState'),
    killGame: () => ipcRenderer.invoke('minecraft:kill'),
    getLogFile: () => ipcRenderer.invoke('minecraft:getLogFile'),

    // Java
    getJavaStatus: () => ipcRenderer.invoke('java:status'),
    installBundledJava: () => ipcRenderer.invoke('java:installBundled'),
    verifyJava: (path) => ipcRenderer.invoke('java:verify', path),
    detectJava: () => ipcRenderer.invoke('java:detect'),

    // Диалоги / файлы
    openFileDialog: () => ipcRenderer.invoke('dialog:openFile'),
    openDirectoryDialog: () => ipcRenderer.invoke('dialog:openDirectory'),
    openPath: (p) => ipcRenderer.send('open:path', p),

    // Информация о приложении
    getVersion: () => ipcRenderer.invoke('app:version'),
    getPlatform: () => ipcRenderer.invoke('app:platform'),
    getGameDir: () => ipcRenderer.invoke('app:gameDir'),

    // Обновления и билды
    checkUpdate: () => ipcRenderer.invoke('app:checkUpdate'),
    getNews: () => ipcRenderer.invoke('app:getNews'),
    getReleases: () => ipcRenderer.invoke('app:getReleases'),
    getCommits: () => ipcRenderer.invoke('app:getCommits'),
    selectDevBuild: (build) => ipcRenderer.invoke('app:selectDevBuild', build),
    applyModUpdate: (assetInfo) => ipcRenderer.invoke('app:applyModUpdate', assetInfo),
    installReleaseMod: (assetInfo) => ipcRenderer.invoke('app:installReleaseMod', assetInfo),

    // Моды — управление стандартными модами
    getInstalledMods: () => ipcRenderer.invoke('mods:getInstalled'),
    toggleMod: (modId, enabled) => ipcRenderer.invoke('mods:toggle', modId, enabled),

    // Переустановка / очистка
    reinstallClient: () => ipcRenderer.invoke('minecraft:reinstall'),
    clearMods: () => ipcRenderer.invoke('minecraft:clearMods'),

    // Мониторинг процесса
    getProcessStats: () => ipcRenderer.invoke('minecraft:processStats'),

    // События
    onProgress: (cb) => ipcRenderer.on('launch:progress', (_, d) => cb(d)),
    onStatus: (cb) => ipcRenderer.on('launch:status', (_, d) => cb(d)),
    onLog: (cb) => ipcRenderer.on('launch:log', (_, e) => cb(e)),
    onGameExit: (cb) => ipcRenderer.on('launch:game-exit', (_, d) => cb(d)),
    onJavaProgress: (cb) => ipcRenderer.on('java:progress', (_, d) => cb(d)),
    onJavaStatus: (cb) => ipcRenderer.on('java:status-msg', (_, m) => cb(m)),

    // Самообновление лаунчера
    selfUpdate: (assetInfo) => ipcRenderer.invoke('app:selfUpdate', assetInfo),

    // Внешние ссылки
    openExternal: (url) => ipcRenderer.send('open:external', url)
});
