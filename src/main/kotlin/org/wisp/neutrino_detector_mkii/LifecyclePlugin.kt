package org.wisp.neutrino_detector_mkii

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.campaign.ids.Abilities

class LifecyclePlugin : BaseModPlugin() {

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
}