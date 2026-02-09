# Исправление проблемы с кодировкой UTF-8

Если вы получили ошибку:
```
failed to solve: Internal: rpc error: code = Internal desc = grpc: failed to unmarshal the received message: string field contains invalid UTF-8
```

## Решение:

1. **Убедитесь, что файлы загружены с правильной кодировкой:**
```bash
cd /opt/integration-platform/web
file -i src/*.js src/**/*.js
# Все файлы должны быть в UTF-8
```

2. **Проверьте .dockerignore:**
Убедитесь, что файлы с русскими символами (документация) исключены из контекста сборки.

3. **Очистите кэш Docker и пересоберите:**
```bash
cd /opt/integration-platform
docker compose build --no-cache car-scheme-web
```

4. **Если проблема сохраняется, пересоздайте файлы на сервере:**
```bash
# Удалите проблемные файлы
cd /opt/integration-platform/web
rm -rf node_modules build

# Пересоберите
docker compose build --no-cache car-scheme-web
```
