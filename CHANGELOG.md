# Changelog

## [0.8.8](https://github.com/Indemnity83/logistics/compare/mc1.21.1-v0.8.7...mc1.21.1-v0.8.8) (2026-09-05)


### Changed

* **routing:** serve pipe shapes from the connection cache ([b7f04d1](https://github.com/Indemnity83/logistics/commit/b7f04d1dbbcc9c737c3c5507e8ce30854427bbad))


### Removed

* **crafting:** drop the classic crafting resource pack ([d58c93c](https://github.com/Indemnity83/logistics/commit/d58c93c8893e2429d4bb7c73c5c7c27c9263ff19)), closes [#960](https://github.com/Indemnity83/logistics/issues/960)


### Fixed

* **ci:** stop the release pipeline publishing broken releases ([1ff390b](https://github.com/Indemnity83/logistics/commit/1ff390b899738a09e22512b9715728cf2d6e4d59))
* **core:** give the Seed Oil Bucket its missing model and texture ([#904](https://github.com/Indemnity83/logistics/issues/904)) ([009c5a8](https://github.com/Indemnity83/logistics/commit/009c5a80eaa5f6cd98aead5025eeb92726909432))
* **core:** survive a malformed config file at startup ([d5c2a36](https://github.com/Indemnity83/logistics/commit/d5c2a36db7aa6c4d27b208d88d4e142d767fa350))
* **crafting:** keep the sourceable remainder when a request is replanned ([83ac2e9](https://github.com/Indemnity83/logistics/commit/83ac2e9d60e92e67691b74c955d58d4f61ab484f))
* **crafting:** order only what was requested from a crafting batch ([83ac2e9](https://github.com/Indemnity83/logistics/commit/83ac2e9d60e92e67691b74c955d58d4f61ab484f))
* **energy:** face a newly placed engine at a full machine on NeoForge ([77cd947](https://github.com/Indemnity83/logistics/commit/77cd947f898b5f6a3f9834fd8ebd041b31a02423)), closes [#977](https://github.com/Indemnity83/logistics/issues/977) [#989](https://github.com/Indemnity83/logistics/issues/989)
* **energy:** give engine and battery models their missing particle texture ([#909](https://github.com/Indemnity83/logistics/issues/909)) ([f379bcb](https://github.com/Indemnity83/logistics/commit/f379bcb089ad5d44a42c692457be0defff562307))
* **energy:** stop a battery starving its own cable network ([77cd947](https://github.com/Indemnity83/logistics/commit/77cd947f898b5f6a3f9834fd8ebd041b31a02423))
* **energy:** stop cables voiding power into slow machines ([351ef35](https://github.com/Indemnity83/logistics/commit/351ef350036476df98bd4411fd7f416baae3989c))
* **energy:** stop duplicating and voiding energy on Fabric ([fbdbd2a](https://github.com/Indemnity83/logistics/commit/fbdbd2a51001ad4eea6b57d57dc28ecac702776b))
* **neoforge:** make peat, bitumen and tar burn again ([a706a27](https://github.com/Indemnity83/logistics/commit/a706a27e074d47c9eb33ad0b2caf671905bd1bac))
* **pump:** keep long intake tubes visible off screen ([aa4881b](https://github.com/Indemnity83/logistics/commit/aa4881b5480bdc5d6fda8516a74cdaa2a74063a4))
* **pump:** stop the intake tube descending through waterlogged blocks ([04fc53e](https://github.com/Indemnity83/logistics/commit/04fc53e20f95c83948c4b1f6cda337e0f65ffc57)), closes [#968](https://github.com/Indemnity83/logistics/issues/968) [#969](https://github.com/Indemnity83/logistics/issues/969)
* **quarry:** keep marker beams visible off screen ([aa4881b](https://github.com/Indemnity83/logistics/commit/aa4881b5480bdc5d6fda8516a74cdaa2a74063a4)), closes [#940](https://github.com/Indemnity83/logistics/issues/940)
* **quarry:** keep the frame and laser visible off screen ([aa4881b](https://github.com/Indemnity83/logistics/commit/aa4881b5480bdc5d6fda8516a74cdaa2a74063a4))
* **quarry:** let players break abandoned frames in survival ([5c8f0bb](https://github.com/Indemnity83/logistics/commit/5c8f0bb56e1c6ee03efc490fe0d5a52d1faed683))
* **quarry:** mine waterlogged blocks instead of skipping them ([04fc53e](https://github.com/Indemnity83/logistics/commit/04fc53e20f95c83948c4b1f6cda337e0f65ffc57))
* **quarry:** remove the unintended duplicate recipe ([98dec57](https://github.com/Indemnity83/logistics/commit/98dec57a553afab89862f12ba9a9f36d1e5ddce4))
* **quarry:** stop a zero arm speed freezing the quarry forever ([654f678](https://github.com/Indemnity83/logistics/commit/654f67822322209d57a88b5d05285d87f7d0eafa))
* **quarry:** stop frame blocks vanishing without warning ([5c8f0bb](https://github.com/Indemnity83/logistics/commit/5c8f0bb56e1c6ee03efc490fe0d5a52d1faed683))
* **quarry:** stop vacuuming loose items off the ground ([25bb95f](https://github.com/Indemnity83/logistics/commit/25bb95f04754f155666b71c6ab08616ba78196c0)), closes [#973](https://github.com/Indemnity83/logistics/issues/973)
* **routing:** apply one interaction range to every pipe menu ([73ad39f](https://github.com/Indemnity83/logistics/commit/73ad39fc05fb2ed932a72d24eb684c9cc4a6d7b2)), closes [#937](https://github.com/Indemnity83/logistics/issues/937) [#942](https://github.com/Indemnity83/logistics/issues/942)
* **routing:** cancel the order behind a job that has finished ([83ac2e9](https://github.com/Indemnity83/logistics/commit/83ac2e9d60e92e67691b74c955d58d4f61ab484f)), closes [#933](https://github.com/Indemnity83/logistics/issues/933) [#944](https://github.com/Indemnity83/logistics/issues/944) [#947](https://github.com/Indemnity83/logistics/issues/947)
* **routing:** close module menus when the pipe is broken ([73ad39f](https://github.com/Indemnity83/logistics/commit/73ad39fc05fb2ed932a72d24eb684c9cc4a6d7b2))
* **routing:** fall through to the next provider when one is fully reserved ([fa05a41](https://github.com/Indemnity83/logistics/commit/fa05a417e094b6194874be4ea7b6f51895cc122a)), closes [#928](https://github.com/Indemnity83/logistics/issues/928) [#938](https://github.com/Indemnity83/logistics/issues/938) [#939](https://github.com/Indemnity83/logistics/issues/939)
* **routing:** release only the delivered part of a shipment ([fa05a41](https://github.com/Indemnity83/logistics/commit/fa05a417e094b6194874be4ea7b6f51895cc122a))
* **routing:** render the right pipe arm after a neighbour changes ([b7f04d1](https://github.com/Indemnity83/logistics/commit/b7f04d1dbbcc9c737c3c5507e8ce30854427bbad))
* **routing:** stop a bad button id crashing the server ([d482da0](https://github.com/Indemnity83/logistics/commit/d482da05e8cb0755f527067126fc292cf93525f2))
* **routing:** stop broadcasting requester contents to every player ([d165385](https://github.com/Indemnity83/logistics/commit/d165385bd1bf9f5156b601797f44741a3057220f))
* **routing:** stop failed deliveries shrinking a provider's stock ([fa05a41](https://github.com/Indemnity83/logistics/commit/fa05a417e094b6194874be4ea7b6f51895cc122a))
* **routing:** stop Providers destroying items on an unpowered network ([7f11ac2](https://github.com/Indemnity83/logistics/commit/7f11ac21bf062ff27f1f066996fcf363643d88e2))
* **routing:** stop the Provider MkII shipping the wrong item to the next order ([3ce9e6e](https://github.com/Indemnity83/logistics/commit/3ce9e6e9742e8e606e46b0e1a4effc64b84556ba))
* **transport:** stop losing items on a partial pipe handoff ([c80697e](https://github.com/Indemnity83/logistics/commit/c80697e133d24ee49be17ba4e470bf563b32be24))

## [0.8.7](https://github.com/Indemnity83/logistics/compare/mc1.21.1-v0.8.6...mc1.21.1-v0.8.7) (2026-09-01)


### Added

* **automation:** add crude oil to petroleum-block transposer recipes ([#851](https://github.com/Indemnity83/logistics/issues/851)) ([043e782](https://github.com/Indemnity83/logistics/commit/043e782192812b0b2889364c44b8a97c37706d8b))
* **automation:** add RF cost and recipe system to the Transposer ([#829](https://github.com/Indemnity83/logistics/issues/829)) ([75ea7c5](https://github.com/Indemnity83/logistics/commit/75ea7c52c231ccae17d9cb8d10e751fe14d34b2b))
* **core:** add shared gear recipe tags ([#871](https://github.com/Indemnity83/logistics/issues/871)) ([ce2bb8c](https://github.com/Indemnity83/logistics/commit/ce2bb8c656013e45aaf6a8b724d25dc6454a9191))
* **energy:** show engine fuels in the recipe browser ([#886](https://github.com/Indemnity83/logistics/issues/886)) ([47d4f1b](https://github.com/Indemnity83/logistics/commit/47d4f1b722223073b45b97223d0f297359644301))
* **fluids:** drain cauldrons with the fluid extractor pipe ([#885](https://github.com/Indemnity83/logistics/issues/885)) ([50f4d37](https://github.com/Indemnity83/logistics/commit/50f4d374c3648e0847852d54f54fae21dad1ddc0))


### Changed

* **automation:** restyle the Crucible progress gauge as a droplet ([#834](https://github.com/Indemnity83/logistics/issues/834)) ([1f2f678](https://github.com/Indemnity83/logistics/commit/1f2f6782492e5513304f120a0eec8caf104dd155))


### Fixed

* **automation:** accept raw ore in the quicksilver amalgamation recipes ([#840](https://github.com/Indemnity83/logistics/issues/840)) ([b85e78a](https://github.com/Indemnity83/logistics/commit/b85e78a5309f6bb00335912e4d423e683da63e46))
* **automation:** resolve missing transposer texture issue ([fa7f75a](https://github.com/Indemnity83/logistics/commit/fa7f75ad189592df8988699009b125dca597616a))
* **automation:** stop refinery shift-click from duplicating items ([#846](https://github.com/Indemnity83/logistics/issues/846)) ([a6e6cf2](https://github.com/Indemnity83/logistics/commit/a6e6cf26acf699be0ba58942c2fb7711f1d181d1))
* **compat:** stop Jade plugin error on dedicated servers ([#878](https://github.com/Indemnity83/logistics/issues/878)) ([405bf57](https://github.com/Indemnity83/logistics/commit/405bf570ec38652a94749b410e4819a6cc1100e5))
* **energy:** add catalyst engine JEI category ([#874](https://github.com/Indemnity83/logistics/issues/874)) ([6320406](https://github.com/Indemnity83/logistics/commit/6320406930b82b702302a67a4e18772126382922))
* **energy:** show the reaction engine's recipes in JEI on Fabric ([#886](https://github.com/Indemnity83/logistics/issues/886)) ([47d4f1b](https://github.com/Indemnity83/logistics/commit/47d4f1b722223073b45b97223d0f297359644301))
* **energy:** stop battery and cable taking forever to mine ([#839](https://github.com/Indemnity83/logistics/issues/839)) ([c98ddff](https://github.com/Indemnity83/logistics/commit/c98ddff2d8e778d58d44e5f75b941d0a4d5c08ff))
* **fluids:** honor pipe transfer rates set above the default ([#885](https://github.com/Indemnity83/logistics/issues/885)) ([50f4d37](https://github.com/Indemnity83/logistics/commit/50f4d374c3648e0847852d54f54fae21dad1ddc0))
* **fluids:** obscure vision and apply Nausea/Poison/Slowness in Crude Oil ([#848](https://github.com/Indemnity83/logistics/issues/848)) ([bab7a21](https://github.com/Indemnity83/logistics/commit/bab7a21c2a7a47e26bfff0f89e966c6c17c339c0))
* **fluids:** stop over-capacity saved tank amounts from crashing on load ([#849](https://github.com/Indemnity83/logistics/issues/849)) ([67e986c](https://github.com/Indemnity83/logistics/commit/67e986c368747a558f01731975d2d2d3361ea8cc))
* **macerator:** grind loose raw ore items into dust ([#838](https://github.com/Indemnity83/logistics/issues/838)) ([a2dc8c3](https://github.com/Indemnity83/logistics/commit/a2dc8c3370260c5787aa3217e60f9baa6ea5e463))
* **quarry:** stop the arm mining through lava or ignoring reappeared blocks ([#850](https://github.com/Indemnity83/logistics/issues/850)) ([88a8f07](https://github.com/Indemnity83/logistics/commit/88a8f07e26b089fcf5b41192f59a3cda5128cbe1))
* **routing:** treat requester deliveries with no inventory as fulfilled ([#847](https://github.com/Indemnity83/logistics/issues/847)) ([6eb6e02](https://github.com/Indemnity83/logistics/commit/6eb6e021201ef051a6731f1ac93b127645b1c9a3))

## [0.8.6](https://github.com/Indemnity83/logistics/compare/mc1.21.1-v0.8.5...mc1.21.1-v0.8.6) (2026-08-07)


### Added

* **automation:** add the Transposer ([#792](https://github.com/Indemnity83/logistics/issues/792)) ([43e57c9](https://github.com/Indemnity83/logistics/commit/43e57c943037dce236fa00cbe440a39a96bdac47))
* **fluids:** add a dedicated fluid supplier GUI ([#811](https://github.com/Indemnity83/logistics/issues/811)) ([b6f3a3a](https://github.com/Indemnity83/logistics/commit/b6f3a3a26ca5c02f5cbf24ef7629c23f7f316211))
* **fluids:** add fluid provider and supplier chassis modules ([#825](https://github.com/Indemnity83/logistics/issues/825)) ([43e5b15](https://github.com/Indemnity83/logistics/commit/43e5b158f5e80af06fd987ab095b54b31b1181d4))
* **fluids:** add supplier partial/exact and minimum-deficit modes ([#822](https://github.com/Indemnity83/logistics/issues/822)) ([f1c2484](https://github.com/Indemnity83/logistics/commit/f1c248452880715d98534fd0f50abdb9724722bb))
* **fluids:** add the fluid provider pipe ([#790](https://github.com/Indemnity83/logistics/issues/790)) ([44c9f7f](https://github.com/Indemnity83/logistics/commit/44c9f7fb73f6417cc460c7461d2dc9ccbbcbe5e3))
* **fluids:** add the fluid supplier pipe ([#791](https://github.com/Indemnity83/logistics/issues/791)) ([e105a34](https://github.com/Indemnity83/logistics/commit/e105a34960386778edb088ea8d9f306fe4a4a437))


### Changed

* **fluids:** switch the fluid packet's frame window to a rectangle ([#823](https://github.com/Indemnity83/logistics/issues/823)) ([9e271dd](https://github.com/Indemnity83/logistics/commit/9e271ddb1ea298ca1f21fc78662182aa4c7d2784))


### Fixed

* **automation:** show the sawmill's real ingredient count in JEI ([#824](https://github.com/Indemnity83/logistics/issues/824)) ([adbcb8f](https://github.com/Indemnity83/logistics/commit/adbcb8f3243c7e4250c64d7b164d43cc01432efe))
* **automation:** stop the sawmill from silently rejecting seeds ([#824](https://github.com/Indemnity83/logistics/issues/824)) ([adbcb8f](https://github.com/Indemnity83/logistics/commit/adbcb8f3243c7e4250c64d7b164d43cc01432efe))
* **fluids:** never drop fluid packets on the ground ([#805](https://github.com/Indemnity83/logistics/issues/805)) ([9294efe](https://github.com/Indemnity83/logistics/commit/9294efe3d383607f7f22f5691cef109999750d8f))
* **fluids:** stop suppliers requesting fluid/items with no room ([#804](https://github.com/Indemnity83/logistics/issues/804)) ([f818ce5](https://github.com/Indemnity83/logistics/commit/f818ce57f2ec0aa82cd4773ba0cb47fe367aa4c0))
* **sawmill:** accept single-item deliveries for batched recipes ([#827](https://github.com/Indemnity83/logistics/issues/827)) ([9cb6126](https://github.com/Indemnity83/logistics/commit/9cb6126a1111e6b080e171ec2bf16d90b8322db1))

## [0.8.5](https://github.com/Indemnity83/logistics/compare/mc1.21.1-v0.8.4...mc1.21.1-v0.8.5) (2026-08-03)


### Changed

* **energy:** gate engine harvesting by tool tier ([#784](https://github.com/Indemnity83/logistics/issues/784)) ([ad95ecd](https://github.com/Indemnity83/logistics/commit/ad95ecd15bf71c4ecf3dada0ea46cbefc468c77e))


### Fixed

* **energy:** correct cable connections to engines ([#801](https://github.com/Indemnity83/logistics/issues/801)) ([d51c67f](https://github.com/Indemnity83/logistics/commit/d51c67f9212e536921efefd0140c09df5f4b4cb4))
* **quarry:** fix immediate crash on published Fabric builds ([#798](https://github.com/Indemnity83/logistics/issues/798)) ([ef3b00d](https://github.com/Indemnity83/logistics/commit/ef3b00d7852632f7e8e6c8cab302fa07b393064b)) — thanks @WerWebWer

### New Contributors

* @WerWebWer made their first contribution in #798

## [0.8.4](https://github.com/Indemnity83/logistics/compare/mc1.21.1-v0.8.3...mc1.21.1-v0.8.4) (2026-07-24)


### Added

* **energy:** add the Fuel Engine ([#759](https://github.com/Indemnity83/logistics/issues/759)) ([e8e7001](https://github.com/Indemnity83/logistics/commit/e8e700125373428cb8c4dd3aa307b1e3617de751))
* **energy:** add the Magmatic Engine ([#776](https://github.com/Indemnity83/logistics/issues/776)) ([e2badad](https://github.com/Indemnity83/logistics/commit/e2badade219646444650e96639927a685544baa3))
* **energy:** add the Reaction Engine ([#777](https://github.com/Indemnity83/logistics/issues/777)) ([b54b119](https://github.com/Indemnity83/logistics/commit/b54b1191e82034023de0de49c75a7a29de02eba4))
* **energy:** add the Steam Engine ([#765](https://github.com/Indemnity83/logistics/issues/765)) ([38c01ba](https://github.com/Indemnity83/logistics/commit/38c01ba6297c9b3e6735d05abcd16ae9c75e11cf))


### Changed

* **core:** retexture the bronze and tin metal blocks and items ([#766](https://github.com/Indemnity83/logistics/issues/766)) ([11f4415](https://github.com/Indemnity83/logistics/commit/11f441566d5c64f04c06062f529b3a0df22f0eca))
* **energy:** restyle the Stirling Engine GUI ([#763](https://github.com/Indemnity83/logistics/issues/763)) ([bf0e065](https://github.com/Indemnity83/logistics/commit/bf0e065b7f2b3920522cc2ffd4490b952ef08557))
* **fluids:** restyle the fluid pipe textures ([#740](https://github.com/Indemnity83/logistics/issues/740)) ([bbe15da](https://github.com/Indemnity83/logistics/commit/bbe15dae727ba4e98ce095c29b4cb8f3f813d8ad))


### Fixed

* **pipes:** fix missing pixel in logistics power junction animation ([#637](https://github.com/Indemnity83/logistics/issues/637)) ([2320d86](https://github.com/Indemnity83/logistics/commit/2320d86ad953e1c07c4c83adf9b19e4e02441fd5))

## [0.8.3](https://github.com/Indemnity83/logistics/compare/mc1.21.1-v0.8.2...mc1.21.1-v0.8.3) (2026-07-20)


### Added

* **automation:** add the refinery ([#714](https://github.com/Indemnity83/logistics/issues/714)) ([7aec821](https://github.com/Indemnity83/logistics/commit/7aec8217c2002a3154106cab4386a629496ebe67))
* **automation:** add the sequential fabricator ([#719](https://github.com/Indemnity83/logistics/issues/719)) ([8586cbe](https://github.com/Indemnity83/logistics/commit/8586cbea75461218df82c6bbc1ac3afe25469a53))
* **automation:** add the sequential fabricator ([#719](https://github.com/Indemnity83/logistics/issues/719)) ([8586cbe](https://github.com/Indemnity83/logistics/commit/8586cbea75461218df82c6bbc1ac3afe25469a53))
* **automation:** fabricate chipsets in the sequential fabricator ([#720](https://github.com/Indemnity83/logistics/issues/720)) ([56ead97](https://github.com/Indemnity83/logistics/commit/56ead97981be2dd263c535d4729330cd42d28269))
* **automation:** fabricate chipsets in the sequential fabricator ([#720](https://github.com/Indemnity83/logistics/issues/720)) ([56ead97](https://github.com/Indemnity83/logistics/commit/56ead97981be2dd263c535d4729330cd42d28269))
* **automation:** make machine tuning configurable per machine ([#713](https://github.com/Indemnity83/logistics/issues/713)) ([606095c](https://github.com/Indemnity83/logistics/commit/606095cbd0907a1472e5e65095bf6db495c161f5))
* **compat:** add Jade HUD support for refinery and sequential fabricator ([#744](https://github.com/Indemnity83/logistics/issues/744)) ([8b5f6cb](https://github.com/Indemnity83/logistics/commit/8b5f6cb9c287f0aac8633ba718bb1a269b464408))
* **core:** add a copper nugget ([#722](https://github.com/Indemnity83/logistics/issues/722)) ([9864c6a](https://github.com/Indemnity83/logistics/commit/9864c6a840306684f083e73cdf8e42155eb91351))
* **crafting:** add tin, rubber, amethyst, and echo valves ([85fb7a2](https://github.com/Indemnity83/logistics/commit/85fb7a2b24a412c00cd7c9095dde30b775cf12e1))
* **crafting:** rework the valve lineup with a bench recipe ([#727](https://github.com/Indemnity83/logistics/issues/727)) ([85fb7a2](https://github.com/Indemnity83/logistics/commit/85fb7a2b24a412c00cd7c9095dde30b775cf12e1))
* **crafting:** show sequential fabricator recipes in JEI ([#745](https://github.com/Indemnity83/logistics/issues/745)) ([b745a2d](https://github.com/Indemnity83/logistics/commit/b745a2dfad07eb3bf169ebed309b532cddaed40d))
* **energy:** add natural and synthetic polymers for rubber ([#716](https://github.com/Indemnity83/logistics/issues/716)) ([f3bf1e6](https://github.com/Indemnity83/logistics/commit/f3bf1e63a8bddabf7de91801c68d752e58bf0bc3))
* **fluids:** add bio fuel and fuel oil fluids ([7aec821](https://github.com/Indemnity83/logistics/commit/7aec8217c2002a3154106cab4386a629496ebe67))
* **fluids:** add tar as an alternative fluid pipe sealant ([#715](https://github.com/Indemnity83/logistics/issues/715)) ([5fa58de](https://github.com/Indemnity83/logistics/commit/5fa58de4296ea974c221c408242be7d9d518aafe))
* **routing:** add chipset crafting alternatives for pipes and modules ([c5fc004](https://github.com/Indemnity83/logistics/commit/c5fc0043fa8e1c5cb7e17fe6bf8c2acb3a276324))
* **ui:** split creative menu into domain tabs ([#738](https://github.com/Indemnity83/logistics/issues/738)) ([f446d03](https://github.com/Indemnity83/logistics/commit/f446d034f0128c06b0cf9a4e9afac259376c1189))


### Changed

* **crafting:** craft valves from quartz, redstone, and a base material ([85fb7a2](https://github.com/Indemnity83/logistics/commit/85fb7a2b24a412c00cd7c9095dde30b775cf12e1))
* **crafting:** restyle every valve with a distinct electron-tube texture ([85fb7a2](https://github.com/Indemnity83/logistics/commit/85fb7a2b24a412c00cd7c9095dde30b775cf12e1))
* **macerator:** recycle netherite dust to and from ingots ([#734](https://github.com/Indemnity83/logistics/issues/734)) ([39133ff](https://github.com/Indemnity83/logistics/commit/39133ffcb498a40da36e51cded3cba39e391b135))
* **routing:** rework logistics pipe recipes with chipset alternatives ([#721](https://github.com/Indemnity83/logistics/issues/721)) ([c5fc004](https://github.com/Indemnity83/logistics/commit/c5fc0043fa8e1c5cb7e17fe6bf8c2acb3a276324))
* **routing:** use a copper nugget in the blank module recipe ([9864c6a](https://github.com/Indemnity83/logistics/commit/9864c6a840306684f083e73cdf8e42155eb91351))


### Removed

* **crafting:** drop the wooden and ender valves ([85fb7a2](https://github.com/Indemnity83/logistics/commit/85fb7a2b24a412c00cd7c9095dde30b775cf12e1))


### Fixed

* **automation:** make the refinery and sequential fabricator harvestable ([24ab4bf](https://github.com/Indemnity83/logistics/commit/24ab4bf5bc19222922b1aebabf47ba31569db493))
* **automation:** show machine recipes in JEI on multiplayer clients ([#735](https://github.com/Indemnity83/logistics/issues/735)) ([cec19be](https://github.com/Indemnity83/logistics/commit/cec19be6386564e616f4370a809702563b247d9f))
* **compat:** register Crucible with NeoForge Jade plugin ([#744](https://github.com/Indemnity83/logistics/issues/744)) ([8b5f6cb](https://github.com/Indemnity83/logistics/commit/8b5f6cb9c287f0aac8633ba718bb1a269b464408))
* **compat:** show values in the Jade HUD instead of only labels ([#762](https://github.com/Indemnity83/logistics/issues/762)) ([d64e17c](https://github.com/Indemnity83/logistics/commit/d64e17c663019b465a0931f59dcceb522bcc11c8))
* **macerator:** use blank JEI background so byproduct output displays correctly ([#737](https://github.com/Indemnity83/logistics/issues/737)) ([b2426da](https://github.com/Indemnity83/logistics/commit/b2426daa14039f443a2758460ff42ad3474391d4))
* **neoforge:** let pipes and cables interact with the refinery and sawmill ([#733](https://github.com/Indemnity83/logistics/issues/733)) ([4188593](https://github.com/Indemnity83/logistics/commit/41885935342c9f97206e756402be561229638ce4))

## [0.8.2](https://github.com/Indemnity83/logistics/compare/mc1.21.1-v0.8.1...mc1.21.1-v0.8.2) (2026-07-09)


### Added

* **automation:** add pulped biomass from the sawmill ([#660](https://github.com/Indemnity83/logistics/issues/660)) ([fd6701b](https://github.com/Indemnity83/logistics/commit/fd6701b83fe65ac4c9f494cd6ad2f2c3baf56ec4))
* **automation:** add the alloy smelter ([#656](https://github.com/Indemnity83/logistics/issues/656)) ([3e334fa](https://github.com/Indemnity83/logistics/commit/3e334faced31ed8473a9a38b94d53ace01852f55))
* **automation:** add the crucible ([#679](https://github.com/Indemnity83/logistics/issues/679)) ([7412d6f](https://github.com/Indemnity83/logistics/commit/7412d6f59135bec2a7098f45d0d3bfd519f8ebf9))
* **fluids:** add custom fluids and buckets for the Magma Crucible ([2178ff8](https://github.com/Indemnity83/logistics/commit/2178ff86cb40471b431fcbd0d052a642e8074add))
* **fluids:** light-emitting fluids glow in pipes and tanks ([#695](https://github.com/Indemnity83/logistics/issues/695)) ([6505e21](https://github.com/Indemnity83/logistics/commit/6505e216dbdcb5c7e570fcfdc88006b1cbc16926))
* **worldgen:** add bog earth and peat fuel ([#658](https://github.com/Indemnity83/logistics/issues/658)) ([0342a4d](https://github.com/Indemnity83/logistics/commit/0342a4d248f7112edfbefa82b93e8b5bf64d1a60))
* **worldgen:** add crude oil and the oil chain ([#690](https://github.com/Indemnity83/logistics/issues/690)) ([38648ad](https://github.com/Indemnity83/logistics/commit/38648addcfe885decb97ff0486a0d261c40c0721))


### Fixed

* **automation:** reject non-finite recipe experience values ([d168dfc](https://github.com/Indemnity83/logistics/commit/d168dfcef7142e231319d521fe0e1af5a88789d4))
* **automation:** sync machine progress and energy so bars can't overflow ([#694](https://github.com/Indemnity83/logistics/issues/694)) ([e7a4331](https://github.com/Indemnity83/logistics/commit/e7a433142b39eceeeb45bc3e6b35d7d777464845))
* **ci:** stop Sentry release failing on cross-branch set-commits ([#683](https://github.com/Indemnity83/logistics/issues/683)) ([a874c05](https://github.com/Indemnity83/logistics/commit/a874c0526a83a626caadfbb46f6da32b4a32f936))
* **ci:** unblock release publishing broken by Sentry set-commits ([#678](https://github.com/Indemnity83/logistics/issues/678)) ([13344f3](https://github.com/Indemnity83/logistics/commit/13344f357af784f66da3292841564fa70448b103))
* **compat:** add missing Jade fluid-pipe config translation ([#701](https://github.com/Indemnity83/logistics/issues/701)) ([b00f9e5](https://github.com/Indemnity83/logistics/commit/b00f9e5288007ae829b277b0486d98bc8704f089))
* **fluids:** stop drained pipes rendering a checkerboard ([#696](https://github.com/Indemnity83/logistics/issues/696)) ([034ab8e](https://github.com/Indemnity83/logistics/commit/034ab8e6f90ac51dd30e5da891d65b7061d191a8))
* **fluids:** stop fluid extractor pipes from connecting to each other ([#692](https://github.com/Indemnity83/logistics/issues/692)) ([ec492c8](https://github.com/Indemnity83/logistics/commit/ec492c8d9ee6ec0ee88ccc5ef34bb6f915289aa2))
* **pump:** make the pump tank output-only ([#693](https://github.com/Indemnity83/logistics/issues/693)) ([10d3746](https://github.com/Indemnity83/logistics/commit/10d37467c38a57272bd2ccc2f05bf3bfae40d059))
* **routing:** drop the failed order's index entry on delivery retry ([28d80b5](https://github.com/Indemnity83/logistics/commit/28d80b50577dd3f912271ee66dd57d5e3ec1a932))

## [0.8.1](https://github.com/Indemnity83/logistics/compare/mc1.21.1-v0.8.0...mc1.21.1-v0.8.1) (2026-06-30)


### Removed

* **core:** drop the unused sturdy casing ([#650](https://github.com/Indemnity83/logistics/issues/650)) ([3b6d2cf](https://github.com/Indemnity83/logistics/commit/3b6d2cfdcf06323a7af028d051d8c7dddbd0fbd1))


### Fixed

* **pump:** accept power from any energy source ([#651](https://github.com/Indemnity83/logistics/issues/651)) ([97951df](https://github.com/Indemnity83/logistics/commit/97951df3971718d5ddd8aadf3723e752bcf94dec))

## [0.8.0](https://github.com/Indemnity83/logistics/compare/mc1.21.1-v0.7.4...mc1.21.1-v0.8.0) (2026-06-29)


### ⚠ BREAKING CHANGES

* **energy:** Cables and batteries no longer power extraction pipes or the Fluid Pump. Only a directly-adjacent engine can power them. Existing setups that fed pipes through cables/batteries will stop working — place an engine against the pipe instead.
* **energy:** Batteries no longer power a logistics network directly. A network is powered only through a Power Junction — place one between your power source (cables/batteries) and the network. Existing battery-on-a-pipe setups stop working until a Power Junction is added.

### Added

* **core:** add opt-in sanitized crash reporting ([#633](https://github.com/Indemnity83/logistics/issues/633)) ([8f1167b](https://github.com/Indemnity83/logistics/commit/8f1167b126f9dd122b5e91ac97dd3f945169a42d))
* **core:** drop niter from the breeze ([#644](https://github.com/Indemnity83/logistics/issues/644)) ([174b165](https://github.com/Indemnity83/logistics/commit/174b16529b787533cf8d574a5cbb3b8332e99eaa))
* **crafting:** craft gunpowder from coal, sulfur, and niter dust ([25a2683](https://github.com/Indemnity83/logistics/commit/25a268352cf712fa834cee0339f125052bcc97ab))
* **energy:** add the power junction ([#612](https://github.com/Indemnity83/logistics/issues/612)) ([176ebbf](https://github.com/Indemnity83/logistics/commit/176ebbfbeeb60db658fa42aab23a9bda52023e45))
* **macerator:** add chance byproducts to ore processing ([#643](https://github.com/Indemnity83/logistics/issues/643)) ([25a2683](https://github.com/Indemnity83/logistics/commit/25a268352cf712fa834cee0339f125052bcc97ab))
* **macerator:** add recycling recipes for common blocks ([25a2683](https://github.com/Indemnity83/logistics/commit/25a268352cf712fa834cee0339f125052bcc97ab))
* **macerator:** add Sulfur Dust, Quicksilver, and Niter items ([25a2683](https://github.com/Indemnity83/logistics/commit/25a268352cf712fa834cee0339f125052bcc97ab))
* **macerator:** macerate breeze rods into wind charges ([25a2683](https://github.com/Indemnity83/logistics/commit/25a268352cf712fa834cee0339f125052bcc97ab))
* **macerator:** macerate logs and planks into sawdust and recycle wooden tools ([25a2683](https://github.com/Indemnity83/logistics/commit/25a268352cf712fa834cee0339f125052bcc97ab))
* **macerator:** recycle diamond tools and armor into diamonds ([25a2683](https://github.com/Indemnity83/logistics/commit/25a268352cf712fa834cee0339f125052bcc97ab))
* **sawmill:** add wood processing ([#580](https://github.com/Indemnity83/logistics/issues/580)) ([b97dd47](https://github.com/Indemnity83/logistics/commit/b97dd47be821c39cb9cbaec3a00f0359ecc97e9a))
* **sawmill:** show recipes in JEI and details in the Jade HUD ([#613](https://github.com/Indemnity83/logistics/issues/613)) ([17c3575](https://github.com/Indemnity83/logistics/commit/17c3575384830770422a77a3d8aa4df9b82c7ad1))


### Changed

* **common:** cache pipe and cable collision shapes ([#631](https://github.com/Indemnity83/logistics/issues/631)) ([91fa21a](https://github.com/Indemnity83/logistics/commit/91fa21a9391a9d59be3c19d0a1378140d71d9fb4))
* **core:** streamline world loading by dropping legacy save migrations ([#586](https://github.com/Indemnity83/logistics/issues/586)) ([f39c830](https://github.com/Indemnity83/logistics/commit/f39c83029b37c8126df6cdb9ec5ac82bee013921))
* **crafting:** require a bronze gear in the machine frame ([#610](https://github.com/Indemnity83/logistics/issues/610)) ([0d231d7](https://github.com/Indemnity83/logistics/commit/0d231d7affed7c7e869d94332ebcc42f20700bca))
* **crafting:** yield one marker per craft ([#607](https://github.com/Indemnity83/logistics/issues/607)) ([71fc9b8](https://github.com/Indemnity83/logistics/commit/71fc9b8838799e2aee2df86d7e626f2a9d28a9b7))
* **energy:** power extraction pipes only from a direct engine ([#641](https://github.com/Indemnity83/logistics/issues/641)) ([078b2e0](https://github.com/Indemnity83/logistics/commit/078b2e09d495972c8ad94c8aae2ed2f6f2be8817))
* **fluids:** speed up fluid split allocation ([#623](https://github.com/Indemnity83/logistics/issues/623)) ([c65386d](https://github.com/Indemnity83/logistics/commit/c65386d9417fd33fa1c56cb0267c306600dd1b2d))
* **macerator:** ore→dust recipes now drop a chance byproduct dust ([25a2683](https://github.com/Indemnity83/logistics/commit/25a268352cf712fa834cee0339f125052bcc97ab))
* **pipes:** raise pipe blast resistance to match glass ([#618](https://github.com/Indemnity83/logistics/issues/618)) ([9938bed](https://github.com/Indemnity83/logistics/commit/9938bed9c537528671c257269561969eedbd1506))
* **quarry:** restyle with the shared machine look ([#582](https://github.com/Indemnity83/logistics/issues/582)) ([4f81e02](https://github.com/Indemnity83/logistics/commit/4f81e02b6a442b943a15f6f0b384d64ade0e9b7b))
* **routing:** cache next-hop routes per destination ([#632](https://github.com/Indemnity83/logistics/issues/632)) ([f5a1bea](https://github.com/Indemnity83/logistics/commit/f5a1beaa70ab6debebffb222e1ba82ac1bbcbd09))
* **sawmill:** match the energy buffer to the other machines ([#647](https://github.com/Indemnity83/logistics/issues/647)) ([0dbe141](https://github.com/Indemnity83/logistics/commit/0dbe1414ae4a5b811dd07b7b7767f3fc817f03a4))
* **ui:** refresh the kiln, macerator, and sawmill GUIs ([#646](https://github.com/Indemnity83/logistics/issues/646)) ([aaa7517](https://github.com/Indemnity83/logistics/commit/aaa751780d3ddea4af6425398a8faf93ae18daff))
* **worldgen:** tin ore drops one raw tin ([#608](https://github.com/Indemnity83/logistics/issues/608)) ([88b48ed](https://github.com/Indemnity83/logistics/commit/88b48edba67be31960c77da6ec2f5cf88a988cef))


### Fixed

* **automation:** let quarry markers connect through solid blocks ([#581](https://github.com/Indemnity83/logistics/issues/581)) ([e9d194c](https://github.com/Indemnity83/logistics/commit/e9d194cb327298d6190d6fca243c5c8670a476df))
* **automation:** pause recipes until byproducts have space ([#597](https://github.com/Indemnity83/logistics/issues/597)) ([a7dba16](https://github.com/Indemnity83/logistics/commit/a7dba16c4758c9bd207174fdc1b207bd579af196))
* **core:** restore valve and quartz crystal recipes ([#600](https://github.com/Indemnity83/logistics/issues/600)) ([38882be](https://github.com/Indemnity83/logistics/commit/38882be152ffadae8f1f3e2f3af91281db21a02a))
* **energy:** drop the creative sink when broken ([#616](https://github.com/Indemnity83/logistics/issues/616)) ([3d14d61](https://github.com/Indemnity83/logistics/commit/3d14d61afcff76ea64ae18ae293617c153c2a04d))
* **energy:** stop battery items from crashing world saves ([#640](https://github.com/Indemnity83/logistics/issues/640)) ([78beca0](https://github.com/Indemnity83/logistics/commit/78beca06aa14957b1169290b8bad9f1a827c655f))
* **fluids:** drop fluid pipes and glass tank when broken ([#614](https://github.com/Indemnity83/logistics/issues/614)) ([06d6289](https://github.com/Indemnity83/logistics/commit/06d6289bd368339bc225eef39fbcf25bd4861825))
* **fluids:** show correct fill level on tank and pipe HUDs ([#619](https://github.com/Indemnity83/logistics/issues/619)) ([ca6cc8f](https://github.com/Indemnity83/logistics/commit/ca6cc8f32c29651e7d34873a9329317db13fe48f))
* **kiln:** accept energy from the power network ([#602](https://github.com/Indemnity83/logistics/issues/602)) ([a9560f8](https://github.com/Indemnity83/logistics/commit/a9560f8db14e45164e9991a67bf51bb3dd876ce4))
* **kiln:** bank smelting XP and pay it out like a furnace ([#605](https://github.com/Indemnity83/logistics/issues/605)) ([f576379](https://github.com/Indemnity83/logistics/commit/f576379d4aa29592a5df8504e2e092225d85085e))
* **kiln:** mine with the correct pickaxe tier ([#601](https://github.com/Indemnity83/logistics/issues/601)) ([99b7087](https://github.com/Indemnity83/logistics/commit/99b7087555b7ae5a2acc9ea4167b3951cfa8debe))
* **macerator:** bank maceration XP and pay it out like a furnace ([f576379](https://github.com/Indemnity83/logistics/commit/f576379d4aa29592a5df8504e2e092225d85085e))
* **macerator:** grant XP for macerating ancient debris ([#624](https://github.com/Indemnity83/logistics/issues/624)) ([45c7479](https://github.com/Indemnity83/logistics/commit/45c7479a81284e9302fbcead171967fe6e24feaa))
* **macerator:** restore the JEI integration ([#599](https://github.com/Indemnity83/logistics/issues/599)) ([d052f47](https://github.com/Indemnity83/logistics/commit/d052f47284b08fea40d9d1cc67b1d4ebce0a7cc5))
* **pump:** clamp the fluid pump search radius ([#625](https://github.com/Indemnity83/logistics/issues/625)) ([cc2b158](https://github.com/Indemnity83/logistics/commit/cc2b1586b455763e96d3d8ea652d808d7949dbab))
* **pump:** match the fluid pump's top to the other machines ([#561](https://github.com/Indemnity83/logistics/issues/561)) ([27a4fae](https://github.com/Indemnity83/logistics/commit/27a4fae0045524d7eb07c00d2ffb091d2ef17a11))
* **pump:** mine the fluid pump with the correct pickaxe tier ([06d6289](https://github.com/Indemnity83/logistics/commit/06d6289bd368339bc225eef39fbcf25bd4861825))
* **quarry:** give the laser quarry frame a display name ([#617](https://github.com/Indemnity83/logistics/issues/617)) ([e786309](https://github.com/Indemnity83/logistics/commit/e7863097b521ea265ffcab6f6f51f25447e00991))
* **routing:** keep chassis modules when a pipe explodes ([#629](https://github.com/Indemnity83/logistics/issues/629)) ([2f2f0f2](https://github.com/Indemnity83/logistics/commit/2f2f0f2603bef151ff68dab7229cdc583c6ebf3b))
* **routing:** refresh neighbor pipe arms when markings change ([#606](https://github.com/Indemnity83/logistics/issues/606)) ([c16bedc](https://github.com/Indemnity83/logistics/commit/c16bedcba314746da0d594fa5ea7991b2d8311c9))
* **sawmill:** add the missing crafting recipe ([#609](https://github.com/Indemnity83/logistics/issues/609)) ([4cb6e51](https://github.com/Indemnity83/logistics/commit/4cb6e51dfdae20c394e06f45a8276f35fc861f91))
* **sawmill:** mine with the correct pickaxe tier ([17c3575](https://github.com/Indemnity83/logistics/commit/17c3575384830770422a77a3d8aa4df9b82c7ad1))
* **ui:** show item tooltips in the Kiln, Sawmill, and Stirling Engine screens ([#638](https://github.com/Indemnity83/logistics/issues/638)) ([8604523](https://github.com/Indemnity83/logistics/commit/8604523156ed42134c5ab464282ce90d2560d7cf))

## [0.7.4](https://github.com/Indemnity83/logistics/compare/mc1.21.1-v0.7.3...mc1.21.1-v0.7.4) (2026-06-21)


### Added

* **automation:** add configurable laser quarry chunk loading ([#536](https://github.com/Indemnity83/logistics/issues/536)) ([ce661fe](https://github.com/Indemnity83/logistics/commit/ce661fe1d1e02421052b6cb2fb31f1cecacb7d97)) — thanks @floralpetals
* **core:** grant ore XP when macerating metal and apatite ores ([#555](https://github.com/Indemnity83/logistics/issues/555)) ([9faaa4f](https://github.com/Indemnity83/logistics/commit/9faaa4f16574368721e1036571d695334a83450c))


### Fixed

* **automation:** add missing macerator recipes for obsidian, netherite, and metal blocks ([#554](https://github.com/Indemnity83/logistics/issues/554)) ([73cb00c](https://github.com/Indemnity83/logistics/commit/73cb00c56c31daddf96d72fbbd74ab3afcd5ecc8))
* **energy:** fix crash when cables power machines from other mods ([#556](https://github.com/Indemnity83/logistics/issues/556)) ([b6f68d2](https://github.com/Indemnity83/logistics/commit/b6f68d29641f33fb06e0f68f08dd5989d008669e))
* **neoforge:** cables and quarry frames no longer render as broken/solid blocks ([#546](https://github.com/Indemnity83/logistics/issues/546)) ([4fb9ae0](https://github.com/Indemnity83/logistics/commit/4fb9ae0f46144e9b08e79e9551bd86327509ef85)) — thanks @AdolfoCarneiro

### New Contributors
* @floralpetals made their first contribution in #536

## [0.7.3](https://github.com/Indemnity83/logistics/compare/mc1.21.1-v0.7.2...mc1.21.1-v0.7.3) (2026-06-18)


### Added

* **api:** add loader-independent API for cross-mod fluid integration ([#516](https://github.com/Indemnity83/logistics/issues/516)) ([e71b87c](https://github.com/Indemnity83/logistics/commit/e71b87c97567f1fb20bad8d0a04e1f3bef33c7dd))
* **pipes:** add copper fluid pipe oxidation and fluid pipe marking ([#520](https://github.com/Indemnity83/logistics/issues/520)) ([e71b87c](https://github.com/Indemnity83/logistics/commit/e71b87c97567f1fb20bad8d0a04e1f3bef33c7dd))
* **pipes:** add fluid pipes, tanks, and powered fluid extraction ([#511](https://github.com/Indemnity83/logistics/issues/511)) ([e71b87c](https://github.com/Indemnity83/logistics/commit/e71b87c97567f1fb20bad8d0a04e1f3bef33c7dd))
* **pipes:** add fluid pump ([#537](https://github.com/Indemnity83/logistics/issues/537)) ([59e3d9a](https://github.com/Indemnity83/logistics/commit/59e3d9af4f8183e5f1a254bf83c2524f63dd8a4b))


### Fixed

* **compat:** hide fluid extractor energy in Jade ([#533](https://github.com/Indemnity83/logistics/issues/533)) ([53ac7c9](https://github.com/Indemnity83/logistics/commit/53ac7c902ea2400b9b6dd2c1ff86cd15605972ed))
* **pipes:** fix fluid pipe drain flicker ([#532](https://github.com/Indemnity83/logistics/issues/532)) ([ca753a7](https://github.com/Indemnity83/logistics/commit/ca753a7e9475c46003df5894aa663f98d95c9c5e))
* **ui:** fix glass tank capacity overlay with held items ([#534](https://github.com/Indemnity83/logistics/issues/534)) ([261ab7d](https://github.com/Indemnity83/logistics/commit/261ab7d551680831c7424966ede9ab74ec112a96))

## [0.7.2](https://github.com/Indemnity83/logistics/compare/mc1.21.1-v0.7.1...mc1.21.1-v0.7.2) (2026-06-15)


### Fixed

* **core:** repair recipes and item colors ([#508](https://github.com/Indemnity83/logistics/issues/508)) ([021701d](https://github.com/Indemnity83/logistics/commit/021701d92688150fefb70cbb2be75f1c44dc00ae)) — thanks @AdolfoCarneiro
* **energy:** add crafting recipe for battery ([#510](https://github.com/Indemnity83/logistics/issues/510)) ([e1abd12](https://github.com/Indemnity83/logistics/commit/e1abd1206cecb9f92b3df73c52c811862899b90f))

## [0.7.1](https://github.com/Indemnity83/logistics/compare/mc1.21.1-v0.7.0...mc1.21.1-v0.7.1) (2026-06-11)


### Added

* **automation:** show laser quarry status in the Jade HUD ([#498](https://github.com/Indemnity83/logistics/issues/498)) ([c99130a](https://github.com/Indemnity83/logistics/commit/c99130a13852fe78dd9f69720d3a0ef46eee7836))
* **automation:** show macerator and kiln progress in the Jade HUD ([#499](https://github.com/Indemnity83/logistics/issues/499)) ([9a5c9cb](https://github.com/Indemnity83/logistics/commit/9a5c9cbb7812d7faffb355e42cbb2f458dfee13b))
* **compat:** integrate Jade and remove the built-in probe ([529558f](https://github.com/Indemnity83/logistics/commit/529558f4ca4734e1e8ab02aa7750f66bb663132c))
* **energy:** show power diagnostics in the Jade HUD ([#497](https://github.com/Indemnity83/logistics/issues/497)) ([e5632d9](https://github.com/Indemnity83/logistics/commit/e5632d9054d069a69ae29cd6f9c131acae0cb008))
* **pipes:** show pipe contents in the Jade HUD ([#500](https://github.com/Indemnity83/logistics/issues/500)) ([d3ea0c7](https://github.com/Indemnity83/logistics/commit/d3ea0c75c31e74704258eb63a1226669ab60398c))
* **pipes:** show pipe module status in the Jade HUD ([#493](https://github.com/Indemnity83/logistics/issues/493)) ([e9db084](https://github.com/Indemnity83/logistics/commit/e9db0847039b40b54167ae1aca070e450b8402a0))


### Fixed

* **pipes:** apply config changes to modules installed in a chassis ([#504](https://github.com/Indemnity83/logistics/issues/504)) ([7fc6c60](https://github.com/Indemnity83/logistics/commit/7fc6c6079b68d942c05edfe1e02ef107c734736d)), closes [#494](https://github.com/Indemnity83/logistics/issues/494)

## [0.7.0](https://github.com/Indemnity83/logistics/compare/mc1.21.1-v0.6.1...mc1.21.1-v0.7.0) (2026-06-09)


### ⚠ BREAKING CHANGES

* **energy:** logistics pipe operations consume power ([#465](https://github.com/Indemnity83/logistics/issues/465))
* **energy:** Logistics pipes now require power from an adjacent Battery. Existing networks stop routing/supplying/crafting — and drop items already in transit — until a charged Battery is connected.

### Added

* **energy:** add a Battery block to power logistics networks ([e420625](https://github.com/Indemnity83/logistics/commit/e42062570dab6ee6318dd50f1f861af4ddda874c))
* **energy:** logistics pipe operations consume power ([#465](https://github.com/Indemnity83/logistics/issues/465)) ([e420625](https://github.com/Indemnity83/logistics/commit/e42062570dab6ee6318dd50f1f861af4ddda874c))
* **ui:** color logistics pipes green when powered, red when not ([#469](https://github.com/Indemnity83/logistics/issues/469)) ([741f92c](https://github.com/Indemnity83/logistics/commit/741f92c355fbc4e0197b2f24cfaed3549d1f1a1f))


### Fixed

* **automation:** stop inactive markers rendering as a black cross ([#484](https://github.com/Indemnity83/logistics/issues/484)) ([ed4c84d](https://github.com/Indemnity83/logistics/commit/ed4c84d7ac27e88e5ee0a68d55ce831a92d03c00))
* **automation:** stop the macerator from trying to load other mods' recipes ([#473](https://github.com/Indemnity83/logistics/issues/473)) ([f5b7ce3](https://github.com/Indemnity83/logistics/commit/f5b7ce366f0414a6b793a625d33296b068827cf3))
* **energy:** make engines visibly change color with heat stage ([e4a3cb3](https://github.com/Indemnity83/logistics/commit/e4a3cb3d1c28d6732f9027b3c2be6e1ba66bd2b1))
* **energy:** stop the battery charge bar rendering black on NeoForge ([1ca61b1](https://github.com/Indemnity83/logistics/commit/1ca61b1e15379b82e03303e51a66dfd9dc659775))


### Improved

* **automation:** reduce memory and load time for machine rendering ([#451](https://github.com/Indemnity83/logistics/issues/451)) ([cfb04dd](https://github.com/Indemnity83/logistics/commit/cfb04dd7b89a0074069819efcc0ea8097c80b105))
* **pipes:** reduce memory and load time for pipe rendering ([#450](https://github.com/Indemnity83/logistics/issues/450)) ([4615511](https://github.com/Indemnity83/logistics/commit/461551129e7965d01aa7184b6cfb7f080cf21edf))

## [0.6.1](https://github.com/Indemnity83/logistics/compare/mc1.21.1-v0.6.0...mc1.21.1-v0.6.1) (2026-06-02)


### Fixed

* **automation:** fix kiln cooking time call for MC 1.21.1 ([a8bf0f2](https://github.com/Indemnity83/logistics/commit/a8bf0f2dc7973ac6bc87612b574bc188642aff50))
* correct laser quarry edge case regressions ([#427](https://github.com/Indemnity83/logistics/issues/427)) ([2228ec8](https://github.com/Indemnity83/logistics/commit/2228ec8c3fa0ee7aabe3e720f18dda8305d8263a))
* enable custom Minecraft version range in build workflows ([#414](https://github.com/Indemnity83/logistics/issues/414)) ([63f0133](https://github.com/Indemnity83/logistics/commit/63f01337419164beb70be9b45e7418092179466e))
* register inventory synchronization for NeoForge requester ([#413](https://github.com/Indemnity83/logistics/issues/413)) ([76678e1](https://github.com/Indemnity83/logistics/commit/76678e19dc0e08a08fab8de968431c67af00de0a))
* update Minecraft version compatibility range for NeoForge ([#416](https://github.com/Indemnity83/logistics/issues/416)) ([af204c4](https://github.com/Indemnity83/logistics/commit/af204c44074413b503309e2ca32e8b43a362b236))

## [0.6.0](https://github.com/Indemnity83/logistics/compare/mc1.21.1-v0.5.6...mc1.21.1-v0.6.0) (2026-05-31)

### Features

* Added NeoForge support, including platform services, networking/lifecycle hooks, client rendering, and storage adapters. [#378](https://github.com/Indemnity83/logistics/issues/378), [#380](https://github.com/Indemnity83/logistics/issues/380), [#381](https://github.com/Indemnity83/logistics/issues/381)
* Added power cables. [f83a857](https://github.com/Indemnity83/logistics/commit/f83a857e044f217ef2cc4a264ce09ea22ae022a6)

### Bug Fixes

* Improved logistics network reliability and safety by handling failed deliveries, validating/sanitizing config fields, and clamping energy values and transfer amounts to non-negative values. [#397](https://github.com/Indemnity83/logistics/issues/397), [#398](https://github.com/Indemnity83/logistics/issues/398), [#399](https://github.com/Indemnity83/logistics/issues/399)
* Fixed NeoForge and multi-loader content issues, including reload listener events, JEI Macerator recipe visibility, marking fluid recipe separation, and missing `META-INF` service files. [#366](https://github.com/Indemnity83/logistics/issues/366), [#382](https://github.com/Indemnity83/logistics/issues/382), [#390](https://github.com/Indemnity83/logistics/issues/390), [0897442](https://github.com/Indemnity83/logistics/commit/089744217d8b28ff08d591e5a785d420a45f260f) — thanks @AdolfoCarneiro
* Fixed power cable compilation errors for Minecraft 1.21.1. [#359](https://github.com/Indemnity83/logistics/issues/359)
* Preserved item components in filter pipe slots across save/reload. [#386](https://github.com/Indemnity83/logistics/issues/386)

### Refactorings

* Reworked the project for multi-loader support, including NeoForge groundwork, loader-agnostic bootstrap flow, service-based platform access, cleaner module boundaries, and build configuration updates. [#306](https://github.com/Indemnity83/logistics/issues/306), [#318](https://github.com/Indemnity83/logistics/issues/318), [#320](https://github.com/Indemnity83/logistics/issues/320), [#341](https://github.com/Indemnity83/logistics/issues/341), [#342](https://github.com/Indemnity83/logistics/issues/342), [#343](https://github.com/Indemnity83/logistics/issues/343), [#344](https://github.com/Indemnity83/logistics/issues/344), [#347](https://github.com/Indemnity83/logistics/issues/347), [#348](https://github.com/Indemnity83/logistics/issues/348), [#360](https://github.com/Indemnity83/logistics/issues/360), [#361](https://github.com/Indemnity83/logistics/issues/361) — thanks @AdolfoCarneiro
* Introduced loader-agnostic storage, energy, fluid, fuel, item matching, and client model abstractions. [#340](https://github.com/Indemnity83/logistics/issues/340), [#349](https://github.com/Indemnity83/logistics/issues/349), [#351](https://github.com/Indemnity83/logistics/issues/351), [#356](https://github.com/Indemnity83/logistics/issues/356), [#364](https://github.com/Indemnity83/logistics/issues/364), [#389](https://github.com/Indemnity83/logistics/issues/389), [#400](https://github.com/Indemnity83/logistics/issues/400)
* Cleaned up common code organization and removed remaining Fabric-specific dependencies/imports from shared sources. [#345](https://github.com/Indemnity83/logistics/issues/345), [#350](https://github.com/Indemnity83/logistics/issues/350), [#352](https://github.com/Indemnity83/logistics/issues/352), [#355](https://github.com/Indemnity83/logistics/issues/355)

### Testing

* Added NeoForge ServiceLoader and energy adapter tests, component coverage, and baseline coverage reporting. [#377](https://github.com/Indemnity83/logistics/issues/377), [#402](https://github.com/Indemnity83/logistics/issues/402)

### New Contributors
* @AdolfoCarneiro made their first contribution in #306

## [0.5.5](https://github.com/Indemnity83/logistics/compare/mc1.21.1-v0.5.4...mc1.21.1-v0.5.5) (2026-04-25)


### Bug Fixes

* resolve vanishing filters in diamond pipes serialization ([#288](https://github.com/Indemnity83/logistics/issues/288)) ([9bc893e](https://github.com/Indemnity83/logistics/commit/9bc893e30c1791b6f5f2ed06c2dc6bc6d0211808))

## [0.5.4](https://github.com/Indemnity83/logistics/compare/mc1.21.1-v0.5.3...mc1.21.1-v0.5.4) (2026-04-22)


### Bug Fixes

* add ender dust macerator recipe ([#282](https://github.com/Indemnity83/logistics/issues/282)) ([a82b1c2](https://github.com/Indemnity83/logistics/commit/a82b1c2a76d786bb5e4fbe3658a4a94a862a0e65)) — thanks @ZayshaaCodes
* use RegistryOps to fix enchanted item crash ([#283](https://github.com/Indemnity83/logistics/issues/283)) ([b9b12be](https://github.com/Indemnity83/logistics/commit/b9b12bed1a1f492832025b437de1eff8cc4d8269)) — thanks @ZayshaaCodes

### New Contributors
* @ZayshaaCodes made their first contribution in #282

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
