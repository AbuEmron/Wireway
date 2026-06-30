# Brand fonts

The app uses **Space Grotesk** (display) and **Inter** (body) to match the web
app. The typography is wired to pick these up automatically — `BrandFonts.resolve`
looks them up by resource name at runtime, so the app builds and runs without
them and upgrades the instant they're present (no code change needed).

> **Status: bundled.** The static OFL `.ttf` weights below are now committed under
> `app/src/main/res/font/` (sourced from Fontsource / jsDelivr, latin subset).
> SIL OFL 1.1 license texts are in `native-android/licenses/`. The drop-in table
> below is kept for reference / to swap in different weights.

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
