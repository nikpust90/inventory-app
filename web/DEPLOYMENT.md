# Инструкция по развертыванию веб-приложения на сервере

## Подключение к серверу

### Через SSH (Windows - Putty)

1. Скачайте и установите Putty: https://www.chiark.greenend.org.uk/
2. Откройте Putty
3. Введите данные:
   - Host Name: `<server-ip>`
   - Port: `22`
   - Connection type: `SSH`
4. Нажмите "Open"
5. Введите логин: `root`
6. Введите пароль: `<ssh-password>`

### Через командную строку (Linux/Mac/Windows с OpenSSH)

```bash
ssh root@<server-ip>
# Пароль: <ssh-password>
```

## Развертывание приложения

### Вариант 1: Добавление в существующий docker-compose.yml

Если у вас уже есть docker-compose.yml в `/opt/integration-platform`, добавьте туда сервис:

```yaml
services:
  # ... существующие сервисы ...
  
  car-scheme-web:
    build:
      context: ./web
      dockerfile: Dockerfile
    container_name: car-scheme-web
    ports:
      - "3001:80"
    restart: unless-stopped
```

### Вариант 2: Отдельный контейнер

1. **Подключитесь к серверу** (см. выше)

2. **Перейдите в директорию проекта:**
```bash
cd /opt/integration-platform
```

3. **Создайте директорию для веб-приложения:**
```bash
mkdir -p web
```

4. **Загрузите файлы проекта на сервер**

   **Вариант A: Через SCP (из Windows можно использовать WinSCP или FileZilla)**
   
   - Используйте WinSCP или FileZilla
   - Подключитесь к серверу с теми же данными
   - Загрузите всю папку `web` в `/opt/integration-platform/web`

   **Вариант B: Через Git (если проект в репозитории)**
   ```bash
   cd /opt/integration-platform
   git clone <ваш-репозиторий> temp
   cp -r temp/web ./
   rm -rf temp
   ```

   **Вариант C: Через прямую загрузку файлов**
   ```bash
   # На сервере создайте файлы вручную или используйте nano/vim
   cd /opt/integration-platform/web
   ```

5. **Проверьте структуру файлов:**
```bash
cd /opt/integration-platform/web
ls -la
# Должны быть: Dockerfile, docker-compose.yml, package.json, src/, public/
```

6. **Соберите и запустите контейнер:**

   **Если используете отдельный docker-compose.yml в папке web:**
   ```bash
   cd /opt/integration-platform/web
   docker compose up -d --build
   ```

   **Если добавили в основной docker-compose.yml:**
   ```bash
   cd /opt/integration-platform
   docker compose up -d --build car-scheme-web
   ```

7. **Проверьте статус контейнера:**
```bash
docker ps | grep car-scheme-web
```

8. **Проверьте логи (если есть проблемы):**
```bash
docker logs car-scheme-web
```

## Доступ к приложению

После успешного развертывания приложение будет доступно по адресу:
- **http://<server-ip>:3001**

## Обновление приложения

Для обновления приложения после изменений:

```bash
cd /opt/integration-platform/web
# Загрузите обновленные файлы
docker compose up -d --build
```

## Просмотр контейнеров через Portainer

Вы можете просматривать и управлять контейнерами через веб-интерфейс:
- URL: http://<server-ip>:9000
- Логин: `admin`
- Пароль: `<portainer-password>`

## Устранение проблем

### Контейнер не запускается

1. Проверьте логи:
```bash
docker logs car-scheme-web
```

2. Проверьте, не занят ли порт 3001:
```bash
netstat -tulpn | grep 3001
# или
ss -tulpn | grep 3001
```

3. Если порт занят, измените порт в docker-compose.yml:
```yaml
ports:
  - "3002:80"  # Вместо 3001
```

### Проблемы с CORS при обращении к API

Если возникают проблемы с CORS при обращении к API `http://<server-ip>:8000`, 
может потребоваться настроить прокси в nginx или добавить заголовки CORS на стороне API.

### Пересборка с нуля

Если нужно пересобрать образ с нуля:
```bash
cd /opt/integration-platform/web
docker compose down
docker rmi car-scheme-web  # или имя образа
docker compose up -d --build
```

## Структура файлов на сервере

```
/opt/integration-platform/
├── web/                    # Веб-приложение
│   ├── Dockerfile
│   ├── docker-compose.yml
│   ├── package.json
│   ├── src/
│   │   ├── App.js
│   │   ├── components/
│   │   │   ├── CarScheme.js
│   │   │   └── CarScheme.css
│   │   ├── services/
│   │   │   └── carApi.js
│   │   └── index.js
│   └── public/
│       └── index.html
└── docker-compose.yml      # Основной compose файл (если есть)
```
