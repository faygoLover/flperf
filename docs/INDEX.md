# faygoLover's Performance — указатель кода

Создано `python _family/tools/mapgen.py` 15.11.2026, версия 1.0.9-1.20.1-forge, коммит `a51e8c8`.
**Руками не править**: пересоздаётся скриптом. Смысл, потоки работы и рецепты — в `docs/MAP.md`.
Разбор по шаблонам, без компиляции: необычно оформленная регистрация может отсутствовать в списках.

Всего: 67 файлов, 3080 строк Java.

## 1. Пакеты

| Пакет | Файлов | Строк |
|---|---:|---:|
| `faygolover.flperf` | 5 | 312 |
| `faygolover.flperf.client` | 4 | 220 |
| `faygolover.flperf.feature` | 9 | 842 |
| `faygolover.flperf.mcreator` | 2 | 215 |
| `faygolover.flperf.mixin` | 1 | 62 |
| `faygolover.flperf.mixin.advancements` | 3 | 96 |
| `faygolover.flperf.mixin.async_save` | 2 | 49 |
| `faygolover.flperf.mixin.client.advancements_hide_ui` | 1 | 19 |
| `faygolover.flperf.mixin.client.background_fps` | 1 | 17 |
| `faygolover.flperf.mixin.client.distance` | 2 | 45 |
| `faygolover.flperf.mixin.client.gpu_leak` | 1 | 40 |
| `faygolover.flperf.mixin.client.recipebook` | 2 | 29 |
| `faygolover.flperf.mixin.despawn` | 2 | 75 |
| `faygolover.flperf.mixin.orphan_tickets` | 1 | 16 |
| `faygolover.flperf.mixin.recipebook` | 1 | 27 |
| `faygolover.flperf.mixin.safety` | 5 | 167 |
| `faygolover.flperf.mixin.structures` | 7 | 264 |
| `faygolover.flperf.mixin.sync_loads` | 8 | 227 |
| `faygolover.flperf.mixin.terrablender` | 5 | 183 |
| `faygolover.flperf.mixin.tick_guard` | 5 | 175 |

## 2. Классы

### `faygolover.flperf`
- `Features`, 74 — Включение правок: config/flperf-mixins.properties.
- `FlPerf`, 52 — faygoLover's Performance: правки производительности и стабильности сборки S5.
- `FlPerfCommands`, 73 — /flperf warm [here|clear], /flperf frozen [resume|kill ], /flperf tickets — для админов (op 2).
- `PerfClientConfig`, 33 — Клиентские настройки: config/flperf-client.toml.
- `PerfConfig`, 80 — Числовые настройки: config/flperf-common.toml.

### `faygolover.flperf.client`
- `BackgroundFps`, 62 — Кадры и звук, когда игра не в фокусе (по образцу Dynamic FPS, его значения по умолчанию): мышь над окном — до 60 кадров, окно не в фокусе — 1 кадр в секунду и звук 25%, свёрнуто — кадры не рисуются и звук 0.
- `ClientEvents`, 82 — Всё клиентское: дальность, видеопамять, кнопки книги рецептов и достижений, встроенный ресурспак атласа.
- `DistanceState`, 41 — Дальность эллипсом: расстояние считается с растяжкой — по вертикали в vertical_scale раз, поперёк взгляда в side_scale раз.
- `GpuLeak`, 35 — Очередь буферов на удаление: заполняется Cleaner-потоком, разбирается в тике клиента на потоке отрисовки.

### `faygolover.flperf.feature`
- `Advancements`, 52 — Белый список достижений: id или маска «namespace:path*»; родители нужных достижений сохраняются автоматически.
- `AsyncSave`, 71 — Один фоновый поток записи: задачи идут по порядку.
- `Chunks`, 29 — «Чанк уже загружен?» — без загрузки (по образцу ServerCore).
- `OrphanTickets`, 173 — Брошенные метки чанков.
- `StructureLimits`, 64 — Пределы поиска структур (по образцу structureessentials).
- `StructureSpacing`, 46 — Занятые структурами клетки — на каждый генератор чанков свои (только в памяти, как в structureessentials).
- `SurfaceBiomes`, 30 — Регион генерации текущего потока (на время buildSurface) и постоянные условия для отсечённых правил.
- `TickGuard`, 140 — Сбой в тике не роняет сервер: виновник «замораживается» (не тикает), в лог — полный стек, админам — сообщение.
- `WarmChunks`, 237 — Тёплые чанки.

### `faygolover.flperf.mcreator`
- `MCreatorTransformer`, 193 — Код, который генерирует MCreator, при каждом обращении к переменным игрока делает cap.orElse(new XxxModVariables.PlayerVariables()) — новый объект на каждый вызов, много раз за тик.
- `SharedDefaults`, 22 — Один объект «переменные игрока по умолчанию» на класс; вызывается из кода модов MCreator после трансформации.

### `faygolover.flperf.mixin`
- `FlPerfMixinPlugin`, 62 — Правка = пакет mixin.

### `faygolover.flperf.mixin.advancements`
- `PlayerAdvancementsMixin`, 52 — В файле прогресса игрока остались тысячи выброшенных достижений — ваниль писала бы предупреждение на каждое при входе.
- `RobitSkinMixin`, 22 — Скины робота Mekanism «за достижение» (Allay, MekaSuit) открыты всем: их достижения выброшены белым списком, и без правки скины были бы закрыты навсегда.
- `ServerAdvancementManagerMixin`, 22 — Из всех достижений сборки (~6300) загружаются только из белого списка и их родители.

### `faygolover.flperf.mixin.async_save`
- `LevelStorageAccessMixin`, 28 — level.dat: запись во временный файл и замена — одной задачей в фоне, в том же порядке, что и ваниль.
- `SavedDataMixin`, 21 — Данные мира и модов (SavedData): тег собирается на основном потоке как обычно, на диск пишется в фоне.

### `faygolover.flperf.mixin.client.advancements_hide_ui`
- `ToastComponentMixin`, 19 — Уведомлений о достижениях нет (только если advancements_hide_ui).

### `faygolover.flperf.mixin.client.background_fps`
- `GameRendererFrameMixin`, 17 — Кадр не рисуется, если окно свёрнуто или не в фокусе и до следующего кадра по лимиту ещё рано (тики идут как обычно).

### `faygolover.flperf.mixin.client.distance`
- `EntityDistanceMixin`, 22 — Дальность сущностей — тем же эллипсом, что и чанки.
- `OcclusionCullerEmbeddiumMixin`, 23 — Embeddium: секция в пределах дальности по его проверке ещё должна попасть в эллипс (выше/ниже и по бокам — короче).

### `faygolover.flperf.mixin.client.gpu_leak`
- `RenderTargetMixin`, 40 — Буфер кадра, который мод забыл уничтожить, освобождается после сборки мусора (через Cleaner, без finalize).

### `faygolover.flperf.mixin.client.recipebook`
- `ImageButtonAccessor` (interface), 13 — *без описания*
- `RecipeBookComponentMixin`, 16 — Книга рецептов никогда не открыта (даже если в сохранении стоит «открыта»).

### `faygolover.flperf.mixin.despawn`
- `EnderManDespawnMixin`, 16 — Эндермен с блоком в руках может исчезнуть (блок выпадет — см.
- `MobDespawnMixin`, 59 — Подобранный предмет больше не делает моба вечным (ваниль ставит persistenceRequired).

### `faygolover.flperf.mixin.orphan_tickets`
- `TicketOwnerAccessor` (interface), 16 — Чей мод и чей владелец у метки Forge (поля закрыты).

### `faygolover.flperf.mixin.recipebook`
- `ServerRecipeBookMixin`, 27 — Книга рецептов выключена: сервер не шлёт игроку открытые рецепты (ни при входе, ни при открытии новых).

### `faygolover.flperf.mixin.safety`
- `AdvancementEventsMixin`, 39 — Достижение уже выдано, событие только сообщает об этом модам.
- `CommandsMixin`, 16 — Ошибка внутри команды пишется в лог со стеком (ваниль — только в режиме отладки).
- `EntityLoadMixin`, 48 — Сущность с битыми данными не роняет сервер при загрузке чанка (по образцу Cupboard).
- `ServerLevelAddEntityMixin`, 45 — Мод добавляет сущность не из потока сервера → она встаёт в очередь и добавляется в потоке сервера (по образцу Cupboard).
- `ThreadingDetectorMixin`, 19 — Ошибка «доступ из нескольких потоков» показывает стеки всех потоков — видно, какой мод полез не из своего потока.

### `faygolover.flperf.mixin.structures`
- `ChunkGeneratorSearchMixin`, 48 — Поиск ближайшей структуры: радиус не больше global_search_radius, время не больше search_timeout_seconds.
- `ExplorationMapFunctionMixin`, 23 — Карты сокровищ и исследователя ищут структуру не дальше map_search_radius.
- `LegacyRandomSourceMixin`, 23 — Мод, взявший общий рандом из нескольких потоков, ваниль роняет («Accessing LegacyRandomSource from multiple threads»).
- `LocateCommandMixin`, 30 — /locate: радиус locate_search_radius и запас времени +30 с (команду зовёт админ осознанно).
- `PlaceCommandMixin`, 38 — /place structure ставит структуру там, где сказал ГМ: расстояние до соседей проверяется только при генерации.
- `PlacedFeatureMixin`, 31 — Фича (дерево, руда, декор мода), упавшая при расстановке, пропускается с записью в лог — генерация чанка не падает.
- `StructureMinDistanceMixin`, 71 — Новая структура не ставится ближе min_structure_distance к другой (как в structureessentials, который сейчас в сборке): пространство делится на клетки этого размера, каждая клетка помнит, какая структура её заняла.

### `faygolover.flperf.mixin.sync_loads`
- `BeeMixin`, 22 — Пчела не проверяет улей в незагруженном чанке (иначе проверка грузит чанк).
- `DynamicGameEventListenerMixin`, 22 — Слушатели игровых событий (скалк и т.п.) обновляются, только если чанк уже загружен.
- `GroundPathNavigationMixin`, 27 — Моб не строит путь к точке в незагруженном чанке.
- `MapItemMixin`, 25 — Карта в руках не грузит чанки, которые рисует: незагруженные пропускаются.
- `RemoveBlockGoalMixin`, 22 — Зомби, ищущий яйца черепах, не грузит чанки.
- `ServerLevelClipMixin`, 52 — Луч (стрелы, взгляд мобов, моды) с концом в незагруженном чанке идёт как обычно до первого незагруженного и там промахивается, чанки не грузит.
- `SleepInBedMixin`, 23 — Житель не идёт спать в кровать из незагруженного чанка.
- `StructureCheckMixin`, 34 — Поиск клада сразу отбрасывает чанки с неподходящим биомом, не читая их с диска.

### `faygolover.flperf.mixin.terrablender`
- `BiomeConditionSourceMixin`, 36 — Условие «биом X»: если X нет рядом — сразу «ложь»; если рядом только X — сразу «истина».
- `NoiseBasedChunkGeneratorMixin`, 29 — На время построения поверхности чанка запоминаем регион генерации (контексту нужны соседние чанки).
- `SequenceRuleSourceMixin`, 22 — Из последовательности правил выкидываются ветки с условием «ложь» и пустые последовательности.
- `SurfaceContextMixin`, 48 — Контекст правил поверхности знает, какие биомы есть в чанке и в 8 соседних (по образцу TerraBlenderFix).
- `SurfaceRuleManagerMixin`, 48 — TerraBlender: правила модов складываются в одну последовательность (а не дерево «по пространствам имён» с проверкой на каждом шаге), порядок регистрации сохраняется (по образцу TerraBlenderFix).

### `faygolover.flperf.mixin.tick_guard`
- `BlockEntityGuardMixin`, 28 — Тик блок-сущности (машины, мебель модов...): замороженная не тикает; исключение — заморозка вместо падения.
- `InventoryGuardMixin`, 33 — Тик предмета в инвентаре: сбой пишется в лог (раз на тип предмета), предмет просто не тикает в этот раз.
- `LevelEntityGuardMixin`, 26 — Тик сущности: замороженная не тикает; исключение — заморозка вместо падения сервера (на клиенте тоже).
- `PlayerTickGuardMixin`, 25 — Сбой в тике игрока — игрок отключается с понятным сообщением, сервер работает дальше.
- `ServerLevelBlockGuardMixin`, 63 — Запланированные и случайные тики блоков и жидкостей: сбой — блок замораживается вместо падения сервера.

Классов без описания: 1 из 67.

## 3. Подписки на события

| Событие | Кто слушает |
|---|---|
| `AddPackFindersEvent` | `ClientEvents.addPacks` |
| `ScreenEvent.Init.Post` | `ClientEvents.onScreenInit` |
| `ScreenEvent.Opening` | `ClientEvents.onScreenOpen` |
| `TickEvent.ClientTickEvent` | `ClientEvents.onClientTick` |

## 4. Сеть

Нет.

## 5. Команды

- `faygolover/flperf/FlPerfCommands.java`: `flperf` `warm` `here` `clear` `tickets` `frozen` `resume` `kill`

## 6. Данные

- свои файлы на диске: `Features`
- свои файлы на диске: `AsyncSave`
- SavedData: `WarmChunks`, файл "flperf_warm_chunks"

## 7. Конфиги

- common: `PerfConfig`, вызовов define 16
- client: `PerfClientConfig`, вызовов define 6

## 8. Что берёт из RP Core и что ему даёт

Не использует.

## 9. Связи с другими модами


Миксины (46):
- `advancements.PlayerAdvancementsMixin` (mixins) → PlayerAdvancements.class
- `advancements.RobitSkinMixin` (mixins) → targets = "mekanism.api.robit.AdvancementBasedRobitSkin", remap = false
- `advancements.ServerAdvancementManagerMixin` (mixins) → ServerAdvancementManager.class
- `async_save.LevelStorageAccessMixin` (mixins) → LevelStorageSource.LevelStorageAccess.class
- `async_save.SavedDataMixin` (mixins) → SavedData.class
- `despawn.EnderManDespawnMixin` (mixins) → EnderMan.class
- `despawn.MobDespawnMixin` (mixins) → Mob.class
- `orphan_tickets.TicketOwnerAccessor` (mixins) → ForgeChunkManager.TicketOwner.class, remap = false
- `recipebook.ServerRecipeBookMixin` (mixins) → ServerRecipeBook.class
- `safety.AdvancementEventsMixin` (mixins) → ForgeEventFactory.class, remap = false
- `safety.CommandsMixin` (mixins) → Commands.class
- `safety.EntityLoadMixin` (mixins) → Entity.class
- `safety.ServerLevelAddEntityMixin` (mixins) → ServerLevel.class
- `safety.ThreadingDetectorMixin` (mixins) → ThreadingDetector.class
- `structures.ChunkGeneratorSearchMixin` (mixins) → ChunkGenerator.class
- `structures.ExplorationMapFunctionMixin` (mixins) → ExplorationMapFunction.class
- `structures.LegacyRandomSourceMixin` (mixins) → LegacyRandomSource.class, priority = 5
- `structures.LocateCommandMixin` (mixins) → LocateCommand.class
- `structures.PlacedFeatureMixin` (mixins) → PlacedFeature.class
- `structures.PlaceCommandMixin` (mixins) → PlaceCommand.class
- `structures.StructureMinDistanceMixin` (mixins) → Structure.class
- `sync_loads.BeeMixin` (mixins) → Bee.class
- `sync_loads.DynamicGameEventListenerMixin` (mixins) → DynamicGameEventListener.class
- `sync_loads.GroundPathNavigationMixin` (mixins) → GroundPathNavigation.class
- `sync_loads.MapItemMixin` (mixins) → MapItem.class
- `sync_loads.RemoveBlockGoalMixin` (mixins) → RemoveBlockGoal.class
- `sync_loads.ServerLevelClipMixin` (mixins) → ServerLevel.class
- `sync_loads.SleepInBedMixin` (mixins) → SleepInBed.class
- `sync_loads.StructureCheckMixin` (mixins) → StructureCheck.class
- `terrablender.BiomeConditionSourceMixin` (mixins) → targets = "net.minecraft.world.level.levelgen.SurfaceRules$BiomeConditionSource"
- `terrablender.NoiseBasedChunkGeneratorMixin` (mixins) → NoiseBasedChunkGenerator.class
- `terrablender.SequenceRuleSourceMixin` (mixins) → SurfaceRules.SequenceRuleSource.class, priority = 1101
- `terrablender.SurfaceContextMixin` (mixins) → SurfaceRules.Context.class
- `terrablender.SurfaceRuleManagerMixin` (mixins) → SurfaceRuleManager.class, remap = false
- `tick_guard.BlockEntityGuardMixin` (mixins) → targets = "net.minecraft.world.level.chunk.LevelChunk$BoundTickingBlockEntity"
- `tick_guard.InventoryGuardMixin` (mixins) → Inventory.class
- `tick_guard.LevelEntityGuardMixin` (mixins) → Level.class
- `tick_guard.PlayerTickGuardMixin` (mixins) → ServerGamePacketListenerImpl.class
- `tick_guard.ServerLevelBlockGuardMixin` (mixins) → ServerLevel.class
- `client.advancements_hide_ui.ToastComponentMixin` (client) → ToastComponent.class
- `client.background_fps.GameRendererFrameMixin` (client) → GameRenderer.class
- `client.distance.EntityDistanceMixin` (client) → Entity.class
- `client.distance.OcclusionCullerEmbeddiumMixin` (client) → OcclusionCuller.class, remap = false
- `client.gpu_leak.RenderTargetMixin` (client) → RenderTarget.class
- `client.recipebook.ImageButtonAccessor` (client) → ImageButton.class
- `client.recipebook.RecipeBookComponentMixin` (client) → RecipeBookComponent.class

## 10. Зарегистрированные id

Нет или регистрируются не строковыми литералами.
