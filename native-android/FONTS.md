# Brand fonts

The app's brand typeface is **Poppins** (used for both display and body — the
"Powered by Precision" identity). The typography is wired to pick it up
automatically — `BrandFonts.resolve` looks fonts up by resource name at runtime,
so the app builds and runs without them and upgrades the instant they're present
(no code change needed). If Poppins is absent it falls back to the legacy
**Space Grotesk** / **Inter** resources, then to the platform sans.

> **Status: bundled.** The static OFL `.ttf` weights below are committed under
> `app/src/main/res/font/` (Poppins from google/fonts; legacy Space Grotesk /
> Inter from Fontsource / jsDelivr, latin subset). SIL OFL 1.1 license texts are
> in `native-android/licenses/`. The drop-in table below is kept for reference.

| File name | Source (OFL) |
| --------- | ------------ |
| `poppins_regular.ttf`  | https://github.com/google/fonts (ofl/poppins, Regular) |
| `poppins_medium.ttf`   | Poppins Medium |
| `poppins_semibold.ttf` | Poppins SemiBold |
| `poppins_bold.ttf`     | Poppins Bold |

### Legacy fallback fonts

> Note: this file lives at `native-android/FONTS.md`, **not** under `res/font/` —
> Android's resource merger only allows `.xml/.ttf/.ttc/.otf` files inside
> `res/font/`, so the instructions can't live there.

Drop the OFL `.ttf` files into **`app/src/main/res/font/`** with **exactly these
lowercase names** (Android font resource names must be lowercase, letters/digits/
underscore only):

| File name | Source (OFL) |
| --------- | ------------ |
| `space_grotesk_regular.ttf`  | https://github.com/floriankarsten/space-grotesk (fonts/ttf, Regular) |
| `space_grotesk_medium.ttf`   | Space Grotesk Medium |
| `space_grotesk_semibold.ttf` | Space Grotesk SemiBold |
| `space_grotesk_bold.ttf`     | Space Grotesk Bold |
| `inter_regular.ttf`          | https://github.com/rsms/inter (Inter Regular) |
| `inter_medium.ttf`           | Inter Medium |
| `inter_semibold.ttf`         | Inter SemiBold |

Both families are SIL Open Font License 1.1, so redistribution inside the app is
permitted. If only some weights are present, the app uses what it finds and falls
back to the platform sans for the rest.
