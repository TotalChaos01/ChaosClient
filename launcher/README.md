# ChaosClient Launcher

Modern, beautiful launcher for ChaosClient — a universal Minecraft client.

## Features
- 🎮 One-click Minecraft launch with ChaosClient mod
- 🎨 Modern dark UI with animations
- ⚙️ RAM, Java, and game directory configuration
- 📦 Multi-version support (1.8 - 1.21)
- 🔒 Zero telemetry — fully local
- 🐧 Linux (.deb, .AppImage) and Windows (.exe) support

## Development

### Prerequisites
- Node.js 18+
- npm or yarn

### Install dependencies
```bash
cd launcher
npm install
```

### Run in development mode
```bash
npm start
```

### Build for distribution

**Linux (deb + AppImage):**
```bash
npm run build:linux
```

**Windows (exe):**
```bash
npm run build:win
```

**All platforms:**
```bash
npm run build:all
```

Built packages will be in the `dist/` directory.

## Tech Stack
- Electron 33
- HTML/CSS/JS (no framework overhead)
- electron-builder for packaging
- electron-store for local settings
