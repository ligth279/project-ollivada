# 🛡️ ZeroPanic v1.0.0 - Arctic Blue Edition

**First stable release of ZeroPanic - Real-time cybersecurity threat detection for Linux applications**

## ✨ Features

### 🔍 Threat Detection
- **Real-time scanning** against Supabase threat database
- Automatically matches installed apps with known vulnerabilities
- Severity indicators (Critical, High, Medium, Low)
- Direct links to CVE/security advisories

### 🎨 Modern UI
- **Arctic Blue theme** - Clean, professional interface
- Ice blue panels with cyan accents
- Animated gradient background
- Responsive design with search functionality

### 💾 Smart Caching
- **20-hour app scan cache** - Reduces system overhead
- **5-hour threat check cache** - Stay updated without spam
- Force scan option to bypass cache
- SQLite local database

### 🔐 Security
- Local user authentication with SHA-256 password hashing
- Data stored locally in `~/.applister/`
- No telemetry or tracking
- Open source

### 🖥️ Platform Support
- **Linux**: Snap, Flatpak, APT packages, Desktop entries
- **Future**: Windows support planned
- Works on any Linux distribution (Ubuntu, Fedora, Arch, Manjaro, etc.)

## 🐧 Installation

### Option 1: .deb Package (Ubuntu/Debian - Easiest!)

**For Ubuntu, Zorin, Debian, Linux Mint, Pop!_OS, Elementary OS:**

```bash
# Download
wget https://github.com/ligth279/project-ollivada/releases/download/v1.0.0/zeropanic_1.0.0.deb

# Install (one command!)
sudo apt install ./zeropanic_1.0.0.deb

# Launch from applications menu or terminal
zeropanic
```

**What you get:**
- ✅ Installs to system like any other app
- ✅ Appears in applications menu with icon
- ✅ Desktop launcher created automatically
- ✅ Easy to uninstall: `sudo apt remove zeropanic`
- ✅ 45 MB package size

### Option 2: AppImage (All Linux Distros)

```bash
# Download
wget https://github.com/ligth279/project-ollivada/releases/download/v1.0.0/ZeroPanic-1.0.0-x86_64.AppImage

# Make executable
chmod +x ZeroPanic-1.0.0-x86_64.AppImage

# Run
./ZeroPanic-1.0.0-x86_64.AppImage
```

**What you get:**
- ✅ Works on ANY Linux distro (Fedora, Arch, etc.)
- ✅ No installation needed - portable
- ✅ 54 MB file size

### ⚠️ Troubleshooting

**"Package may be corrupted" or "Permission denied" or "Exception in Application start method"**

The AppImage/deb requires executable permissions. Run:
```bash
# For AppImage
chmod +x ZeroPanic-1.0.0-x86_64.AppImage
./ZeroPanic-1.0.0-x86_64.AppImage

# For .deb (after installation)
sudo apt install ./zeropanic_1.0.0.deb
zeropanic
```

**"CSS Error" or "LoadException" when launching**

The current version has a CSS compatibility issue with JavaFX. To fix:

```bash
# Extract the AppImage
./ZeroPanic-1.0.0-x86_64.AppImage --appimage-extract

# Run from extracted folder
cd squashfs-root
./AppRun
```

**Note:** A patched version 1.0.1 will be released soon without this CSS issue.

**"FUSE error" or "dlopen() error loading libfuse.so.2"**

AppImage needs FUSE. Install it:
```bash
# Ubuntu/Debian/Zorin
sudo apt install libfuse2

# Fedora
sudo dnf install fuse

# Arch
sudo pacman -S fuse2
```

**Download incomplete or file corrupted**

Verify the download:
```bash
# Check file size (should be ~54 MB for AppImage, ~45 MB for .deb)
ls -lh ZeroPanic-1.0.0-x86_64.AppImage

# Re-download if size is wrong
wget https://github.com/ligth279/project-ollivada/releases/download/v1.0.0/ZeroPanic-1.0.0-x86_64.AppImage
```

**Double-click doesn't work in file manager**

Right-click → Properties → Permissions → Check "Allow executing file as program"

Or use terminal as shown above.

## 🚀 Getting Started

1. **Launch** the application
2. **Create account** - Your credentials are stored locally
3. **Initial scan** - Wait for the app to scan your installed applications (~10-30 seconds)
4. **View threats** - Check the Alerts panel for any detected vulnerabilities
5. **Browse apps** - See all installed applications in the All Apps panel
6. **Force scan** - Use the Force Scan button to refresh immediately

## 📦 What's Included

- ✅ **Java 25 runtime** bundled (no separate installation needed)
- ✅ **JavaFX 21** for the GUI
- ✅ **SQLite 3.47** for local database
- ✅ **All dependencies** packaged inside
- ✅ **54 MB** self-contained AppImage

## 🔧 System Requirements

- **OS**: Linux x86_64 (any modern distribution)
- **RAM**: 512 MB minimum, 1 GB recommended
- **Disk**: ~60 MB for AppImage + ~5 MB for database
- **Internet**: Required for threat database synchronization

## 📊 Technical Stack

- **Language**: Java 25
- **Framework**: JavaFX 21
- **Database**: SQLite 3.47.1
- **API**: Supabase REST API
- **Build**: Maven 3.9.12
- **Package**: jpackage + AppImageTool

## 📸 Screenshots

![Login Screen](https://via.placeholder.com/800x600?text=Login+Screen+-+Upload+your+screenshot)
*Arctic Blue themed login with animated gradient background*

![Dashboard](https://via.placeholder.com/800x600?text=Dashboard+-+Upload+your+screenshot)
*Main dashboard showing threat alerts and statistics*

![Apps Panel](https://via.placeholder.com/800x600?text=Apps+Panel+-+Upload+your+screenshot)
*Browse and search all installed applications*

## 🐛 Known Issues

- ⚠️ **CSS compatibility issue** - JavaFX doesn't support `-fx-background-size` property. Causes "CSS Error parsing" warning but doesn't prevent the app from working. Will be fixed in v1.0.1.
- ⚠️ **LoadException on some systems** - Related to CSS parsing. Workaround: Extract AppImage and run from folder (see Troubleshooting above).
- ⚠️ Restricted method warnings in Java 25 (cosmetic, will be addressed in future release)

## 🔜 Upcoming Features

- 🪟 Windows support (.exe installer)
- 📦 Native .deb packages for Debian/Ubuntu
- 🔔 Desktop notifications for new threats
- 📊 Threat history and timeline
- 🌐 Multi-language support
- 🎨 Additional themes

## 📝 Changelog

### v1.0.0 (2026-01-15)

**Added:**
- Initial stable release
- Real-time threat detection with Supabase integration
- User authentication system
- Application scanning (Snap, Flatpak, APT, Desktop entries)
- Arctic Blue UI theme
- SQLite caching system
- Force scan functionality
- Search and filter capabilities
- AppImage packaging

## 🤝 Contributing

Found a bug? Have a feature request? Contributions are welcome!

- 🐛 [Report a bug](https://github.com/ligth279/project-ollivada/issues/new?labels=bug)
- 💡 [Request a feature](https://github.com/ligth279/project-ollivada/issues/new?labels=enhancement)
- 🔧 [Submit a pull request](https://github.com/ligth279/project-ollivada/pulls)

## 📄 License

This project is open source. Check the repository for license details.

## 🙏 Credits

- **Developer**: Ullivada
- **Framework**: JavaFX by Oracle
- **Database**: Supabase
- **Icons**: Shield emoji graphics
- **Packaging**: AppImage project

---

**⭐ If you find ZeroPanic useful, please star the repository!**

**🔗 Links:**
- 📚 [Documentation](https://github.com/ligth279/project-ollivada#readme)
- 💬 [Discussions](https://github.com/ligth279/project-ollivada/discussions)
- 🐛 [Issues](https://github.com/ligth279/project-ollivada/issues)
