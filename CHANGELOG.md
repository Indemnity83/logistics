# Changelog

## [0.7.4](https://github.com/Indemnity83/logistics/compare/mc26.3-v0.8.5...mc26.3-v0.7.4) (2026-09-05)


### ⚠ BREAKING CHANGES

* **energy:** Cables and batteries no longer power extraction pipes or the Fluid Pump. Only a directly-adjacent engine can power them. Existing setups that fed pipes through cables/batteries will stop working — place an engine against the pipe instead.
* **energy:** Batteries no longer power a logistics network directly. A network is powered only through a Power Junction — place one between your power source (cables/batteries) and the network. Existing battery-on-a-pipe setups stop working until a Power Junction is added.
* **energy:** logistics pipe operations consume power ([#465](https://github.com/Indemnity83/logistics/issues/465))
* **energy:** Logistics pipes now require power from an adjacent Battery. Existing networks stop routing/supplying/crafting — and drop items already in transit — until a charged Battery is connected.
* logistics pipe and module crafting recipes have changed; enable the "Classic Logistics Pipes crafting recipes" built-in datapack to restore the original gear-based recipes as alternates

### Added

* add macerator recipes for copper, gold, and iron dusts ([#244](https://github.com/Indemnity83/logistics/issues/244)) ([0ebb885](https://github.com/Indemnity83/logistics/commit/0ebb88514c302e8bd8e6a382589043df29ae35ef))
* add Minecraft 26.1 support updates ([b49938c](https://github.com/Indemnity83/logistics/commit/b49938c1415f3f3f8190c3c21a7f57a21272b24c))
* add NeoForge client rendering support ([#381](https://github.com/Indemnity83/logistics/issues/381)) ([cff4db9](https://github.com/Indemnity83/logistics/commit/cff4db9e6cd778dbca421971197c2fb8f5b9e0b5))
* add NeoForge platform SPI services ([#378](https://github.com/Indemnity83/logistics/issues/378)) ([08d5e16](https://github.com/Indemnity83/logistics/commit/08d5e161b04c5b192327241787f9250182435a77))
* add NeoForge storage adapters ([cdeb6a7](https://github.com/Indemnity83/logistics/commit/cdeb6a7a53adf74379ba522c980560ad9f7b6966))
* add power cables ([f2cc460](https://github.com/Indemnity83/logistics/commit/f2cc460e64714aebab4c9c9ebcebaec393f69403))
* **api:** add loader-independent API for cross-mod fluid integration ([#516](https://github.com/Indemnity83/logistics/issues/516)) ([bc46019](https://github.com/Indemnity83/logistics/commit/bc46019c726b20817a2f5a281fa3929ce246ce6e))
* **automation:** add configurable laser quarry chunk loading ([#536](https://github.com/Indemnity83/logistics/issues/536)) ([2095caf](https://github.com/Indemnity83/logistics/commit/2095caf4bd769317cbd98de404b1a1e9245a904e))
* **automation:** add crude oil to petroleum-block transposer recipes ([#851](https://github.com/Indemnity83/logistics/issues/851)) ([5792112](https://github.com/Indemnity83/logistics/commit/57921128b8d0e5f97adbb4a6d928244cc38fc0e9))
* **automation:** add pulped biomass from the sawmill ([#660](https://github.com/Indemnity83/logistics/issues/660)) ([6a83220](https://github.com/Indemnity83/logistics/commit/6a83220185f68f8298543e7745b90191f4f7f457))
* **automation:** add RF cost and recipe system to the Transposer ([e2883e8](https://github.com/Indemnity83/logistics/commit/e2883e82075c2058985e7c92443fb736525e7842))
* **automation:** add RF cost and recipe system to the Transposer ([#829](https://github.com/Indemnity83/logistics/issues/829)) ([e2883e8](https://github.com/Indemnity83/logistics/commit/e2883e82075c2058985e7c92443fb736525e7842))
* **automation:** add the alloy smelter ([#656](https://github.com/Indemnity83/logistics/issues/656)) ([3ee3325](https://github.com/Indemnity83/logistics/commit/3ee33250f5673033c29f7a014080db2ab0b38c20))
* **automation:** add the crucible ([#679](https://github.com/Indemnity83/logistics/issues/679)) ([e3eff42](https://github.com/Indemnity83/logistics/commit/e3eff42eacf4d61240bde8d738d5bd2f40a26833))
* **automation:** add the refinery ([#714](https://github.com/Indemnity83/logistics/issues/714)) ([0ace76c](https://github.com/Indemnity83/logistics/commit/0ace76c237ec785f898573e70dbc64f32feaca14))
* **automation:** add the sequential fabricator ([#719](https://github.com/Indemnity83/logistics/issues/719)) ([ae6321b](https://github.com/Indemnity83/logistics/commit/ae6321bc4008dac305767cdaa0176726ff54a153))
* **automation:** add the sequential fabricator ([#719](https://github.com/Indemnity83/logistics/issues/719)) ([ae6321b](https://github.com/Indemnity83/logistics/commit/ae6321bc4008dac305767cdaa0176726ff54a153))
* **automation:** add the Transposer ([#792](https://github.com/Indemnity83/logistics/issues/792)) ([831477e](https://github.com/Indemnity83/logistics/commit/831477e4cdccb86592ed8a432799165cb3974f33))
* **automation:** fabricate chipsets in the sequential fabricator ([#720](https://github.com/Indemnity83/logistics/issues/720)) ([a07c428](https://github.com/Indemnity83/logistics/commit/a07c428d5cbc65baec7384b2b7e9b79cbe6c0474))
* **automation:** fabricate chipsets in the sequential fabricator ([#720](https://github.com/Indemnity83/logistics/issues/720)) ([a07c428](https://github.com/Indemnity83/logistics/commit/a07c428d5cbc65baec7384b2b7e9b79cbe6c0474))
* **automation:** make machine tuning configurable per machine ([#713](https://github.com/Indemnity83/logistics/issues/713)) ([1c48581](https://github.com/Indemnity83/logistics/commit/1c48581a6431fdbf98004c6c098af01e798b942b))
* **automation:** show laser quarry status in the Jade HUD ([#498](https://github.com/Indemnity83/logistics/issues/498)) ([0beaace](https://github.com/Indemnity83/logistics/commit/0beaacee007bcacaf9965300e467aa05d59c6f6b))
* **automation:** show macerator and kiln progress in the Jade HUD ([#499](https://github.com/Indemnity83/logistics/issues/499)) ([9d9944e](https://github.com/Indemnity83/logistics/commit/9d9944e131ecf8b4f62170791ab70614c75200c8))
* **compat:** add Jade HUD support for refinery and sequential fabricator ([#744](https://github.com/Indemnity83/logistics/issues/744)) ([c3ff99c](https://github.com/Indemnity83/logistics/commit/c3ff99c57cdff308521eb707c6fcbd61695ef499))
* **compat:** integrate Jade and remove the built-in probe ([3979277](https://github.com/Indemnity83/logistics/commit/397927718371980974c507b4c79a33d29ef0891b))
* **core:** add a copper nugget ([#722](https://github.com/Indemnity83/logistics/issues/722)) ([9629b47](https://github.com/Indemnity83/logistics/commit/9629b4714885dab8439b4dc8ab7795e8938f0dd7))
* **core:** add opt-in sanitized crash reporting ([#633](https://github.com/Indemnity83/logistics/issues/633)) ([826792d](https://github.com/Indemnity83/logistics/commit/826792ddb747f60169a4bb7dacb0827e57d4c201))
* **core:** add shared gear recipe tags ([#871](https://github.com/Indemnity83/logistics/issues/871)) ([6bf2c3f](https://github.com/Indemnity83/logistics/commit/6bf2c3f969c1677f96f4ba3c5435aebe2c52b29d))
* **core:** drop niter from the breeze ([#644](https://github.com/Indemnity83/logistics/issues/644)) ([173823e](https://github.com/Indemnity83/logistics/commit/173823ef45df009cb4b47cd74e5d0c7627c28c44))
* **core:** grant ore XP when macerating metal and apatite ores ([#555](https://github.com/Indemnity83/logistics/issues/555)) ([0adb9a7](https://github.com/Indemnity83/logistics/commit/0adb9a7a1719877d3a6ee3ec415a6d0d8ba40c92))
* **crafting:** add tin, rubber, amethyst, and echo valves ([139d341](https://github.com/Indemnity83/logistics/commit/139d341ed2bc9dc8fec1e1ea9ab0fcdfb43b70ab))
* **crafting:** craft gunpowder from coal, sulfur, and niter dust ([86a9c71](https://github.com/Indemnity83/logistics/commit/86a9c7128cbfd27a160f777ce1dcca3c177231c8))
* **crafting:** rework the valve lineup with a bench recipe ([#727](https://github.com/Indemnity83/logistics/issues/727)) ([139d341](https://github.com/Indemnity83/logistics/commit/139d341ed2bc9dc8fec1e1ea9ab0fcdfb43b70ab))
* **crafting:** show sequential fabricator recipes in JEI ([#745](https://github.com/Indemnity83/logistics/issues/745)) ([9fa46ea](https://github.com/Indemnity83/logistics/commit/9fa46eae97629bfa06b1e5f131b62013c4ce5f57))
* **energy:** add a Battery block to power logistics networks ([a711b09](https://github.com/Indemnity83/logistics/commit/a711b09270a6a459dac21e543a840bd5da89d289))
* **energy:** add natural and synthetic polymers for rubber ([#716](https://github.com/Indemnity83/logistics/issues/716)) ([07b678a](https://github.com/Indemnity83/logistics/commit/07b678a533396180ed561b217934a6dfbf9b6eee))
* **energy:** add the Fuel Engine ([#759](https://github.com/Indemnity83/logistics/issues/759)) ([3ead98b](https://github.com/Indemnity83/logistics/commit/3ead98bb07a20b4ba1dbb8f9d60a2f8ace46f6a1))
* **energy:** add the Magmatic Engine ([#776](https://github.com/Indemnity83/logistics/issues/776)) ([8cb6069](https://github.com/Indemnity83/logistics/commit/8cb60697f0459297d3ccf789166c47e453a656a0))
* **energy:** add the power junction ([#612](https://github.com/Indemnity83/logistics/issues/612)) ([e33e50a](https://github.com/Indemnity83/logistics/commit/e33e50a018a833049a4796f84ec76a5cae21ad8b))
* **energy:** add the Reaction Engine ([#777](https://github.com/Indemnity83/logistics/issues/777)) ([0e31e65](https://github.com/Indemnity83/logistics/commit/0e31e6512a7460c5050b2af1c8ce0abc67859b95))
* **energy:** add the Steam Engine ([#765](https://github.com/Indemnity83/logistics/issues/765)) ([8f7d8fd](https://github.com/Indemnity83/logistics/commit/8f7d8fda95acf33c1adfafcd6b427c65eb7cb971))
* **energy:** logistics pipe operations consume power ([#465](https://github.com/Indemnity83/logistics/issues/465)) ([a711b09](https://github.com/Indemnity83/logistics/commit/a711b09270a6a459dac21e543a840bd5da89d289))
* **energy:** show engine fuels in the recipe browser ([#886](https://github.com/Indemnity83/logistics/issues/886)) ([783af72](https://github.com/Indemnity83/logistics/commit/783af72b77fbe5f3eaceaf2fbf9cec03b8a990bc))
* **energy:** show power diagnostics in the Jade HUD ([#497](https://github.com/Indemnity83/logistics/issues/497)) ([b720220](https://github.com/Indemnity83/logistics/commit/b720220883cfc17f890e9770f62daf772efdc0ba))
* **fluids:** add a dedicated fluid supplier GUI ([#811](https://github.com/Indemnity83/logistics/issues/811)) ([ab14d7c](https://github.com/Indemnity83/logistics/commit/ab14d7c844106ce065ff0bb965fc4191ff62d64b))
* **fluids:** add bio fuel and fuel oil fluids ([0ace76c](https://github.com/Indemnity83/logistics/commit/0ace76c237ec785f898573e70dbc64f32feaca14))
* **fluids:** add custom fluids and buckets for the Magma Crucible ([6b988d6](https://github.com/Indemnity83/logistics/commit/6b988d63216ce054919f434a2381b82b24ff0e6c))
* **fluids:** add fluid provider and supplier chassis modules ([#825](https://github.com/Indemnity83/logistics/issues/825)) ([4955202](https://github.com/Indemnity83/logistics/commit/4955202dc6f3dba204b9e7cbee1c2b7765c1652f))
* **fluids:** add supplier partial/exact and minimum-deficit modes ([#822](https://github.com/Indemnity83/logistics/issues/822)) ([814eb9b](https://github.com/Indemnity83/logistics/commit/814eb9bd4781a7d01d9309f7acb5ed6e0126f3a0))
* **fluids:** add tar as an alternative fluid pipe sealant ([#715](https://github.com/Indemnity83/logistics/issues/715)) ([a6c2fba](https://github.com/Indemnity83/logistics/commit/a6c2fba82c57dd78e35c1995c23d40736d8373f9))
* **fluids:** add the fluid provider pipe ([#790](https://github.com/Indemnity83/logistics/issues/790)) ([5804080](https://github.com/Indemnity83/logistics/commit/58040807ae5629fec79fffe72d5ffd88077f9ef0))
* **fluids:** add the fluid supplier pipe ([#791](https://github.com/Indemnity83/logistics/issues/791)) ([64abf5c](https://github.com/Indemnity83/logistics/commit/64abf5c131453b9934a4b621a6ba710e67e60239))
* **fluids:** drain and fill cauldrons with fluid pipes ([#885](https://github.com/Indemnity83/logistics/issues/885)) ([2b8df9f](https://github.com/Indemnity83/logistics/commit/2b8df9f7a3db99b1c15d352e7970d5aeea4dae28))
* **fluids:** light-emitting fluids glow in pipes and tanks ([#695](https://github.com/Indemnity83/logistics/issues/695)) ([c713692](https://github.com/Indemnity83/logistics/commit/c7136928281c87322ceaa65792e926a5b6d72710))
* **fluids:** show machine fluid tanks in the Jade HUD ([#703](https://github.com/Indemnity83/logistics/issues/703)) ([ad8c057](https://github.com/Indemnity83/logistics/commit/ad8c057ac461210ad27f2b652a2fa522fc8dcce6))
* implement recipe book for Kiln with access widener support ([#242](https://github.com/Indemnity83/logistics/issues/242)) ([e36899b](https://github.com/Indemnity83/logistics/commit/e36899b708a12f6d6ff0f31dc67de80a331d81dc))
* implement recipe book for macerator crafting interface ([#243](https://github.com/Indemnity83/logistics/issues/243)) ([0d0a142](https://github.com/Indemnity83/logistics/commit/0d0a1421e3fc80e9a3cd232f709986d9ccfe15a5))
* **macerator:** add chance byproducts to ore processing ([#643](https://github.com/Indemnity83/logistics/issues/643)) ([86a9c71](https://github.com/Indemnity83/logistics/commit/86a9c7128cbfd27a160f777ce1dcca3c177231c8))
* **macerator:** add recycling recipes for glass, concrete, sandstone, netherrack, clay, bricks, wool, magma, and more ([86a9c71](https://github.com/Indemnity83/logistics/commit/86a9c7128cbfd27a160f777ce1dcca3c177231c8))
* **macerator:** add Sulfur Dust, Quicksilver, and Niter items ([86a9c71](https://github.com/Indemnity83/logistics/commit/86a9c7128cbfd27a160f777ce1dcca3c177231c8))
* **macerator:** macerate breeze rods into wind charges ([86a9c71](https://github.com/Indemnity83/logistics/commit/86a9c7128cbfd27a160f777ce1dcca3c177231c8))
* **macerator:** macerate cinnabar and sulfur blocks and their slab/stairs/wall/polished forms into quicksilver and sulfur dust ([86a9c71](https://github.com/Indemnity83/logistics/commit/86a9c7128cbfd27a160f777ce1dcca3c177231c8))
* **macerator:** macerate logs and planks into sawdust and recycle wooden tools ([86a9c71](https://github.com/Indemnity83/logistics/commit/86a9c7128cbfd27a160f777ce1dcca3c177231c8))
* **macerator:** macerate sulfur spikes into sulfur dust ([86a9c71](https://github.com/Indemnity83/logistics/commit/86a9c7128cbfd27a160f777ce1dcca3c177231c8))
* **macerator:** recycle diamond tools and armor into diamonds ([86a9c71](https://github.com/Indemnity83/logistics/commit/86a9c7128cbfd27a160f777ce1dcca3c177231c8))
* **pipes:** add copper fluid pipe oxidation and fluid pipe marking ([#520](https://github.com/Indemnity83/logistics/issues/520)) ([dda9508](https://github.com/Indemnity83/logistics/commit/dda95084150f6f56c35c8803758be71ea39ac37c))
* **pipes:** add fluid pipes, tanks, and powered fluid extraction ([#511](https://github.com/Indemnity83/logistics/issues/511)) ([c04a76a](https://github.com/Indemnity83/logistics/commit/c04a76a0c30e45592299091a76ae89bd876e9e12))
* **pipes:** add fluid pump ([#537](https://github.com/Indemnity83/logistics/issues/537)) ([2dfdc3b](https://github.com/Indemnity83/logistics/commit/2dfdc3b152d96108f0110e68a823a1abc21f24f5))
* **pipes:** show pipe contents in the Jade HUD ([#500](https://github.com/Indemnity83/logistics/issues/500)) ([b53ee0c](https://github.com/Indemnity83/logistics/commit/b53ee0c47e3458e79f6fa091f5157c0947b687b0))
* **pipes:** show pipe module status in the Jade HUD ([#493](https://github.com/Indemnity83/logistics/issues/493)) ([86b7f72](https://github.com/Indemnity83/logistics/commit/86b7f729523280a318689be8174409d1d36faa03))
* rework pipe and module recipes ([08bda95](https://github.com/Indemnity83/logistics/commit/08bda9578569df5e2d603fb0e18b01878b09b5b0))
* **routing:** add chipset crafting alternatives for pipes and modules ([5747612](https://github.com/Indemnity83/logistics/commit/5747612932b87783645baac039558ecd3b0f2f65))
* **sawmill:** add a recipe book to the sawmill GUI ([#642](https://github.com/Indemnity83/logistics/issues/642)) ([859698a](https://github.com/Indemnity83/logistics/commit/859698ac9cd15b02447a328cf6decb29cafc414f))
* **sawmill:** add wood processing ([#580](https://github.com/Indemnity83/logistics/issues/580)) ([d26ff18](https://github.com/Indemnity83/logistics/commit/d26ff182324bad9771fd854ca2ed60df7f647bcd))
* **sawmill:** show recipes in JEI and details in the Jade HUD ([#613](https://github.com/Indemnity83/logistics/issues/613)) ([95ae978](https://github.com/Indemnity83/logistics/commit/95ae9786129fd4a7bd2073e964cf4127a7dc4dda))
* **ui:** color logistics pipes green when powered, red when not ([#469](https://github.com/Indemnity83/logistics/issues/469)) ([d9f9d60](https://github.com/Indemnity83/logistics/commit/d9f9d60519c9b5d665ccc3f641749a57613ed261))
* **ui:** split creative menu into domain tabs ([#738](https://github.com/Indemnity83/logistics/issues/738)) ([74a0f99](https://github.com/Indemnity83/logistics/commit/74a0f991e0ac046647e7066dd4b608fa0b6280f8))
* wire NeoForge capabilities ([#379](https://github.com/Indemnity83/logistics/issues/379)) ([6bf8b73](https://github.com/Indemnity83/logistics/commit/6bf8b73989d577fd4a8edd66a51c0c4d10a10a12))
* wire NeoForge networking and lifecycle ([#380](https://github.com/Indemnity83/logistics/issues/380)) ([13c0fa5](https://github.com/Indemnity83/logistics/commit/13c0fa552921053b1626fd196fd8b89e2d225e13))
* **worldgen:** add bog earth and peat fuel ([#658](https://github.com/Indemnity83/logistics/issues/658)) ([f6b9b17](https://github.com/Indemnity83/logistics/commit/f6b9b17bf83ed83876ab769cf9203dcba65f07f4))
* **worldgen:** add crude oil and the oil chain ([#690](https://github.com/Indemnity83/logistics/issues/690)) ([f89aa1a](https://github.com/Indemnity83/logistics/commit/f89aa1aa2afe648bb3f567f41cf4cc6bf826843e))


### Changed

* **automation:** reduce memory and load time for machine rendering ([f858416](https://github.com/Indemnity83/logistics/commit/f858416733564b6442f10a0c26023725b869d43f))
* **automation:** reduce memory and load time for machine rendering ([#451](https://github.com/Indemnity83/logistics/issues/451)) ([f858416](https://github.com/Indemnity83/logistics/commit/f858416733564b6442f10a0c26023725b869d43f))
* **automation:** restyle the Crucible progress gauge as a droplet ([#834](https://github.com/Indemnity83/logistics/issues/834)) ([41512e7](https://github.com/Indemnity83/logistics/commit/41512e7fef719953df8a2ac194b801ec49f113e0))
* **build:** update to released MC 26.2 with official NeoForge beta ([#525](https://github.com/Indemnity83/logistics/issues/525)) ([d9b9daa](https://github.com/Indemnity83/logistics/commit/d9b9daae92de7c87dcf47360d80cf0dceebfdfd1))
* **common:** cache pipe and cable collision shapes ([#631](https://github.com/Indemnity83/logistics/issues/631)) ([f31895a](https://github.com/Indemnity83/logistics/commit/f31895a009d315fd311f2f34124314d9aa3bd2a8))
* **core:** force version bump ([f83dafc](https://github.com/Indemnity83/logistics/commit/f83dafcf884941f84696496494db2cbdd2d40892))
* **core:** retexture the bronze and tin metal blocks and items ([#766](https://github.com/Indemnity83/logistics/issues/766)) ([3b2a686](https://github.com/Indemnity83/logistics/commit/3b2a686d24728b4c66449f9cb2344a68602f35d8))
* **core:** streamline world loading by dropping legacy save migrations ([#586](https://github.com/Indemnity83/logistics/issues/586)) ([6e1748c](https://github.com/Indemnity83/logistics/commit/6e1748c54b8b9ff653293f169079ac24d3ece74d))
* **crafting:** craft valves from quartz, redstone, and a base material ([139d341](https://github.com/Indemnity83/logistics/commit/139d341ed2bc9dc8fec1e1ea9ab0fcdfb43b70ab))
* **crafting:** require a bronze gear in the machine frame ([#610](https://github.com/Indemnity83/logistics/issues/610)) ([1673320](https://github.com/Indemnity83/logistics/commit/1673320663b1ae7915e101d7439f77bcddca601a))
* **crafting:** restyle every valve with a distinct electron-tube texture ([139d341](https://github.com/Indemnity83/logistics/commit/139d341ed2bc9dc8fec1e1ea9ab0fcdfb43b70ab))
* **crafting:** yield one marker per craft ([#607](https://github.com/Indemnity83/logistics/issues/607)) ([3c168a7](https://github.com/Indemnity83/logistics/commit/3c168a792509c632caedbb749cfa1694061ec65c))
* **energy:** gate engine harvesting by tool tier ([#784](https://github.com/Indemnity83/logistics/issues/784)) ([820a5e6](https://github.com/Indemnity83/logistics/commit/820a5e6c12cc4b22351c09ccf02d2e024468752f))
* **energy:** power extraction pipes only from a direct engine ([#641](https://github.com/Indemnity83/logistics/issues/641)) ([704c7e8](https://github.com/Indemnity83/logistics/commit/704c7e84bd9391eefd137ccaa2fa3994347e0e18))
* **energy:** power the logistics network through a junction instead of a battery ([e33e50a](https://github.com/Indemnity83/logistics/commit/e33e50a018a833049a4796f84ec76a5cae21ad8b))
* **energy:** reduce memory and load time for cable rendering ([#449](https://github.com/Indemnity83/logistics/issues/449)) ([1adf690](https://github.com/Indemnity83/logistics/commit/1adf6905136ea456d5a3763da06f7bf1ce4f49d6))
* **energy:** restyle the Stirling Engine GUI ([#763](https://github.com/Indemnity83/logistics/issues/763)) ([901cf64](https://github.com/Indemnity83/logistics/commit/901cf64aa9370d14e975e71da0baeebcce821628))
* **fluids:** restyle the fluid pipe textures ([#740](https://github.com/Indemnity83/logistics/issues/740)) ([4351016](https://github.com/Indemnity83/logistics/commit/4351016da8e408a7f4e66a5dc5fb58f01eb86513))
* **fluids:** speed up fluid split allocation ([#623](https://github.com/Indemnity83/logistics/issues/623)) ([ca3b89d](https://github.com/Indemnity83/logistics/commit/ca3b89dc685a34b8a7a94f6a960eaf7089e9f7d0))
* **fluids:** switch the fluid packet's frame window to a rectangle ([#823](https://github.com/Indemnity83/logistics/issues/823)) ([aef59b9](https://github.com/Indemnity83/logistics/commit/aef59b9bfc7a146261d36fa86d02226dc022211c))
* **kiln:** restyle with the shared machine look ([7a97c75](https://github.com/Indemnity83/logistics/commit/7a97c7594651b13a61fe0ebcf565b6fb5393902b))
* **kiln:** retune smelting speed and RF cost ([7a97c75](https://github.com/Indemnity83/logistics/commit/7a97c7594651b13a61fe0ebcf565b6fb5393902b))
* **kiln:** standardize recipe around machine components ([7a97c75](https://github.com/Indemnity83/logistics/commit/7a97c7594651b13a61fe0ebcf565b6fb5393902b))
* **macerator:** ore→dust recipes now drop a chance byproduct dust ([86a9c71](https://github.com/Indemnity83/logistics/commit/86a9c7128cbfd27a160f777ce1dcca3c177231c8))
* **macerator:** recycle netherite dust to and from ingots ([#734](https://github.com/Indemnity83/logistics/issues/734)) ([3022ce0](https://github.com/Indemnity83/logistics/commit/3022ce07b8d50a52b93fd2969313f4bdcce1cbe4))
* **macerator:** restyle with the shared machine look ([8183eb9](https://github.com/Indemnity83/logistics/commit/8183eb955b534709a1cadb02dc9cff246839b353))
* **macerator:** standardize recipe around machine components ([8183eb9](https://github.com/Indemnity83/logistics/commit/8183eb955b534709a1cadb02dc9cff246839b353))
* **pipes:** raise pipe blast resistance to match glass ([#618](https://github.com/Indemnity83/logistics/issues/618)) ([8a5069b](https://github.com/Indemnity83/logistics/commit/8a5069b05dee22403290bc1e2bfe34d92083ea01))
* **pipes:** reduce memory and load time for pipe rendering ([c0bb14f](https://github.com/Indemnity83/logistics/commit/c0bb14fe0af13a0ab3fab4dae625fbe5c4a297af))
* **pipes:** reduce memory and load time for pipe rendering ([#450](https://github.com/Indemnity83/logistics/issues/450)) ([c0bb14f](https://github.com/Indemnity83/logistics/commit/c0bb14fe0af13a0ab3fab4dae625fbe5c4a297af))
* **pump:** standardize recipe around machine components ([71dd4df](https://github.com/Indemnity83/logistics/commit/71dd4dfbd6008f245cfb181e7642428e63c6859f))
* **quarry:** restyle with the shared machine look ([#582](https://github.com/Indemnity83/logistics/issues/582)) ([5a8b069](https://github.com/Indemnity83/logistics/commit/5a8b069d2be23df9bc57525d41969cad32a1ce2f))
* **quarry:** standardize recipe around machine components ([5a8b069](https://github.com/Indemnity83/logistics/commit/5a8b069d2be23df9bc57525d41969cad32a1ce2f))
* **routing:** cache next-hop routes per destination ([#632](https://github.com/Indemnity83/logistics/issues/632)) ([178d7a9](https://github.com/Indemnity83/logistics/commit/178d7a9783d449de53045b2074c164075bda8aec))
* **routing:** rework logistics pipe recipes with chipset alternatives ([#721](https://github.com/Indemnity83/logistics/issues/721)) ([5747612](https://github.com/Indemnity83/logistics/commit/5747612932b87783645baac039558ecd3b0f2f65))
* **routing:** serve pipe shapes from the connection cache ([ba9b068](https://github.com/Indemnity83/logistics/commit/ba9b068cb091d246aaa7ee9641d5e30f34c7c69a))
* **routing:** use a copper nugget in the blank module recipe ([9629b47](https://github.com/Indemnity83/logistics/commit/9629b4714885dab8439b4dc8ab7795e8938f0dd7))
* **sawmill:** match the energy buffer to the other machines ([#647](https://github.com/Indemnity83/logistics/issues/647)) ([86244a0](https://github.com/Indemnity83/logistics/commit/86244a01235d755801070cc0b0827ccf9babca48))
* **sawmill:** move wood processing from macerator ([d26ff18](https://github.com/Indemnity83/logistics/commit/d26ff182324bad9771fd854ca2ed60df7f647bcd))
* **sawmill:** rename Wood Pulp to Sawdust ([d26ff18](https://github.com/Indemnity83/logistics/commit/d26ff182324bad9771fd854ca2ed60df7f647bcd))
* **transport:** lighten the item extractor pipe to distinguish it from oxidized copper ([4351016](https://github.com/Indemnity83/logistics/commit/4351016da8e408a7f4e66a5dc5fb58f01eb86513))
* **ui:** refresh the kiln, macerator, and sawmill GUIs ([#646](https://github.com/Indemnity83/logistics/issues/646)) ([5ae4c45](https://github.com/Indemnity83/logistics/commit/5ae4c4556ad32d816a76f56f2f6a4146c7af8e29))
* **worldgen:** tin ore drops one raw tin ([#608](https://github.com/Indemnity83/logistics/issues/608)) ([59071ef](https://github.com/Indemnity83/logistics/commit/59071ef8a9004fe0c9351d707a2ee5c214dcb963))


### Removed

* **core:** drop the tin gear ([1673320](https://github.com/Indemnity83/logistics/commit/1673320663b1ae7915e101d7439f77bcddca601a))
* **core:** drop the unused sturdy casing ([#650](https://github.com/Indemnity83/logistics/issues/650)) ([8b2900b](https://github.com/Indemnity83/logistics/commit/8b2900bb2e0e8908589c1fa430e8077f7a8d7d82))
* **crafting:** drop the classic crafting resource pack ([99a4e13](https://github.com/Indemnity83/logistics/commit/99a4e134f219e27cbd5431f45ed447851f4e5133)), closes [#960](https://github.com/Indemnity83/logistics/issues/960)
* **crafting:** drop the wooden and ender valves ([139d341](https://github.com/Indemnity83/logistics/commit/139d341ed2bc9dc8fec1e1ea9ab0fcdfb43b70ab))
* **macerator:** remove wood-pulp recipes ([d26ff18](https://github.com/Indemnity83/logistics/commit/d26ff182324bad9771fd854ca2ed60df7f647bcd))


### Fixed

* add ender dust macerator recipe ([#282](https://github.com/Indemnity83/logistics/issues/282)) ([0323f36](https://github.com/Indemnity83/logistics/commit/0323f3649230ec8787208a16a62ae163124e461b))
* add min_format and max_format to pack.mcmeta for recipes ([#250](https://github.com/Indemnity83/logistics/issues/250)) ([0f0ea05](https://github.com/Indemnity83/logistics/commit/0f0ea0587becc2e871e4c0c9896c65604e993d35))
* add outline rendering for quarry area placement ([#274](https://github.com/Indemnity83/logistics/issues/274)) ([b06f9fe](https://github.com/Indemnity83/logistics/commit/b06f9fec1c4ed99ddf186a9543482f1bd213b539))
* add supported_formats to pack.mcmeta for compatibility ([#252](https://github.com/Indemnity83/logistics/issues/252)) ([08521b0](https://github.com/Indemnity83/logistics/commit/08521b0dd1ecc3b2fca721960df35c69f50a01c3))
* add translations for tin and bronze item tags ([#257](https://github.com/Indemnity83/logistics/issues/257)) ([a7264fe](https://github.com/Indemnity83/logistics/commit/a7264fe84827b3d70f94f03637ff9496d8ae1476))
* add validation and sanitization for logistics config fields ([#398](https://github.com/Indemnity83/logistics/issues/398)) ([f5cd45a](https://github.com/Indemnity83/logistics/commit/f5cd45aa6e240d754fc681502e14711878169cdc))
* **automation:** accept raw ore in the quicksilver amalgamation recipes ([#840](https://github.com/Indemnity83/logistics/issues/840)) ([c773748](https://github.com/Indemnity83/logistics/commit/c773748cf7bf846e56d7582b03fe7a87a7271065))
* **automation:** add missing macerator recipes for obsidian, netherite, and metal blocks ([#554](https://github.com/Indemnity83/logistics/issues/554)) ([f5b7304](https://github.com/Indemnity83/logistics/commit/f5b7304c5f8816b7055fbf5f84276729a4b11fd7))
* **automation:** make the refinery and sequential fabricator harvestable ([31dfc5d](https://github.com/Indemnity83/logistics/commit/31dfc5df8de2d497f4f55f518931ca6468d54b54))
* **automation:** pause recipes until byproducts have space ([#597](https://github.com/Indemnity83/logistics/issues/597)) ([5990bef](https://github.com/Indemnity83/logistics/commit/5990befead5b340bd3f88c56e3a59ac96a3c87c0))
* **automation:** reject non-finite recipe experience values ([bca2ef4](https://github.com/Indemnity83/logistics/commit/bca2ef498309e59391dc1d753fb6d056ab5ea35a))
* **automation:** show machine recipes in JEI on multiplayer clients ([#735](https://github.com/Indemnity83/logistics/issues/735)) ([57b2901](https://github.com/Indemnity83/logistics/commit/57b2901b5c59761ffc74581541d74486cf44a373))
* **automation:** show the sawmill's real ingredient count in JEI ([#824](https://github.com/Indemnity83/logistics/issues/824)) ([5a9bb60](https://github.com/Indemnity83/logistics/commit/5a9bb60e15997a18ad4a6bc6a82cc40eb95bfc40))
* **automation:** stop refinery shift-click from duplicating items ([#846](https://github.com/Indemnity83/logistics/issues/846)) ([4c0466d](https://github.com/Indemnity83/logistics/commit/4c0466d7ad924e8f0f7b206822c09ec7311e1e8d))
* **automation:** stop the Alloy Smelter duplicating recipe inputs ([2a1edbb](https://github.com/Indemnity83/logistics/commit/2a1edbb72dceb5fe9f9aa61dd5ae2c5d23710607))
* **automation:** stop the macerator from trying to load other mods' recipes ([#473](https://github.com/Indemnity83/logistics/issues/473)) ([f347495](https://github.com/Indemnity83/logistics/commit/f3474952fdbe8b879f218e9c6343e144c6799985))
* **automation:** stop the sawmill from silently rejecting seeds ([#824](https://github.com/Indemnity83/logistics/issues/824)) ([5a9bb60](https://github.com/Indemnity83/logistics/commit/5a9bb60e15997a18ad4a6bc6a82cc40eb95bfc40))
* **automation:** sync machine progress and energy so bars can't overflow ([#694](https://github.com/Indemnity83/logistics/issues/694)) ([e5a8991](https://github.com/Indemnity83/logistics/commit/e5a89911407b58ad620275c574c63b47db39c6a2))
* **ci:** stop Sentry release failing on cross-branch set-commits ([#683](https://github.com/Indemnity83/logistics/issues/683)) ([f175426](https://github.com/Indemnity83/logistics/commit/f17542687d2da52251ae5288797db7ca2f9019f4))
* **ci:** stop the release pipeline publishing broken releases ([0d947ea](https://github.com/Indemnity83/logistics/commit/0d947eadc9bc1597c57bb3faaf628a1b51492f59))
* **ci:** unblock release publishing broken by Sentry set-commits ([#678](https://github.com/Indemnity83/logistics/issues/678)) ([849df6b](https://github.com/Indemnity83/logistics/commit/849df6b88fe6abc79695f643c195da69f5c155d3))
* clamp energy values and transfer amounts to non-negative ([#399](https://github.com/Indemnity83/logistics/issues/399)) ([33f95ee](https://github.com/Indemnity83/logistics/commit/33f95ee3d74dab007c95aee9b2ce572b70e7c530))
* **compat:** add missing Jade fluid-pipe config translation ([#701](https://github.com/Indemnity83/logistics/issues/701)) ([fc7575d](https://github.com/Indemnity83/logistics/commit/fc7575d9116172214ee8afda1b59ffe9232221c7))
* **compat:** hide fluid extractor energy in Jade ([#533](https://github.com/Indemnity83/logistics/issues/533)) ([441669e](https://github.com/Indemnity83/logistics/commit/441669e67ae130841e3320122633caba79d648d5))
* **compat:** register Crucible with NeoForge Jade plugin ([#744](https://github.com/Indemnity83/logistics/issues/744)) ([c3ff99c](https://github.com/Indemnity83/logistics/commit/c3ff99c57cdff308521eb707c6fcbd61695ef499))
* **compat:** show values in the Jade HUD instead of only labels ([#757](https://github.com/Indemnity83/logistics/issues/757)) ([8aff490](https://github.com/Indemnity83/logistics/commit/8aff490a58dabf10718f15ecd2fe4fb501970a06))
* **compat:** stop Jade plugin error on dedicated servers ([#878](https://github.com/Indemnity83/logistics/issues/878)) ([906cfae](https://github.com/Indemnity83/logistics/commit/906cfaeb48753422f445ed774e063f9dc4c5eae0))
* **core:** give the Seed Oil Bucket its missing model and texture ([#904](https://github.com/Indemnity83/logistics/issues/904)) ([fce0010](https://github.com/Indemnity83/logistics/commit/fce001098c92929848e207ee1075c0a94d09cea4))
* **core:** let markers connect through solid blocks ([#581](https://github.com/Indemnity83/logistics/issues/581)) ([b392890](https://github.com/Indemnity83/logistics/commit/b3928905feec0475bc1b98fc5abf425d454e7459))
* **core:** restore valve and quartz crystal recipes ([#600](https://github.com/Indemnity83/logistics/issues/600)) ([b267476](https://github.com/Indemnity83/logistics/commit/b267476e70cc20d5c82e7071f512d233eb3b07ff))
* **core:** survive a malformed config file at startup ([53c2b7d](https://github.com/Indemnity83/logistics/commit/53c2b7deaf7335bc47e25b4e4c771147120f6859))
* correct engine block colors and transparency rendering ([#248](https://github.com/Indemnity83/logistics/issues/248)) ([9d1d08a](https://github.com/Indemnity83/logistics/commit/9d1d08adebf673495572d3f17d4921e9a81058fe))
* correct laser quarry edge case regressions ([#427](https://github.com/Indemnity83/logistics/issues/427)) ([f72c93a](https://github.com/Indemnity83/logistics/commit/f72c93a012acae06ac94a1f94b82aa9b2a04f88f))
* correct lever and dust placement on engine blocks ([#300](https://github.com/Indemnity83/logistics/issues/300)) ([6f912ca](https://github.com/Indemnity83/logistics/commit/6f912ca94b19b8db514504c9d8e119aff371bbe4))
* correct power cable compilation errors for mc/26.1 ([#357](https://github.com/Indemnity83/logistics/issues/357)) ([5b1ae4d](https://github.com/Indemnity83/logistics/commit/5b1ae4d89c772803b06f6b36c3279a4e445512ad))
* correct supplier module UI targeting ([#295](https://github.com/Indemnity83/logistics/issues/295)) ([128358a](https://github.com/Indemnity83/logistics/commit/128358af0cd2b00446926c12ab76e95f9d764edd))
* **crafting:** keep the sourceable remainder when a request is replanned ([cec951a](https://github.com/Indemnity83/logistics/commit/cec951a809ab02bbb8425d1dd9402f83f0ef7b7f))
* **crafting:** order only what was requested from a crafting batch ([cec951a](https://github.com/Indemnity83/logistics/commit/cec951a809ab02bbb8425d1dd9402f83f0ef7b7f))
* enable custom Minecraft version range in build workflows ([#414](https://github.com/Indemnity83/logistics/issues/414)) ([30ad892](https://github.com/Indemnity83/logistics/commit/30ad892eb8a30594c7d791679c15b97ec7677e5d))
* enchanted items being provided on the network ([#292](https://github.com/Indemnity83/logistics/issues/292)) ([1d24b3a](https://github.com/Indemnity83/logistics/commit/1d24b3ab7c0be50504085f468104a05fae85b8fa))
* **energy:** add catalyst engine JEI category ([#874](https://github.com/Indemnity83/logistics/issues/874)) ([7213542](https://github.com/Indemnity83/logistics/commit/72135420de187c89cdfd2c5e1f8e4fdb58aac978))
* **energy:** add crafting recipe for battery ([#510](https://github.com/Indemnity83/logistics/issues/510)) ([a101ad3](https://github.com/Indemnity83/logistics/commit/a101ad355dc8d27caa858d3f106f512460e8cfc1))
* **energy:** correct cable connections to engines ([#801](https://github.com/Indemnity83/logistics/issues/801)) ([567f65e](https://github.com/Indemnity83/logistics/commit/567f65e17242dfd4718e83477301c5e04f02796c))
* **energy:** drop the creative sink when broken ([#616](https://github.com/Indemnity83/logistics/issues/616)) ([2d92961](https://github.com/Indemnity83/logistics/commit/2d92961d7668658ab2675d083e2b32f94a12582b))
* **energy:** fix crash when cables power machines from other mods ([#556](https://github.com/Indemnity83/logistics/issues/556)) ([56f1d44](https://github.com/Indemnity83/logistics/commit/56f1d449cfa92148b72ced98283f30528194a5fb))
* **energy:** give engine and battery models their missing particle texture ([#909](https://github.com/Indemnity83/logistics/issues/909)) ([e47e1f1](https://github.com/Indemnity83/logistics/commit/e47e1f1205efb81caa9ed5dcf341c0d8c33df32c))
* **energy:** make engines visibly change color with heat stage ([#482](https://github.com/Indemnity83/logistics/issues/482)) ([681de24](https://github.com/Indemnity83/logistics/commit/681de2436cd9e926ed7b861fceef66aec447a1ee))
* **energy:** stop battery and cable taking forever to mine ([#839](https://github.com/Indemnity83/logistics/issues/839)) ([f5bbcfd](https://github.com/Indemnity83/logistics/commit/f5bbcfd442cd838184cb8d63a48769c1b48a32e3))
* **energy:** stop cables voiding power into slow machines ([874624f](https://github.com/Indemnity83/logistics/commit/874624f4553cc688a7bc02213a4610f4bb0a5ad9))
* **energy:** stop duplicating and voiding energy on Fabric ([1af1b32](https://github.com/Indemnity83/logistics/commit/1af1b32e80477d2f8858c6993dc1f976c6f33788))
* expose laser quarry configuration settings to user ([#277](https://github.com/Indemnity83/logistics/issues/277)) ([d51a24d](https://github.com/Indemnity83/logistics/commit/d51a24d7f1b41b1a5c38d11b0fa91a0a05d2bc50))
* **fluids:** drop fluid pipes and glass tank when broken ([#614](https://github.com/Indemnity83/logistics/issues/614)) ([5c47c62](https://github.com/Indemnity83/logistics/commit/5c47c628d54b0f0add33e513a2643df0fb1eb1b8))
* **fluids:** honor pipe transfer rates set above the default ([#885](https://github.com/Indemnity83/logistics/issues/885)) ([2b8df9f](https://github.com/Indemnity83/logistics/commit/2b8df9f7a3db99b1c15d352e7970d5aeea4dae28))
* **fluids:** never drop fluid packets on the ground ([#805](https://github.com/Indemnity83/logistics/issues/805)) ([441befb](https://github.com/Indemnity83/logistics/commit/441befb759087362d45e85181e0dbddbd1d53191))
* **fluids:** obscure vision and apply Nausea/Poison/Slowness in Crude Oil ([#848](https://github.com/Indemnity83/logistics/issues/848)) ([d86cb99](https://github.com/Indemnity83/logistics/commit/d86cb99179564f9ae9ed7f195322def625ca6377))
* **fluids:** render custom fluids in the look-at HUD ([#706](https://github.com/Indemnity83/logistics/issues/706)) ([4cb29af](https://github.com/Indemnity83/logistics/commit/4cb29af5423e98d48d42589a90f066b7584464a3))
* **fluids:** show correct fill level on tank and pipe HUDs ([#619](https://github.com/Indemnity83/logistics/issues/619)) ([0d8f2c1](https://github.com/Indemnity83/logistics/commit/0d8f2c1280b9fbd6155f16b0c8b12bf7000b22d0))
* **fluids:** stop drained pipes rendering a checkerboard ([#696](https://github.com/Indemnity83/logistics/issues/696)) ([ceece30](https://github.com/Indemnity83/logistics/commit/ceece3008c47c4569894610d7f40c5886e1d7c43))
* **fluids:** stop fluid extractor pipes from connecting to each other ([#692](https://github.com/Indemnity83/logistics/issues/692)) ([97b68df](https://github.com/Indemnity83/logistics/commit/97b68dfaa35104a4fbc5a771bfa5f77ceb530b06))
* **fluids:** stop over-capacity saved tank amounts from crashing on load ([#849](https://github.com/Indemnity83/logistics/issues/849)) ([5f603b5](https://github.com/Indemnity83/logistics/commit/5f603b5dd5c3a9cd6fa2a9a4e789c0883e0960bd))
* **fluids:** stop suppliers requesting fluid/items with no room ([#804](https://github.com/Indemnity83/logistics/issues/804)) ([a41190a](https://github.com/Indemnity83/logistics/commit/a41190ac5096919ce7e5e08eb2fd40a4e9e338dd))
* handle failed deliveries in logistics network tracking ([#397](https://github.com/Indemnity83/logistics/issues/397)) ([159df97](https://github.com/Indemnity83/logistics/commit/159df97614ca0abee91a89dc8ff739850f5d00a6))
* **kiln:** accept energy from the power network ([#602](https://github.com/Indemnity83/logistics/issues/602)) ([d7bc02a](https://github.com/Indemnity83/logistics/commit/d7bc02a98e586eff3aaf7668cd285fa3d2006f91))
* **kiln:** bank smelting XP and pay it out like a furnace ([#605](https://github.com/Indemnity83/logistics/issues/605)) ([8bfa074](https://github.com/Indemnity83/logistics/commit/8bfa074fa20ef5ed89ece0b3913adfdc96185313))
* **kiln:** mine with the correct pickaxe tier ([#601](https://github.com/Indemnity83/logistics/issues/601)) ([d59f801](https://github.com/Indemnity83/logistics/commit/d59f801c3d49cf181a57facf865da320ebfbf0c5))
* **macerator:** bank maceration XP and pay it out like a furnace ([8bfa074](https://github.com/Indemnity83/logistics/commit/8bfa074fa20ef5ed89ece0b3913adfdc96185313))
* **macerator:** grant XP for macerating ancient debris ([#624](https://github.com/Indemnity83/logistics/issues/624)) ([68a5ce5](https://github.com/Indemnity83/logistics/commit/68a5ce5cc36aae0dec1bd899dc61846507cdaaf9))
* **macerator:** grind loose raw ore items into dust ([#838](https://github.com/Indemnity83/logistics/issues/838)) ([a20d3f1](https://github.com/Indemnity83/logistics/commit/a20d3f154793357a5a41a4ecf5dc50b652f4e9bf))
* **macerator:** restore the JEI integration ([#599](https://github.com/Indemnity83/logistics/issues/599)) ([3c02705](https://github.com/Indemnity83/logistics/commit/3c02705580530a9bda2cf03c56bde8c0e0304935))
* **neoforge:** correct energy bridge insert/extract accounting ([e10bdfe](https://github.com/Indemnity83/logistics/commit/e10bdfeab1966cd4b8e17a65a9643e18df87e7c6))
* **neoforge:** fix startup crash when JEI is on the classpath ([#453](https://github.com/Indemnity83/logistics/issues/453)) ([d45efef](https://github.com/Indemnity83/logistics/commit/d45efefa3640efd8154c5984492be0fadfe67d3c))
* **neoforge:** let pipes and cables interact with the refinery and sawmill ([#733](https://github.com/Indemnity83/logistics/issues/733)) ([0da9eee](https://github.com/Indemnity83/logistics/commit/0da9eeeecf48debd2cf9a121dcf5a23a1c29a8f0))
* **neoforge:** stop the creative menu crashing on duplicate items ([#702](https://github.com/Indemnity83/logistics/issues/702)) ([ed04eb7](https://github.com/Indemnity83/logistics/commit/ed04eb7cc0ad9093bbbfe24755ef85ce3da88fd5))
* normalize laser quarry recipe to use machine core ([#267](https://github.com/Indemnity83/logistics/issues/267)) ([c734a2e](https://github.com/Indemnity83/logistics/commit/c734a2e9bfd63590fa69e8398b2926a133632560))
* **pipes:** apply config changes to modules installed in a chassis ([#504](https://github.com/Indemnity83/logistics/issues/504)) ([19b2a61](https://github.com/Indemnity83/logistics/commit/19b2a61594bf11fc835a026da6bcf5ce2cd1cf99)), closes [#494](https://github.com/Indemnity83/logistics/issues/494)
* **pipes:** fix fluid pipe drain flicker ([#532](https://github.com/Indemnity83/logistics/issues/532)) ([eec7f47](https://github.com/Indemnity83/logistics/commit/eec7f47df920ee23f4cf4516627afde3cda073ea))
* **pipes:** fix missing pixel in logistics power junction animation ([#637](https://github.com/Indemnity83/logistics/issues/637)) ([4bd6261](https://github.com/Indemnity83/logistics/commit/4bd62610fb6e74707c2929bc19880d24d64f39c1))
* preserve item components in filter pipe slots across save/reload ([#386](https://github.com/Indemnity83/logistics/issues/386)) ([6e450c4](https://github.com/Indemnity83/logistics/commit/6e450c4cfa28c9335095b38a2d2e981487bfaad7))
* prevent component-bearing items from sharing crafter slot ([#302](https://github.com/Indemnity83/logistics/issues/302)) ([ab6b03c](https://github.com/Indemnity83/logistics/commit/ab6b03c5dbb799dd6600b8911499ec6cbd935418))
* prevent passive supplier from overfilling inventory items ([#303](https://github.com/Indemnity83/logistics/issues/303)) ([33792f0](https://github.com/Indemnity83/logistics/commit/33792f07093086eebe98f0e3c3d0dd72418b8b53))
* **pump:** accept power from any energy source ([#651](https://github.com/Indemnity83/logistics/issues/651)) ([89966f8](https://github.com/Indemnity83/logistics/commit/89966f8658d2335c3016f9ab8d238e68f5143970))
* **pump:** clamp the fluid pump search radius ([#625](https://github.com/Indemnity83/logistics/issues/625)) ([4a55de1](https://github.com/Indemnity83/logistics/commit/4a55de1562f987ddb3e74bcf8193492669cb5f45))
* **pump:** keep long intake tubes visible off screen ([10aff3f](https://github.com/Indemnity83/logistics/commit/10aff3ff463c13e29fce76e90b431d7cbe082b3a))
* **pump:** make the pump tank output-only ([#693](https://github.com/Indemnity83/logistics/issues/693)) ([b9a814f](https://github.com/Indemnity83/logistics/commit/b9a814f1e98010648bdf446713c393795a0ef54c))
* **pump:** match the fluid pump's top to the other machines ([#561](https://github.com/Indemnity83/logistics/issues/561)) ([7557f5e](https://github.com/Indemnity83/logistics/commit/7557f5eafa7d096109022d0da6d561a5467454ce))
* **pump:** mine the fluid pump with the correct pickaxe tier ([5c47c62](https://github.com/Indemnity83/logistics/commit/5c47c628d54b0f0add33e513a2643df0fb1eb1b8))
* **pump:** stop destroying waterlogged blocks ([71dd4df](https://github.com/Indemnity83/logistics/commit/71dd4dfbd6008f245cfb181e7642428e63c6859f))
* **pump:** stop the intake tube descending through waterlogged blocks ([13a6d10](https://github.com/Indemnity83/logistics/commit/13a6d105885c8226a13058e9620563fcb6820d99)), closes [#968](https://github.com/Indemnity83/logistics/issues/968) [#969](https://github.com/Indemnity83/logistics/issues/969)
* **pump:** sync tube animation on phase changes ([71dd4df](https://github.com/Indemnity83/logistics/commit/71dd4dfbd6008f245cfb181e7642428e63c6859f))
* **quarry:** fix immediate crash on published Fabric builds ([#798](https://github.com/Indemnity83/logistics/issues/798)) ([3e71220](https://github.com/Indemnity83/logistics/commit/3e71220e5f655ccf04af62be3bdcb46757a6b58c))
* **quarry:** give the laser quarry frame a display name ([#617](https://github.com/Indemnity83/logistics/issues/617)) ([4922962](https://github.com/Indemnity83/logistics/commit/4922962ab7bf1e9c7782879f9dbd12f0c823ee49))
* **quarry:** keep marker beams visible off screen ([10aff3f](https://github.com/Indemnity83/logistics/commit/10aff3ff463c13e29fce76e90b431d7cbe082b3a)), closes [#940](https://github.com/Indemnity83/logistics/issues/940)
* **quarry:** keep the frame and laser visible off screen ([10aff3f](https://github.com/Indemnity83/logistics/commit/10aff3ff463c13e29fce76e90b431d7cbe082b3a))
* **quarry:** let players break abandoned frames in survival ([ec49e6a](https://github.com/Indemnity83/logistics/commit/ec49e6a2766cacf42251bd5499b39e7d4a9f2e6d))
* **quarry:** mine waterlogged blocks instead of skipping them ([13a6d10](https://github.com/Indemnity83/logistics/commit/13a6d105885c8226a13058e9620563fcb6820d99))
* **quarry:** remove the unintended duplicate recipe ([5317b03](https://github.com/Indemnity83/logistics/commit/5317b03f2cfef743cdbc159bf0ec523171cdc865))
* **quarry:** stop a zero arm speed freezing the quarry forever ([1ada0bd](https://github.com/Indemnity83/logistics/commit/1ada0bdb27de31a9bda8e6cb248fa1383bfa5350))
* **quarry:** stop frame blocks vanishing without warning ([ec49e6a](https://github.com/Indemnity83/logistics/commit/ec49e6a2766cacf42251bd5499b39e7d4a9f2e6d))
* **quarry:** stop the arm mining through lava or ignoring reappeared blocks ([#850](https://github.com/Indemnity83/logistics/issues/850)) ([a449259](https://github.com/Indemnity83/logistics/commit/a4492590af782891dd1496da7dba8adbd49f79ac))
* **quarry:** stop vacuuming loose items off the ground ([dc6d6d0](https://github.com/Indemnity83/logistics/commit/dc6d6d0938c929605358b396309d42ad245d439e)), closes [#973](https://github.com/Indemnity83/logistics/issues/973)
* relocate quartz crystal asset files to core directory ([#305](https://github.com/Indemnity83/logistics/issues/305)) ([e075108](https://github.com/Indemnity83/logistics/commit/e075108e8f124c74c82405825ffcb69cebd19219))
* remove orphaned advanced extractor module assets and update JEI entrypoint ([#256](https://github.com/Indemnity83/logistics/issues/256)) ([cd0d626](https://github.com/Indemnity83/logistics/commit/cd0d626ea4df38b84693f6e54324010ec41ba0b0))
* rename MACHINE_FRAME to MACHINE_CORE and update assets ([#265](https://github.com/Indemnity83/logistics/issues/265)) ([120e024](https://github.com/Indemnity83/logistics/commit/120e0241772886eb2c8625ca042f51d4501bb7d3))
* resolve pipe network registration issues on load ([#269](https://github.com/Indemnity83/logistics/issues/269)) ([35c1f00](https://github.com/Indemnity83/logistics/commit/35c1f00d4f0055c016cc7ba44c5d9f793e456dbb))
* resolve vanishing filters in diamond pipes serialization ([#288](https://github.com/Indemnity83/logistics/issues/288)) ([dfe2440](https://github.com/Indemnity83/logistics/commit/dfe24400c6e7e0e25678a8246cf461b929588f4c))
* **routing:** apply one interaction range to every pipe menu ([8a8cf3c](https://github.com/Indemnity83/logistics/commit/8a8cf3c3e281c70cab73965dc1c59da238f23088)), closes [#937](https://github.com/Indemnity83/logistics/issues/937) [#942](https://github.com/Indemnity83/logistics/issues/942)
* **routing:** cancel the order behind a job that has finished ([cec951a](https://github.com/Indemnity83/logistics/commit/cec951a809ab02bbb8425d1dd9402f83f0ef7b7f)), closes [#933](https://github.com/Indemnity83/logistics/issues/933) [#944](https://github.com/Indemnity83/logistics/issues/944) [#947](https://github.com/Indemnity83/logistics/issues/947)
* **routing:** close module menus when the pipe is broken ([8a8cf3c](https://github.com/Indemnity83/logistics/commit/8a8cf3c3e281c70cab73965dc1c59da238f23088))
* **routing:** drop the failed order's index entry on delivery retry ([482c223](https://github.com/Indemnity83/logistics/commit/482c223f8c35093880c60977f5eedcef4d0487f6))
* **routing:** fall through to the next provider when one is fully reserved ([0f2992e](https://github.com/Indemnity83/logistics/commit/0f2992ea7dff2c0bd4e87d7d1b8a5c41faba4918)), closes [#928](https://github.com/Indemnity83/logistics/issues/928) [#938](https://github.com/Indemnity83/logistics/issues/938) [#939](https://github.com/Indemnity83/logistics/issues/939)
* **routing:** keep chassis modules when a pipe explodes ([#629](https://github.com/Indemnity83/logistics/issues/629)) ([d86d5a7](https://github.com/Indemnity83/logistics/commit/d86d5a78fb117458c7ab2713ddada88ef7e1e735))
* **routing:** refresh neighbor pipe arms when markings change ([#606](https://github.com/Indemnity83/logistics/issues/606)) ([eb260e7](https://github.com/Indemnity83/logistics/commit/eb260e7c9dce2c23901126cd355a2b405440a3d3))
* **routing:** release only the delivered part of a shipment ([0f2992e](https://github.com/Indemnity83/logistics/commit/0f2992ea7dff2c0bd4e87d7d1b8a5c41faba4918))
* **routing:** render the right pipe arm after a neighbour changes ([ba9b068](https://github.com/Indemnity83/logistics/commit/ba9b068cb091d246aaa7ee9641d5e30f34c7c69a))
* **routing:** stop a bad button id crashing the server ([7ec222a](https://github.com/Indemnity83/logistics/commit/7ec222a9104283ed066b66715e274b204f860bd4))
* **routing:** stop broadcasting requester contents to every player ([e338f59](https://github.com/Indemnity83/logistics/commit/e338f59bb62c548a085b752f579f71b5ef62ca15))
* **routing:** stop failed deliveries shrinking a provider's stock ([0f2992e](https://github.com/Indemnity83/logistics/commit/0f2992ea7dff2c0bd4e87d7d1b8a5c41faba4918))
* **routing:** stop Providers destroying items on an unpowered network ([89bb2c0](https://github.com/Indemnity83/logistics/commit/89bb2c0b797755da28775379197e5bb359bca0ee))
* **routing:** stop the Provider MkII shipping the wrong item to the next order ([a5d8d56](https://github.com/Indemnity83/logistics/commit/a5d8d5679dfecfecd296fb26e890be964a8e5d14))
* **routing:** treat requester deliveries with no inventory as fulfilled ([#847](https://github.com/Indemnity83/logistics/issues/847)) ([a8cc2c5](https://github.com/Indemnity83/logistics/commit/a8cc2c56f152f7de89ca67062ffd98bffcff078a))
* **sawmill:** accept single-item deliveries for batched recipes ([#827](https://github.com/Indemnity83/logistics/issues/827)) ([6e23930](https://github.com/Indemnity83/logistics/commit/6e239301c2ed4d0fb6eb6a18843e946a27cf9c1d))
* **sawmill:** add the missing crafting recipe ([#609](https://github.com/Indemnity83/logistics/issues/609)) ([1c4a125](https://github.com/Indemnity83/logistics/commit/1c4a1256f3572e1f215fc4163de41aefd0f8bd50))
* **sawmill:** mine with the correct pickaxe tier ([95ae978](https://github.com/Indemnity83/logistics/commit/95ae9786129fd4a7bd2073e964cf4127a7dc4dda))
* show Macerator recipe category in JEI on NeoForge ([#390](https://github.com/Indemnity83/logistics/issues/390)) ([db73fd1](https://github.com/Indemnity83/logistics/commit/db73fd1d0aee86e5493063fdcb5d4cc0dd986f7c))
* split loader-specific marking fluid recipes ([#382](https://github.com/Indemnity83/logistics/issues/382)) ([def0ad0](https://github.com/Indemnity83/logistics/commit/def0ad061430c33323a0c295eea293e241981297))
* tooltips for items in requester screen ([#294](https://github.com/Indemnity83/logistics/issues/294)) ([720b699](https://github.com/Indemnity83/logistics/commit/720b699b901685fa01d9586c327ba473a9a171fc))
* track META-INF service files blocked by overly broad gitignore ([affc30f](https://github.com/Indemnity83/logistics/commit/affc30feb49ecec0597965ea41717caa81bc7f06))
* **transport:** stop losing items on a partial pipe handoff ([0da6707](https://github.com/Indemnity83/logistics/commit/0da670710abaf211dd9d5a04ef978f02bd6df7fb))
* **ui:** fix glass tank capacity overlay with held items ([#534](https://github.com/Indemnity83/logistics/issues/534)) ([bbe20d4](https://github.com/Indemnity83/logistics/commit/bbe20d457c3748d37175e554cf9e4ffedc1e6b35))
* update build script to check Gradle task by MC version ([#249](https://github.com/Indemnity83/logistics/issues/249)) ([fe5d9db](https://github.com/Indemnity83/logistics/commit/fe5d9dbdbcca1e62ec14433f5504aee3888bf2d5))
* update macerator tags for mining and loot table renaming ([#266](https://github.com/Indemnity83/logistics/issues/266)) ([95f7131](https://github.com/Indemnity83/logistics/commit/95f71314e403e8f0b940160f572a8993c2c93e48))
* update Minecraft version compatibility range for NeoForge ([#416](https://github.com/Indemnity83/logistics/issues/416)) ([b1c384d](https://github.com/Indemnity83/logistics/commit/b1c384d8dc16c6d124a594d40f362f4b9c4262f2))
* use RegistryOps to fix enchanted item crash ([#283](https://github.com/Indemnity83/logistics/issues/283)) ([967be91](https://github.com/Indemnity83/logistics/commit/967be91480f14b35090fa8f3b651af2511704a37))

## [0.8.5](https://github.com/Indemnity83/logistics/compare/mc26.2-v0.8.4...mc26.2-v0.8.5) (2026-08-03)


### Changed

* **energy:** gate engine harvesting by tool tier ([#784](https://github.com/Indemnity83/logistics/issues/784)) ([820a5e6](https://github.com/Indemnity83/logistics/commit/820a5e6c12cc4b22351c09ccf02d2e024468752f))


### Fixed

* **energy:** correct cable connections to engines ([#801](https://github.com/Indemnity83/logistics/issues/801)) ([567f65e](https://github.com/Indemnity83/logistics/commit/567f65e17242dfd4718e83477301c5e04f02796c))
* **quarry:** fix immediate crash on published Fabric builds ([#798](https://github.com/Indemnity83/logistics/issues/798)) ([3e71220](https://github.com/Indemnity83/logistics/commit/3e71220e5f655ccf04af62be3bdcb46757a6b58c)) — thanks @WerWebWer

### New Contributors

* @WerWebWer made their first contribution in #798

## [0.8.4](https://github.com/Indemnity83/logistics/compare/mc26.2-v0.8.3...mc26.2-v0.8.4) (2026-07-24)


### Added

* **energy:** add the Fuel Engine ([#759](https://github.com/Indemnity83/logistics/issues/759)) ([3ead98b](https://github.com/Indemnity83/logistics/commit/3ead98bb07a20b4ba1dbb8f9d60a2f8ace46f6a1))
* **energy:** add the Magmatic Engine ([#776](https://github.com/Indemnity83/logistics/issues/776)) ([8cb6069](https://github.com/Indemnity83/logistics/commit/8cb60697f0459297d3ccf789166c47e453a656a0))
* **energy:** add the Reaction Engine ([#777](https://github.com/Indemnity83/logistics/issues/777)) ([0e31e65](https://github.com/Indemnity83/logistics/commit/0e31e6512a7460c5050b2af1c8ce0abc67859b95))
* **energy:** add the Steam Engine ([#765](https://github.com/Indemnity83/logistics/issues/765)) ([8f7d8fd](https://github.com/Indemnity83/logistics/commit/8f7d8fda95acf33c1adfafcd6b427c65eb7cb971))


### Changed

* **core:** retexture the bronze and tin metal blocks and items ([#766](https://github.com/Indemnity83/logistics/issues/766)) ([3b2a686](https://github.com/Indemnity83/logistics/commit/3b2a686d24728b4c66449f9cb2344a68602f35d8))
* **energy:** restyle the Stirling Engine GUI ([#763](https://github.com/Indemnity83/logistics/issues/763)) ([901cf64](https://github.com/Indemnity83/logistics/commit/901cf64aa9370d14e975e71da0baeebcce821628))

## [0.8.3](https://github.com/Indemnity83/logistics/compare/mc26.2-v0.8.2...mc26.2-v0.8.3) (2026-07-20)


### Added

* **automation:** add the refinery ([#714](https://github.com/Indemnity83/logistics/issues/714)) ([0ace76c](https://github.com/Indemnity83/logistics/commit/0ace76c237ec785f898573e70dbc64f32feaca14))
* **automation:** add the sequential fabricator ([#719](https://github.com/Indemnity83/logistics/issues/719)) ([ae6321b](https://github.com/Indemnity83/logistics/commit/ae6321bc4008dac305767cdaa0176726ff54a153))
* **automation:** add the sequential fabricator ([#719](https://github.com/Indemnity83/logistics/issues/719)) ([ae6321b](https://github.com/Indemnity83/logistics/commit/ae6321bc4008dac305767cdaa0176726ff54a153))
* **automation:** fabricate chipsets in the sequential fabricator ([#720](https://github.com/Indemnity83/logistics/issues/720)) ([a07c428](https://github.com/Indemnity83/logistics/commit/a07c428d5cbc65baec7384b2b7e9b79cbe6c0474))
* **automation:** fabricate chipsets in the sequential fabricator ([#720](https://github.com/Indemnity83/logistics/issues/720)) ([a07c428](https://github.com/Indemnity83/logistics/commit/a07c428d5cbc65baec7384b2b7e9b79cbe6c0474))
* **automation:** make machine tuning configurable per machine ([#713](https://github.com/Indemnity83/logistics/issues/713)) ([1c48581](https://github.com/Indemnity83/logistics/commit/1c48581a6431fdbf98004c6c098af01e798b942b))
* **compat:** add Jade HUD support for refinery and sequential fabricator ([#744](https://github.com/Indemnity83/logistics/issues/744)) ([c3ff99c](https://github.com/Indemnity83/logistics/commit/c3ff99c57cdff308521eb707c6fcbd61695ef499))
* **core:** add a copper nugget ([#722](https://github.com/Indemnity83/logistics/issues/722)) ([9629b47](https://github.com/Indemnity83/logistics/commit/9629b4714885dab8439b4dc8ab7795e8938f0dd7))
* **crafting:** add tin, rubber, amethyst, and echo valves ([139d341](https://github.com/Indemnity83/logistics/commit/139d341ed2bc9dc8fec1e1ea9ab0fcdfb43b70ab))
* **crafting:** rework the valve lineup with a bench recipe ([#727](https://github.com/Indemnity83/logistics/issues/727)) ([139d341](https://github.com/Indemnity83/logistics/commit/139d341ed2bc9dc8fec1e1ea9ab0fcdfb43b70ab))
* **crafting:** show sequential fabricator recipes in JEI ([#745](https://github.com/Indemnity83/logistics/issues/745)) ([9fa46ea](https://github.com/Indemnity83/logistics/commit/9fa46eae97629bfa06b1e5f131b62013c4ce5f57))
* **energy:** add natural and synthetic polymers for rubber ([#716](https://github.com/Indemnity83/logistics/issues/716)) ([07b678a](https://github.com/Indemnity83/logistics/commit/07b678a533396180ed561b217934a6dfbf9b6eee))
* **fluids:** add bio fuel and fuel oil fluids ([0ace76c](https://github.com/Indemnity83/logistics/commit/0ace76c237ec785f898573e70dbc64f32feaca14))
* **fluids:** add tar as an alternative fluid pipe sealant ([#715](https://github.com/Indemnity83/logistics/issues/715)) ([a6c2fba](https://github.com/Indemnity83/logistics/commit/a6c2fba82c57dd78e35c1995c23d40736d8373f9))
* **routing:** add chipset crafting alternatives for pipes and modules ([5747612](https://github.com/Indemnity83/logistics/commit/5747612932b87783645baac039558ecd3b0f2f65))
* **ui:** split creative menu into domain tabs ([#738](https://github.com/Indemnity83/logistics/issues/738)) ([74a0f99](https://github.com/Indemnity83/logistics/commit/74a0f991e0ac046647e7066dd4b608fa0b6280f8))


### Changed

* **crafting:** craft valves from quartz, redstone, and a base material ([139d341](https://github.com/Indemnity83/logistics/commit/139d341ed2bc9dc8fec1e1ea9ab0fcdfb43b70ab))
* **crafting:** restyle every valve with a distinct electron-tube texture ([139d341](https://github.com/Indemnity83/logistics/commit/139d341ed2bc9dc8fec1e1ea9ab0fcdfb43b70ab))
* **fluids:** restyle the fluid pipe textures ([#740](https://github.com/Indemnity83/logistics/issues/740)) ([4351016](https://github.com/Indemnity83/logistics/commit/4351016da8e408a7f4e66a5dc5fb58f01eb86513))
* **macerator:** recycle netherite dust to and from ingots ([#734](https://github.com/Indemnity83/logistics/issues/734)) ([3022ce0](https://github.com/Indemnity83/logistics/commit/3022ce07b8d50a52b93fd2969313f4bdcce1cbe4))
* **routing:** rework logistics pipe recipes with chipset alternatives ([#721](https://github.com/Indemnity83/logistics/issues/721)) ([5747612](https://github.com/Indemnity83/logistics/commit/5747612932b87783645baac039558ecd3b0f2f65))
* **routing:** use a copper nugget in the blank module recipe ([9629b47](https://github.com/Indemnity83/logistics/commit/9629b4714885dab8439b4dc8ab7795e8938f0dd7))
* **transport:** lighten the item extractor pipe to distinguish it from oxidized copper ([4351016](https://github.com/Indemnity83/logistics/commit/4351016da8e408a7f4e66a5dc5fb58f01eb86513))


### Removed

* **crafting:** drop the wooden and ender valves ([139d341](https://github.com/Indemnity83/logistics/commit/139d341ed2bc9dc8fec1e1ea9ab0fcdfb43b70ab))


### Fixed

* **automation:** make the refinery and sequential fabricator harvestable ([31dfc5d](https://github.com/Indemnity83/logistics/commit/31dfc5df8de2d497f4f55f518931ca6468d54b54))
* **automation:** show machine recipes in JEI on multiplayer clients ([#735](https://github.com/Indemnity83/logistics/issues/735)) ([57b2901](https://github.com/Indemnity83/logistics/commit/57b2901b5c59761ffc74581541d74486cf44a373))
* **compat:** register Crucible with NeoForge Jade plugin ([#744](https://github.com/Indemnity83/logistics/issues/744)) ([c3ff99c](https://github.com/Indemnity83/logistics/commit/c3ff99c57cdff308521eb707c6fcbd61695ef499))
* **compat:** show values in the Jade HUD instead of only labels ([#757](https://github.com/Indemnity83/logistics/issues/757)) ([8aff490](https://github.com/Indemnity83/logistics/commit/8aff490a58dabf10718f15ecd2fe4fb501970a06))
* **neoforge:** let pipes and cables interact with the refinery and sawmill ([#733](https://github.com/Indemnity83/logistics/issues/733)) ([0da9eee](https://github.com/Indemnity83/logistics/commit/0da9eeeecf48debd2cf9a121dcf5a23a1c29a8f0))

## [0.8.2](https://github.com/Indemnity83/logistics/compare/mc26.2-v0.8.1...mc26.2-v0.8.2) (2026-07-04)


### Added

* **automation:** add pulped biomass from the sawmill ([#660](https://github.com/Indemnity83/logistics/issues/660)) ([6a83220](https://github.com/Indemnity83/logistics/commit/6a83220185f68f8298543e7745b90191f4f7f457))
* **automation:** add the alloy smelter ([#656](https://github.com/Indemnity83/logistics/issues/656)) ([3ee3325](https://github.com/Indemnity83/logistics/commit/3ee33250f5673033c29f7a014080db2ab0b38c20))
* **automation:** add the crucible ([#679](https://github.com/Indemnity83/logistics/issues/679)) ([e3eff42](https://github.com/Indemnity83/logistics/commit/e3eff42eacf4d61240bde8d738d5bd2f40a26833))
* **fluids:** add custom fluids and buckets for the Magma Crucible ([6b988d6](https://github.com/Indemnity83/logistics/commit/6b988d63216ce054919f434a2381b82b24ff0e6c))
* **fluids:** light-emitting fluids glow in pipes and tanks ([#695](https://github.com/Indemnity83/logistics/issues/695)) ([c713692](https://github.com/Indemnity83/logistics/commit/c7136928281c87322ceaa65792e926a5b6d72710))
* **fluids:** show machine fluid tanks in the Jade HUD ([#703](https://github.com/Indemnity83/logistics/issues/703)) ([ad8c057](https://github.com/Indemnity83/logistics/commit/ad8c057ac461210ad27f2b652a2fa522fc8dcce6))
* **worldgen:** add bog earth and peat fuel ([#658](https://github.com/Indemnity83/logistics/issues/658)) ([f6b9b17](https://github.com/Indemnity83/logistics/commit/f6b9b17bf83ed83876ab769cf9203dcba65f07f4))
* **worldgen:** add crude oil and the oil chain ([#690](https://github.com/Indemnity83/logistics/issues/690)) ([f89aa1a](https://github.com/Indemnity83/logistics/commit/f89aa1aa2afe648bb3f567f41cf4cc6bf826843e))


### Fixed

* **automation:** reject non-finite recipe experience values ([bca2ef4](https://github.com/Indemnity83/logistics/commit/bca2ef498309e59391dc1d753fb6d056ab5ea35a))
* **automation:** sync machine progress and energy so bars can't overflow ([#694](https://github.com/Indemnity83/logistics/issues/694)) ([e5a8991](https://github.com/Indemnity83/logistics/commit/e5a89911407b58ad620275c574c63b47db39c6a2))
* **ci:** stop Sentry release failing on cross-branch set-commits ([#683](https://github.com/Indemnity83/logistics/issues/683)) ([f175426](https://github.com/Indemnity83/logistics/commit/f17542687d2da52251ae5288797db7ca2f9019f4))
* **ci:** unblock release publishing broken by Sentry set-commits ([#678](https://github.com/Indemnity83/logistics/issues/678)) ([849df6b](https://github.com/Indemnity83/logistics/commit/849df6b88fe6abc79695f643c195da69f5c155d3))
* **compat:** add missing Jade fluid-pipe config translation ([#701](https://github.com/Indemnity83/logistics/issues/701)) ([fc7575d](https://github.com/Indemnity83/logistics/commit/fc7575d9116172214ee8afda1b59ffe9232221c7))
* **fluids:** render custom fluids in the look-at HUD ([#706](https://github.com/Indemnity83/logistics/issues/706)) ([4cb29af](https://github.com/Indemnity83/logistics/commit/4cb29af5423e98d48d42589a90f066b7584464a3))
* **fluids:** stop drained pipes rendering a checkerboard ([#696](https://github.com/Indemnity83/logistics/issues/696)) ([ceece30](https://github.com/Indemnity83/logistics/commit/ceece3008c47c4569894610d7f40c5886e1d7c43))
* **fluids:** stop fluid extractor pipes from connecting to each other ([#692](https://github.com/Indemnity83/logistics/issues/692)) ([97b68df](https://github.com/Indemnity83/logistics/commit/97b68dfaa35104a4fbc5a771bfa5f77ceb530b06))
* **neoforge:** stop the creative menu crashing on duplicate items ([#702](https://github.com/Indemnity83/logistics/issues/702)) ([ed04eb7](https://github.com/Indemnity83/logistics/commit/ed04eb7cc0ad9093bbbfe24755ef85ce3da88fd5))
* **pump:** make the pump tank output-only ([#693](https://github.com/Indemnity83/logistics/issues/693)) ([b9a814f](https://github.com/Indemnity83/logistics/commit/b9a814f1e98010648bdf446713c393795a0ef54c))
* **routing:** drop the failed order's index entry on delivery retry ([482c223](https://github.com/Indemnity83/logistics/commit/482c223f8c35093880c60977f5eedcef4d0487f6))

## [0.8.1](https://github.com/Indemnity83/logistics/compare/mc26.2-v0.8.0...mc26.2-v0.8.1) (2026-06-30)


### Removed

* **core:** drop the unused sturdy casing ([#650](https://github.com/Indemnity83/logistics/issues/650)) ([8b2900b](https://github.com/Indemnity83/logistics/commit/8b2900bb2e0e8908589c1fa430e8077f7a8d7d82))


### Fixed

* **pump:** accept power from any energy source ([#651](https://github.com/Indemnity83/logistics/issues/651)) ([89966f8](https://github.com/Indemnity83/logistics/commit/89966f8658d2335c3016f9ab8d238e68f5143970))

## [0.8.0](https://github.com/Indemnity83/logistics/compare/mc26.2-v0.7.4...mc26.2-v0.8.0) (2026-06-29)


### ⚠ BREAKING CHANGES

* **energy:** Cables and batteries no longer power extraction pipes or the Fluid Pump. Only a directly-adjacent engine can power them. Existing setups that fed pipes through cables/batteries will stop working — place an engine against the pipe instead.
* **energy:** Batteries no longer power a logistics network directly. A network is powered only through a Power Junction — place one between your power source (cables/batteries) and the network. Existing battery-on-a-pipe setups stop working until a Power Junction is added.

### Added

* **core:** add opt-in sanitized crash reporting ([#633](https://github.com/Indemnity83/logistics/issues/633)) ([826792d](https://github.com/Indemnity83/logistics/commit/826792ddb747f60169a4bb7dacb0827e57d4c201))
* **core:** drop niter from the breeze ([#644](https://github.com/Indemnity83/logistics/issues/644)) ([173823e](https://github.com/Indemnity83/logistics/commit/173823ef45df009cb4b47cd74e5d0c7627c28c44))
* **crafting:** craft gunpowder from coal, sulfur, and niter dust ([86a9c71](https://github.com/Indemnity83/logistics/commit/86a9c7128cbfd27a160f777ce1dcca3c177231c8))
* **energy:** add the power junction ([#612](https://github.com/Indemnity83/logistics/issues/612)) ([e33e50a](https://github.com/Indemnity83/logistics/commit/e33e50a018a833049a4796f84ec76a5cae21ad8b))
* **macerator:** add chance byproducts to ore processing ([#643](https://github.com/Indemnity83/logistics/issues/643)) ([86a9c71](https://github.com/Indemnity83/logistics/commit/86a9c7128cbfd27a160f777ce1dcca3c177231c8))
* **macerator:** add recycling recipes for common blocks ([86a9c71](https://github.com/Indemnity83/logistics/commit/86a9c7128cbfd27a160f777ce1dcca3c177231c8))
* **macerator:** add Sulfur Dust, Quicksilver, and Niter items ([86a9c71](https://github.com/Indemnity83/logistics/commit/86a9c7128cbfd27a160f777ce1dcca3c177231c8))
* **macerator:** macerate breeze rods into wind charges ([86a9c71](https://github.com/Indemnity83/logistics/commit/86a9c7128cbfd27a160f777ce1dcca3c177231c8))
* **macerator:** macerate cinnabar and sulfur blocks into quicksilver and sulfur dust ([86a9c71](https://github.com/Indemnity83/logistics/commit/86a9c7128cbfd27a160f777ce1dcca3c177231c8))
* **macerator:** macerate logs and planks into sawdust and recycle wooden tools ([86a9c71](https://github.com/Indemnity83/logistics/commit/86a9c7128cbfd27a160f777ce1dcca3c177231c8))
* **macerator:** macerate sulfur spikes into sulfur dust ([86a9c71](https://github.com/Indemnity83/logistics/commit/86a9c7128cbfd27a160f777ce1dcca3c177231c8))
* **macerator:** recycle diamond tools and armor into diamonds ([86a9c71](https://github.com/Indemnity83/logistics/commit/86a9c7128cbfd27a160f777ce1dcca3c177231c8))
* **sawmill:** add a recipe book to the sawmill GUI ([#642](https://github.com/Indemnity83/logistics/issues/642)) ([859698a](https://github.com/Indemnity83/logistics/commit/859698ac9cd15b02447a328cf6decb29cafc414f))
* **sawmill:** add wood processing ([#580](https://github.com/Indemnity83/logistics/issues/580)) ([d26ff18](https://github.com/Indemnity83/logistics/commit/d26ff182324bad9771fd854ca2ed60df7f647bcd))
* **sawmill:** show recipes in JEI and details in the Jade HUD ([#613](https://github.com/Indemnity83/logistics/issues/613)) ([95ae978](https://github.com/Indemnity83/logistics/commit/95ae9786129fd4a7bd2073e964cf4127a7dc4dda))


### Changed

* **common:** cache pipe and cable collision shapes ([#631](https://github.com/Indemnity83/logistics/issues/631)) ([f31895a](https://github.com/Indemnity83/logistics/commit/f31895a009d315fd311f2f34124314d9aa3bd2a8))
* **core:** streamline world loading by dropping legacy save migrations ([#586](https://github.com/Indemnity83/logistics/issues/586)) ([6e1748c](https://github.com/Indemnity83/logistics/commit/6e1748c54b8b9ff653293f169079ac24d3ece74d))
* **crafting:** require a bronze gear in the machine frame ([#610](https://github.com/Indemnity83/logistics/issues/610)) ([1673320](https://github.com/Indemnity83/logistics/commit/1673320663b1ae7915e101d7439f77bcddca601a))
* **crafting:** yield one marker per craft ([#607](https://github.com/Indemnity83/logistics/issues/607)) ([3c168a7](https://github.com/Indemnity83/logistics/commit/3c168a792509c632caedbb749cfa1694061ec65c))
* **energy:** power extraction pipes only from a direct engine ([#641](https://github.com/Indemnity83/logistics/issues/641)) ([704c7e8](https://github.com/Indemnity83/logistics/commit/704c7e84bd9391eefd137ccaa2fa3994347e0e18))
* **energy:** power the logistics network through a junction instead of a battery ([e33e50a](https://github.com/Indemnity83/logistics/commit/e33e50a018a833049a4796f84ec76a5cae21ad8b))
* **fluids:** speed up fluid split allocation ([#623](https://github.com/Indemnity83/logistics/issues/623)) ([ca3b89d](https://github.com/Indemnity83/logistics/commit/ca3b89dc685a34b8a7a94f6a960eaf7089e9f7d0))
* **kiln:** restyle with the shared machine look ([7a97c75](https://github.com/Indemnity83/logistics/commit/7a97c7594651b13a61fe0ebcf565b6fb5393902b))
* **kiln:** retune smelting speed and RF cost ([7a97c75](https://github.com/Indemnity83/logistics/commit/7a97c7594651b13a61fe0ebcf565b6fb5393902b))
* **kiln:** standardize recipe around machine components ([7a97c75](https://github.com/Indemnity83/logistics/commit/7a97c7594651b13a61fe0ebcf565b6fb5393902b))
* **macerator:** ore→dust recipes now drop a chance byproduct dust ([86a9c71](https://github.com/Indemnity83/logistics/commit/86a9c7128cbfd27a160f777ce1dcca3c177231c8))
* **macerator:** restyle with the shared machine look ([8183eb9](https://github.com/Indemnity83/logistics/commit/8183eb955b534709a1cadb02dc9cff246839b353))
* **macerator:** standardize recipe around machine components ([8183eb9](https://github.com/Indemnity83/logistics/commit/8183eb955b534709a1cadb02dc9cff246839b353))
* **pipes:** raise pipe blast resistance to match glass ([#618](https://github.com/Indemnity83/logistics/issues/618)) ([8a5069b](https://github.com/Indemnity83/logistics/commit/8a5069b05dee22403290bc1e2bfe34d92083ea01))
* **pump:** standardize recipe around machine components ([71dd4df](https://github.com/Indemnity83/logistics/commit/71dd4dfbd6008f245cfb181e7642428e63c6859f))
* **quarry:** restyle with the shared machine look ([#582](https://github.com/Indemnity83/logistics/issues/582)) ([5a8b069](https://github.com/Indemnity83/logistics/commit/5a8b069d2be23df9bc57525d41969cad32a1ce2f))
* **quarry:** standardize recipe around machine components ([5a8b069](https://github.com/Indemnity83/logistics/commit/5a8b069d2be23df9bc57525d41969cad32a1ce2f))
* **routing:** cache next-hop routes per destination ([#632](https://github.com/Indemnity83/logistics/issues/632)) ([178d7a9](https://github.com/Indemnity83/logistics/commit/178d7a9783d449de53045b2074c164075bda8aec))
* **sawmill:** match the energy buffer to the other machines ([#647](https://github.com/Indemnity83/logistics/issues/647)) ([86244a0](https://github.com/Indemnity83/logistics/commit/86244a01235d755801070cc0b0827ccf9babca48))
* **sawmill:** move wood processing from macerator ([d26ff18](https://github.com/Indemnity83/logistics/commit/d26ff182324bad9771fd854ca2ed60df7f647bcd))
* **sawmill:** rename Wood Pulp to Sawdust ([d26ff18](https://github.com/Indemnity83/logistics/commit/d26ff182324bad9771fd854ca2ed60df7f647bcd))
* **ui:** refresh the kiln, macerator, and sawmill GUIs ([#646](https://github.com/Indemnity83/logistics/issues/646)) ([5ae4c45](https://github.com/Indemnity83/logistics/commit/5ae4c4556ad32d816a76f56f2f6a4146c7af8e29))
* **worldgen:** tin ore drops one raw tin ([#608](https://github.com/Indemnity83/logistics/issues/608)) ([59071ef](https://github.com/Indemnity83/logistics/commit/59071ef8a9004fe0c9351d707a2ee5c214dcb963))


### Removed

* **core:** drop the tin gear ([1673320](https://github.com/Indemnity83/logistics/commit/1673320663b1ae7915e101d7439f77bcddca601a))


### Fixed

* **automation:** pause recipes until byproducts have space ([#597](https://github.com/Indemnity83/logistics/issues/597)) ([5990bef](https://github.com/Indemnity83/logistics/commit/5990befead5b340bd3f88c56e3a59ac96a3c87c0))
* **core:** let markers connect through solid blocks ([#581](https://github.com/Indemnity83/logistics/issues/581)) ([b392890](https://github.com/Indemnity83/logistics/commit/b3928905feec0475bc1b98fc5abf425d454e7459))
* **core:** restore valve and quartz crystal recipes ([#600](https://github.com/Indemnity83/logistics/issues/600)) ([b267476](https://github.com/Indemnity83/logistics/commit/b267476e70cc20d5c82e7071f512d233eb3b07ff))
* **energy:** drop the creative sink when broken ([#616](https://github.com/Indemnity83/logistics/issues/616)) ([2d92961](https://github.com/Indemnity83/logistics/commit/2d92961d7668658ab2675d083e2b32f94a12582b))
* **fluids:** drop fluid pipes and glass tank when broken ([#614](https://github.com/Indemnity83/logistics/issues/614)) ([5c47c62](https://github.com/Indemnity83/logistics/commit/5c47c628d54b0f0add33e513a2643df0fb1eb1b8))
* **fluids:** show correct fill level on tank and pipe HUDs ([#619](https://github.com/Indemnity83/logistics/issues/619)) ([0d8f2c1](https://github.com/Indemnity83/logistics/commit/0d8f2c1280b9fbd6155f16b0c8b12bf7000b22d0))
* **kiln:** accept energy from the power network ([#602](https://github.com/Indemnity83/logistics/issues/602)) ([d7bc02a](https://github.com/Indemnity83/logistics/commit/d7bc02a98e586eff3aaf7668cd285fa3d2006f91))
* **kiln:** bank smelting XP and pay it out like a furnace ([#605](https://github.com/Indemnity83/logistics/issues/605)) ([8bfa074](https://github.com/Indemnity83/logistics/commit/8bfa074fa20ef5ed89ece0b3913adfdc96185313))
* **kiln:** mine with the correct pickaxe tier ([#601](https://github.com/Indemnity83/logistics/issues/601)) ([d59f801](https://github.com/Indemnity83/logistics/commit/d59f801c3d49cf181a57facf865da320ebfbf0c5))
* **macerator:** bank maceration XP and pay it out like a furnace ([8bfa074](https://github.com/Indemnity83/logistics/commit/8bfa074fa20ef5ed89ece0b3913adfdc96185313))
* **macerator:** grant XP for macerating ancient debris ([#624](https://github.com/Indemnity83/logistics/issues/624)) ([68a5ce5](https://github.com/Indemnity83/logistics/commit/68a5ce5cc36aae0dec1bd899dc61846507cdaaf9))
* **macerator:** restore the JEI integration ([#599](https://github.com/Indemnity83/logistics/issues/599)) ([3c02705](https://github.com/Indemnity83/logistics/commit/3c02705580530a9bda2cf03c56bde8c0e0304935))
* **pipes:** fix missing pixel in logistics power junction animation ([#637](https://github.com/Indemnity83/logistics/issues/637)) ([4bd6261](https://github.com/Indemnity83/logistics/commit/4bd62610fb6e74707c2929bc19880d24d64f39c1))
* **pump:** clamp the fluid pump search radius ([#625](https://github.com/Indemnity83/logistics/issues/625)) ([4a55de1](https://github.com/Indemnity83/logistics/commit/4a55de1562f987ddb3e74bcf8193492669cb5f45))
* **pump:** match the fluid pump's top to the other machines ([#561](https://github.com/Indemnity83/logistics/issues/561)) ([7557f5e](https://github.com/Indemnity83/logistics/commit/7557f5eafa7d096109022d0da6d561a5467454ce))
* **pump:** mine the fluid pump with the correct pickaxe tier ([5c47c62](https://github.com/Indemnity83/logistics/commit/5c47c628d54b0f0add33e513a2643df0fb1eb1b8))
* **pump:** stop destroying waterlogged blocks ([71dd4df](https://github.com/Indemnity83/logistics/commit/71dd4dfbd6008f245cfb181e7642428e63c6859f))
* **pump:** sync tube animation on phase changes ([71dd4df](https://github.com/Indemnity83/logistics/commit/71dd4dfbd6008f245cfb181e7642428e63c6859f))
* **quarry:** give the laser quarry frame a display name ([#617](https://github.com/Indemnity83/logistics/issues/617)) ([4922962](https://github.com/Indemnity83/logistics/commit/4922962ab7bf1e9c7782879f9dbd12f0c823ee49))
* **routing:** keep chassis modules when a pipe explodes ([#629](https://github.com/Indemnity83/logistics/issues/629)) ([d86d5a7](https://github.com/Indemnity83/logistics/commit/d86d5a78fb117458c7ab2713ddada88ef7e1e735))
* **routing:** refresh neighbor pipe arms when markings change ([#606](https://github.com/Indemnity83/logistics/issues/606)) ([eb260e7](https://github.com/Indemnity83/logistics/commit/eb260e7c9dce2c23901126cd355a2b405440a3d3))
* **sawmill:** add the missing crafting recipe ([#609](https://github.com/Indemnity83/logistics/issues/609)) ([1c4a125](https://github.com/Indemnity83/logistics/commit/1c4a1256f3572e1f215fc4163de41aefd0f8bd50))
* **sawmill:** mine with the correct pickaxe tier ([95ae978](https://github.com/Indemnity83/logistics/commit/95ae9786129fd4a7bd2073e964cf4127a7dc4dda))

## [0.7.4](https://github.com/Indemnity83/logistics/compare/mc26.2-v0.6.1...mc26.2-v0.7.4) (2026-06-21)


### ⚠ BREAKING CHANGES

* **energy:** logistics pipe operations consume power ([#465](https://github.com/Indemnity83/logistics/issues/465))
* **energy:** Logistics pipes now require power from an adjacent Battery. Existing networks stop routing/supplying/crafting — and drop items already in transit — until a charged Battery is connected.

### chore

* **build:** update to released MC 26.2 with official NeoForge beta ([#525](https://github.com/Indemnity83/logistics/issues/525)) ([d9b9daa](https://github.com/Indemnity83/logistics/commit/d9b9daae92de7c87dcf47360d80cf0dceebfdfd1))
* **core:** force version bump ([f83dafc](https://github.com/Indemnity83/logistics/commit/f83dafcf884941f84696496494db2cbdd2d40892))


### Added

* **api:** add loader-independent API for cross-mod fluid integration ([#516](https://github.com/Indemnity83/logistics/issues/516)) ([bc46019](https://github.com/Indemnity83/logistics/commit/bc46019c726b20817a2f5a281fa3929ce246ce6e))
* **automation:** add configurable laser quarry chunk loading ([#536](https://github.com/Indemnity83/logistics/issues/536)) ([2095caf](https://github.com/Indemnity83/logistics/commit/2095caf4bd769317cbd98de404b1a1e9245a904e))
* **automation:** show laser quarry status in the Jade HUD ([#498](https://github.com/Indemnity83/logistics/issues/498)) ([0beaace](https://github.com/Indemnity83/logistics/commit/0beaacee007bcacaf9965300e467aa05d59c6f6b))
* **automation:** show macerator and kiln progress in the Jade HUD ([#499](https://github.com/Indemnity83/logistics/issues/499)) ([9d9944e](https://github.com/Indemnity83/logistics/commit/9d9944e131ecf8b4f62170791ab70614c75200c8))
* **compat:** integrate Jade and remove the built-in probe ([3979277](https://github.com/Indemnity83/logistics/commit/397927718371980974c507b4c79a33d29ef0891b))
* **core:** grant ore XP when macerating metal and apatite ores ([#555](https://github.com/Indemnity83/logistics/issues/555)) ([0adb9a7](https://github.com/Indemnity83/logistics/commit/0adb9a7a1719877d3a6ee3ec415a6d0d8ba40c92))
* **energy:** add a Battery block to power logistics networks ([a711b09](https://github.com/Indemnity83/logistics/commit/a711b09270a6a459dac21e543a840bd5da89d289))
* **energy:** logistics pipe operations consume power ([#465](https://github.com/Indemnity83/logistics/issues/465)) ([a711b09](https://github.com/Indemnity83/logistics/commit/a711b09270a6a459dac21e543a840bd5da89d289))
* **energy:** show power diagnostics in the Jade HUD ([#497](https://github.com/Indemnity83/logistics/issues/497)) ([b720220](https://github.com/Indemnity83/logistics/commit/b720220883cfc17f890e9770f62daf772efdc0ba))
* **pipes:** add copper fluid pipe oxidation and fluid pipe marking ([#520](https://github.com/Indemnity83/logistics/issues/520)) ([dda9508](https://github.com/Indemnity83/logistics/commit/dda95084150f6f56c35c8803758be71ea39ac37c))
* **pipes:** add fluid pipes, tanks, and powered fluid extraction ([#511](https://github.com/Indemnity83/logistics/issues/511)) ([c04a76a](https://github.com/Indemnity83/logistics/commit/c04a76a0c30e45592299091a76ae89bd876e9e12))
* **pipes:** add fluid pump ([#537](https://github.com/Indemnity83/logistics/issues/537)) ([2dfdc3b](https://github.com/Indemnity83/logistics/commit/2dfdc3b152d96108f0110e68a823a1abc21f24f5))
* **pipes:** show pipe contents in the Jade HUD ([#500](https://github.com/Indemnity83/logistics/issues/500)) ([b53ee0c](https://github.com/Indemnity83/logistics/commit/b53ee0c47e3458e79f6fa091f5157c0947b687b0))
* **pipes:** show pipe module status in the Jade HUD ([#493](https://github.com/Indemnity83/logistics/issues/493)) ([86b7f72](https://github.com/Indemnity83/logistics/commit/86b7f729523280a318689be8174409d1d36faa03))
* **ui:** color logistics pipes green when powered, red when not ([#469](https://github.com/Indemnity83/logistics/issues/469)) ([d9f9d60](https://github.com/Indemnity83/logistics/commit/d9f9d60519c9b5d665ccc3f641749a57613ed261))


### Fixed

* **automation:** add missing macerator recipes for obsidian, netherite, and metal blocks ([#554](https://github.com/Indemnity83/logistics/issues/554)) ([f5b7304](https://github.com/Indemnity83/logistics/commit/f5b7304c5f8816b7055fbf5f84276729a4b11fd7))
* **automation:** stop the macerator from trying to load other mods' recipes ([#473](https://github.com/Indemnity83/logistics/issues/473)) ([f347495](https://github.com/Indemnity83/logistics/commit/f3474952fdbe8b879f218e9c6343e144c6799985))
* **compat:** hide fluid extractor energy in Jade ([#533](https://github.com/Indemnity83/logistics/issues/533)) ([441669e](https://github.com/Indemnity83/logistics/commit/441669e67ae130841e3320122633caba79d648d5))
* **energy:** add crafting recipe for battery ([#510](https://github.com/Indemnity83/logistics/issues/510)) ([a101ad3](https://github.com/Indemnity83/logistics/commit/a101ad355dc8d27caa858d3f106f512460e8cfc1))
* **energy:** fix crash when cables power machines from other mods ([#556](https://github.com/Indemnity83/logistics/issues/556)) ([56f1d44](https://github.com/Indemnity83/logistics/commit/56f1d449cfa92148b72ced98283f30528194a5fb))
* **energy:** make engines visibly change color with heat stage ([#482](https://github.com/Indemnity83/logistics/issues/482)) ([681de24](https://github.com/Indemnity83/logistics/commit/681de2436cd9e926ed7b861fceef66aec447a1ee))
* **neoforge:** correct energy bridge insert/extract accounting ([e10bdfe](https://github.com/Indemnity83/logistics/commit/e10bdfeab1966cd4b8e17a65a9643e18df87e7c6))
* **neoforge:** fix startup crash when JEI is on the classpath ([#453](https://github.com/Indemnity83/logistics/issues/453)) ([d45efef](https://github.com/Indemnity83/logistics/commit/d45efefa3640efd8154c5984492be0fadfe67d3c))
* **pipes:** apply config changes to modules installed in a chassis ([#504](https://github.com/Indemnity83/logistics/issues/504)) ([19b2a61](https://github.com/Indemnity83/logistics/commit/19b2a61594bf11fc835a026da6bcf5ce2cd1cf99)), closes [#494](https://github.com/Indemnity83/logistics/issues/494)
* **pipes:** fix fluid pipe drain flicker ([#532](https://github.com/Indemnity83/logistics/issues/532)) ([eec7f47](https://github.com/Indemnity83/logistics/commit/eec7f47df920ee23f4cf4516627afde3cda073ea))
* **ui:** fix glass tank capacity overlay with held items ([#534](https://github.com/Indemnity83/logistics/issues/534)) ([bbe20d4](https://github.com/Indemnity83/logistics/commit/bbe20d457c3748d37175e554cf9e4ffedc1e6b35))


### Improved

* **automation:** reduce memory and load time for machine rendering ([f858416](https://github.com/Indemnity83/logistics/commit/f858416733564b6442f10a0c26023725b869d43f))
* **automation:** reduce memory and load time for machine rendering ([#451](https://github.com/Indemnity83/logistics/issues/451)) ([f858416](https://github.com/Indemnity83/logistics/commit/f858416733564b6442f10a0c26023725b869d43f))
* **energy:** reduce memory and load time for cable rendering ([#449](https://github.com/Indemnity83/logistics/issues/449)) ([1adf690](https://github.com/Indemnity83/logistics/commit/1adf6905136ea456d5a3763da06f7bf1ce4f49d6))
* **pipes:** reduce memory and load time for pipe rendering ([c0bb14f](https://github.com/Indemnity83/logistics/commit/c0bb14fe0af13a0ab3fab4dae625fbe5c4a297af))
* **pipes:** reduce memory and load time for pipe rendering ([#450](https://github.com/Indemnity83/logistics/issues/450)) ([c0bb14f](https://github.com/Indemnity83/logistics/commit/c0bb14fe0af13a0ab3fab4dae625fbe5c4a297af))

## [0.6.1](https://github.com/Indemnity83/logistics/compare/mc26.1-v0.6.0...mc26.1-v0.6.1) (2026-06-02)


### Fixed

* correct laser quarry edge case regressions ([#427](https://github.com/Indemnity83/logistics/issues/427)) ([f72c93a](https://github.com/Indemnity83/logistics/commit/f72c93a012acae06ac94a1f94b82aa9b2a04f88f))
* enable custom Minecraft version range in build workflows ([#414](https://github.com/Indemnity83/logistics/issues/414)) ([30ad892](https://github.com/Indemnity83/logistics/commit/30ad892eb8a30594c7d791679c15b97ec7677e5d))
* update Minecraft version compatibility range for NeoForge ([#416](https://github.com/Indemnity83/logistics/issues/416)) ([b1c384d](https://github.com/Indemnity83/logistics/commit/b1c384d8dc16c6d124a594d40f362f4b9c4262f2))

## [0.6.0](https://github.com/Indemnity83/logistics/compare/mc26.1-v0.5.6...mc26.1-v0.6.0) (2026-05-31)

### Features

* Added NeoForge support, including platform services, capabilities, networking/lifecycle hooks, client rendering, and storage adapters. [#378](https://github.com/Indemnity83/logistics/issues/378), [#379](https://github.com/Indemnity83/logistics/issues/379), [#380](https://github.com/Indemnity83/logistics/issues/380), [#381](https://github.com/Indemnity83/logistics/issues/381), [cdeb6a7](https://github.com/Indemnity83/logistics/commit/cdeb6a7a53adf74379ba522c980560ad9f7b6966)
* Added power cables. [f2cc460](https://github.com/Indemnity83/logistics/commit/f2cc460e64714aebab4c9c9ebcebaec393f69403)

### Bug Fixes

* Improved logistics network reliability and safety by handling failed deliveries, validating/sanitizing config fields, and clamping energy values and transfer amounts to non-negative values. [#397](https://github.com/Indemnity83/logistics/issues/397), [#398](https://github.com/Indemnity83/logistics/issues/398), [#399](https://github.com/Indemnity83/logistics/issues/399)
* Fixed NeoForge and multi-loader content issues, including JEI Macerator recipe visibility, marking fluid recipe separation, and missing `META-INF` service files. [#382](https://github.com/Indemnity83/logistics/issues/382), [#390](https://github.com/Indemnity83/logistics/issues/390), [affc30f](https://github.com/Indemnity83/logistics/commit/affc30feb49ecec0597965ea41717caa81bc7f06)
* Fixed power cable compilation errors for Minecraft 26.1. [#357](https://github.com/Indemnity83/logistics/issues/357)
* Preserved item components in filter pipe slots across save/reload. [#386](https://github.com/Indemnity83/logistics/issues/386)

### Refactorings

* Reworked the project for multi-loader support, including NeoForge groundwork, loader-agnostic bootstrap flow, service-based platform access, cleaner module boundaries, and build configuration updates. [#306](https://github.com/Indemnity83/logistics/issues/306), [#318](https://github.com/Indemnity83/logistics/issues/318), [#320](https://github.com/Indemnity83/logistics/issues/320), [#341](https://github.com/Indemnity83/logistics/issues/341), [#342](https://github.com/Indemnity83/logistics/issues/342), [#343](https://github.com/Indemnity83/logistics/issues/343), [#344](https://github.com/Indemnity83/logistics/issues/344), [#347](https://github.com/Indemnity83/logistics/issues/347), [#348](https://github.com/Indemnity83/logistics/issues/348), [#360](https://github.com/Indemnity83/logistics/issues/360), [#361](https://github.com/Indemnity83/logistics/issues/361)
* Introduced loader-agnostic storage, energy, fluid, fuel, item matching, and client model abstractions. [#340](https://github.com/Indemnity83/logistics/issues/340), [#349](https://github.com/Indemnity83/logistics/issues/349), [#351](https://github.com/Indemnity83/logistics/issues/351), [#356](https://github.com/Indemnity83/logistics/issues/356), [#364](https://github.com/Indemnity83/logistics/issues/364), [#389](https://github.com/Indemnity83/logistics/issues/389), [#400](https://github.com/Indemnity83/logistics/issues/400)
* Cleaned up common code organization and removed remaining Fabric-specific dependencies/imports from shared sources. [#345](https://github.com/Indemnity83/logistics/issues/345), [#350](https://github.com/Indemnity83/logistics/issues/350), [#352](https://github.com/Indemnity83/logistics/issues/352), [#355](https://github.com/Indemnity83/logistics/issues/355)

### Testing

* Added Fabric/NeoForge test infrastructure, NeoForge ServiceLoader and energy adapter tests, component coverage, and baseline coverage reporting. [#377](https://github.com/Indemnity83/logistics/issues/377), [#402](https://github.com/Indemnity83/logistics/issues/402), [f6c6a35](https://github.com/Indemnity83/logistics/commit/f6c6a35c2404c74519b6130590c6b62c0428d14b)

## [0.5.6](https://github.com/Indemnity83/logistics/compare/mc26.1-v0.5.5...mc26.1-v0.5.6) (2026-05-02)


### Bug Fixes

* correct lever and dust placement on engine blocks ([#300](https://github.com/Indemnity83/logistics/issues/300)) ([6f912ca](https://github.com/Indemnity83/logistics/commit/6f912ca94b19b8db514504c9d8e119aff371bbe4))
* correct supplier module UI targeting ([#295](https://github.com/Indemnity83/logistics/issues/295)) ([128358a](https://github.com/Indemnity83/logistics/commit/128358af0cd2b00446926c12ab76e95f9d764edd))
* enchanted items being provided on the network ([#292](https://github.com/Indemnity83/logistics/issues/292)) ([1d24b3a](https://github.com/Indemnity83/logistics/commit/1d24b3ab7c0be50504085f468104a05fae85b8fa))
* prevent component-bearing items from sharing crafter slot ([#302](https://github.com/Indemnity83/logistics/issues/302)) ([ab6b03c](https://github.com/Indemnity83/logistics/commit/ab6b03c5dbb799dd6600b8911499ec6cbd935418))
* prevent passive supplier from overfilling inventory items ([#303](https://github.com/Indemnity83/logistics/issues/303)) ([33792f0](https://github.com/Indemnity83/logistics/commit/33792f07093086eebe98f0e3c3d0dd72418b8b53))
* relocate quartz crystal asset files to core directory ([#305](https://github.com/Indemnity83/logistics/issues/305)) ([e075108](https://github.com/Indemnity83/logistics/commit/e075108e8f124c74c82405825ffcb69cebd19219))
* tooltips for items in requester screen ([#294](https://github.com/Indemnity83/logistics/issues/294)) ([720b699](https://github.com/Indemnity83/logistics/commit/720b699b901685fa01d9586c327ba473a9a171fc))


### Refactorings

* move quartz crystal registration to core domain ([#304](https://github.com/Indemnity83/logistics/issues/304)) ([6de65f3](https://github.com/Indemnity83/logistics/commit/6de65f3dcaa791d47a4792e76cb365928e41e672))

## [0.5.5](https://github.com/Indemnity83/logistics/compare/mc26.1-v0.5.4...mc26.1-v0.5.5) (2026-04-25)


### Bug Fixes

* resolve vanishing filters in diamond pipes serialization ([#288](https://github.com/Indemnity83/logistics/issues/288)) ([dfe2440](https://github.com/Indemnity83/logistics/commit/dfe24400c6e7e0e25678a8246cf461b929588f4c))

## [0.5.4](https://github.com/Indemnity83/logistics/compare/mc26.1-v0.5.3...mc26.1-v0.5.4) (2026-04-22)


### Bug Fixes

* add ender dust macerator recipe ([#282](https://github.com/Indemnity83/logistics/issues/282)) ([0323f36](https://github.com/Indemnity83/logistics/commit/0323f3649230ec8787208a16a62ae163124e461b))
* use RegistryOps to fix enchanted item crash ([#283](https://github.com/Indemnity83/logistics/issues/283)) ([967be91](https://github.com/Indemnity83/logistics/commit/967be91480f14b35090fa8f3b651af2511704a37))

## [0.5.3](https://github.com/Indemnity83/logistics/compare/mc26.1-v0.5.2...mc26.1-v0.5.3) (2026-04-14)


### Bug Fixes

* add outline rendering for quarry area placement ([#274](https://github.com/Indemnity83/logistics/issues/274)) ([b06f9fe](https://github.com/Indemnity83/logistics/commit/b06f9fec1c4ed99ddf186a9543482f1bd213b539))
* expose laser quarry configuration settings to user ([#277](https://github.com/Indemnity83/logistics/issues/277)) ([d51a24d](https://github.com/Indemnity83/logistics/commit/d51a24d7f1b41b1a5c38d11b0fa91a0a05d2bc50))

## [0.5.2](https://github.com/Indemnity83/logistics/compare/mc26.1-v0.5.1...mc26.1-v0.5.2) (2026-04-10)


### Bug Fixes

* resolve pipe network registration issues on load ([#269](https://github.com/Indemnity83/logistics/issues/269)) ([35c1f00](https://github.com/Indemnity83/logistics/commit/35c1f00d4f0055c016cc7ba44c5d9f793e456dbb))

## [0.5.1](https://github.com/Indemnity83/logistics/compare/mc26.1-v0.5.0...mc26.1-v0.5.1) (2026-04-06)


### Bug Fixes

* add translations for tin and bronze item tags ([#257](https://github.com/Indemnity83/logistics/issues/257)) ([a7264fe](https://github.com/Indemnity83/logistics/commit/a7264fe84827b3d70f94f03637ff9496d8ae1476))
* normalize laser quarry recipe to use machine core ([#267](https://github.com/Indemnity83/logistics/issues/267)) ([c734a2e](https://github.com/Indemnity83/logistics/commit/c734a2e9bfd63590fa69e8398b2926a133632560))
* remove orphaned advanced extractor module assets and update JEI entrypoint ([#256](https://github.com/Indemnity83/logistics/issues/256)) ([cd0d626](https://github.com/Indemnity83/logistics/commit/cd0d626ea4df38b84693f6e54324010ec41ba0b0))
* rename MACHINE_FRAME to MACHINE_CORE and update assets ([#265](https://github.com/Indemnity83/logistics/issues/265)) ([120e024](https://github.com/Indemnity83/logistics/commit/120e0241772886eb2c8625ca042f51d4501bb7d3))
* update macerator tags for mining and loot table renaming ([#266](https://github.com/Indemnity83/logistics/issues/266)) ([95f7131](https://github.com/Indemnity83/logistics/commit/95f71314e403e8f0b940160f572a8993c2c93e48))

## [0.5.0](https://github.com/Indemnity83/logistics/compare/mc26.1-v0.4.0...mc26.1-v0.5.0) (2026-04-04)


### ⚠ BREAKING CHANGES

* logistics pipe and module crafting recipes have changed; enable the "Classic Logistics Pipes crafting recipes" built-in datapack to restore the original gear-based recipes as alternates

### Features

* add crafting recipes for new chip items to automation ([#240](https://github.com/Indemnity83/logistics/issues/240)) ([598aec6](https://github.com/Indemnity83/logistics/commit/598aec638416b31887e798cb10be091ad86f566a))
* add experience handling to macerator for new recipes ([#239](https://github.com/Indemnity83/logistics/issues/239)) ([3cf3dd2](https://github.com/Indemnity83/logistics/commit/3cf3dd234a9d5968f849cefcfd39d8008de6247e))
* add grinding time to macerator recipes ([#238](https://github.com/Indemnity83/logistics/issues/238)) ([67dfcf4](https://github.com/Indemnity83/logistics/commit/67dfcf4cbddb358335eb1899ccd537d51238274d))
* add JEI support for custom machines ([#234](https://github.com/Indemnity83/logistics/issues/234)) ([f20f57c](https://github.com/Indemnity83/logistics/commit/f20f57cebab68d9b3776fe454e04068e22e6f1db))
* add macerator recipes for copper, gold, and iron dusts ([#244](https://github.com/Indemnity83/logistics/issues/244)) ([0ebb885](https://github.com/Indemnity83/logistics/commit/0ebb88514c302e8bd8e6a382589043df29ae35ef))
* add macerator with full recipe set, new dust items, and flour ([#230](https://github.com/Indemnity83/logistics/issues/230)) ([6a2c27c](https://github.com/Indemnity83/logistics/commit/6a2c27c14a7da60e035eaeff10c8169373a6366f))
* add Minecraft 26.1 support updates ([b49938c](https://github.com/Indemnity83/logistics/commit/b49938c1415f3f3f8190c3c21a7f57a21272b24c))
* add wooden valve and new automation cores to system ([#235](https://github.com/Indemnity83/logistics/issues/235)) ([10c8808](https://github.com/Indemnity83/logistics/commit/10c880848b5845764c2c3c6bddd4449de6949bd0))
* implement recipe book for Kiln with access widener support ([#242](https://github.com/Indemnity83/logistics/issues/242)) ([e36899b](https://github.com/Indemnity83/logistics/commit/e36899b708a12f6d6ff0f31dc67de80a331d81dc))
* implement recipe book for macerator crafting interface ([#243](https://github.com/Indemnity83/logistics/issues/243)) ([0d0a142](https://github.com/Indemnity83/logistics/commit/0d0a1421e3fc80e9a3cd232f709986d9ccfe15a5))
* replace kiln valve recipes with crafting table recipes ([#233](https://github.com/Indemnity83/logistics/issues/233)) ([a52e897](https://github.com/Indemnity83/logistics/commit/a52e897bc292648dd2f472f9db919a799f273638))
* rework pipe and module recipes ([08bda95](https://github.com/Indemnity83/logistics/commit/08bda9578569df5e2d603fb0e18b01878b09b5b0))


### Bug Fixes

* add min_format and max_format to pack.mcmeta for recipes ([#250](https://github.com/Indemnity83/logistics/issues/250)) ([0f0ea05](https://github.com/Indemnity83/logistics/commit/0f0ea0587becc2e871e4c0c9896c65604e993d35))
* add supported_formats to pack.mcmeta for compatibility ([#252](https://github.com/Indemnity83/logistics/issues/252)) ([08521b0](https://github.com/Indemnity83/logistics/commit/08521b0dd1ecc3b2fca721960df35c69f50a01c3))
* correct engine block colors and transparency rendering ([#248](https://github.com/Indemnity83/logistics/issues/248)) ([9d1d08a](https://github.com/Indemnity83/logistics/commit/9d1d08adebf673495572d3f17d4921e9a81058fe))
* resolve pipe access bug for satellite and process pipes ([#231](https://github.com/Indemnity83/logistics/issues/231)) ([3603a8f](https://github.com/Indemnity83/logistics/commit/3603a8fbe831699f3dc7c8d6f7c83d3083bc5d45))
* update build script to check Gradle task by MC version ([#249](https://github.com/Indemnity83/logistics/issues/249)) ([fe5d9db](https://github.com/Indemnity83/logistics/commit/fe5d9dbdbcca1e62ec14433f5504aee3888bf2d5))


### Refactorings

* change kiln to act as an electric furnace ([#236](https://github.com/Indemnity83/logistics/issues/236)) ([f208e48](https://github.com/Indemnity83/logistics/commit/f208e484b55f758e30bdf5322aed0a8f7c037f89))
* relocate intermediates functionality to core package ([#241](https://github.com/Indemnity83/logistics/issues/241)) ([21138dc](https://github.com/Indemnity83/logistics/commit/21138dcc16c4729af14e7743c28ca090014caff0))

## [0.4.0](https://github.com/Indemnity83/logistics/compare/mc26.1-v0.3.5...mc26.1-v0.4.0) (2026-03-27)


### Features

* add logistics network with provider, supplier, requester, and sink modules; supports multiple supply modes (BULK50, BULK100, INFINITE, FULL), concurrent crafting orders, supply reconciliation, and in-transit item tracking ([#161](https://github.com/Indemnity83/logistics/issues/161), [#165](https://github.com/Indemnity83/logistics/issues/165), [#174](https://github.com/Indemnity83/logistics/issues/174), [#175](https://github.com/Indemnity83/logistics/issues/175), [#178](https://github.com/Indemnity83/logistics/issues/178), [#183](https://github.com/Indemnity83/logistics/issues/183), [#184](https://github.com/Indemnity83/logistics/issues/184), [#185](https://github.com/Indemnity83/logistics/issues/185), [#186](https://github.com/Indemnity83/logistics/issues/186), [#188](https://github.com/Indemnity83/logistics/issues/188), [#228](https://github.com/Indemnity83/logistics/issues/228), [3281871](https://github.com/Indemnity83/logistics/commit/3281871ee6afd33316b5a01b0a9be83f98fea6b8))
* add chassis pipes with modular, swappable module slots (MkI–MkV) ([#194](https://github.com/Indemnity83/logistics/issues/194), [#195](https://github.com/Indemnity83/logistics/issues/195), [#203](https://github.com/Indemnity83/logistics/issues/203))
* add crafting pipe with autocrafter integration, tiered ingredient buffers, capacity limits, and recipe import ([#189](https://github.com/Indemnity83/logistics/issues/189), [#193](https://github.com/Indemnity83/logistics/issues/193), [#200](https://github.com/Indemnity83/logistics/issues/200), [#204](https://github.com/Indemnity83/logistics/issues/204), [381c99d](https://github.com/Indemnity83/logistics/commit/381c99d888f125909ccbbeda18533f068b58836d))
* add processing, satellite, and mod sink pipe modules with GUIs ([#206](https://github.com/Indemnity83/logistics/issues/206), [#207](https://github.com/Indemnity83/logistics/issues/207))
* add beta build workflow for release branches and register kiln recipe type and serializer ([#154](https://github.com/Indemnity83/logistics/issues/154), [#155](https://github.com/Indemnity83/logistics/issues/155))


### Bug Fixes

* fix asset and presentation issues: missing particle textures, item tag names, and pipe loot tables ([#167](https://github.com/Indemnity83/logistics/issues/167), [#169](https://github.com/Indemnity83/logistics/issues/169), [#181](https://github.com/Indemnity83/logistics/issues/181))
* fix kiln screen issues: empty recipe display and missing menu registration ([#168](https://github.com/Indemnity83/logistics/issues/168), [#219](https://github.com/Indemnity83/logistics/issues/219))
* fix logistics network bugs: crafting order logic, dispatch edge cases, craftable item display, requester search sorting, provider screen GUI, extractor filter, and memory leak ([#173](https://github.com/Indemnity83/logistics/issues/173), [#176](https://github.com/Indemnity83/logistics/issues/176), [#179](https://github.com/Indemnity83/logistics/issues/179), [#180](https://github.com/Indemnity83/logistics/issues/180), [#192](https://github.com/Indemnity83/logistics/issues/192), [#198](https://github.com/Indemnity83/logistics/issues/198), [#201](https://github.com/Indemnity83/logistics/issues/201), [#205](https://github.com/Indemnity83/logistics/issues/205), [#208](https://github.com/Indemnity83/logistics/issues/208), [2f51bc8](https://github.com/Indemnity83/logistics/commit/2f51bc8a316dbd87d61c6f0eff675f78bba12707), [3281871](https://github.com/Indemnity83/logistics/commit/3281871ee6afd33316b5a01b0a9be83f98fea6b8))
* fix build and configuration issues: schemaVersion placement and beta workflow branch naming ([#157](https://github.com/Indemnity83/logistics/issues/157), [#166](https://github.com/Indemnity83/logistics/issues/166))
* fix crash caused by jar remapping collision in IPipeAccess ([#222](https://github.com/Indemnity83/logistics/issues/222))
* resolve quarry test failures by improving terrain handling ([#227](https://github.com/Indemnity83/logistics/issues/227))


### Performance

* throttle provider pipe extraction to 8 items per cycle at 6-tick intervals ([3281871](https://github.com/Indemnity83/logistics/commit/3281871ee6afd33316b5a01b0a9be83f98fea6b8))


### Refactorings

* redesign network architecture: replace request system with standing-order dispatch controller, restructure network package, and introduce dispatch planning with partial supply and explicit commands ([#177](https://github.com/Indemnity83/logistics/issues/177), [#182](https://github.com/Indemnity83/logistics/issues/182), [#187](https://github.com/Indemnity83/logistics/issues/187), [#190](https://github.com/Indemnity83/logistics/issues/190), [381c99d](https://github.com/Indemnity83/logistics/commit/381c99d888f125909ccbbeda18533f068b58836d))
* reorganize pipe module interfaces, sink management, filter slots, and components into core library; clarify supplier mode names and remove unnecessary buffer handling ([#199](https://github.com/Indemnity83/logistics/issues/199), [#202](https://github.com/Indemnity83/logistics/issues/202), [#213](https://github.com/Indemnity83/logistics/issues/213), [#214](https://github.com/Indemnity83/logistics/issues/214), [#215](https://github.com/Indemnity83/logistics/issues/215), [#216](https://github.com/Indemnity83/logistics/issues/216), [#218](https://github.com/Indemnity83/logistics/issues/218), [3281871](https://github.com/Indemnity83/logistics/commit/3281871ee6afd33316b5a01b0a9be83f98fea6b8))
* clean up imports, NBT abbreviations, and packet organization ([#209](https://github.com/Indemnity83/logistics/issues/209), [#210](https://github.com/Indemnity83/logistics/issues/210), [#211](https://github.com/Indemnity83/logistics/issues/211), [#212](https://github.com/Indemnity83/logistics/issues/212))


### Testing

* add unit, integration, and flow tests for logistics network and modules ([#191](https://github.com/Indemnity83/logistics/issues/191), [#221](https://github.com/Indemnity83/logistics/issues/221), [#223](https://github.com/Indemnity83/logistics/issues/223))

## [0.3.5-beta.0](https://github.com/Indemnity83/logistics/compare/mc26.1-v0.3.4-beta.0...mc26.1-v0.3.5-beta.0) (2026-02-27)


### Bug Fixes

* restore crafting and kiln recipe loading by updating recipe data paths ([#152](https://github.com/Indemnity83/logistics/issues/152)) ([3c4d2c5](https://github.com/Indemnity83/logistics/commit/3c4d2c5c6f19a7be65e35b09fc85e850e385bef1))
* standardize internal pipe item drops via PipeBlockEntity helper ([#149](https://github.com/Indemnity83/logistics/issues/149)) ([6742071](https://github.com/Indemnity83/logistics/commit/67420714560157cd91e25d3be25fe4b01c8e983b))
* update Gradle testLogging exceptionFormat syntax ([#147](https://github.com/Indemnity83/logistics/issues/147)) ([cee7c92](https://github.com/Indemnity83/logistics/commit/cee7c927a5bd18612b2f900aa5577bc08215ab28))


### Performance

* avoid per-tick pipe connection cache recalculation ([#143](https://github.com/Indemnity83/logistics/issues/143)) ([2a7f461](https://github.com/Indemnity83/logistics/commit/2a7f461e1d288a05b38b2a1bd75285386e643747))
* gate pipe connection updates behind cache dirty flag ([#150](https://github.com/Indemnity83/logistics/issues/150)) ([bd86812](https://github.com/Indemnity83/logistics/commit/bd868120698239522fdf3f36f7157820414c8bef))

## [0.3.4-beta.0](https://github.com/Indemnity83/logistics/compare/mc26.1-v0.3.3-beta.0...mc26.1-v0.3.4-beta.0) (2026-02-25)


### Features

* add kiln machine with molten glass, valve recipes, and shared heat component ([#131](https://github.com/Indemnity83/logistics/issues/131)) ([8c16640](https://github.com/Indemnity83/logistics/commit/8c166405b5953332da3d1437db232ea3bc80dab5))
* add ResourceId compatibility layer and migrate identifier usage ([#136](https://github.com/Indemnity83/logistics/issues/136)) ([9bca415](https://github.com/Indemnity83/logistics/commit/9bca415960ad42407059f86ceacc5eaaa8c859c7))


### Bug Fixes

* kiln recipe loading ([fa13752](https://github.com/Indemnity83/logistics/commit/fa1375226bab57ec9ae3fc59fb3e077f5779bb51))
* pin minecraft dependency to 26.1-alpha.6 ([fce5ad6](https://github.com/Indemnity83/logistics/commit/fce5ad6c0f09dfdfcc6c1c23472523a0a46580a4))
* properly set sided inventory for engines and kiln ([#138](https://github.com/Indemnity83/logistics/issues/138)) ([d73d983](https://github.com/Indemnity83/logistics/commit/d73d9830f9b36eb0b990e2a1698ad2c262d33be2))

## [0.3.3-beta.0](https://github.com/Indemnity83/logistics/compare/mc26.1-v0.3.2-beta.0...mc26.1-v0.3.3-beta.0) (2026-02-22)


### Bug Fixes

* correct vertical centering for block items rendered inside pipes ([#113](https://github.com/Indemnity83/logistics/issues/113)) ([ea89339](https://github.com/Indemnity83/logistics/commit/ea89339ee7b44351fcbbe4eef68c120a14f494ca))
* register mod content during common initialization ([#132](https://github.com/Indemnity83/logistics/issues/132)) ([c5eac34](https://github.com/Indemnity83/logistics/commit/c5eac34659cc16e79c3d733a10029e309e26edb5))

## [0.3.2-beta.0](https://github.com/Indemnity83/logistics/compare/mc26.1-v0.3.1-beta.0...mc26.1-v0.3.2-beta.0) (2026-02-17)


### Bug Fixes

* migrate tin/bronze recipes to c: tags and drop apatite tag entries ([#110](https://github.com/Indemnity83/logistics/issues/110)) ([c32d5e3](https://github.com/Indemnity83/logistics/commit/c32d5e3f9f456f187f22bfe843d73d89c933b985))
* normalize engine rendering with extra models, cutout layer, and heat tints ([#106](https://github.com/Indemnity83/logistics/issues/106)) ([f3c3a1a](https://github.com/Indemnity83/logistics/commit/f3c3a1a15e5d6f35bc09f045b549de49fd7cf988))
* restore non-overheating engine warm flash and scale piston speed by heat ([#111](https://github.com/Indemnity83/logistics/issues/111)) ([9e6478c](https://github.com/Indemnity83/logistics/commit/9e6478c1563d8dafaa740ee2474e62c928e1d04a))

## [0.3.1-beta.0](https://github.com/Indemnity83/logistics/compare/mc26.1-v0.3.0-beta.0...mc26.1-v0.3.1-beta.0) (2026-02-16)


### Features

* add tin, bronze, and apatite materials with worldgen and progression ([#100](https://github.com/Indemnity83/logistics/issues/100)) ([879aa61](https://github.com/Indemnity83/logistics/commit/879aa61690542435a6f51d3189813fa277b9936e))
* refresh gear textures and update creative tab ([#91](https://github.com/Indemnity83/logistics/issues/91)) ([d00bbd2](https://github.com/Indemnity83/logistics/commit/d00bbd2feee1631815c9f35a00a38900cc000685))


### Bug Fixes

* correct engine blockstates, particles, and drops ([#86](https://github.com/Indemnity83/logistics/issues/86)) ([088b00f](https://github.com/Indemnity83/logistics/commit/088b00fde890339bb62cf5e49e08e8199f7cc2de))
* correct laser quarry LED working state and screen rotation ([#99](https://github.com/Indemnity83/logistics/issues/99)) ([31a280c](https://github.com/Indemnity83/logistics/commit/31a280c4959d5b96c82c2f902c0c6af34b7d010c))

## [0.3.0](https://github.com/Indemnity83/logistics/compare/v0.2.5...v0.3.0) (2026-02-02)


### ⚠ BREAKING CHANGES

* The basic (non-powered) quarry has been removed and will be automatically deleted from existing worlds on upgrade. It may return in a redesigned form in a future release.
* The Logistics API has been reorganized and namespaced; internal and external integrations using the old API structure will need to update imports and references.


### Features

* add energy-powered laser quarry ([#69](https://github.com/Indemnity83/logistics/issues/69)) ([3500ad6](https://github.com/Indemnity83/logistics/commit/3500ad6cf70faf9f3a2d0b430b83f81cb67b80d2))
* add power system with engines, creative sink, and energy API ([#60](https://github.com/Indemnity83/logistics/issues/60)) ([4c6333d](https://github.com/Indemnity83/logistics/commit/4c6333d7097fd6f563f01bf2b6e1713d6ff8829d))
* add creative mode probe tool for getting runtime information from some blocks ([#61](https://github.com/Indemnity83/logistics/issues/61)) ([0dbb9a8](https://github.com/Indemnity83/logistics/commit/0dbb9a870de82b31398aa87b35236bf89283785e))
* extraction pipes now use energy from engines to power item movement ([#74](https://github.com/Indemnity83/logistics/issues/74)) ([ee02bd9](https://github.com/Indemnity83/logistics/commit/ee02bd9edd1ceae0f435a03de23126ad93b44188))


### Bug Fixes

* allow wrench to reset Stirling overheat and prevent non-overheating engines from overheating ([#62](https://github.com/Indemnity83/logistics/issues/62)) ([5172210](https://github.com/Indemnity83/logistics/commit/517221094242cf42c44250f2255bf3510beb8e05))
* correct engine renderer rotation for north/south facing ([#71](https://github.com/Indemnity83/logistics/issues/71)) ([46f8c2c](https://github.com/Indemnity83/logistics/commit/46f8c2c199bd3e21de07c14e95e4f56cb8a27ed3))
* prevent redstone engine from powering blocks that don’t accept low-tier energy ([#73](https://github.com/Indemnity83/logistics/issues/73)) ([3144215](https://github.com/Indemnity83/logistics/commit/31442159b8971271f4c410c076eb7549af60e45c))
* update Stirling Engine UI texture and title translation ([#63](https://github.com/Indemnity83/logistics/issues/63)) ([973dbf8](https://github.com/Indemnity83/logistics/commit/973dbf868879d4e0e91eccecd20447a3a936b038))

## [0.2.5](https://github.com/Indemnity83/logistics/compare/v0.2.4...v0.2.5) (2026-01-27)


### Bug Fixes

* preserve pipe block entity state on normal pick-block ([#45](https://github.com/Indemnity83/logistics/issues/45)) ([a72956f](https://github.com/Indemnity83/logistics/commit/a72956f43589757b406a830d2f8c4667022993e9))
* quarry now drops item when broken ([#47](https://github.com/Indemnity83/logistics/issues/47)) ([84d1fbe](https://github.com/Indemnity83/logistics/commit/84d1fbe3c9a593aa83701019db6e95b0f48f560d))

## [0.2.4](https://github.com/Indemnity83/logistics/compare/v0.2.3...v0.2.4) (2026-01-27)


### Features

* add quarry to mine blocks using tools ([#42](https://github.com/Indemnity83/logistics/issues/42)) ([1d02736](https://github.com/Indemnity83/logistics/commit/1d02736c725bfb5276eaf2ce498809ae42a7cc79))


### Bug Fixes

* split item stacks when inventories have partial capacity ([#44](https://github.com/Indemnity83/logistics/issues/44)) ([a53cea0](https://github.com/Indemnity83/logistics/commit/a53cea04bc7f55cf7e3efc48ed2598f8eb65d1df))

## [0.2.3](https://github.com/Indemnity83/logistics/compare/v0.2.2...v0.2.3) (2026-01-21)


### Features

* add copper pipe weathering with oxidation, waxing, and variant items ([#36](https://github.com/Indemnity83/logistics/issues/36)) ([ce29df5](https://github.com/Indemnity83/logistics/commit/ce29df5e5a2c17d6cc9d5d8a7716e7b43f800cdd))
* add marking fluid to color copper pipes and segment networks ([#29](https://github.com/Indemnity83/logistics/issues/29)) ([c012762](https://github.com/Indemnity83/logistics/commit/c0127625c55a4141314e0e07dc6c429819e79a20))


### Bug Fixes

* allow extraction pipes to accept items using default pipe rules ([#38](https://github.com/Indemnity83/logistics/issues/38)) ([9e87c8f](https://github.com/Indemnity83/logistics/commit/9e87c8f3a70089911407d2146445a3220ba8a961))

## [0.2.2](https://github.com/Indemnity83/logistics/compare/v0.2.1...v0.2.2) (2026-01-15)


### Bug Fixes

* pipe interaction and presentation polish ([#27](https://github.com/Indemnity83/logistics/issues/27)) ([3c1c5d4](https://github.com/Indemnity83/logistics/commit/3c1c5d4380c34fb8e8af538d24159c31ceba8bc6))
  * pipes break instantly by hand in survival
  * pipes drop their item when broken in survival
  * pipe place and break sounds are now consistent
  * pipes no longer render as colored blocks on maps
  * fix stale pipe arm rendering when neighbors change
* tune pipe item speeds ([#25](https://github.com/Indemnity83/logistics/issues/25)) ([96c1fb6](https://github.com/Indemnity83/logistics/commit/96c1fb699a85a78ce092b0ce0442eb9af3169d0f))

## [0.2.1](https://github.com/Indemnity83/logistics/compare/v0.2.0...v0.2.1) (2026-01-14)


### Bug Fixes

* correct item passthrough pipe recipe ([#18](https://github.com/Indemnity83/logistics/issues/18)) ([f65f489](https://github.com/Indemnity83/logistics/commit/f65f489fafed4ca6c575c4feb8ab3e2360355d58))

## [0.2.0](https://github.com/Indemnity83/logistics/compare/v0.1.0...v0.2.0) (2026-01-14)


### ⚠ BREAKING CHANGES

* Quartz pipes no longer output comparator signals. Existing item sensor pipes will automatically resolve to copper transport pipes when loading older worlds.

### Features

* add item passthrough pipe for inventory bypass ([#17](https://github.com/Indemnity83/logistics/issues/17)) ([487cabb](https://github.com/Indemnity83/logistics/commit/487cabba6aa8d99c70461f280c1a433dfc7e4426))
* add quartz pipe inventory overflow behavior ([#16](https://github.com/Indemnity83/logistics/issues/16)) ([5e9b51b](https://github.com/Indemnity83/logistics/commit/5e9b51badd130c6ba01e2821dfee87242f2fce9c))


### Bug Fixes

* remove invalid property references from pipe blockstates ([#14](https://github.com/Indemnity83/logistics/issues/14)) ([a621f05](https://github.com/Indemnity83/logistics/commit/a621f0560f8b2628fa9faaf262033773552552cb))

## 0.1.0 (2026-01-14)


### Features

- add core pipe system with in-pipe item transport and routing ([087a53a](https://github.com/Indemnity83/logistics/commit/087a53ad6ee2f9690a481093fc7cd08ab8502bf1))
- add copper transport pipe with randomized junction routing ([b49810c](https://github.com/Indemnity83/logistics/commit/b49810c147a86142c42a1231156000a38ce36cdd))
- add item merger pipe with wrench-configurable output routing ([709546d](https://github.com/Indemnity83/logistics/commit/709546d66e904d5b4f07f31c04d46203d54d5710))
- add item void pipe for deleting items in transit ([1bdee7b](https://github.com/Indemnity83/logistics/commit/1bdee7b9a26102aa01158d2e3417ac217b38c55c))
- add item sensor pipe with comparator output based on item count ([8042f22](https://github.com/Indemnity83/logistics/commit/8042f22d43194f5f365dc5625ce85b73c7c89a51))
- add item filter pipe with per-side filtering UI ([9cc943d](https://github.com/Indemnity83/logistics/commit/9cc943d65a032d25080b5ee2b8954b003e5ec9bc))
- allow item merger pipe output to any connection ([270bc3d](https://github.com/Indemnity83/logistics/commit/270bc3d35c50844282859b7351490a1c3a7a86d6))
- align pipe naming and material progression (stone/copper/gold transport; item extractor/merger/filter/sensor/void) ([#9](https://github.com/Indemnity83/logistics/issues/9)) ([8eb95d9](https://github.com/Indemnity83/logistics/commit/8eb95d9f56a7ecd908ad277f1ef0a37bbd4081a0))

### Performance

- dynamic pipe rendering via Block Entity Renderer (fixes [#6](https://github.com/Indemnity83/logistics/issues/6)) ([#11](https://github.com/Indemnity83/logistics/issues/11)) ([5383ec3](https://github.com/Indemnity83/logistics/commit/5383ec36604c47f9f8ce33debe7a39e1da17b87b))
- reduce pipe blockstate count ([acb9993](https://github.com/Indemnity83/logistics/commit/acb9993650d579a51a8c9be4f42585271241b1ad))
- gate timing logs behind a system property ([aab01de](https://github.com/Indemnity83/logistics/commit/aab01de7c2a578f96dfaf948a5f8646f8534329a))

## Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html),
and uses [Conventional Commits](https://www.conventionalcommits.org/) for automated changelog generation.
