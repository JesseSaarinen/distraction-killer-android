# Testaus puhelimella (Galaxy S21)

Käy nämä läpi uuden buildin jälkeen ja ennen `rt8`-tagia. Merkitse, mikä toimi, ja jaa tarkastelun loki, jos jokin ei toiminut. Tarkastelun lokissa on vain teknisiä tietoja (mikä valvottu sovellus oli edessä, päivän ajat, keskeytykset), ei tekstejä, videoita tai muiden sovellusten nimiä.

## 1. Asennus

- [ ] Asetukset → Turvallisuus ja yksityisyys → **Auto Blocker** pois. Muuten APK ei asennu.
- [ ] Avaa puhelimella [uusin versio](../../releases/tag/latest) ja lataa `distraction-killer.apk`. Asenna. Jos Play Protect varoittaa, valitse "Asenna silti".
- [ ] Avaa sovellus. Kotinäkymän alareunan versio (`0.1.N`) on sama kuin julkaisun build-numero.
- [ ] Uusi build asentuu vanhan päälle: asetukset, tilastot ja oikeudet säilyvät, ja ajanseuranta käynnistyy itsestään (ilmoitus "Ajanseuranta on päällä").
- [ ] N21: Play Protect ei varoita asennuksessa esteettömyyspalvelusta.

## 2. Käyttöönotto

Jos vanha versio (esteettömyyspalvelulla) on asennettu, uusi asentuu sen päälle. Vanha esteettömyysasetus katoaa itsestään.

- [ ] Kotinäkymä → *Avaa käyttötietojen asetukset* → Distraction Killer → Salli käyttötietojen käyttö → päälle.
- [ ] Kotinäkymä → *Avaa näyttö muiden päällä -asetus* → päälle.
- [ ] Kirjaa ylös: pyysikö puhelin sallimaan "rajoitetut asetukset" jommallekummalle. Jos pyysi: *Avaa sovelluksen tiedot* → ⋮ → Salli rajoitetut asetukset, ja uudelleen.
- [ ] *Avaa sovelluksen tiedot* → Akku → **Rajoittamaton**.
- [ ] Takaisin sovellukseen: vaiheet 1–3 saavat ✓-merkin, ja ilmoituksissa on "Ajanseuranta on päällä" (jos ilmoitukset on sallittu).
- [ ] Auto Blockerin voi kytkeä takaisin päälle.
- [ ] **Tärkein tarkistus:** avaa pankkisovellus (ja MobilePay tms.). Ne toimivat normaalisti.

## 3. Ajanseuranta (Tarkastelu)

Avaa *Tarkastelu (kehittäjille)* ja paina *Tyhjennä*. Yläosassa pitää lukea "Tunnistus käynnissä klo … alkaen".

- [ ] Avaa TikTok minuutiksi ja palaa tarkasteluun: rivi `FRONT TikTok in front`, ja TikTokin "tänään" on kasvanut noin minuutilla.
- [ ] Avaa jokin valvomaton sovellus: rivi `FRONT another app in front`, eikä minkään sovelluksen aika kasva.
- [ ] Lukitse näyttö minuutiksi TikTokin ollessa auki: aika ei kasva lukittuna.
- [ ] Pyyhkäise Distraction Killer pois viimeisimmistä ja avaa uudelleen: TikTokin "tänään" on yhä sama (luetaan Androidin käyttötiedoista).
- [ ] Käynnistä puhelin uudelleen avaamatta sovellusta: "Ajanseuranta on päällä" -ilmoitus palaa, ja aika kasvaa TikTokissa.
- [ ] Paina *Jaa* ja lähetä teksti. Tekstin alussa on tila, päivän ajat, asetukset ja versio.

## 4. Keskeytykset (demotila)

Asetukset → *Demotila* päälle. Jokaisen sovelluksen päivän aika on silloin 15 s, koko näytön tauko tulee 10 s myöhemmin ja muistutus toistuu 30 s välein. Tarkastelun *Nollaa tämän päivän ajat* aloittaa alusta.

- [ ] TikTok: 15 s → banneri ja hälytys (ääni ja värinä). *Jatka* → hälytys loppuu.
- [ ] 10 s lisää → koko näytön tauko, video pysähtyy taustalla. *Ohita* tulee 5 s jälkeen. Tarkastelussa on `NOTE level 2 screen opened`.
- [ ] Takaisin TikTokiin: 30 s → banneri uudelleen. *Lopeta tähän* → kiitosnäkymä → *Sulje* vie aloitusnäyttöön.
- [ ] Uusi kierros: tauko → tehtävä loppuun → *Lopetan tältä erää* → kiitosnäkymä. Kysymystehtävässä näppäimistö toimii.
- [ ] Banneri näkyvissä → paina Koti → banneri ja hälytys loppuvat.
- [ ] Sama Instagramissa ja YouTubessa. Kummankin aika lasketaan erikseen.
- [ ] Peli tai muu sovellus, jossa ei vieritetä (lisää se asetuksista): banneri tulee silti 15 s jälkeen.

## 5. Asetukset

- [ ] *Keskeytykset päällä* pois → kotinäkymä kertoo tauosta, eikä mitään keskeydy. Kotinäkymän nappi laittaa ne takaisin päälle.
- [ ] Demotila pois. Valvotut sovellukset → TikTok → karuselleista 0 h 0 min → *Tallenna*. Avaa TikTok: banneri tulee heti.
- [ ] Sama TikTok 0 h 5 min: banneri tulee viiden minuutin käytön jälkeen, ja sen jälkeen Ajoitus-asetuksen välein.
- [ ] Karuselleja voi vierittää ja napauttaa, ja valinta pysähtyy keskelle. Fonttikoko suurimmaksi: numerot mahtuvat.
- [ ] Ääni pois ja värinä pois → *Kokeile hälytystä* noudattaa valintoja.
- [ ] Ruksi poistaa YouTuben → Shorts ei keskeydy. *Lisää sovellus* → YouTube takaisin.
- [ ] *Lisää sovellus*: lista näyttää puhelimen sovellukset nimillä ilman jo valvottuja ja tätä sovellusta, ja haku toimii. Lisää esim. Chrome → päivän ajan valinta aukeaa heti, ja Chrome näkyy kortissa ja kotinäkymässä.

## 6. Tilastot ja lokit

- [ ] *Tilastot* näyttää keskeytykset ja sen, miten kukin päättyi. *Jaa loki (JSON)* avaa jakovalikon.
- [ ] Pyyhkäise sovellus pois viimeisimmistä ja avaa uudelleen: tarkastelun rivit ovat tallessa ja lopussa on `NOTE app started`.
- [ ] Tilastot → *Aloita testaaja 1*. Kotinäkymän tilakortissa lukee "Testaaja 1". Tee yksi keskeytys: tilastojen ylälaidassa on valinta *Testaaja 1* / *Kaikki*.
- [ ] *Aloita testaaja 2* → testaajan 2 luvut alkavat nollasta, ja *Kaikki* näyttää molempien keskeytykset. Jaetussa JSONissa jokaisella rivillä on `"tester"`.
- [ ] *Päätöksen nopeus*: paina bannerissa *Jatka* heti ja seuraavalla kerralla *Lopeta tähän* muutaman sekunnin päästä. Ajat näyttävät järkeviltä. Paina tauossa *Ohita* heti, kun se tulee näkyviin → "Ohita heti tauossa" kasvaa.
- [ ] *Lopeta testaus* → "Testaaja"-merkki katoaa kotinäkymästä.

## 7. Ulkoasu

- [ ] Sovelluksen kuvake on aurinko horisontin yllä ja näyttää siistiltä aloitusnäytöllä.
- [ ] Käy läpi koti, asetukset, tilastot, banneri, tauko (kaikki kolme tehtävää) ja kiitosnäkymä sekä vaaleana että tummana (Asetukset → Näyttö → Tumma). Kaikki tekstit erottuvat taustastaan.
- [ ] Tummana tauko ja kiitosnäkymä eivät välähdä vaaleina auetessaan.
- [ ] Fonttikoko suurimmaksi (Asetukset → Näyttö → fonttikoko): mikään teksti ei leikkaudu tai mene päällekkäin, ja kaikki napit toimivat.
- [ ] Kysymystehtävä: näppäimistö ei peitä tekstikenttää tai *Vastaa*-nappia.

## Jos jokin ei toimi

- **APK ei asennu:** Auto Blocker on päällä. Jos puhelin sanoo "Sovellusta ei asennettu", puhelimessa on uudempi versio kuin asennettava (esim. vanha PR-build uudemman päälle). Asenna uusin, tai poista sovellus ensin, jolloin tilastot ja asetukset häviävät.
- **Tyhjä loki tai aika ei kasva:** katso jaetun tekstin rivi "Tunnistus: …". "EI käynnissä" tarkoittaa, että käyttötietojen lupa puuttuu tai puhelin pysäytti palvelun. Anna lupa ja tarkista akkuasetus.
- **Banneria ei tule, mutta tarkastelussa on `LEVEL1`:** "Näytä muiden sovellusten päällä" on pois. Tarkastelussa on silloin myös `ERROR overlay`.
- **Tauko ei aukea, mutta tarkastelussa on `LEVEL2`:** sama oikeus puuttuu, tai puhelin estää taustalta avatut näkymät. Jaa loki.
- **Ajanseuranta pysähtyy taustalla:** Akku → Rajoittamaton. Tarkastelussa näkyy `NOTE service started` aina, kun palvelu käynnistyy uudelleen.
- **ERROR-rivi tarkastelussa:** sovellus jatkoi virheen jälkeen. Jaa loki, niin virhe voidaan korjata.

## Ennen `rt8`-tagia (N13)

- [ ] Kaikki yllä toimii, tai poikkeamat on kirjattu.
- [ ] Tyhjennä tilastot. Jokaisen testin alussa Tilastot → *Aloita testaaja N*, ja testin jälkeen *Jaa loki (JSON)*.
- [ ] Tagi tehdään testatusta commitista: `git tag rt8 <commit>` ja `git push origin rt8`. Actions tekee siitä pysyvän julkaisun. Älä tee julkaisua GitHubin Releases-sivulla käsin, koska Actions luo sen itse.
