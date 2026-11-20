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
| Release AAB | `app/build/outputs/bundle/release/app-release.aab` | `./gradlew :app:bundleRelease` |

`<lang>` je `en cs sk pl hu ro de es fr`. V Play Console je to: en → *English – en-US*,
cs → *Czech – cs-CZ*, sk → *Slovak*, pl → *Polish*, hu → *Hungarian*, ro → *Romanian*,
de → *German – de-DE*, es → *Spanish – es-ES*, fr → *French – fr-FR*.

Soubory `store/**` se po změně textů/ikony přegenerují těmi skripty; needitujte je ručně.

## 1. Upload klíč (jednou)

Příkaz spusťte v terminálu sami (zadáte hesla). Soubor ani hesla nikdy nedávejte do gitu.

```bash
keytool -genkeypair -v -keystore ~/vernostni-karta-upload.jks -alias upload \
  -keyalg RSA -keysize 4096 -validity 10000
```

Do kořene repa vytvořte `keystore.properties` (je v `.gitignore`):

```properties
storeFile=/home/petr/vernostni-karta-upload.jks
storePassword=...
keyAlias=upload
keyPassword=...
```

Pro CI lze místo souboru použít proměnné `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`,
`KEY_PASSWORD`. Keystore si zálohujte mimo počítač. V Play Console nechte zapnuté
**Play App Signing** (výchozí); klíč, který držíte vy, je pouze *upload key* a při ztrátě se dá
na žádost resetovat.

## 2. Sestavení

```bash
./gradlew check :app:bundleRelease
```

Před každým dalším nahráním zvyšte `versionCode` v `app/build.gradle.kts` (Play nepřijme stejný
dvakrát) a podle potřeby `versionName`. Release je minifikovaný (R8); před nahráním ho
nainstalujte a projděte skener, detail, export a import.

## 3. Účet a nová aplikace

1. https://play.google.com/console → účet vývojáře (jednorázově 25 USD, ověření identity).
   U osobního účtu vytvořeného po listopadu 2023 Google vyžaduje před produkcí **closed test**
   (v době psaní: min. 12 testerů, 14 dní nepřetržitě) — ověřte aktuální stav v konzoli.
2. **Create app**: název *Loyalty Card*, výchozí jazyk *English (United States)*, typ *App*,
   *Free*, odsouhlasit deklarace.

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

1. *Testing → Internal testing → Create new release* → nahrát `app-release.aab`, vyplnit
   *Release notes* (např. „První verze.“), uložit a vydat. Přidejte sebe jako testera
   a nainstalujte z odkazu — ověříte, že AAB funguje a podpis sedí.
2. Pro osobní účet: *Closed testing* s testery a po splnění podmínky *Apply for production*.
3. *Production → Create new release* → stejný nebo novější AAB → *Send for review*.
   První kontrola trvá obvykle několik dní.

## 7. Screenshoty

Telefon: min. 2, max. 8 na jazyk, PNG/JPEG, strana 320–3840 px, poměr stran max. 2:1.
Pořizují se v emulátoru `Medium_Phone` (viz `CLAUDE.md`): jazyk appky
`adb shell cmd locale set-app-locales io.github.petrf22.vernostnikarta --locales <lang>`,
data v ASCII, stav ověřovat `ui.py dump`, snímek `ui.py screenshot <cesta>`.
