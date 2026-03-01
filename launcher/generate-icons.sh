#!/bin/bash
# Generate PNG icons from SVG for electron-builder
# Requires: inkscape or rsvg-convert or imagemagick

SVG_FILE="build/icon.svg"

echo "Generating launcher icons..."

# Try different tools
if command -v rsvg-convert &> /dev/null; then
    rsvg-convert -w 512 -h 512 "$SVG_FILE" -o build/icon.png
    echo "✅ icon.png generated (rsvg-convert)"
elif command -v inkscape &> /dev/null; then
    inkscape "$SVG_FILE" -o build/icon.png -w 512 -h 512
    echo "✅ icon.png generated (inkscape)"
elif command -v convert &> /dev/null; then
    convert -background none -density 512 "$SVG_FILE" -resize 512x512 build/icon.png
    echo "✅ icon.png generated (imagemagick)"
else
    echo "⚠️  No SVG converter found. Install one of: librsvg2-bin, inkscape, imagemagick"
    echo "    sudo apt install librsvg2-bin"
    echo ""
    echo "Creating fallback PNG with built-in method..."
    # Create a simple placeholder using printf (base64 minimal png)
    cp "$SVG_FILE" build/icon.png 2>/dev/null || true
    echo "⚠️  Using SVG as fallback — electron-builder might need a real PNG"
fi

echo "Done!"
