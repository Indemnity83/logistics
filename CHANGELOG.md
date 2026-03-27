# Changelog

## [0.4.0](https://github.com/Indemnity83/logistics/compare/mc26.1-v0.3.5...mc26.1-v0.4.0) (2026-03-27)


### chore

* bump to v0.4.0 ([ab779cd](https://github.com/Indemnity83/logistics/commit/ab779cd83b2968ac47e42e172f1dba0d5d6ac1e0))


### Features

* add beta build workflow for release branches ([#155](https://github.com/Indemnity83/logistics/issues/155)) ([ccc3bfa](https://github.com/Indemnity83/logistics/commit/ccc3bfab9673f407486d0f37c2d1b6d2dbc37fbb))
* add BULK50, BULK100, INFINITE, and FULL supply modes to supplier pipe ([9ddf234](https://github.com/Indemnity83/logistics/commit/9ddf234ac423d7ce25a395f8f78bca6fa525f10b))
* add crafting logistics pipe with autocrafter integration ([f16c43e](https://github.com/Indemnity83/logistics/commit/f16c43ee7da8e334d0a4c2520d2765138a5f8238))
* add dynamic chassis module system for logistics pipes ([#195](https://github.com/Indemnity83/logistics/issues/195)) ([e568364](https://github.com/Indemnity83/logistics/commit/e56836457fe088db6077cea3a8d114ffa0351f5d))
* add import button for autocrafter recipes in crafting screen ([#204](https://github.com/Indemnity83/logistics/issues/204)) ([8b5aa21](https://github.com/Indemnity83/logistics/commit/8b5aa21110f6e4a6b088958c17800bc339cd149f))
* add modular logistics pipe network with provider/supplier/requester/sink modules ([#161](https://github.com/Indemnity83/logistics/issues/161)) ([b3933a9](https://github.com/Indemnity83/logistics/commit/b3933a903fa86711eb06136d0e17c011b64dfcb2))
* add processing and satellite pipe modules with GUIs ([#206](https://github.com/Indemnity83/logistics/issues/206)) ([1e2ed94](https://github.com/Indemnity83/logistics/commit/1e2ed94b83d38b49e068e69550fa024ceaa246ad))
* add runtime-toggleable network debug logging and /logistics debug command ([#174](https://github.com/Indemnity83/logistics/issues/174)) ([f96c19c](https://github.com/Indemnity83/logistics/commit/f96c19c92b10973cef18fbb13a43145ad94c19a7))
* add supplier mode configuration and fulfillment normalization ([#183](https://github.com/Indemnity83/logistics/issues/183)) ([f13524d](https://github.com/Indemnity83/logistics/commit/f13524d0a2a91c1ac8ded0a8d85e1b058681c4cb))
* add support for chassis logistics pipes with modular slots ([#194](https://github.com/Indemnity83/logistics/issues/194)) ([1499338](https://github.com/Indemnity83/logistics/commit/14993382852ed2ce11ec2f26d049ef5105ec9341))
* clear module state when modules are removed from pipes ([#203](https://github.com/Indemnity83/logistics/issues/203)) ([59f04dc](https://github.com/Indemnity83/logistics/commit/59f04dc5aa5c9630fc24fc2dcd557fd298982c8c))
* enable processing of multiple concurrent orders in pipeline ([#228](https://github.com/Indemnity83/logistics/issues/228)) ([bf2c68f](https://github.com/Indemnity83/logistics/commit/bf2c68f19ddbaf371279d9201a32c7997ba359a1))
* implement capacity limits for crafting module batches ([#189](https://github.com/Indemnity83/logistics/issues/189)) ([f553cca](https://github.com/Indemnity83/logistics/commit/f553ccad6d3b6db609cf5a9f807103d0baa8e1ca))
* implement fulfillment checks for dynamic providers in logistics network ([#178](https://github.com/Indemnity83/logistics/issues/178)) ([cbda39c](https://github.com/Indemnity83/logistics/commit/cbda39c4d00c3768c1f3bc503c46d2cd582bf69a))
* implement Mod Sink module with GUI for mod filtering ([#207](https://github.com/Indemnity83/logistics/issues/207)) ([8a0d9e6](https://github.com/Indemnity83/logistics/commit/8a0d9e600805062c140ddf15cfbf83f43d0e11b9))
* implement supply reconciliation for order fulfillment failures ([#188](https://github.com/Indemnity83/logistics/issues/188)) ([6018b9e](https://github.com/Indemnity83/logistics/commit/6018b9edffdefedc01ee2d970b85a2467b95abce))
* implement tiered crafting modules with ingredient buffers ([#200](https://github.com/Indemnity83/logistics/issues/200)) ([6f5d6fc](https://github.com/Indemnity83/logistics/commit/6f5d6fc6774f003e05fc3294e36898f2e29a935f))
* introduce network job orchestration for item delivery ([#186](https://github.com/Indemnity83/logistics/issues/186)) ([4566b04](https://github.com/Indemnity83/logistics/commit/4566b048ed47ccb9f2607329f0554158e067ee49))
* introduce pure Java value objects for network states ([#184](https://github.com/Indemnity83/logistics/issues/184)) ([004353b](https://github.com/Indemnity83/logistics/commit/004353bfc0858c28fdc991e7e9e9a76780a3c636))
* introduce ReservationManager for item reservation tracking ([#185](https://github.com/Indemnity83/logistics/issues/185)) ([5668c16](https://github.com/Indemnity83/logistics/commit/5668c1614211de5637c790509d22032a7ea63a91))
* optimize auto crafter pulse duration and cooldown settings ([#193](https://github.com/Indemnity83/logistics/issues/193)) ([7a6a685](https://github.com/Indemnity83/logistics/commit/7a6a685b9de0b0dc86b223d49feb9098b73f538e))
* register kiln recipe type and serializer for JSON parsing ([#154](https://github.com/Indemnity83/logistics/issues/154)) ([9705899](https://github.com/Indemnity83/logistics/commit/9705899c404970b4df2605e1d6418dd161b5a4b5))
* support multiple concurrent crafting orders ([#175](https://github.com/Indemnity83/logistics/issues/175)) ([9d866ae](https://github.com/Indemnity83/logistics/commit/9d866ae58d84ee62876b82a889a552237dbd4756))
* track in-transit orders from provider extraction through pipe delivery ([#165](https://github.com/Indemnity83/logistics/issues/165)) ([9ddf234](https://github.com/Indemnity83/logistics/commit/9ddf234ac423d7ce25a395f8f78bca6fa525f10b))


### Bug Fixes

* add loot tables for pipes to drop on block break ([#181](https://github.com/Indemnity83/logistics/issues/181)) ([4f78457](https://github.com/Indemnity83/logistics/commit/4f7845708ac7f0dd3b38b0bc69e3379463a3be08))
* address edge case handling in dispatch and availability logic ([#192](https://github.com/Indemnity83/logistics/issues/192)) ([a635914](https://github.com/Indemnity83/logistics/commit/a63591447c4c1b0511c1fcc7a6593d9f16e6802c))
* cancel entry orders when removing completed crafting queue entry ([6d44322](https://github.com/Indemnity83/logistics/commit/6d44322fa7c17c77aded48a83bffecfb0288a827))
* cap crafted amounts to requested quantity in crafting module ([#201](https://github.com/Indemnity83/logistics/issues/201)) ([1082870](https://github.com/Indemnity83/logistics/commit/1082870a05e2fece2cd91d70bac1033e085ebc42))
* crash caused by jar remapping collision in IPipeAccess ([#222](https://github.com/Indemnity83/logistics/issues/222)) ([75db23d](https://github.com/Indemnity83/logistics/commit/75db23d66cdcabbe3eef95785743ae48afd76099))
* notify delivery tracking before dropping undeliverable pipe items ([#173](https://github.com/Indemnity83/logistics/issues/173)) ([13480e2](https://github.com/Indemnity83/logistics/commit/13480e2433658189951ca46577d3c4e1c61d193a))
* purge terminal to resolve memory leak in job coordination ([#208](https://github.com/Indemnity83/logistics/issues/208)) ([f4ed3c9](https://github.com/Indemnity83/logistics/commit/f4ed3c9b66946f5b167258945eef88dbea05df19))
* register menu type for kiln screen handler ([#219](https://github.com/Indemnity83/logistics/issues/219)) ([cdc8c57](https://github.com/Indemnity83/logistics/commit/cdc8c57a323c1958f779bb5cd19256c3ca2b3969))
* remove unused imports from IWorldView and PipeContext classes ([#210](https://github.com/Indemnity83/logistics/issues/210)) ([c887f9a](https://github.com/Indemnity83/logistics/commit/c887f9a62a3e10838310bee07e163cb1586dee47))
* replace inventory-delta pending tracking with network order accounting ([9ddf234](https://github.com/Indemnity83/logistics/commit/9ddf234ac423d7ce25a395f8f78bca6fa525f10b))
* resolve crafting logic triggering for supplier pipes ([#180](https://github.com/Indemnity83/logistics/issues/180)) ([1d6f4ee](https://github.com/Indemnity83/logistics/commit/1d6f4eeeed8e17a69d4eb2785d106f7be1ed39f5))
* resolve crafting order placement logic for missing items ([#205](https://github.com/Indemnity83/logistics/issues/205)) ([f3602e8](https://github.com/Indemnity83/logistics/commit/f3602e8639440cb4a0826eb4476bff6f8d58c711))
* resolve GUI mode mismatch in ProviderScreenHandler class ([#179](https://github.com/Indemnity83/logistics/issues/179)) ([86d7d3c](https://github.com/Indemnity83/logistics/commit/86d7d3cc75d65c48d7d078cb57144fccd617a422))
* resolve quarry test failures by improving terrain handling ([#227](https://github.com/Indemnity83/logistics/issues/227)) ([c857591](https://github.com/Indemnity83/logistics/commit/c85759168cb941a7190b39ce0f4ff4baababed6f))
* show craftable items as 0 stock and stabilize requester search sorting ([#176](https://github.com/Indemnity83/logistics/issues/176)) ([16ccadf](https://github.com/Indemnity83/logistics/commit/16ccadf25b5a1c95fe5dbe65c7e6eed8a9c2432b))
* update beta build workflow for new release-please branch naming ([#157](https://github.com/Indemnity83/logistics/issues/157)) ([5cf7a7f](https://github.com/Indemnity83/logistics/commit/5cf7a7f15dbf69a2ef8efe07dd83c98d5f8ff8b6))
* update filter item retrieval logic in extractor modules ([#198](https://github.com/Indemnity83/logistics/issues/198)) ([3586eb6](https://github.com/Indemnity83/logistics/commit/3586eb643db4eb3658f877abbda4389cf494ec4c))


### Performance

* throttle provider pipe extraction to 8 items per cycle at 6-tick intervals   ([9ddf234](https://github.com/Indemnity83/logistics/commit/9ddf234ac423d7ce25a395f8f78bca6fa525f10b))


### Refactorings

* clarify and standardize supplier mode names and logic ([#199](https://github.com/Indemnity83/logistics/issues/199)) ([1cb190f](https://github.com/Indemnity83/logistics/commit/1cb190f6668add9599f2fc1554923c991a0537e2))
* consolidate filter item management with FilterSlots class ([#215](https://github.com/Indemnity83/logistics/issues/215)) ([ac30bae](https://github.com/Indemnity83/logistics/commit/ac30baec0d5a2fc3e453ad392c92623c11ff2ea8))
* dispatch partial inventory supply before falling back to crafting ([#177](https://github.com/Indemnity83/logistics/issues/177)) ([9a3d7f2](https://github.com/Indemnity83/logistics/commit/9a3d7f229c942d2312fc2a05712dbc61eeea5e5e))
* extract dispatch planning logic into RequestPlanner ([#187](https://github.com/Indemnity83/logistics/issues/187)) ([4a4354a](https://github.com/Indemnity83/logistics/commit/4a4354abfc1e50cfbbde7b3a1840e18d363a8ba1))
* implement focused role interfaces for pipe modules ([#213](https://github.com/Indemnity83/logistics/issues/213)) ([94c4482](https://github.com/Indemnity83/logistics/commit/94c4482744483f1dd14946f3661f6c71fd151636))
* introduce explicit dispatch commands for network actions ([#190](https://github.com/Indemnity83/logistics/issues/190)) ([071c63c](https://github.com/Indemnity83/logistics/commit/071c63ca80dfcbee7ecdbaa81d86a3fc9a9a9933))
* move ItemRequest and LogisticsOrder to core.lib.network ([9ddf234](https://github.com/Indemnity83/logistics/commit/9ddf234ac423d7ce25a395f8f78bca6fa525f10b))
* move pipe components into core library package ([#218](https://github.com/Indemnity83/logistics/issues/218)) ([03c6d94](https://github.com/Indemnity83/logistics/commit/03c6d94ba9c862ee7bfdba53c9e25fe338dd5954))
* move sink management logic to SinkResolver class ([#214](https://github.com/Indemnity83/logistics/issues/214)) ([ec5f186](https://github.com/Indemnity83/logistics/commit/ec5f186d3847327e41652923a8a5ff7e6793bd54))
* optimize ArrayList imports in PipeContext class ([#209](https://github.com/Indemnity83/logistics/issues/209)) ([5fd7ef9](https://github.com/Indemnity83/logistics/commit/5fd7ef9dffd7bbcd42593c2e3e3a22901729d3bb))
* remove default route sink registration and methods ([#216](https://github.com/Indemnity83/logistics/issues/216)) ([36f076b](https://github.com/Indemnity83/logistics/commit/36f076b98675d87a5a44c013a78a91be0eb7360a))
* remove unnecessary buffer handling from crafting module ([#202](https://github.com/Indemnity83/logistics/issues/202)) ([5f6a3a1](https://github.com/Indemnity83/logistics/commit/5f6a3a1aa55029a94cf11cf7d5a90b7893510a3c))
* reorganize network packet classes and update imports ([#212](https://github.com/Indemnity83/logistics/issues/212)) ([9b6c75f](https://github.com/Indemnity83/logistics/commit/9b6c75f1b36556a49d359ac930f223221b059906))
* replace NBT abbreviations in ProviderModule class ([#211](https://github.com/Indemnity83/logistics/issues/211)) ([4b079bd](https://github.com/Indemnity83/logistics/commit/4b079bd01b3136087316eed8e17e82d481882eb6))
* replace network request system with standing-order dispatch controller ([f16c43e](https://github.com/Indemnity83/logistics/commit/f16c43ee7da8e334d0a4c2520d2765138a5f8238))
* restructure network package and update related imports ([#182](https://github.com/Indemnity83/logistics/issues/182)) ([c8d35ae](https://github.com/Indemnity83/logistics/commit/c8d35ae3e9096f7cce1ceb3400b4de393e478445))
* simplify screen constructor parameters for several screens ([ecb9401](https://github.com/Indemnity83/logistics/commit/ecb9401d4bef457bf63342a411a849fc66de10e5))


### Testing

* add integration and flow tests for logistics network ([#221](https://github.com/Indemnity83/logistics/issues/221)) ([d49fcec](https://github.com/Indemnity83/logistics/commit/d49fcec5175da1e325b67d01944fe6317ea229d1))
* add unit tests for core logistics components and services ([#191](https://github.com/Indemnity83/logistics/issues/191)) ([2447a9c](https://github.com/Indemnity83/logistics/commit/2447a9cd071806a0429a369093581214caffea2d))
* enhance test suite with new module and game tests ([#223](https://github.com/Indemnity83/logistics/issues/223)) ([d3b4eb7](https://github.com/Indemnity83/logistics/commit/d3b4eb78295da213dba8305e0b5946bf19a9a4f1))


### Build System

* adjust beta workflow to set game version filter for snapshot builds ([afb87a2](https://github.com/Indemnity83/logistics/commit/afb87a25470ddae85e3ac984bc4f39864ec256f8))
* transition to stable release numbering ([4772b50](https://github.com/Indemnity83/logistics/commit/4772b50fd2bfdfe525c67676e603004e96bdb681))

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
