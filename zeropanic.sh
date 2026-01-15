#!/bin/bash
# ZeroPanic Launcher for Linux
# This script ensures JavaFX modules are properly loaded

# Source SDKMAN if available
if [ -f "$HOME/.sdkman/bin/sdkman-init.sh" ]; then
    source "$HOME/.sdkman/bin/sdkman-init.sh"
fi

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Path to the JAR file
JAR_FILE="$SCRIPT_DIR/target/app-lister-cli-0.1.0-SNAPSHOT.jar"

# Check if JAR exists
if [ ! -f "$JAR_FILE" ]; then
    echo "Error: JAR file not found at $JAR_FILE"
    echo "Please run 'mvn clean package' first"
    exit 1
fi

# Find JavaFX modules path
JAVAFX_BASE="$HOME/.m2/repository/org/openjfx"

# Build classpath with all JavaFX JARs
JAVAFX_CP=""
for jar in \
    "$JAVAFX_BASE/javafx-base/21.0.1/javafx-base-21.0.1.jar" \
    "$JAVAFX_BASE/javafx-base/21.0.1/javafx-base-21.0.1-linux.jar" \
    "$JAVAFX_BASE/javafx-controls/21.0.1/javafx-controls-21.0.1.jar" \
    "$JAVAFX_BASE/javafx-controls/21.0.1/javafx-controls-21.0.1-linux.jar" \
    "$JAVAFX_BASE/javafx-graphics/21.0.1/javafx-graphics-21.0.1.jar" \
    "$JAVAFX_BASE/javafx-graphics/21.0.1/javafx-graphics-21.0.1-linux.jar" \
    "$JAVAFX_BASE/javafx-fxml/21.0.1/javafx-fxml-21.0.1.jar" \
    "$JAVAFX_BASE/javafx-fxml/21.0.1/javafx-fxml-21.0.1-linux.jar"; do
    if [ -f "$jar" ]; then
        JAVAFX_CP="$JAVAFX_CP:$jar"
    fi
done

if [ -z "$JAVAFX_CP" ]; then
    echo "JavaFX not found. Please run: mvn dependency:resolve"
    exit 1
fi

# Launch with full classpath
java -cp "$JAR_FILE$JAVAFX_CP" \
    com.ullivada.zeropanic.applister.javafx.ZeroPanicApp "$@"
