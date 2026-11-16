# faygoLover's Performance (flperf) — карта

Версия на момент написания: 1.0.9 (16.11.2026). Факты по классам и миксинам — `docs/INDEX.md`.

## 1. Что делает и чего не делает

Производительность и устойчивость сборки S5 в одном моде. Набор независимых **правок**, каждая включается
своим ключом: тёплые чанки, защита тика (сбой сущности или блока не роняет сервер — виновник замораживается),
достижения по белому списку, запрет синхронной загрузки чанков посреди тика, эллиптическая дальность
прорисовки, быстрая генерация поверхности, пределы поиска структур, выключенная книга рецептов, утечка
видеопамяти, фоновая запись на диск, малый FPS вне фокуса, брошенные метки чанков, экономия в модах MCreator.

Самостоятельный мод: от RP и других FL-модов не зависит. За этим — не сюда:
- заплатки поведения конкретных чужих модов — RP Fixes; сеть и соединение — `flnettweaks`;
- оптимизация своих модов — в самих модах, по замеру.

## 2. Как течёт работа

**Включение правки.** `Features` читает `config/flperf-mixins.properties` **до загрузки модов** (миксины решаются
раньше, чем Forge грузит конфиги) → `mixin/FlPerfMixinPlugin` применяет миксины пакета `mixin/<ключ>/` только
если `Features.on("<ключ>")` → логика правки — класс в `feature/` (или `client/`) → числа правки — в
`PerfConfig` (`config/flperf-common.toml`) и `PerfClientConfig`. Смена ключа — после перезапуска.

**Правки и где их логика:**
- `warm_chunks` → `feature/WarmChunks`; `orphan_tickets` → `feature/OrphanTickets`; `sync_loads` → `feature/Chunks`;
- `tick_guard`, `safety` → `feature/TickGuard` (заморозка виновника, `/flperf frozen`);
- `advancements`, `advancements_hide_ui` → `feature/Advancements`;
- `structures` → `feature/StructureLimits`, `StructureSpacing`; `terrablender` → `feature/SurfaceBiomes`;
- `async_save` → `feature/AsyncSave`; `despawn`, `recipebook` — только миксины;
- `mcreator`, `mcreator_advancements` → `mcreator/MCreatorTransformer` (правит байт-код чужих классов при загрузке), `SharedDefaults`;
- клиент: `distance` → `client/DistanceState`; `gpu_leak` → `client/GpuLeak`; `background_fps` → `client/BackgroundFps`.

**Команды админа.** `FlPerfCommands`: `/flperf warm`, `frozen`, `tickets`.

## 3. Карта пакетов

| Пакет | За что отвечает | С чего начать читать |
|---|---|---|
| `flperf` | вход, список правок, числа, команды | `FlPerf`, `Features` (все ключи с описанием), `PerfConfig`, `PerfClientConfig`, `FlPerfCommands` |
| `feature` | логика серверных правок | класс по имени правки |
| `client` | логика клиентских правок | `ClientEvents`, `DistanceState`, `GpuLeak`, `BackgroundFps` |
| `mcreator` | правка сгенерированного MCreator кода | `MCreatorTransformer` |
| `mixin` | плагин включения | `FlPerfMixinPlugin` |
| `mixin.<ключ>` | миксины правки: `advancements`, `async_save`, `despawn`, `orphan_tickets`, `recipebook`, `safety`, `structures`, `sync_loads`, `terrablender`, `tick_guard`; клиентские — `client.advancements_hide_ui`, `client.background_fps`, `client.distance`, `client.gpu_leak`, `client.recipebook` | имя пакета = ключ правки |

## 4. Хочу добавить… — рецепты

| Что | Где | Образец | Не забыть |
|---|---|---|---|
| новую правку | 1) ключ и описание в `Features` (`def("ключ", "описание")`); 2) миксины в `mixin/<ключ>/` (клиентские — `mixin/client/<ключ>/`); 3) логика в `feature/` или `client/`; 4) миксины — в json конфигурации миксинов; 5) числа — в `PerfConfig` | `orphan_tickets` (один миксин + `feature/OrphanTickets`) | имя пакета миксинов должно совпасть с ключом — по нему `FlPerfMixinPlugin` решает, применять ли |
| число для правки | `PerfConfig` (сервер и общее) или `PerfClientConfig` | белый список `advancements_keep` | включение — только в `Features`, числа — только здесь |
| команду админа | `FlPerfCommands` | `/flperf tickets` | права 2 |
| правку чужого мода MCreator | `mcreator/MCreatorTransformer` | переменные игрока | заплатки логики отправки тех же модов — в RP Fixes, не дублировать |

## 5. Данные

Мод ничего не хранит у игрока. Тёплые чанки и замороженные виновники — состояние сервера в памяти (и метки
чанков мира, которые сам же снимает). Настройки — три файла в `config/`.

## 6. Связи

Ни от чего в семействе не зависит и никем не импортируется. Образцы решений — ServerCore, Dynamic FPS,
structureessentials (названы в описаниях классов); их код не копируется дословно.

## 7. Конфиги

- `config/flperf-mixins.properties` — включение правок (`Features`), читается до загрузки модов, меняется перезапуском;
- `config/flperf-common.toml` — `PerfConfig`: числа;
- `config/flperf-client.toml` — `PerfClientConfig`.

## 8. Сеть

Своей сети нет.

## 9. Ловушки

- **`Features` нельзя связывать с классами Minecraft:** его читает плагин миксинов до загрузки игры — обращение
  к игровому классу оттуда сломает запуск.
- **Включение правки и её числа живут в разных файлах** — так задумано (первое нужно до загрузки модов).
- **Защита тика скрывает сбой:** виновник заморожен и сервер живёт дальше, но причина остаётся — смотреть
  `/flperf frozen` и лог, а не считать, что всё в порядке.
- **Правки бьют по горячим местам ванили:** каждую проверять на сервере со сборкой, а не только компиляцией.
- На живом сервере часть правок может быть выключена ключом — сверяться с файлом сервера.

## 10. Тесты

Игровых тестов нет. `python ../_family/family.py verify "faygoLover's Performance"` — компиляция. Проверка —
стенды `Pack Audit` и замер `spark` на сервере.

## 11. Документы в docs/

| Файл | Состояние |
|---|---|
| `MAP.md`, `INDEX.md` | действуют |

Описание для игроков и админов — `README.md` в корне мода. История находок по сборке — `Pack Audit/`.
