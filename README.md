# Distraction Killer (Android)

Kurssin SCI-C1002 (Aalto, ryhmä 5) demo. Sovellus huomaa, kun selaat lyhytvideoita TikTokissa, Instagram Reelsissä tai YouTube Shortsissa, ja keskeyttää selauksen valitsemiesi väliaikojen jälkeen.

Suunnitelma ja tehtävät: [PLAN.md](PLAN.md). Ohjeet AI-agenteille: [CLAUDE.md](CLAUDE.md) = [AGENTS.md](AGENTS.md).

## Asennus puhelimeen (Samsung Galaxy)

1. Kytke **Auto Blocker** pois: Asetukset → Turvallisuus ja yksityisyys → Auto Blocker. Se estää APK-asennukset.
2. Avaa puhelimella [uusin versio](../../releases/tag/latest) ja lataa `distraction-killer.apk`.
3. Salli Chromelle tuntemattomien sovellusten asennus, kun sitä kysytään. Jos Play Protect varoittaa, valitse "Asenna silti".
4. Päivitykset asennetaan samalla tavalla vanhan version päälle. Lokit ja asetukset säilyvät.

## Selauksen tunnistuksen käyttöönotto

1. Asetukset → Esteettömyys → Asennetut sovellukset → **Distraction Killer: selauksen tunnistus** → päälle. Sovelluksen kotinäkymän nappi vie suoraan esteettömyysasetuksiin.
2. Jos kytkin on harmaana (Android 13+ sivuladatuille sovelluksille): Asetukset → Sovellukset → Distraction Killer → ⋮ → **Salli rajoitetut asetukset**, ja sitten kohta 1 uudelleen.
3. Sovelluksen tiedot → Akku → **Rajoittamaton**, jotta Samsung ei pysäytä tunnistusta taustalla.
4. Auto Blockerin voi nyt kytkeä takaisin päälle.

Sovelluksen kotinäkymässä on samat vaiheet, ja valmis vaihe saa ✓-merkin. "Näytä muiden sovellusten päällä" -oikeutta ei tarvita, koska banneri piirretään tunnistuksen omaan ikkunaan.

Tunnistus lukee vain, mikä sovellus on auki, näkymien tunnisteet ja vierityksen kohdan. Se ei lue tekstejä, viestejä tai videoiden sisältöä, eikä sovelluksella ole internet-oikeutta.

## Testaus

Puhelimella testattavat asiat ovat listana tiedostossa [TESTING.md](TESTING.md). Jos jokin ei toimi, avaa sovelluksessa *Tarkastelu (kehittäjille)* → *Jaa*. Jaetun tekstin alussa on tunnistuksen tila, asetukset ja versio.

## Kehitys

- GitHub Actions rakentaa APK:n jokaisesta pull requestista (Actions-välilehti → ajo → Artifacts) ja päivittää `latest`-julkaisun aina, kun `main` muuttuu.
- Paikallinen build: `./gradlew assembleDebug` (vaatii Android Studion tai Android SDK:n). Android Studion build on versio `0.1.dev`, joten se ei asennu GitHubin buildin päälle. Poista sovellus ensin, tai asenna GitHubin build.
- Yksikkötestit: `./gradlew testDebugUnitTest`. Ne ajetaan myös GitHubissa ennen jokaista buildia.
- Tagit `rt8` ja `demo` tekevät pysyvät julkaisut RT8-testeille ja messuille.
