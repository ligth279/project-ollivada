#!/bin/bash
# Create GitHub Release for ZeroPanic

set -e

VERSION="v1.0.0"
RELEASE_NAME="ZeroPanic v1.0.0 - Arctic Blue Edition"
APPIMAGE="ZeroPanic-1.0.0-x86_64.AppImage"

echo "🚀 Creating GitHub Release: $VERSION"
echo ""

# Check if AppImage exists
if [ ! -f "$APPIMAGE" ]; then
    echo "❌ Error: $APPIMAGE not found!"
    echo "Run ./build-appimage.sh first"
    exit 1
fi

# Check if we're in a git repository
if ! git rev-parse --git-dir > /dev/null 2>&1; then
    echo "❌ Error: Not in a git repository!"
    exit 1
fi

# Check for uncommitted changes
if ! git diff-index --quiet HEAD -- 2>/dev/null; then
    echo "⚠️  Warning: You have uncommitted changes"
    read -p "Continue anyway? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Commit any staged changes
echo "📝 Checking git status..."
git status

echo ""
read -p "📦 Commit current changes before release? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    read -p "Enter commit message: " commit_msg
    git add .
    git commit -m "$commit_msg" || echo "Nothing to commit"
fi

# Create and push tag
echo ""
echo "🏷️  Creating tag $VERSION..."
git tag -a "$VERSION" -m "$RELEASE_NAME"

echo "📤 Pushing to GitHub..."
git push origin build1
git push origin "$VERSION"

echo ""
echo "✅ Tag created and pushed!"
echo ""
echo "📋 Release Notes:"
cat << EOF

## 🛡️ ZeroPanic v1.0.0 - Arctic Blue Edition

### ✨ Features
- 🔍 **Real-time Threat Detection** - Scans installed applications against Supabase threat database
- 🎨 **Arctic Blue Theme** - Modern, clean UI with ice blue panels and cyan accents
- 💾 **SQLite Caching** - 20-hour app scan cache, 5-hour threat check cache
- 🔐 **User Authentication** - Secure login with SHA-256 password hashing
- 🖥️ **Cross-Platform** - Supports Linux (Snap, Flatpak, Apt, Desktop entries)
- ⚡ **Force Scan** - Bypass cache and scan immediately
- 📊 **Dashboard** - View threats, browse installed apps, search functionality

### 🐧 Installation (Linux)

#### AppImage (Universal - Recommended)
\`\`\`bash
# Download
wget https://github.com/YOUR_USERNAME/zeropanic/releases/download/v1.0.0/ZeroPanic-1.0.0-x86_64.AppImage

# Make executable
chmod +x ZeroPanic-1.0.0-x86_64.AppImage

# Run
./ZeroPanic-1.0.0-x86_64.AppImage
\`\`\`

### 📦 What's Included
- Java 25 runtime (no separate installation needed)
- JavaFX 21 GUI framework
- SQLite database for caching
- All application dependencies

### 🔧 Requirements
- Linux x86_64 (any modern distro)
- ~60MB disk space
- Internet connection for threat database sync

### 🔐 First Run
1. Launch the application
2. Create an account (stored locally)
3. Wait for initial app scan
4. View detected threats in the Alerts panel

### 📊 Technical Details
- **Language**: Java 25
- **GUI**: JavaFX 21
- **Database**: SQLite 3.47
- **API**: Supabase REST API
- **Size**: 54 MB

### 🐛 Known Issues
- None reported yet!

### 🤝 Contributing
Issues and pull requests welcome!

---

**Built with ❤️ by Ullivada**
EOF

echo ""
echo "🌐 Next Steps:"
echo "1. Go to: https://github.com/YOUR_USERNAME/zeropanic/releases/new?tag=$VERSION"
echo "2. Paste the release notes above"
echo "3. Upload the AppImage: $APPIMAGE"
echo "4. Click 'Publish release'"
echo ""
echo "Or install GitHub CLI to do it from terminal:"
echo "  sudo apt install gh"
echo "  gh auth login"
echo "  gh release create $VERSION $APPIMAGE --title '$RELEASE_NAME' --notes-file release-notes.md"
