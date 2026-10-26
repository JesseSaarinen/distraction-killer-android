# Distraction Killer (Android)

Kurssin SCI-C1002 (Aalto, ryhmä 5) demo. Sovellus huomaa, kun selaat lyhytvideoita TikTokissa, Instagram Reelsissä tai YouTube Shortsissa, ja keskeyttää selauksen valitsemiesi väliaikojen jälkeen.

Suunnitelma ja tehtävät: [PLAN.md](PLAN.md). Ohjeet AI-agenteille: [CLAUDE.md](CLAUDE.md) = [AGENTS.md](AGENTS.md).

## Asennus puhelimeen (Samsung Galaxy)

1. Kytke **Auto Blocker** pois: Asetukset → Turvallisuus ja yksityisyys → Auto Blocker. Se estää APK-asennukset.
2. Avaa puhelimella [uusin versio](../../releases/tag/latest) ja lataa `distraction-killer.apk`.
3. Salli Chromelle tuntemattomien sovellusten asennus, kun sitä kysytään. Jos Play Protect varoittaa, valitse "Asenna silti".
4. Päivitykset asennetaan samalla tavalla vanhan version päälle. Lokit ja asetukset säilyvät.

Valvonnan käyttöönotto-ohjeet (Esteettömyys-asetus ja akunkäyttö) tulevat tähän, kun Accessibility Service on mukana (tehtävä N4).

## Kehitys

- GitHub Actions rakentaa APK:n jokaisesta pull requestista (Actions-välilehti → ajo → Artifacts) ja päivittää `latest`-julkaisun aina, kun `main` muuttuu.
- Paikallinen build: `./gradlew assembleDebug` (vaatii Android Studion tai Android SDK:n).
- Tagit `rt8` ja `demo` tekevät pysyvät julkaisut RT8-testeille ja messuille.
