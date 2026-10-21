<div align="center">
<h1>Meld Glass</h1>
<p>A redesigned build of <a href="https://github.com/FrancescoGrazioso/Meld">Meld</a>: liquid glass UI, DJ-style transitions, car mode and more</p>

[![Based on Meld](https://img.shields.io/badge/based%20on-Meld-8A5CFF?style=for-the-badge)](https://github.com/FrancescoGrazioso/Meld)
[![License](https://img.shields.io/badge/license-GPL--3.0-FF4D9D?style=for-the-badge)](LICENSE)

</div>

## Huge thanks to Meld

**All the heavy lifting here is [Meld](https://github.com/FrancescoGrazioso/Meld) by [Francesco Grazioso](https://github.com/FrancescoGrazioso)**: the YouTube Music client, the Spotify integration, the recommendation engine, lyrics, downloads and everything else. Meld itself builds on the excellent [Metrolist](https://github.com/MetrolistGroup/Metrolist). Meld Glass only changes how the app looks and adds a few listening features on top. If you like this, go star the original projects.

Meld Glass installs as a separate app (`com.meld.glass`), so it can sit next to the original Meld.

## What's different

### Look and feel
- **Liquid glass**: the bottom navigation bar, mini player and top bar are real glass. The content behind them is blurred and bent at the edges as you scroll.
- **OLED black by default**, a new font ([Figtree](https://github.com/erikdkennedy/figtree)) and smoother page transitions.
- **Background glow**: soft light streaks behind the app, taking their colors from the current song's cover (or Aurora, Ocean, Sunset and Forest presets). Settings → Appearance → Background glow.
- **Redesigned player**: round play/pause button, an *Up next* preview that opens the queue, a cleaner profile menu, and a glass mini player with its own play button.
- **App icon & name changer**: Glass, Split, Mono or Classic icons, with names like "Music" or "Player". Settings → Appearance → App icon & name.

### Listening
- **DJ Mix**: tap *DJ Mix* on any playlist or album for DJ-style transitions. Meld Glass detects each song's tempo and beats, starts the next song on a beat with its tempo matched (up to ±8 %), swaps the bass halfway through, and keeps the beats locked. Songs too far apart in tempo get a smooth blend instead.
- **Car mode**: turn the phone sideways while music plays for a full-screen player with huge buttons. Swipe the cover to change songs.
- **Not for me**: the 👎 button skips a song and never plays it again. Undo it in Settings → Disliked songs.
- **Listening stats**: minutes listened, plays, songs, artists, your top song and artist, and a per-day chart, by week, month or year. Settings → Listening stats.
- **YouTube Music / Spotify switch** right in the search bar.
- Small things: skipping while paused starts playback, tapping a *Similar content* song plays it, and long-pressing the mini player opens the song menu.

### Fixes
- Volume no longer drops suddenly a while into a song after crossfades or DJ transitions.
- The login page no longer turns white.

## Install

Download the APK from [Releases](https://github.com/martin6727/meld-glass/releases) if there is one, or build it yourself:

```bash
./gradlew :app:assembleIzzyRelease   # release build (unsigned: sign it with your own key)
./gradlew :app:assembleIzzyDebug     # debug build you can install directly
```

The build needs JDK 21+ and the Android SDK; see [development_guide.md](development_guide.md) from upstream.

## Credits

- **[Meld](https://github.com/FrancescoGrazioso/Meld)** by Francesco Grazioso: the app this is built on (GPL-3.0)
- **[Metrolist](https://github.com/MetrolistGroup/Metrolist)**: the YouTube Music client Meld is based on (GPL-3.0)
- **[AndroidLiquidGlass / Backdrop](https://github.com/Kyant0/AndroidLiquidGlass)** by Kyant: the glass effect (Apache-2.0)
- **[Figtree](https://github.com/erikdkennedy/figtree)** by Erik Kennedy: the font (SIL OFL 1.1, see [licenses/Figtree-OFL.txt](licenses/Figtree-OFL.txt))

## License

GPL-3.0, the same as Meld and Metrolist. See [LICENSE](LICENSE).

## Disclaimer

Meld Glass is an unofficial project. It isn't affiliated with or endorsed by YouTube, Google, Spotify, or the Meld and Metrolist developers. YouTube Music and Spotify are trademarks of their owners; the optional "Split" icon is just a nod to the two services. Third-party clients may go against those services' terms, so use it at your own risk.
