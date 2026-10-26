# PLAN – Distraction Killer (Android)

## Tavoite

Natiivi Android-sovellus, joka huomaa lyhytvideoiden selauksen TikTokissa, Instagram Reelsissä ja YouTube Shortsissa ja keskeyttää sen. Sama kahden tason keskeytys kuin web-demossa, mutta oikeiden sovellusten päällä. Testipuhelin: Samsung Galaxy S21 (Jesse). Asennus APK:na, ei Play Kauppaa.

**Aikataulu:** RT8-versio jäädytetään **5.11.** (tagi `rt8`), messuversio **25.11.** (tagi `demo`), messut 27.11.

## Miten se toimii

1. **Accessibility Service** saa tapahtumat valvotuista sovelluksista. Se lukee vain paketin nimen, näkymien ID:t ja tapahtuman tyypin.
2. **Lyhytvideonäkymän tunnistus** (merkit tiedostossa `detection/AppMarkers.kt`, tarkistetaan tehtävässä N5):

   | Sovellus | Paketti | Tunnistus |
   | --- | --- | --- |
   | TikTok | `com.zhiliaoapp.musically` | Koko sovellus on lyhytvideosyöte |
   | Instagram Reels | `com.instagram.android` | Reels-näkymän ID (ehdokas: sisältää `clips_viewer`) |
   | YouTube Shorts | `com.google.android.youtube` | Shorts-näkymän ID (ehdokas: alkaa `reel_`) |

3. **Ajastin** laskee sekunnin, kun lyhytvideonäkymä on edessä, näyttö on päällä ja käyttäjä on pyyhkäissyt viimeisen 60 s aikana. 10 minuutin tauko aloittaa laskennan alusta.
4. **Taso 1**, kun väli täyttyy: banneri sovelluksen päällä (*Lopeta tähän* / *Jatka*) + hälytys (ääni ja värinä 1,5 s välein, kunnes kuitataan tai 15 s).
5. **Taso 2**, jos selaus jatkuu bannerin jälkeen 30 s (demotila 10 s): koko näytön rauhallinen tehtävä (hengitys, kysymys tai liike), *Ohita* 5 s jälkeen.
6. **Lopetus** painaa Koti-näppäintä käyttäjän puolesta. Jatkaminen nollaa ajastimen.
7. **Loki** tallentuu puhelimeen. Tilastot ja JSON-vienti jakovalikon kautta.

Overlayt piirretään Accessibility Servicen omaan ikkunaan, joten "Näytä muiden sovellusten päällä" -oikeutta ei tarvita.

## Asetukset ja tapahtumat

- Asetukset: `{ intervalMin, tasks, alarmMode: "untilDismissed"|"timed", sound, vibration, demoMode, watchedApps }`
- Tapahtuma: `{ ts, app, type: "level1"|"level2"|"outcome", level?, task, outcome: "stopped"|"continued"|"skipped"|null, scrollSeconds, answer }`

## Tiedostorakenne (kasvaa tehtävien mukana)

```
app/src/main/java/io/github/jessesaarinen/distractionkiller/
  MainActivity.kt          # käynnistys, näkymien vaihto
  Config.kt                # kaikki ajat ja vaihtoehdot (vrt. web-demon config.js)
  ui/HomeScreen.kt         # kotinäkymä: tunnistuksen tila (N12: käyttöönotto-opas)
  ui/InspectorScreen.kt    # N4: tarkastelunäkymä kehittäjille
  ui/theme/Theme.kt        # kaikki värit
  detection/AppMarkers.kt          # valvotut sovellukset (N5: lyhytvideonäkymän merkit)
  detection/ScrollWatcherService.kt # N4: Accessibility Service
  detection/InspectorLog.kt        # N4: tarkastelun loki muistissa (ei tallennu)
  detection/ScrollTimer.kt         # N6: aktiivisen ajan ajastin
  interrupt/               # N7–N9: taso 1 ja taso 2 overlayt, hälytys, tehtävät
  data/                    # N10–N11: asetukset, loki, vienti
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
- ☐ N5 Selataan jokaista sovellusta minuutti, valitaan merkit, tarkistetaan että pyyhkäisyt näkyvät
- ☐ Päätös sovelluksittain: vain lyhytvideonäkymä vai koko sovellus

**M2 – Ajastin ja taso 1 (1.–2.11.)**
- ☑ N6 Aktiivisen ajan ajastin (60 s pyyhkäisysääntö)
- ☐ N7 Tason 1 banneri + hälytys (ääni, värinä, molemmat kestot)
- ☐ N8 *Lopeta tähän* → Koti, *Jatka* → tason 2 seuranta

**M3 – Taso 2, asetukset ja tilastot (3.–4.11.)**
- ☐ N9 Tason 2 overlay: hengitys, kysymys, liike, *Ohita* 5 s jälkeen
- ☐ N10 Asetukset: väli, tehtävät, hälytys, ääni, värinä, demotila, valvotut sovellukset
- ☐ N11 Loki, tilastonäkymä, JSON-vienti jakovalikolla
- ☐ N12 Kotinäkymään käyttöönotto-opas ja palvelun tila

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
- **Samsung pysäyttää palvelun taustalla.** Akunkäyttö "Rajoittamaton", kotinäkymä näyttää palvelun tilan.
- **Testaajat eivät voi asentaa omiin puhelimiinsa.** RT8-testit tehdään Jessen puhelimella.

## Tunnetut rajoitukset

- Ei Play Kauppaan (Googlen Accessibility-käytäntö). Asennus vain APK:na.
- Android 13+: sivuladatulle sovellukselle pitää sallia "rajoitetut asetukset" ennen Esteettömyys-kytkintä.
- Ääni ja värinä voivat vaimentua äänettömällä tilalla tai virransäästössä.
