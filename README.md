# Courier Management System

Инструкции по подготовке окружения и запуску серверного приложения на Kotlin + Spring Boot.

## Предварительные требования
- **JDK 21** — установите и убедитесь, что `java -version` выводит 21.x. При необходимости обновите переменную окружения `JAVA_HOME`.
- **Gradle Wrapper** — репозиторий содержит `./gradlew` (`gradlew.bat` для Windows), устанавливать Gradle отдельно не нужно.
- **PostgreSQL** (необязательно) — требуется только для запуска в профиле `prod`. Для разработки по умолчанию используется встроенная база H2.

## Быстрый запуск (встроенная H2)
1. Установите JDK 21 и выполните `./gradlew --version`, чтобы убедиться, что всё готово.
2. Запустите приложение:
   ```bash
   ./gradlew bootRun
   ```
   На Windows используйте `gradlew.bat bootRun`.
3. После старта сервис доступен на `http://localhost:8080`. Встроенная база H2 создаётся в памяти, миграции Liquibase применяются автоматически.

### Что включено по умолчанию
- Профиль по умолчанию (`application.yml`) использует H2 и Liquibase (`classpath:db/changelog/db.changelog-master.xml`).
- Консоль H2 доступна на `http://localhost:8080/h2-console` (логин `sa`, пароль пустой).
- Swagger UI расположен на `http://localhost:8080/swagger-ui.html`.

## Запуск с PostgreSQL (профиль `prod`)
1. Установите PostgreSQL 14+ и создайте базу:
   ```sql
   CREATE DATABASE courier_management;
   CREATE USER courier_user WITH ENCRYPTED PASSWORD 'courier_password';
   GRANT ALL PRIVILEGES ON DATABASE courier_management TO courier_user;
   ```
2. При необходимости измените креды и URL в `src/main/resources/application-prod.yml`.
3. Установите переменные окружения (если используете свои значения):
   ```bash
   export DB_USERNAME=courier_user
   export DB_PASSWORD=courier_password
   ```
4. Запустите приложение с профилем `prod`:
   ```bash
   SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun
   ```
   Для Windows PowerShell:
   ```powershell
   $env:SPRING_PROFILES_ACTIVE = "prod"
   .\gradlew.bat bootRun
   ```
5. Liquibase выполнит миграции в PostgreSQL автоматически.

## Тесты и сборка
- Запуск unit-тестов:
  ```bash
  ./gradlew test
  ```
- Полная сборка артефактов:
  ```bash
  ./gradlew clean build
  ```

## Полезные ссылки
- Основной класс приложения: `com.example.couriermanagement.CourierManagementSystemApplicationKt`.
- Настройки профилей: `src/main/resources/application.yml`, `src/main/resources/application-prod.yml`.
- Миграции Liquibase: `src/main/resources/db/changelog/`.
