#!/bin/bash

set -e

EXECUTABLE_FILE="public-declarations-executable"
LIBS_DIR="./build/libs"
JAR_FILE=$(find "$LIBS_DIR" -maxdepth 1 -name "*.jar" | head -n 1)


if [[ ! -f "$JAR_FILE" ]]; then
  echo "JAR file not found."
  echo "Building..."
  ./gradlew jar
  JAR_FILE=$(find "$LIBS_DIR" -maxdepth 1 -name "*.jar" | head -n 1)
  if [[ -z "$JAR_FILE" ]]; then
    echo "JAR file could not be created. Check gradle configuration."
    exit 1
  fi
fi

echo '#!/usr/bin/java -jar' | cat - "$JAR_FILE" > "$EXECUTABLE_FILE"

chmod +x "$EXECUTABLE_FILE"

echo "Executable file created: $EXECUTABLE_FILE"
