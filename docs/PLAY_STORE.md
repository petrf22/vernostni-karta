# Publikace na Google Play

Návod krok za krokem. Podklady jsou ve složce `store/` (generované, viz níže), vše ostatní je
tady. Do Play Console klikáte vy; pravidla Googlu se mění, proto si před odesláním ověřte
aktuální požadavky (zejména minimální `targetSdk` a podmínky testování u nových osobních účtů).

## 0. Co je v repu hotové

| Co | Kde | Jak vzniká |
|---|---|---|
| Ikona 512×512 | `store/<lang>/images/icon.png` | `python3 tools/store/graphics.py` |
| Feature graphic 1024×500 | `store/<lang>/images/featureGraphic.png` | totéž |
| Název, krátký a dlouhý popis | `store/<lang>/{title,short_description,full_description}.txt` | `python3 tools/store/listing.py` (texty jsou v tom skriptu, kontroluje limity 30/80/4000) |
| Screenshoty telefonu | `store/<lang>/images/phoneScreenshots/*.png` | viz sekce „Screenshoty“ |
| Privacy policy | `PRIVACY.md` | ručně |
| Podepsaný AAB | `~/releases/vernostnikarta/vX.Y.Z/app-release.aab` | `/release-vernostni-karta` (viz „Vydání verze“) |

`<lang>` je `en cs sk pl hu ro de es fr`. V Play Console je to: en → *English – en-US*,
cs → *Czech – cs-CZ*, sk → *Slovak*, pl → *Polish*, hu → *Hungarian*, ro → *Romanian*,
de → *German – de-DE*, es → *Spanish – es-ES*, fr → *French – fr-FR*.

Soubory `store/**` se po změně textů/ikony přegenerují těmi skripty; needitujte je ručně.

## 1. Upload klíč (jednou)

Stejný vzor jako u ostatních aplikací (kvalita-cena). Příkaz spusťte v terminálu sami (zadáte
hesla); soubor ani hesla nikdy nejdou do repa (`.gitignore`: `*.jks`, `*.keystore`).

```bash
mkdir -p ~/.keystores
keytool -genkeypair -v -keystore ~/.keystores/vernostnikarta-release.jks \
  -alias vernostnikarta -keyalg RSA -keysize 4096 -validity 12000
```

Do `~/.gradle/gradle.properties` přidejte:

```properties
VERNOSTNIKARTA_STORE_FILE=/home/petr/.keystores/vernostnikarta-release.jks
VERNOSTNIKARTA_STORE_PASSWORD=...
VERNOSTNIKARTA_KEY_ALIAS=vernostnikarta
VERNOSTNIKARTA_KEY_PASSWORD=...
```

`app/build.gradle.kts` je čte přes `findProperty`. Chybí-li kterákoli, `bundleRelease` vyrobí
nepodepsaný AAB (CI, cizí stroj); pro Play se proto staví `./gradlew :app:publishableBundle`,
který bez klíče **selže** a hotový AAB ověří jarsignerem. Keystore zálohujte mimo tento počítač.
V Play Console nechte zapnuté **Play App Signing**; váš klíč je pak jen *upload key*.

Otisk klíče (pro registraci balíčku v Play Console, viz níže):

```bash
keytool -list -v -keystore ~/.keystores/vernostnikarta-release.jks -alias vernostnikarta | grep SHA256
```

## 2. Vydání verze

Verze je v kořenovém souboru `VERSION` (`X.Y.Z`); `versionName` je přímo ona a
`versionCode = X·10000 + Y·100 + Z` (1.0.0 → 10000), obojí počítá `app/build.gradle.kts`.
Změny se během vývoje zapisují do `CHANGELOG.md`, sekce `[Nezveřejněno]`.

Vydání dělá Claude Code skill **`/release-vernostni-karta [X.Y.Z|major|minor|patch]`**
(`.claude/skills/release-vernostni-karta/SKILL.md`): uzavře sekci changelogu, povýší `VERSION`,
pošle PR `release/X.Y.Z` (main je chráněný, jen přes PR), po vašem potvrzení ho smerguje,
otaguje `vX.Y.Z`, sestaví z tagu ve worktree `check` + `publishableBundle` a uloží
`app-release.aab` a `mapping.txt` do `~/releases/vernostnikarta/vX.Y.Z/`. Nakonec vypíše
poznámky k vydání v devíti jazycích s tagy Play Console. První vydání (bez tagu) použije
verzi z `VERSION` tak, jak je (1.0.0).

Release je minifikovaný (R8); před prvním nahráním do produkce ho nainstalujte (internal
testing) a projděte skener, detail, export a import.

## 3. Nová aplikace v Play Console

Účet vývojáře už existuje (jsou na něm jiné aplikace), takže stačí:

1. **Create app**: název *Loyalty Card*, výchozí jazyk *English (United States)*, typ *App*,
   *Free*, odsouhlasit deklarace.
2. Pokud konzole u balíčku chce **registraci / ověření vlastnictví** (Android developer
   verification), zadejte `io.github.petrf22.vernostnikarta` a SHA-256 otisk klíče z příkazu
   výše. `applicationId` je pak napořád.
3. U osobního účtu Google může před produkcí vyžadovat **closed test** (v době psaní 12 testerů
   po 14 dní, platí pro účty založené po listopadu 2023) — stav je vidět v *Dashboard*.

## 4. Store listing

- *Main store listing* (en-US): texty ze `store/en/`, ikona, feature graphic, screenshoty.
- *Manage translations* → přidejte ostatní jazyky a z `store/<lang>/` nahrajte totéž.
- Kategorie: **Tools** nebo **Shopping** (obě jsou přijatelné; Shopping je nasnadě).
- Kontaktní údaje: e-mail (povinný, zobrazuje se veřejně), web volitelně
  `https://github.com/petrf22/vernostni-karta`.

## 5. App content (odpovědi)

- **Privacy policy URL:** `https://github.com/petrf22/vernostni-karta/blob/main/PRIVACY.md`
  (repo musí být veřejné a soubor pushnutý).
- **Ads:** No.
- **App access:** všechny funkce dostupné bez přihlášení.
- **Content rating:** dotazník IARC, kategorie *Utility/Reference*; žádný násilný, sexuální ani
  uživatelský obsah, žádné nákupy → očekává se nejnižší věkové hodnocení.
- **Target audience:** 18+ (nejde o aplikaci pro děti; vyhnete se pravidlům Families).
- **News / Government / Financial features / Health:** No. (Věrnostní karty nejsou platební.)
- **Data safety:**
  - *Does your app collect or share any of the required user data types?* → **No.**
    Důvod: žádná data neopouštějí zařízení (aplikace nemá oprávnění k internetu). Kamera
    a poloha se zpracovávají jen v zařízení; podle definice Googlu to není „collection“.
  - *Is all of the user data encrypted in transit?* se nezobrazí, pokud nic nesbíráte.
  - *Data deletion:* neaplikuje se; data se smažou odinstalací.
- **Permissions declarations:** kamera (skenování kódů) a poloha (volitelné „Poblíž“, výchozí
  stav vypnuto) — zdůvodnění je v `PRIVACY.md`. Konzole může u přesné polohy chtít dodatečné
  vysvětlení; pokud by s tím byl problém, dá se `ACCESS_FINE_LOCATION` odebrat a nechat jen
  `ACCESS_COARSE_LOCATION` (pro řazení „poblíž“ stačí; je to změna kódu, domluvte se).
- **Advertising ID:** No (aplikace ho nepoužívá).

## 6. Nahrání a vydání

1. *Testing → Internal testing → Create new release* → nahrát
   `~/releases/vernostnikarta/vX.Y.Z/app-release.aab`, do *Release notes* vložit blok, který
   vypíše `/release-vernostni-karta`, uložit a vydat. Přidejte sebe jako testera
   a nainstalujte z odkazu — ověříte, že AAB funguje a podpis sedí.
2. Pro osobní účet: *Closed testing* s testery a po splnění podmínky *Apply for production*.
3. *Production → Create new release* → stejný nebo novější AAB → *Send for review*.
   První kontrola trvá obvykle několik dní.

## 7. Screenshoty

Telefon: min. 2, max. 8 na jazyk, PNG/JPEG, strana 320–3840 px, poměr stran max. 2:1.
Pořizují se v emulátoru `Medium_Phone` (viz `CLAUDE.md`): jazyk appky
`adb shell cmd locale set-app-locales io.github.petrf22.vernostnikarta --locales <lang>`,
data v ASCII, stav ověřovat `ui.py dump`, snímek `ui.py screenshot <cesta>`.
