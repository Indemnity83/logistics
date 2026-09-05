# Changelog

## [0.8.8](https://github.com/Indemnity83/logistics/compare/mc26.1-v0.8.7...mc26.1-v0.8.8) (2026-09-05)


### Changed

* **routing:** serve pipe shapes from the connection cache ([e2501b3](https://github.com/Indemnity83/logistics/commit/e2501b34d5ce01105e94eef5e4086303f751616f))


### Removed

* **crafting:** drop the classic crafting resource pack ([55fe8ad](https://github.com/Indemnity83/logistics/commit/55fe8ad8249bbb64e8dc0a97b3abddeafa0b7b5f)), closes [#960](https://github.com/Indemnity83/logistics/issues/960)


### Fixed

* **automation:** stop the Alloy Smelter duplicating recipe inputs ([ac0f6ba](https://github.com/Indemnity83/logistics/commit/ac0f6ba81a7179da0710f2f1bb748b01cab7653f))
* **ci:** stop the release pipeline publishing broken releases ([e1a7efd](https://github.com/Indemnity83/logistics/commit/e1a7efd4877d0a850d7c9eb379311a401f192794))
* **core:** give the Seed Oil Bucket its missing model and texture ([#904](https://github.com/Indemnity83/logistics/issues/904)) ([2d044f1](https://github.com/Indemnity83/logistics/commit/2d044f1774e20fdc301cdf466ac6e328f41aa9e7))
* **core:** survive a malformed config file at startup ([23381b8](https://github.com/Indemnity83/logistics/commit/23381b86aae9fc654b5ce36867ec1a92315fff1b))
* **crafting:** keep the sourceable remainder when a request is replanned ([174c1f2](https://github.com/Indemnity83/logistics/commit/174c1f28dca4d5da4770fe37e8be9fe8fb9b28b5))
* **crafting:** order only what was requested from a crafting batch ([174c1f2](https://github.com/Indemnity83/logistics/commit/174c1f28dca4d5da4770fe37e8be9fe8fb9b28b5))
* **energy:** face a newly placed engine at a full machine on NeoForge ([b558d02](https://github.com/Indemnity83/logistics/commit/b558d0266abb620f1b07d2135d0cedf79858ffa2)), closes [#977](https://github.com/Indemnity83/logistics/issues/977) [#989](https://github.com/Indemnity83/logistics/issues/989)
* **energy:** give engine and battery models their missing particle texture ([#909](https://github.com/Indemnity83/logistics/issues/909)) ([43d1d04](https://github.com/Indemnity83/logistics/commit/43d1d041a03e292412822bf1d038e4e58a307a88))
* **energy:** stop a battery starving its own cable network ([b558d02](https://github.com/Indemnity83/logistics/commit/b558d0266abb620f1b07d2135d0cedf79858ffa2))
* **energy:** stop cables voiding power into slow machines ([3c2472b](https://github.com/Indemnity83/logistics/commit/3c2472b2149e5320c964db19306a33d4aebb4195))
* **energy:** stop duplicating and voiding energy on Fabric ([170ad27](https://github.com/Indemnity83/logistics/commit/170ad27ed6a06c262977bd54134d1c54326b789e))
* **fluids:** fade fluid out of a pipe instead of popping it ([eb8295c](https://github.com/Indemnity83/logistics/commit/eb8295c6ef6f5d42f71d75883e8550b507e4318a))
* **fluids:** keep the fill animation steady at any frame rate ([eb8295c](https://github.com/Indemnity83/logistics/commit/eb8295c6ef6f5d42f71d75883e8550b507e4318a))
* **pump:** keep long intake tubes visible off screen ([52f153a](https://github.com/Indemnity83/logistics/commit/52f153a8d8481fa0387ceaf6cf87a2936effb41e))
* **pump:** stop the intake tube descending through waterlogged blocks ([a280f8e](https://github.com/Indemnity83/logistics/commit/a280f8e1873e0642479e83198868577932cdf7ab)), closes [#968](https://github.com/Indemnity83/logistics/issues/968) [#969](https://github.com/Indemnity83/logistics/issues/969)
* **quarry:** keep marker beams visible off screen ([52f153a](https://github.com/Indemnity83/logistics/commit/52f153a8d8481fa0387ceaf6cf87a2936effb41e)), closes [#940](https://github.com/Indemnity83/logistics/issues/940)
* **quarry:** keep the frame and laser visible off screen ([52f153a](https://github.com/Indemnity83/logistics/commit/52f153a8d8481fa0387ceaf6cf87a2936effb41e))
* **quarry:** let players break abandoned frames in survival ([d92cde2](https://github.com/Indemnity83/logistics/commit/d92cde2957a04e12c88834a58e8f17bd62aa74ef))
* **quarry:** mine waterlogged blocks instead of skipping them ([a280f8e](https://github.com/Indemnity83/logistics/commit/a280f8e1873e0642479e83198868577932cdf7ab))
* **quarry:** remove the unintended duplicate recipe ([13758c8](https://github.com/Indemnity83/logistics/commit/13758c89f624da9fc1e6ce12650b8fd7487169eb))
* **quarry:** stop a zero arm speed freezing the quarry forever ([38f1730](https://github.com/Indemnity83/logistics/commit/38f17307e31e1dc494f24ff90bbb56fabb6a6af5))
* **quarry:** stop frame blocks vanishing without warning ([d92cde2](https://github.com/Indemnity83/logistics/commit/d92cde2957a04e12c88834a58e8f17bd62aa74ef))
* **quarry:** stop vacuuming loose items off the ground ([7d3f302](https://github.com/Indemnity83/logistics/commit/7d3f302f63d4bb4fb7899dc5e670b92eb58f9f91)), closes [#973](https://github.com/Indemnity83/logistics/issues/973)
* **routing:** apply one interaction range to every pipe menu ([587b984](https://github.com/Indemnity83/logistics/commit/587b9841bcf4332d0a1031fbeb5bc11a277fb41b)), closes [#937](https://github.com/Indemnity83/logistics/issues/937) [#942](https://github.com/Indemnity83/logistics/issues/942)
* **routing:** cancel the order behind a job that has finished ([174c1f2](https://github.com/Indemnity83/logistics/commit/174c1f28dca4d5da4770fe37e8be9fe8fb9b28b5)), closes [#933](https://github.com/Indemnity83/logistics/issues/933) [#944](https://github.com/Indemnity83/logistics/issues/944) [#947](https://github.com/Indemnity83/logistics/issues/947)
* **routing:** close module menus when the pipe is broken ([587b984](https://github.com/Indemnity83/logistics/commit/587b9841bcf4332d0a1031fbeb5bc11a277fb41b))
* **routing:** fall through to the next provider when one is fully reserved ([c0f0662](https://github.com/Indemnity83/logistics/commit/c0f066244a8e9e7efdf46261e9a9471e97d579d8)), closes [#928](https://github.com/Indemnity83/logistics/issues/928) [#938](https://github.com/Indemnity83/logistics/issues/938) [#939](https://github.com/Indemnity83/logistics/issues/939)
* **routing:** release only the delivered part of a shipment ([c0f0662](https://github.com/Indemnity83/logistics/commit/c0f066244a8e9e7efdf46261e9a9471e97d579d8))
* **routing:** render the right pipe arm after a neighbour changes ([e2501b3](https://github.com/Indemnity83/logistics/commit/e2501b34d5ce01105e94eef5e4086303f751616f))
* **routing:** stop a bad button id crashing the server ([60a6e36](https://github.com/Indemnity83/logistics/commit/60a6e36a2539cbfba4885b8b68e013bc6bfac4d9))
* **routing:** stop broadcasting requester contents to every player ([37918fe](https://github.com/Indemnity83/logistics/commit/37918fe5162631c939caaf85d0723a486d6fc429))
* **routing:** stop failed deliveries shrinking a provider's stock ([c0f0662](https://github.com/Indemnity83/logistics/commit/c0f066244a8e9e7efdf46261e9a9471e97d579d8))
* **routing:** stop Providers destroying items on an unpowered network ([2b62986](https://github.com/Indemnity83/logistics/commit/2b62986c717994797cb8ac1809ae2da3778c96bb))
* **routing:** stop the Provider MkII shipping the wrong item to the next order ([f24f717](https://github.com/Indemnity83/logistics/commit/f24f7170533e806408499ae44111f27358c7b43e))
* **transport:** apply the configured minimum pipe speed ([eb8295c](https://github.com/Indemnity83/logistics/commit/eb8295c6ef6f5d42f71d75883e8550b507e4318a)), closes [#950](https://github.com/Indemnity83/logistics/issues/950)
* **transport:** stop losing items on a partial pipe handoff ([43e98c1](https://github.com/Indemnity83/logistics/commit/43e98c17c419cf57b82b4730710865a90aa174a7))

## [0.8.7](https://github.com/Indemnity83/logistics/compare/mc26.1-v0.8.6...mc26.1-v0.8.7) (2026-09-01)


### Added

* **automation:** add crude oil to petroleum-block transposer recipes ([#851](https://github.com/Indemnity83/logistics/issues/851)) ([5a7a588](https://github.com/Indemnity83/logistics/commit/5a7a588bcb52de8c6fbc88e9dc7c1d6d8b04f866))
* **automation:** add RF cost and recipe system to the Transposer ([#829](https://github.com/Indemnity83/logistics/issues/829)) ([8ccc413](https://github.com/Indemnity83/logistics/commit/8ccc413c32c13c7c72583ab2b3f2036e98d00c5f))
* **core:** add shared gear recipe tags ([#871](https://github.com/Indemnity83/logistics/issues/871)) ([7bc1527](https://github.com/Indemnity83/logistics/commit/7bc152796837ed4795456eaf7add7fa29a8b0de0))
* **energy:** show engine fuels in the recipe browser ([#886](https://github.com/Indemnity83/logistics/issues/886)) ([0a58190](https://github.com/Indemnity83/logistics/commit/0a58190b0f7f7b1515987151bb88b56cc012f324))
* **fluids:** drain cauldrons with the fluid extractor pipe ([#885](https://github.com/Indemnity83/logistics/issues/885)) ([f0d916b](https://github.com/Indemnity83/logistics/commit/f0d916b52eaadda1591b7d2f79b483893251acbc))


### Changed

* **automation:** restyle the Crucible progress gauge as a droplet ([#834](https://github.com/Indemnity83/logistics/issues/834)) ([25a4ff4](https://github.com/Indemnity83/logistics/commit/25a4ff4d10814fe567f3dea09337cfa767250175))


### Fixed

* **automation:** accept raw ore in the quicksilver amalgamation recipes ([#840](https://github.com/Indemnity83/logistics/issues/840)) ([b2ea20c](https://github.com/Indemnity83/logistics/commit/b2ea20c736cf7596b24f31089b4483b3d363a3f3))
* **automation:** stop refinery shift-click from duplicating items ([#846](https://github.com/Indemnity83/logistics/issues/846)) ([329d875](https://github.com/Indemnity83/logistics/commit/329d875c02079ed5826b5018645501db108bcae0))
* **compat:** stop Jade plugin error on dedicated servers ([#878](https://github.com/Indemnity83/logistics/issues/878)) ([6593a3e](https://github.com/Indemnity83/logistics/commit/6593a3ec21f4a37eca2c59d87a10c6fb57076ea9))
* **energy:** add catalyst engine JEI category ([#874](https://github.com/Indemnity83/logistics/issues/874)) ([18df47f](https://github.com/Indemnity83/logistics/commit/18df47f38ce34676e980dd5365698ad983919863))
* **energy:** show the reaction engine's recipes in JEI on Fabric ([#886](https://github.com/Indemnity83/logistics/issues/886)) ([0a58190](https://github.com/Indemnity83/logistics/commit/0a58190b0f7f7b1515987151bb88b56cc012f324))
* **energy:** stop battery and cable taking forever to mine ([#839](https://github.com/Indemnity83/logistics/issues/839)) ([8413528](https://github.com/Indemnity83/logistics/commit/8413528051a4747555e6ba8d2819b9cf38eb3a02))
* **fluids:** honor pipe transfer rates set above the default ([#885](https://github.com/Indemnity83/logistics/issues/885)) ([f0d916b](https://github.com/Indemnity83/logistics/commit/f0d916b52eaadda1591b7d2f79b483893251acbc))
* **fluids:** obscure vision and apply Nausea/Poison/Slowness in Crude Oil ([#848](https://github.com/Indemnity83/logistics/issues/848)) ([335c3f6](https://github.com/Indemnity83/logistics/commit/335c3f65022b3b8eae42a31398686b957f447067))
* **fluids:** stop over-capacity saved tank amounts from crashing on load ([#849](https://github.com/Indemnity83/logistics/issues/849)) ([6159f1a](https://github.com/Indemnity83/logistics/commit/6159f1a994c9b920d34bed07b49565a37ce69694))
* **macerator:** grind loose raw ore items into dust ([#838](https://github.com/Indemnity83/logistics/issues/838)) ([7fe8771](https://github.com/Indemnity83/logistics/commit/7fe87715615b6c6afa9a314c222add7732e9397b))
* **quarry:** stop the arm mining through lava or ignoring reappeared blocks ([#850](https://github.com/Indemnity83/logistics/issues/850)) ([9361d19](https://github.com/Indemnity83/logistics/commit/9361d191705e76eca3d7d6c6ffbe17d39e5a64e8))
* **routing:** treat requester deliveries with no inventory as fulfilled ([#847](https://github.com/Indemnity83/logistics/issues/847)) ([02f4cd8](https://github.com/Indemnity83/logistics/commit/02f4cd805438571a341fb5f5f25752aeb4b7d5ad))

## [0.8.6](https://github.com/Indemnity83/logistics/compare/mc26.1-v0.8.5...mc26.1-v0.8.6) (2026-08-07)


### Added

* **automation:** add the Transposer ([#792](https://github.com/Indemnity83/logistics/issues/792)) ([fac7307](https://github.com/Indemnity83/logistics/commit/fac7307089c3c508edea938e2688d30ebef9c969))
* **fluids:** add a dedicated fluid supplier GUI ([#811](https://github.com/Indemnity83/logistics/issues/811)) ([f89c779](https://github.com/Indemnity83/logistics/commit/f89c7797da81beb0113ac6f08e54c3b0d1973cb2))
* **fluids:** add fluid provider and supplier chassis modules ([#825](https://github.com/Indemnity83/logistics/issues/825)) ([88938dc](https://github.com/Indemnity83/logistics/commit/88938dcf4b55f40790590e1f8966df0a389656d1))
* **fluids:** add supplier partial/exact and minimum-deficit modes ([#822](https://github.com/Indemnity83/logistics/issues/822)) ([cfe8215](https://github.com/Indemnity83/logistics/commit/cfe82154c1a86e33afe9274a9131d6b56769c8db))
* **fluids:** add the fluid provider pipe ([#790](https://github.com/Indemnity83/logistics/issues/790)) ([eef7f60](https://github.com/Indemnity83/logistics/commit/eef7f60fdcf006305d95a6bab08754194fce7ce4))
* **fluids:** add the fluid supplier pipe ([#791](https://github.com/Indemnity83/logistics/issues/791)) ([174b2dc](https://github.com/Indemnity83/logistics/commit/174b2dc154d09d7a98285793aa903f3512d57911))


### Changed

* **fluids:** switch the fluid packet's frame window to a rectangle ([#823](https://github.com/Indemnity83/logistics/issues/823)) ([15e7553](https://github.com/Indemnity83/logistics/commit/15e7553f8a486b782fa6b4a1fbac90f785ec0af2))


### Fixed

* **automation:** show the sawmill's real ingredient count in JEI ([#824](https://github.com/Indemnity83/logistics/issues/824)) ([94ab19d](https://github.com/Indemnity83/logistics/commit/94ab19d4666f36e9e968b2284c4d4730396d90ff))
* **automation:** stop the sawmill from silently rejecting seeds ([#824](https://github.com/Indemnity83/logistics/issues/824)) ([94ab19d](https://github.com/Indemnity83/logistics/commit/94ab19d4666f36e9e968b2284c4d4730396d90ff))
* **fluids:** never drop fluid packets on the ground ([#805](https://github.com/Indemnity83/logistics/issues/805)) ([a97ad9f](https://github.com/Indemnity83/logistics/commit/a97ad9f5e0294601b3d5e1b05909c82cf9c69d31))
* **fluids:** stop suppliers requesting fluid/items with no room ([#804](https://github.com/Indemnity83/logistics/issues/804)) ([4f14970](https://github.com/Indemnity83/logistics/commit/4f1497047ff886b5485d1dbea30238dcd1375801))
* **sawmill:** accept single-item deliveries for batched recipes ([#827](https://github.com/Indemnity83/logistics/issues/827)) ([861b5b8](https://github.com/Indemnity83/logistics/commit/861b5b8d6f5154d84b120ccae01aa001d46819b3))

## [0.8.5](https://github.com/Indemnity83/logistics/compare/mc26.1-v0.8.4...mc26.1-v0.8.5) (2026-08-03)


### Changed

* **energy:** gate engine harvesting by tool tier ([#784](https://github.com/Indemnity83/logistics/issues/784)) ([1d3cb5c](https://github.com/Indemnity83/logistics/commit/1d3cb5cf400634098b275634901d7b1ac28f865a))


### Fixed

* **energy:** correct cable connections to engines ([#801](https://github.com/Indemnity83/logistics/issues/801)) ([d260edc](https://github.com/Indemnity83/logistics/commit/d260edc5f40eff05832c45945ee556520172e38d))
* **quarry:** fix immediate crash on published Fabric builds ([#798](https://github.com/Indemnity83/logistics/issues/798)) ([c59c02f](https://github.com/Indemnity83/logistics/commit/c59c02faa288325b46223c5b9ce222bea4baa270)) — thanks @WerWebWer

### New Contributors

* @WerWebWer made their first contribution in #798

## [0.8.4](https://github.com/Indemnity83/logistics/compare/mc26.1-v0.8.3...mc26.1-v0.8.4) (2026-07-24)


### Added

* **energy:** add the Fuel Engine ([#759](https://github.com/Indemnity83/logistics/issues/759)) ([ac1b2eb](https://github.com/Indemnity83/logistics/commit/ac1b2ebc63bd387d4dfd9281916152eb26862f77))
* **energy:** add the Magmatic Engine ([#776](https://github.com/Indemnity83/logistics/issues/776)) ([fd98c4e](https://github.com/Indemnity83/logistics/commit/fd98c4e0ee64fae9189ebb2f0d653543e332a800))
* **energy:** add the Reaction Engine ([#777](https://github.com/Indemnity83/logistics/issues/777)) ([9e4b602](https://github.com/Indemnity83/logistics/commit/9e4b602d8274cb47dd57484723cb6e4343b7b01a))
* **energy:** add the Steam Engine ([#765](https://github.com/Indemnity83/logistics/issues/765)) ([22a06fd](https://github.com/Indemnity83/logistics/commit/22a06fd6898816e1a46afc5157d27db1d2ea15ce))


### Changed

* **core:** retexture the bronze and tin metal blocks and items ([#766](https://github.com/Indemnity83/logistics/issues/766)) ([67597e5](https://github.com/Indemnity83/logistics/commit/67597e5b92cc4787c9484a33717f6e73f801b346))
* **energy:** restyle the Stirling Engine GUI ([#763](https://github.com/Indemnity83/logistics/issues/763)) ([8a00c4a](https://github.com/Indemnity83/logistics/commit/8a00c4a4788bab70620b2204e245108f55746c21))
* **fluids:** restyle the fluid pipe textures ([#740](https://github.com/Indemnity83/logistics/issues/740)) ([a49c103](https://github.com/Indemnity83/logistics/commit/a49c1035f5c4391609d108e067369e9171486cdc))


### Fixed

* **pipes:** fix missing pixel in logistics power junction animation ([#637](https://github.com/Indemnity83/logistics/issues/637)) ([b3fe7df](https://github.com/Indemnity83/logistics/commit/b3fe7df82e95ab24c0c65ad526d978395a1578aa))

## [0.8.3](https://github.com/Indemnity83/logistics/compare/mc26.1-v0.8.2...mc26.1-v0.8.3) (2026-07-20)


### Added

* **automation:** add the refinery ([#714](https://github.com/Indemnity83/logistics/issues/714)) ([a42e0d0](https://github.com/Indemnity83/logistics/commit/a42e0d0893f2b6ebedbec65410e6390872bbec22))
* **automation:** add the sequential fabricator ([#719](https://github.com/Indemnity83/logistics/issues/719)) ([f208f30](https://github.com/Indemnity83/logistics/commit/f208f3009e26a74a03cfe99ee866c02bfdf52a05))
* **automation:** add the sequential fabricator ([#719](https://github.com/Indemnity83/logistics/issues/719)) ([f208f30](https://github.com/Indemnity83/logistics/commit/f208f3009e26a74a03cfe99ee866c02bfdf52a05))
* **automation:** fabricate chipsets in the sequential fabricator ([#720](https://github.com/Indemnity83/logistics/issues/720)) ([0646bfb](https://github.com/Indemnity83/logistics/commit/0646bfbeb743899c84d765ea08b0d87ad0fe0d7b))
* **automation:** fabricate chipsets in the sequential fabricator ([#720](https://github.com/Indemnity83/logistics/issues/720)) ([0646bfb](https://github.com/Indemnity83/logistics/commit/0646bfbeb743899c84d765ea08b0d87ad0fe0d7b))
* **automation:** make machine tuning configurable per machine ([#713](https://github.com/Indemnity83/logistics/issues/713)) ([edfd279](https://github.com/Indemnity83/logistics/commit/edfd27979952fc47b90f21e8d4edff79217ff580))
* **compat:** add Jade HUD support for refinery and sequential fabricator ([#744](https://github.com/Indemnity83/logistics/issues/744)) ([9df1b69](https://github.com/Indemnity83/logistics/commit/9df1b69a39a304ba473f9acd6f73312302ec02a4))
* **core:** add a copper nugget ([#722](https://github.com/Indemnity83/logistics/issues/722)) ([79bebbb](https://github.com/Indemnity83/logistics/commit/79bebbb026fa80c849b5a2793e32f8077a23e343))
* **crafting:** add tin, rubber, amethyst, and echo valves ([701fbee](https://github.com/Indemnity83/logistics/commit/701fbeee8220a89855ace7129243e649bffbe6ac))
* **crafting:** rework the valve lineup with a bench recipe ([#727](https://github.com/Indemnity83/logistics/issues/727)) ([701fbee](https://github.com/Indemnity83/logistics/commit/701fbeee8220a89855ace7129243e649bffbe6ac))
* **crafting:** show sequential fabricator recipes in JEI ([#745](https://github.com/Indemnity83/logistics/issues/745)) ([be059f3](https://github.com/Indemnity83/logistics/commit/be059f36cfdfe763567525cad0c4fa7cd2a85aff))
* **energy:** add natural and synthetic polymers for rubber ([#716](https://github.com/Indemnity83/logistics/issues/716)) ([7678f48](https://github.com/Indemnity83/logistics/commit/7678f48cdcf29a86aac0dd6dcfa83c1d65b6e8a8))
* **fluids:** add bio fuel and fuel oil fluids ([a42e0d0](https://github.com/Indemnity83/logistics/commit/a42e0d0893f2b6ebedbec65410e6390872bbec22))
* **fluids:** add tar as an alternative fluid pipe sealant ([#715](https://github.com/Indemnity83/logistics/issues/715)) ([4c5c82b](https://github.com/Indemnity83/logistics/commit/4c5c82b21d38f9c73dac3070f27f04cf32941cf5))
* **routing:** add chipset crafting alternatives for pipes and modules ([d4e9029](https://github.com/Indemnity83/logistics/commit/d4e90299e1e0c20794255bd3a4eaa47b8243f1f1))
* **ui:** split creative menu into domain tabs ([#738](https://github.com/Indemnity83/logistics/issues/738)) ([878b34e](https://github.com/Indemnity83/logistics/commit/878b34ea06c1308732be4ef09b7dd0cf26f867d5))


### Changed

* **crafting:** craft valves from quartz, redstone, and a base material ([701fbee](https://github.com/Indemnity83/logistics/commit/701fbeee8220a89855ace7129243e649bffbe6ac))
* **crafting:** restyle every valve with a distinct electron-tube texture ([701fbee](https://github.com/Indemnity83/logistics/commit/701fbeee8220a89855ace7129243e649bffbe6ac))
* **macerator:** recycle netherite dust to and from ingots ([#734](https://github.com/Indemnity83/logistics/issues/734)) ([0b0ecca](https://github.com/Indemnity83/logistics/commit/0b0eccad2d829047be9897dd0e73f730b8cf6df8))
* **routing:** rework logistics pipe recipes with chipset alternatives ([#721](https://github.com/Indemnity83/logistics/issues/721)) ([d4e9029](https://github.com/Indemnity83/logistics/commit/d4e90299e1e0c20794255bd3a4eaa47b8243f1f1))
* **routing:** use a copper nugget in the blank module recipe ([79bebbb](https://github.com/Indemnity83/logistics/commit/79bebbb026fa80c849b5a2793e32f8077a23e343))


### Removed

* **crafting:** drop the wooden and ender valves ([701fbee](https://github.com/Indemnity83/logistics/commit/701fbeee8220a89855ace7129243e649bffbe6ac))


### Fixed

* **automation:** make the refinery and sequential fabricator harvestable ([49b5329](https://github.com/Indemnity83/logistics/commit/49b5329f75966df2693ad0bc3fe24b279ec58aa2))
* **automation:** show machine recipes in JEI on multiplayer clients ([#735](https://github.com/Indemnity83/logistics/issues/735)) ([b51ea1e](https://github.com/Indemnity83/logistics/commit/b51ea1ed98403a8c9123290aa1f7e7932065bca9))
* **compat:** register Crucible with NeoForge Jade plugin ([#744](https://github.com/Indemnity83/logistics/issues/744)) ([9df1b69](https://github.com/Indemnity83/logistics/commit/9df1b69a39a304ba473f9acd6f73312302ec02a4))
* **compat:** show values in the Jade HUD instead of only labels ([#760](https://github.com/Indemnity83/logistics/issues/760)) ([9759af8](https://github.com/Indemnity83/logistics/commit/9759af8099465b5012741e7396ae6b186b78228f))
* **neoforge:** let pipes and cables interact with the refinery and sawmill ([#733](https://github.com/Indemnity83/logistics/issues/733)) ([5c9cded](https://github.com/Indemnity83/logistics/commit/5c9cded467f283664d6901229279d8fc40ddd4cc))

## [0.8.2](https://github.com/Indemnity83/logistics/compare/mc26.1-v0.8.1...mc26.1-v0.8.2) (2026-07-05)


### Added

* **automation:** add pulped biomass from the sawmill ([#660](https://github.com/Indemnity83/logistics/issues/660)) ([3c24a11](https://github.com/Indemnity83/logistics/commit/3c24a1189e18acc76d87a21952624d766c326fc0))
* **automation:** add the alloy smelter ([#656](https://github.com/Indemnity83/logistics/issues/656)) ([7812be1](https://github.com/Indemnity83/logistics/commit/7812be1a52c9212d3d6b480b47333f7eb673422b))
* **automation:** add the crucible ([#679](https://github.com/Indemnity83/logistics/issues/679)) ([33527d7](https://github.com/Indemnity83/logistics/commit/33527d7b894fad253c76898ebd9de6e67364c562))
* **fluids:** add custom fluids and buckets for the Magma Crucible ([539e547](https://github.com/Indemnity83/logistics/commit/539e5474f65b219ae2e303e8c3aa8e4a6c0c196e))
* **fluids:** light-emitting fluids glow in pipes and tanks ([#695](https://github.com/Indemnity83/logistics/issues/695)) ([90060e5](https://github.com/Indemnity83/logistics/commit/90060e514d8c065dd73c278e76367d90ab5967f5))
* **fluids:** show machine fluid tanks in the Jade HUD ([#703](https://github.com/Indemnity83/logistics/issues/703)) ([714856a](https://github.com/Indemnity83/logistics/commit/714856a0185188e88114f8d525ddf21f7f97c78c))
* **worldgen:** add bog earth and peat fuel ([#658](https://github.com/Indemnity83/logistics/issues/658)) ([f3c4db8](https://github.com/Indemnity83/logistics/commit/f3c4db8b863cccf3e77a2545d09925af2a47e732))
* **worldgen:** add crude oil and the oil chain ([#690](https://github.com/Indemnity83/logistics/issues/690)) ([1367bf8](https://github.com/Indemnity83/logistics/commit/1367bf85bea234c38124ed0bd1f71fe10bc88cfa))


### Fixed

* **automation:** reject non-finite recipe experience values ([1596dd6](https://github.com/Indemnity83/logistics/commit/1596dd6d1feeff3285670ee99ca53f881b032760))
* **automation:** sync machine progress and energy so bars can't overflow ([#694](https://github.com/Indemnity83/logistics/issues/694)) ([eedda9a](https://github.com/Indemnity83/logistics/commit/eedda9a298db2ef3f42b91ac82b97c2ab29e094a))
* **ci:** stop Sentry release failing on cross-branch set-commits ([#683](https://github.com/Indemnity83/logistics/issues/683)) ([fa67d0c](https://github.com/Indemnity83/logistics/commit/fa67d0cbb5cfec68b574a32f552922cd5f2f743d))
* **ci:** unblock release publishing broken by Sentry set-commits ([#678](https://github.com/Indemnity83/logistics/issues/678)) ([1d65ba9](https://github.com/Indemnity83/logistics/commit/1d65ba92cbe1e2ced5f7994f0cdc4026a1a5573f))
* **compat:** add missing Jade fluid-pipe config translation ([#701](https://github.com/Indemnity83/logistics/issues/701)) ([6a1b420](https://github.com/Indemnity83/logistics/commit/6a1b4203453b1f3b598a021fd4a97c85a3c7237c))
* **fluids:** render custom fluids in the look-at HUD ([#706](https://github.com/Indemnity83/logistics/issues/706)) ([5340a0c](https://github.com/Indemnity83/logistics/commit/5340a0cfc8ac1737a1a493505fb0060bcdda02cf))
* **fluids:** stop drained pipes rendering a checkerboard ([#696](https://github.com/Indemnity83/logistics/issues/696)) ([e8e6047](https://github.com/Indemnity83/logistics/commit/e8e6047513088152e8c0b5cff6d83e5a5f70d1df))
* **fluids:** stop fluid extractor pipes from connecting to each other ([#692](https://github.com/Indemnity83/logistics/issues/692)) ([ab14375](https://github.com/Indemnity83/logistics/commit/ab143754a718ad9f76c30c1599ddd66d02e4eb2b))
* **pump:** make the pump tank output-only ([#693](https://github.com/Indemnity83/logistics/issues/693)) ([8eed87f](https://github.com/Indemnity83/logistics/commit/8eed87f4235765ec723aa01565dd85bdc389038e))
* **routing:** drop the failed order's index entry on delivery retry ([5798527](https://github.com/Indemnity83/logistics/commit/5798527bbb1c6a6edcc0d07cf451f4b419f6fccc))

## [0.8.1](https://github.com/Indemnity83/logistics/compare/mc26.1-v0.8.0...mc26.1-v0.8.1) (2026-06-30)


### Removed

* **core:** drop the unused sturdy casing ([#650](https://github.com/Indemnity83/logistics/issues/650)) ([19512ce](https://github.com/Indemnity83/logistics/commit/19512ce69819c6793d64d9cc2304808a10c801ec))


### Fixed

* **pump:** accept power from any energy source ([#651](https://github.com/Indemnity83/logistics/issues/651)) ([c8e8609](https://github.com/Indemnity83/logistics/commit/c8e8609826004929ad3d34580860710c9fc99fc6))

## [0.8.0](https://github.com/Indemnity83/logistics/compare/mc26.1-v0.7.4...mc26.1-v0.8.0) (2026-06-29)


### ⚠ BREAKING CHANGES

* **energy:** Cables and batteries no longer power extraction pipes or the Fluid Pump. Only a directly-adjacent engine can power them. Existing setups that fed pipes through cables/batteries will stop working — place an engine against the pipe instead.
* **energy:** Batteries no longer power a logistics network directly. A network is powered only through a Power Junction — place one between your power source (cables/batteries) and the network. Existing battery-on-a-pipe setups stop working until a Power Junction is added.

### Added

* **core:** add opt-in sanitized crash reporting ([#633](https://github.com/Indemnity83/logistics/issues/633)) ([24b9b95](https://github.com/Indemnity83/logistics/commit/24b9b95ce73d5f61567c699d3fa7ebc7427e3e27))
* **core:** drop niter from the breeze ([#644](https://github.com/Indemnity83/logistics/issues/644)) ([923fb5f](https://github.com/Indemnity83/logistics/commit/923fb5f46dd2941000507d86eefe9a7b23d208c6))
* **crafting:** craft gunpowder from coal, sulfur, and niter dust ([2936332](https://github.com/Indemnity83/logistics/commit/2936332ceda17d712c1a63c0b3ea08340b8f1202))
* **energy:** add the power junction ([#612](https://github.com/Indemnity83/logistics/issues/612)) ([822c174](https://github.com/Indemnity83/logistics/commit/822c17427cbe1d44df113c1e408133b64d5c93ce))
* **macerator:** add chance byproducts to ore processing ([#643](https://github.com/Indemnity83/logistics/issues/643)) ([2936332](https://github.com/Indemnity83/logistics/commit/2936332ceda17d712c1a63c0b3ea08340b8f1202))
* **macerator:** add recycling recipes for common blocks ([2936332](https://github.com/Indemnity83/logistics/commit/2936332ceda17d712c1a63c0b3ea08340b8f1202))
* **macerator:** add Sulfur Dust, Quicksilver, and Niter items ([2936332](https://github.com/Indemnity83/logistics/commit/2936332ceda17d712c1a63c0b3ea08340b8f1202))
* **macerator:** macerate breeze rods into wind charges ([2936332](https://github.com/Indemnity83/logistics/commit/2936332ceda17d712c1a63c0b3ea08340b8f1202))
* **macerator:** macerate logs and planks into sawdust and recycle wooden tools ([2936332](https://github.com/Indemnity83/logistics/commit/2936332ceda17d712c1a63c0b3ea08340b8f1202))
* **macerator:** recycle diamond tools and armor into diamonds ([2936332](https://github.com/Indemnity83/logistics/commit/2936332ceda17d712c1a63c0b3ea08340b8f1202))
* **sawmill:** add a recipe book to the sawmill GUI ([#642](https://github.com/Indemnity83/logistics/issues/642)) ([0d1aae2](https://github.com/Indemnity83/logistics/commit/0d1aae2312db3d1f83ac02d2a725213062b2f9ce))
* **sawmill:** add wood processing ([#580](https://github.com/Indemnity83/logistics/issues/580)) ([338dacd](https://github.com/Indemnity83/logistics/commit/338dacd1d22ef53a6cf702860d6aac75cb6ca491))
* **sawmill:** show recipes in JEI and details in the Jade HUD ([#613](https://github.com/Indemnity83/logistics/issues/613)) ([e608ead](https://github.com/Indemnity83/logistics/commit/e608eade5a14e3b97736c5384b01e26bcb9c59e8))


### Changed

* **common:** cache pipe and cable collision shapes ([#631](https://github.com/Indemnity83/logistics/issues/631)) ([ec78fde](https://github.com/Indemnity83/logistics/commit/ec78fde08d05c9a5f955b5699f2cce5aab85e06b))
* **core:** streamline world loading by dropping legacy save migrations ([#586](https://github.com/Indemnity83/logistics/issues/586)) ([ca013d1](https://github.com/Indemnity83/logistics/commit/ca013d182794a140fedf80c394249c506639f4c4))
* **crafting:** require a bronze gear in the machine frame ([#610](https://github.com/Indemnity83/logistics/issues/610)) ([d4aa502](https://github.com/Indemnity83/logistics/commit/d4aa50226ff11a782e16b5f3f23763df3cdfafcb))
* **crafting:** yield one marker per craft ([#607](https://github.com/Indemnity83/logistics/issues/607)) ([4dccee1](https://github.com/Indemnity83/logistics/commit/4dccee1b2755dd752a80087ce95c4032b59097d6))
* **energy:** power extraction pipes only from a direct engine ([#641](https://github.com/Indemnity83/logistics/issues/641)) ([4cec664](https://github.com/Indemnity83/logistics/commit/4cec664ccf257bbb52c9ecedc6dcda20d72fede7))
* **fluids:** speed up fluid split allocation ([#623](https://github.com/Indemnity83/logistics/issues/623)) ([318c0ba](https://github.com/Indemnity83/logistics/commit/318c0bacc532b30a605f6211d5c42f2c2159294f))
* **macerator:** ore→dust recipes now drop a chance byproduct dust ([2936332](https://github.com/Indemnity83/logistics/commit/2936332ceda17d712c1a63c0b3ea08340b8f1202))
* **pipes:** raise pipe blast resistance to match glass ([#618](https://github.com/Indemnity83/logistics/issues/618)) ([4e4aa99](https://github.com/Indemnity83/logistics/commit/4e4aa9969441b7a1c296e6e424570e5c1af9cea9))
* **quarry:** restyle with the shared machine look ([#582](https://github.com/Indemnity83/logistics/issues/582)) ([9e0db0e](https://github.com/Indemnity83/logistics/commit/9e0db0e5a81ac402a3323628ab06f560487f2fa1))
* **routing:** cache next-hop routes per destination ([#632](https://github.com/Indemnity83/logistics/issues/632)) ([366c0cb](https://github.com/Indemnity83/logistics/commit/366c0cb3fa81dfed9c1fbd54945f680ada429c12))
* **sawmill:** match the energy buffer to the other machines ([#647](https://github.com/Indemnity83/logistics/issues/647)) ([e34808d](https://github.com/Indemnity83/logistics/commit/e34808df6b494c8ad881034a45f59162d1da72e3))
* **ui:** refresh the kiln, macerator, and sawmill GUIs ([#646](https://github.com/Indemnity83/logistics/issues/646)) ([f3bfd18](https://github.com/Indemnity83/logistics/commit/f3bfd1873b6519fc50d073946c962a07dbd96197))
* **worldgen:** tin ore drops one raw tin ([#608](https://github.com/Indemnity83/logistics/issues/608)) ([2c7b8cc](https://github.com/Indemnity83/logistics/commit/2c7b8cc26f020e804fb32875b8b3a82eafcf43b3))


### Fixed

* **automation:** let quarry markers connect through solid blocks ([#581](https://github.com/Indemnity83/logistics/issues/581)) ([05e6d5b](https://github.com/Indemnity83/logistics/commit/05e6d5bed852ba4ada35d26d02899d510477bf23))
* **automation:** pause recipes until byproducts have space ([#597](https://github.com/Indemnity83/logistics/issues/597)) ([315e068](https://github.com/Indemnity83/logistics/commit/315e06850e47dfdb57c28d70b223a35f644c1f3e))
* **core:** restore valve and quartz crystal recipes ([#600](https://github.com/Indemnity83/logistics/issues/600)) ([e89c6b7](https://github.com/Indemnity83/logistics/commit/e89c6b7ad25feeda21d30afd49fdb93653e2bf0f))
* **energy:** drop the creative sink when broken ([#616](https://github.com/Indemnity83/logistics/issues/616)) ([f41a53a](https://github.com/Indemnity83/logistics/commit/f41a53a8bc3b680a92f764e463fa10854ceb6371))
* **fluids:** drop fluid pipes and glass tank when broken ([#614](https://github.com/Indemnity83/logistics/issues/614)) ([29d47d0](https://github.com/Indemnity83/logistics/commit/29d47d0e062fb40fa5cdd23b91301f3d6eae693c))
* **fluids:** show correct fill level on tank and pipe HUDs ([#619](https://github.com/Indemnity83/logistics/issues/619)) ([a3cb638](https://github.com/Indemnity83/logistics/commit/a3cb6381959d4ca81b48101c1b0fc70c3fcb0c21))
* **kiln:** accept energy from the power network ([#602](https://github.com/Indemnity83/logistics/issues/602)) ([44b64c1](https://github.com/Indemnity83/logistics/commit/44b64c15a644780494723b2ba1368303dec8acb6))
* **kiln:** bank smelting XP and pay it out like a furnace ([#605](https://github.com/Indemnity83/logistics/issues/605)) ([e925931](https://github.com/Indemnity83/logistics/commit/e925931f34ff863feec3631d660adb730ac23a9b))
* **kiln:** mine with the correct pickaxe tier ([#601](https://github.com/Indemnity83/logistics/issues/601)) ([a331f65](https://github.com/Indemnity83/logistics/commit/a331f658c39e3db0e64ff91718528bb004a466f4))
* **macerator:** bank maceration XP and pay it out like a furnace ([e925931](https://github.com/Indemnity83/logistics/commit/e925931f34ff863feec3631d660adb730ac23a9b))
* **macerator:** grant XP for macerating ancient debris ([#624](https://github.com/Indemnity83/logistics/issues/624)) ([3ed6bf0](https://github.com/Indemnity83/logistics/commit/3ed6bf05765bc0eddacda2bde6d97759e848e895))
* **macerator:** restore the JEI integration ([#599](https://github.com/Indemnity83/logistics/issues/599)) ([3efb268](https://github.com/Indemnity83/logistics/commit/3efb26869373f572a85ba438c8ab59416467f24c))
* **pump:** clamp the fluid pump search radius ([#625](https://github.com/Indemnity83/logistics/issues/625)) ([737e820](https://github.com/Indemnity83/logistics/commit/737e82070416945fa187cdfe083f05f27d3fb35e))
* **pump:** match the fluid pump's top to the other machines ([#561](https://github.com/Indemnity83/logistics/issues/561)) ([47c953c](https://github.com/Indemnity83/logistics/commit/47c953caa1c9a90a1a156d244c633c68de19e7e0))
* **pump:** mine the fluid pump with the correct pickaxe tier ([29d47d0](https://github.com/Indemnity83/logistics/commit/29d47d0e062fb40fa5cdd23b91301f3d6eae693c))
* **quarry:** give the laser quarry frame a display name ([#617](https://github.com/Indemnity83/logistics/issues/617)) ([76bdcfb](https://github.com/Indemnity83/logistics/commit/76bdcfbfc54c09d9b126a23555d95a7267969bba))
* **routing:** keep chassis modules when a pipe explodes ([#629](https://github.com/Indemnity83/logistics/issues/629)) ([1bcefc3](https://github.com/Indemnity83/logistics/commit/1bcefc3376c9c41cc770bae6f98529d93eff65ae))
* **routing:** refresh neighbor pipe arms when markings change ([#606](https://github.com/Indemnity83/logistics/issues/606)) ([5ccc871](https://github.com/Indemnity83/logistics/commit/5ccc871b92d7d605eb9e5b49f0f0c6f9a4da41ee))
* **sawmill:** add the missing crafting recipe ([#609](https://github.com/Indemnity83/logistics/issues/609)) ([6da660a](https://github.com/Indemnity83/logistics/commit/6da660ab318ce8d24b0b00accab3c4fe360ec6a2))
* **sawmill:** mine with the correct pickaxe tier ([e608ead](https://github.com/Indemnity83/logistics/commit/e608eade5a14e3b97736c5384b01e26bcb9c59e8))

## [0.7.4](https://github.com/Indemnity83/logistics/compare/mc26.1-v0.7.3...mc26.1-v0.7.4) (2026-06-21)


### Added

* **automation:** add configurable laser quarry chunk loading ([#536](https://github.com/Indemnity83/logistics/issues/536)) ([11fcba1](https://github.com/Indemnity83/logistics/commit/11fcba15bf5419ad45eeffde80e6463693c1f273)) — thanks @floralpetals
* **core:** grant ore XP when macerating metal and apatite ores ([#555](https://github.com/Indemnity83/logistics/issues/555)) ([c48dd10](https://github.com/Indemnity83/logistics/commit/c48dd104b949a90d7d5f05928ac4baa7847107fe))


### Fixed

* **automation:** add missing macerator recipes for obsidian, netherite, and metal blocks ([#554](https://github.com/Indemnity83/logistics/issues/554)) ([f710856](https://github.com/Indemnity83/logistics/commit/f710856f3b0d57e12ada9a59b23aac25c206de93))
* **energy:** fix crash when cables power machines from other mods ([#556](https://github.com/Indemnity83/logistics/issues/556)) ([189e427](https://github.com/Indemnity83/logistics/commit/189e42700c5c674484795c0d2c62508c54f2b867))

### New Contributors
* @floralpetals made their first contribution in #536

## [0.7.3](https://github.com/Indemnity83/logistics/compare/mc26.1-v0.7.2...mc26.1-v0.7.3) (2026-06-18)


### Added

* **api:** add loader-independent API for cross-mod fluid integration ([#516](https://github.com/Indemnity83/logistics/issues/516)) ([2a23258](https://github.com/Indemnity83/logistics/commit/2a23258e3f559f84a66058efb72a0f12179ad2c1))
* **pipes:** add copper fluid pipe oxidation and fluid pipe marking ([#520](https://github.com/Indemnity83/logistics/issues/520)) ([034fe89](https://github.com/Indemnity83/logistics/commit/034fe892e1273ce3734ba74dc656c5fe338e88a8))
* **pipes:** add fluid pipes, tanks, and powered fluid extraction ([#511](https://github.com/Indemnity83/logistics/issues/511)) ([16c277e](https://github.com/Indemnity83/logistics/commit/16c277e8c415b299bb682db6228b34d049ca90d2))
* **pipes:** add fluid pump ([#537](https://github.com/Indemnity83/logistics/issues/537)) ([9f43734](https://github.com/Indemnity83/logistics/commit/9f43734c9db39300fa411154a11915bc98e87214))


### Fixed

* **compat:** hide fluid extractor energy in Jade ([#533](https://github.com/Indemnity83/logistics/issues/533)) ([cf1f95a](https://github.com/Indemnity83/logistics/commit/cf1f95a33a5b9190d76b4bda5391fa65a6fa88e4))
* **pipes:** fix fluid pipe drain flicker ([#532](https://github.com/Indemnity83/logistics/issues/532)) ([0dd55f5](https://github.com/Indemnity83/logistics/commit/0dd55f5b9b4c4375bb28c7e66c892d5c657b1596))
* **ui:** fix glass tank capacity overlay with held items ([#534](https://github.com/Indemnity83/logistics/issues/534)) ([8f35118](https://github.com/Indemnity83/logistics/commit/8f3511899036896bf47c8d34ad26ce8d55060fe9))

## [0.7.2](https://github.com/Indemnity83/logistics/compare/mc26.1-v0.7.1...mc26.1-v0.7.2) (2026-06-15)


### Fixed

* **energy:** add crafting recipe for battery ([#510](https://github.com/Indemnity83/logistics/issues/510)) ([b38e601](https://github.com/Indemnity83/logistics/commit/b38e6010bc40d8dd8faf8387bd3e5ab9f0c06e62))

## [0.7.1](https://github.com/Indemnity83/logistics/compare/mc26.1-v0.7.0...mc26.1-v0.7.1) (2026-06-11)


### Added

* **automation:** show laser quarry status in the Jade HUD ([#498](https://github.com/Indemnity83/logistics/issues/498)) ([d37ee33](https://github.com/Indemnity83/logistics/commit/d37ee3399d40d17f3349d10a4da13d209278264e))
* **automation:** show macerator and kiln progress in the Jade HUD ([#499](https://github.com/Indemnity83/logistics/issues/499)) ([a9a51f1](https://github.com/Indemnity83/logistics/commit/a9a51f122ffbd384711ea521a2da089cea34df0d))
* **compat:** integrate Jade and remove the built-in probe ([49fb320](https://github.com/Indemnity83/logistics/commit/49fb3207c587bb8dab812ca3711ef03359b61f44))
* **energy:** show power diagnostics in the Jade HUD ([#497](https://github.com/Indemnity83/logistics/issues/497)) ([5fea7b3](https://github.com/Indemnity83/logistics/commit/5fea7b31f9ef23c57265692b596b2aad957975ea))
* **pipes:** show pipe contents in the Jade HUD ([#500](https://github.com/Indemnity83/logistics/issues/500)) ([3853f6c](https://github.com/Indemnity83/logistics/commit/3853f6c77a4557af75ca3f8d5dd17be632358f1b))
* **pipes:** show pipe module status in the Jade HUD ([#493](https://github.com/Indemnity83/logistics/issues/493)) ([17def1c](https://github.com/Indemnity83/logistics/commit/17def1ce0e9dd334187ace4b3bdc8452c8d84226))


### Fixed

* **pipes:** apply config changes to modules installed in a chassis ([#504](https://github.com/Indemnity83/logistics/issues/504)) ([09d3b02](https://github.com/Indemnity83/logistics/commit/09d3b02e03737c2f1ca0a4ec3b4d3864f9f79e98)), closes [#494](https://github.com/Indemnity83/logistics/issues/494)

## [0.7.0](https://github.com/Indemnity83/logistics/compare/mc26.1-v0.6.3...mc26.1-v0.7.0) (2026-06-09)


### ⚠ BREAKING CHANGES

* **energy:** logistics pipe operations consume power ([#465](https://github.com/Indemnity83/logistics/issues/465))
* **energy:** Logistics pipes now require power from an adjacent Battery. Existing networks stop routing/supplying/crafting — and drop items already in transit — until a charged Battery is connected.

### Added

* **energy:** add a Battery block to power logistics networks ([aff4ba6](https://github.com/Indemnity83/logistics/commit/aff4ba65fad56cfc5c966d268bc5e0f050468ad8))
* **energy:** logistics pipe operations consume power ([#465](https://github.com/Indemnity83/logistics/issues/465)) ([aff4ba6](https://github.com/Indemnity83/logistics/commit/aff4ba65fad56cfc5c966d268bc5e0f050468ad8))
* **ui:** color logistics pipes green when powered, red when not ([#469](https://github.com/Indemnity83/logistics/issues/469)) ([b9162d3](https://github.com/Indemnity83/logistics/commit/b9162d35f98f85d5a6e78c5d6a302bc7a663a7d1))


### Fixed

* **automation:** stop the macerator from trying to load other mods' recipes ([#473](https://github.com/Indemnity83/logistics/issues/473)) ([df69b72](https://github.com/Indemnity83/logistics/commit/df69b72163012a7077fa7dbb1759e38a99d95fa3))
* **energy:** make engines visibly change color with heat stage ([#482](https://github.com/Indemnity83/logistics/issues/482)) ([d3c2463](https://github.com/Indemnity83/logistics/commit/d3c24631ae7e5931181882107658ed8ed689fc7a))

## [0.6.3](https://github.com/Indemnity83/logistics/compare/mc26.1-v0.6.2...mc26.1-v0.6.3) (2026-06-05)


### Fixed

* **energy:** transport energy through cables on NeoForge ([#462](https://github.com/Indemnity83/logistics/issues/462)) ([af7bd3f](https://github.com/Indemnity83/logistics/commit/af7bd3f540b9819f25db51c31378885477371954))

## [0.6.2](https://github.com/Indemnity83/logistics/compare/mc26.1-v0.6.1...mc26.1-v0.6.2) (2026-06-04)


### Fixed

* **neoforge:** fix startup crash when JEI is on the classpath ([#453](https://github.com/Indemnity83/logistics/issues/453)) ([d45efef](https://github.com/Indemnity83/logistics/commit/d45efefa3640efd8154c5984492be0fadfe67d3c))


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
* Fixed NeoForge and multi-loader content issues, including JEI Macerator recipe visibility, marking fluid recipe separation, and missing `META-INF` service files. [#382](https://github.com/Indemnity83/logistics/issues/382), [#390](https://github.com/Indemnity83/logistics/issues/390), [affc30f](https://github.com/Indemnity83/logistics/commit/affc30feb49ecec0597965ea41717caa81bc7f06) — thanks @AdolfoCarneiro
* Fixed power cable compilation errors for Minecraft 26.1. [#357](https://github.com/Indemnity83/logistics/issues/357)
* Preserved item components in filter pipe slots across save/reload. [#386](https://github.com/Indemnity83/logistics/issues/386)

### Refactorings

* Reworked the project for multi-loader support, including NeoForge groundwork, loader-agnostic bootstrap flow, service-based platform access, cleaner module boundaries, and build configuration updates. [#306](https://github.com/Indemnity83/logistics/issues/306), [#318](https://github.com/Indemnity83/logistics/issues/318), [#320](https://github.com/Indemnity83/logistics/issues/320), [#341](https://github.com/Indemnity83/logistics/issues/341), [#342](https://github.com/Indemnity83/logistics/issues/342), [#343](https://github.com/Indemnity83/logistics/issues/343), [#344](https://github.com/Indemnity83/logistics/issues/344), [#347](https://github.com/Indemnity83/logistics/issues/347), [#348](https://github.com/Indemnity83/logistics/issues/348), [#360](https://github.com/Indemnity83/logistics/issues/360), [#361](https://github.com/Indemnity83/logistics/issues/361) — thanks @AdolfoCarneiro
* Introduced loader-agnostic storage, energy, fluid, fuel, item matching, and client model abstractions. [#340](https://github.com/Indemnity83/logistics/issues/340), [#349](https://github.com/Indemnity83/logistics/issues/349), [#351](https://github.com/Indemnity83/logistics/issues/351), [#356](https://github.com/Indemnity83/logistics/issues/356), [#364](https://github.com/Indemnity83/logistics/issues/364), [#389](https://github.com/Indemnity83/logistics/issues/389), [#400](https://github.com/Indemnity83/logistics/issues/400)
* Cleaned up common code organization and removed remaining Fabric-specific dependencies/imports from shared sources. [#345](https://github.com/Indemnity83/logistics/issues/345), [#350](https://github.com/Indemnity83/logistics/issues/350), [#352](https://github.com/Indemnity83/logistics/issues/352), [#355](https://github.com/Indemnity83/logistics/issues/355)

### Testing

* Added Fabric/NeoForge test infrastructure, NeoForge ServiceLoader and energy adapter tests, component coverage, and baseline coverage reporting. [#377](https://github.com/Indemnity83/logistics/issues/377), [#402](https://github.com/Indemnity83/logistics/issues/402), [f6c6a35](https://github.com/Indemnity83/logistics/commit/f6c6a35c2404c74519b6130590c6b62c0428d14b)

### New Contributors
* @AdolfoCarneiro made their first contribution in #306

## [0.5.6](https://github.com/Indemnity83/logistics/compare/mc26.1-v0.5.5...mc26.1-v0.5.6) (2026-05-02)


### Bug Fixes

* correct lever and dust placement on engine blocks ([#300](https://github.com/Indemnity83/logistics/issues/300)) ([6f912ca](https://github.com/Indemnity83/logistics/commit/6f912ca94b19b8db514504c9d8e119aff371bbe4))
* correct supplier module UI targeting ([#295](https://github.com/Indemnity83/logistics/issues/295)) ([128358a](https://github.com/Indemnity83/logistics/commit/128358af0cd2b00446926c12ab76e95f9d764edd)) — thanks @ZayshaaCodes
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

* add ender dust macerator recipe ([#282](https://github.com/Indemnity83/logistics/issues/282)) ([0323f36](https://github.com/Indemnity83/logistics/commit/0323f3649230ec8787208a16a62ae163124e461b)) — thanks @ZayshaaCodes
* use RegistryOps to fix enchanted item crash ([#283](https://github.com/Indemnity83/logistics/issues/283)) ([967be91](https://github.com/Indemnity83/logistics/commit/967be91480f14b35090fa8f3b651af2511704a37)) — thanks @ZayshaaCodes

### New Contributors
* @ZayshaaCodes made their first contribution in #282

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
