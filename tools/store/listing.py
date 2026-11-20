#!/usr/bin/env python3
"""Generates the Google Play store listing texts for all nine languages.

    store/<lang>/title.txt              (max 30 characters)
    store/<lang>/short_description.txt  (max 80 characters)
    store/<lang>/full_description.txt   (max 4000 characters)

The title is the app name from tools/i18n/strings.py. Run:  python3 tools/store/listing.py
"""
import os
import runpy

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SOURCE_URL = "https://github.com/petrf22/vernostni-karta"

# lang: (short description, intro, [feature bullets], privacy heading, privacy text, source line)
LISTING = {
    "en": (
        "All your loyalty cards in one place. Offline, private, no ads.",
        "Leave the stack of plastic cards at home. Loyalty Card keeps all your store cards in your phone and shows the barcode at the checkout.",
        ["Add a card by scanning it with the camera or from an image, or type the number in.",
         "Large, high-contrast code with automatically raised screen brightness, so the cashier's scanner reads it at once.",
         "Search the list and arrange the cards by drag and drop.",
         "Nearby (optional, off by default): offers first the cards you use near your current position. The location stays on the device.",
         "Back up and restore everything as a file (export and import)."],
        "Private by design",
        "The app has no internet permission at all, so it cannot send anything anywhere. There are no ads, no analytics and no accounts. Your cards stay on your device and leave it only when you export them yourself.",
        "Free and open source (GPLv3)",
    ),
    "cs": (
        "Všechny věrnostní karty na jednom místě. Offline, soukromě, bez reklam.",
        "Nechte hromadu plastových karet doma. Věrnostní karta drží všechny vaše obchodní karty v telefonu a na pokladně ukáže čárový kód.",
        ["Kartu přidáte naskenováním kamerou nebo z obrázku, případně opíšete číslo ručně.",
         "Velký kontrastní kód se automaticky zvýšeným jasem displeje, aby ho čtečka na pokladně přečetla hned.",
         "Hledání v seznamu a řazení karet přetažením.",
         "Poblíž (volitelné, ve výchozím stavu vypnuté): nabídne jako první karty, které používáte poblíž své polohy. Poloha zůstává v zařízení.",
         "Záloha a obnova všeho do souboru (export a import)."],
        "Soukromí především",
        "Aplikace nemá vůbec oprávnění k internetu, takže nemůže nic nikam odeslat. Žádné reklamy, analytika ani účty. Vaše karty zůstávají v zařízení a opustí ho jen tehdy, když je sami exportujete.",
        "Zdarma a open source (GPLv3)",
    ),
    "sk": (
        "Všetky vernostné karty na jednom mieste. Offline, súkromne, bez reklám.",
        "Nechajte kopu plastových kariet doma. Vernostná karta drží všetky vaše obchodné karty v telefóne a na pokladni ukáže čiarový kód.",
        ["Kartu pridáte naskenovaním kamerou alebo z obrázka, prípadne prepíšete číslo ručne.",
         "Veľký kontrastný kód s automaticky zvýšeným jasom displeja, aby ho čítačka na pokladni prečítala hneď.",
         "Vyhľadávanie v zozname a radenie kariet presúvaním.",
         "V blízkosti (voliteľné, predvolene vypnuté): ponúkne ako prvé karty, ktoré používate blízko svojej polohy. Poloha zostáva v zariadení.",
         "Záloha a obnova všetkého do súboru (export a import)."],
        "Súkromie predovšetkým",
        "Aplikácia nemá vôbec oprávnenie na internet, takže nemôže nič nikam odoslať. Žiadne reklamy, analytika ani účty. Vaše karty zostávajú v zariadení a opustia ho len vtedy, keď ich sami exportujete.",
        "Zadarmo a open source (GPLv3)",
    ),
    "pl": (
        "Wszystkie karty lojalnościowe w jednym miejscu. Offline, bez reklam.",
        "Zostaw stos plastikowych kart w domu. Karta lojalnościowa trzyma wszystkie Twoje karty sklepowe w telefonie i pokazuje kod kreskowy przy kasie.",
        ["Kartę dodasz, skanując ją aparatem lub z obrazu, albo wpisując numer ręcznie.",
         "Duży, kontrastowy kod z automatycznie zwiększoną jasnością ekranu, aby czytnik przy kasie odczytał go od razu.",
         "Wyszukiwanie na liście i ustawianie kolejności kart przeciąganiem.",
         "W pobliżu (opcjonalnie, domyślnie wyłączone): na początku pokazuje karty, których używasz blisko swojej pozycji. Lokalizacja zostaje na urządzeniu.",
         "Kopia zapasowa i przywracanie wszystkiego do pliku (eksport i import)."],
        "Prywatność przede wszystkim",
        "Aplikacja w ogóle nie ma uprawnienia do internetu, więc nie może niczego nigdzie wysłać. Brak reklam, analityki i kont. Twoje karty zostają na urządzeniu i opuszczają je tylko wtedy, gdy sam je wyeksportujesz.",
        "Za darmo i open source (GPLv3)",
    ),
    "hu": (
        "Minden törzskártyája egy helyen. Offline, privát, hirdetések nélkül.",
        "Hagyja otthon a műanyag kártyák halmát. A Törzskártya a telefonjában tartja az összes bolti kártyáját, és a pénztárnál megjeleníti a vonalkódot.",
        ["Kártyát felvehet a kamerával vagy képből beolvasva, illetve a számot kézzel beírva.",
         "Nagy, kontrasztos kód automatikusan megemelt képernyőfényerővel, hogy a pénztári olvasó azonnal beolvassa.",
         "Keresés a listában és a kártyák sorrendjének módosítása húzással.",
         "A közelben (opcionális, alapból kikapcsolva): elöl kínálja azokat a kártyákat, amelyeket a jelenlegi helyzete közelében használ. A helyadat az eszközön marad.",
         "Minden adat mentése és visszaállítása fájlba (export és import)."],
        "Alapból privát",
        "Az alkalmazásnak egyáltalán nincs internet-engedélye, ezért semmit nem tud sehová elküldeni. Nincsenek hirdetések, elemzések és fiókok. A kártyái az eszközén maradnak, és csak akkor hagyják el, ha Ön exportálja őket.",
        "Ingyenes és nyílt forráskódú (GPLv3)",
    ),
    "ro": (
        "Toate cardurile de fidelitate într-un loc. Offline, privat, fără reclame.",
        "Lasă grămada de carduri de plastic acasă. Card de fidelitate păstrează toate cardurile tale de magazin în telefon și afișează codul de bare la casă.",
        ["Adaugi un card scanându-l cu camera sau dintr-o imagine, ori introducând numărul manual.",
         "Cod mare, cu contrast ridicat, cu luminozitatea ecranului crescută automat, ca scannerul de la casă să îl citească imediat.",
         "Căutare în listă și ordonarea cardurilor prin tragere.",
         "În apropiere (opțional, dezactivat implicit): îți oferă primele cardurile pe care le folosești lângă poziția ta curentă. Locația rămâne pe dispozitiv.",
         "Copie de rezervă și restaurare pentru tot, într-un fișier (export și import)."],
        "Confidențialitate înainte de toate",
        "Aplicația nu are deloc permisiunea de internet, deci nu poate trimite nimic nicăieri. Fără reclame, fără analiză și fără conturi. Cardurile tale rămân pe dispozitiv și îl părăsesc doar atunci când le exporți chiar tu.",
        "Gratuită și open source (GPLv3)",
    ),
    "de": (
        "Alle Kundenkarten an einem Ort. Offline, privat, ohne Werbung.",
        "Lassen Sie den Stapel Plastikkarten zu Hause. Die Kundenkarte speichert alle Ihre Karten im Smartphone und zeigt an der Kasse den Barcode an.",
        ["Karten per Kamera oder aus einem Bild einscannen oder die Nummer von Hand eingeben.",
         "Großer, kontrastreicher Code mit automatisch erhöhter Bildschirmhelligkeit, damit der Scanner an der Kasse ihn sofort liest.",
         "Suche in der Liste und Sortieren der Karten per Drag-and-drop.",
         "In der Nähe (optional, standardmäßig aus): zeigt zuerst die Karten, die Sie nahe Ihrem aktuellen Standort verwenden. Der Standort bleibt auf dem Gerät.",
         "Alles als Datei sichern und wiederherstellen (Export und Import)."],
        "Datenschutz von Grund auf",
        "Die App hat überhaupt keine Internetberechtigung und kann daher nichts irgendwohin senden. Keine Werbung, keine Analyse, keine Konten. Ihre Karten bleiben auf dem Gerät und verlassen es nur, wenn Sie sie selbst exportieren.",
        "Kostenlos und Open Source (GPLv3)",
    ),
    "es": (
        "Todas tus tarjetas de fidelidad en un lugar. Sin conexión y sin anuncios.",
        "Deja el montón de tarjetas de plástico en casa. Tarjeta de fidelidad guarda todas tus tarjetas de tiendas en el móvil y muestra el código de barras en la caja.",
        ["Añade una tarjeta escaneándola con la cámara o desde una imagen, o escribe el número a mano.",
         "Código grande y de alto contraste con el brillo de la pantalla aumentado automáticamente, para que el lector de la caja lo lea al instante.",
         "Busca en la lista y ordena las tarjetas arrastrándolas.",
         "Cerca (opcional, desactivado por defecto): ofrece primero las tarjetas que usas cerca de tu posición actual. La ubicación se queda en el dispositivo.",
         "Copia de seguridad y restauración de todo en un archivo (exportar e importar)."],
        "Privada desde el diseño",
        "La aplicación no tiene ningún permiso de internet, así que no puede enviar nada a ninguna parte. Sin anuncios, sin analíticas y sin cuentas. Tus tarjetas permanecen en tu dispositivo y solo salen de él cuando las exportas tú.",
        "Gratuita y de código abierto (GPLv3)",
    ),
    "fr": (
        "Toutes vos cartes de fidélité au même endroit. Hors ligne, privé, sans pub.",
        "Laissez la pile de cartes en plastique à la maison. Carte de fidélité garde toutes vos cartes de magasin dans votre téléphone et affiche le code-barres en caisse.",
        ["Ajoutez une carte en la scannant avec l’appareil photo ou depuis une image, ou saisissez le numéro à la main.",
         "Grand code très contrasté, avec la luminosité de l’écran augmentée automatiquement, pour que le lecteur de la caisse le lise tout de suite.",
         "Recherche dans la liste et classement des cartes par glisser-déposer.",
         "À proximité (facultatif, désactivé par défaut) : propose en premier les cartes que vous utilisez près de votre position actuelle. La position reste sur l’appareil.",
         "Sauvegarde et restauration de tout dans un fichier (export et import)."],
        "Privé par conception",
        "L’application n’a aucune autorisation Internet : elle ne peut donc rien envoyer nulle part. Pas de publicité, pas d’analyse, pas de compte. Vos cartes restent sur votre appareil et ne le quittent que lorsque vous les exportez vous-même.",
        "Gratuite et open source (GPLv3)",
    ),
}

LIMITS = {"title": 30, "short_description": 80, "full_description": 4000}


def build_full(intro, bullets, heading, privacy, source):
    parts = [intro, "", "\n".join("• " + b for b in bullets), "", heading.upper(), privacy, "",
             source + ": " + SOURCE_URL]
    return "\n".join(parts)


def main():
    ns = runpy.run_path(os.path.join(ROOT, "tools", "i18n", "strings.py"))
    langs, names = ns["LANGS"], ns["STRINGS"]["app_name"]
    assert set(LISTING) == set(langs), "LISTING must cover exactly LANGS"
    for lang, title in zip(langs, names):
        short, intro, bullets, heading, privacy, source = LISTING[lang]
        texts = {
            "title": title,
            "short_description": short,
            "full_description": build_full(intro, bullets, heading, privacy, source),
        }
        folder = os.path.join(ROOT, "store", lang)
        os.makedirs(folder, exist_ok=True)
        for name, text in texts.items():
            assert len(text) <= LIMITS[name], "%s/%s too long: %d > %d" % (lang, name, len(text), LIMITS[name])
            with open(os.path.join(folder, name + ".txt"), "w", encoding="utf-8") as out:
                out.write(text + "\n")
        print(lang, {name: len(text) for name, text in texts.items()})


if __name__ == "__main__":
    main()
