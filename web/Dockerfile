# Многоэтапная сборка для оптимизации размера образа
# Этап 1: Сборка приложения
FROM node:18-alpine AS build

# Устанавливаем рабочую директорию
WORKDIR /app

# Устанавливаем переменные окружения для корректной работы с кодировкой
ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8

# Копируем package.json и package-lock.json (если есть)
COPY package*.json ./

# Устанавливаем зависимости
RUN npm install

# Копируем только необходимые файлы для сборки
COPY public/ ./public/
COPY src/ ./src/

# Собираем приложение для production
RUN npm run build

# Этап 2: Production образ с nginx
FROM nginx:alpine

# Копируем собранное приложение из этапа сборки
COPY --from=build /app/build /usr/share/nginx/html

# Копируем конфигурацию nginx с проксированием API
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Открываем порт 80
EXPOSE 80

# Запускаем nginx
CMD ["nginx", "-g", "daemon off;"]
