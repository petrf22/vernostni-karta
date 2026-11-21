---
name: release-vernostni-karta
description: Vydat novou verzi Věrnostní karty — povýšit VERSION/CHANGELOG přes PR, otagovat a sestavit podepsaný a ověřený AAB pro Google Play včetně poznámek k vydání v devíti jazycích
argument-hint: [X.Y.Z|major|minor|patch]
arguments: [bump]
disable-model-invocation: true
---

## Aktuální stav

Verze: !`cat VERSION`

Poslední tagy: !`git tag --list 'v*' | sort -V | tail -5`

Větev a čistota stromu: !`git status -sb`

Commity od posledního tagu: !`git log --oneline HEAD --not --tags`

Sekce `[Nezveřejněno]` v CHANGELOG.md: !`grep -n "Nezveřejněno" CHANGELOG.md || echo "(žádná)"`

Podpisové property: !`grep -o "^VERNOSTNIKARTA_[A-Z_]*" ~/.gradle/gradle.properties 2>/dev/null || echo "(žádné)"`

Dnešní datum: !`date +%Y-%m-%d`

## Postup

Opakovaný release flow — proveď ho celý sám, bez zbytečných dotazů, ale se dvěma pevnými
zastaveními (mergnutí PR a závěrečné shrnutí). `X.Y.Z`/`vX.Y.Z` níže je placeholder za verzi
spočítanou v kroku 1. Do Play Console nic nenahráváš — AAB nahrává uživatel ručně.

0. **Předpoklady.** Větev `main`, `git status --short` prázdný, jinak se zastav a vypiš, co
   brání (rozpracovaná práce se nesmí tiše zabalit do release commitu). Pokud ve výpisu výše
   chybí některá ze čtyř `VERNOSTNIKARTA_*` property, zastav se hned a odkaž na
   `docs/PLAY_STORE.md`, „Upload klíč“ — klíč a hesla zakládá uživatel sám, ty ne.

1. **Cílová verze.** Argument `$bump`:
   - `X.Y.Z` → přímo (musí být vyšší než poslední tag),
   - `major`/`minor`/`patch` → SemVer bump z `VERSION`,
   - prázdné → `patch`; **výjimka: neexistuje-li žádný tag `v*`** (první vydání), cílová verze
     je současná hodnota `VERSION` beze změny.
   Každá složka musí být 0–99 (`versionCode = major·10000 + minor·100 + patch`, počítá ho
   `app/build.gradle.kts`).

2. **`CHANGELOG.md`.**
   - Existuje-li `## [Nezveřejněno]` s položkami, přejmenuj ji na `## [X.Y.Z] – <dnešní datum>`.
   - Jinak projdi commity od posledního tagu a navrhni položky do Přidáno/Změněno/Opraveno —
     česky, z pohledu uživatele appky, jedna položka na řádek. **Ukaž návrh a počkej na
     potvrzení**, než ho zapíšeš.

3. **`VERSION`** — zapiš `X.Y.Z` (u prvního vydání se nemění).

4. **`git diff --stat`** — smí obsahovat jen `VERSION` a `CHANGELOG.md`. Cokoli jiného: zastav se.

5. **Commit na krátkodobé větvi.** `main` chrání ruleset „Ochrana main“ (jen přes PR):
   ```bash
   git checkout -b release/X.Y.Z
   git add VERSION CHANGELOG.md
   git commit -m 'Vydat X.Y.Z'
   git push -u origin release/X.Y.Z
   gh pr create --title 'Vydat X.Y.Z' --body '<sekce changelogu>'
   ```

6. **Počkej na CI** (`gh pr checks <číslo> --watch`, workflow *Build*: testy, lint, kontrola
   manifestu, APK). Červené → zastav se a vypiš chybu.

7. **Zastavení č. 1 — mergnutí.** Zeptej se uživatele před `gh pr merge <číslo> --merge`.
   Když merge zablokuje oprávnění, požádej uživatele o ruční merge na GitHubu a počkej.

8. **Tag AŽ na commitu, který přistál na `main`** (merge může změnit hash):
   ```bash
   git fetch origin main
   git log -1 --format='%H %s' origin/main   # musí obsahovat „Vydat X.Y.Z“ (u merge commitu ověř, že je to merge PR release/X.Y.Z)
   git tag -a vX.Y.Z origin/main -m 'Verze X.Y.Z'
   git push origin vX.Y.Z
   ```

9. **Dorovnej `main` a ukliď:**
   ```bash
   git checkout main
   git merge --ff-only origin/main
   git branch -d release/X.Y.Z   # na originu ji smaže GitHub (delete_branch_on_merge)
   ```

10. **Sestav ve worktree z tagu** (hlavní checkout zůstává na `main`):
    ```bash
    WORKDIR=$(mktemp -d -t vernostnikarta-release-vX.Y.Z-XXXX)
    git worktree add "$WORKDIR" vX.Y.Z
    cp local.properties "$WORKDIR/"            # negituje se, bez něj chybí sdk.dir
    (cd "$WORKDIR" && ./gradlew check :app:publishableBundle)
    ```
    `publishableBundle` bez klíče selže a hotový AAB ověří jarsignerem; na konci vypíše
    verzi a `versionCode` — ověř, že odpovídají `X.Y.Z`.

11. **Ulož výstup mimo worktree:**
    ```bash
    mkdir -p ~/releases/vernostnikarta/vX.Y.Z
    cp "$WORKDIR/app/build/outputs/bundle/release/app-release.aab" ~/releases/vernostnikarta/vX.Y.Z/
    cp "$WORKDIR/app/build/outputs/mapping/release/mapping.txt" ~/releases/vernostnikarta/vX.Y.Z/
    ```

12. **Úklid:** `git worktree remove "$WORKDIR"`.

13. **Zastavení č. 2 — shrnutí.** Verze, `versionCode`, cesta k `.aab` a `mapping.txt`, odkaz na
    PR a tag. Připomeň, pokud se od minulého tagu týkalo:
    - změny UI → screenshoty v `store/*/images/phoneScreenshots/` jsou zastaralé,
    - změny práce s polohou, kamerou, exportem nebo oprávněními → `PRIVACY.md` a odpovědi
      v Data safety (`docs/PLAY_STORE.md`),
    - změny textů store (`tools/store/listing.py`) → nahrát nové texty v Play Console.
    Kam AAB nahrát: Play Console → aplikace *Loyalty Card* → *Testing*/*Production* →
    *Create new release*.

14. **Poznámky k vydání pro Google Play** — úplný konec shrnutí, hotový text k vložení:
    - zdroj je `CHANGELOG.md` od poslední verze nahrané do Play (typicky jen sekce `X.Y.Z`;
      nešla-li předchozí verze do Play, zahrň i ji); u prvního vydání stručné představení appky,
    - všech devět jazyků appky; obsah stejný, každý jazyk psaný přirozeně, ne doslovný překlad,
    - srozumitelně, bez vývojářských termínů, krátké odrážky,
    - limit 500 znaků na jazyk — spočítej (třeba `python3 -c`) a počty uveď pod textem,
    - každý jazyk v tagu Play Console, vše v jednom bloku kódu:
      `<en-US>`, `<cs-CZ>`, `<sk>`, `<pl-PL>`, `<hu-HU>`, `<ro>`, `<de-DE>`, `<es-ES>`, `<fr-FR>`
      (tag musí odpovídat jazyku přidanému v Play Console, jinak ho vložení odmítne),
    - nic viditelného → „Drobné opravy a vylepšení.“ a ekvivalenty.

## Co tenhle skill neřeší

Hotfix starší vydané verze (větev z tagu `vX.Y.Z` → `X.Y.(Z+1)`), založení upload klíče,
registraci aplikace v Play Console ani samotné nahrání AAB — viz `docs/PLAY_STORE.md`.
