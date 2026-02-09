#!/bin/bash

# Скрипт для проверки файлов перед сборкой Docker образа

echo "🔍 Проверка файлов перед сборкой..."

cd /opt/integration-platform/web || exit 1

# Проверяем кодировку файлов
echo "📄 Проверка кодировки файлов..."
find src public -type f -name "*.js" -o -name "*.jsx" -o -name "*.json" -o -name "*.html" | while read file; do
    if ! file -i "$file" | grep -q "utf-8\|us-ascii"; then
        echo "⚠️  Проблема с кодировкой: $file"
        file -i "$file"
    fi
done

# Проверяем наличие необходимых файлов
echo ""
echo "📋 Проверка необходимых файлов..."
required_files=(
    "Dockerfile"
    "package.json"
    "src/index.js"
    "src/App.js"
    "public/index.html"
)

missing_files=0
for file in "${required_files[@]}"; do
    if [ ! -f "$file" ]; then
        echo "❌ Отсутствует: $file"
        missing_files=1
    else
        echo "✅ Найден: $file"
    fi
done

# Проверяем .dockerignore
echo ""
echo "📦 Проверка .dockerignore..."
if [ -f ".dockerignore" ]; then
    echo "✅ .dockerignore существует"
    if grep -q "*.md" .dockerignore; then
        echo "✅ Markdown файлы исключены"
    else
        echo "⚠️  Рекомендуется исключить *.md файлы из .dockerignore"
    fi
else
    echo "⚠️  .dockerignore не найден"
fi

# Проверяем размер node_modules (если есть)
if [ -d "node_modules" ]; then
    echo ""
    echo "📊 Размер node_modules:"
    du -sh node_modules
fi

echo ""
if [ $missing_files -eq 0 ]; then
    echo "✅ Все необходимые файлы на месте!"
    echo "🚀 Можно запускать сборку: docker compose build --no-cache car-scheme-web"
else
    echo "❌ Обнаружены отсутствующие файлы. Исправьте перед сборкой."
    exit 1
fi
