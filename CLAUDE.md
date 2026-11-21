# CLAUDE.md

Komunikuj s uživatelem česky.

## Přehled

Android aplikace na věrnostní karty (Kotlin + Jetpack Compose, jeden modul `app`). Dvě zadání
určují architekturu:

1. **Aplikace nesmí odesílat žádná data.** Nemá oprávnění `INTERNET`; manifest ho navíc odebírá
   (`tools:node="remove"`) a úloha `check<Variant>NoNetworkPermission` v `app/build.gradle.kts`
   shodí build, kdyby se do sloučeného manifestu dostalo. Proto žádné Google Play Services,
   Firebase ani ML Kit: kódy čte ZXing (`barcode/`), poloha jde přes systémový `LocationManager`
   (`location/LocationProvider.kt`), zálohování do cloudu je vypnuté. Nepřidávej knihovnu, která
   potřebuje síť.
2. **Vše jde exportovat a importovat** (`backup/`). Nové pole v kartě nebo v nastavení se musí
   přidat i do `BackupModel.kt`; při změně, kterou starší verze nepřečte, zvyš `BackupCodec.VERSION`.

## Příkazy

```bash
./gradlew :app:compileDebugKotlin   # rychlá kontrola
./gradlew check assembleDebug       # testy, lint, kontrola manifestu, APK
python3 tools/i18n/strings.py       # přegeneruje strings.xml všech jazyků
```

AGP 9 má Kotlin vestavěný — plugin `org.jetbrains.kotlin.android` nepřidávat. `compileSdk 37`,
`targetSdk 36`, `minSdk 26`. Verze knihoven jsou v `gradle/libs.versions.toml`.

## Google Play

Postup publikace je v `docs/PLAY_STORE.md`. Složka `store/<lang>/` je **generovaná**: ikona a
feature graphic z `tools/store/graphics.py`, texty z `tools/store/listing.py` (tabulka textů
a kontrola limitů 30/80/4000 znaků je v něm), screenshoty se pořizují v emulátoru. `PRIVACY.md` musí odpovídat
chování aplikace — při změně práce s polohou, kamerou nebo exportem ho uprav.

Verze je v kořenovém `VERSION`; `versionName`/`versionCode` z něj počítá `app/build.gradle.kts`
— needituj je v buildu. Uživatelsky viditelné změny zapisuj do `CHANGELOG.md`, sekce
`[Nezveřejněno]`, jedna položka na řádek. Vydání dělá `/release-vernostni-karta`
(`.claude/skills/`); podpis bere z `VERNOSTNIKARTA_*` v `~/.gradle/gradle.properties`
a pro Play se staví `:app:publishableBundle`, ne `bundleRelease` (ten bez klíče tiše vyrobí
nepodepsaný AAB). `main` je chráněný rulesetem — změny jen přes PR.

## Texty a jazyky

`app/src/main/res/values*/strings.xml` jsou **generované** — needituj je. Zdroj je tabulka
`tools/i18n/strings.py` (en, cs, sk, pl, hu, ro, de, es, fr vedle sebe), po úpravě spusť skript.
Nový jazyk: sloupec v tabulce, `LANGS`, a řádek v `res/xml/locales_config.xml`.

## Ladění v emulátoru

**Stav UI ověřuj `python3 tools/mobile/ui.py dump`, ne screenshotem. Screenshot pořizuj výhradně
na výslovné vyžádání uživatele.** Nástroj je převzatý z projektu kvalita-cena (stejné příkazy:
`setup`, `dump`, `tap`, `tap-xy`, `text`, `key`, `wait`, `open`, `log`, `geo`).

```bash
~/Android/Sdk/emulator/emulator -avd Medium_Phone -no-snapshot -no-boot-anim -gpu swiftshader_indirect
~/Android/Sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
python3 tools/mobile/ui.py setup
python3 tools/mobile/ui.py open list
```

- `ui.py open <trasa>` skočí na obrazovku (`list`, `scan`, `settings`, `detail/<id>`,
  `edit/<id>?code=…&format=EAN_13`; trasy jsou v `ui/Navigation.kt`). Extra `route` čte
  `MainActivity` jen v laditelném buildu. `edit/0?code=…&format=…` je stejná cesta, jakou jde
  výsledek skeneru — kameru emulátoru tak není potřeba.
- `tap --nth` čísluje od nuly a je potřeba jen tehdy, když `tap` vypíše víc shod. Na formuláři je
  „Store name“ dvakrát (náhled karty a pole), ale klikatelné pole má přednost — `--nth` netřeba.
- `adb shell input text` neumí diakritiku — testovací data v ASCII.
- Polohu nastavuj `ui.py geo <lat> <lon>`, ne `adb emu geo fix`. Seznam bere polohu při návratu
  na obrazovku; po změně polohy appku restartuj (`am force-stop` + `ui.py open list`).
- Přetažení v seznamu: `adb shell input draganddrop x1 y1 x2 y2 2500`.
- Jazyk appky: `adb shell cmd locale set-app-locales io.github.petrf22.vernostnikarta --locales cs`
  (prázdný řetězec vrátí systémový).
- Čárový kód je kreslený na Canvas, v `dump` není vidět. Že se dá přečíst, ověřuje test
  `BarcodeRendererTest.everyFormatReadsBack`; na obrazovce to jde ověřit dekódováním snímku
  knihovnou ZXing (bez prohlížení obrázku).

## Konvence

- Jedna Activity + Compose Navigation, obrazovky v `ui/<funkce>/XxxScreen.kt`, ruční DI přes
  `AppContainer` (`App.kt`). Obrazovky čtou `Flow` z repozitáře přímo, bez ViewModelů.
- Obrazovky se automatizovaně netestují. Čistá logika bez Androidu (`filterCards`,
  `NearbyRanker`, `BackupCodec`, `BarcodeRenderer.encode`, `StoreColors`) má JUnit testy
  v `app/src/test/` — novou logiku tohoto druhu vytáhni do vlastní funkce a otestuj.
- Komentáře a identifikátory v kódu anglicky.
