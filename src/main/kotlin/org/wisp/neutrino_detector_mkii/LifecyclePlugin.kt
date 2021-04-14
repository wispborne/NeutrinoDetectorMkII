package org.wisp.neutrino_detector_mkii

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.campaign.ids.Abilities
import com.thoughtworks.xstream.XStream

class LifecyclePlugin : BaseModPlugin() {

    override fun onNewGameAfterTimePass() {
        super.onNewGameAfterTimePass()
    }

    override fun onGameLoad(newGame: Boolean) {
        super.onGameLoad(newGame)
        val neutDetectMk2Id = "neutrino_detector_mkII"
        val playerAbilities =
            Global.getSector().characterData.abilities + Global.getSector().playerPerson.stats.grantedAbilityIds

        if (Abilities.GRAVITIC_SCAN in playerAbilities
            && neutDetectMk2Id !in playerAbilities
        ) {
            Global.getSector().characterData.addAbility(neutDetectMk2Id)
        }
    }

    override fun beforeGameSave() {
        super.beforeGameSave()
    }

    /**
     * Tell the XML serializer to use custom naming, so that moving or renaming classes doesn't break saves.
     */
    override fun configureXStream(x: XStream) {
        super.configureXStream(x)
    }
}