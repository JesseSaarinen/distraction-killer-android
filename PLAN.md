# PLAN – Distraction Killer (Android)

## Tavoite

Natiivi Android-sovellus, joka huomaa lyhytvideoiden selauksen TikTokissa, Instagram Reelsissä ja YouTube Shortsissa ja keskeyttää sen. Sama kahden tason keskeytys kuin web-demossa, mutta oikeiden sovellusten päällä. Testipuhelin: Samsung Galaxy S21 (Jesse). Asennus APK:na, ei Play Kauppaa.

**Aikataulu:** RT8-versio jäädytetään **5.11.** (tagi `rt8`), messuversio **25.11.** (tagi `demo`), messut 27.11.

## Miten se toimii

1. **Accessibility Service** saa tapahtumat valvotuista sovelluksista. Se lukee vain paketin nimen, näkymien ID:t ja tapahtuman tyypin.
2. **Lyhytvideonäkymän tunnistus** (merkit tiedostossa `detection/AppMarkers.kt`, tarkistetaan tehtävässä N5):

   | Sovellus | Paketti | Tunnistus |
   | --- | --- | --- |
   | TikTok | `com.zhiliaoapp.musically` | Koko sovellus on lyhytvideosyöte (ei vielä tarkistettu) |
   | Instagram Reels | `com.instagram.android` | Vain Reels: vieritys näkymässä `clips_viewer_view_pager` (tarkistettu 26.10.). Etusivun syöte `list` ei laske. |
   | YouTube Shorts | `com.google.android.youtube` | Shorts-näkymän ID (ehdokas: alkaa `reel_`). Kunnes se on tarkistettu, koko sovellus laskee. |

   Pyyhkäisy on vieritystapahtuma, jossa näkymä oikeasti liikkui. Lista lähettää vieritystapahtuman myös latautuessaan, mutta silloin siirtymä on nolla (`dy=0`), eikä sitä lasketa. Kun käyttäjä vierittää samassa sovelluksessa jotain muuta (esim. Instagramin etusivua), lyhytvideonäkymä ei ole enää edessä. Tarkastelunäkymä merkitsee lasketut pyyhkäisyt sanalla `swipe`.

3. **Ajastin** laskee sekunnin, kun lyhytvideonäkymä on edessä, näyttö on päällä ja käyttäjä on pyyhkäissyt viimeisen 60 s aikana. 10 minuutin tauko aloittaa laskennan alusta.
4. **Taso 1**, kun väli täyttyy: banneri sovelluksen päällä (*Lopeta tähän* / *Jatka*) + hälytys (ääni ja värinä 1,5 s välein, kunnes kuitataan tai 15 s).
5. **Taso 2**, jos selaus jatkuu bannerin jälkeen 30 s (demotila 10 s): koko näytön rauhallinen tehtävä (hengitys, kysymys tai liike), *Ohita* 5 s jälkeen.
6. **Lopetus** (tasolla 1 tai 2) avaa kiitosnäkymän, ja *Sulje* vie puhelimen aloitusnäyttöön. Jatkaminen ja ohittaminen nollaavat ajastimen.
7. **Loki** tallentuu puhelimeen. Tilastot ja JSON-vienti jakovalikon kautta.

Tason 1 banneri piirretään Accessibility Servicen omaan ikkunaan, joten "Näytä muiden sovellusten päällä" -oikeutta ei tarvita. Taso 2 ja kiitosnäkymä ovat oma Activity (`CalmActivity`), jonka palvelu avaa: video pysähtyy taustalle ja näppäimistö toimii kysymystehtävässä.

## Asetukset ja tapahtumat

- Asetukset: `{ intervalMin, tasks, alarmMode: "untilDismissed"|"timed", sound, vibration, demoMode, watchedApps }`. `watchedApps` sisältää sovellusten nimet (`TikTok`, `Instagram`, `YouTube`). Tallennus: `data/SettingsStore.kt` (SharedPreferences).
- Tapahtuma: `{ ts, app, type: "level1"|"level2"|"outcome", level?, task, outcome: "stopped"|"continued"|"skipped"|"dismissed"|null, scrollSeconds, answer }`. `dismissed` = käyttäjä poistui sovelluksesta keskeytyksen aikana (Koti, sovelluksen vaihto, näyttö pois). Tallennus: `data/EventLog.kt` (`events.jsonl` sovelluksen omassa tallennustilassa).

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
  ui/theme/Theme.kt        # kaikki värit
  detection/AppMarkers.kt          # valvotut sovellukset (N5: lyhytvideonäkymän merkit)
  detection/ScrollWatcherService.kt # N4: Accessibility Service
  detection/InspectorLog.kt        # N4: tarkastelun loki muistissa (ei tallennu)
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
  data/Stats.kt                    # N11: tilastojen laskenta lokista
app/src/main/res/values/strings.xml   # kaikki käyttöliittymän tekstit
.github/workflows/build.yml           # APK-build ja julkaisut
```

## Tehtävät

Tila: ☐ ei aloitettu · ◐ kesken · ☑ valmis

**M0 – Repo ja APK-build (27.–28.10.)**
- ☑ N1 Gradle-projektin runko, kotinäkymä, `CLAUDE.md`, `AGENTS.md`, `PLAN.md`
- ☑ N2 GitHub Actions: APK-build, `latest`-julkaisu, kiinteä allekirjoitusavain
- ☐ N3 Asennus S21:lle ja tarkistus, että uusi build asentuu vanhan päälle

**M1 – Tunnistuksen tarkistuspiste (29.–31.10.)**
- ☑ N4 Accessibility Service + tarkastelunäkymä, joka listaa paketit, näkymä-ID:t ja tapahtumatyypit
- ◐ N5 Selataan jokaista sovellusta minuutti, valitaan merkit, tarkistetaan että pyyhkäisyt näkyvät. Instagram valmis, TikTok ja YouTube odottavat tarkastelun lokia
- ◐ Päätös sovelluksittain: vain lyhytvideonäkymä vai koko sovellus. Instagram: vain Reels

**M2 – Ajastin ja taso 1 (1.–2.11.)**
- ☑ N6 Aktiivisen ajan ajastin (60 s pyyhkäisysääntö)
- ☑ N7 Tason 1 banneri + hälytys (ääni, värinä, molemmat kestot)
- ☑ N8 *Lopeta tähän* → Koti, *Jatka* → tason 2 seuranta

**M3 – Taso 2, asetukset ja tilastot (3.–4.11.)**
- ☑ N9 Taso 2: hengitys, kysymys, liike, *Ohita* 5 s jälkeen. Kiitosnäkymä, jonne myös tason 1 *Lopeta tähän* vie
- ☑ N10 Asetukset: väli, tehtävät, hälytys, ääni, värinä, demotila, valvotut sovellukset
- ☑ N11 Loki, tilastonäkymä, JSON-vienti jakovalikolla
- ☑ N12 Kotinäkymään käyttöönotto-opas ja palvelun tila

**RT8 (5.11.)**
- ☐ N13 Tagi `rt8`, sovellusten automaattipäivitykset pois testipuhelimesta

**Messuversio (13.–25.11.)**
- ☐ N14 RT8-palautteen perusteella tehtävät muutokset
- ☐ N15 Messutila: automaattinen nollaus, nopea vaihto demoaikoihin
- ☐ N16 Viimeistely, saavutettavuus, tilastojen visualisointi
- ☐ N17 Tagi `demo` 25.11.

## Riskit

- **Aikataulu RT8:aan on tiukka.** Karsintajärjestys: tilastonäkymä, kysymys- ja liiketehtävä, asetukset. Hengitys + taso 1 riittää arvolupauksen testaamiseen.
- **Sovelluspäivitys muuttaa näkymä-ID:t.** Automaattipäivitykset pois ennen 5.11. ja 25.11. Merkit ovat yhdessä tiedostossa.
- **Taso 2 ei aukea**, jos Android estää taustalta avatun näkymän. Accessibility Service on sallittu poikkeus, ja varmuuden vuoksi ajastin palaa alkuun 10 minuutin tauon jälkeen myös tasolla 2.
- **Samsung pysäyttää palvelun taustalla.** Akunkäyttö "Rajoittamaton", kotinäkymä näyttää palvelun tilan.
- **Testaajat eivät voi asentaa omiin puhelimiinsa.** RT8-testit tehdään Jessen puhelimella.

## Tunnetut rajoitukset

- Ei Play Kauppaan (Googlen Accessibility-käytäntö). Asennus vain APK:na.
- Android 13+: sivuladatulle sovellukselle pitää sallia "rajoitetut asetukset" ennen Esteettömyys-kytkintä.
- Ääni ja värinä voivat vaimentua äänettömällä tilalla tai virransäästössä.
