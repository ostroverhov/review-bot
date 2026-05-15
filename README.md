# review-bot

Сервис на **Spring Boot** принимает вебхуки **GitLab** (merge request и комментарии), пишет в **Slack** в заданный канал и в **тред** к сообщению о MR. Связка MR ↔ корневое сообщение Slack и маппинг «проект GitLab → канал Slack» хранятся в **PostgreSQL**.

## Требования

- **JDK 17+** (сборка через Gradle toolchain подтянет 17 при необходимости).
- Для запуска `./gradlew` на машине с **Java 25** используйте **Gradle 8.14+** или задайте `JAVA_HOME` на **JDK 22** (Gradle 8.11 не поддерживает Java 25 как JVM для самого Gradle).

## Быстрый старт (локально)

1. Поднять Postgres:

   ```bash
   docker compose up -d
   ```

2. Скопировать и заполнить переменные окружения (пример):

   ```bash
   export GITLAB_WEBHOOK_SECRET='секрет-из-настроек-webhook-gitlab'
   export SLACK_BOT_TOKEN='xoxb-...'
   ```

3. Добавить строки в таблицу `team_rule` (см. ниже).

4. Запуск:

   ```bash
   ./gradlew bootRun
   ```

По умолчанию приложение слушает порт **8080**.

## Конфигурация команд (таблица `team_rule`)

| Колонка | Описание |
|---------|----------|
| `gitlab_project` | `path_with_namespace` проекта в GitLab (как в URL репозитория). |
| `slack_channel_id` | ID канала Slack (канал → сведения → «Channel ID»). |

Пример:

```sql
INSERT INTO team_rule (gitlab_project, slack_channel_id)
VALUES ('identity/qa/postman-collection-temporal', 'C01234567890');
```

Один и тот же `gitlab_project` можно указать в **нескольких** строках с разными каналами — для каждого канала будет своё корневое сообщение и свой тред. Пара `(gitlab_project, slack_channel_id)` уникальна.

Изменения в таблице подхватываются **автоматически по расписанию** (Spring `@Scheduled`, по умолчанию раз в час). При старте приложения правила тоже загружаются из БД.

## GitLab webhook

1. В проекте (или группе): **Settings → Webhooks**.
2. **URL**: `https://<ваш-хост>/webhook/gitlab`
3. **Secret token**: тот же, что в `GITLAB_WEBHOOK_SECRET`.
4. Включить триггеры: **Merge request events**, **Comments** (комментарии к MR и обсуждения).
5. SSL и остальное — по политике вашей инфраструктуры.

Заголовок `X-Gitlab-Event` должен доходить до сервиса (обратный прокси не должен его срезать).

## Slack приложение

1. [api.slack.com/apps](https://api.slack.com/apps) → Create app → **From scratch**.
2. **OAuth & Permissions** → Bot Token Scopes:
   - `chat:write`
   - `reactions:write`
3. Установить приложение в workspace, скопировать **Bot User OAuth Token** (`xoxb-...`) в `SLACK_BOT_TOKEN`.
4. Пригласить бота в нужные каналы (`/invite @YourBot`).

Реакция при закрытии/мерже задаётся переменной `REVIEW_BOT_CLOSE_REACTION` (имя emoji **без** двоеточий), по умолчанию `white_check_mark`. Для кастомного emoji из workspace используйте его короткое имя (например `done`).

## Переменные окружения

| Переменная | Назначение |
|------------|------------|
| `GITLAB_WEBHOOK_SECRET` | Секрет вебхука GitLab (`X-Gitlab-Token`). Без него вебхуки отклоняются. |
| `SLACK_BOT_TOKEN` | OAuth токен бота Slack. |
| `SPRING_DATASOURCE_URL` / `USERNAME` / `PASSWORD` | JDBC к Postgres (см. `application.yml`). |
| `REVIEW_BOT_CLOSE_REACTION` | Имя emoji для реакции на корневом сообщении при close/merge. |
| `REVIEW_BOT_TEAM_RULES_REFRESH_INTERVAL_MS` | Интервал перечитывания `team_rule` из БД, мс (по умолчанию `3600000` — 1 час). |

## Поведение

| Событие GitLab | Slack |
|----------------|--------|
| MR открыт / переоткрыт (`action`: `open`, `reopen`) | Сообщение в канал(ы) по маппингу; сохраняется `ts` для тредов |
| Комментарий к MR (`Note Hook`, `noteable_type`: `MergeRequest`) | Сообщение в тред(ы) к соответствующим корневым сообщениям |
| MR закрыт или смержен (`close`, `merge`) | Сообщение в тред + реакция на корневое сообщение |

Обработка вебхука **синхронная** (ответ GitLab после вызовов Slack и БД).

## Сборка и тесты

```bash
./gradlew build
```

Тесты используют in-memory **H2** и профиль `test` (`src/test/resources/application-test.yml`).
