document.addEventListener('DOMContentLoaded', async () => {
    const $ = id => document.getElementById(id);
    const sleep = ms => new Promise(r => setTimeout(r, ms));
    // ===== BOOT =====
    const bootScreen = $('boot-screen'), app = $('app');
    const bootLog = $('boot-log'), bootBar = $('boot-bar'), bootSpinner = $('boot-spinner');
    const W = 30, spin = ['|','/','-','\\'];
    let si = 0, spInt, pct = 0;
    const bar = p => {
        const f = Math.round(p / 100 * W);
        bootBar.textContent = '[' + '#'.repeat(f) + ' '.repeat(W - f) + '] ' + String(p).padStart(3) + '%';
    };
    const startSpin = () => { spInt = setInterval(() => { si=(si+1)%4; bootSpinner.textContent=spin[si]; }, 80); };
    const stopSpin = () => { clearInterval(spInt); bootSpinner.textContent = '✓'; };
    const blog = (t, c) => {
        const d = document.createElement('div');
        d.innerHTML = '<span class="' + (c||'b-dim') + '">' + t + '</span>';
        bootLog.appendChild(d); bootLog.scrollTop = 99999;
    };
    const steps = [
        { m:'[ OK ] ChaosClient Kernel v1.3.0', c:'b-ok', j:12 },
        { m:'[ OK ] Mounting filesystem',        c:'b-ok', j:10 },
        { m:'[ OK ] Loading Fabric 1.21.11',     c:'b-ok', j:15 },
        { m:'[ OK ] JVM runtime check',          c:'b-ok', j:10 },
        { m:'[ OK ] Baritone AI loaded',         c:'b-ok', j:12 },
        { m:'[ OK ] Render modules ready',       c:'b-ok', j:10 },
        { m:'[ OK ] ViaMCP bridge OK',           c:'b-ok', j:10 },
        { m:'[ OK ] Integrity verified',         c:'b-ok', j:8 },
        { m:'System ready.',                     c:'b-info', j:13 },
    ];
    startSpin(); bar(0);
    for (const s of steps) {
        blog(s.m, s.c);
        const target = Math.min(100, pct + s.j);
        while (pct < target) {
            pct = Math.min(target, pct + (Math.random() < 0.3 ? 2 : 1));
            bar(pct);
            await sleep(15 + Math.random() * 30);
        }
        await sleep(40 + Math.random() * 80);
    }
    while (pct < 100) { pct++; bar(pct); await sleep(10); }
    stopSpin();
    // ===== CHECK UPDATES IN BOOT =====
    let modUpdateData = null;
    try {
        const upd = await launcher.checkUpdate();
        if (upd && !upd.isFirstInstall) {
            // Launcher update — only if NOT first install
            if (upd.launcherAssets && upd.launcherAssets.length > 0 && upd.latestVersion !== upd.currentVersion) {
                const platform = await launcher.getPlatform();
                let asset = null;
                if (platform === 'linux') asset = upd.launcherAssets.find(a => a.name.endsWith('.AppImage')) || upd.launcherAssets.find(a => a.name.endsWith('.deb'));
                else if (platform === 'win32') asset = upd.launcherAssets.find(a => a.name.endsWith('.exe'));
                else asset = upd.launcherAssets[0];
                if (asset) {
                    $('boot-update-ver').textContent = upd.latestVersion;
                    $('boot-update').classList.remove('hidden');
                    const decision = await new Promise(resolve => {
                        $('boot-update-yes').onclick = () => resolve('update');
                        $('boot-update-skip').onclick = () => resolve('skip');
                    });
                    if (decision === 'update') {
                        $('boot-update-btns').classList.add('hidden');
                        $('boot-update-progress').classList.remove('hidden');
                        $('boot-update-progress').textContent = 'Скачивание...';
                        try {
                            await launcher.selfUpdate(asset);
                            $('boot-update-progress').textContent = 'Перезапуск...';
                            return;
                        } catch(e) {
                            $('boot-update-progress').textContent = 'Ошибка: ' + e.message;
                            await sleep(2000);
                        }
                    }
                    $('boot-update').classList.add('hidden');
                }
            }
            // Mod update — only if NOT first install and mod filename differs
            if (upd.modAsset && upd.modAsset.name !== upd.installedModName) {
                modUpdateData = upd;
            }
        }
    } catch(e) { blog('[ !! ] Update check: ' + e.message, 'b-warn'); }
    // ===== TRANSITION =====
    await sleep(300);
    bootScreen.classList.add('fade-out');
    await sleep(400);
    app.classList.remove('app-hidden');
    app.classList.add('app-visible');
    await sleep(300);
    bootScreen.style.display = 'none';
    // ===== LOAD SETTINGS =====
    try {
        const s = await launcher.getAllSettings();
        if (s.username) { $('input-username').value = s.username; updateAvatar(s.username); }
        if (s.memory) { $('input-ram').value = s.memory; $('ram-value').textContent = s.memory + ' МБ'; }
        if (s.javaMode) {
            $('java-mode').value = s.javaMode;
            toggleJavaUI(s.javaMode);
        }
        if (s.customJavaPath) $('custom-java-path').textContent = s.customJavaPath;
        const ver = await launcher.getVersion();
        if (ver) $('profile-ver').textContent = 'v' + ver;
        const gd = await launcher.getGameDir();
        if (gd) $('game-dir-path').textContent = gd;
        const js = await launcher.getJavaStatus();
        if (js && js.bundledInstalled) $('java-status').textContent = js.bundledVersion || 'Java 21 установлена';
    } catch(e) {}
    // ===== SHOW MOD UPDATE BANNER =====
    if (modUpdateData && modUpdateData.modAsset) {
        const desc = (modUpdateData.releaseName || modUpdateData.latestVersion) + ' — ' + modUpdateData.modAsset.name;
        $('mod-update-desc').textContent = desc;
        $('mod-update-banner').classList.remove('hidden');
    }
    // ===== LOAD NEWS =====
    try {
        const news = await launcher.getNews();
        if (news && news.length > 0) {
            const nl = $('news-list');
            nl.innerHTML = '';
            for (const n of news) {
                const tc = n.tag === 'Исправление' ? 'fix' : n.tag === 'Нововведение' ? 'feat' : '';
                nl.innerHTML += '<div class="news-item"><div class="news-dot"></div><div class="news-body">'
                    + '<div class="news-head"><span class="news-tag ' + tc + '">' + esc(n.tag) + '</span>'
                    + '<span class="news-date">' + esc(n.date) + '</span></div>'
                    + '<div class="news-title">' + esc(n.title) + '</div></div></div>';
            }
        }
    } catch(e) {}
    // ===== NAV =====
    document.querySelectorAll('.nav-btn').forEach(b => {
        b.addEventListener('click', () => {
            document.querySelectorAll('.nav-btn').forEach(x => x.classList.remove('active'));
            document.querySelectorAll('.page').forEach(x => x.classList.remove('active'));
            b.classList.add('active');
            $('page-' + b.dataset.page).classList.add('active');
        });
    });
    // ===== TITLEBAR =====
    $('btn-minimize').onclick = () => launcher.minimize();
    $('btn-maximize').onclick = () => launcher.maximize();
    $('btn-close').onclick = () => launcher.close();
    // ===== USERNAME =====
    const uInput = $('input-username');
    uInput.addEventListener('input', () => {
        const n = uInput.value.trim();
        updateAvatar(n);
        try { launcher.setSetting('username', n); } catch(e) {}
    });
    // ===== RAM =====
    const ramS = $('input-ram'), ramV = $('ram-value');
    ramS.oninput = () => {
        ramV.textContent = ramS.value + ' МБ';
        try { launcher.setSetting('memory', parseInt(ramS.value)); } catch(e) {}
    };
    // ===== JAVA SETTINGS =====
    const javaMode = $('java-mode');
    const javaBrowse = $('btn-java-browse');
    const customJavaRow = $('custom-java-row');
    function toggleJavaUI(mode) {
        if (mode === 'custom') {
            javaBrowse.classList.remove('hidden');
            customJavaRow.classList.remove('hidden');
        } else {
            javaBrowse.classList.add('hidden');
            customJavaRow.classList.add('hidden');
        }
    }
    javaMode.addEventListener('change', () => {
        const mode = javaMode.value;
        toggleJavaUI(mode);
        try { launcher.setSetting('javaMode', mode); } catch(e) {}
    });
    javaBrowse?.addEventListener('click', async () => {
        try {
            const p = await launcher.openFileDialog();
            if (p) {
                $('custom-java-path').textContent = p;
                launcher.setSetting('customJavaPath', p);
                // Verify
                const r = await launcher.verifyJava(p);
                if (r.success) {
                    $('java-status').textContent = r.version;
                    clog('Java проверена: ' + r.version, '#00ff88');
                } else {
                    $('java-status').textContent = 'Ошибка: ' + r.error;
                    clog('Java невалидна: ' + r.error, '#ff2255');
                }
            }
        } catch(e) {}
    });
    $('btn-java-detect')?.addEventListener('click', async () => {
        try {
            const list = await launcher.detectJava();
            if (list && list.length > 0) {
                // Pick best (21+) or first
                const best = list.find(j => j.major >= 21) || list[0];
                $('custom-java-path').textContent = best.path;
                $('java-status').textContent = best.version;
                launcher.setSetting('customJavaPath', best.path);
                clog('Найдено ' + list.length + ' Java. Выбрана: ' + best.version + ' — ' + best.path, '#00ff88');
            } else {
                clog('Java не найдена на системе', '#ffaa00');
            }
        } catch(e) {}
    });
    // ===== PLAY BUTTON =====
    const playBtn = $('btn-play'), playText = $('btn-play-text'), playStat = $('play-status');
    let gameRunning = false, launching = false;
    let launchSpinInterval = null;
    const spinChars = ['◐','◓','◑','◒'];
    let lsi = 0;
    playBtn.addEventListener('click', async () => {
        // If game is running — kill it
        if (gameRunning) {
            try {
                await launcher.killGame();
                clog('Игра принудительно завершена', '#ffaa00');
            } catch(e) {}
            resetPlayBtn();
            playStat.textContent = 'Остановлено';
            setStatus('Готов', true);
            return;
        }
        if (launching) return;
        const username = uInput.value.trim();
        if (!username) {
            playStat.textContent = '⚠ Введите ник!';
            playStat.style.color = '#ff2255';
            uInput.focus();
            return;
        }
        launching = true;
        playBtn.disabled = true;
        playBtn.classList.add('launching');
        playStat.style.color = '';
        lsi = 0;
        launchSpinInterval = setInterval(() => {
            lsi = (lsi + 1) % spinChars.length;
            playText.textContent = spinChars[lsi] + ' Запуск...';
        }, 120);
        setStatus('Запускается...', false);
        clog('Запуск ChaosClient для ' + username + '...', '#bf00ff');
        try {
            const r = await launcher.launch();
            if (r && r.error) {
                clog('Ошибка: ' + r.error, '#ff2255');
                playStat.textContent = '✗ ' + r.error;
                playStat.style.color = '#ff2255';
                setStatus('Ошибка', false);
                resetPlayBtn();
            } else {
                clearInterval(launchSpinInterval);
                launching = false;
                gameRunning = true;
                playBtn.disabled = false;
                playBtn.classList.remove('launching');
                playBtn.classList.add('running');
                playText.innerHTML = '<i class="fas fa-stop"></i> Закрыть';
                playStat.textContent = 'PID: ' + (r.pid || '?');
                setStatus('Игра запущена', true);
                clog('Клиент запущен (PID: ' + (r.pid || '?') + ')', '#00ff88');
            }
        } catch(e) {
            clog('FATAL: ' + e.message, '#ff2255');
            playStat.textContent = '✗ Ошибка запуска';
            playStat.style.color = '#ff2255';
            setStatus('Ошибка', false);
            resetPlayBtn();
        }
    });
    function resetPlayBtn() {
        clearInterval(launchSpinInterval);
        launching = false;
        gameRunning = false;
        playBtn.disabled = false;
        playBtn.classList.remove('launching', 'running');
        playText.innerHTML = '<i class="fas fa-play"></i> Играть';
    }
    // ===== LAUNCH EVENTS =====
    try {
        launcher.onProgress(d => {
            if (d.percent !== undefined) {
                playStat.textContent = d.message || ('Загрузка... ' + d.percent + '%');
            } else if (d.message) {
                playStat.textContent = d.message;
            }
        });
        launcher.onStatus(d => {
            clog(typeof d === 'string' ? d : JSON.stringify(d), '#9a8fb5');
            if (typeof d === 'string') playStat.textContent = d;
        });
        launcher.onLog(e => {
            const msg = typeof e === 'string' ? e : e.message || '';
            const clr = (e.level === 'error') ? '#ff2255' : (e.level === 'warn') ? '#ffaa00' : '#9a8fb5';
            clog(msg, clr);
        });
        launcher.onGameExit(d => {
            clog('Игра завершена (код ' + (d.code || 0) + ')', d.code === 0 ? '#00ff88' : '#ffaa00');
            resetPlayBtn();
            playStat.textContent = 'Игра завершена';
            setStatus('Готов', true);
        });
    } catch(e) {}
    // ===== MOD UPDATE =====
    $('btn-mod-update')?.addEventListener('click', async () => {
        if (!modUpdateData || !modUpdateData.modAsset) return;
        const btn = $('btn-mod-update');
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Обновление...';
        try {
            const r = await launcher.applyModUpdate(modUpdateData.modAsset);
            if (r.success) {
                btn.innerHTML = '<i class="fas fa-check"></i> Обновлено';
                $('mod-update-banner').style.borderColor = '#00ff88';
                clog('Мод обновлён: ' + r.fileName, '#00ff88');
            } else { throw new Error(r.error); }
        } catch(e) {
            btn.innerHTML = '<i class="fas fa-times"></i> Ошибка';
            btn.disabled = false;
            clog('Ошибка обновления: ' + e.message, '#ff2255');
        }
    });
    // ===== SETTINGS =====
    $('btn-open-gamedir')?.addEventListener('click', async () => {
        try { const d = await launcher.getGameDir(); if (d) launcher.openPath(d); } catch(e) {}
    });
    $('btn-reinstall')?.addEventListener('click', async () => {
        const btn = $('btn-reinstall');
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Удаление...';
        try {
            const r = await launcher.reinstallClient();
            if (r && r.success) {
                clog('Переустановка: ' + (r.message || 'OK'), '#00ff88');
                btn.innerHTML = '<i class="fas fa-check"></i> Готово';
            } else {
                clog('Ошибка: ' + (r?.error || 'Unknown'), '#ff2255');
                btn.innerHTML = '<i class="fas fa-times"></i> Ошибка';
            }
        } catch(e) {
            clog('Ошибка: ' + e.message, '#ff2255');
            btn.innerHTML = '<i class="fas fa-times"></i> Ошибка';
        }
        setTimeout(() => {
            btn.disabled = false;
            btn.innerHTML = '<i class="fas fa-trash-can"></i> Переустановить';
        }, 3000);
    });
    // ===== CONSOLE BUTTONS =====
    $('btn-clear-log')?.addEventListener('click', () => {
        $('log-console').innerHTML = '<div class="log-line log-muted">Консоль очищена</div>';
    });
    $('btn-copy-log')?.addEventListener('click', () => {
        const lc = $('log-console');
        const text = Array.from(lc.querySelectorAll('.log-line')).map(l => l.textContent).join('\n');
        navigator.clipboard.writeText(text).then(() => {
            const btn = $('btn-copy-log');
            const orig = btn.innerHTML;
            btn.innerHTML = '<i class="fas fa-check"></i> Скопировано';
            setTimeout(() => { btn.innerHTML = orig; }, 1500);
        });
    });
    $('btn-load-mclog')?.addEventListener('click', async () => {
        try {
            const r = await launcher.getLogFile();
            if (r && r.success && r.content) {
                const lc = $('log-console');
                lc.innerHTML = '';
                const lines = r.content.split('\n');
                for (const line of lines) {
                    const d = document.createElement('div');
                    d.className = 'log-line';
                    const lower = line.toLowerCase();
                    d.style.color = lower.includes('error') || lower.includes('exception') ? '#ff2255'
                        : lower.includes('warn') ? '#ffaa00'
                        : lower.includes('info') ? '#9a8fb5' : '#5c5275';
                    d.textContent = line;
                    lc.appendChild(d);
                }
                lc.scrollTop = 99999;
                clog('Загружен MC лог: ' + r.path, '#bf00ff');
            } else {
                clog('Лог не найден: ' + (r?.error || 'latest.log отсутствует'), '#ffaa00');
            }
        } catch(e) { clog('Ошибка чтения лога: ' + e.message, '#ff2255'); }
    });
    // ===== HELPERS =====
    function clog(text, color) {
        const lc = $('log-console');
        const d = document.createElement('div');
        d.className = 'log-line';
        d.style.color = color || '#9a8fb5';
        d.textContent = '[' + new Date().toLocaleTimeString() + '] ' + text;
        lc.appendChild(d);
        lc.scrollTop = 99999;
    }
    function setStatus(text, ok) {
        $('status-text').textContent = text;
        const d = $('status-dot');
        d.style.background = ok ? '#00ff88' : '#ffaa00';
        d.style.boxShadow = '0 0 6px ' + (ok ? '#00ff88' : '#ffaa00');
    }
    function updateAvatar(name) {
        const img = $('player-avatar');
        const ph = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" width="36" height="36"><rect width="36" height="36" fill="#130e1e" rx="18"/><text x="18" y="24" fill="#5c5275" font-size="16" font-weight="bold" text-anchor="middle" font-family="sans-serif">?</text></svg>');
        if (name && name.length > 0) {
            img.src = 'https://mc-heads.net/avatar/' + encodeURIComponent(name) + '/36';
            img.onerror = () => { img.src = ph; };
        } else { img.src = ph; }
    }
    function esc(t) { const d = document.createElement('div'); d.textContent = t; return d.innerHTML; }
    updateAvatar(uInput.value.trim());
});
