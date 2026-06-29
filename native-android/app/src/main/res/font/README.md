# Brand fonts (drop-in)

The app uses **Space Grotesk** (display) and **Inter** (body) to match the web
app. The typography is wired to pick these up automatically — `BrandFonts.resolve`
looks them up by resource name at runtime, so the app builds and runs without
them and upgrades the instant they're present (no code change needed).

Drop the OFL `.ttf` files into **this folder** (`app/src/main/res/font/`) with
**exactly these lowercase names** (Android font resource names must be
lowercase, letters/digits/underscore only):

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
