#!/bin/bash

# Скрипт для быстрого развертывания веб-приложения на сервере
# Использование: ./deploy.sh
# 
# ВАРИАНТ 1: Если используете отдельный docker-compose.yml в папке web
# ВАРИАНТ 2: Используйте add-to-existing-compose.sh для добавления в основной compose

echo "🚀 Начинаем развертывание веб-приложения для схем автомобилей..."

# Проверяем, что мы в правильной директории
if [ ! -f "Dockerfile" ]; then
    echo "❌ Ошибка: Dockerfile не найден. Убедитесь, что вы находитесь в директории web/"
    exit 1
fi

# Определяем, какой compose файл использовать
COMPOSE_FILE="docker-compose.yml"
PROJECT_NAME=""

# Проверяем, есть ли standalone версия
if [ -f "docker-compose.standalone.yml" ]; then
    read -p "Использовать отдельный compose файл? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        COMPOSE_FILE="docker-compose.standalone.yml"
        PROJECT_NAME="-p car-scheme-web"
        echo "📦 Используем изолированный проект"
    fi
fi

# Останавливаем существующий контейнер (если есть)
echo "🛑 Останавливаем существующий контейнер..."
if [ -n "$PROJECT_NAME" ]; then
    docker compose -f "$COMPOSE_FILE" $PROJECT_NAME down 2>/dev/null || true
else
    docker compose -f "$COMPOSE_FILE" down 2>/dev/null || true
fi

# Собираем и запускаем новый контейнер
echo "🔨 Собираем образ..."
if [ -n "$PROJECT_NAME" ]; then
    docker compose -f "$COMPOSE_FILE" $PROJECT_NAME build --no-cache
else
    docker compose -f "$COMPOSE_FILE" build --no-cache
fi

echo "▶️  Запускаем контейнер..."
if [ -n "$PROJECT_NAME" ]; then
    docker compose -f "$COMPOSE_FILE" $PROJECT_NAME up -d
else
    docker compose -f "$COMPOSE_FILE" up -d
fi

# Ждем немного, чтобы контейнер запустился
sleep 3

# Проверяем статус
echo "📊 Проверяем статус контейнера..."
docker ps | grep car-scheme-web

if [ $? -eq 0 ]; then
    echo "✅ Контейнер успешно запущен!"
    echo "🌐 Приложение доступно по адресу: http://45.155.207.231:3001"
    echo "📋 Для просмотра логов используйте: docker logs car-scheme-web"
else
    echo "❌ Ошибка: Контейнер не запущен. Проверьте логи: docker logs car-scheme-web"
    exit 1
fi
