# PLAN – Distraction Killer (Android)

## Tavoite

Natiivi Android-sovellus, joka laskee, kuinka kauan käyttäjä käyttää valitsemiaan sovelluksia (oletuksena TikTok, Instagram ja YouTube, ja asetuksista voi lisätä minkä tahansa). Jokaisella sovelluksella on oma päivittäinen aika. Kun se on käytetty, sovellus muistuttaa samalla kahden tason keskeytyksellä kuin web-demossa, oikeiden sovellusten päällä. Testipuhelin: Samsung Galaxy S21 (Jesse). Asennus APK:na, ei Play Kauppaa.

**Aikataulu:** RT8-versio jäädytetään **5.11.** (tagi `rt8`), messuversio **25.11.** (tagi `demo`), messut 27.11.

## Miten se toimii (N21)

1. **Käyttötiedot** (`UsageStatsManager`, oikeus "Salli käyttötietojen käyttö"). `detection/UsageWatcherService.kt` on etualapalvelu (foreground service, tyyppi `specialUse`). Se lukee kerran sekunnissa Androidin käyttötapahtumat (sovellus tuli eteen / poistui edestä, näyttö pois) ja tietää niistä, mikä sovellus on edessä. Se ei näe, mitä ruudulla on. Palvelu laskee vain, kun näyttö on päällä ja lukitus auki. Etualapalvelun takia puhelimessa näkyy ilmoitus "Ajanseuranta on päällä". Palvelu käynnistyy sovellusta avattaessa, puhelimen käynnistyessä ja sovelluksen päivittyessä (`detection/StartReceiver.kt`).
2. **Päivän aika** (`detection/UsageTimer.kt`, yksikkötestit). Jokainen sekunti, jonka valvottu sovellus on edessä, kasvattaa sen päivän aikaa. Sovelluksilla on omat ajat (oletus 30 min, asetuksissa 0 h – 12 h 5 minuutin välein). Keskiyöllä kaikki alkaa alusta. Kun palvelu käynnistyy kesken päivän, päivän aika luetaan Androidin käyttötapahtumista keskiyöstä alkaen (`detection/Foreground.kt`, `secondsInFront`), joten uudelleenkäynnistys ei nollaa sitä.
3. **Taso 1** heti, kun sovelluksen päivän aika täyttyy: banneri sovelluksen päällä (*Lopeta tähän* / *Jatka*) + hälytys (ääni ja värinä 1,5 s välein, kunnes kuitataan tai 15 s). Päivän aika 0 = muistutus heti sovelluksen avautuessa.
4. **Taso 2**, jos käyttö jatkuu bannerin jälkeen 30 s (asetuksista 15 s – 2 min, demotila 10 s): koko näytön rauhallinen tehtävä (hengitys, kysymys tai liike), *Ohita* 5 s jälkeen.
5. **Seuraava muistutus**: tason 2 jälkeen, tai kun käyttäjä valitsee tasolla 1 *Lopeta tähän*, taso 1 tulee uudelleen Ajoitus-asetuksen välein (1–15 min käyttöä, demotilassa 30 s). Aika lasketaan vain, kun sovellus on edessä.
6. **Lopetus** (tasolla 1 tai 2) avaa kiitosnäkymän, ja *Sulje* vie puhelimen aloitusnäyttöön.
7. **Loki** tallentuu puhelimeen. Tilastot ja JSON-vienti jakovalikon kautta.

Banneri ja taso 2 tarvitsevat oikeuden "Näytä muiden sovellusten päällä" (`SYSTEM_ALERT_WINDOW`). Banneri on `TYPE_APPLICATION_OVERLAY`-ikkuna, ja sama oikeus sallii palvelun avata tason 2 näkymän (`CalmActivity`) taustalta. Pankkisovellukset piilottavat tällaiset ikkunat omien näkymiensä päältä, mutta eivät estä toimintaansa.

**Miksi ei Accessibility Serviceä (N21):** aiempi versio tunnisti lyhytvideoiden pyyhkäisyt esteettömyyspalvelulla. Pankkisovellukset kieltäytyivät toimimasta, kun palvelu oli päällä, Play Protect varoitti asennuksesta tai esti sen, ja Android 13+ vaati "rajoitettujen asetusten" sallimisen. Pyyhkäisyjen tunnistus ei myöskään vastannut tavoitetta: ajan pitää kulua, kun sovellusta käytetään, vierittipä käyttäjä tai ei (esim. pelit eivät lähetä vieritystapahtumia lainkaan).

## Asetukset ja tapahtumat

- Asetukset: `{ intervalMin, level2AfterSec, tasks, alarmMode: "untilDismissed"|"timed", sound, vibration, demoMode, apps, interruptionsOn, tester }`. `apps` on valvotut sovellukset: paketin nimi, nimi ja päivän aika minuutteina (tallennettuna `paketti<TAB>minuutit<TAB>nimi`). `intervalMin` on muistutusten väli päivän ajan täytyttyä (1, 3, 5, 10 tai 15 min). `level2AfterSec` on aika tasolta 1 tasolle 2 (15, 30, 60 tai 120 s). Demotilassa jokaisen sovelluksen päivän aika on 15 s, taso 2 tulee 10 s myöhemmin ja muistutus toistuu 30 s välein. `interruptionsOn = false` on tauko: mitään ei lasketa eikä keskeytetä. `tester` on käynnissä olevan RT8-testin testaajan numero (N19), 0 = testi ei ole käynnissä. Tallennus: `data/SettingsStore.kt` (SharedPreferences). Ennen N21:tä tallennetut `watchedApps` ja `customApps` jätetään lukematta, ja tilalle tulevat oletussovellukset.
- Tapahtuma: `{ ts, app, type: "level1"|"level2"|"outcome", level?, task, outcome: "stopped"|"continued"|"skipped"|"dismissed"|null, scrollSeconds, answer, tester }`. `ts` on millisekunteina. `scrollSeconds` on N21:stä alkaen sovelluksen päivän aika sekunteina tapahtumahetkellä (nimi on säilytetty, jotta vanhat ja uudet lokit luetaan samalla tavalla). `dismissed` = käyttäjä poistui sovelluksesta keskeytyksen aikana (Koti, sovelluksen vaihto, näyttö pois). `tester` = testaajan numero asetuksista (N19). Vanhoissa riveissä sitä ei ole, ja ne luetaan nollana. Tallennus: `data/EventLog.kt` (`events.jsonl` sovelluksen omassa tallennustilassa).

## Tiedostorakenne (kasvaa tehtävien mukana)

```
app/src/main/java/io/github/jessesaarinen/distractionkiller/
  DistractionKillerApp.kt  # N11: lataa asetukset ja lokin ennen muuta
  MainActivity.kt          # käynnistys, näkymien vaihto, oikeuksien tarkistus, palvelun käynnistys
  Config.kt                # kaikki ajat, vaihtoehdot ja oletussovellukset (vrt. web-demon config.js)
  ui/HomeScreen.kt         # kotinäkymä: ajanseurannan tila ja käyttöönotto-opas (N12, N21)
  ui/InspectorScreen.kt    # N4/N21: tarkastelunäkymä kehittäjille (sovellus edessä, päivän ajat, loki)
  ui/SettingsScreen.kt     # N10: asetusnäkymä
  ui/AppPicker.kt          # N20: sovelluksen valinta valvottavaksi (vain nimet)
  ui/AllowancePicker.kt    # N21: päivän ajan valinta (tunnit ja minuutit)
  ui/StatsScreen.kt        # N11: tilastot ja lokin jako
  ui/theme/Theme.kt        # kaikki värit, fontit ja kulmat (ks. DESIGN.md)
  ui/components/SunMark.kt # N18: tunnus, aurinko nousee horisontin yli
  ui/components/Layout.kt  # N18: alanäkymän runko, kortit, otsakkeet, palkit, tauon tausta
  ui/components/Controls.kt # N18: napit, valitsimet ja kytkinrivit
  ui/components/WheelPicker.kt # N21: pystysuora numerokaruselli
  detection/Permissions.kt         # N21: käyttötietojen, muiden päällä näyttämisen ja akun tarkistus
  detection/UsageWatcherService.kt # N21: etualapalvelu, joka lukee käyttötapahtumat kerran sekunnissa
  detection/StartReceiver.kt       # N21: käynnistää palvelun puhelimen käynnistyttyä ja päivityksen jälkeen
  detection/Foreground.kt          # N21: mikä sovellus on edessä ja kuinka kauan (yksikkötestit)
  detection/UsageTimer.kt          # N21: päivän ajat ja muistutusten ajoitus (yksikkötestit)
  detection/InspectorLog.kt        # N4: tarkastelun loki (inspector.log, 500 riviä) ja tila, myös jaettavaan tekstiin
  interrupt/InterruptController.kt # N7: näyttää keskeytykset ja toimii valinnan mukaan
  interrupt/OverlayWindow.kt       # N7: Compose-ikkuna muiden sovellusten päälle
  interrupt/Level1Banner.kt        # N7: tason 1 banneri
  interrupt/Alarm.kt, Chime.kt     # N7: hälytys (värinä + koodissa tehty ääni)
  interrupt/CalmActivity.kt        # N9: tason 2 tauko ja kiitosnäkymä omana Activitynä
  interrupt/Level2Screen.kt        # N9: tason 2 näkymä, Ohita 5 s jälkeen
  interrupt/DoneScreen.kt          # N9: kiitosnäkymä lopettamisen jälkeen
  interrupt/tasks/                 # N9: hengitys-, kysymys- ja liiketehtävä
  ui/Format.kt                     # kestot tekstiksi ("1 min 30 s", "1 h 30 min")
  data/Settings.kt                 # N10: asetukset, oletusarvot ja valvotut sovellukset
  data/SettingsStore.kt            # N10: ainoa paikka, joka tallentaa asetukset puhelimeen
  data/LogEvent.kt                 # N11: yksi lokirivi
  data/EventLog.kt                 # N11: ainoa paikka, joka tallentaa lokin; JSON-vienti
  data/Stats.kt                    # N11: tilastojen laskenta lokista; N19: päätöksen nopeus, testaajien numerot
app/src/main/res/values/strings.xml   # kaikki käyttöliittymän tekstit
app/src/main/res/drawable/ic_*.xml    # N18: viivaikonit (24 dp, värjätään käyttöpaikassa) ja sovelluksen kuvake
app/src/main/res/font/                # N18: otsikkofontti Fraunces
app/src/main/assets/licenses/         # N18: fontin lisenssi (SIL OFL)
app/src/test/java/…                   # yksikkötestit (ajetaan CI:ssä ennen buildia)
.github/workflows/build.yml           # yksikkötestit, APK-build ja julkaisut
TESTING.md                            # puhelimella testattavat asiat ennen tagia
DESIGN.md                             # N18: ulkoasun säännöt (värit, fontit, ikonit, komponentit)
docs/design/                          # N18: kuvat näkymistä ennen ja jälkeen uudistuksen; N19: testaajat ja päätöksen nopeus
```

## Tehtävät

Tila: ☐ ei aloitettu · ◐ kesken · ☑ valmis

**M0 – Repo ja APK-build (27.–28.10.)**
- ☑ N1 Gradle-projektin runko, kotinäkymä, `CLAUDE.md`, `AGENTS.md`, `PLAN.md`
- ☑ N2 GitHub Actions: APK-build, `latest`-julkaisu, kiinteä allekirjoitusavain
- ◐ N3 Asennus S21:lle ja tarkistus, että uusi build asentuu vanhan päälle. Asennettu ja käytetty 26.10., päivitys vanhan päälle tarkistamatta (`TESTING.md`, kohta 1)

**M1 – Tunnistuksen tarkistuspiste (29.–31.10.)**
- ☑ N4 Accessibility Service + tarkastelunäkymä, joka listaa paketit, näkymä-ID:t ja tapahtumatyypit. Korvattu N21:ssä (tarkastelunäkymä jäi, sisältö vaihtui)
- ✕ N5 Lyhytvideonäkymien merkit. Poistettu N21:ssä: pyyhkäisyjä ei enää tunnisteta

**M2 – Ajastin ja taso 1 (1.–2.11.)**
- ☑ N6 Aktiivisen ajan ajastin (60 s pyyhkäisysääntö). Korvattu N21:ssä päivän ajalla
- ☑ N7 Tason 1 banneri + hälytys (ääni, värinä, molemmat kestot)
- ☑ N8 *Lopeta tähän* → Koti, *Jatka* → tason 2 seuranta

**M3 – Taso 2, asetukset ja tilastot (3.–4.11.)**
- ☑ N9 Taso 2: hengitys, kysymys, liike, *Ohita* 5 s jälkeen. Kiitosnäkymä, jonne myös tason 1 *Lopeta tähän* vie
- ☑ N10 Asetukset: väli, tason 2 aika, tehtävät, hälytys, ääni, värinä, demotila, valvotut sovellukset, keskeytysten tauko
- ☑ N11 Loki, tilastonäkymä, JSON-vienti jakovalikolla
- ☑ N12 Kotinäkymään käyttöönotto-opas ja palvelun tila

**RT8 (5.11.)**
- ☑ N18 Ulkoasun uudistus: uusi teema (värit, Fraunces-otsikot, kulmat), viivaikonit, aurinkotunnus ja sovelluksen kuvake, kaikki näkymät uudelleen vaaleana ja tummana. Säännöt `DESIGN.md`:ssä. Puhelimella 27.10. (0.1.62) käyty läpi banneri, tauko hengitys- ja liiketehtävällä, kiitosnäkymä ja tilastot. Kuvake, tumma tila, suurin fontti ja kysymystehtävän näppäimistö (`TESTING.md`, kohta 7) tarkistettu vain koodista ja kuvista, ei puhelimella
- ☑ N19 RT8-testaajat ja päätöksen nopeus: tilastoissa *Aloita testaaja N*, testaajan numero jokaiseen tapahtumaan ja vientiin, tilastot testaajittain tai kaikki. Uusi kortti *Päätöksen nopeus*: aika bannerin tulosta *Jatka*- ja *Lopeta tähän* -valintaan (mediaani) ja kuinka usein *Ohita* painettiin heti (`Config.QUICK_SKIP_SECONDS`). Puhelimella 27.10. (0.1.62) tarkistettu testaajat 1 ja 2, testaajan numero viennissä sekä *Jatka*- ja *Lopeta tähän* -ajat. *Ohita heti* (`TESTING.md`, kohta 6) tarkistettu vain yksikkötestillä
- ☐ N13 Tagi `rt8`, sovellusten automaattipäivitykset pois testipuhelimesta (lista `TESTING.md`:n lopussa)

**Messuversio (13.–25.11.)**
- ☐ N14 RT8-palautteen perusteella tehtävät muutokset
- ☐ N15 Messutila: automaattinen nollaus, nopea vaihto demoaikoihin
- ☐ N16 Viimeistely, saavutettavuus, tilastojen visualisointi
- ☐ N17 Tagi `demo` 25.11.
- ☑ N20 Omat valvotut sovellukset: *Lisää sovellus* asetuksissa (lista puhelimen sovelluksista nimillä, haku, poisto). Yhdistetty N21:een, jossa lisätyt sovellukset lasketaan samoin kuin oletussovellukset
- ◐ N21 Uusi arkkitehtuuri: Accessibility Service ja pyyhkäisyjen tunnistus pois, tilalle käyttötiedot (`UsageStatsManager`) etualapalvelussa, "Näytä muiden sovellusten päällä" bannerille ja tasolle 2, ja jokaiselle sovellukselle oma päivän aika (tunti- ja minuuttikaruselli). Yksikkötestit lisätty. Emulaattorilla (Android 15) 10.11. tarkistettu: käyttöönotto-opas ja oikeuksien tunnistus, palvelu käynnistyy, YouTubessa demotilassa taso 1 15 s ja taso 2 10 s myöhemmin, taso 2 aukeaa taustalta, uusi muistutus 30 s kuluttua, *Lopeta tähän* → kiitosnäkymä, päivän aika säilyy sovelluksen pakotetun sulkemisen yli, päivän ajan valinta tallentuu. S21:llä tarkistamatta (`TESTING.md`), erityisesti: varoittaako Play Protect asennuksessa, toimivatko pankkisovellukset, sallitaanko oikeudet ilman "rajoitettuja asetuksia" ja pysyykö palvelu käynnissä

## Riskit

- **Aikataulu messuihin on tiukka.** N21 muutti tunnistuksen kokonaan, joten `TESTING.md` pitää käydä S21:llä läpi ennen `demo`-tagia.
- **Taso 2 ei aukea**, jos Android estää taustalta avatun näkymän. "Näytä muiden sovellusten päällä" on Androidin sallima poikkeus. Tarkastelussa näkyy `NOTE level 2 screen opened`, kun näkymä aukesi.
- **Samsung pysäyttää palvelun taustalla.** Akunkäyttö "Rajoittamaton". Palvelu on etualapalvelu ja käynnistyy uudelleen (`START_STICKY`, puhelimen käynnistys, sovelluksen avaus).
- **Käyttötapahtumat myöhästyvät** joillakin puhelimilla. Silloin sovelluksen vaihto näkyy sekunnin tai kaksi myöhässä, mikä ei haittaa minuuttien aikoja.
- **Testaajat eivät voi asentaa omiin puhelimiinsa.** RT8-testit tehdään Jessen puhelimella. N21:n jälkeen asennus omiin puhelimiin pitäisi onnistua, koska esteettömyyspalvelua ei enää ole.

## Tunnetut rajoitukset

- Ei Play Kauppaan (`specialUse`-etualapalvelu ja käyttötiedot vaatisivat Googlen tarkastuksen). Asennus vain APK:na.
- Jos puhelin sanoo, että asetus on rajoitettu, sovelluksen tiedoista pitää sallia "rajoitetut asetukset". Käyttötietoja ja muiden päällä näyttämistä tämä ei koske ainakaan emulaattorilla, mutta Samsung voi toimia toisin.
- Ilmoitus "Ajanseuranta on päällä" näkyy aina, kun palvelu toimii (Androidin vaatimus etualapalvelulle). Android 13+:ssa se näkyy vain, jos sovellukselle on sallittu ilmoitukset. Palvelu toimii ilman sitäkin.
- Ääni ja värinä voivat vaimentua äänettömällä tilalla tai virransäästössä.
