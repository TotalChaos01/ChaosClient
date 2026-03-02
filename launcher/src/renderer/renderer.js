// ============================================
// ChaosClient Лаунчер — Скрипт рендерера
// Полностью на русском
// ============================================

document.addEventListener('DOMContentLoaded', async () => {
    // ===== Управление окном =====
    document.getElementById('btn-minimize').addEventListener('click', () => launcher.minimize());
    document.getElementById('btn-maximize').addEventListener('click', () => launcher.maximize());
    document.getElementById('btn-close').addEventListener('click', () => launcher.close());

    // ===== Навигация =====
    const navButtons = document.querySelectorAll('.nav-btn');
    const pages = document.querySelectorAll('.page');

    navButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const targetPage = btn.dataset.page;
            navButtons.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            pages.forEach(p => p.classList.remove('active'));
            document.getElementById(`page-${targetPage}`).classList.add('active');
            if (targetPage === 'settings') refreshJavaStatus();
        });
    });

    // ===== Загрузка настроек =====
    const settings = await launcher.getAllSettings();

    // ===== Мастер первого запуска =====
    if (!settings.wizardDone) {
        document.getElementById('wizard-overlay').style.display = 'flex';
    }

    document.getElementById('wizard-confirm').addEventListener('click', async () => {
        const checkboxes = document.querySelectorAll('#wizard-mods input[type="checkbox"]');
        const selectedMods = [];
        checkboxes.forEach(cb => { if (cb.checked) selectedMods.push(cb.value); });
        launcher.setSetting('selectedMods', selectedMods);
        launcher.setSetting('wizardDone', true);
        document.getElementById('wizard-overlay').style.display = 'none';
    });

    // ===== Никнейм =====
    const usernameInput = document.getElementById('username-input');
    usernameInput.value = settings.username || '';
    usernameInput.addEventListener('input', (e) => {
        const username = e.target.value.trim();
        launcher.setSetting('username', username);
        updateAvatar(username);
    });

    // ===== Память =====
    const memorySlider = document.getElementById('memory-slider');
    const memoryValue = document.getElementById('memory-value');
    memorySlider.value = settings.memory || 4096;
    memoryValue.textContent = formatMemory(memorySlider.value);
    memorySlider.addEventListener('input', (e) => {
        memoryValue.textContent = formatMemory(e.target.value);
        launcher.setSetting('memory', parseInt(e.target.value));
    });

    // ===== Папка игры =====
    const gameDir = document.getElementById('game-dir');
    gameDir.value = settings.gameDir || await launcher.getGameDir();

    document.getElementById('browse-gamedir-btn')?.addEventListener('click', async () => {
        const dir = await launcher.openDirectoryDialog();
        if (dir) { gameDir.value = dir; launcher.setSetting('gameDir', dir); }
    });

    document.getElementById('open-gamedir-btn')?.addEventListener('click', () => {
        launcher.openPath(gameDir.value);
    });

    // ===== Java =====
    const javaModeRadios = document.querySelectorAll('input[name="javaMode"]');
    const bundledSection = document.getElementById('bundled-java-section');
    const customSection = document.getElementById('custom-java-section');
    const customJavaPath = document.getElementById('custom-java-path');
    const installJavaBtn = document.getElementById('install-java-btn');
    const javaProgressBar = document.getElementById('java-progress-bar');
    const javaProgressFill = document.getElementById('java-progress-fill');
    const javaStatusEl = document.getElementById('java-status');
    const javaStatusIcon = document.getElementById('java-status-icon');
    const javaStatusText = document.getElementById('java-status-text');
    const customJavaHint = document.getElementById('custom-java-hint');

    const currentJavaMode = settings.javaMode || 'bundled';
    document.querySelector(`input[name="javaMode"][value="${currentJavaMode}"]`).checked = true;
    updateJavaSectionVisibility(currentJavaMode);
    if (settings.customJavaPath) customJavaPath.value = settings.customJavaPath;

    javaModeRadios.forEach(radio => {
        radio.addEventListener('change', (e) => {
            const mode = e.target.value;
            launcher.setSetting('javaMode', mode);
            updateJavaSectionVisibility(mode);
            if (mode === 'bundled') refreshJavaStatus();
        });
    });

    function updateJavaSectionVisibility(mode) {
        bundledSection.style.display = mode === 'bundled' ? 'block' : 'none';
        customSection.style.display = mode === 'custom' ? 'block' : 'none';
    }

    document.getElementById('browse-java-btn').addEventListener('click', async () => {
        const filePath = await launcher.openFileDialog();
        if (filePath) { customJavaPath.value = filePath; launcher.setSetting('customJavaPath', filePath); }
    });

    document.getElementById('verify-java-btn').addEventListener('click', async () => {
        const jpath = customJavaPath.value.trim();
        if (!jpath) { customJavaHint.textContent = '❌ Укажите путь к Java'; customJavaHint.style.color = '#ef4444'; return; }
        customJavaHint.textContent = '⏳ Проверка...'; customJavaHint.style.color = '#f59e0b';
        const result = await launcher.verifyJava(jpath);
        if (result.success) {
            customJavaHint.textContent = `✅ Java ${result.version} — OK`; customJavaHint.style.color = '#22c55e';
            launcher.setSetting('customJavaPath', jpath);
        } else {
            customJavaHint.textContent = `❌ ${result.error}`; customJavaHint.style.color = '#ef4444';
        }
    });

    // Автоопределение Java
    document.getElementById('detect-java-btn').addEventListener('click', async () => {
        const btn = document.getElementById('detect-java-btn');
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Поиск...'; btn.disabled = true;
        const list = document.getElementById('detected-java-list');
        list.innerHTML = '';
        try {
            const javas = await launcher.detectJava();
            if (javas && javas.length > 0) {
                for (const j of javas) {
                    const el = document.createElement('div');
                    el.className = 'detected-java-item';
                    el.innerHTML = `<span class="java-path">${escapeHtml(j.path)}</span><span class="java-ver">${escapeHtml(j.version)}</span>`;
                    el.addEventListener('click', () => {
                        customJavaPath.value = j.path;
                        launcher.setSetting('customJavaPath', j.path);
                        customJavaHint.textContent = `✅ Выбрана Java ${j.version}`; customJavaHint.style.color = '#22c55e';
                    });
                    list.appendChild(el);
                }
            } else {
                list.innerHTML = '<p class="setting-hint" style="color:#f59e0b;">Java не найдена на компьютере</p>';
            }
        } catch (e) {
            list.innerHTML = `<p class="setting-hint" style="color:#ef4444;">Ошибка поиска: ${escapeHtml(e.message)}</p>`;
        }
        btn.innerHTML = '<i class="fas fa-search"></i> Найти Java на ПК'; btn.disabled = false;
    });

    customJavaPath.addEventListener('change', (e) => launcher.setSetting('customJavaPath', e.target.value));

    installJavaBtn.addEventListener('click', async () => {
        installJavaBtn.disabled = true;
        installJavaBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Загрузка...';
        javaProgressBar.style.display = 'block'; javaProgressFill.style.width = '0%';
        const result = await launcher.installBundledJava();
        if (result.success) {
            setJavaStatus('installed', '✅', 'Java 21 установлена!');
            installJavaBtn.style.display = 'none';
        } else {
            setJavaStatus('error', '❌', `Ошибка: ${result.error}`);
            installJavaBtn.disabled = false;
            installJavaBtn.innerHTML = '<i class="fas fa-download"></i> Повторить';
        }
        javaProgressBar.style.display = 'none';
    });

    launcher.onJavaProgress((data) => { javaProgressFill.style.width = `${data.percent}%`; });
    launcher.onJavaStatus((msg) => { javaStatusText.textContent = msg; });

    function setJavaStatus(cls, icon, text) {
        javaStatusEl.className = 'java-status ' + cls;
        javaStatusIcon.textContent = icon;
        javaStatusText.textContent = text;
    }

    async function refreshJavaStatus() {
        try {
            const status = await launcher.getJavaStatus();
            if (status.bundledInstalled) {
                setJavaStatus('installed', '✅', status.bundledVersion || 'Java 21 установлена');
                installJavaBtn.style.display = 'none';
            } else {
                setJavaStatus('missing', '⚠️', 'Java 21 не установлена — нажмите "Скачать"');
                installJavaBtn.style.display = 'inline-flex'; installJavaBtn.disabled = false;
                installJavaBtn.innerHTML = '<i class="fas fa-download"></i> Скачать Java 21';
            }
        } catch (e) { setJavaStatus('error', '❌', 'Ошибка проверки Java'); }
    }
    refreshJavaStatus();

    // ===== Обновления =====
    const updateChannel = document.getElementById('update-channel');
    updateChannel.value = settings.updateChannel || 'release';
    updateChannel.addEventListener('change', (e) => launcher.setSetting('updateChannel', e.target.value));

    document.getElementById('check-update-btn')?.addEventListener('click', async () => {
        const btn = document.getElementById('check-update-btn');
        const status = document.getElementById('update-status');
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Проверка...'; btn.disabled = true;
        try {
            const result = await launcher.checkUpdate();
            if (result.hasUpdate) {
                status.innerHTML = `<span style="color:#22c55e">✅ Доступно обновление: ${escapeHtml(result.latestVersion)}</span>`;
                if (result.modAsset) {
                    status.innerHTML += `<br><small style="color:var(--text-muted)">Мод: ${escapeHtml(result.modAsset.name)}</small>`;
                }
            } else if (result.error) {
                status.textContent = `Ошибка: ${result.error}`; status.style.color = '#ef4444';
            } else {
                status.textContent = 'У вас последняя версия!';
                status.style.color = '#22c55e';
            }
        } catch (e) {
            status.textContent = `Ошибка: ${e.message}`; status.style.color = '#ef4444';
        }
        btn.innerHTML = '<i class="fas fa-sync"></i> Проверить обновления'; btn.disabled = false;
    });

    // ===== Переустановка =====
    document.getElementById('reinstall-btn')?.addEventListener('click', async () => {
        if (!confirm('Вы уверены? Все файлы игры будут удалены и переустановлены.\nНастройки лаунчера сохранятся.')) return;
        const btn = document.getElementById('reinstall-btn');
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Удаление...'; btn.disabled = true;
        try {
            await launcher.reinstallClient();
            // Сбрасываем wizard чтобы спросить про моды заново
            launcher.setSetting('wizardDone', false);
            alert('Клиент удалён. При следующем запуске всё скачается заново.');
            location.reload();
        } catch (e) {
            alert(`Ошибка: ${e.message}`);
        }
        btn.innerHTML = '<i class="fas fa-redo"></i> Переустановить клиент'; btn.disabled = false;
    });

    // ===== Очистка модов =====
    document.getElementById('clear-mods-btn')?.addEventListener('click', async () => {
        if (!confirm('Удалить все моды? Они скачаются заново при следующем запуске.')) return;
        try {
            await launcher.clearMods();
            launcher.setSetting('wizardDone', false);
            alert('Моды удалены. При запуске выберите моды заново.');
            location.reload();
        } catch (e) { alert(`Ошибка: ${e.message}`); }
    });

    // ===== Аватар =====
    updateAvatar(settings.username || '');

    // ===== Версия приложения =====
    try {
        const version = await launcher.getVersion();
        document.getElementById('app-version').textContent = `v${version}`;
        document.getElementById('about-version').textContent = `Версия ${version}`;
    } catch (e) { /* ignore */ }

    // ===== Кнопка запуска =====
    const launchBtn = document.getElementById('btn-launch');
    const progressContainer = document.getElementById('progress-container');
    const progressFill = document.getElementById('progress-fill');
    const progressText = document.getElementById('progress-text');
    let isLaunching = false;

    launchBtn.addEventListener('click', async () => {
        if (isLaunching) return;
        const username = usernameInput.value.trim();
        if (!username) {
            usernameInput.focus();
            usernameInput.style.borderColor = '#ef4444';
            usernameInput.style.boxShadow = '0 0 0 3px rgba(239,68,68,0.3)';
            setTimeout(() => { usernameInput.style.borderColor = ''; usernameInput.style.boxShadow = ''; }, 2000);
            return;
        }

        // Проверить wizard
        const wizardDone = await launcher.getSetting('wizardDone');
        if (!wizardDone) {
            document.getElementById('wizard-overlay').style.display = 'flex';
            return;
        }

        isLaunching = true; launchBtn.disabled = true;
        gameLaunched = false;
        launchBtn.classList.add('launching');
        launchBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> <span>ЗАПУСК...</span>';
        progressContainer.style.display = 'flex'; progressFill.style.background = '';
        playLaunchSound();

        try {
            const result = await launcher.launch();
            if (result.success) {
                gameLaunched = true;
                progressFill.style.width = '100%';
                progressText.textContent = 'ChaosClient запущен!';
                launchBtn.innerHTML = '<i class="fas fa-check"></i> <span>ЗАПУЩЕН</span>';
                playSuccessSound();
                // Показать монитор

            } else { throw new Error(result.error); }
        } catch (err) {
            progressText.textContent = `Ошибка: ${err.message}`;
            progressFill.style.width = '0%';
            progressFill.style.background = 'linear-gradient(135deg, #ef4444, #dc2626)';
            launchBtn.innerHTML = '<i class="fas fa-exclamation-triangle"></i> <span>ОШИБКА</span>';
            playErrorSound();
        }

        setTimeout(() => {
            isLaunching = false; launchBtn.disabled = false;
            gameLaunched = false;
            launchBtn.classList.remove('launching');
            launchBtn.innerHTML = '<i class="fas fa-play"></i> <span>ЗАПУСК</span> <div class="btn-glow"></div>';
            progressContainer.style.display = 'none'; progressFill.style.width = '0%'; progressFill.style.background = '';
        }, 5000);
    });


    // ===== Обработка событий =====
    let gameLaunched = false;

    launcher.onProgress((data) => {
        if (data.percent !== undefined) progressFill.style.width = `${data.percent}%`;
    });

    launcher.onStatus((msg) => {
        // Статус показывается ТОЛЬКО на прогресс-баре во время запуска, не после
        if (!gameLaunched) {
            progressText.textContent = msg || '';
        }
    });

    // ===== Логи (только на вкладке Логи) =====
    const logEntries = document.getElementById('log-entries');
    const logEmpty = document.getElementById('log-empty');
    const logsContainer = document.getElementById('logs-container');
    const autoscrollCheck = document.getElementById('log-autoscroll');
    const filterButtons = document.querySelectorAll('.log-filter-btn');
    let currentFilter = 'all';
    const MAX_LOG_ENTRIES = 300;
    let logCount = 0;

    filterButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            filterButtons.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            currentFilter = btn.dataset.filter;
            applyLogFilter();
        });
    });

    function applyLogFilter() {
        const entries = logEntries.querySelectorAll('.log-entry');
        entries.forEach(entry => {
            if (currentFilter === 'all') { entry.style.display = ''; }
            else if (currentFilter === 'game') { entry.style.display = (entry.classList.contains('level-game') || entry.classList.contains('level-game-err')) ? '' : 'none'; }
            else { entry.style.display = entry.classList.contains(`level-${currentFilter}`) ? '' : 'none'; }
        });
    }

    // Дедупликация логов в UI
    let lastLogMsg = '';
    let lastLogEl = null;
    let logRepeatCounter = 0;

    function addLogEntry(entry) {
        if (!entry || !entry.message) return;
        if (logEmpty) logEmpty.style.display = 'none';
        const level = entry.level || 'info';
        const msg = entry.message;

        // Дедупликация — если то же самое сообщение подряд, обновляем счётчик
        if (msg === lastLogMsg && lastLogEl) {
            logRepeatCounter++;
            const badge = lastLogEl.querySelector('.log-repeat');
            if (badge) {
                badge.textContent = `×${logRepeatCounter}`;
            } else {
                const b = document.createElement('span');
                b.className = 'log-repeat';
                b.textContent = `×${logRepeatCounter}`;
                lastLogEl.querySelector('.log-message').appendChild(b);
            }
            if (autoscrollCheck && autoscrollCheck.checked) logsContainer.scrollTop = logsContainer.scrollHeight;
            return;
        }

        lastLogMsg = msg;
        logRepeatCounter = 1;

        const el = document.createElement('div');
        el.className = `log-entry level-${level}`;
        const levelLabels = { 'info': 'ИНФО', 'warn': 'ПРЕД', 'error': 'ОШИБ', 'game': 'ИГРА', 'game-err': 'STDERR' };
        el.innerHTML = `<span class="log-time">${escapeHtml(entry.time || '')}</span><span class="log-level">${levelLabels[level] || level.toUpperCase()}</span><span class="log-message">${escapeHtml(msg)}</span>`;
        if (currentFilter !== 'all') {
            if (currentFilter === 'game') { if (level !== 'game' && level !== 'game-err') el.style.display = 'none'; }
            else if (level !== currentFilter) el.style.display = 'none';
        }
        logEntries.appendChild(el);
        lastLogEl = el;
        logCount++;
        if (logCount > MAX_LOG_ENTRIES) { const first = logEntries.firstChild; if (first) { logEntries.removeChild(first); logCount--; } }
        if (autoscrollCheck && autoscrollCheck.checked) logsContainer.scrollTop = logsContainer.scrollHeight;
    }

    launcher.onLog((entry) => addLogEntry(entry));

    launcher.onGameExit((data) => {
        const ts = new Date().toLocaleTimeString('ru-RU', { hour12: false });
        if (data.code !== null) {
            addLogEntry({ time: ts, level: data.code === 0 ? 'info' : 'error', message: `━━━ Minecraft завершился с кодом ${data.code} ━━━` });
        } else {
            addLogEntry({ time: ts, level: 'warn', message: `━━━ Minecraft завершился сигналом ${data.signal} ━━━` });
        }

    });

    document.getElementById('btn-copy-logs').addEventListener('click', () => {
        const entries = logEntries.querySelectorAll('.log-entry');
        const text = Array.from(entries).filter(e => e.style.display !== 'none').map(e => {
            const time = e.querySelector('.log-time')?.textContent || '';
            const level = e.querySelector('.log-level')?.textContent || '';
            const msg = e.querySelector('.log-message')?.textContent || '';
            return `[${time}] [${level}] ${msg}`;
        }).join('\n');
        navigator.clipboard.writeText(text).then(() => {
            const btn = document.getElementById('btn-copy-logs');
            btn.innerHTML = '<i class="fas fa-check"></i> Скопировано';
            setTimeout(() => { btn.innerHTML = '<i class="fas fa-copy"></i> Копировать'; }, 1500);
        });
    });

    document.getElementById('btn-clear-logs').addEventListener('click', () => {
        logEntries.innerHTML = ''; logCount = 0;
        lastLogMsg = ''; lastLogEl = null; logRepeatCounter = 0;
        if (logEmpty) logEmpty.style.display = '';
    });

    // ===== Горячие клавиши (F5/F12 заблокированы в main.js) =====

    // ===== Страница билдов =====
    initDevBuildsPage();

    // ===== Загрузка новостей с GitHub =====
    loadNews();
});

// ===== Утилиты =====
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// ===== Звуки лаунчера (Web Audio API, синтезированные) =====
const AudioCtx = window.AudioContext || window.webkitAudioContext;
let audioCtx = null;

function getAudioCtx() {
    if (!audioCtx) audioCtx = new AudioCtx();
    return audioCtx;
}

function playClickSound() {
    try {
        const ctx = getAudioCtx();
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.connect(gain); gain.connect(ctx.destination);
        osc.type = 'sine';
        osc.frequency.setValueAtTime(800, ctx.currentTime);
        osc.frequency.exponentialRampToValueAtTime(600, ctx.currentTime + 0.06);
        gain.gain.setValueAtTime(0.08, ctx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.08);
        osc.start(ctx.currentTime);
        osc.stop(ctx.currentTime + 0.08);
    } catch (e) { /* silent fail */ }
}

function playLaunchSound() {
    try {
        const ctx = getAudioCtx();
        [0, 0.12, 0.24].forEach((delay, i) => {
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();
            osc.connect(gain); gain.connect(ctx.destination);
            osc.type = 'sine';
            const freqs = [523.25, 659.25, 783.99]; // C5, E5, G5 chord arpeggio
            osc.frequency.setValueAtTime(freqs[i], ctx.currentTime + delay);
            gain.gain.setValueAtTime(0.1, ctx.currentTime + delay);
            gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + delay + 0.3);
            osc.start(ctx.currentTime + delay);
            osc.stop(ctx.currentTime + delay + 0.3);
        });
    } catch (e) { /* silent fail */ }
}

function playSuccessSound() {
    try {
        const ctx = getAudioCtx();
        [0, 0.1].forEach((delay, i) => {
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();
            osc.connect(gain); gain.connect(ctx.destination);
            osc.type = 'sine';
            osc.frequency.setValueAtTime(i === 0 ? 523.25 : 783.99, ctx.currentTime + delay);
            gain.gain.setValueAtTime(0.1, ctx.currentTime + delay);
            gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + delay + 0.25);
            osc.start(ctx.currentTime + delay);
            osc.stop(ctx.currentTime + delay + 0.25);
        });
    } catch (e) { /* silent fail */ }
}

function playErrorSound() {
    try {
        const ctx = getAudioCtx();
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.connect(gain); gain.connect(ctx.destination);
        osc.type = 'square';
        osc.frequency.setValueAtTime(220, ctx.currentTime);
        osc.frequency.exponentialRampToValueAtTime(110, ctx.currentTime + 0.2);
        gain.gain.setValueAtTime(0.06, ctx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.3);
        osc.start(ctx.currentTime);
        osc.stop(ctx.currentTime + 0.3);
    } catch (e) { /* silent fail */ }
}

function playHoverSound() {
    try {
        const ctx = getAudioCtx();
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.connect(gain); gain.connect(ctx.destination);
        osc.type = 'sine';
        osc.frequency.setValueAtTime(1200, ctx.currentTime);
        gain.gain.setValueAtTime(0.03, ctx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.04);
        osc.start(ctx.currentTime);
        osc.stop(ctx.currentTime + 0.04);
    } catch (e) { /* silent fail */ }
}

// Attach sounds to nav buttons and interactive elements
document.addEventListener('DOMContentLoaded', () => {
    // Nav button click sounds
    document.querySelectorAll('.nav-btn').forEach(btn => {
        btn.addEventListener('click', playClickSound);
    });
    // Nav button hover sounds
    document.querySelectorAll('.nav-btn').forEach(btn => {
        btn.addEventListener('mouseenter', playHoverSound);
    });
    // Launch button hover
    const launchBtnEl = document.getElementById('btn-launch');
    if (launchBtnEl) launchBtnEl.addEventListener('mouseenter', playHoverSound);
    // Settings card hover
    document.querySelectorAll('.setting-card').forEach(card => {
        card.addEventListener('mouseenter', playHoverSound);
    });
});

function updateAvatar(username) {
    const avatar = document.getElementById('player-avatar');
    const placeholder = 'data:image/svg+xml,' + encodeURIComponent(
        `<svg xmlns="http://www.w3.org/2000/svg" width="56" height="56" viewBox="0 0 56 56"><rect width="56" height="56" fill="#1a1a26" rx="12"/><text x="28" y="36" fill="#555570" font-size="24" font-weight="bold" text-anchor="middle" font-family="sans-serif">?</text></svg>`
    );
    if (username && username.length > 0) {
        avatar.src = `https://mc-heads.net/avatar/${encodeURIComponent(username)}/56`;
        avatar.onerror = () => { avatar.src = placeholder; };
    } else { avatar.src = placeholder; }
}

function formatMemory(mb) {
    const gb = parseInt(mb) / 1024;
    return gb >= 1 ? `${gb.toFixed(1)} ГБ` : `${mb} МБ`;
}

async function loadNews() {
    try {
        const news = await launcher.getNews();
        if (news && news.length > 0) {
            const list = document.getElementById('news-list');
            list.innerHTML = '';
            for (const item of news) {
                const card = document.createElement('div');
                card.className = 'news-card';
                const tagClass = item.tag === 'Релиз' ? 'tag-release' : item.tag === 'Исправление' ? 'tag-fix' : 'tag-feature';
                const iconClass = item.tag === 'Релиз' ? 'icon-release' : item.tag === 'Исправление' ? 'icon-fix' : 'icon-feature';
                const iconName = item.tag === 'Релиз' ? 'fa-rocket' : item.tag === 'Исправление' ? 'fa-wrench' : 'fa-sparkles';
                card.innerHTML = `
                    <div class="news-card-body">
                        <div class="news-header">
                            <div class="news-card-icon ${iconClass}"><i class="fas ${iconName}"></i></div>
                            <span class="news-tag ${tagClass}">${escapeHtml(item.tag)}</span>
                            <span class="news-date">${escapeHtml(item.date)}</span>
                        </div>
                        <h3 class="news-card-title">${escapeHtml(item.title)}</h3>
                        <p class="news-card-text">${escapeHtml(item.text)}</p>
                    </div>`;
                list.appendChild(card);
            }
        }
    } catch (e) { /* Используем статические новости */ }
}

// ===== Dev Builds Page =====
let lastUpdateResult = null;

async function initDevBuildsPage() {
    const channelBtns = document.querySelectorAll('.channel-btn');
    const releasesList = document.getElementById('releases-list');
    const commitsList = document.getElementById('commits-list');

    // Channel switcher
    channelBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            channelBtns.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            const channel = btn.dataset.channel;
            releasesList.style.display = channel === 'releases' ? '' : 'none';
            commitsList.style.display = channel === 'commits' ? '' : 'none';
            if (channel === 'commits' && commitsList.querySelector('.builds-loading')) loadCommits();
            if (channel === 'releases' && releasesList.querySelector('.builds-loading')) loadReleases();
        });
    });

    // Refresh button
    document.getElementById('btn-refresh-updates')?.addEventListener('click', async () => {
        const btn = document.getElementById('btn-refresh-updates');
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i>'; btn.disabled = true;
        await checkForUpdates();
        await loadReleases();
        btn.innerHTML = '<i class="fas fa-sync"></i> Обновить'; btn.disabled = false;
    });

    // Apply update button
    document.getElementById('btn-apply-update')?.addEventListener('click', async () => {
        if (!lastUpdateResult || !lastUpdateResult.modAsset) return;
        const btn = document.getElementById('btn-apply-update');
        const progressEl = document.getElementById('update-apply-progress');
        const progressText = document.getElementById('update-progress-text');
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Установка...';
        progressEl.style.display = '';
        progressText.textContent = `Загрузка ${lastUpdateResult.modAsset.name}...`;

        try {
            const result = await launcher.applyModUpdate(lastUpdateResult.modAsset);
            if (result.success) {
                progressText.textContent = `✅ Мод обновлён: ${result.fileName}`;
                btn.innerHTML = '<i class="fas fa-check"></i> Установлено';
                document.getElementById('update-title').textContent = 'Обновление установлено!';
                document.getElementById('update-icon').className = 'update-status-icon up-to-date';
                document.getElementById('update-icon').innerHTML = '<i class="fas fa-check-circle"></i>';
            } else {
                throw new Error(result.error);
            }
        } catch (e) {
            progressText.textContent = `❌ Ошибка: ${e.message}`;
            btn.innerHTML = '<i class="fas fa-download"></i> Повторить';
            btn.disabled = false;
        }
    });

    // Initial load
    await checkForUpdates();
    loadReleases();
}

async function checkForUpdates() {
    const titleEl = document.getElementById('update-title');
    const subtitleEl = document.getElementById('update-subtitle');
    const iconEl = document.getElementById('update-icon');
    const actionsEl = document.getElementById('update-actions');
    const changelogEl = document.getElementById('update-changelog');
    const applyBtn = document.getElementById('btn-apply-update');

    titleEl.textContent = 'Проверка обновлений...';
    iconEl.className = 'update-status-icon';
    iconEl.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
    actionsEl.style.display = 'none';
    document.getElementById('update-apply-progress').style.display = 'none';

    try {
        const version = await launcher.getVersion();
        subtitleEl.textContent = `Текущая версия: ${version}`;

        const result = await launcher.checkUpdate();
        lastUpdateResult = result;

        if (result.hasUpdate && result.modAsset) {
            titleEl.textContent = `Доступно обновление: ${result.releaseName || result.latestVersion}`;
            iconEl.className = 'update-status-icon has-update';
            iconEl.innerHTML = '<i class="fas fa-arrow-circle-up"></i>';
            actionsEl.style.display = '';
            applyBtn.disabled = false;
            applyBtn.innerHTML = '<i class="fas fa-download"></i> Установить обновление';
            changelogEl.textContent = result.releaseBody || '';
        } else if (result.error) {
            titleEl.textContent = 'Не удалось проверить обновления';
            iconEl.className = 'update-status-icon error';
            iconEl.innerHTML = '<i class="fas fa-exclamation-circle"></i>';
        } else {
            titleEl.textContent = 'Установлена последняя версия';
            iconEl.className = 'update-status-icon up-to-date';
            iconEl.innerHTML = '<i class="fas fa-check-circle"></i>';
        }
    } catch (e) {
        titleEl.textContent = 'Ошибка проверки обновлений';
        iconEl.className = 'update-status-icon error';
        iconEl.innerHTML = '<i class="fas fa-exclamation-circle"></i>';
    }
}

async function loadReleases() {
    const listEl = document.getElementById('releases-list');
    listEl.innerHTML = '<div class="builds-loading"><i class="fas fa-spinner fa-spin"></i> Загрузка релизов...</div>';

    try {
        const releases = await launcher.getReleases();
        if (!releases || releases.length === 0) {
            listEl.innerHTML = '<div class="builds-empty"><i class="fas fa-box-open"></i>Нет доступных релизов</div>';
            return;
        }

        listEl.innerHTML = '';
        for (const rel of releases) {
            const modJar = rel.assets.find(a => a.name.endsWith('.jar') && a.name.includes('ChaosClient'));
            const tagClass = rel.prerelease ? 'prerelease' : rel.draft ? 'draft' : 'release';
            const tagText = rel.prerelease ? 'Dev' : rel.draft ? 'Черновик' : 'Release';
            const dateStr = rel.publishedAt ? new Date(rel.publishedAt).toLocaleDateString('ru-RU', { day: 'numeric', month: 'short', year: 'numeric' }) : '';
            const assetCount = rel.assets.length;

            const el = document.createElement('div');
            el.className = 'build-item';
            el.innerHTML = `
                <div class="build-item-icon ${tagClass}"><i class="fas fa-${rel.prerelease ? 'flask' : 'tag'}"></i></div>
                <div class="build-item-info">
                    <div class="build-item-title">${escapeHtml(rel.name || rel.tag)}</div>
                    <div class="build-item-meta">
                        <span><span class="build-tag ${tagClass}">${tagText}</span></span>
                        <span><i class="fas fa-calendar"></i> ${escapeHtml(dateStr)}</span>
                        <span><i class="fas fa-file"></i> ${assetCount} файл(ов)</span>
                    </div>
                </div>
                <div class="build-item-actions">
                    ${modJar ? `<button class="btn-small-build" data-url="${escapeHtml(modJar.url)}" data-name="${escapeHtml(modJar.name)}" data-tag="${escapeHtml(rel.tag)}"><i class="fas fa-download"></i> Мод</button>` : ''}
                </div>
            `;

            // Install button for mod
            const installBtn = el.querySelector('.btn-small-build');
            if (installBtn) {
                installBtn.addEventListener('click', async () => {
                    installBtn.disabled = true;
                    installBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
                    try {
                        const result = await launcher.applyModUpdate({
                            name: installBtn.dataset.name,
                            url: installBtn.dataset.url
                        });
                        if (result.success) {
                            installBtn.innerHTML = '<i class="fas fa-check"></i> OK';
                            installBtn.classList.add('active');
                        } else {
                            installBtn.innerHTML = '<i class="fas fa-times"></i>';
                            installBtn.disabled = false;
                        }
                    } catch (e) {
                        installBtn.innerHTML = '<i class="fas fa-times"></i>';
                        installBtn.disabled = false;
                    }
                });
            }

            listEl.appendChild(el);
        }
    } catch (e) {
        listEl.innerHTML = `<div class="builds-empty"><i class="fas fa-exclamation-circle"></i>Ошибка загрузки: ${escapeHtml(e.message)}</div>`;
    }
}

async function loadCommits() {
    const listEl = document.getElementById('commits-list');
    listEl.innerHTML = '<div class="builds-loading"><i class="fas fa-spinner fa-spin"></i> Загрузка коммитов...</div>';

    try {
        const commits = await launcher.getCommits();
        if (!commits || commits.length === 0) {
            listEl.innerHTML = '<div class="builds-empty"><i class="fas fa-code-commit"></i>Нет коммитов</div>';
            return;
        }

        listEl.innerHTML = '';
        for (const c of commits) {
            const dateStr = c.date ? new Date(c.date).toLocaleDateString('ru-RU', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' }) : '';
            const el = document.createElement('div');
            el.className = 'build-item';
            el.innerHTML = `
                <div class="build-item-icon commit"><i class="fas fa-code-commit"></i></div>
                <div class="build-item-info">
                    <div class="build-item-title">${escapeHtml(c.message)}</div>
                    <div class="build-item-meta">
                        <span><code style="color:var(--accent);font-size:11px;">${escapeHtml(c.shortSha)}</code></span>
                        <span><i class="fas fa-user"></i> ${escapeHtml(c.author)}</span>
                        <span><i class="fas fa-clock"></i> ${escapeHtml(dateStr)}</span>
                    </div>
                </div>
            `;
            listEl.appendChild(el);
        }
    } catch (e) {
        listEl.innerHTML = `<div class="builds-empty"><i class="fas fa-exclamation-circle"></i>Ошибка: ${escapeHtml(e.message)}</div>`;
    }
}
