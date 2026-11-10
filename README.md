# Distraction Killer (Android)

Kurssin SCI-C1002 (Aalto, ryhmä 5) demo. Jokaiselle valitsemallesi sovellukselle (oletuksena TikTok, Instagram ja YouTube) on oma päivittäinen aika. Kun se on käytetty, sovellus muistuttaa sinua pitämään tauon: ensin banneri, sitten koko näytön tauko.

Suunnitelma ja tehtävät: [PLAN.md](PLAN.md). Ulkoasun säännöt: [DESIGN.md](DESIGN.md). Ohjeet AI-agenteille: [CLAUDE.md](CLAUDE.md) = [AGENTS.md](AGENTS.md).

## Asennus puhelimeen (Samsung Galaxy)

1. Kytke **Auto Blocker** pois: Asetukset → Turvallisuus ja yksityisyys → Auto Blocker. Se estää APK-asennukset.
2. Avaa puhelimella [uusin versio](../../releases/tag/latest) ja lataa `distraction-killer.apk`.
3. Salli Chromelle tuntemattomien sovellusten asennus, kun sitä kysytään. Jos Play Protect varoittaa, valitse "Asenna silti".
4. Päivitykset asennetaan samalla tavalla vanhan version päälle. Lokit ja asetukset säilyvät.

## Ajanseurannan käyttöönotto

1. **Salli käyttötietojen käyttö:** Asetukset → Sovellukset → Erityiset käyttöoikeudet → Käyttötietojen käyttö → Distraction Killer → päälle. Sovelluksen kotinäkymän nappi vie suoraan oikeaan kohtaan.
2. **Näytä muiden sovellusten päällä:** samasta Erityiset käyttöoikeudet -valikosta → Distraction Killer → päälle. Kotinäkymässä on tähänkin nappi.
3. Jos puhelin sanoo, että asetus on rajoitettu: Asetukset → Sovellukset → Distraction Killer → ⋮ → **Salli rajoitetut asetukset**, ja kohdat 1–2 uudelleen.
4. Sovelluksen tiedot → Akku → **Rajoittamaton**, jotta Samsung ei pysäytä ajanseurantaa taustalla.
5. Auto Blockerin voi nyt kytkeä takaisin päälle.

Sovelluksen kotinäkymässä on samat vaiheet, ja valmis vaihe saa ✓-merkin. Kun ajanseuranta toimii, ilmoituksissa näkyy "Ajanseuranta on päällä".

Sovellus näkee vain, mikä sovellus on edessä ja kuinka kauan. Se ei näe, mitä ruudulla on, eikä sillä ole internet-oikeutta. Esteettömyyspalvelua ei käytetä (se poistettiin tehtävässä N21, koska pankkisovellukset ja Play Protect eivät hyväksy sitä).

## Testaus

Puhelimella testattavat asiat ovat listana tiedostossa [TESTING.md](TESTING.md). Jos jokin ei toimi, avaa sovelluksessa *Tarkastelu (kehittäjille)* → *Jaa*. Jaetun tekstin alussa on ajanseurannan tila, sovellusten päivän ajat, asetukset ja versio.

## Kehitys

- GitHub Actions rakentaa APK:n jokaisesta pull requestista (Actions-välilehti → ajo → Artifacts) ja päivittää `latest`-julkaisun aina, kun `main` muuttuu.
- Paikallinen build: `./gradlew assembleDebug` (vaatii Android Studion tai Android SDK:n). Android Studion build on versio `0.1.dev`, joten se ei asennu GitHubin buildin päälle. Poista sovellus ensin, tai asenna GitHubin build.
- Yksikkötestit: `./gradlew testDebugUnitTest`. Ne ajetaan myös GitHubissa ennen jokaista buildia.
- Tagit `rt8` ja `demo` tekevät pysyvät julkaisut RT8-testeille ja messuille.
- Otsikkofontti Fraunces on SIL Open Font License 1.1 -lisenssillä. Lisenssi on sovelluksen mukana: `app/src/main/assets/licenses/fraunces-ofl.txt`.
