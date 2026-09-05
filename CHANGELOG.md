# Changelog

## [0.8.8](https://github.com/Indemnity83/logistics/compare/mc1.21.11-v0.8.7...mc1.21.11-v0.8.8) (2026-09-05)


### Changed

* **routing:** serve pipe shapes from the connection cache ([de0bd86](https://github.com/Indemnity83/logistics/commit/de0bd86d2885df55f24aba8ff5b94d80b2f26103))


### Removed

* **crafting:** drop the classic crafting resource pack ([c24b160](https://github.com/Indemnity83/logistics/commit/c24b16003244bce76f3c364aea08a40df1f00c34)), closes [#960](https://github.com/Indemnity83/logistics/issues/960)


### Fixed

* **automation:** stop the Alloy Smelter duplicating recipe inputs ([d17f48d](https://github.com/Indemnity83/logistics/commit/d17f48dcfbe725d28cfeff12b548b09e3f35a73f))
* **ci:** stop the release pipeline publishing broken releases ([3aa7a64](https://github.com/Indemnity83/logistics/commit/3aa7a6431f25c7ee8b53f9b1ac4d29d7e19a9e4c))
* **core:** give the Seed Oil Bucket its missing model and texture ([#904](https://github.com/Indemnity83/logistics/issues/904)) ([f30a1d3](https://github.com/Indemnity83/logistics/commit/f30a1d37f19335bab13803b53380964446436882))
* **core:** survive a malformed config file at startup ([0305550](https://github.com/Indemnity83/logistics/commit/03055504816376395e2ba248f35c9ff7e23afbdf))
* **crafting:** keep the sourceable remainder when a request is replanned ([2a98d88](https://github.com/Indemnity83/logistics/commit/2a98d88aa2e13e55fa60fe24dceff7b2d509fa3b))
* **crafting:** order only what was requested from a crafting batch ([2a98d88](https://github.com/Indemnity83/logistics/commit/2a98d88aa2e13e55fa60fe24dceff7b2d509fa3b))
* **energy:** face a newly placed engine at a full machine on NeoForge ([4d4879b](https://github.com/Indemnity83/logistics/commit/4d4879be70e13a181caceb4661261dcefe9587c7)), closes [#977](https://github.com/Indemnity83/logistics/issues/977) [#989](https://github.com/Indemnity83/logistics/issues/989)
* **energy:** give engine and battery models their missing particle texture ([#909](https://github.com/Indemnity83/logistics/issues/909)) ([1bf0684](https://github.com/Indemnity83/logistics/commit/1bf0684b4c1066c70e10732420021a5d8e0657c6))
* **energy:** stop a battery starving its own cable network ([4d4879b](https://github.com/Indemnity83/logistics/commit/4d4879be70e13a181caceb4661261dcefe9587c7))
* **energy:** stop cables voiding power into slow machines ([1a3780e](https://github.com/Indemnity83/logistics/commit/1a3780e9fba6766fe4166d5cfe0f785fd2fc8b28))
* **energy:** stop duplicating and voiding energy on Fabric ([ab6b225](https://github.com/Indemnity83/logistics/commit/ab6b22547d2b06959633e4234eff4384545ead16))
* **fluids:** fade fluid out of a pipe instead of popping it ([efcf709](https://github.com/Indemnity83/logistics/commit/efcf709192c65ba5a50fb64c8f734775a82a24c2))
* **fluids:** keep the fill animation steady at any frame rate ([efcf709](https://github.com/Indemnity83/logistics/commit/efcf709192c65ba5a50fb64c8f734775a82a24c2))
* **pump:** keep long intake tubes visible off screen ([73cac73](https://github.com/Indemnity83/logistics/commit/73cac7353a90d501d85d3140e99e3b1e59244549))
* **pump:** stop the intake tube descending through waterlogged blocks ([cfa4ee3](https://github.com/Indemnity83/logistics/commit/cfa4ee39642f117b56bd4c498d9d9c5f2eea4840)), closes [#968](https://github.com/Indemnity83/logistics/issues/968) [#969](https://github.com/Indemnity83/logistics/issues/969)
* **quarry:** keep marker beams visible off screen ([73cac73](https://github.com/Indemnity83/logistics/commit/73cac7353a90d501d85d3140e99e3b1e59244549)), closes [#940](https://github.com/Indemnity83/logistics/issues/940)
* **quarry:** keep the frame and laser visible off screen ([73cac73](https://github.com/Indemnity83/logistics/commit/73cac7353a90d501d85d3140e99e3b1e59244549))
* **quarry:** let players break abandoned frames in survival ([2118f45](https://github.com/Indemnity83/logistics/commit/2118f45d5cb060b2cd8b7a7ec00772300af8d65a))
* **quarry:** mine waterlogged blocks instead of skipping them ([cfa4ee3](https://github.com/Indemnity83/logistics/commit/cfa4ee39642f117b56bd4c498d9d9c5f2eea4840))
* **quarry:** remove the unintended duplicate recipe ([81d6e2b](https://github.com/Indemnity83/logistics/commit/81d6e2bc44dc708b62ca9b53bec0156e1bbfdfe4))
* **quarry:** stop a zero arm speed freezing the quarry forever ([a1536c5](https://github.com/Indemnity83/logistics/commit/a1536c526fb8e51fcc1eb0042bb3b2a0a72c23f2))
* **quarry:** stop frame blocks vanishing without warning ([2118f45](https://github.com/Indemnity83/logistics/commit/2118f45d5cb060b2cd8b7a7ec00772300af8d65a))
* **quarry:** stop vacuuming loose items off the ground ([d8bcf4d](https://github.com/Indemnity83/logistics/commit/d8bcf4d465c7c044cb128b297a645453228aa57c)), closes [#973](https://github.com/Indemnity83/logistics/issues/973)
* **routing:** apply one interaction range to every pipe menu ([2afb47f](https://github.com/Indemnity83/logistics/commit/2afb47f567813b5b863ecea4f1b46cd4792aef6b)), closes [#937](https://github.com/Indemnity83/logistics/issues/937) [#942](https://github.com/Indemnity83/logistics/issues/942)
* **routing:** cancel the order behind a job that has finished ([2a98d88](https://github.com/Indemnity83/logistics/commit/2a98d88aa2e13e55fa60fe24dceff7b2d509fa3b)), closes [#933](https://github.com/Indemnity83/logistics/issues/933) [#944](https://github.com/Indemnity83/logistics/issues/944) [#947](https://github.com/Indemnity83/logistics/issues/947)
* **routing:** close module menus when the pipe is broken ([2afb47f](https://github.com/Indemnity83/logistics/commit/2afb47f567813b5b863ecea4f1b46cd4792aef6b))
* **routing:** fall through to the next provider when one is fully reserved ([9b99466](https://github.com/Indemnity83/logistics/commit/9b994667983edddbb002bc28564149635aff0aac)), closes [#928](https://github.com/Indemnity83/logistics/issues/928) [#938](https://github.com/Indemnity83/logistics/issues/938) [#939](https://github.com/Indemnity83/logistics/issues/939)
* **routing:** release only the delivered part of a shipment ([9b99466](https://github.com/Indemnity83/logistics/commit/9b994667983edddbb002bc28564149635aff0aac))
* **routing:** render the right pipe arm after a neighbour changes ([de0bd86](https://github.com/Indemnity83/logistics/commit/de0bd86d2885df55f24aba8ff5b94d80b2f26103))
* **routing:** stop a bad button id crashing the server ([b41652d](https://github.com/Indemnity83/logistics/commit/b41652da833b22e750263f14cecb077906d6eb7e))
* **routing:** stop broadcasting requester contents to every player ([166088a](https://github.com/Indemnity83/logistics/commit/166088a611459b0ad5af278b924db5f27c2b3f6d))
* **routing:** stop failed deliveries shrinking a provider's stock ([9b99466](https://github.com/Indemnity83/logistics/commit/9b994667983edddbb002bc28564149635aff0aac))
* **routing:** stop Providers destroying items on an unpowered network ([55583d5](https://github.com/Indemnity83/logistics/commit/55583d58ee8da6291e0b81b324f755a9496d2e2e))
* **routing:** stop the Provider MkII shipping the wrong item to the next order ([68f4429](https://github.com/Indemnity83/logistics/commit/68f4429278b592e25def0ec66653d4aa95ca4bed))
* **transport:** apply the configured minimum pipe speed ([efcf709](https://github.com/Indemnity83/logistics/commit/efcf709192c65ba5a50fb64c8f734775a82a24c2)), closes [#950](https://github.com/Indemnity83/logistics/issues/950)
* **transport:** stop losing items on a partial pipe handoff ([086f01c](https://github.com/Indemnity83/logistics/commit/086f01c3611e52724fbf40b5bbc2bac3bc40c1c9))

## [0.8.7](https://github.com/Indemnity83/logistics/compare/mc1.21.11-v0.8.6...mc1.21.11-v0.8.7) (2026-09-01)


### Added

* **automation:** add crude oil to petroleum-block transposer recipes ([#851](https://github.com/Indemnity83/logistics/issues/851)) ([cccdadd](https://github.com/Indemnity83/logistics/commit/cccdadd4b04c42119cf06c47ac83e61f7ee8922f))
* **automation:** add RF cost and recipe system to the Transposer ([#829](https://github.com/Indemnity83/logistics/issues/829)) ([c325e9b](https://github.com/Indemnity83/logistics/commit/c325e9b43c963b15c78891a10403a95dcd158fb9))
* **core:** add shared gear recipe tags ([#871](https://github.com/Indemnity83/logistics/issues/871)) ([e43884c](https://github.com/Indemnity83/logistics/commit/e43884c74dc07c720417fad09f617817894f0583))
* **energy:** show engine fuels in the recipe browser ([#886](https://github.com/Indemnity83/logistics/issues/886)) ([0476cda](https://github.com/Indemnity83/logistics/commit/0476cdaec38b01eedab23d7362b778980157568c))
* **fluids:** drain cauldrons with the fluid extractor pipe ([#885](https://github.com/Indemnity83/logistics/issues/885)) ([23b5bad](https://github.com/Indemnity83/logistics/commit/23b5badb5141a103e3c0208e9ad21f082f58047f))


### Changed

* **automation:** restyle the Crucible progress gauge as a droplet ([#834](https://github.com/Indemnity83/logistics/issues/834)) ([a7774b3](https://github.com/Indemnity83/logistics/commit/a7774b3d3db42357daf6d59dcb97835c9c1e713e))


### Fixed

* **automation:** accept raw ore in the quicksilver amalgamation recipes ([#840](https://github.com/Indemnity83/logistics/issues/840)) ([99773ed](https://github.com/Indemnity83/logistics/commit/99773edcdc37ebd9b3dc55f0c7602f70658c3496))
* **automation:** stop refinery shift-click from duplicating items ([#846](https://github.com/Indemnity83/logistics/issues/846)) ([4b72c6f](https://github.com/Indemnity83/logistics/commit/4b72c6f21d866d91702370a93c1e8c92c5b36b4e))
* **compat:** stop Jade plugin error on dedicated servers ([#878](https://github.com/Indemnity83/logistics/issues/878)) ([c32fe49](https://github.com/Indemnity83/logistics/commit/c32fe49a3c74fd1f298ba57eccc09106b0d3b432))
* **energy:** add catalyst engine JEI category ([#874](https://github.com/Indemnity83/logistics/issues/874)) ([bd7569a](https://github.com/Indemnity83/logistics/commit/bd7569aca8ff5ba7019ec3dc5547390cfc20fdec))
* **energy:** show the reaction engine's recipes in JEI on Fabric ([#886](https://github.com/Indemnity83/logistics/issues/886)) ([0476cda](https://github.com/Indemnity83/logistics/commit/0476cdaec38b01eedab23d7362b778980157568c))
* **energy:** stop battery and cable taking forever to mine ([#839](https://github.com/Indemnity83/logistics/issues/839)) ([8c5a8ff](https://github.com/Indemnity83/logistics/commit/8c5a8ff322407c547805e3e94bb6a411694b8ee3))
* **fluids:** honor pipe transfer rates set above the default ([#885](https://github.com/Indemnity83/logistics/issues/885)) ([23b5bad](https://github.com/Indemnity83/logistics/commit/23b5badb5141a103e3c0208e9ad21f082f58047f))
* **fluids:** obscure vision and apply Nausea/Poison/Slowness in Crude Oil ([#848](https://github.com/Indemnity83/logistics/issues/848)) ([155735f](https://github.com/Indemnity83/logistics/commit/155735fefeaf51ed3840b4c51d7167a66826bf39))
* **fluids:** stop over-capacity saved tank amounts from crashing on load ([#849](https://github.com/Indemnity83/logistics/issues/849)) ([2fc0764](https://github.com/Indemnity83/logistics/commit/2fc0764a5e1ed830ac2f92187dadfad9288a2166))
* **macerator:** grind loose raw ore items into dust ([#838](https://github.com/Indemnity83/logistics/issues/838)) ([e7eb896](https://github.com/Indemnity83/logistics/commit/e7eb896858c87c9462a5468069962328167478d8))
* **quarry:** stop the arm mining through lava or ignoring reappeared blocks ([#850](https://github.com/Indemnity83/logistics/issues/850)) ([9aec6fd](https://github.com/Indemnity83/logistics/commit/9aec6fd49d549dd8de8a149cdc1fbbc308b86c06))
* **routing:** treat requester deliveries with no inventory as fulfilled ([#847](https://github.com/Indemnity83/logistics/issues/847)) ([eb8ce68](https://github.com/Indemnity83/logistics/commit/eb8ce68c3f78a89ffdc3d6ae0da3ceef3293a943))

## [0.8.6](https://github.com/Indemnity83/logistics/compare/mc1.21.11-v0.8.5...mc1.21.11-v0.8.6) (2026-08-07)


### Added

* **automation:** add the Transposer ([#792](https://github.com/Indemnity83/logistics/issues/792)) ([07bd037](https://github.com/Indemnity83/logistics/commit/07bd037bf3113df67aca3463960c59d95f03b3c7))
* **fluids:** add a dedicated fluid supplier GUI ([#811](https://github.com/Indemnity83/logistics/issues/811)) ([c7cbcf5](https://github.com/Indemnity83/logistics/commit/c7cbcf5689d46e1886705b5aa3b8080385af9204))
* **fluids:** add fluid provider and supplier chassis modules ([#825](https://github.com/Indemnity83/logistics/issues/825)) ([f84fbca](https://github.com/Indemnity83/logistics/commit/f84fbca02b8b42d5565f115b40007d0fa913f78b))
* **fluids:** add supplier partial/exact and minimum-deficit modes ([#822](https://github.com/Indemnity83/logistics/issues/822)) ([36f0320](https://github.com/Indemnity83/logistics/commit/36f0320a808990799293ce8c46d9b71d3cd18793))
* **fluids:** add the fluid provider pipe ([#790](https://github.com/Indemnity83/logistics/issues/790)) ([2774bb1](https://github.com/Indemnity83/logistics/commit/2774bb14224b7aca32a705af52ffcac748d5b15f))
* **fluids:** add the fluid supplier pipe ([#791](https://github.com/Indemnity83/logistics/issues/791)) ([973e643](https://github.com/Indemnity83/logistics/commit/973e643dd0acceb98287c51d38f571e882e75c48))


### Changed

* **fluids:** switch the fluid packet's frame window to a rectangle ([#823](https://github.com/Indemnity83/logistics/issues/823)) ([2474c31](https://github.com/Indemnity83/logistics/commit/2474c316ec502f89bbf1be4c7cb87533832d5a01))


### Fixed

* **automation:** show the sawmill's real ingredient count in JEI ([#824](https://github.com/Indemnity83/logistics/issues/824)) ([4e26216](https://github.com/Indemnity83/logistics/commit/4e262168b14a3d7a17b1409197aaa6ab1441ffae))
* **automation:** stop the sawmill from silently rejecting seeds ([#824](https://github.com/Indemnity83/logistics/issues/824)) ([4e26216](https://github.com/Indemnity83/logistics/commit/4e262168b14a3d7a17b1409197aaa6ab1441ffae))
* **fluids:** never drop fluid packets on the ground ([#805](https://github.com/Indemnity83/logistics/issues/805)) ([a1bab4a](https://github.com/Indemnity83/logistics/commit/a1bab4ad8126473a18352c5bef27d31e90d80559))
* **fluids:** stop suppliers requesting fluid/items with no room ([#804](https://github.com/Indemnity83/logistics/issues/804)) ([eb93b8c](https://github.com/Indemnity83/logistics/commit/eb93b8ca0ab606e069fa3c7febfc20ebd171f4fb))
* **sawmill:** accept single-item deliveries for batched recipes ([#827](https://github.com/Indemnity83/logistics/issues/827)) ([5a5c425](https://github.com/Indemnity83/logistics/commit/5a5c425d8b8027bcd2590bad1389a22765768e7a))

## [0.8.5](https://github.com/Indemnity83/logistics/compare/mc1.21.11-v0.8.4...mc1.21.11-v0.8.5) (2026-08-03)


### Changed

* **energy:** gate engine harvesting by tool tier ([#784](https://github.com/Indemnity83/logistics/issues/784)) ([39c2299](https://github.com/Indemnity83/logistics/commit/39c2299feb32dbe7a97d934e79ba8d1f05ceed9f))


### Fixed

* **energy:** correct cable connections to engines ([#801](https://github.com/Indemnity83/logistics/issues/801)) ([830e65a](https://github.com/Indemnity83/logistics/commit/830e65a108ca73ef1ab23efe869a62e553490fee))
* **quarry:** fix immediate crash on published Fabric builds ([#798](https://github.com/Indemnity83/logistics/issues/798)) ([36ef445](https://github.com/Indemnity83/logistics/commit/36ef445723d95035102a1c3b7c97ebeb5ca891e8)) — thanks @WerWebWer

### New Contributors

* @WerWebWer made their first contribution in #798

## [0.8.4](https://github.com/Indemnity83/logistics/compare/mc1.21.11-v0.8.3...mc1.21.11-v0.8.4) (2026-07-24)


### Added

* **energy:** add the Fuel Engine ([#759](https://github.com/Indemnity83/logistics/issues/759)) ([b36ae14](https://github.com/Indemnity83/logistics/commit/b36ae146056ccb249c7422815d26a69c45f7ca9c))
* **energy:** add the Magmatic Engine ([#776](https://github.com/Indemnity83/logistics/issues/776)) ([30883c6](https://github.com/Indemnity83/logistics/commit/30883c6f61414984d1b6911e21858e5926285a6e))
* **energy:** add the Reaction Engine ([#777](https://github.com/Indemnity83/logistics/issues/777)) ([4aea1c1](https://github.com/Indemnity83/logistics/commit/4aea1c1d92a1293173d68deb5801e0793bbef19b))
* **energy:** add the Steam Engine ([#765](https://github.com/Indemnity83/logistics/issues/765)) ([b45dc55](https://github.com/Indemnity83/logistics/commit/b45dc55bdc8a59acf8a09612e543b6ce40eae6b6))


### Changed

* **core:** retexture the bronze and tin metal blocks and items ([#766](https://github.com/Indemnity83/logistics/issues/766)) ([4ca4775](https://github.com/Indemnity83/logistics/commit/4ca47752a2df25958f792723349efc75a0f0d7e9))
* **energy:** restyle the Stirling Engine GUI ([#763](https://github.com/Indemnity83/logistics/issues/763)) ([5bc8836](https://github.com/Indemnity83/logistics/commit/5bc8836381cac289ba6fe42003522dadc4e2fb09))
* **fluids:** restyle the fluid pipe textures ([#740](https://github.com/Indemnity83/logistics/issues/740)) ([fe095fd](https://github.com/Indemnity83/logistics/commit/fe095fd9c15f04d1fedd445c292c7e0da04c6080))


### Fixed

* **pipes:** fix missing pixel in logistics power junction animation ([#637](https://github.com/Indemnity83/logistics/issues/637)) ([b5d7c5a](https://github.com/Indemnity83/logistics/commit/b5d7c5aa855d25800912b0c1d72eae59bad1e113))

## [0.8.3](https://github.com/Indemnity83/logistics/compare/mc1.21.11-v0.8.2...mc1.21.11-v0.8.3) (2026-07-20)


### Added

* **automation:** add the refinery ([#714](https://github.com/Indemnity83/logistics/issues/714)) ([6488f4c](https://github.com/Indemnity83/logistics/commit/6488f4c28da10fe8bd37728defba77bf143b0662))
* **automation:** add the sequential fabricator ([#719](https://github.com/Indemnity83/logistics/issues/719)) ([607c3b4](https://github.com/Indemnity83/logistics/commit/607c3b4d6e2cdaa0d4b89387cae1a75866d63aba))
* **automation:** add the sequential fabricator ([#719](https://github.com/Indemnity83/logistics/issues/719)) ([607c3b4](https://github.com/Indemnity83/logistics/commit/607c3b4d6e2cdaa0d4b89387cae1a75866d63aba))
* **automation:** fabricate chipsets in the sequential fabricator ([#720](https://github.com/Indemnity83/logistics/issues/720)) ([8cbff82](https://github.com/Indemnity83/logistics/commit/8cbff82d0554a4dac49ede577681355ef0aaa507))
* **automation:** fabricate chipsets in the sequential fabricator ([#720](https://github.com/Indemnity83/logistics/issues/720)) ([8cbff82](https://github.com/Indemnity83/logistics/commit/8cbff82d0554a4dac49ede577681355ef0aaa507))
* **automation:** make machine tuning configurable per machine ([#713](https://github.com/Indemnity83/logistics/issues/713)) ([08d214b](https://github.com/Indemnity83/logistics/commit/08d214b9add2ae3b1b0897c85e6efa313eba4042))
* **compat:** add Jade HUD support for refinery and sequential fabricator ([#744](https://github.com/Indemnity83/logistics/issues/744)) ([e2dabbe](https://github.com/Indemnity83/logistics/commit/e2dabbea4143e808beebccd5b7a973b8ec4ea624))
* **core:** add a copper nugget ([#722](https://github.com/Indemnity83/logistics/issues/722)) ([4435420](https://github.com/Indemnity83/logistics/commit/4435420cd4c5f618f641b034b7e6d8a289a1f70c))
* **crafting:** add tin, rubber, amethyst, and echo valves ([7536bb9](https://github.com/Indemnity83/logistics/commit/7536bb9ac7055227294225bbe942c64cbcac8135))
* **crafting:** rework the valve lineup with a bench recipe ([#727](https://github.com/Indemnity83/logistics/issues/727)) ([7536bb9](https://github.com/Indemnity83/logistics/commit/7536bb9ac7055227294225bbe942c64cbcac8135))
* **crafting:** show sequential fabricator recipes in JEI ([#745](https://github.com/Indemnity83/logistics/issues/745)) ([c9bbd56](https://github.com/Indemnity83/logistics/commit/c9bbd56129a348f04a1ce0fb1a8ad399140bbc87))
* **energy:** add natural and synthetic polymers for rubber ([#716](https://github.com/Indemnity83/logistics/issues/716)) ([c5d9d87](https://github.com/Indemnity83/logistics/commit/c5d9d8773eadc83ea5c14769fbebe091eefcdd2a))
* **fluids:** add bio fuel and fuel oil fluids ([6488f4c](https://github.com/Indemnity83/logistics/commit/6488f4c28da10fe8bd37728defba77bf143b0662))
* **fluids:** add tar as an alternative fluid pipe sealant ([#715](https://github.com/Indemnity83/logistics/issues/715)) ([67331b7](https://github.com/Indemnity83/logistics/commit/67331b717a24e219ce4c880669ac20e1841f2c82))
* **routing:** add chipset crafting alternatives for pipes and modules ([3095cc7](https://github.com/Indemnity83/logistics/commit/3095cc7def6bccafb114e74a1f25486b9b591399))
* **ui:** split creative menu into domain tabs ([#738](https://github.com/Indemnity83/logistics/issues/738)) ([1def318](https://github.com/Indemnity83/logistics/commit/1def3189a3da5c7aaa9aadbe66ed8a0e0e643c96))


### Changed

* **crafting:** craft valves from quartz, redstone, and a base material ([7536bb9](https://github.com/Indemnity83/logistics/commit/7536bb9ac7055227294225bbe942c64cbcac8135))
* **crafting:** restyle every valve with a distinct electron-tube texture ([7536bb9](https://github.com/Indemnity83/logistics/commit/7536bb9ac7055227294225bbe942c64cbcac8135))
* **macerator:** recycle netherite dust to and from ingots ([#734](https://github.com/Indemnity83/logistics/issues/734)) ([5cbce97](https://github.com/Indemnity83/logistics/commit/5cbce97e6f393042392f629d0f7eb0195c097e99))
* **routing:** rework logistics pipe recipes with chipset alternatives ([#721](https://github.com/Indemnity83/logistics/issues/721)) ([3095cc7](https://github.com/Indemnity83/logistics/commit/3095cc7def6bccafb114e74a1f25486b9b591399))
* **routing:** use a copper nugget in the blank module recipe ([4435420](https://github.com/Indemnity83/logistics/commit/4435420cd4c5f618f641b034b7e6d8a289a1f70c))


### Removed

* **crafting:** drop the wooden and ender valves ([7536bb9](https://github.com/Indemnity83/logistics/commit/7536bb9ac7055227294225bbe942c64cbcac8135))


### Fixed

* **automation:** make the refinery and sequential fabricator harvestable ([e7dfe5c](https://github.com/Indemnity83/logistics/commit/e7dfe5c7731dea79ae33215c766fccc536f07912))
* **automation:** show machine recipes in JEI on multiplayer clients ([#735](https://github.com/Indemnity83/logistics/issues/735)) ([d8c2af7](https://github.com/Indemnity83/logistics/commit/d8c2af724ec5d84992901b95dbf6de3197c93448))
* **compat:** register Crucible with NeoForge Jade plugin ([#744](https://github.com/Indemnity83/logistics/issues/744)) ([e2dabbe](https://github.com/Indemnity83/logistics/commit/e2dabbea4143e808beebccd5b7a973b8ec4ea624))
* **compat:** show values in the Jade HUD instead of only labels ([#761](https://github.com/Indemnity83/logistics/issues/761)) ([8b5cf44](https://github.com/Indemnity83/logistics/commit/8b5cf442c0038f80a6e9d64735841b70b77d98f7))
* **neoforge:** let pipes and cables interact with the refinery and sawmill ([#733](https://github.com/Indemnity83/logistics/issues/733)) ([480ce33](https://github.com/Indemnity83/logistics/commit/480ce333301b3cb17fc321d1350a45fa57922278))

## [0.8.2](https://github.com/Indemnity83/logistics/compare/mc1.21.11-v0.8.1...mc1.21.11-v0.8.2) (2026-07-09)


### Added

* **automation:** add pulped biomass from the sawmill ([#660](https://github.com/Indemnity83/logistics/issues/660)) ([96dc6b6](https://github.com/Indemnity83/logistics/commit/96dc6b6d9d1220036885703015bc19efbbc0b904))
* **automation:** add the alloy smelter ([#656](https://github.com/Indemnity83/logistics/issues/656)) ([749f564](https://github.com/Indemnity83/logistics/commit/749f5640b3ce0061423b6e65f41382c0ab99512e))
* **automation:** add the crucible ([#679](https://github.com/Indemnity83/logistics/issues/679)) ([726217d](https://github.com/Indemnity83/logistics/commit/726217dc6137ca21584e4100c04747d30def3390))
* **fluids:** add custom fluids and buckets for the Magma Crucible ([7b7c8ed](https://github.com/Indemnity83/logistics/commit/7b7c8eda76d1dd4d9b8d9a6782f4f0ec50689595))
* **fluids:** light-emitting fluids glow in pipes and tanks ([#695](https://github.com/Indemnity83/logistics/issues/695)) ([b6fd1e3](https://github.com/Indemnity83/logistics/commit/b6fd1e366e5ea74058016c01dd6e34c58fb04fcb))
* **fluids:** show machine fluid tanks in the Jade HUD ([#703](https://github.com/Indemnity83/logistics/issues/703)) ([525c99f](https://github.com/Indemnity83/logistics/commit/525c99f3f9fcbbf4321e317a4da312c6434e66db))
* **worldgen:** add bog earth and peat fuel ([#658](https://github.com/Indemnity83/logistics/issues/658)) ([44a5401](https://github.com/Indemnity83/logistics/commit/44a5401811364e2964966e7bc989a1042b9ff9d9))
* **worldgen:** add crude oil and the oil chain ([#690](https://github.com/Indemnity83/logistics/issues/690)) ([3a70942](https://github.com/Indemnity83/logistics/commit/3a709423d53da10b5ddd6f3eb42bb1d5b6e34d6a))


### Fixed

* **automation:** reject non-finite recipe experience values ([60f2a66](https://github.com/Indemnity83/logistics/commit/60f2a6663942f6e1f1591b93018b627037414c78))
* **automation:** sync machine progress and energy so bars can't overflow ([#694](https://github.com/Indemnity83/logistics/issues/694)) ([fae9b32](https://github.com/Indemnity83/logistics/commit/fae9b32405c19e07d315043c064bd60c1dd984b6))
* **ci:** stop Sentry release failing on cross-branch set-commits ([#683](https://github.com/Indemnity83/logistics/issues/683)) ([9791421](https://github.com/Indemnity83/logistics/commit/9791421cb32a9942e46b237ffcafac91c84d853f))
* **ci:** unblock release publishing broken by Sentry set-commits ([#678](https://github.com/Indemnity83/logistics/issues/678)) ([56afa20](https://github.com/Indemnity83/logistics/commit/56afa20661c3364e2cf4668522a896d7efa65b46))
* **compat:** add missing Jade fluid-pipe config translation ([#701](https://github.com/Indemnity83/logistics/issues/701)) ([fa24f72](https://github.com/Indemnity83/logistics/commit/fa24f7207ba0d471b6462fc5d4c7fb99ed42fb7e))
* **fluids:** render custom fluids in the look-at HUD ([#706](https://github.com/Indemnity83/logistics/issues/706)) ([d45436d](https://github.com/Indemnity83/logistics/commit/d45436dee53c35dc189e618327be924dba24b64f))
* **fluids:** stop drained pipes rendering a checkerboard ([#696](https://github.com/Indemnity83/logistics/issues/696)) ([852ce38](https://github.com/Indemnity83/logistics/commit/852ce382f3c8f0db80cec56be58b80cb3a5ffef4))
* **fluids:** stop fluid extractor pipes from connecting to each other ([#692](https://github.com/Indemnity83/logistics/issues/692)) ([b6c59c6](https://github.com/Indemnity83/logistics/commit/b6c59c6e51cac0b442546f15903b9200adb4fc55))
* **pump:** make the pump tank output-only ([#693](https://github.com/Indemnity83/logistics/issues/693)) ([ac5fcaa](https://github.com/Indemnity83/logistics/commit/ac5fcaaad0b80614709e7379eecee096dbaef39b))
* **routing:** drop the failed order's index entry on delivery retry ([5216286](https://github.com/Indemnity83/logistics/commit/5216286ef4c008788f5e4450f40057469054f012))

## [0.8.1](https://github.com/Indemnity83/logistics/compare/mc1.21.11-v0.8.0...mc1.21.11-v0.8.1) (2026-06-30)


### Removed

* **core:** drop the unused sturdy casing ([#650](https://github.com/Indemnity83/logistics/issues/650)) ([d1156b7](https://github.com/Indemnity83/logistics/commit/d1156b7774cdf8f8a5a0d5f555750da865b1580e))


### Fixed

* **pump:** accept power from any energy source ([#651](https://github.com/Indemnity83/logistics/issues/651)) ([ccc21a9](https://github.com/Indemnity83/logistics/commit/ccc21a903f51aff0ae91ba5e50f55306ff0d791d))

## [0.8.0](https://github.com/Indemnity83/logistics/compare/mc1.21.11-v0.7.4...mc1.21.11-v0.8.0) (2026-06-29)


### ⚠ BREAKING CHANGES

* **energy:** Cables and batteries no longer power extraction pipes or the Fluid Pump. Only a directly-adjacent engine can power them. Existing setups that fed pipes through cables/batteries will stop working — place an engine against the pipe instead.
* **energy:** Batteries no longer power a logistics network directly. A network is powered only through a Power Junction — place one between your power source (cables/batteries) and the network. Existing battery-on-a-pipe setups stop working until a Power Junction is added.

### Added

* **core:** add opt-in sanitized crash reporting ([#633](https://github.com/Indemnity83/logistics/issues/633)) ([29d7de6](https://github.com/Indemnity83/logistics/commit/29d7de616b88b8ba59a72ad8a5c8870b932d7f5d))
* **core:** drop niter from the breeze ([#644](https://github.com/Indemnity83/logistics/issues/644)) ([173d868](https://github.com/Indemnity83/logistics/commit/173d868e63cd107865b7f6b96d0003e6b86cebb0))
* **crafting:** craft gunpowder from coal, sulfur, and niter dust ([77d4544](https://github.com/Indemnity83/logistics/commit/77d4544a66399dedae1397a02c1b0746b23e781a))
* **energy:** add the power junction ([#612](https://github.com/Indemnity83/logistics/issues/612)) ([d10c116](https://github.com/Indemnity83/logistics/commit/d10c116d567ab27fd2a63ce7fdfaba7766b90eb6))
* **macerator:** add chance byproducts to ore processing ([#643](https://github.com/Indemnity83/logistics/issues/643)) ([77d4544](https://github.com/Indemnity83/logistics/commit/77d4544a66399dedae1397a02c1b0746b23e781a))
* **macerator:** add recycling recipes for common blocks ([77d4544](https://github.com/Indemnity83/logistics/commit/77d4544a66399dedae1397a02c1b0746b23e781a))
* **macerator:** add Sulfur Dust, Quicksilver, and Niter items ([77d4544](https://github.com/Indemnity83/logistics/commit/77d4544a66399dedae1397a02c1b0746b23e781a))
* **macerator:** macerate breeze rods into wind charges ([77d4544](https://github.com/Indemnity83/logistics/commit/77d4544a66399dedae1397a02c1b0746b23e781a))
* **macerator:** macerate logs and planks into sawdust and recycle wooden tools ([77d4544](https://github.com/Indemnity83/logistics/commit/77d4544a66399dedae1397a02c1b0746b23e781a))
* **macerator:** recycle diamond tools and armor into diamonds ([77d4544](https://github.com/Indemnity83/logistics/commit/77d4544a66399dedae1397a02c1b0746b23e781a))
* **sawmill:** add a recipe book to the sawmill GUI ([#642](https://github.com/Indemnity83/logistics/issues/642)) ([7042821](https://github.com/Indemnity83/logistics/commit/70428214ddfd98da03d3e0ab2b4a3c947e9dfab1))
* **sawmill:** add wood processing ([#580](https://github.com/Indemnity83/logistics/issues/580)) ([2cfeb10](https://github.com/Indemnity83/logistics/commit/2cfeb10432e4dae529f8fb2aa7ba46abce709e22))
* **sawmill:** show recipes in JEI and details in the Jade HUD ([#613](https://github.com/Indemnity83/logistics/issues/613)) ([2f59bca](https://github.com/Indemnity83/logistics/commit/2f59bca9603e06efc2acfe88847dc4787b44d287))


### Changed

* **common:** cache pipe and cable collision shapes ([#631](https://github.com/Indemnity83/logistics/issues/631)) ([6e739e9](https://github.com/Indemnity83/logistics/commit/6e739e9bc276af14f85c73f42958e4611fd97dc3))
* **core:** streamline world loading by dropping legacy save migrations ([#586](https://github.com/Indemnity83/logistics/issues/586)) ([38a3d9e](https://github.com/Indemnity83/logistics/commit/38a3d9e91afc08ff7260eccd7b30db055a0b192d))
* **crafting:** require a bronze gear in the machine frame ([#610](https://github.com/Indemnity83/logistics/issues/610)) ([9ea3e33](https://github.com/Indemnity83/logistics/commit/9ea3e33e954f4b2edba818467d5e8780415a2b46))
* **crafting:** yield one marker per craft ([#607](https://github.com/Indemnity83/logistics/issues/607)) ([f29c05e](https://github.com/Indemnity83/logistics/commit/f29c05e4f79e39911715faec6dfcc06c0858b5f3))
* **energy:** power extraction pipes only from a direct engine ([#641](https://github.com/Indemnity83/logistics/issues/641)) ([65d343b](https://github.com/Indemnity83/logistics/commit/65d343bbb77f701a279cf23ec3d1d298a8a8eeee))
* **fluids:** speed up fluid split allocation ([#623](https://github.com/Indemnity83/logistics/issues/623)) ([8e6e649](https://github.com/Indemnity83/logistics/commit/8e6e6498fa1872e2a7e5c54e189b189163a0ea16))
* **macerator:** ore→dust recipes now drop a chance byproduct dust ([77d4544](https://github.com/Indemnity83/logistics/commit/77d4544a66399dedae1397a02c1b0746b23e781a))
* **pipes:** raise pipe blast resistance to match glass ([#618](https://github.com/Indemnity83/logistics/issues/618)) ([3179869](https://github.com/Indemnity83/logistics/commit/31798695790223f63f980bcfd4546731fcf1a06e))
* **quarry:** restyle with the shared machine look ([#582](https://github.com/Indemnity83/logistics/issues/582)) ([6f6d9c0](https://github.com/Indemnity83/logistics/commit/6f6d9c062e38d7ae0e2c63e907562c50bf53bd92))
* **routing:** cache next-hop routes per destination ([#632](https://github.com/Indemnity83/logistics/issues/632)) ([3ded27e](https://github.com/Indemnity83/logistics/commit/3ded27ea9bbbe5b8d458397a941d562247d18161))
* **sawmill:** match the energy buffer to the other machines ([#647](https://github.com/Indemnity83/logistics/issues/647)) ([4c88590](https://github.com/Indemnity83/logistics/commit/4c885902b84136d41c78913560cb26acd6d6dec8))
* **ui:** refresh the kiln, macerator, and sawmill GUIs ([#646](https://github.com/Indemnity83/logistics/issues/646)) ([12c8f9a](https://github.com/Indemnity83/logistics/commit/12c8f9a5c578d7ca73455e37e1c4e662ac1f4435))
* **worldgen:** tin ore drops one raw tin ([#608](https://github.com/Indemnity83/logistics/issues/608)) ([6747a29](https://github.com/Indemnity83/logistics/commit/6747a2953bf30c4e14bc0a367aac6571b029d7a6))


### Fixed

* **automation:** let quarry markers connect through solid blocks ([#581](https://github.com/Indemnity83/logistics/issues/581)) ([87fd19e](https://github.com/Indemnity83/logistics/commit/87fd19e099adc1a3dffca7f2f93d5ff104a607c6))
* **automation:** pause recipes until byproducts have space ([#597](https://github.com/Indemnity83/logistics/issues/597)) ([308b7be](https://github.com/Indemnity83/logistics/commit/308b7be0488fc53022af5a0c3c1e31138286f387))
* **core:** restore valve and quartz crystal recipes ([#600](https://github.com/Indemnity83/logistics/issues/600)) ([9a15308](https://github.com/Indemnity83/logistics/commit/9a153081ba482dc7df4f6f6791a3e111c4ee9952))
* **energy:** drop the creative sink when broken ([#616](https://github.com/Indemnity83/logistics/issues/616)) ([e5ba6b1](https://github.com/Indemnity83/logistics/commit/e5ba6b1ed9884128f81e4f702696f1db75d50ad2))
* **fluids:** drop fluid pipes and glass tank when broken ([#614](https://github.com/Indemnity83/logistics/issues/614)) ([6ac59f1](https://github.com/Indemnity83/logistics/commit/6ac59f19f9217d7d8ea1bd931313cc5c61ece45f))
* **fluids:** show correct fill level on tank and pipe HUDs ([#619](https://github.com/Indemnity83/logistics/issues/619)) ([34c41f4](https://github.com/Indemnity83/logistics/commit/34c41f451c8a3b11fcbdd87d7b06680087df0987))
* **kiln:** accept energy from the power network ([#602](https://github.com/Indemnity83/logistics/issues/602)) ([898e446](https://github.com/Indemnity83/logistics/commit/898e446fb16ec20102bf4e0569e713e3af6653c3))
* **kiln:** bank smelting XP and pay it out like a furnace ([#605](https://github.com/Indemnity83/logistics/issues/605)) ([560a528](https://github.com/Indemnity83/logistics/commit/560a5287ab4fe1ef2e6d00bf94fcd6f3b1883505))
* **kiln:** mine with the correct pickaxe tier ([#601](https://github.com/Indemnity83/logistics/issues/601)) ([ed542cd](https://github.com/Indemnity83/logistics/commit/ed542cd9bca89717fb36dc6be44a0ed4b3db1c94))
* **macerator:** bank maceration XP and pay it out like a furnace ([560a528](https://github.com/Indemnity83/logistics/commit/560a5287ab4fe1ef2e6d00bf94fcd6f3b1883505))
* **macerator:** grant XP for macerating ancient debris ([#624](https://github.com/Indemnity83/logistics/issues/624)) ([e4a62a4](https://github.com/Indemnity83/logistics/commit/e4a62a459ca834285b238c69fada2a093da9193f))
* **macerator:** restore the JEI integration ([#599](https://github.com/Indemnity83/logistics/issues/599)) ([927bac2](https://github.com/Indemnity83/logistics/commit/927bac26e3ef71d6b54919db218973b632f0e5e7))
* **pump:** clamp the fluid pump search radius ([#625](https://github.com/Indemnity83/logistics/issues/625)) ([0bb8256](https://github.com/Indemnity83/logistics/commit/0bb82565673240dd0c6a1c07ea2d72256c294994))
* **pump:** match the fluid pump's top to the other machines ([#561](https://github.com/Indemnity83/logistics/issues/561)) ([5670103](https://github.com/Indemnity83/logistics/commit/567010375ed3544857a9fa21f1223d8419e98e99))
* **pump:** mine the fluid pump with the correct pickaxe tier ([6ac59f1](https://github.com/Indemnity83/logistics/commit/6ac59f19f9217d7d8ea1bd931313cc5c61ece45f))
* **quarry:** give the laser quarry frame a display name ([#617](https://github.com/Indemnity83/logistics/issues/617)) ([2abf533](https://github.com/Indemnity83/logistics/commit/2abf533062dc29b12031a613b4fca004f63afe6d))
* **routing:** keep chassis modules when a pipe explodes ([#629](https://github.com/Indemnity83/logistics/issues/629)) ([e31d8f5](https://github.com/Indemnity83/logistics/commit/e31d8f50af328550b9220d26d8c260dff84d47d5))
* **routing:** refresh neighbor pipe arms when markings change ([#606](https://github.com/Indemnity83/logistics/issues/606)) ([6f19887](https://github.com/Indemnity83/logistics/commit/6f198874a108655599d71f4f5ad5a524d70afaad))
* **sawmill:** add the missing crafting recipe ([#609](https://github.com/Indemnity83/logistics/issues/609)) ([9827d3a](https://github.com/Indemnity83/logistics/commit/9827d3af06d1fcc35d92d2849bc0e3b89c6d37a9))
* **sawmill:** mine with the correct pickaxe tier ([2f59bca](https://github.com/Indemnity83/logistics/commit/2f59bca9603e06efc2acfe88847dc4787b44d287))

## [0.7.4](https://github.com/Indemnity83/logistics/compare/mc1.21.11-v0.7.3...mc1.21.11-v0.7.4) (2026-06-21)


### Added

* **automation:** add configurable laser quarry chunk loading ([#536](https://github.com/Indemnity83/logistics/issues/536)) ([5a952db](https://github.com/Indemnity83/logistics/commit/5a952dbefaa546bdf384935c948c8079ecb4728a)) — thanks @floralpetals
* **core:** grant ore XP when macerating metal and apatite ores ([#555](https://github.com/Indemnity83/logistics/issues/555)) ([4c1bf61](https://github.com/Indemnity83/logistics/commit/4c1bf61c5fb3210570a3c52ae0aa8f9987867d28))


### Fixed

* **automation:** add missing macerator recipes for obsidian, netherite, and metal blocks ([#554](https://github.com/Indemnity83/logistics/issues/554)) ([74b291c](https://github.com/Indemnity83/logistics/commit/74b291c2489ca75de62a7fe654565f183d5644bb))
* **energy:** fix crash when cables power machines from other mods ([#556](https://github.com/Indemnity83/logistics/issues/556)) ([0338bd0](https://github.com/Indemnity83/logistics/commit/0338bd044d9ae951103ce58b090092aaea4b7428))

### New Contributors
* @floralpetals made their first contribution in #536

## [0.7.3](https://github.com/Indemnity83/logistics/compare/mc1.21.11-v0.7.2...mc1.21.11-v0.7.3) (2026-06-18)


### Added

* **api:** add loader-independent API for cross-mod fluid integration ([#516](https://github.com/Indemnity83/logistics/issues/516)) ([d8f0c9b](https://github.com/Indemnity83/logistics/commit/d8f0c9bc852b9bd2c9b166751e319fad1980e39e))
* **pipes:** add copper fluid pipe oxidation and fluid pipe marking ([#520](https://github.com/Indemnity83/logistics/issues/520)) ([d8f0c9b](https://github.com/Indemnity83/logistics/commit/d8f0c9bc852b9bd2c9b166751e319fad1980e39e))
* **pipes:** add fluid pipes, tanks, and powered fluid extraction ([#511](https://github.com/Indemnity83/logistics/issues/511)) ([d8f0c9b](https://github.com/Indemnity83/logistics/commit/d8f0c9bc852b9bd2c9b166751e319fad1980e39e))
* **pipes:** add fluid pump ([#537](https://github.com/Indemnity83/logistics/issues/537)) ([4606765](https://github.com/Indemnity83/logistics/commit/46067657ca8655389a4fd7ecc25b0c1c4dd153c1))


### Fixed

* **compat:** hide fluid extractor energy in Jade ([#533](https://github.com/Indemnity83/logistics/issues/533)) ([209976d](https://github.com/Indemnity83/logistics/commit/209976ddf8fda5d82ecc430075468ab536863d25))
* **pipes:** fix fluid pipe drain flicker ([#532](https://github.com/Indemnity83/logistics/issues/532)) ([0bd1b83](https://github.com/Indemnity83/logistics/commit/0bd1b836a83e533e023b1ec30890ecd87197e81c))
* **ui:** fix glass tank capacity overlay with held items ([#534](https://github.com/Indemnity83/logistics/issues/534)) ([caddca1](https://github.com/Indemnity83/logistics/commit/caddca1c9d8db5c766f4bf798d4fbc88c6dc17b6))

## [0.7.2](https://github.com/Indemnity83/logistics/compare/mc1.21.11-v0.7.1...mc1.21.11-v0.7.2) (2026-06-15)


### Fixed

* **energy:** add crafting recipe for battery ([#510](https://github.com/Indemnity83/logistics/issues/510)) ([53d3387](https://github.com/Indemnity83/logistics/commit/53d3387766c409b73b7c166321824e5c57a78482))

## [0.7.1](https://github.com/Indemnity83/logistics/compare/mc1.21.11-v0.7.0...mc1.21.11-v0.7.1) (2026-06-11)


### Added

* **automation:** show laser quarry status in the Jade HUD ([#498](https://github.com/Indemnity83/logistics/issues/498)) ([d76b84a](https://github.com/Indemnity83/logistics/commit/d76b84ad7d0ebd72a3d66996197770fa4d62f5bd))
* **automation:** show macerator and kiln progress in the Jade HUD ([#499](https://github.com/Indemnity83/logistics/issues/499)) ([2ee8ea5](https://github.com/Indemnity83/logistics/commit/2ee8ea5fde49611eeab45e4d435b4e24452ec334))
* **compat:** integrate Jade and remove the built-in probe ([a1daae5](https://github.com/Indemnity83/logistics/commit/a1daae57fbdc84e6f5fbabe5e25d2d090fe390f4))
* **energy:** show power diagnostics in the Jade HUD ([#497](https://github.com/Indemnity83/logistics/issues/497)) ([6029420](https://github.com/Indemnity83/logistics/commit/6029420aab3fc0f88eb696c768de014340dd74be))
* **pipes:** show pipe contents in the Jade HUD ([#500](https://github.com/Indemnity83/logistics/issues/500)) ([f2ab878](https://github.com/Indemnity83/logistics/commit/f2ab87880fcd95f505205c51b55efef44e9a2bba))
* **pipes:** show pipe module status in the Jade HUD ([#493](https://github.com/Indemnity83/logistics/issues/493)) ([e3c2776](https://github.com/Indemnity83/logistics/commit/e3c2776fc98a3cc852a94e65a6c7e8ca300882eb))


### Fixed

* **pipes:** apply config changes to modules installed in a chassis ([#504](https://github.com/Indemnity83/logistics/issues/504)) ([4c3dedc](https://github.com/Indemnity83/logistics/commit/4c3dedcb48c6f8cf30cdf6d8f88daadba4b2cde8)), closes [#494](https://github.com/Indemnity83/logistics/issues/494)

## [0.7.0](https://github.com/Indemnity83/logistics/compare/mc1.21.11-v0.6.3...mc1.21.11-v0.7.0) (2026-06-09)


### ⚠ BREAKING CHANGES

* **energy:** logistics pipe operations consume power ([#465](https://github.com/Indemnity83/logistics/issues/465))
* **energy:** Logistics pipes now require power from an adjacent Battery. Existing networks stop routing/supplying/crafting — and drop items already in transit — until a charged Battery is connected.

### Added

* **energy:** add a Battery block to power logistics networks ([1048c01](https://github.com/Indemnity83/logistics/commit/1048c013f32008095a1d313ef1be53c97d962f0d))
* **energy:** logistics pipe operations consume power ([#465](https://github.com/Indemnity83/logistics/issues/465)) ([1048c01](https://github.com/Indemnity83/logistics/commit/1048c013f32008095a1d313ef1be53c97d962f0d))
* **ui:** color logistics pipes green when powered, red when not ([#469](https://github.com/Indemnity83/logistics/issues/469)) ([815b49b](https://github.com/Indemnity83/logistics/commit/815b49bd88e155c4b2963bc19fb2146e61bfedf7))


### Fixed

* **automation:** stop inactive markers rendering as a black cross ([#484](https://github.com/Indemnity83/logistics/issues/484)) ([c9f28e8](https://github.com/Indemnity83/logistics/commit/c9f28e8c006004db554df0cea4d7e9d8501f2a8a))
* **automation:** stop the macerator from trying to load other mods' recipes ([#473](https://github.com/Indemnity83/logistics/issues/473)) ([21c7225](https://github.com/Indemnity83/logistics/commit/21c722532e101f224ced3c42a18ef5a6e651dd3b))
* **energy:** make engines visibly change color with heat stage ([695288c](https://github.com/Indemnity83/logistics/commit/695288cdb6d3bb55244410da55478e4dd9473fd9))
* **energy:** stop the battery charge bar rendering black on NeoForge ([2ec31f4](https://github.com/Indemnity83/logistics/commit/2ec31f48c1818f5c0fcb6f5b04a8fe156eecc358))

## [0.6.3](https://github.com/Indemnity83/logistics/compare/mc1.21.11-v0.6.2...mc1.21.11-v0.6.3) (2026-06-05)


### Fixed

* **energy:** transport energy through cables on NeoForge ([a7a9a5e](https://github.com/Indemnity83/logistics/commit/a7a9a5e8bb7329a3e6496e737ed12d8592bdeb61))

## [0.6.2](https://github.com/Indemnity83/logistics/compare/mc1.21.11-v0.6.1...mc1.21.11-v0.6.2) (2026-06-04)


### Improved

* **automation:** reduce memory and load time for machine rendering ([#451](https://github.com/Indemnity83/logistics/issues/451)) ([b8eba57](https://github.com/Indemnity83/logistics/commit/b8eba5757681c2e097c2277681f42ebc8934c5b2))
* **energy:** reduce memory and load time for cable rendering ([#449](https://github.com/Indemnity83/logistics/issues/449)) ([2a82bad](https://github.com/Indemnity83/logistics/commit/2a82bad95744684425ec8ceb22084c577a13621c))
* **pipes:** reduce memory and load time for pipe rendering ([#450](https://github.com/Indemnity83/logistics/issues/450)) ([155e7b7](https://github.com/Indemnity83/logistics/commit/155e7b77d9f580ff292d7e09f075bfefc23141cb))

## [0.6.1](https://github.com/Indemnity83/logistics/compare/mc1.21.11-v0.6.0...mc1.21.11-v0.6.1) (2026-06-02)


### Fixed

* correct laser quarry edge case regressions ([#427](https://github.com/Indemnity83/logistics/issues/427)) ([48afb93](https://github.com/Indemnity83/logistics/commit/48afb93a3943e030a3130c73c41191489b8e6f91))
* enable custom Minecraft version range in build workflows ([#414](https://github.com/Indemnity83/logistics/issues/414)) ([42b66ed](https://github.com/Indemnity83/logistics/commit/42b66ede0de3f963247eca7ed522c6e8d883d142))
* update Minecraft version compatibility range for NeoForge ([#416](https://github.com/Indemnity83/logistics/issues/416)) ([5f735aa](https://github.com/Indemnity83/logistics/commit/5f735aa552712172252ee5658cd304a0e55a34e2))

## [0.6.0](https://github.com/Indemnity83/logistics/compare/mc1.21.11-v0.5.6...mc1.21.11-v0.6.0) (2026-05-31)

### Features

* Added NeoForge support, including platform services, capabilities, networking/lifecycle hooks, client rendering, and storage adapters. [#378](https://github.com/Indemnity83/logistics/issues/378), [#379](https://github.com/Indemnity83/logistics/issues/379), [#380](https://github.com/Indemnity83/logistics/issues/380), [#381](https://github.com/Indemnity83/logistics/issues/381), [56a58cf](https://github.com/Indemnity83/logistics/commit/56a58cff6cc8d3efd84562ce9fbdc4594be76fe1)
* Added power cables. [3acd758](https://github.com/Indemnity83/logistics/commit/3acd75831ac3e1d8ff91808b733a2e7ee53e11af)

### Bug Fixes

* Improved logistics network reliability and safety by handling failed deliveries, validating/sanitizing config fields, and clamping energy values and transfer amounts to non-negative values. [#397](https://github.com/Indemnity83/logistics/issues/397), [#398](https://github.com/Indemnity83/logistics/issues/398), [#399](https://github.com/Indemnity83/logistics/issues/399)
* Fixed NeoForge and multi-loader content issues, including JEI Macerator recipe visibility, marking fluid recipe separation, and missing `META-INF` service files. [#382](https://github.com/Indemnity83/logistics/issues/382), [#390](https://github.com/Indemnity83/logistics/issues/390), [5fb4573](https://github.com/Indemnity83/logistics/commit/5fb457332c5e5114e791cbaa68ace00f9eef9f13) — thanks @AdolfoCarneiro
* Fixed power cable compilation errors for Minecraft 1.21.11. [#358](https://github.com/Indemnity83/logistics/issues/358)
* Preserved item components in filter pipe slots across save/reload. [#386](https://github.com/Indemnity83/logistics/issues/386)

### Refactorings

* Reworked the project for multi-loader support, including NeoForge groundwork, loader-agnostic bootstrap flow, service-based platform access, cleaner module boundaries, and build configuration updates. [#306](https://github.com/Indemnity83/logistics/issues/306), [#318](https://github.com/Indemnity83/logistics/issues/318), [#320](https://github.com/Indemnity83/logistics/issues/320), [#341](https://github.com/Indemnity83/logistics/issues/341), [#342](https://github.com/Indemnity83/logistics/issues/342), [#343](https://github.com/Indemnity83/logistics/issues/343), [#344](https://github.com/Indemnity83/logistics/issues/344), [#347](https://github.com/Indemnity83/logistics/issues/347), [#348](https://github.com/Indemnity83/logistics/issues/348), [#360](https://github.com/Indemnity83/logistics/issues/360), [#361](https://github.com/Indemnity83/logistics/issues/361) — thanks @AdolfoCarneiro
* Introduced loader-agnostic storage, energy, fluid, fuel, item matching, and client model abstractions. [#340](https://github.com/Indemnity83/logistics/issues/340), [#349](https://github.com/Indemnity83/logistics/issues/349), [#351](https://github.com/Indemnity83/logistics/issues/351), [#356](https://github.com/Indemnity83/logistics/issues/356), [#364](https://github.com/Indemnity83/logistics/issues/364), [#389](https://github.com/Indemnity83/logistics/issues/389), [#400](https://github.com/Indemnity83/logistics/issues/400)
* Cleaned up common code organization and removed remaining Fabric-specific dependencies/imports from shared sources. [#345](https://github.com/Indemnity83/logistics/issues/345), [#350](https://github.com/Indemnity83/logistics/issues/350), [#352](https://github.com/Indemnity83/logistics/issues/352), [#355](https://github.com/Indemnity83/logistics/issues/355)

### Testing

* Added Fabric/NeoForge test infrastructure, NeoForge ServiceLoader and energy adapter tests, component coverage, and baseline coverage reporting. [#377](https://github.com/Indemnity83/logistics/issues/377), [#402](https://github.com/Indemnity83/logistics/issues/402), [93d7f9d](https://github.com/Indemnity83/logistics/commit/93d7f9dc14592a56938ed8e0aaff574b15072a27)

### New Contributors
* @AdolfoCarneiro made their first contribution in #306

## [0.5.6](https://github.com/Indemnity83/logistics/compare/mc1.21.11-v0.5.5...mc1.21.11-v0.5.6) (2026-05-04)


### Bug Fixes

* correct lever and dust placement on engine blocks ([#300](https://github.com/Indemnity83/logistics/issues/300)) ([8add73b](https://github.com/Indemnity83/logistics/commit/8add73b4632ad827c2986de97d5d27d625844d39))
* correct supplier module UI targeting ([#295](https://github.com/Indemnity83/logistics/issues/295)) ([321ae4f](https://github.com/Indemnity83/logistics/commit/321ae4f8f0ec409bde2e2d208c073137477edd16)) — thanks @ZayshaaCodes
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

* add ender dust macerator recipe ([#282](https://github.com/Indemnity83/logistics/issues/282)) ([3c7baad](https://github.com/Indemnity83/logistics/commit/3c7baad1b45369e6be48bb5df2a45e8d1346bc63)) — thanks @ZayshaaCodes
* use RegistryOps to fix enchanted item crash ([#283](https://github.com/Indemnity83/logistics/issues/283)) ([2a096a9](https://github.com/Indemnity83/logistics/commit/2a096a9467b9a0a2764edc12a6b900e60bd73748)) — thanks @ZayshaaCodes

### New Contributors
* @ZayshaaCodes made their first contribution in #282

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
