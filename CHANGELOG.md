# Changelog

## [0.6.0](https://github.com/Indemnity83/logistics/compare/mc1.21.1-v0.5.6...mc1.21.1-v0.6.0) (2026-05-31)


### Features

* add NeoForge client rendering support ([#381](https://github.com/Indemnity83/logistics/issues/381)) ([82d96f0](https://github.com/Indemnity83/logistics/commit/82d96f0b43c8e5f29a02f9a3c2659a6f62dab5b2))
* add NeoForge platform SPI services ([#378](https://github.com/Indemnity83/logistics/issues/378)) ([8baeae3](https://github.com/Indemnity83/logistics/commit/8baeae3a502fa4640a3bd0981d6b455daf9cc652))
* add power cables ([f83a857](https://github.com/Indemnity83/logistics/commit/f83a857e044f217ef2cc4a264ce09ea22ae022a6))
* wire NeoForge networking and lifecycle ([#380](https://github.com/Indemnity83/logistics/issues/380)) ([a87d87d](https://github.com/Indemnity83/logistics/commit/a87d87db3c079b43a56cdb279b9e727d6c0ed6c0))


### Bug Fixes

* add validation and sanitization for logistics config fields ([#398](https://github.com/Indemnity83/logistics/issues/398)) ([9107753](https://github.com/Indemnity83/logistics/commit/91077539c63847e669d7c5f57244f93180a8ef44))
* clamp energy values and transfer amounts to non-negative ([#399](https://github.com/Indemnity83/logistics/issues/399)) ([e1e927e](https://github.com/Indemnity83/logistics/commit/e1e927e563ed22e685bb34903b7dc5199fc7ff7d))
* correct NeoForge reload listener event for mc/1.21.1 ([#366](https://github.com/Indemnity83/logistics/issues/366)) ([6c829c7](https://github.com/Indemnity83/logistics/commit/6c829c7598283e1edc3ca318e60954c812eb0ed5))
* correct power cable compilation errors for mc/1.21.1 ([#359](https://github.com/Indemnity83/logistics/issues/359)) ([425a496](https://github.com/Indemnity83/logistics/commit/425a496631f8616915955a5af3904c71d6a8b504))
* handle failed deliveries in logistics network tracking ([#397](https://github.com/Indemnity83/logistics/issues/397)) ([e4dfc32](https://github.com/Indemnity83/logistics/commit/e4dfc32eccddfa6736270f3d36498aadeb4d3673))
* preserve item components in filter pipe slots across save/reload ([#386](https://github.com/Indemnity83/logistics/issues/386)) ([dbaafd0](https://github.com/Indemnity83/logistics/commit/dbaafd00d08aa494a89bd2debf30f9f2d197acc1))
* show Macerator recipe category in JEI on NeoForge ([#390](https://github.com/Indemnity83/logistics/issues/390)) ([cecd2c6](https://github.com/Indemnity83/logistics/commit/cecd2c6ec31dd0418ed81b27932cf50d0bd763c9))
* split loader-specific marking fluid recipes ([#382](https://github.com/Indemnity83/logistics/issues/382)) ([f4ac3c6](https://github.com/Indemnity83/logistics/commit/f4ac3c6cf88925dac2b8cb06de03aaff8b7c4522))
* track META-INF service files blocked by overly broad gitignore ([0897442](https://github.com/Indemnity83/logistics/commit/089744217d8b28ff08d591e5a785d420a45f260f))


### Refactorings

* Add NeoForge groundwork and decouple bootstrap initialization ([#306](https://github.com/Indemnity83/logistics/issues/306)) ([7531b52](https://github.com/Indemnity83/logistics/commit/7531b52a5521c7399bd855aadc36368677a81b6e))
* implement ISlottedItemStorage in Container and NeoForge storage ([#400](https://github.com/Indemnity83/logistics/issues/400)) ([c7efb50](https://github.com/Indemnity83/logistics/commit/c7efb50c715f41721c1087696ba68ba5bbe6d12b))
* implement ItemMatcher utility for filtering logic ([#389](https://github.com/Indemnity83/logistics/issues/389)) ([649efa2](https://github.com/Indemnity83/logistics/commit/649efa23dd0571f4bf7b459c8965ae67b4288049))
* introduce ClientModelRegistry SPI to abstract Fabric model loading ([#356](https://github.com/Indemnity83/logistics/issues/356)) ([4ec2184](https://github.com/Indemnity83/logistics/commit/4ec2184d3eca869f499be348408cac37d982875d))
* introduce FuelHelper interface for fuel value management ([#364](https://github.com/Indemnity83/logistics/issues/364)) ([e80397f](https://github.com/Indemnity83/logistics/commit/e80397fb315a107bed268a2cd4a8e2c0aab3ce6b))
* introduce loader-agnostic IEnergyStorage ([#340](https://github.com/Indemnity83/logistics/issues/340)) ([044c13e](https://github.com/Indemnity83/logistics/commit/044c13e76f720ab4037b6191de6f9eed479a11f7))
* introduce loader-agnostic IFluidStorage contract ([#351](https://github.com/Indemnity83/logistics/issues/351)) ([74ea422](https://github.com/Indemnity83/logistics/commit/74ea42208e557a23274f78faa61a9e87d3427f07))
* introduce loader-agnostic IItemStorage contract ([#349](https://github.com/Indemnity83/logistics/issues/349)) ([9309960](https://github.com/Indemnity83/logistics/commit/93099609e69497f0d462524a707cb918f749d785))
* move capability lookup registrations to fabric module ([#343](https://github.com/Indemnity83/logistics/issues/343)) ([e750741](https://github.com/Indemnity83/logistics/commit/e75074198bfc8b0fc72fba3ff59d40ab4ca4d5bb))
* move creative tab and biome registration to Fabric module ([#344](https://github.com/Indemnity83/logistics/issues/344)) ([891e7ee](https://github.com/Indemnity83/logistics/commit/891e7ee432581fc0776e231739884a902d51865a))
* move fabric event registrations out of common module ([#341](https://github.com/Indemnity83/logistics/issues/341)) ([5892e0d](https://github.com/Indemnity83/logistics/commit/5892e0db4c4aa7c2c00009b1a6567af410efe613))
* move packet registration out of common module ([#342](https://github.com/Indemnity83/logistics/issues/342)) ([b815a18](https://github.com/Indemnity83/logistics/commit/b815a18d7925131e38bacb2bd0054ae6e35a0c56))
* remove fabric-api from common compile classpath ([#361](https://github.com/Indemnity83/logistics/issues/361)) ([b8bf7aa](https://github.com/Indemnity83/logistics/commit/b8bf7aa165a1b955f360d2d8c8ce4a8a007f4f7d))
* remove last Fabric API imports from common/ ([#352](https://github.com/Indemnity83/logistics/issues/352)) ([a21cc3b](https://github.com/Indemnity83/logistics/commit/a21cc3bf3d9d1ad72f03723eded8f07364a43259))
* reorganize core.lib sub-packages ([#350](https://github.com/Indemnity83/logistics/issues/350)) ([280f181](https://github.com/Indemnity83/logistics/commit/280f18126731506a6ac47f6e22d147389f4ecc2c))
* simplify block entity registration process with factories ([#345](https://github.com/Indemnity83/logistics/issues/345)) ([b1c77da](https://github.com/Indemnity83/logistics/commit/b1c77daf00d46cf638741a3cd81b6e6a6c6107d7))
* split vanilla client code into common/src/client/ for multi-loader support ([#355](https://github.com/Indemnity83/logistics/issues/355)) ([b7f1b31](https://github.com/Indemnity83/logistics/commit/b7f1b31ab289f9450e2124643641c9b40d8d63ec))
* streamline bootstrap classes to registration-only responsibilities ([#348](https://github.com/Indemnity83/logistics/issues/348)) ([a49b867](https://github.com/Indemnity83/logistics/commit/a49b8679a4892f0514d9ed72417ffc1fd7060eaf))
* unify platform access with service implementations ([#347](https://github.com/Indemnity83/logistics/issues/347)) ([a8888f3](https://github.com/Indemnity83/logistics/commit/a8888f3bbeb13f8b0efb77318a58f0eba473c195))
* wire neoforge subproject to compile against common sources ([#360](https://github.com/Indemnity83/logistics/issues/360)) ([8607789](https://github.com/Indemnity83/logistics/commit/8607789d2856acde05926428649d9f4c1e97de25))


### Testing

* add baseline coverage statistics and tests for components ([#402](https://github.com/Indemnity83/logistics/issues/402)) ([84ac559](https://github.com/Indemnity83/logistics/commit/84ac559c897d01f3965ade949034115bd61c6345))
* add NeoForge ServiceLoader and energy adapter tests ([#377](https://github.com/Indemnity83/logistics/issues/377)) ([ee20dfc](https://github.com/Indemnity83/logistics/commit/ee20dfc66d0f0823485189f072ce465816337109))


### Build System

* add NeoForge support to multi-loader configuration ([#320](https://github.com/Indemnity83/logistics/issues/320)) ([1eba7e5](https://github.com/Indemnity83/logistics/commit/1eba7e5e20cf62187a4bb75d81a66770bfa78348))
* restructure project to support multiloader architecture ([#318](https://github.com/Indemnity83/logistics/issues/318)) ([4c7a060](https://github.com/Indemnity83/logistics/commit/4c7a060178a8ef65c5ce0d31667155fef0de3198))

## [0.5.6](https://github.com/Indemnity83/logistics/compare/mc1.21.1-v0.5.5...mc1.21.1-v0.5.6) (2026-05-04)


### Bug Fixes

* correct lever and dust placement on engine blocks ([#300](https://github.com/Indemnity83/logistics/issues/300)) ([62efa31](https://github.com/Indemnity83/logistics/commit/62efa31eec8729263fd8a031ac279ef4482298f1))
* correct supplier module UI targeting ([#295](https://github.com/Indemnity83/logistics/issues/295)) ([67b43a5](https://github.com/Indemnity83/logistics/commit/67b43a5fafe8966f5d6cbce2aa9b92f863ed2b5c))
* enchanted items being provided on the network ([#292](https://github.com/Indemnity83/logistics/issues/292)) ([650262f](https://github.com/Indemnity83/logistics/commit/650262feae06d5b35ef301e9ce6bb19b6e2943b8))
* prevent component-bearing items from sharing crafter slot ([#302](https://github.com/Indemnity83/logistics/issues/302)) ([792aa10](https://github.com/Indemnity83/logistics/commit/792aa1074a9624dcc599b8b0162316abf230f6b7))
* prevent passive supplier from overfilling inventory items ([#303](https://github.com/Indemnity83/logistics/issues/303)) ([984a9e3](https://github.com/Indemnity83/logistics/commit/984a9e33496263ed37a42635e39130c2d5dd9dd0))
* relocate quartz crystal assets to core directory ([#311](https://github.com/Indemnity83/logistics/issues/311)) ([6d5f3b4](https://github.com/Indemnity83/logistics/commit/6d5f3b418800786ba344f0e99dd896cbb85c3524))
* tooltips for items in requester screen ([#294](https://github.com/Indemnity83/logistics/issues/294)) ([5664d1f](https://github.com/Indemnity83/logistics/commit/5664d1f21c1a7416eafe8657ddbc178e241f3a28))


### Refactorings

* move quartz crystal registration to core domain ([#304](https://github.com/Indemnity83/logistics/issues/304)) ([62c7aaf](https://github.com/Indemnity83/logistics/commit/62c7aafc7fa9bb0f88a4cea8a498f4c37274187f))

## [0.5.5](https://github.com/Indemnity83/logistics/compare/mc1.21.1-v0.5.4...mc1.21.1-v0.5.5) (2026-04-25)


### Bug Fixes

* resolve vanishing filters in diamond pipes serialization ([#288](https://github.com/Indemnity83/logistics/issues/288)) ([9bc893e](https://github.com/Indemnity83/logistics/commit/9bc893e30c1791b6f5f2ed06c2dc6bc6d0211808))

## [0.5.4](https://github.com/Indemnity83/logistics/compare/mc1.21.1-v0.5.3...mc1.21.1-v0.5.4) (2026-04-22)


### Bug Fixes

* add ender dust macerator recipe ([#282](https://github.com/Indemnity83/logistics/issues/282)) ([a82b1c2](https://github.com/Indemnity83/logistics/commit/a82b1c2a76d786bb5e4fbe3658a4a94a862a0e65))
* use RegistryOps to fix enchanted item crash ([#283](https://github.com/Indemnity83/logistics/issues/283)) ([b9b12be](https://github.com/Indemnity83/logistics/commit/b9b12bed1a1f492832025b437de1eff8cc4d8269))

## [0.5.3](https://github.com/Indemnity83/logistics/compare/mc1.21.1-v0.5.2...mc1.21.1-v0.5.3) (2026-04-14)


### Bug Fixes

* add outline rendering for quarry area placement ([#274](https://github.com/Indemnity83/logistics/issues/274)) ([6601a74](https://github.com/Indemnity83/logistics/commit/6601a741d9ea6ac492cddb672a058ffc1ae000ed))
* expose laser quarry configuration settings to user ([#277](https://github.com/Indemnity83/logistics/issues/277)) ([4b8aad8](https://github.com/Indemnity83/logistics/commit/4b8aad8b8b8ca60668fffbf5d11d44350b018f19))

## [0.5.2](https://github.com/Indemnity83/logistics/compare/mc1.21.1-v0.5.1...mc1.21.1-v0.5.2) (2026-04-10)


### Bug Fixes

* resolve pipe network registration issues on load ([#269](https://github.com/Indemnity83/logistics/issues/269)) ([4b32de3](https://github.com/Indemnity83/logistics/commit/4b32de3e24b69ca7b1b7524a02b95c7f88b2b02e))

## [0.5.1](https://github.com/Indemnity83/logistics/compare/mc1.21.1-v0.5.0...mc1.21.1-v0.5.1) (2026-04-06)


### Bug Fixes

* add translations for tin and bronze item tags ([#257](https://github.com/Indemnity83/logistics/issues/257)) ([f5fecb4](https://github.com/Indemnity83/logistics/commit/f5fecb4452d7f31438e0b828ac829927560113ae))
* normalize laser quarry recipe to use machine core ([#267](https://github.com/Indemnity83/logistics/issues/267)) ([67485e0](https://github.com/Indemnity83/logistics/commit/67485e00d8518dfad1ec2cfa1daf3895f2d737e3))
* remove orphaned advanced extractor module assets and update JEI entrypoint ([#256](https://github.com/Indemnity83/logistics/issues/256)) ([86d257e](https://github.com/Indemnity83/logistics/commit/86d257e4ef30a796ea9ecca51a011653937e2e23))
* rename MACHINE_FRAME to MACHINE_CORE and update assets ([#265](https://github.com/Indemnity83/logistics/issues/265)) ([50b96ca](https://github.com/Indemnity83/logistics/commit/50b96cac1808c12bbafd0be793543ad931948e70))
* update item references in machine_core recipe JSON ([f72fae2](https://github.com/Indemnity83/logistics/commit/f72fae2304d5b1ae5bd8fa78fcd3f03b066f3356))
* update macerator tags for mining and loot table renaming ([#266](https://github.com/Indemnity83/logistics/issues/266)) ([ae7c592](https://github.com/Indemnity83/logistics/commit/ae7c59222c95075941aae5acf19d8c882cf66bfa))

## [0.5.0](https://github.com/Indemnity83/logistics/compare/mc1.21.1-v0.4.0...mc1.21.1-v0.5.0) (2026-04-04)


### ⚠ BREAKING CHANGES

* logistics pipe and module crafting recipes have changed; enable the "Classic Logistics Pipes crafting recipes" built-in datapack to restore the original gear-based recipes as alternates

### Features

* add macerator machine with grinding time, XP drops, dust and flour outputs, and ingredient tag support ([#230](https://github.com/Indemnity83/logistics/issues/230), [#238](https://github.com/Indemnity83/logistics/issues/238), [#239](https://github.com/Indemnity83/logistics/issues/239), [#244](https://github.com/Indemnity83/logistics/issues/244), [#251](https://github.com/Indemnity83/logistics/issues/251)) ([a33a1f0](https://github.com/Indemnity83/logistics/commit/a33a1f0fda542a19878471e67ff3a695956afaba), [89439e6](https://github.com/Indemnity83/logistics/commit/89439e63704de06ad624892fbbc60beafdcbe8b1), [403b672](https://github.com/Indemnity83/logistics/commit/403b67273351b1d24b05cb7d6c782bb0a6178fb9), [25b76a5](https://github.com/Indemnity83/logistics/commit/25b76a5b730f4ac5d6848c9508ff8dd171382739), [3c304d4](https://github.com/Indemnity83/logistics/commit/3c304d4068cc20769bcac0299d4c2189373dbd1f))
* add JEI support for custom machines ([#234](https://github.com/Indemnity83/logistics/issues/234)) ([8b8f2cd](https://github.com/Indemnity83/logistics/commit/8b8f2cd1e95b4b7025c0748c7bcf950688402a6f))
* add wooden valve and new automation cores ([#235](https://github.com/Indemnity83/logistics/issues/235)) ([79e6044](https://github.com/Indemnity83/logistics/commit/79e6044c0a7167121865b7e6324fa6287c5e9765))
* improve Kiln with crafting table valve recipes and in-game recipe book ([#233](https://github.com/Indemnity83/logistics/issues/233), [#242](https://github.com/Indemnity83/logistics/issues/242)) ([ade8f4e](https://github.com/Indemnity83/logistics/commit/ade8f4ed0084e145bf482eb89b42fef13b22797c), [d1ae352](https://github.com/Indemnity83/logistics/commit/d1ae352e07205e8876d6a0c010fe50a497500fc6))
* rework pipe and module recipes ([19c9cd3](https://github.com/Indemnity83/logistics/commit/19c9cd32e26df7a6ec69637c0ca480ab9bed825f))


### Bug Fixes

* add min_format and max_format to pack.mcmeta for recipes ([#250](https://github.com/Indemnity83/logistics/issues/250)) ([6783b99](https://github.com/Indemnity83/logistics/commit/6783b99b8b2ab19653be06043197e67efec7a83b))
* compatability with minecraft 1.21.1 ([88c2bc7](https://github.com/Indemnity83/logistics/commit/88c2bc755ea7863c38e1493e66b7ae614c122471))
* resolve pipe access bug for satellite and process pipes ([#231](https://github.com/Indemnity83/logistics/issues/231)) ([2657e4b](https://github.com/Indemnity83/logistics/commit/2657e4b41b867a3e4ac7186f1811f241bedb31c1))
* update build script to check Gradle task by MC version ([#249](https://github.com/Indemnity83/logistics/issues/249)) ([05ff265](https://github.com/Indemnity83/logistics/commit/05ff265e2bdc8ed1c8381604be7ac66039993cbe))

## [0.4.0](https://github.com/Indemnity83/logistics/compare/mc1.21.1-v0.3.5...mc1.21.1-v0.4.0) (2026-03-27)


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
