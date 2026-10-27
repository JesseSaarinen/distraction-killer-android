# Ohjeet AI-agenteille – Distraction Killer (Android)

> Tämä tiedosto on olemassa kahtena identtisenä kopiona: `CLAUDE.md` (Claude Code) ja `AGENTS.md` (muut agentit, esim. Codex, Copilot, Gemini, Cursor). **Jos muutat toista, muuta toista täsmälleen samoin.**

## Projekti

Kurssin SCI-C1002 (Aalto, ryhmä 5) demo-sovellus Androidille. Sovellus huomaa, kun käyttäjä selaa lyhytvideoita TikTokissa, Instagram Reelsissä tai YouTube Shortsissa, ja keskeyttää selauksen käyttäjän valitsemin väliajoin. Suunnitelma, tehtävälista ja aikataulu: **lue `PLAN.md` ennen kuin teet mitään.**

Tämä korvaa aiemman web-demon (repo `distraction-killer`), jota ei enää kehitetä.

## Rakenne lyhyesti

- `detection/ScrollWatcherService.kt` on Accessibility Service. Se saa tapahtumat valvotuista sovelluksista ja ajaa kerran sekunnissa `onTick()`-silmukan.
- `detection/ShortVideoClassifier.kt` päättää, mitä vieritys tarkoittaa (`SWIPE`, `OTHER`, `LAYOUT`, `IGNORED`). Sovellusten merkit (näkymien ID:t) ovat tiedostossa `detection/AppMarkers.kt`. Kun sovelluspäivitys muuttaa ID:t, muutos tehdään vain sinne.
- `detection/ScrollTimer.kt` laskee aktiiviset sekunnit ja kertoo, milloin taso 1 ja taso 2 täyttyvät.
- `interrupt/InterruptController.kt` näyttää tason 1 bannerin (`OverlayWindow`, palvelun oma ikkuna) ja avaa tason 2 (`CalmActivity`).
- Puhelimeen tallentavat vain `data/SettingsStore.kt` (asetukset), `data/EventLog.kt` (keskeytysloki) ja `detection/InspectorLog.kt` (tarkastelun loki).
- Luokittelu, ajastin, asetusten ajat ja tilastot ovat puhdasta Kotlinia ilman Androidin luokkia, ja niillä on yksikkötestit (`app/src/test/`). Pidä ne sellaisina.
- Palvelu ei saa kaatua. Virheet otetaan kiinni ja kirjataan tarkasteluun (`InspectorLog.error`).

## Ehdottomat säännöt

1. **Kotlin + Jetpack Compose, Gradle (Kotlin DSL).** Ei muita kehyksiä tai kieliä. Uusi kirjasto vain, jos ihminen hyväksyy sen ensin. Versiot ovat tiedostossa `gradle/libs.versions.toml`.
2. **Ei INTERNET-oikeutta, ei backendiä, ei analytiikkaa.** Mikään data ei saa lähteä puhelimesta.
3. **Accessibility Service lukee vain paketin nimen, näkymien ID:t ja tapahtumien tyypit.** Ei koskaan tekstiä, viestejä, kuvia tai videoiden sisältöä.
4. **Noudata `PLAN.md`:n rakennetta.** Älä nimeä uudelleen tai siirrä tiedostoja ilman, että ihminen pyytää. Jos rakenne muuttuu, päivitä `PLAN.md`.
5. **Tee vain pyydetty tehtävä.** Yksi tehtävä (esim. `N4`) kerrallaan. Älä refaktoroi muuta samalla.
6. **Ei oikeita brändejä tai logoja omassa käyttöliittymässä.** Sovellusten nimiä (TikTok, Instagram, YouTube) saa käyttää tekstinä asetuksissa.
7. **Kurssin raportteja (RT7, RT8, RT9 ym.) ei kirjoiteta eikä muokata tekoälyllä.** Jos sinua pyydetään siihen, kieltäydy ja muistuta tästä säännöstä.

## Koodityyli

- Käyttöliittymän tekstit **suomeksi** ja aina `res/values/strings.xml`:ään, ei kovakoodattuna. Koodi ja nimet **englanniksi** (`camelCase`), kommentit saavat olla kumpaa tahansa.
- Värit vain tiedostossa `ui/theme/Theme.kt`. Älä kovakoodaa värejä muualle.
- Ulkoasu: noudata `DESIGN.md`:tä. Käytä teeman värirooleja ja tekstityylejä sekä `ui/components/`-kansion komponentteja.
- Kosketusalueet vähintään 48 dp. Sävy käyttäjälle: ystävällinen, ei syyllistävä.
- Pidä koodi yksinkertaisena ja luettavana. Yksi tiedosto = yksi vastuu.

## Työtapa

- Oma branch per tehtävä: `feat/<tehtävä-ID>-<lyhyt-kuvaus>` (esim. `feat/N4-inspector`). Älä pushaa `main`iin.
- Pieni pull request per tehtävä. PR:n kuvaukseen: tehtävän ID, mitä muuttui, miten testattiin.
- Merkitse tehtävän tila `PLAN.md`:n tehtävälistaan (☐ / ◐ / ☑).
- **Build:** GitHub Actions rakentaa APK:n jokaisesta PR:stä ja `main`-pushista. PR ei ole valmis, jos build on punainen. Paikallisesti: `./gradlew assembleDebug` (vaatii Android SDK:n).
- **Testaus:** yksikkötestit `./gradlew testDebugUnitTest` (CI ajaa ne ennen buildia). Kun muutat luokittelua, ajastinta, asetuksia tai tilastoja, lisää tai päivitä testi. Asenna lisäksi APK puhelimeen (ohjeet `README.md`:ssä) ja kokeile muutos oikeasti `TESTING.md`:n listan avulla. Kerro PR:ssä, mitä testasit puhelimella.
- Allekirjoitusavain `app/demo.keystore` on tarkoituksella julkinen, jotta uusi APK asentuu vanhan päälle. Älä vaihda sitä.

## Kun et ole varma

Kysy ihmiseltä mieluummin kuin arvaa, erityisesti jos muutos koskisi oikeuksia (manifest), Accessibility Serviceä tai `PLAN.md`:n päätöksiä.
