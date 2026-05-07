# Final product update verification

Дата проверки: 2026-05-07

## Проверенные изменения

- Виртуальный кошелёк пользователя: `user_wallets`, `wallet_transactions`, стартовый баланс 1000 монет.
- Виртуальная ответственность в `goal_commitments`: залог, штраф за пропуск, статус денег, сумма списаний.
- Списание штрафа при пропущенном дне через `GoalCommitmentService.processPreviousDay`.
- Отображение баланса в sidebar, Dashboard, Motivation, Profile и чате-мотиваторе.
- История виртуальных операций отображается в профиле и загружается из `/api/wallet/transactions`.
- Вертикальная мотивационная лента сохранена как Reels/Shorts layout с `scroll-snap`.
- Действия `Неинтересно` и `Пожаловаться` остались серверными и проверяются тестами.
- Мотиватор получает wallet context и умеет отвечать на вопрос про штрафы/монеты.

## Команды

### Backend tests

```bash
docker run --rm -v "$PWD":/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -q test
```

Результат: успешно. Старые integration tests и новые проверки wallet/penalty прошли.

### Backend formatting

```bash
docker run --rm -v "$PWD":/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -q spotless:check
```

Результат: успешно после `spotless:apply`.

### Frontend tests

```bash
docker run --rm -v "$PWD":/app -w /app node:22-alpine sh -lc 'npm install >/tmp/npm-install.log && npm run test:run'
```

Результат: успешно, 7 test files, 17 tests passed. Есть предупреждения React Router future flags и `act(...)` warning в существующем тесте MotivationPage, тесты не падают.

### Frontend lint

```bash
docker run --rm -v "$PWD":/app -w /app node:22-alpine sh -lc 'npm install >/tmp/npm-install.log && npm run lint'
```

Результат: успешно.

### Frontend build

```bash
docker run --rm -v "$PWD":/app -w /app node:22-alpine sh -lc 'npm install >/tmp/npm-install.log && npm run build'
```

Результат: успешно, Vite production build собран.

### Docker Compose

```bash
docker compose up --build -d
```

Результат: успешно. Контейнеры `postgres`, `backend`, `frontend` поднялись. Backend применил Flyway V6.

### Smoke API

```bash
curl -X POST http://localhost:18080/api/auth/register ...
curl http://localhost:18080/api/wallet -H "Authorization: Bearer <token>"
```

Результат: успешно. Новый пользователь получил wallet с балансом 1000 монет и статусом `ACTIVE`.

## TODO

- Для презентационной демо-базы стоит создать несколько целей с включенной виртуальной ответственностью, чтобы в UI сразу были видны штрафы и transaction history.
- Можно отдельно добавить фильтры по типам wallet-транзакций, сейчас профиль показывает последние операции компактным списком.
