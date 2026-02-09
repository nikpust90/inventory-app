#!/bin/bash

# Скрипт для добавления веб-приложения в существующий docker-compose.yml
# Безопасно - существующие контейнеры не пересоберутся

echo "🔧 Добавляем веб-приложение в существующий docker-compose.yml..."

COMPOSE_FILE="/opt/integration-platform/docker-compose.yml"
BACKUP_FILE="/opt/integration-platform/docker-compose.yml.backup"

# Проверяем существование файла
if [ ! -f "$COMPOSE_FILE" ]; then
    echo "❌ Ошибка: $COMPOSE_FILE не найден!"
    exit 1
fi

# Создаем резервную копию
echo "💾 Создаем резервную копию..."
cp "$COMPOSE_FILE" "$BACKUP_FILE"
echo "✅ Резервная копия создана: $BACKUP_FILE"

# Проверяем, не добавлен ли уже сервис
if grep -q "car-scheme-web:" "$COMPOSE_FILE"; then
    echo "⚠️  Сервис car-scheme-web уже существует в docker-compose.yml"
    echo "Пропускаем добавление..."
else
    echo "➕ Добавляем сервис car-scheme-web..."
    
    # Создаем временный файл с новым сервисом
    cat > /tmp/car-scheme-service.yml << 'EOF'
  car-scheme-web:
    build: ./web
    container_name: car-scheme-web
    ports:
      - "3001:80"
    restart: unless-stopped
EOF

    # Вставляем перед volumes:
    if grep -q "^volumes:" "$COMPOSE_FILE"; then
        # Используем sed для вставки перед volumes:
        sed -i '/^volumes:/i \  car-scheme-web:\n    build: ./web\n    container_name: car-scheme-web\n    ports:\n      - "3001:80"\n    restart: unless-stopped\n' "$COMPOSE_FILE"
        echo "✅ Сервис добавлен в docker-compose.yml"
    else
        echo "⚠️  Не найден раздел volumes: в docker-compose.yml"
        echo "Добавьте вручную в конец файла перед volumes:"
        cat /tmp/car-scheme-service.yml
    fi
    
    rm /tmp/car-scheme-service.yml
fi

# Переходим в директорию проекта
cd /opt/integration-platform

# Запускаем только новый сервис (существующие не пересоберутся!)
echo "🚀 Запускаем только новый сервис (существующие контейнеры не затронутся)..."
docker compose up -d --build car-scheme-web

if [ $? -eq 0 ]; then
    echo "✅ Веб-приложение успешно запущено!"
    echo "🌐 Доступно по адресу: http://45.155.207.231:3001"
    echo ""
    echo "📋 Существующие контейнеры (postgres, backend, bot, portainer) не были затронуты!"
else
    echo "❌ Ошибка при запуске. Проверьте логи: docker logs car-scheme-web"
    echo "💾 Для восстановления используйте резервную копию: $BACKUP_FILE"
    exit 1
fi
