package org.wisp.neutrino_detector_mkii

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.campaign.ids.Abilities
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import org.lazywizard.lazylib.ext.json.optFloat
import org.lazywizard.lazylib.ext.logging.e
import java.awt.Color

class LifecyclePlugin : BaseModPlugin() {
    companion object {
        // 15 LY, increased from vanilla 10 LY
        var slipstreamDetectionRange: Float = 30000f
        const val COMMODITY_ID: String = Commodities.VOLATILES

        var commoditiesPerDay: Float? = null
        var detectabilityPercent: Float = 50f
        var color = Color(128, 2, 173, 255)//overriden
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

        // Reload on game load
        loadSettings()
    }

    private fun loadSettings() {
        kotlin.runCatching {
            val config =
                Global.getSettings().getMergedJSONForMod("data/config/modSettings.json", "wisp_NeutrinoDetectorMkII")
                    .getJSONObject("NeutrinoDetectorMkII")

            slipstreamDetectionRange = config.optFloat("slipstreamDetectionRange", 30000f)
            detectabilityPercent = config.optFloat("detectabilityPercent", 50f)
            commoditiesPerDay = config.optFloat("volatilesPerDay", 1.8f)
            color = config.getJSONArray("color").let {
                Color(
                    it[0].toString().toInt(),
                    it[1].toString().toInt(),
                    it[2].toString().toInt(),
                    it[3].toString().toInt()
                )
            }
        }
            .onFailure {
                Global.getLogger(NeutrinoDetectorMkIIAbility::class.java).e({ it.message ?: "" }, it)
            }
            .getOrThrow()
    }
}