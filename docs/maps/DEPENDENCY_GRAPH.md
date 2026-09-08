<!-- GERADO POR tools/codemap/codemap.mjs — NÃO EDITE À MÃO. Rode: node tools/codemap/codemap.mjs -->

# Grafo de Dependências

> Acoplamento entre pacotes, derivado dos imports `com.limelight.*`. Use para prever o raio de impacto de uma mudança.

## Pacotes por centralidade

| Pacote | PageRank | Arquivos | Linhas |
|---|--:|--:|--:|
| `com.limelight` | 230.6 | 22 | 10878 |
| `com.limelight.nvstream.http` | 106.8 | 6 | 1678 |
| `com.limelight.utils` | 97.7 | 22 | 5747 |
| `com.limelight.nvstream.jni` | 70.8 | 1 | 445 |
| `com.limelight.nvstream` | 51.1 | 4 | 950 |
| `com.limelight.preferences` | 50.3 | 11 | 3612 |
| `com.limelight.nvstream.av.audio` | 36.5 | 1 | 16 |
| `com.limelight.nvstream.av.video` | 31.5 | 1 | 22 |
| `com.limelight.ui` | 26.4 | 13 | 1609 |
| `com.limelight.nvstream.input` | 25.1 | 3 | 51 |
| `com.limelight.profiles` | 24.8 | 8 | 1019 |
| `com.limelight.computers` | 20.7 | 7 | 1598 |
| `com.limelight.grid.assets` | 20.6 | 5 | 718 |
| `com.limelight.binding.input` | 18.1 | 5 | 4308 |
| `com.limelight.binding.video` | 15.4 | 5 | 3758 |
| `com.limelight.nvstream.mdns` | 15.0 | 5 | 733 |
| `com.limelight.binding.input.capture` | 14.6 | 6 | 403 |
| `com.limelight.binding` | 14.4 | 1 | 15 |
| `com.limelight.grid` | 14.2 | 5 | 621 |
| `com.limelight.binding.input.evdev` | 13.5 | 6 | 627 |
| `com.limelight.binding.input.virtual_controller.keyboard` | 13.1 | 12 | 4198 |
| `com.limelight.binding.input.touch` | 13.0 | 4 | 1104 |
| `com.limelight.binding.audio` | 11.6 | 1 | 234 |
| `com.limelight.binding.crypto` | 11.6 | 1 | 260 |
| `com.limelight.binding.input.driver` | 11.1 | 8 | 1695 |
| `com.limelight.binding.input.virtual_controller` | 10.1 | 13 | 2815 |
| `com.limelight.nvstream.wol` | 10.0 | 1 | 150 |
| `com.limelight.discovery` | 7.7 | 1 | 91 |
| `com.limelight.nvstream.av` | 7.0 | 1 | 58 |
| `com.limelight.shadows` | 7.0 | 3 | 72 |

## Mapa de dependências (Mermaid)

Arestas com peso >= 3 (número de imports distintos entre os pacotes).

```mermaid
graph LR
  cl["cl"] -->|33| cl_utils["cl.utils"]
  cl_utils["cl.utils"] -->|24| cl["cl"]
  cl_preferences["cl.preferences"] -->|16| cl["cl"]
  cl["cl"] -->|15| cl_nvstream_http["cl.nvstream.http"]
  cl_profiles["cl.profiles"] -->|14| cl["cl"]
  cl_computers["cl.computers"] -->|13| cl_nvstream_http["cl.nvstream.http"]
  cl["cl"] -->|11| cl_preferences["cl.preferences"]
  cl["cl"] -->|11| cl_profiles["cl.profiles"]
  cl["cl"] -->|11| cl_ui["cl.ui"]
  cl_binding_input_virtual_controller_keyboard["cl.binding.input.virtual_controller.keyboard"] -->|11| cl["cl"]
  cl_preferences["cl.preferences"] -->|11| cl_utils["cl.utils"]
  cl_utils["cl.utils"] -->|9| cl_nvstream_http["cl.nvstream.http"]
  cl_grid["cl.grid"] -->|8| cl["cl"]
  cl_nvstream["cl.nvstream"] -->|8| cl_nvstream_http["cl.nvstream.http"]
  cl["cl"] -->|7| cl_computers["cl.computers"]
  cl_binding_input_driver["cl.binding.input.driver"] -->|7| cl["cl"]
  cl_ui["cl.ui"] -->|7| cl["cl"]
  cl["cl"] -->|6| cl_binding_input["cl.binding.input"]
  cl_binding_input_virtual_controller["cl.binding.input.virtual_controller"] -->|6| cl["cl"]
  cl["cl"] -->|5| cl_grid_assets["cl.grid.assets"]
  cl_binding_input["cl.binding.input"] -->|5| cl["cl"]
  cl_binding_input_virtual_controller["cl.binding.input.virtual_controller"] -->|5| cl_preferences["cl.preferences"]
  cl_binding_input_virtual_controller["cl.binding.input.virtual_controller"] -->|5| cl_nvstream_input["cl.nvstream.input"]
  cl_binding_input_virtual_controller_keyboard["cl.binding.input.virtual_controller.keyboard"] -->|5| cl_preferences["cl.preferences"]
  cl_discovery["cl.discovery"] -->|5| cl_nvstream_mdns["cl.nvstream.mdns"]
  cl_utils["cl.utils"] -->|5| cl_preferences["cl.preferences"]
  cl["cl"] -->|4| cl_grid["cl.grid"]
  cl["cl"] -->|4| cl_binding_input_touch["cl.binding.input.touch"]
  cl["cl"] -->|4| cl_binding_video["cl.binding.video"]
  cl_binding_input_driver["cl.binding.input.driver"] -->|4| cl_nvstream_jni["cl.nvstream.jni"]
  cl_binding_input_driver["cl.binding.input.driver"] -->|4| cl_nvstream_input["cl.nvstream.input"]
  cl_binding_input_virtual_controller_keyboard["cl.binding.input.virtual_controller.keyboard"] -->|4| cl_binding_input_virtual_controller["cl.binding.input.virtual_controller"]
  cl_binding_input_virtual_controller_keyboard["cl.binding.input.virtual_controller.keyboard"] -->|4| cl_utils["cl.utils"]
  cl_binding_video["cl.binding.video"] -->|4| cl["cl"]
  cl_computers["cl.computers"] -->|4| cl["cl"]
  cl_grid["cl.grid"] -->|4| cl_nvstream_http["cl.nvstream.http"]
  cl_grid["cl.grid"] -->|4| cl_grid_assets["cl.grid.assets"]
  cl_grid_assets["cl.grid.assets"] -->|4| cl["cl"]
  cl_nvstream_http["cl.nvstream.http"] -->|4| cl["cl"]
  cl["cl"] -->|3| cl_nvstream["cl.nvstream"]
  cl_binding_input["cl.binding.input"] -->|3| cl_binding_input_driver["cl.binding.input.driver"]
  cl_binding_input["cl.binding.input"] -->|3| cl_nvstream_input["cl.nvstream.input"]
  cl_binding_input_capture["cl.binding.input.capture"] -->|3| cl["cl"]
  cl_binding_input_evdev["cl.binding.input.evdev"] -->|3| cl["cl"]
  cl_binding_input_touch["cl.binding.input.touch"] -->|3| cl_nvstream["cl.nvstream"]
  cl_binding_input_touch["cl.binding.input.touch"] -->|3| cl_nvstream_input["cl.nvstream.input"]
  cl_computers["cl.computers"] -->|3| cl_utils["cl.utils"]
  cl_grid_assets["cl.grid.assets"] -->|3| cl_nvstream_http["cl.nvstream.http"]
  cl_nvstream_mdns["cl.nvstream.mdns"] -->|3| cl["cl"]
  cl_ui["cl.ui"] -->|3| cl_utils["cl.utils"]
  cl_ui["cl.ui"] -->|3| cl_grid_assets["cl.grid.assets"]
  cl_ui["cl.ui"] -->|3| cl_nvstream_http["cl.nvstream.http"]
```

## Comunidades detectadas

Agrupamento por label propagation — pacotes que mudam juntos com mais probabilidade.

**Comunidade 1** (28): `com.limelight`, `com.limelight.binding`, `com.limelight.binding.audio`, `com.limelight.binding.crypto`, `com.limelight.binding.input`, `com.limelight.binding.input.capture`, `com.limelight.binding.input.driver`, `com.limelight.binding.input.evdev`, `com.limelight.binding.input.touch`, `com.limelight.binding.input.virtual_controller`, `com.limelight.binding.input.virtual_controller.keyboard`, `com.limelight.binding.video`, `com.limelight.computers`, `com.limelight.discovery`, `com.limelight.grid`, `com.limelight.grid.assets`, `com.limelight.nvstream`, `com.limelight.nvstream.av.audio`, `com.limelight.nvstream.av.video`, `com.limelight.nvstream.http`, `com.limelight.nvstream.input`, `com.limelight.nvstream.jni`, `com.limelight.nvstream.mdns`, `com.limelight.nvstream.wol`, `com.limelight.preferences`, `com.limelight.profiles`, `com.limelight.ui`, `com.limelight.utils`

