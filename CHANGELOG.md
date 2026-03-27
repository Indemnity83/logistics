# Changelog

## [0.4.0](https://github.com/Indemnity83/logistics/compare/mc1.21.1-v0.3.5...mc1.21.1-v0.4.0) (2026-03-27)


### chore

* bump to v0.4.0 ([8e35f37](https://github.com/Indemnity83/logistics/commit/8e35f37c0e58b4e57b1179d7c97094d2a11caf16))


### Features

* add beta build workflow for release branches ([#155](https://github.com/Indemnity83/logistics/issues/155)) ([53dd697](https://github.com/Indemnity83/logistics/commit/53dd697ef4a41b23204b7a317e233f80ed0b284c))
* add BULK50, BULK100, INFINITE, and FULL supply modes to supplier pipe ([a8e0a4f](https://github.com/Indemnity83/logistics/commit/a8e0a4f9033ff20494d1c01a1700d20a816713f5))
* add crafting logistics pipe with autocrafter integration ([d3770d7](https://github.com/Indemnity83/logistics/commit/d3770d74ea0911dbd0df3e391fd794429a6f0d9a))
* add dynamic chassis module system for logistics pipes ([#195](https://github.com/Indemnity83/logistics/issues/195)) ([22349ef](https://github.com/Indemnity83/logistics/commit/22349ef3bafd483e40b32d3d064de03f893b9c5f))
* add game tests for logistics pipe network functionality ([83f22ea](https://github.com/Indemnity83/logistics/commit/83f22eab61ce5cb65fc4b39fb6797f5fde27acb7))
* add import button for autocrafter recipes in crafting screen ([#204](https://github.com/Indemnity83/logistics/issues/204)) ([659d310](https://github.com/Indemnity83/logistics/commit/659d310deb540de358edb4f5081a4d0721167ca6))
* add modular logistics pipe network with provider/supplier/requester/sink modules ([#161](https://github.com/Indemnity83/logistics/issues/161)) ([01d3200](https://github.com/Indemnity83/logistics/commit/01d320007deb1ebb0275c7bc569cbf70693c6e40))
* add processing and satellite pipe modules with GUIs ([#206](https://github.com/Indemnity83/logistics/issues/206)) ([a6bbc58](https://github.com/Indemnity83/logistics/commit/a6bbc58ae7f51d0d52c57f43bd18eb6ac46e7485))
* add runtime-toggleable network debug logging and /logistics debug command ([#174](https://github.com/Indemnity83/logistics/issues/174)) ([4c99f02](https://github.com/Indemnity83/logistics/commit/4c99f02ca8fc307afc66abc38eb9f780226f73dc))
* add supplier mode configuration and fulfillment normalization ([#183](https://github.com/Indemnity83/logistics/issues/183)) ([18c76b2](https://github.com/Indemnity83/logistics/commit/18c76b2e38e5deced7c43f1a93cc3eb9188a7359))
* add support for chassis logistics pipes with modular slots ([#194](https://github.com/Indemnity83/logistics/issues/194)) ([e08baca](https://github.com/Indemnity83/logistics/commit/e08bacaf8c0fe71c6f906d096fc58205a6b40221))
* clear module state when modules are removed from pipes ([#203](https://github.com/Indemnity83/logistics/issues/203)) ([5504010](https://github.com/Indemnity83/logistics/commit/550401030a9356dd1b485997a2a45f12ac7b9b61))
* enable processing of multiple concurrent orders in pipeline ([#228](https://github.com/Indemnity83/logistics/issues/228)) ([1233d86](https://github.com/Indemnity83/logistics/commit/1233d86849095feb45581d503c561856af560743))
* implement capacity limits for crafting module batches ([#189](https://github.com/Indemnity83/logistics/issues/189)) ([d7b9be7](https://github.com/Indemnity83/logistics/commit/d7b9be7511bac560b5a10c51e7fa08c97f7019ed))
* implement fulfillment checks for dynamic providers in logistics network ([#178](https://github.com/Indemnity83/logistics/issues/178)) ([8102935](https://github.com/Indemnity83/logistics/commit/81029351aa98e98d8bbe3912403067d1aa4ba086))
* implement Mod Sink module with GUI for mod filtering ([#207](https://github.com/Indemnity83/logistics/issues/207)) ([e6b7f68](https://github.com/Indemnity83/logistics/commit/e6b7f6846f43bbdd279fa03a709d70740a22b10e))
* implement supply reconciliation for order fulfillment failures ([#188](https://github.com/Indemnity83/logistics/issues/188)) ([b20b092](https://github.com/Indemnity83/logistics/commit/b20b0927587b7d955d5113c201dce325abf5536a))
* implement tiered crafting modules with ingredient buffers ([#200](https://github.com/Indemnity83/logistics/issues/200)) ([3fbac67](https://github.com/Indemnity83/logistics/commit/3fbac67675de007403f4b0561f9d47998c63ad57))
* introduce network job orchestration for item delivery ([#186](https://github.com/Indemnity83/logistics/issues/186)) ([192077b](https://github.com/Indemnity83/logistics/commit/192077b6100991d49cfc67a9ac9532e00fac8bfd))
* introduce pure Java value objects for network states ([#184](https://github.com/Indemnity83/logistics/issues/184)) ([b981dd2](https://github.com/Indemnity83/logistics/commit/b981dd2a4f54811cb94c78119758689ac9812af8))
* introduce ReservationManager for item reservation tracking ([#185](https://github.com/Indemnity83/logistics/issues/185)) ([b5711de](https://github.com/Indemnity83/logistics/commit/b5711de64281d8ccfe39cc03fbce40622cf0cddc))
* optimize auto crafter pulse duration and cooldown settings ([#193](https://github.com/Indemnity83/logistics/issues/193)) ([7e924c7](https://github.com/Indemnity83/logistics/commit/7e924c7e474db109a15f754e632e951fed1e41e7))
* support multiple concurrent crafting orders ([a85fac3](https://github.com/Indemnity83/logistics/commit/a85fac3c6eb8bce0f3284427f8821dc097247677))
* track in-transit orders from provider extraction through pipe delivery ([#165](https://github.com/Indemnity83/logistics/issues/165)) ([a8e0a4f](https://github.com/Indemnity83/logistics/commit/a8e0a4f9033ff20494d1c01a1700d20a816713f5))


### Bug Fixes

* add loot tables for pipes to drop on block break ([#181](https://github.com/Indemnity83/logistics/issues/181)) ([1fdb02a](https://github.com/Indemnity83/logistics/commit/1fdb02a0f8dc62c961270a9e816af6a22d7b65d8))
* address edge case handling in dispatch and availability logic ([#192](https://github.com/Indemnity83/logistics/issues/192)) ([0d971de](https://github.com/Indemnity83/logistics/commit/0d971dea0983d42e5b77c8e7494a60931dda160f))
* cancel entry orders when removing completed crafting queue entry ([f74ed64](https://github.com/Indemnity83/logistics/commit/f74ed64964d784ce63bea5878e514d8632c8b962))
* cap crafted amounts to requested quantity in crafting module ([#201](https://github.com/Indemnity83/logistics/issues/201)) ([f75802b](https://github.com/Indemnity83/logistics/commit/f75802ba09166113c2f9415112860901ea51324f))
* correct inverted fuel validation in StirlingEngine item storage ([dd1d2b2](https://github.com/Indemnity83/logistics/commit/dd1d2b2f560e1f3a727f1c85a4e038a1fc8dd52f))
* crash caused by jar remapping collision in IPipeAccess ([#222](https://github.com/Indemnity83/logistics/issues/222)) ([60d2845](https://github.com/Indemnity83/logistics/commit/60d28457a0b845ed4c3f86b9835e1f6a2a32ff23))
* handle potential null value from existing queue retrieval ([3f6dbfe](https://github.com/Indemnity83/logistics/commit/3f6dbfecb2f840db7d7fa1134bc1b29febc7a980))
* improve permission checking logic in LogisticsCommands and ProviderModule ([6b30e49](https://github.com/Indemnity83/logistics/commit/6b30e49ee65749a65c293a6ca023227c00b8cf08))
* notify delivery tracking before dropping undeliverable pipe items ([#173](https://github.com/Indemnity83/logistics/issues/173)) ([4b5c7fd](https://github.com/Indemnity83/logistics/commit/4b5c7fdc61b14ec514b3a36cd0e87ff0ac8b8a8d))
* purge terminal to resolve memory leak in job coordination ([#208](https://github.com/Indemnity83/logistics/issues/208)) ([25a563a](https://github.com/Indemnity83/logistics/commit/25a563a46f08936d7ef5d53367de7f31e3fd5bdc))
* quarry mining game test by clarifying frame placement and bounds ([6346e5f](https://github.com/Indemnity83/logistics/commit/6346e5ffb3bd14e8907ce2e4738858ae831a7d67))
* register menu type for kiln screen handler ([#219](https://github.com/Indemnity83/logistics/issues/219)) ([204fea3](https://github.com/Indemnity83/logistics/commit/204fea3e8a320af506dddbef5a72008c5c59a955))
* remove unused imports from IWorldView and PipeContext classes ([#210](https://github.com/Indemnity83/logistics/issues/210)) ([888d822](https://github.com/Indemnity83/logistics/commit/888d82248b30a3c78f7fcac6c23c814d0a22720a))
* replace inventory-delta pending tracking with network order accounting ([a8e0a4f](https://github.com/Indemnity83/logistics/commit/a8e0a4f9033ff20494d1c01a1700d20a816713f5))
* resolve crafting logic triggering for supplier pipes ([#180](https://github.com/Indemnity83/logistics/issues/180)) ([ab3b681](https://github.com/Indemnity83/logistics/commit/ab3b6812a7c653c66a45c8cc8f75935da6402d90))
* resolve crafting order placement logic for missing items ([#205](https://github.com/Indemnity83/logistics/issues/205)) ([14ffae1](https://github.com/Indemnity83/logistics/commit/14ffae1e2731ee45ebbcf84243a7c03974c84451))
* resolve GUI mode mismatch in ProviderScreenHandler class ([#179](https://github.com/Indemnity83/logistics/issues/179)) ([aa7d99a](https://github.com/Indemnity83/logistics/commit/aa7d99a2d30710db3dda94a4dca5421bfdea1876))
* show craftable items as 0 stock and stabilize requester search sorting ([#176](https://github.com/Indemnity83/logistics/issues/176)) ([8bb7d26](https://github.com/Indemnity83/logistics/commit/8bb7d262ec84d73f50f36952a64c3c8e7b57b9ae))
* update beta build workflow for new release-please branch naming ([#157](https://github.com/Indemnity83/logistics/issues/157)) ([ed126eb](https://github.com/Indemnity83/logistics/commit/ed126eb7338b9b53294392bd19c7d2374954a66d))
* update filter item retrieval logic in extractor modules ([#198](https://github.com/Indemnity83/logistics/issues/198)) ([77f58c4](https://github.com/Indemnity83/logistics/commit/77f58c4543eff31633b32623b0bff6a52347de1b))
* use fabric-gametest-api-v1:empty template for MC 1.21.1 game tests ([fcfd412](https://github.com/Indemnity83/logistics/commit/fcfd412d4417f6d07109ba4a159e37fd6b1ebec3))
* wrap pipe recipe key entries in item objects ([7e81845](https://github.com/Indemnity83/logistics/commit/7e818451a94a617aacb9aab43051e580648f884a))


### Performance

* throttle provider pipe extraction to 8 items per cycle at 6-tick intervals ([a8e0a4f](https://github.com/Indemnity83/logistics/commit/a8e0a4f9033ff20494d1c01a1700d20a816713f5))


### Refactorings

* clarify and standardize supplier mode names and logic ([#199](https://github.com/Indemnity83/logistics/issues/199)) ([85d1858](https://github.com/Indemnity83/logistics/commit/85d1858daf4132825a4ba8ca7d3d96e0714753e7))
* consolidate filter item management with FilterSlots class ([#215](https://github.com/Indemnity83/logistics/issues/215)) ([0d474f9](https://github.com/Indemnity83/logistics/commit/0d474f99d7543f96e9e85ac58ad99ffadcbf562a))
* dispatch partial inventory supply before falling back to crafting ([#177](https://github.com/Indemnity83/logistics/issues/177)) ([e17b74e](https://github.com/Indemnity83/logistics/commit/e17b74e756e443ed51134467f5957e6578032a4e))
* extract dispatch planning logic into RequestPlanner ([#187](https://github.com/Indemnity83/logistics/issues/187)) ([a8d19dc](https://github.com/Indemnity83/logistics/commit/a8d19dc25f65ef3b401c069dd9c048de2b305103))
* implement focused role interfaces for pipe modules ([#213](https://github.com/Indemnity83/logistics/issues/213)) ([c52b196](https://github.com/Indemnity83/logistics/commit/c52b196cfeca036b2965255c399d6d7cb8e2a34b))
* introduce explicit dispatch commands for network actions ([#190](https://github.com/Indemnity83/logistics/issues/190)) ([8b7e889](https://github.com/Indemnity83/logistics/commit/8b7e889b3bb9cf5192f0fac9387e838262bb5143))
* move ItemRequest and LogisticsOrder to core.lib.network ([a8e0a4f](https://github.com/Indemnity83/logistics/commit/a8e0a4f9033ff20494d1c01a1700d20a816713f5))
* move pipe components into core library package ([#218](https://github.com/Indemnity83/logistics/issues/218)) ([3913d13](https://github.com/Indemnity83/logistics/commit/3913d13b0efe9a1549e088f24f4f672d0aea3fbe))
* move sink management logic to SinkResolver class ([#214](https://github.com/Indemnity83/logistics/issues/214)) ([46f05a6](https://github.com/Indemnity83/logistics/commit/46f05a64c24417c8518d735c47851cae5e0ff142))
* optimize ArrayList imports in PipeContext class ([#209](https://github.com/Indemnity83/logistics/issues/209)) ([03276c7](https://github.com/Indemnity83/logistics/commit/03276c7913a3362fa8535a4ff0c0b5a2b72b5ec6))
* remove default route sink registration and methods ([#216](https://github.com/Indemnity83/logistics/issues/216)) ([5ef9ec5](https://github.com/Indemnity83/logistics/commit/5ef9ec5665a263567f59d8f252781c9811fe68e0))
* remove unnecessary buffer handling from crafting module ([#202](https://github.com/Indemnity83/logistics/issues/202)) ([730dc90](https://github.com/Indemnity83/logistics/commit/730dc90a142f29338760a478cd1ad0201c94ccc3))
* reorganize network packet classes and update imports ([#212](https://github.com/Indemnity83/logistics/issues/212)) ([cb8d8dc](https://github.com/Indemnity83/logistics/commit/cb8d8dc1b3768813855b8e917affd86df82f44c1))
* replace NBT abbreviations in ProviderModule class ([#211](https://github.com/Indemnity83/logistics/issues/211)) ([f5ea0e9](https://github.com/Indemnity83/logistics/commit/f5ea0e9d51ee88c95278c0f741c6c2948953024d))
* replace network request system with standing-order dispatch controller ([d3770d7](https://github.com/Indemnity83/logistics/commit/d3770d74ea0911dbd0df3e391fd794429a6f0d9a))
* restructure network package and update related imports ([#182](https://github.com/Indemnity83/logistics/issues/182)) ([5a0e1ab](https://github.com/Indemnity83/logistics/commit/5a0e1ab2cca343a7c57897d7805cfd9d31f4fa88))


### Testing

* add unit tests for core logistics components and services ([#191](https://github.com/Indemnity83/logistics/issues/191)) ([cb0c4b8](https://github.com/Indemnity83/logistics/commit/cb0c4b8d6bfd22d9f8c795bc568ffa3fbd475d71))
* enhance test suite with new module and game tests ([#223](https://github.com/Indemnity83/logistics/issues/223)) ([b6d4883](https://github.com/Indemnity83/logistics/commit/b6d488397876546a764dd0dea8e2797cbaa6aa86))

## [0.3.5](https://github.com/Indemnity83/logistics/compare/mc1.21.1-v0.3.4...mc1.21.1-v0.3.5) (2026-02-27)


### Features

* register kiln recipe type and serializer for JSON parsing ([#154](https://github.com/Indemnity83/logistics/issues/154)) ([6261829](https://github.com/Indemnity83/logistics/commit/6261829ce0a7d99e094b2759ab7db90b25021a14))


### Bug Fixes

* restore crafting and kiln recipe loading by updating recipe data paths ([#152](https://github.com/Indemnity83/logistics/issues/152)) ([455374c](https://github.com/Indemnity83/logistics/commit/455374c521a16a58d6bbc1de1b75216e06ba2e88))
* standardize internal pipe item drops via PipeBlockEntity helper ([#149](https://github.com/Indemnity83/logistics/issues/149)) ([7fa55db](https://github.com/Indemnity83/logistics/commit/7fa55dbed90af089936ec05609d72708ba2f630b))
* update Gradle testLogging exceptionFormat syntax ([#147](https://github.com/Indemnity83/logistics/issues/147)) ([ac53503](https://github.com/Indemnity83/logistics/commit/ac53503ead76099964d34081778a0cd5374bee9d))


### Performance

* avoid per-tick pipe connection cache recalculation ([#143](https://github.com/Indemnity83/logistics/issues/143)) ([c1761a7](https://github.com/Indemnity83/logistics/commit/c1761a7c0592017dbcc8ef194851e476f92f122e))
* gate pipe connection updates behind cache dirty flag ([#150](https://github.com/Indemnity83/logistics/issues/150)) ([ffddf08](https://github.com/Indemnity83/logistics/commit/ffddf08db5702b6a4e2ff5721c9f0eeb58afb03f))

## [0.3.4](https://github.com/Indemnity83/logistics/compare/mc1.21.1-v0.3.3...mc1.21.1-v0.3.4) (2026-02-25)


### Features

* add kiln machine with molten glass, valve recipes, and shared heat component ([#131](https://github.com/Indemnity83/logistics/issues/131)) ([e9ac328](https://github.com/Indemnity83/logistics/commit/e9ac3287dde2e29f7c837aac7a976090cc377f4c))
* add ResourceId compatibility layer and migrate identifier usage ([#136](https://github.com/Indemnity83/logistics/issues/136)) ([e1a8dbf](https://github.com/Indemnity83/logistics/commit/e1a8dbf6be2a07befaf6d7f1487ae9adcf8a3015))


### Bug Fixes

* kiln recipe loading ([3f3cca1](https://github.com/Indemnity83/logistics/commit/3f3cca1d8e9e09169487fd04a13e1af6070469ae))
* properly set sided inventory for engines and kiln ([#138](https://github.com/Indemnity83/logistics/issues/138)) ([959e1b6](https://github.com/Indemnity83/logistics/commit/959e1b6177ac87df446b4f5fa8a56b76bb57173d))
* use Fabric FuelRegistry to detect kiln fuel items ([3b10287](https://github.com/Indemnity83/logistics/commit/3b102878859386fb29fe7609191cf1654840950b))

## [0.3.3](https://github.com/Indemnity83/logistics/compare/mc1.21.1-v0.3.2...mc1.21.1-v0.3.3) (2026-02-22)


### Bug Fixes

* preserve copper pipe weathering in item state and models ([#125](https://github.com/Indemnity83/logistics/issues/125)) ([60ec68e](https://github.com/Indemnity83/logistics/commit/60ec68e654388f2ccdc5b8bee280bfd72f944b63))
* register mod content during common initialization ([#132](https://github.com/Indemnity83/logistics/issues/132)) ([43667d4](https://github.com/Indemnity83/logistics/commit/43667d4d6eec9a0e8b1d0a6808b7ec207203c0ca))
* restore laser quarry block entity renderer and client render cache cleanup ([#127](https://github.com/Indemnity83/logistics/issues/127)) ([105defa](https://github.com/Indemnity83/logistics/commit/105defad83b30b65b574220b9798c17d3958b6bb))

## [0.3.2](https://github.com/Indemnity83/logistics/compare/mc1.21.1-v0.3.1...mc1.21.1-v0.3.2) (2026-02-18)


### Bug Fixes

* correct engine piston back-face lighting in renderer ([#123](https://github.com/Indemnity83/logistics/issues/123)) ([bf7eb98](https://github.com/Indemnity83/logistics/commit/bf7eb98064f061fd93f77f14e6846659efaaed59))
* correct vertical centering for block items rendered inside pipes ([#113](https://github.com/Indemnity83/logistics/issues/113)) ([e967e05](https://github.com/Indemnity83/logistics/commit/e967e05e566fea7ffa5a4e437ea870a9f3295e39))
* preserve item filter pipe GUI entries when closing ([#122](https://github.com/Indemnity83/logistics/issues/122)) ([0d751fe](https://github.com/Indemnity83/logistics/commit/0d751feb6f713c0629efb11e6c2a40878d6a677b))

## [0.3.1](https://github.com/Indemnity83/logistics/compare/mc1.21.1-v0.3.0...mc1.21.1-v0.3.1) (2026-02-17)


### Features

* add tin, bronze, and apatite materials with worldgen and progression ([#100](https://github.com/Indemnity83/logistics/issues/100)) ([efd6acc](https://github.com/Indemnity83/logistics/commit/efd6acca1810f32d43f3566925914c6ce71e48e6))
* port to Minecraft 1.21.1 ([0a9d65f](https://github.com/Indemnity83/logistics/commit/0a9d65f7f4bffcf16edc05eb7317f5ba1b1dfd6c))


### Bug Fixes

* back-port basic rendering functions ([f36c8ea](https://github.com/Indemnity83/logistics/commit/f36c8eae9bc8ff5db03b4f1aeaf81501a2d24e1b))
* engine renderer tick handling and texture scaling ([d56d568](https://github.com/Indemnity83/logistics/commit/d56d56827a1409e0b006da1eea3763df16044533))
* item tinting on marking fluid items ([81db3ed](https://github.com/Indemnity83/logistics/commit/81db3edd9143f905124038234525ac37483f6e8c))
* migrate tin/bronze recipes to c: tags and drop apatite tag entries ([#110](https://github.com/Indemnity83/logistics/issues/110)) ([7121d47](https://github.com/Indemnity83/logistics/commit/7121d47036c597accd9b860f551f774c1d25bf70))
* pipe rendering with cutout layers, model loading, and BE renderer ([09c0623](https://github.com/Indemnity83/logistics/commit/09c0623f2bd929a36e60af0a45917baeb4ab00ca))
* recipes to match 1.21.1 format ([9097c4d](https://github.com/Indemnity83/logistics/commit/9097c4de2d09104152dff1ebb4ea38a27ec13a2a))
* render marker beams using baked model segments ([add119c](https://github.com/Indemnity83/logistics/commit/add119cdc75899e6806d2f79f80d6ac62b3818f4))
* restore non-overheating engine warm flash and scale piston speed by heat ([#111](https://github.com/Indemnity83/logistics/issues/111)) ([7976ef2](https://github.com/Indemnity83/logistics/commit/7976ef28e6e6e146ed692b2ca5090d8b637a2ae6))

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
