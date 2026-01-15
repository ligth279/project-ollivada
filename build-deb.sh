#!/bin/bash
# Build ZeroPanic .deb package for Ubuntu/Debian
# Note: Run this on the HOST system (not inside Flatpak)

set -e

echo "📦 Building ZeroPanic .deb package for Ubuntu/Debian..."

# Variables
APP_NAME="zeropanic"
APP_VERSION="1.0.0"

# Check if running inside Flatpak
if [ -f /.flatpak-info ]; then
    echo "⚠️  Detected Flatpak environment. Building via host system..."
    flatpak-spawn --host bash -c "cd '$(pwd)' && ./build-deb-host.sh"
    exit $?
fi

# Check for required tools
if ! command -v dpkg-deb &> /dev/null; then
    echo "❌ Error: dpkg-deb not found. Install with:"
    echo "   sudo apt install dpkg-dev"
    exit 1
fi

# Clean previous builds
echo "🧹 Cleaning previous builds..."
rm -rf build/deb-manual
mkdir -p build/deb-manual

# Create Debian package structure manually
DEB_DIR="build/deb-manual/${APP_NAME}_${APP_VERSION}"
mkdir -p "$DEB_DIR/DEBIAN"
mkdir -p "$DEB_DIR/opt/zeropanic"
mkdir -p "$DEB_DIR/usr/bin"
mkdir -p "$DEB_DIR/usr/share/applications"
mkdir -p "$DEB_DIR/usr/share/icons/hicolor/256x256/apps"

# Copy AppImage contents
if [ ! -d "build/appimage/ZeroPanic" ]; then
    echo "❌ Error: AppImage build not found. Run ./build-appimage.sh first"
    exit 1
fi

echo "📦 Copying application files..."
cp -r build/appimage/ZeroPanic/* "$DEB_DIR/opt/zeropanic/"

# Create launcher script
cat > "$DEB_DIR/usr/bin/zeropanic" << 'EOF'
#!/bin/bash
exec /opt/zeropanic/bin/ZeroPanic "$@"
EOF
chmod +x "$DEB_DIR/usr/bin/zeropanic"

# Create desktop entry
cat > "$DEB_DIR/usr/share/applications/zeropanic.desktop" << 'EOF'
[Desktop Entry]
Name=ZeroPanic
Comment=Cybersecurity threat detection for installed applications
Exec=zeropanic
Icon=zeropanic
Type=Application
Categories=Security;System;Utility;
Terminal=false
StartupNotify=true
EOF

# Create simple icon
cp build/appimage/ZeroPanic.AppDir/zeropanic.svg "$DEB_DIR/usr/share/icons/hicolor/256x256/apps/zeropanic.svg" 2>/dev/null || echo "Icon not found, skipping"

# Create control file
cat > "$DEB_DIR/DEBIAN/control" << EOF
Package: zeropanic
Version: $APP_VERSION
Section: utils
Priority: optional
Architecture: amd64
Maintainer: Ullivada <your-email@example.com>
Description: Cybersecurity threat detection for installed applications
 ZeroPanic scans your installed applications against a real-time threat
 database and alerts you to known vulnerabilities and security risks.
 .
 Features:
  - Real-time threat detection
  - Arctic Blue modern UI
  - Smart caching system
  - Local authentication
  - Supports Snap, Flatpak, APT, and Desktop entries
Depends: libc6, libx11-6, libxext6, libxrender1, libxtst6, libxi6
EOF

# Build the package
echo "🔨 Building .deb package..."
dpkg-deb --build "$DEB_DIR"

# Move to project root
mv "build/deb-manual/${APP_NAME}_${APP_VERSION}.deb" .

echo ""
echo "✅ .deb package created successfully!"
echo "📦 File: ${APP_NAME}_${APP_VERSION}.deb"
echo "📏 Size: $(du -h "${APP_NAME}_${APP_VERSION}.deb" | cut -f1)"
echo ""
echo "🚀 To test installation:"
echo "   sudo apt install ./${APP_NAME}_${APP_VERSION}.deb"
echo ""
echo "🗑️  To uninstall later:"
echo "   sudo apt remove $APP_NAME"
echo ""
echo "📤 Ready for GitHub Releases!"
