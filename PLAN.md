# PLAN – Distraction Killer (Android)

## Tavoite

Natiivi Android-sovellus, joka huomaa lyhytvideoiden selauksen TikTokissa, Instagram Reelsissä ja YouTube Shortsissa ja keskeyttää sen. Sama kahden tason keskeytys kuin web-demossa, mutta oikeiden sovellusten päällä. Testipuhelin: Samsung Galaxy S21 (Jesse). Asennus APK:na, ei Play Kauppaa.

**Aikataulu:** RT8-versio jäädytetään **5.11.** (tagi `rt8`), messuversio **25.11.** (tagi `demo`), messut 27.11.

## Miten se toimii

1. **Accessibility Service** saa tapahtumat valvotuista sovelluksista. Se lukee vain paketin nimen, näkymien ID:t ja tapahtuman tyypin.
2. **Lyhytvideonäkymän tunnistus** (merkit tiedostossa `detection/AppMarkers.kt`, tarkistetaan tehtävässä N5):

   | Sovellus | Paketti | Tunnistus |
   | --- | --- | --- |
   | TikTok | `com.zhiliaoapp.musically` | Videosyöte: pystysuora vieritys näkymässä `viewpager`. Sivuttainen vieritys (välilehdet) ja muut listat eivät laske. |
   | Instagram Reels | `com.instagram.android` | Vain Reels: vieritys näkymässä `clips_viewer_view_pager`. Etusivun syöte `list` ei laske. |
   | YouTube Shorts | `com.google.android.youtube` | Vain Shorts: vieritys näkymässä `reel_recycler`. Shorts on edessä, kun `reel_recycler` tai `reel_watch_fragment_root` näkyy ruudulla. Etusivu ja tavallinen video eivät laske. |

   Merkit on katsottu tarkastelun lokista Jessen S21:llä 26.10. Luokittelu on tiedostossa `detection/ShortVideoClassifier.kt`, ja sillä on yksikkötestit.

   Pyyhkäisy on vieritystapahtuma lyhytvideosyötteessä. TikTok ja Instagram kertovat, paljonko näkymä liikkui: jos siirtymä on nolla (`dy=0`), lista vain latautui, eikä sitä lasketa. YouTuben listat ilmoittavat aina `dy=0`, joten YouTubessa jokainen `reel_recycler`-tapahtuma lasketaan. Lisäksi palvelu katsoo kerran sekunnissa, mikä Short on ruudulla, ja uusi Short lasketaan pyyhkäisyksi (`ITEM`-rivi), vaikka YouTube ei lähettäisi siitä vieritystapahtumaa. Kun käyttäjä vierittää samassa sovelluksessa jotain muuta (esim. Instagramin etusivua), lyhytvideot eivät ole enää edessä. YouTubessa sen ratkaisee Shorts-soitin ruudulla. Tarkastelunäkymä näyttää jokaisen vierityksen tuloksen (`→ swipe`, `→ other`, `→ layout`) ja `FRONT`-rivin, kun lyhytvideot tulevat eteen tai poistuvat.

3. **Ajastin** laskee sekunnin, kun lyhytvideonäkymä on edessä, näyttö on päällä ja käyttäjä on pyyhkäissyt viimeisen 60 s aikana. 10 minuutin tauko aloittaa laskennan alusta.
4. **Taso 1**, kun väli täyttyy: banneri sovelluksen päällä (*Lopeta tähän* / *Jatka*) + hälytys (ääni ja värinä 1,5 s välein, kunnes kuitataan tai 15 s).
5. **Taso 2**, jos selaus jatkuu bannerin jälkeen 30 s (asetuksista 15 s – 2 min, demotila 10 s): koko näytön rauhallinen tehtävä (hengitys, kysymys tai liike), *Ohita* 5 s jälkeen.
6. **Lopetus** (tasolla 1 tai 2) avaa kiitosnäkymän, ja *Sulje* vie puhelimen aloitusnäyttöön. Jatkaminen ja ohittaminen nollaavat ajastimen.
7. **Loki** tallentuu puhelimeen. Tilastot ja JSON-vienti jakovalikon kautta.

Tason 1 banneri piirretään Accessibility Servicen omaan ikkunaan, joten "Näytä muiden sovellusten päällä" -oikeutta ei tarvita. Taso 2 ja kiitosnäkymä ovat oma Activity (`CalmActivity`), jonka palvelu avaa: video pysähtyy taustalle ja näppäimistö toimii kysymystehtävässä.

## Asetukset ja tapahtumat

- Asetukset: `{ intervalMin, level2AfterSec, tasks, alarmMode: "untilDismissed"|"timed", sound, vibration, demoMode, watchedApps, interruptionsOn, tester }`. `level2AfterSec` on aika tasolta 1 tasolle 2 (15, 30, 60 tai 120 s, demotilassa aina 10 s). `watchedApps` sisältää sovellusten nimet (`TikTok`, `Instagram`, `YouTube`). `interruptionsOn = false` on tauko: mitään ei lasketa eikä keskeytetä, mutta tarkastelu näyttää tapahtumat. `tester` on käynnissä olevan RT8-testin testaajan numero (N19), 0 = testi ei ole käynnissä. Tallennus: `data/SettingsStore.kt` (SharedPreferences).
- Tapahtuma: `{ ts, app, type: "level1"|"level2"|"outcome", level?, task, outcome: "stopped"|"continued"|"skipped"|"dismissed"|null, scrollSeconds, answer, tester }`. `ts` on millisekunteina. `dismissed` = käyttäjä poistui sovelluksesta keskeytyksen aikana (Koti, sovelluksen vaihto, näyttö pois). `tester` = testaajan numero asetuksista (N19). Vanhoissa riveissä sitä ei ole, ja ne luetaan nollana. Tallennus: `data/EventLog.kt` (`events.jsonl` sovelluksen omassa tallennustilassa).

## Tiedostorakenne (kasvaa tehtävien mukana)

```
app/src/main/java/io/github/jessesaarinen/distractionkiller/
  DistractionKillerApp.kt  # N11: lataa asetukset ja lokin ennen muuta
  MainActivity.kt          # käynnistys, näkymien vaihto
  Config.kt                # kaikki ajat ja vaihtoehdot (vrt. web-demon config.js)
  ui/HomeScreen.kt         # kotinäkymä: tunnistuksen tila ja käyttöönotto-opas (N12)
  ui/InspectorScreen.kt    # N4: tarkastelunäkymä kehittäjille
  ui/SettingsScreen.kt     # N10: asetusnäkymä
  ui/StatsScreen.kt        # N11: tilastot ja lokin jako
  ui/DayChart.kt           # N16: tilastojen pylväät viimeisiltä 7 päivältä, päivän luvut napauttamalla
  ui/theme/Theme.kt        # kaikki värit, fontit ja kulmat (ks. DESIGN.md)
  ui/components/SunMark.kt # N18: tunnus, aurinko nousee horisontin yli
  ui/components/Layout.kt  # N18: alanäkymän runko, kortit, otsakkeet, palkit, tauon tausta
  ui/components/Controls.kt # N18: napit, valitsimet ja kytkinrivit
  detection/AppMarkers.kt          # valvotut sovellukset ja niiden lyhytvideomerkit (N5)
  detection/ShortVideoClassifier.kt # N5: mitä vieritystapahtuma tarkoittaa (yksikkötestit)
  detection/ScrollWatcherService.kt # N4: Accessibility Service
  detection/InspectorLog.kt        # N4: tarkastelun loki (inspector.log, 500 riviä) ja tila, myös jaettavaan tekstiin
  detection/ScrollTimer.kt         # N6: aktiivisen ajan ajastin
  interrupt/InterruptController.kt # N7: näyttää keskeytykset ja toimii valinnan mukaan
  interrupt/OverlayWindow.kt       # N7: Compose-ikkuna muiden sovellusten päälle
  interrupt/Level1Banner.kt        # N7: tason 1 banneri
  interrupt/Alarm.kt, Chime.kt     # N7: hälytys (värinä + koodissa tehty ääni)
  interrupt/CalmActivity.kt        # N9: tason 2 tauko ja kiitosnäkymä omana Activitynä
  interrupt/Level2Screen.kt        # N9: tason 2 näkymä, Ohita 5 s jälkeen
  interrupt/DoneScreen.kt          # N9: kiitosnäkymä lopettamisen jälkeen
  interrupt/tasks/                 # N9: hengitys-, kysymys- ja liiketehtävä
  ui/Format.kt                     # kestot tekstiksi ("1 min 30 s")
  data/Settings.kt                 # N10: asetukset ja oletusarvot
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
- ☑ N4 Accessibility Service + tarkastelunäkymä, joka listaa paketit, näkymä-ID:t ja tapahtumatyypit
- ◐ N5 Selataan jokaista sovellusta minuutti, valitaan merkit, tarkistetaan että pyyhkäisyt näkyvät. Merkit valittu kaikille kolmelle. Puhelimella tarkistamatta: näkyvätkö Shortsin pyyhkäisyt `→ swipe`-riveinä
- ☑ Päätös sovelluksittain: kaikissa kolmessa lasketaan vain lyhytvideosyöte (TikTokin videosyöte, Reels, Shorts)

**M2 – Ajastin ja taso 1 (1.–2.11.)**
- ☑ N6 Aktiivisen ajan ajastin (60 s pyyhkäisysääntö)
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

## Riskit

- **Aikataulu RT8:aan on tiukka.** Karsintajärjestys: tilastonäkymä, kysymys- ja liiketehtävä, asetukset. Hengitys + taso 1 riittää arvolupauksen testaamiseen.
- **Sovelluspäivitys muuttaa näkymä-ID:t.** Automaattipäivitykset pois ennen 5.11. ja 25.11. Merkit ovat yhdessä tiedostossa.
- **Taso 2 ei aukea**, jos Android estää taustalta avatun näkymän. Accessibility Service on sallittu poikkeus, ja varmuuden vuoksi ajastin palaa alkuun 10 minuutin tauon jälkeen myös tasolla 2. Tarkastelussa näkyy `NOTE level 2 screen opened`, kun näkymä aukesi.
- **Samsung pysäyttää palvelun taustalla.** Akunkäyttö "Rajoittamaton", kotinäkymä näyttää palvelun tilan.
- **Testaajat eivät voi asentaa omiin puhelimiinsa.** RT8-testit tehdään Jessen puhelimella.

## Tunnetut rajoitukset

- Ei Play Kauppaan (Googlen Accessibility-käytäntö). Asennus vain APK:na.
- Android 13+: sivuladatulle sovellukselle pitää sallia "rajoitetut asetukset" ennen Esteettömyys-kytkintä.
- Ääni ja värinä voivat vaimentua äänettömällä tilalla tai virransäästössä.
