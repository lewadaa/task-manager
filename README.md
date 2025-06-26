# Task Manager

Spring Boot REST API для управления задачами с поддержкой ролей `USER` и `ADMIN`, аутентификацией через JWT, логированием, валидацией и интеграционными тестами.

---

## Технологии и стек

- Java 17
- Spring Boot 3
  - Spring Security (JWT)
  - Spring Data JPA (PostgreSQL)
  - Spring Validation
- Flyway (миграции БД)
- Redis (хранение refresh токенов)
- Lombok
- MapStruct
- JUnit 5, Testcontainers
- Logback (настроен цветной вывод и логирование в файл)
- Swagger / OpenAPI 3
- Docker, Docker Compose

---

## Функциональность

- Аутентификация пользователей с ролями USER и ADMIN (инициализация и добавление админом)
- Выдача JWT (access + refresh токены)
- CRUD-операции над задачами
- Фильтрация задач по статусу
- Админ-доступ: просмотр и управление всеми пользователями
- Глобальная обработка ошибок (`@RestControllerAdvice`)
- Интеграционные тесты (Testcontainers)
- Поддержка логирования в файл и цветного вывода в консоль

---

##  Запуск проекта

# Переменные окружения

Проект использует переменные окружения, значения которых по умолчанию заданы в application.yml:

Переменная |          Назначение            | Значение по умолчанию
-----------------------------------------------
DB_USER    | Имя пользователя PostgreSQL    | postgres
DB_PASS    | Пароль от PostgreSQL           | root
JWT_SECRET | Secret для подписи JWT токенов | dev-secret (заменить!)

!!! Важно: JWT_SECRET обязательно должен быть задан. Без него приложение не запустится. Не используйте dev-secret в продакшене.

# Создание .env файла
Создайте файл .env рядом с docker-compose.yml вручную или выполните:

##bash##
cp .env.example .env

Затем укажите значения переменных в .env:

JWT_SECRET=your-secure-secret    # обязательно свое значение!
DB_USER=postgres                 # при необходимости - свое значение
DB_PASS=root                     # при необходимости - свое значение

1. Через Docker Compose

##bash##
docker-compose up --build

2. Локально (без Docker)
Установите PostgreSQL и Redis

При необходимости обновите application.yml

Запустите main() в TaskManagerApplication

---

## Тестирование
Интеграционные тесты запускаются автоматически и используют Testcontainers (Postgres + Redis):

./mvnw test

# Swagger UI
Доступен по адресу: http://localhost:8080/swagger-ui.html

---

## Логирование
Логи:

В консоль - с цветным форматированием

В файлы:

logs/my-app-info.log

logs/my-app-error.log

Настройки — в logback-spring.xml.
