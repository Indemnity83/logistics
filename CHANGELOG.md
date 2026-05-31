# Changelog

## [0.6.0](https://github.com/Indemnity83/logistics/compare/mc1.21.11-v0.5.6...mc1.21.11-v0.6.0) (2026-05-31)


### Features

* add NeoForge client rendering support ([#381](https://github.com/Indemnity83/logistics/issues/381)) ([a1b958c](https://github.com/Indemnity83/logistics/commit/a1b958c85fd6821770a6e6076abb14358fcf3f60))
* add NeoForge platform SPI services ([#378](https://github.com/Indemnity83/logistics/issues/378)) ([b2b3feb](https://github.com/Indemnity83/logistics/commit/b2b3feb9a1f9c20e048815b66bd38df3f51158b9))
* add NeoForge storage adapters ([56a58cf](https://github.com/Indemnity83/logistics/commit/56a58cff6cc8d3efd84562ce9fbdc4594be76fe1))
* add power cables ([3acd758](https://github.com/Indemnity83/logistics/commit/3acd75831ac3e1d8ff91808b733a2e7ee53e11af))
* wire NeoForge capabilities ([#379](https://github.com/Indemnity83/logistics/issues/379)) ([eabe091](https://github.com/Indemnity83/logistics/commit/eabe0915f26ecefe81cdfb5fd22f11c60a02cf9e))
* wire NeoForge networking and lifecycle ([#380](https://github.com/Indemnity83/logistics/issues/380)) ([4679062](https://github.com/Indemnity83/logistics/commit/467906278df0b93eab96819d50fd73e08f462dca))


### Bug Fixes

* add validation and sanitization for logistics config fields ([#398](https://github.com/Indemnity83/logistics/issues/398)) ([2e4e55b](https://github.com/Indemnity83/logistics/commit/2e4e55b223f7582985714e5dc1ae07a65444ad59))
* clamp energy values and transfer amounts to non-negative ([#399](https://github.com/Indemnity83/logistics/issues/399)) ([a9bd9ed](https://github.com/Indemnity83/logistics/commit/a9bd9ed29f03407106e1f664f55026dfcd9f8a48))
* correct power cable compilation errors for mc/1.21.11 ([#358](https://github.com/Indemnity83/logistics/issues/358)) ([0961cad](https://github.com/Indemnity83/logistics/commit/0961cad6484afca4442678bb5e47e598642e49ba))
* handle failed deliveries in logistics network tracking ([#397](https://github.com/Indemnity83/logistics/issues/397)) ([e165e38](https://github.com/Indemnity83/logistics/commit/e165e386af84915da164eba150d0dfcd4a820a4c))
* preserve item components in filter pipe slots across save/reload ([#386](https://github.com/Indemnity83/logistics/issues/386)) ([99cc1ac](https://github.com/Indemnity83/logistics/commit/99cc1ac82142680f73f78d3a2dafd84b97c3ccc0))
* show Macerator recipe category in JEI on NeoForge ([#390](https://github.com/Indemnity83/logistics/issues/390)) ([5086881](https://github.com/Indemnity83/logistics/commit/508688141bf140c11f9b19a7395e8686a7cd50e6))
* split loader-specific marking fluid recipes ([#382](https://github.com/Indemnity83/logistics/issues/382)) ([be203cb](https://github.com/Indemnity83/logistics/commit/be203cb3371bae2ac13bab3cd22746be5a8bd519))
* track META-INF service files blocked by overly broad gitignore ([5fb4573](https://github.com/Indemnity83/logistics/commit/5fb457332c5e5114e791cbaa68ace00f9eef9f13))


### Refactorings

* Add NeoForge groundwork and decouple bootstrap initialization ([#306](https://github.com/Indemnity83/logistics/issues/306)) ([9362487](https://github.com/Indemnity83/logistics/commit/936248760f5f771eecc4b5e3ccccca166d7d55de))
* implement ISlottedItemStorage in Container and NeoForge storage ([#400](https://github.com/Indemnity83/logistics/issues/400)) ([8c0e946](https://github.com/Indemnity83/logistics/commit/8c0e9460851b3e96780e85c25d4b7b76625c1469))
* implement ItemMatcher utility for filtering logic ([#389](https://github.com/Indemnity83/logistics/issues/389)) ([2b6ee27](https://github.com/Indemnity83/logistics/commit/2b6ee279eab5dbd52ac824be29dee49535c27e9f))
* introduce ClientModelRegistry SPI to abstract Fabric model loading ([#356](https://github.com/Indemnity83/logistics/issues/356)) ([1429673](https://github.com/Indemnity83/logistics/commit/1429673b4961d358951104964affd0ea43f2b962))
* introduce FuelHelper interface for fuel value management ([#364](https://github.com/Indemnity83/logistics/issues/364)) ([744d139](https://github.com/Indemnity83/logistics/commit/744d139d0431971020ccb238a57e3e1ceff468f1))
* introduce loader-agnostic IEnergyStorage ([#340](https://github.com/Indemnity83/logistics/issues/340)) ([cfcf4fb](https://github.com/Indemnity83/logistics/commit/cfcf4fbb84f8596d6b4fb491a76a67f3e6dfe2a2))
* introduce loader-agnostic IFluidStorage contract ([#351](https://github.com/Indemnity83/logistics/issues/351)) ([dc67b23](https://github.com/Indemnity83/logistics/commit/dc67b23ab427ed6839a0a9b16a1e91496b407681))
* introduce loader-agnostic IItemStorage contract ([#349](https://github.com/Indemnity83/logistics/issues/349)) ([fc57d3e](https://github.com/Indemnity83/logistics/commit/fc57d3e8ecdbc72954b88b2994b8fadd8884ddc2))
* move capability lookup registrations to fabric module ([#343](https://github.com/Indemnity83/logistics/issues/343)) ([7bb2f30](https://github.com/Indemnity83/logistics/commit/7bb2f30acdad44c60fca506a7c46acf87837a2c2))
* move creative tab and biome registration to Fabric module ([#344](https://github.com/Indemnity83/logistics/issues/344)) ([bd48875](https://github.com/Indemnity83/logistics/commit/bd48875aea7bdb38dc9e6a7124b9e8a49ea35537))
* move fabric event registrations out of common module ([#341](https://github.com/Indemnity83/logistics/issues/341)) ([5caaa13](https://github.com/Indemnity83/logistics/commit/5caaa132db7bdabe291fbe108c06170cc71f6d76))
* move packet registration out of common module ([#342](https://github.com/Indemnity83/logistics/issues/342)) ([b14548d](https://github.com/Indemnity83/logistics/commit/b14548d99a09e3483c4994324b8fef85e1f4a49e))
* remove fabric-api from common compile classpath ([#361](https://github.com/Indemnity83/logistics/issues/361)) ([96e742d](https://github.com/Indemnity83/logistics/commit/96e742d1a692d137941054d18f3dae055a691bad))
* remove last Fabric API imports from common/ ([#352](https://github.com/Indemnity83/logistics/issues/352)) ([5900289](https://github.com/Indemnity83/logistics/commit/59002898bd64f49f6e45b7568a6ca0b9755e12ba))
* reorganize core.lib sub-packages ([#350](https://github.com/Indemnity83/logistics/issues/350)) ([678aaea](https://github.com/Indemnity83/logistics/commit/678aaea99201b195e5901ad7d9d7ce11741ecddc))
* simplify block entity registration process with factories ([#345](https://github.com/Indemnity83/logistics/issues/345)) ([d2a398d](https://github.com/Indemnity83/logistics/commit/d2a398dcf1565923803c3e66f0acc3460b02a6a8))
* split vanilla client code into common/src/client/ for multi-loader support ([#355](https://github.com/Indemnity83/logistics/issues/355)) ([18c7cc2](https://github.com/Indemnity83/logistics/commit/18c7cc244ddb6b9b78c8fd7d3f09b1e16018ed92))
* streamline bootstrap classes to registration-only responsibilities ([#348](https://github.com/Indemnity83/logistics/issues/348)) ([be39147](https://github.com/Indemnity83/logistics/commit/be391478253cb8b92ed5c09aebe57aba8e4b366d))
* unify platform access with service implementations ([#347](https://github.com/Indemnity83/logistics/issues/347)) ([1b2f104](https://github.com/Indemnity83/logistics/commit/1b2f1046a3c778118dbdc4644c45f620f6292d63))
* wire neoforge subproject to compile against common sources ([#360](https://github.com/Indemnity83/logistics/issues/360)) ([8a0ce59](https://github.com/Indemnity83/logistics/commit/8a0ce59060d8f603d7aef35ab00da3ab363bd767))


### Testing

* add baseline coverage statistics and tests for components ([#402](https://github.com/Indemnity83/logistics/issues/402)) ([9de08b8](https://github.com/Indemnity83/logistics/commit/9de08b8d316e6abdf69e7f6edcfe1b19b024f8c7))
* add NeoForge ServiceLoader and energy adapter tests ([#377](https://github.com/Indemnity83/logistics/issues/377)) ([a4e7ec1](https://github.com/Indemnity83/logistics/commit/a4e7ec117aac2947d43484012aafdb7e53c08532))
* establish test infrastructure for fabric/neoforge and expand common coverage ([93d7f9d](https://github.com/Indemnity83/logistics/commit/93d7f9dc14592a56938ed8e0aaff574b15072a27))


### Build System

* add NeoForge support to multi-loader configuration ([#320](https://github.com/Indemnity83/logistics/issues/320)) ([98082b0](https://github.com/Indemnity83/logistics/commit/98082b0b80bb6b90f9982767be41e3bfc5229ff0))
* restructure project to support multiloader architecture ([#318](https://github.com/Indemnity83/logistics/issues/318)) ([1663b4f](https://github.com/Indemnity83/logistics/commit/1663b4f423595226f06054870ae61406d78d66df))

## [0.5.6](https://github.com/Indemnity83/logistics/compare/mc1.21.11-v0.5.5...mc1.21.11-v0.5.6) (2026-05-04)


### Bug Fixes

* correct lever and dust placement on engine blocks ([#300](https://github.com/Indemnity83/logistics/issues/300)) ([8add73b](https://github.com/Indemnity83/logistics/commit/8add73b4632ad827c2986de97d5d27d625844d39))
* correct supplier module UI targeting ([#295](https://github.com/Indemnity83/logistics/issues/295)) ([321ae4f](https://github.com/Indemnity83/logistics/commit/321ae4f8f0ec409bde2e2d208c073137477edd16))
* enchanted items being provided on the network ([#292](https://github.com/Indemnity83/logistics/issues/292)) ([b852306](https://github.com/Indemnity83/logistics/commit/b85230690a774c7c3e5aee9425e0adf1d7d5c7b9))
* prevent component-bearing items from sharing crafter slot ([#302](https://github.com/Indemnity83/logistics/issues/302)) ([95785ca](https://github.com/Indemnity83/logistics/commit/95785cacefc2d1b9aebc7da98fba7cd265562c07))
* prevent passive supplier from overfilling inventory items ([#303](https://github.com/Indemnity83/logistics/issues/303)) ([0cdc2d9](https://github.com/Indemnity83/logistics/commit/0cdc2d90461f08c797b85bc73a826d2407e7d1c1))
* relocate quartz crystal assets to core directory ([#311](https://github.com/Indemnity83/logistics/issues/311)) ([b03a1ce](https://github.com/Indemnity83/logistics/commit/b03a1ce091e781478f9cdfca2ce01a12c5ff3d8f))
* tooltips for items in requester screen ([#294](https://github.com/Indemnity83/logistics/issues/294)) ([3e184bf](https://github.com/Indemnity83/logistics/commit/3e184bf8c005272434bd6d1b25a9c6fec8e86dbb))


### Refactorings

* move quartz crystal registration to core domain ([#304](https://github.com/Indemnity83/logistics/issues/304)) ([89a28f8](https://github.com/Indemnity83/logistics/commit/89a28f8f0fd3ccd2f3395c9c38aad47f7a0366e0))

## [0.5.5](https://github.com/Indemnity83/logistics/compare/mc1.21.11-v0.5.4...mc1.21.11-v0.5.5) (2026-04-25)


### Bug Fixes

* resolve vanishing filters in diamond pipes serialization ([#288](https://github.com/Indemnity83/logistics/issues/288)) ([b16164e](https://github.com/Indemnity83/logistics/commit/b16164e439d5c92e67d707e0f3ec13f373e8cf5d))

## [0.5.4](https://github.com/Indemnity83/logistics/compare/mc1.21.11-v0.5.3...mc1.21.11-v0.5.4) (2026-04-22)


### Bug Fixes

* add ender dust macerator recipe ([#282](https://github.com/Indemnity83/logistics/issues/282)) ([3c7baad](https://github.com/Indemnity83/logistics/commit/3c7baad1b45369e6be48bb5df2a45e8d1346bc63))
* use RegistryOps to fix enchanted item crash ([#283](https://github.com/Indemnity83/logistics/issues/283)) ([2a096a9](https://github.com/Indemnity83/logistics/commit/2a096a9467b9a0a2764edc12a6b900e60bd73748))

## [0.5.3](https://github.com/Indemnity83/logistics/compare/mc1.21.11-v0.5.2...mc1.21.11-v0.5.3) (2026-04-14)


### Bug Fixes

* add newline at end of fabric.mod.json file ([f59c4ba](https://github.com/Indemnity83/logistics/commit/f59c4bac3f6fd2ba318c1a8a2c6b4906ccc3e589))
* add outline rendering for quarry area placement ([#274](https://github.com/Indemnity83/logistics/issues/274)) ([1f10443](https://github.com/Indemnity83/logistics/commit/1f10443273e4b9747377d7360bd10f64a11d5d08))
* expose laser quarry configuration settings to user ([#277](https://github.com/Indemnity83/logistics/issues/277)) ([4769c3e](https://github.com/Indemnity83/logistics/commit/4769c3ed49ae23f9de8edbb6cc371b8ad266ba34))

## [0.5.2](https://github.com/Indemnity83/logistics/compare/mc1.21.11-v0.5.1...mc1.21.11-v0.5.2) (2026-04-10)


### Bug Fixes

* resolve pipe network registration issues on load ([#269](https://github.com/Indemnity83/logistics/issues/269)) ([bf641d5](https://github.com/Indemnity83/logistics/commit/bf641d517b0ed9b8cd4caf14284d29765d3d8df6))

## [0.5.1](https://github.com/Indemnity83/logistics/compare/mc1.21.11-v0.5.0...mc1.21.11-v0.5.1) (2026-04-06)


### Bug Fixes

* add translations for tin and bronze item tags ([#257](https://github.com/Indemnity83/logistics/issues/257)) ([df1a70e](https://github.com/Indemnity83/logistics/commit/df1a70e97bd120bcc8331fcafb401edb7ec6a53a))
* normalize laser quarry recipe to use machine core ([#267](https://github.com/Indemnity83/logistics/issues/267)) ([30cec9f](https://github.com/Indemnity83/logistics/commit/30cec9f6901b671f471adb27392a9865979ebf50))
* remove orphaned advanced extractor module assets and update JEI entrypoint ([#256](https://github.com/Indemnity83/logistics/issues/256)) ([4e563f2](https://github.com/Indemnity83/logistics/commit/4e563f29c753f37c3b995bf3e0a5ac1e5082c9f7))
* rename MACHINE_FRAME to MACHINE_CORE and update assets ([#265](https://github.com/Indemnity83/logistics/issues/265)) ([03063bc](https://github.com/Indemnity83/logistics/commit/03063bc04244ba89fa1f014cd494ef19c5ab042c))
* update macerator tags for mining and loot table renaming ([#266](https://github.com/Indemnity83/logistics/issues/266)) ([c70c6c2](https://github.com/Indemnity83/logistics/commit/c70c6c2ae58029b5bcefb8355a1801f9bd4398dc))

## [0.5.0](https://github.com/Indemnity83/logistics/compare/mc1.21.11-v0.4.0...mc1.21.11-v0.5.0) (2026-04-04)


### ⚠ BREAKING CHANGES

* logistics pipe and module crafting recipes have changed; enable the "Classic Logistics Pipes crafting recipes" built-in datapack to restore the original gear-based recipes as alternates

### Features

* add macerator machine with grinding time, XP drops, dust and flour outputs, in-game recipe book, and ingredient tag support ([#230](https://github.com/Indemnity83/logistics/issues/230), [#238](https://github.com/Indemnity83/logistics/issues/238), [#239](https://github.com/Indemnity83/logistics/issues/239), [#243](https://github.com/Indemnity83/logistics/issues/243), [#244](https://github.com/Indemnity83/logistics/issues/244), [#251](https://github.com/Indemnity83/logistics/issues/251)) ([b351be7](https://github.com/Indemnity83/logistics/commit/b351be73982e99e27b9aed01bad89d0ae312f790), [39126fa](https://github.com/Indemnity83/logistics/commit/39126faf97217471968c4b41b570fa87b815619c), [7c017f0](https://github.com/Indemnity83/logistics/commit/7c017f01f1052632d1afbc8a6f98d333b7467b1b), [f87ac33](https://github.com/Indemnity83/logistics/commit/f87ac33366fb37909d6e06714507fa5022bd912f), [ae5777a](https://github.com/Indemnity83/logistics/commit/ae5777a08386edd1af0db9b5cdf17df2f6e52c13), [d8f60b1](https://github.com/Indemnity83/logistics/commit/d8f60b121cd042d840c3a442793910c9d86a06d2))
* add wooden valve, automation cores, and chip crafting recipes ([#235](https://github.com/Indemnity83/logistics/issues/235), [#240](https://github.com/Indemnity83/logistics/issues/240)) ([419a685](https://github.com/Indemnity83/logistics/commit/419a68553999d0662785feee4667c4942f9d967a), [fd963e4](https://github.com/Indemnity83/logistics/commit/fd963e457e21136df83ec1146dbe11c7b5369831))
* add JEI support for custom machines ([#234](https://github.com/Indemnity83/logistics/issues/234)) ([78a148e](https://github.com/Indemnity83/logistics/commit/78a148e25385bccf1fd89ce0a72cce14180018f5))
* improve Kiln with crafting table valve recipes and in-game recipe book ([#233](https://github.com/Indemnity83/logistics/issues/233), [#242](https://github.com/Indemnity83/logistics/issues/242)) ([22a9de3](https://github.com/Indemnity83/logistics/commit/22a9de372c0187936dcb0c62cd2780d739ba251a), [6ea5144](https://github.com/Indemnity83/logistics/commit/6ea5144726632f0d6ab06428d7fd0075bfee73ad))
* rework pipe and module recipes ([05dfec5](https://github.com/Indemnity83/logistics/commit/05dfec576da896d88e730bbac1d4cd2edc579d0b))


### Bug Fixes

* fix pack.mcmeta format fields for recipe datapack compatibility ([#250](https://github.com/Indemnity83/logistics/issues/250), [#252](https://github.com/Indemnity83/logistics/issues/252)) ([45eb90a](https://github.com/Indemnity83/logistics/commit/45eb90a579c2e3cd2cbc7d4ba36b924e4111589d), [29425ed](https://github.com/Indemnity83/logistics/commit/29425eda997f47c9716a4e20a4076e4cffe520ab))
* resolve pipe access bug for satellite and process pipes ([#231](https://github.com/Indemnity83/logistics/issues/231)) ([91ef7c9](https://github.com/Indemnity83/logistics/commit/91ef7c990f97e5a8ec86d40e7e18836a725c084d))
* update build script to check Gradle task by MC version ([#249](https://github.com/Indemnity83/logistics/issues/249)) ([fe8336d](https://github.com/Indemnity83/logistics/commit/fe8336d28128953051ba513dd4d39f45b32c3f16))

## [0.4.0](https://github.com/Indemnity83/logistics/compare/mc1.21.11-v0.3.5...mc1.21.11-v0.4.0) (2026-03-27)


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

## [0.3.5](https://github.com/Indemnity83/logistics/compare/mc1.21.11-v0.3.4...mc1.21.11-v0.3.5) (2026-02-27)


### Bug Fixes

* restore crafting and kiln recipe loading by updating recipe data paths ([#152](https://github.com/Indemnity83/logistics/issues/152)) ([77e83c8](https://github.com/Indemnity83/logistics/commit/77e83c8343307bea9a6bccee15f71324e3dcdcbc))
* standardize internal pipe item drops via PipeBlockEntity helper ([#149](https://github.com/Indemnity83/logistics/issues/149)) ([5ca7755](https://github.com/Indemnity83/logistics/commit/5ca77556a38864e0ecb3b2593cfe15a550fb756e))
* update Gradle testLogging exceptionFormat syntax ([#147](https://github.com/Indemnity83/logistics/issues/147)) ([f25f8ad](https://github.com/Indemnity83/logistics/commit/f25f8ad5b0e004e02edd40bfdc7aac6c77cbdc37))


### Performance

* avoid per-tick pipe connection cache recalculation ([#143](https://github.com/Indemnity83/logistics/issues/143)) ([b076f8c](https://github.com/Indemnity83/logistics/commit/b076f8c974e31dce1222c14d015929764b222dae))
* gate pipe connection updates behind cache dirty flag ([#150](https://github.com/Indemnity83/logistics/issues/150)) ([cffb4f0](https://github.com/Indemnity83/logistics/commit/cffb4f061c2f64bc71367de471f6842b633aca02))

## [0.3.4](https://github.com/Indemnity83/logistics/compare/mc1.21.11-v0.3.3...mc1.21.11-v0.3.4) (2026-02-25)


### Features

* add kiln machine with molten glass, valve recipes, and shared heat component ([#131](https://github.com/Indemnity83/logistics/issues/131)) ([2e96ed8](https://github.com/Indemnity83/logistics/commit/2e96ed83c0c60f82e315ddc7d3534dadba6dea8e))
* add ResourceId compatibility layer and migrate identifier usage ([#136](https://github.com/Indemnity83/logistics/issues/136)) ([bb97139](https://github.com/Indemnity83/logistics/commit/bb97139fa6306ff910e4eb93ad5dae223c5777bb))


### Bug Fixes

* kiln recipe loading ([758ffbd](https://github.com/Indemnity83/logistics/commit/758ffbd3efab51278e7bc2287554b22a3233ef36))
* properly set sided inventory for engines and kiln ([#138](https://github.com/Indemnity83/logistics/issues/138)) ([933bd0e](https://github.com/Indemnity83/logistics/commit/933bd0e9cfef99aebcba1bdd3e80dadc27934035))

## [0.3.3](https://github.com/Indemnity83/logistics/compare/mc1.21.11-v0.3.2...mc1.21.11-v0.3.3) (2026-02-22)


### Bug Fixes

* correct vertical centering for block items rendered inside pipes ([#113](https://github.com/Indemnity83/logistics/issues/113)) ([ab2bcb8](https://github.com/Indemnity83/logistics/commit/ab2bcb870745c73e0c94e33c4017194292bf57eb))
* register mod content during common initialization ([#132](https://github.com/Indemnity83/logistics/issues/132)) ([3855965](https://github.com/Indemnity83/logistics/commit/3855965915f305c9fa3c3419b71b7213a5a967b0))

## [0.3.2](https://github.com/Indemnity83/logistics/compare/mc1.21.11-v0.3.1...mc1.21.11-v0.3.2) (2026-02-17)


### Bug Fixes

* migrate tin/bronze recipes to c: tags and drop apatite tag entries ([#110](https://github.com/Indemnity83/logistics/issues/110)) ([dd4a564](https://github.com/Indemnity83/logistics/commit/dd4a564a8b9e8f340bb93a0d2375d782483e4bb7))
* normalize engine rendering with extra models, cutout layer, and heat tints ([#106](https://github.com/Indemnity83/logistics/issues/106)) ([317f9a7](https://github.com/Indemnity83/logistics/commit/317f9a7737d7f0d47fbbe43c3b452145cb9336f3))
* restore non-overheating engine warm flash and scale piston speed by heat ([#111](https://github.com/Indemnity83/logistics/issues/111)) ([1b909c6](https://github.com/Indemnity83/logistics/commit/1b909c67ec35279887b4b3f648e963ec7406f90b))

## [0.3.1](https://github.com/Indemnity83/logistics/compare/mc1.21.11-v0.3.0...mc1.21.11-v0.3.1) (2026-02-16)


### Features

* add tin, bronze, and apatite materials with worldgen and progression ([#100](https://github.com/Indemnity83/logistics/issues/100)) ([cdd2341](https://github.com/Indemnity83/logistics/commit/cdd234100d125dfd02a047b58b9f92b214ea5fd7))
* refresh gear textures and update creative tab ([#91](https://github.com/Indemnity83/logistics/issues/91)) ([516c75f](https://github.com/Indemnity83/logistics/commit/516c75f1e1d25bc42eefb13af054a75108220dc4))


### Bug Fixes

* correct engine blockstates, particles, and drops ([#86](https://github.com/Indemnity83/logistics/issues/86)) ([b39c296](https://github.com/Indemnity83/logistics/commit/b39c2960b0d9a9b58608526a2df71414d1501a41))
* correct laser quarry LED working state and screen rotation ([#99](https://github.com/Indemnity83/logistics/issues/99)) ([a51c360](https://github.com/Indemnity83/logistics/commit/a51c3606b3c5803818784b008060d75dcf68803c))

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
