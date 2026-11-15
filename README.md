# Věrnostní karta

Jednoduchá Android aplikace na věrnostní karty: naskenujete čárový nebo QR kód karty a v obchodě
ho ukážete na displeji. **Aplikace nemá přístup k internetu a nikam neodesílá žádná data.**

*English summary below.*

## Co umí

- **Seznam karet** s rychlým hledáním podle názvu obchodu (bez ohledu na velikost písmen
  a diakritiku), přidání tlačítkem **+**, pořadí změníte přetažením (dlouhý stisk).
- **Skenování** fotoaparátem, načtení kódu z obrázku nebo ruční zadání. Podporované kódy:
  QR, Aztec, Data Matrix, PDF417, EAN-13, EAN-8, UPC-A, UPC-E, Code 128, Code 39, Code 93, ITF, Codabar.
- **Karta vypadá jako karta** – zaoblené rohy, barva obchodu (u známých řetězců se předvyplní,
  jinak si vyberete z palety nebo namícháte vlastní), název a poznámka. Při zobrazení se zvýší jas
  displeje a kód jde klepnutím zvětšit přes celou obrazovku.
- **Karty poblíž** – pokud to povolíte, aplikace si při zobrazení karty zapamatuje místo a příště
  vám tam kartu nabídne nahoře v seznamu. Poloha se ukládá jen v telefonu a historii lze smazat.
- **Export a import** všech karet, míst a nastavení do jednoho souboru JSON – pro přenos na jiný telefon.
- **Jazyky:** čeština, slovenština, polština, maďarština, rumunština, němčina, španělština,
  francouzština a angličtina.

## Soukromí

- Aplikace **nemá oprávnění k internetu** (`android.permission.INTERNET`). Není to slib, ale
  technické omezení: bez něj Android aplikaci žádné síťové spojení nedovolí. Sestavení navíc selže,
  kdyby si toto oprávnění vyžádala některá knihovna (úloha `checkDebugNoNetworkPermission`).
- Žádné reklamy, analytika, Google Play Services ani Firebase. Kódy čte knihovna ZXing přímo v telefonu.
- Automatické zálohování do cloudu je vypnuté. Data opustí telefon jen tehdy, když je sami exportujete.
- Oprávnění: **fotoaparát** (skenování) a volitelně **poloha** (karty poblíž; bere se ze systémové
  služby určování polohy).

## Sestavení

Potřebujete JDK 17+ a Android SDK (cesta v `local.properties` jako `sdk.dir=…` nebo v `ANDROID_HOME`).

```bash
./gradlew assembleDebug          # APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew check                  # testy, lint a kontrola, že aplikace nežádá přístup k síti
```

Texty všech jazyků jsou v jedné tabulce `tools/i18n/strings.py`; po úpravě spusťte
`python3 tools/i18n/strings.py`.

## Překlady

Překlady kromě češtiny a angličtiny nevytvořil rodilý mluvčí. Opravy jsou vítány –
upravte `tools/i18n/strings.py` a pošlete pull request.

## Licence

[GNU General Public License v3.0](LICENSE)

---

## English

A simple Android app for loyalty cards: scan the barcode or QR code of a card and show it on the
screen at the checkout. **The app has no internet access and never sends any data anywhere.**

- Card list with quick search, add with **+**, reorder by long-press and drag.
- Scan with the camera, load the code from an image, or type it in (QR, Aztec, Data Matrix, PDF417,
  EAN-13/8, UPC-A/E, Code 128/39/93, ITF, Codabar).
- Cards look like cards: rounded corners, store color, name and note; full brightness while shown.
- Optional “nearby” suggestions: the place where a card was shown is remembered on the device only.
- Export / import of all cards and settings as a single JSON file.
- No `INTERNET` permission (enforced by the build), no ads, no analytics, no Google Play Services,
  no cloud backup.
- Languages: English, Czech, Slovak, Polish, Hungarian, Romanian, German, Spanish, French.
  Translations other than Czech and English were not made by native speakers — corrections are
  welcome in `tools/i18n/strings.py`.

Build with `./gradlew assembleDebug` (JDK 17+, Android SDK). Licensed under the
[GNU GPL v3.0](LICENSE).
