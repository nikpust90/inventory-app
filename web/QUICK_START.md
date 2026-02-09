# Быстрый старт - Развертывание на сервере

## Шаг 1: Подключение к серверу

### Windows (Putty):
1. Скачайте Putty: https://www.chiark.greenend.org.uk/
2. Подключитесь:
   - Host: `<server-ip>`
   - Port: `22`
   - User: `root`
   - Password: `<ssh-password>`

### Linux/Mac:
```bash
ssh root@<server-ip>
# Пароль: <ssh-password>
```

## Шаг 2: Загрузка файлов на сервер

### Вариант A: WinSCP (Windows)
1. Скачайте WinSCP: https://winscp.net/
2. Подключитесь с теми же данными
3. Скопируйте папку `web` в `/opt/integration-platform/`

### Вариант B: SCP (командная строка)
```bash
# Из локальной машины (Linux/Mac)
scp -r web root@<server-ip>:/opt/integration-platform/
```

### Вариант C: Вручную через nano/vim
Создайте файлы на сервере вручную, скопировав содержимое из проекта.

## Шаг 3: Развертывание

```bash
# Переходим в директорию проекта
cd /opt/integration-platform/web

# Делаем скрипт исполняемым
chmod +x deploy.sh

# Запускаем развертывание
./deploy.sh
```

Или вручную:
```bash
cd /opt/integration-platform/web
docker compose up -d --build
```

## Шаг 4: Проверка

1. Проверьте статус контейнера:
```bash
docker ps | grep car-scheme-web
```

2. Проверьте логи (если нужно):
```bash
docker logs car-scheme-web
```

3. Откройте в браузере:
   - **http://<server-ip>:3001**

## Обновление

После изменений в коде:
```bash
cd /opt/integration-platform/web
docker compose up -d --build
```

## Проблемы?

- **Порт занят?** Измените порт в `docker-compose.yml` (например, на 3002)
- **Контейнер не запускается?** Проверьте логи: `docker logs car-scheme-web`
- **Нужна помощь?** Используйте Portainer: http://<server-ip>:9000
