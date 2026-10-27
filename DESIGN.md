# DESIGN – Distraction Killer (Android)

Ulkoasun säännöt. Lue tämä ennen kuin muutat näkymiä. Värit, fontit ja muodot ovat tiedostossa `ui/theme/Theme.kt` ja yhteiset palat kansiossa `ui/components/`. Kuvat näkymistä ennen ja jälkeen uudistuksen (N18) ovat kansiossa `docs/design/`.

## Idea: kaksi maailmaa

- Videosovellukset ovat kovaäänisiä ja kirkkaita. Oma sovellus on niiden vastakohta: lämmin paperi, tumma muste, petroolinvihreä ja persikanvärinen aurinko. Rauhallinen, ei saarnaava.
- **Tunnus:** aurinko nousee horisontin yli, eli "nosta katse puhelimesta". Komponentti `SunMark` ja sovelluksen kuvake `res/drawable/ic_launcher_foreground.xml` (kuvakkeessa on lisäksi säteet, jotta se erottuu pienenä).
- Aurinko kertoo tarinaa: muistutuksessa (taso 1) se on vasta nousemassa, kiitosnäkymässä se nousee kokonaan, ja hengitystehtävässä se kasvaa ja pienenee hengityksen tahdissa.

## Värit

Näkymät käyttävät vain värirooleja (`MaterialTheme.colorScheme.<rooli>`), eivät värikoodeja. Uusi väri lisätään `Theme.kt`:hen, sekä vaaleaan että tummaan teemaan.

| Rooli | Vaalea | Tumma | Käyttö |
| --- | --- | --- | --- |
| `primary` | Petrol `#2F6F6A` | `#86C5BC` | Napit, valinnat, edistyspalkit, korostettu teksti |
| `primaryContainer` | `#D8E9E5` | `#1D3D3A` | Valittu vaihtoehto, päällä oleva pääkytkin |
| `secondary` | Persikka `#E8A87C` | `#F0B48A` | Aurinko ja pienet koristeet. **Ei tekstin väriksi** |
| `secondaryContainer` | `#F9E3D3` | `#4A3020` | Lämmin huomio: tauko, demotila, liiketehtävän kuva |
| `tertiary` | `#24524E` | `#1D3D3A` | Isot "sankarikortit": kotinäkymän tila ja lopetusprosentti |
| `background` | Paperi `#F6F3EE` | `#111A1C` | Näkymän tausta |
| `surface` | `#FFFDFA` | `#1A2528` | Kortit |
| `surfaceVariant` | Hiekka `#EDE6DB` | `#243134` | Hiljaiset täytöt: palkkien pohjat, valitsimen pohja |
| `onSurface` / `onBackground` | Muste `#1F2A2E` | `#ECE7E0` | Teksti |
| `onSurfaceVariant` | `#56636A` | `#A7B3B5` | Toissijainen teksti ja vihjeet |
| `outlineVariant` | `#E6DFD4` | `#2A3639` | Korttien reunaviiva |
| `outline` | `#9A9184` | `#5A686B` | Reunallisen napin reuna, tilastokaavion muut keskeytykset |
| `error` | Savi `#A8432F` | `#F0A08E` | Vain tuhoavat toiminnot, kuten *Tyhjennä* |

Säännöt:

- **Kontrasti:** teksti vähintään 4,5:1, iso teksti (24 sp tai isompi) ja ikonit vähintään 3:1. Mitattuja: muste paperilla 13,3:1, toissijainen teksti paperilla 5,6:1 ja hiekalla 5,0:1, napin teksti petrolilla 5,3:1. Tummassa teemassa kaikki tekstiparit ovat yli 7:1.
- **Persikka on koriste.** Vaalealla taustalla sen kontrasti (1,8:1) ei riitä tekstille. Tekstiin käytetään `primary`- tai `onSurfaceVariant`-väriä.
- **Punainen vain tuhoaville toiminnoille.** Tilaviestit eivät ole virheitä: jos tunnistus ei ole päällä, se ei ole käyttäjän vika, joten se näytetään rauhallisesti.
- **Tumma teema on oma suunnitelmansa**, ei käännetty vaalea. Tarkista jokainen muutos myös tummana.
- Ei dynaamisia värejä (Material You). Sovelluksen ilme on osa konseptia.
- Ikkunan tausta ja kuvake ovat poikkeus: ne ovat tiedostoissa `res/values/colors.xml` ja `res/values-night/colors.xml`. Jos muutat paperin tai yön väriä, muuta ne myös sinne.

## Typografia

Otsikot ja isot numerot: **Fraunces**, pehmeä serif (`display*`- ja `headline*`-tyylit). Kaikki muu: puhelimen oma fontti, Roboto (`title*`, `body*`, `label*`).

| Tyyli | Koko | Käyttö |
| --- | --- | --- |
| `displayLarge` | 64 sp | Lopetusprosentti |
| `displayMedium` | 44 sp | Hengityksen sekunnit |
| `displaySmall` | 36 sp | Kotinäkymän otsikko, kiitosnäkymä |
| `headlineLarge` | 32 sp | Näkymien otsikot, tason 2 tehtävät |
| `headlineMedium` | 28 sp | Tilastojen luvut, liiketehtävä |
| `headlineSmall` | 24 sp | Korttien otsikot, tason 1 banneri |
| `titleMedium` | 16 sp | Asetusten ja valintojen nimet |
| `bodyLarge` / `bodyMedium` | 16 / 14 sp | Leipäteksti |
| `bodySmall` | 13 sp | Vihjeet (`Hint`) |
| `labelLarge` | 15 sp | Napit |
| `labelMedium` | 12 sp | PIENET YLÄOTSAKKEET (`SectionLabel`), aina isoin kirjaimin |

- Käytä aina teeman tyylejä. Älä kirjoita `fontSize = 18.sp` näkymiin.
- Tekstit skaalautuvat puhelimen fonttikoon mukaan. Suomenkieliset sanat ovat pitkiä, joten anna tekstin rivittyä, äläkä katkaise sitä (`maxLines = 1`) ilman syytä.
- Kun rivillä on teksti ja sen vieressä jotain muuta (esim. "0/3 valmis" tai versionumero), käytä `FlowRow`-asettelua. Isolla fonttikoolla jälkimmäinen siirtyy silloin omalle rivilleen eikä purista tekstiä kapeaksi. Kiinteä korkeus (`height(48.dp)`) leikkaa isoa tekstiä, joten käytä `heightIn(min = 48.dp)`.
- Fonttitiedosto `res/font/fraunces_medium.ttf` on Fraunces-muuttuvafontista tehty kiinteä versio (paino 500, opsz 72, SOFT 100, WONK 0). Lisenssi on SIL Open Font License 1.1: `app/src/main/assets/licenses/fraunces-ofl.txt`. Uusia fontteja ei lisätä ilman ihmisen hyväksyntää.

## Muodot ja välit

- Kulmat (`MaterialTheme.shapes`): `extraSmall` 8 dp, `small` 12 dp, `medium` 16 dp (valinnat, tekstikentät), `large` 24 dp (kortit), `extraLarge` 32 dp (sankarikortit, tason 1 banneri). Napit ovat pyöreäpäisiä pillereitä.
- Välit 4 dp:n askelin. Näkymän reunat 20 dp (taso 2 ja kiitosnäkymä 24 dp), korttien sisällä 20 dp sivuilla ja 16 dp ylhäällä ja alhaalla, korttien välissä 12 dp.
- Kosketusalueet vähintään 48 dp. Pääpainike on 56 dp korkea.

## Ikonit

- `res/drawable/ic_*.xml`: 24 dp:n viivaikonit, viivan paksuus 2, pyöreät päät ja kulmat, ei täyttöä. Ne on piirretty mustalla ja värjätään käyttöpaikassa: `Icon(painterResource(R.drawable.ic_bell), contentDescription = null, tint = …)`.
- Uusi ikoni tehdään samalla tyylillä. Ei brändien logoja eikä emojeja (emojit näyttävät eri puhelimissa erilaisilta).
- Koristeikonin `contentDescription` on `null`, kun viereinen teksti kertoo saman. Pelkän ikonin napissa kuvaus tulee `strings.xml`:stä (esim. `back`).

## Komponentit

Käytä näitä ennen kuin teet uuden. Jos sama rakenne toistuu kolmessa paikassa, siirrä se tänne.

| Komponentti | Tiedosto | Käyttö |
| --- | --- | --- |
| `ScreenScaffold` | `ui/components/Layout.kt` | Alanäkymä: takaisin-nappi, iso otsikko, vierittyvä sisältö |
| `SectionLabel`, `SectionCard` | `Layout.kt` | Pieni yläotsake ja valkoinen kortti sen alla |
| `Hint` | `Layout.kt` | Vihjeteksti asetuksen tai otsikon alla |
| `IconBubble` | `Layout.kt` | Ikoni pehmeässä ympyrässä |
| `ProgressBar` | `Layout.kt` | Ohut pyöreä palkki (tilastot, käyttöönotto) |
| `CalmBackground` | `Layout.kt` | Taso 2 ja kiitosnäkymä: tausta, jonka alareunasta nousee lämmin hehku |
| `SideBySide` | `Layout.kt` | Kortit rinnakkain samankorkuisina (kotinäkymän laatat, tilastojen luvut). Isolla fonttikoolla allekkain, jos pitkä sana ei mahdu |
| `PrimaryButton` | `ui/components/Controls.kt` | Näkymän tai kortin yksi päätoiminto |
| `SecondaryButton` | `Controls.kt` | Hiljaisempi vaihtoehto päätoiminnon vieressä |
| `SegmentedChoice` | `Controls.kt` | Yksi valinta 2–5 lyhyestä vaihtoehdosta (esim. 1 / 3 / 5 / 10 / 15 min) |
| `SwitchRow` | `Controls.kt` | Päälle/pois-asetus koko rivin kokoisella kosketusalueella |
| `SelectRow`, `CheckCircle` | `Controls.kt` | Monivalinta, esim. tason 2 tehtävät |
| `SunMark` | `ui/components/SunMark.kt` | Tunnus. `rise` 0–1 kertoo, kuinka korkealla aurinko on |

## Näkymät lyhyesti

- **Koti:** tunnus ja otsikko, tilakortti (päällä: tumma petrolikortti ja aurinko, persikkamerkit demotilasta ja testaajasta; tauolla: persikkakortti ja nappi päälle; ei päällä: valkoinen kortti), käyttöönotto-opas kunnes kaikki vaiheet ovat valmiita, Asetukset- ja Tilastot-laatat, yksityisyysrivi.
- **Asetukset:** pääkytkin ylimpänä, sitten kortit Ajoitus, Keskeytyksen tehtävät, Hälytys, Valvotut sovellukset ja Esittely.
- **Tilastot:** lopetusprosentti isona, sitten luvut, viimeisten 7 päivän pylväät (`ui/DayChart.kt`: lopetukset petroolina, muut keskeytykset harmaana, 2 dp rako osien välissä) ja palkit. Testin aikana ylimpänä valinta *Testaaja N* / *Kaikki*. Testaajan vaihto ja lokin jako alimpana.
- **Taso 1 (banneri):** liukuu ylhäältä videon päälle. *Lopeta tähän* on isompi kuin *Jatka*.
- **Taso 2:** tehtävä keskellä, valintanapit alhaalla vasta tehtävän jälkeen. *Ohita* tulee näkyviin 5 s jälkeen.
- **Kiitosnäkymä:** aurinko nousee 1,6 sekunnissa.

## Liike

- Animaatiot ovat hitaita ja pehmeitä (noin 300–1600 ms). Ei pomppivia, vilkkuvia tai nopeita efektejä, koska tarkoitus on rauhoittaa.
- Hengitys: 4 s sisään ja 4 s ulos (`Config.BREATHING_PHASE_SECONDS`).

## Tarkistuslista ennen PR:ää

- [ ] Vain teeman värit, tyylit ja muodot. Ei `Color(0xFF…)`-arvoja tai `fontSize`-kokoja näkymissä.
- [ ] Tekstit `strings.xml`:ssä, suomeksi, ystävällisellä sävyllä.
- [ ] Tarkistettu vaaleana ja tummana (puhelimen Asetukset → Näyttö → Tumma tila).
- [ ] Tarkistettu isolla fonttikoolla (Asetukset → Näyttö → Fonttikoko ja -tyyli → suurin): mikään ei leikkaudu tai mene päällekkäin.
- [ ] Kosketusalueet vähintään 48 dp, ja pelkän ikonin napeilla on kuvaus.
- [ ] Kun ulkoasu muuttuu, PR:ään kuvakaappaukset ennen ja jälkeen.
