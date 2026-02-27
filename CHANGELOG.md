# Changelog

## [0.4.0](https://github.com/Indemnity83/logistics/compare/mc26.1-v0.3.5...mc26.1-v0.4.0) (2026-02-27)


### ⚠ BREAKING CHANGES

* The basic (non-powered) quarry has been removed and will be automatically deleted from existing worlds on upgrade. It may return in a redesigned form in a future release.
* The Logistics API has been reorganized and namespaced; internal and external integrations using the old API structure will need to update imports and references.
* Quartz pipes no longer output comparator signals. Existing item sensor pipes will automatically resolve to copper transport pipes when loading older worlds.

### Features

* add beta build workflow for release branches ([#155](https://github.com/Indemnity83/logistics/issues/155)) ([ccc3bfa](https://github.com/Indemnity83/logistics/commit/ccc3bfab9673f407486d0f37c2d1b6d2dbc37fbb))
* add copper pipe weathering with oxidation, waxing, and variant items ([#36](https://github.com/Indemnity83/logistics/issues/36)) ([ce29df5](https://github.com/Indemnity83/logistics/commit/ce29df5e5a2c17d6cc9d5d8a7716e7b43f800cdd))
* add copper round-robin pipe ([b49810c](https://github.com/Indemnity83/logistics/commit/b49810c147a86142c42a1231156000a38ce36cdd))
* add core pipe system with item transport ([087a53a](https://github.com/Indemnity83/logistics/commit/087a53ad6ee2f9690a481093fc7cd08ab8502bf1))
* add diamond filtering pipe with UI ([9cc943d](https://github.com/Indemnity83/logistics/commit/9cc943d65a032d25080b5ee2b8954b003e5ec9bc))
* add energy-powered laser quarry ([#69](https://github.com/Indemnity83/logistics/issues/69)) ([3500ad6](https://github.com/Indemnity83/logistics/commit/3500ad6cf70faf9f3a2d0b430b83f81cb67b80d2))
* add iron directional pipe with wrench tool ([709546d](https://github.com/Indemnity83/logistics/commit/709546d66e904d5b4f07f31c04d46203d54d5710))
* add item passthrough pipe for inventory bypass ([#17](https://github.com/Indemnity83/logistics/issues/17)) ([487cabb](https://github.com/Indemnity83/logistics/commit/487cabba6aa8d99c70461f280c1a433dfc7e4426))
* add kiln machine with molten glass, valve recipes, and shared heat component ([#131](https://github.com/Indemnity83/logistics/issues/131)) ([8c16640](https://github.com/Indemnity83/logistics/commit/8c166405b5953332da3d1437db232ea3bc80dab5))
* add marking fluid to color copper pipes and segment networks ([#29](https://github.com/Indemnity83/logistics/issues/29)) ([c012762](https://github.com/Indemnity83/logistics/commit/c0127625c55a4141314e0e07dc6c429819e79a20))
* add power system with engines, creative sink, and energy API ([#60](https://github.com/Indemnity83/logistics/issues/60)) ([4c6333d](https://github.com/Indemnity83/logistics/commit/4c6333d7097fd6f563f01bf2b6e1713d6ff8829d))
* add probe tool and interface-based wrench delegation ([#61](https://github.com/Indemnity83/logistics/issues/61)) ([0dbb9a8](https://github.com/Indemnity83/logistics/commit/0dbb9a870de82b31398aa87b35236bf89283785e))
* add quarry to mine blocks using tools ([#42](https://github.com/Indemnity83/logistics/issues/42)) ([1d02736](https://github.com/Indemnity83/logistics/commit/1d02736c725bfb5276eaf2ce498809ae42a7cc79))
* add quartz comparator pipe ([8042f22](https://github.com/Indemnity83/logistics/commit/8042f22d43194f5f365dc5625ce85b73c7c89a51))
* add quartz pipe inventory overflow behavior ([#16](https://github.com/Indemnity83/logistics/issues/16)) ([5e9b51b](https://github.com/Indemnity83/logistics/commit/5e9b51badd130c6ba01e2821dfee87242f2fce9c))
* add ResourceId compatibility layer and migrate identifier usage ([#136](https://github.com/Indemnity83/logistics/issues/136)) ([9bca415](https://github.com/Indemnity83/logistics/commit/9bca415960ad42407059f86ceacc5eaaa8c859c7))
* add tin, bronze, and apatite materials with worldgen and progression ([#100](https://github.com/Indemnity83/logistics/issues/100)) ([879aa61](https://github.com/Indemnity83/logistics/commit/879aa61690542435a6f51d3189813fa277b9936e))
* add void pipe ([1bdee7b](https://github.com/Indemnity83/logistics/commit/1bdee7b9a26102aa01158d2e3417ac217b38c55c))
* allow iron pipe output to any connection ([270bc3d](https://github.com/Indemnity83/logistics/commit/270bc3d35c50844282859b7351490a1c3a7a86d6))
* bump minecraft to 26.1-snapshot-6 ([67fe1d2](https://github.com/Indemnity83/logistics/commit/67fe1d22db93b904791d3d4c90db0070aba51991))
* extraction pipes now use energy from engines to power extractions ([#74](https://github.com/Indemnity83/logistics/issues/74)) ([ee02bd9](https://github.com/Indemnity83/logistics/commit/ee02bd9edd1ceae0f435a03de23126ad93b44188))
* refresh gear textures and update creative tab ([#91](https://github.com/Indemnity83/logistics/issues/91)) ([d00bbd2](https://github.com/Indemnity83/logistics/commit/d00bbd2feee1631815c9f35a00a38900cc000685))


### Bug Fixes

* allow extraction pipes to accept items using default pipe rules ([#38](https://github.com/Indemnity83/logistics/issues/38)) ([9e87c8f](https://github.com/Indemnity83/logistics/commit/9e87c8f3a70089911407d2146445a3220ba8a961))
* allow wrench to reset Stirling overheat and prevent non-overheating engines from overheating ([#62](https://github.com/Indemnity83/logistics/issues/62)) ([5172210](https://github.com/Indemnity83/logistics/commit/517221094242cf42c44250f2255bf3510beb8e05))
* correct engine blockstates, particles, and drops ([#86](https://github.com/Indemnity83/logistics/issues/86)) ([088b00f](https://github.com/Indemnity83/logistics/commit/088b00fde890339bb62cf5e49e08e8199f7cc2de))
* correct engine renderer rotation for north/south facing ([#71](https://github.com/Indemnity83/logistics/issues/71)) ([46f8c2c](https://github.com/Indemnity83/logistics/commit/46f8c2c199bd3e21de07c14e95e4f56cb8a27ed3))
* correct item passthrough pipe recipe ([#18](https://github.com/Indemnity83/logistics/issues/18)) ([f65f489](https://github.com/Indemnity83/logistics/commit/f65f489fafed4ca6c575c4feb8ab3e2360355d58))
* correct laser quarry LED working state and screen rotation ([#99](https://github.com/Indemnity83/logistics/issues/99)) ([31a280c](https://github.com/Indemnity83/logistics/commit/31a280c4959d5b96c82c2f902c0c6af34b7d010c))
* correct vertical centering for block items rendered inside pipes ([#113](https://github.com/Indemnity83/logistics/issues/113)) ([ea89339](https://github.com/Indemnity83/logistics/commit/ea89339ee7b44351fcbbe4eef68c120a14f494ca))
* kiln recipe loading ([fa13752](https://github.com/Indemnity83/logistics/commit/fa1375226bab57ec9ae3fc59fb3e077f5779bb51))
* migrate tin/bronze recipes to c: tags and drop apatite tag entries ([#110](https://github.com/Indemnity83/logistics/issues/110)) ([c32d5e3](https://github.com/Indemnity83/logistics/commit/c32d5e3f9f456f187f22bfe843d73d89c933b985))
* normalize engine rendering with extra models, cutout layer, and heat tints ([#106](https://github.com/Indemnity83/logistics/issues/106)) ([f3c3a1a](https://github.com/Indemnity83/logistics/commit/f3c3a1a15e5d6f35bc09f045b549de49fd7cf988))
* pipe interaction and presentation polish ([#27](https://github.com/Indemnity83/logistics/issues/27)) ([3c1c5d4](https://github.com/Indemnity83/logistics/commit/3c1c5d4380c34fb8e8af538d24159c31ceba8bc6))
* preserve pipe block entity state on normal pick-block ([#45](https://github.com/Indemnity83/logistics/issues/45)) ([a72956f](https://github.com/Indemnity83/logistics/commit/a72956f43589757b406a830d2f8c4667022993e9))
* prevent redstone engine from powering blocks that don’t accept low-tier energy ([#73](https://github.com/Indemnity83/logistics/issues/73)) ([3144215](https://github.com/Indemnity83/logistics/commit/31442159b8971271f4c410c076eb7549af60e45c))
* properly set sided inventory for engines and kiln ([#138](https://github.com/Indemnity83/logistics/issues/138)) ([d73d983](https://github.com/Indemnity83/logistics/commit/d73d9830f9b36eb0b990e2a1698ad2c262d33be2))
* quarry now drops item when broken ([#47](https://github.com/Indemnity83/logistics/issues/47)) ([84d1fbe](https://github.com/Indemnity83/logistics/commit/84d1fbe3c9a593aa83701019db6e95b0f48f560d))
* register mod content during common initialization ([#132](https://github.com/Indemnity83/logistics/issues/132)) ([c5eac34](https://github.com/Indemnity83/logistics/commit/c5eac34659cc16e79c3d733a10029e309e26edb5))
* remove invalid property references from pipe blockstates ([#14](https://github.com/Indemnity83/logistics/issues/14)) ([a621f05](https://github.com/Indemnity83/logistics/commit/a621f0560f8b2628fa9faaf262033773552552cb))
* restore crafting and kiln recipe loading by updating recipe data paths ([#152](https://github.com/Indemnity83/logistics/issues/152)) ([3c4d2c5](https://github.com/Indemnity83/logistics/commit/3c4d2c5c6f19a7be65e35b09fc85e850e385bef1))
* restore non-overheating engine warm flash and scale piston speed by heat ([#111](https://github.com/Indemnity83/logistics/issues/111)) ([9e6478c](https://github.com/Indemnity83/logistics/commit/9e6478c1563d8dafaa740ee2474e62c928e1d04a))
* split item stacks when inventories have partial capacity ([#44](https://github.com/Indemnity83/logistics/issues/44)) ([a53cea0](https://github.com/Indemnity83/logistics/commit/a53cea04bc7f55cf7e3efc48ed2598f8eb65d1df))
* standardize internal pipe item drops via PipeBlockEntity helper ([#149](https://github.com/Indemnity83/logistics/issues/149)) ([6742071](https://github.com/Indemnity83/logistics/commit/67420714560157cd91e25d3be25fe4b01c8e983b))
* tune pipe item speeds ([#25](https://github.com/Indemnity83/logistics/issues/25)) ([96c1fb6](https://github.com/Indemnity83/logistics/commit/96c1fb699a85a78ce092b0ce0442eb9af3169d0f))
* update beta build workflow for new release-please branch naming ([#157](https://github.com/Indemnity83/logistics/issues/157)) ([5cf7a7f](https://github.com/Indemnity83/logistics/commit/5cf7a7f15dbf69a2ef8efe07dd83c98d5f8ff8b6))
* update Gradle testLogging exceptionFormat syntax ([#147](https://github.com/Indemnity83/logistics/issues/147)) ([cee7c92](https://github.com/Indemnity83/logistics/commit/cee7c927a5bd18612b2f900aa5577bc08215ab28))
* update Stirling Engine UI texture and title translation ([#63](https://github.com/Indemnity83/logistics/issues/63)) ([973dbf8](https://github.com/Indemnity83/logistics/commit/973dbf868879d4e0e91eccecd20447a3a936b038))


### Performance

* avoid per-tick pipe connection cache recalculation ([#143](https://github.com/Indemnity83/logistics/issues/143)) ([2a7f461](https://github.com/Indemnity83/logistics/commit/2a7f461e1d288a05b38b2a1bd75285386e643747))
* dynamic pipe rendering via Block Entity Renderer (fixes [#6](https://github.com/Indemnity83/logistics/issues/6)) ([#11](https://github.com/Indemnity83/logistics/issues/11)) ([5383ec3](https://github.com/Indemnity83/logistics/commit/5383ec36604c47f9f8ce33debe7a39e1da17b87b))
* gate pipe connection updates behind cache dirty flag ([#150](https://github.com/Indemnity83/logistics/issues/150)) ([bd86812](https://github.com/Indemnity83/logistics/commit/bd868120698239522fdf3f36f7157820414c8bef))
* gate timing logs behind system property ([aab01de](https://github.com/Indemnity83/logistics/commit/aab01de7c2a578f96dfaf948a5f8646f8534329a))
* reduce pipe blockstate count ([acb9993](https://github.com/Indemnity83/logistics/commit/acb9993650d579a51a8c9be4f42585271241b1ad))


### Refactorings

* add NBT compatibility helpers for compound/list reads ([#97](https://github.com/Indemnity83/logistics/issues/97)) ([3156ffc](https://github.com/Indemnity83/logistics/commit/3156ffc4398d5b3abd9d149f1e1e47dbcf007b91))
* behavior-based pipe naming and material progression ([#9](https://github.com/Indemnity83/logistics/issues/9)) ([8eb95d9](https://github.com/Indemnity83/logistics/commit/8eb95d9f56a7ecd908ad277f1ef0a37bbd4081a0))
* break up PipeRuntime tick method and simplify routing ([#34](https://github.com/Indemnity83/logistics/issues/34)) ([88d36e3](https://github.com/Indemnity83/logistics/commit/88d36e3dc6488266b4760807988acc1f14b5f9f6))
* centralize resource identifier creation ([#96](https://github.com/Indemnity83/logistics/issues/96)) ([5005cdc](https://github.com/Indemnity83/logistics/commit/5005cdc4b3537c9baf251ca8d42b9313da5b29b8))
* consolidate pipe arm models with render-time rotation ([#35](https://github.com/Indemnity83/logistics/issues/35)) ([d825293](https://github.com/Indemnity83/logistics/commit/d8252937de85480a414b6481b47ae012c7cc1ede))
* consolidate traveling item speed physics into dedicated helper with tests ([#148](https://github.com/Indemnity83/logistics/issues/148)) ([5c41086](https://github.com/Indemnity83/logistics/commit/5c41086fd921736851267e2487d4ce5de69e2e9b))
* expand core block entity capabilities and machine behaviors ([#101](https://github.com/Indemnity83/logistics/issues/101)) ([18e8c51](https://github.com/Indemnity83/logistics/commit/18e8c5102851de85a9e78441d8173bfde5207e31))
* extract shared engine block behavior into core power lib ([#72](https://github.com/Indemnity83/logistics/issues/72)) ([d01e123](https://github.com/Indemnity83/logistics/commit/d01e1232fac97a0061e929ae3e4ab5e1d86c23b7))
* implement composable pipe behavior modules ([0d95820](https://github.com/Indemnity83/logistics/commit/0d95820993043d6751e0ca622e2ca999f17abd7d))
* load marker beam renderer model via Fabric extra model API ([#112](https://github.com/Indemnity83/logistics/issues/112)) ([b8b5e4f](https://github.com/Indemnity83/logistics/commit/b8b5e4fd1c6e59fbe87903ab2696f1d11fbf63b8))
* migrate energy system to Team Reborn SimpleEnergyStorage ([#93](https://github.com/Indemnity83/logistics/issues/93)) ([1ebe865](https://github.com/Indemnity83/logistics/commit/1ebe8653a14f995f1c5f1f8a5ab3e57ecc9c2075))
* modernize pipe routing with ItemStorage API ([f0ce874](https://github.com/Indemnity83/logistics/commit/f0ce8745df0ef8b7f499131c9053b0b594418d3c))
* register extra block models via Fabric ModelLoadingPlugin with static keys ([#129](https://github.com/Indemnity83/logistics/issues/129)) ([7fed078](https://github.com/Indemnity83/logistics/commit/7fed078bc4b62fee4a66b30469aaf8c3dfea43a6))
* register laser quarry renderer models via ModelLoadingPlugin ([#128](https://github.com/Indemnity83/logistics/issues/128)) ([7fcb7cd](https://github.com/Indemnity83/logistics/commit/7fcb7cde5dce5aec3be05e8e313cebe2e28bd8ef))
* reorganize domain bootstraps and client initialization order ([#90](https://github.com/Indemnity83/logistics/issues/90)) ([41482f5](https://github.com/Indemnity83/logistics/commit/41482f5d73b67ede50b7b6f9a87f4d7ae7bfb510))
* replace traveling item progress magic numbers with named thresholds ([#146](https://github.com/Indemnity83/logistics/issues/146)) ([8cb27b0](https://github.com/Indemnity83/logistics/commit/8cb27b03a87cba32e88079abbd7456e2fa112188))
* route wrench interactions through module onUseWithItem ([#31](https://github.com/Indemnity83/logistics/issues/31)) ([ec534f8](https://github.com/Indemnity83/logistics/commit/ec534f82b5c8c1de6b38c6ac991ce17b042682c8))
* split mod into domain-based components ([#49](https://github.com/Indemnity83/logistics/issues/49)) ([5182670](https://github.com/Indemnity83/logistics/commit/5182670dc24cbd298b8c2f6f1d80198aacfef048))
* standardize block entity NBT persistence and client sync ([#94](https://github.com/Indemnity83/logistics/issues/94)) ([cc908e3](https://github.com/Indemnity83/logistics/commit/cc908e3bc46adfe01df3fd94d6dd6bf01d33ca57))
* switch to Mojang mappings (mojmap) APIs ([#82](https://github.com/Indemnity83/logistics/issues/82)) ([122aede](https://github.com/Indemnity83/logistics/commit/122aede4542f043d388f1d5870d36f8677b33063))
* unify pipe connectivity under PipeConnection and registry ([#70](https://github.com/Indemnity83/logistics/issues/70)) ([2319a34](https://github.com/Indemnity83/logistics/commit/2319a341d2cac96b6a6053984cc770e6f53d0d89))


### Testing

* add unit and game test suites with CI execution ([cdb5e7d](https://github.com/Indemnity83/logistics/commit/cdb5e7d370ef46d3d85f0ea60b1e2595cdfc2b60))


### Build System

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
