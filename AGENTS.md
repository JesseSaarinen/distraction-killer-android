# Ohjeet AI-agenteille – Distraction Killer (Android)

> Tämä tiedosto on olemassa kahtena identtisenä kopiona: `CLAUDE.md` (Claude Code) ja `AGENTS.md` (muut agentit, esim. Codex, Copilot, Gemini, Cursor). **Jos muutat toista, muuta toista täsmälleen samoin.**

## Projekti

Kurssin SCI-C1002 (Aalto, ryhmä 5) demo-sovellus Androidille. Sovellus laskee, kuinka kauan käyttäjä käyttää valitsemiaan sovelluksia (oletuksena TikTok, Instagram ja YouTube). Jokaisella sovelluksella on oma päivittäinen aika, ja kun se on käytetty, sovellus muistuttaa kahden tason keskeytyksellä. Suunnitelma, tehtävälista ja aikataulu: **lue `PLAN.md` ennen kuin teet mitään.**

Tämä korvaa aiemman web-demon (repo `distraction-killer`), jota ei enää kehitetä.

## Rakenne lyhyesti

- `detection/UsageWatcherService.kt` on etualapalvelu (foreground service). Se lukee kerran sekunnissa Androidin käyttötapahtumat (`UsageStatsManager`) ja tietää niistä, mikä sovellus on edessä.
- `detection/Foreground.kt` päättää tapahtumista, mikä sovellus on edessä ja kuinka kauan se on ollut edessä tänään. `detection/UsageTimer.kt` laskee jokaisen sovelluksen päivän ajan ja kertoo, milloin taso 1 ja taso 2 tulevat.
- `interrupt/InterruptController.kt` näyttää tason 1 bannerin (`OverlayWindow`, "Näytä muiden sovellusten päällä" -ikkuna) ja avaa tason 2 (`CalmActivity`).
- Puhelimeen tallentavat vain `data/SettingsStore.kt` (asetukset), `data/EventLog.kt` (keskeytysloki) ja `detection/InspectorLog.kt` (tarkastelun loki).
- Päivän ajat, sovelluksen tunnistus käyttötapahtumista, asetusten ajat ja tilastot ovat puhdasta Kotlinia ilman Androidin luokkia, ja niillä on yksikkötestit (`app/src/test/`). Pidä ne sellaisina.
- Palvelu ei saa kaatua. Virheet otetaan kiinni ja kirjataan tarkasteluun (`InspectorLog.error`).

## Ehdottomat säännöt

1. **Kotlin + Jetpack Compose, Gradle (Kotlin DSL).** Ei muita kehyksiä tai kieliä. Uusi kirjasto vain, jos ihminen hyväksyy sen ensin. Versiot ovat tiedostossa `gradle/libs.versions.toml`.
2. **Ei INTERNET-oikeutta, ei backendiä, ei analytiikkaa.** Mikään data ei saa lähteä puhelimesta.
3. **Sovellus lukee vain, mikä sovellus on edessä ja milloin** (käyttötapahtumat). Ei koskaan ruudun sisältöä, tekstiä, viestejä, kuvia tai videoita. Ei Accessibility Serviceä: pankkisovellukset ja Play Protect eivät hyväksy sitä (päätös N21, ks. `PLAN.md`).
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
- **Testaus:** yksikkötestit `./gradlew testDebugUnitTest` (CI ajaa ne ennen buildia). Kun muutat sovelluksen tunnistusta, päivän aikoja, asetuksia tai tilastoja, lisää tai päivitä testi. Asenna lisäksi APK puhelimeen (ohjeet `README.md`:ssä) ja kokeile muutos oikeasti `TESTING.md`:n listan avulla. Kerro PR:ssä, mitä testasit puhelimella.
- Allekirjoitusavain `app/demo.keystore` on tarkoituksella julkinen, jotta uusi APK asentuu vanhan päälle. Älä vaihda sitä.

## Kun et ole varma

Kysy ihmiseltä mieluummin kuin arvaa, erityisesti jos muutos koskisi oikeuksia (manifest), ajanseurantapalvelua tai `PLAN.md`:n päätöksiä.
