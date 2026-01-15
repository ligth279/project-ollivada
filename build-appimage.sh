#!/bin/bash
# Build ZeroPanic AppImage for Linux

set -e

echo "🔨 Building ZeroPanic AppImage..."

# Variables
APP_NAME="ZeroPanic"
APP_VERSION="1.0.0"
MAIN_CLASS="com.ullivada.zeropanic.applister.javafx.ZeroPanicApp"
JAVA_HOME="${JAVA_HOME:-$HOME/.sdkman/candidates/java/25-open}"
JAVAFX_MODS="$HOME/.m2/repository/org/openjfx"

# Clean previous builds
echo "🧹 Cleaning previous builds..."
rm -rf build/appimage
mkdir -p build/appimage

# Build JAR if not exists
if [ ! -f "target/app-lister-cli-0.1.0-SNAPSHOT.jar" ]; then
    echo "📦 Building JAR..."
    mvn clean package -q
fi

# Create AppImage using jpackage
echo "📦 Creating AppImage with jpackage..."
$JAVA_HOME/bin/jpackage \
    --type app-image \
    --name "$APP_NAME" \
    --app-version "$APP_VERSION" \
    --input target \
    --main-jar app-lister-cli-0.1.0-SNAPSHOT.jar \
    --main-class "$MAIN_CLASS" \
    --dest build/appimage \
    --module-path "$JAVAFX_MODS/javafx-controls/21.0.1:$JAVAFX_MODS/javafx-graphics/21.0.1:$JAVAFX_MODS/javafx-base/21.0.1:$JAVAFX_MODS/javafx-fxml/21.0.1" \
    --add-modules javafx.controls,javafx.fxml,java.sql,java.net.http \
    --java-options '--enable-native-access=javafx.graphics,ALL-UNNAMED' \
    --description "Cybersecurity threat detection for installed applications" \
    --vendor "Ullivada" \
    --copyright "2026 Ullivada" \
    --verbose

# Package as AppImage using appimagetool
echo "📦 Creating portable AppImage..."
cd build/appimage

# Download appimagetool if not exists
if [ ! -f "appimagetool" ]; then
    echo "⬇️  Downloading appimagetool..."
    wget -q "https://github.com/AppImage/AppImageKit/releases/download/continuous/appimagetool-x86_64.AppImage" -O appimagetool
    chmod +x appimagetool
fi

# Create AppDir structure
APP_DIR="$APP_NAME.AppDir"
rm -rf "$APP_DIR"
mkdir -p "$APP_DIR"

# Copy jpackage output to AppDir
cp -r "$APP_NAME"/* "$APP_DIR/"

# Create AppRun script
cat > "$APP_DIR/AppRun" << 'APPRUN_EOF'
#!/bin/bash
SELF=$(readlink -f "$0")
HERE=${SELF%/*}
export PATH="${HERE}/bin:${PATH}"
export LD_LIBRARY_PATH="${HERE}/lib:${LD_LIBRARY_PATH}"
exec "${HERE}/bin/ZeroPanic" "$@"
APPRUN_EOF
chmod +x "$APP_DIR/AppRun"

# Create .desktop file
cat > "$APP_DIR/zeropanic.desktop" << 'DESKTOP_EOF'
[Desktop Entry]
Name=ZeroPanic
Comment=Cybersecurity threat detection
Exec=AppRun
Icon=zeropanic
Type=Application
Categories=Security;Utility;
Terminal=false
DESKTOP_EOF

# Create simple icon (shield emoji as text-based icon)
cat > "$APP_DIR/zeropanic.svg" << 'SVG_EOF'
<?xml version="1.0" encoding="UTF-8"?>
<svg width="256" height="256" xmlns="http://www.w3.org/2000/svg">
  <defs>
    <linearGradient id="grad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" style="stop-color:#00ACC1;stop-opacity:1" />
      <stop offset="100%" style="stop-color:#26A69A;stop-opacity:1" />
    </linearGradient>
  </defs>
  <rect width="256" height="256" rx="40" fill="url(#grad)"/>
  <path d="M128 40 L200 80 L200 140 C200 180 170 210 128 220 C86 210 56 180 56 140 L56 80 Z" 
        fill="white" opacity="0.95"/>
  <path d="M100 130 L115 145 L155 105" 
        stroke="#00ACC1" stroke-width="12" stroke-linecap="round" 
        stroke-linejoin="round" fill="none"/>
</svg>
SVG_EOF

# Build AppImage
echo "🎁 Building final AppImage..."
./appimagetool "$APP_DIR" "$APP_NAME-$APP_VERSION-x86_64.AppImage"

cd ../..
mv "build/appimage/$APP_NAME-$APP_VERSION-x86_64.AppImage" .

echo ""
echo "✅ AppImage created successfully!"
echo "📦 File: $APP_NAME-$APP_VERSION-x86_64.AppImage"
echo "📏 Size: $(du -h "$APP_NAME-$APP_VERSION-x86_64.AppImage" | cut -f1)"
echo ""
echo "🚀 To test it:"
echo "   chmod +x $APP_NAME-$APP_VERSION-x86_64.AppImage"
echo "   ./$APP_NAME-$APP_VERSION-x86_64.AppImage"
echo ""
echo "📤 Ready for GitHub Releases!"
