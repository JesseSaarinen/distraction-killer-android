# Testaus puhelimella (Galaxy S21)

Käy nämä läpi uuden buildin jälkeen ja ennen `rt8`-tagia. Merkitse, mikä toimi, ja jaa tarkastelun loki, jos jokin ei toiminut. Tarkastelun lokissa on vain teknisiä tietoja (sovellusten paketit, tapahtumien tyypit, näkymien tunnisteet, vieritysten määrät ja ajat), ei tekstejä tai videoita.

## 1. Asennus

- [ ] Asetukset → Turvallisuus ja yksityisyys → **Auto Blocker** pois. Muuten APK ei asennu.
- [ ] Avaa puhelimella [uusin versio](../../releases/tag/latest) ja lataa `distraction-killer.apk`. Asenna. Jos Play Protect varoittaa, valitse "Asenna silti".
- [ ] Avaa sovellus. Kotinäkymän alareunan versio (`0.1.N`) on sama kuin julkaisun build-numero.
- [ ] Uusi build asentuu vanhan päälle: asetukset, tilastot ja esteettömyysasetus säilyvät.

## 2. Käyttöönotto

- [ ] Kotinäkymä → *Avaa esteettömyysasetukset* → Asennetut sovellukset → Distraction Killer: selauksen tunnistus → päälle. Jos kytkin on harmaana: *Avaa sovelluksen tiedot* → ⋮ → Salli rajoitetut asetukset, ja uudelleen.
- [ ] *Avaa sovelluksen tiedot* → Akku → **Rajoittamaton**.
- [ ] Takaisin sovellukseen: vaiheet 1 ja 2 saavat ✓-merkin.
- [ ] Auto Blockerin voi kytkeä takaisin päälle.

"Näytä muiden sovellusten päällä" -oikeutta ei tarvita. Banneri piirretään tunnistuksen omaan ikkunaan.

## 3. Tunnistus (Tarkastelu)

Avaa *Tarkastelu (kehittäjille)* ja paina *Tyhjennä*. Yläosassa pitää lukea "Tunnistus käynnissä klo … alkaen". Selaa sitten jokaista sovellusta noin minuutti ja palaa katsomaan rivejä.

- [ ] **TikTok**, Sinulle-syöte: pyyhkäisyistä tulee rivejä `SCROLLED id=viewpager … → swipe (in the feed)` ja kerran `FRONT short videos in front (swipe)`. Ajastin kasvaa.
- [ ] TikTokin muut näkymät, esim. välilehdet sivuttain tai kommentit: `→ other`, ei swipe.
- [ ] **Instagram**, etusivu: `id=list … → other (not the feed)`. Ajastin ei kasva.
- [ ] Instagram, Reels: `id=clips_viewer_view_pager … → swipe`. Ajastin kasvaa. Takaisin etusivulle ja vieritä: `FRONT short videos not in front (scrolled list)`.
- [ ] **YouTube**, etusivu ja tavallinen video: ei `FRONT short videos in front` -riviä.
- [ ] YouTube, Shorts: `FRONT short videos in front (player on screen)`.
- [ ] **Tärkein tarkistus:** pyyhkäise viisi Shortsia. Tuleeko jokaisesta `id=reel_recycler … → swipe`, ja kasvaako ajastin koko ajan? Jos ajastin pysähtyy noin minuutin päästä, Shortsin pyyhkäisyt eivät näy tunnistukselle. Jaa loki.
- [ ] Paina *Jaa* ja lähetä teksti. Tekstin alussa on tila, asetukset ja versio, joten tyhjäkin loki kertoo syyn.

## 4. Keskeytykset (demotila)

Asetukset → *Demotila* päälle. Ensimmäinen keskeytys tulee 15 s ja koko näytön tauko 10 s myöhemmin.

- [ ] TikTok: selaa 15 s → banneri ja hälytys (ääni ja värinä). *Jatka* → hälytys loppuu.
- [ ] Selaa vielä 10 s → koko näytön tauko, video pysähtyy taustalla. *Ohita* tulee 5 s jälkeen. Tarkastelussa on `NOTE level 2 screen opened`.
- [ ] Uusi kierros: banneri → *Lopeta tähän* → kiitosnäkymä → *Sulje* vie aloitusnäyttöön.
- [ ] Uusi kierros: tauko → tehtävä loppuun → *Lopetan scrollauksen* → kiitosnäkymä. Kysymystehtävässä näppäimistö toimii.
- [ ] Banneri näkyvissä → paina Koti → banneri ja hälytys loppuvat.
- [ ] Sama Reelsissä ja Shortsissa.

## 5. Asetukset

- [ ] *Keskeytykset päällä* pois → kotinäkymä kertoo tauosta, eikä selaus keskeydy. Kotinäkymän nappi laittaa ne takaisin päälle.
- [ ] Demotila pois, väli 1 min, tauko 15 s: banneri tulee minuutin ja koko näytön tauko 15 s myöhemmin.
- [ ] Ääni pois ja värinä pois → *Kokeile hälytystä* noudattaa valintoja.
- [ ] Valvotut sovellukset: YouTube pois → Shorts ei keskeydy.

## 6. Tilastot ja lokit

- [ ] *Tilastot* näyttää keskeytykset ja sen, miten kukin päättyi. *Jaa loki (JSON)* avaa jakovalikon.
- [ ] Pyyhkäise sovellus pois viimeisimmistä ja avaa uudelleen: tarkastelun rivit ovat tallessa ja lopussa on `NOTE app started`.

## Jos jokin ei toimi

- **APK ei asennu:** Auto Blocker on päällä. Jos puhelin sanoo "Sovellusta ei asennettu", puhelimessa on uudempi versio kuin asennettava (esim. vanha PR-build uudemman päälle). Asenna uusin, tai poista sovellus ensin, jolloin tilastot ja asetukset häviävät.
- **Tyhjä loki:** katso jaetun tekstin rivi "Tunnistus: …". "EI käynnissä" tarkoittaa, että esteettömyysasetus on pois tai puhelin pysäytti palvelun. Laita se päälle ja tarkista akkuasetus.
- **Tunnistus pysähtyy taustalla:** Akku → Rajoittamaton. Tarkastelussa näkyy `NOTE service started` aina, kun palvelu käynnistyy uudelleen.
- **ERROR-rivi tarkastelussa:** sovellus jatkoi virheen jälkeen. Jaa loki, niin virhe voidaan korjata.

## Ennen `rt8`-tagia (N13)

- [ ] Kaikki yllä toimii, tai poikkeamat on kirjattu.
- [ ] Play Kauppa → TikTok, Instagram ja YouTube → ⋮ → automaattinen päivitys pois, ettei näkymien tunnisteet muutu ennen testejä.
- [ ] Tagi tehdään testatusta commitista: `git tag rt8 <commit>` ja `git push origin rt8`. Actions tekee siitä pysyvän julkaisun. Älä tee julkaisua GitHubin Releases-sivulla käsin, koska Actions luo sen itse.
