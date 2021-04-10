package org.wisp.neutrino_detector_mkii

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignEngineLayers
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.graphics.SpriteAPI
import com.fs.starfarer.api.impl.campaign.abilities.BaseToggleAbility
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.util.*

/**
 * Source: com.fs.starfarer.api.impl.campaign.abilities.GraviticScanAbility
 */
class NeutrinoDetectorMkIIAbility : BaseToggleAbility() {
    companion object {
        val COMMODITY_ID: String = Commodities.VOLATILES

        // Wisp: Increased from 1.0 to 1.2
        const val COMMODITY_PER_DAY = 1.2f
        const val DETECTABILITY_PERCENT = 50f
    }

    override fun getActivationText(): String? {
        return if (COMMODITY_ID != null && fleet != null && fleet.cargo.getCommodityQuantity(
                COMMODITY_ID
            ) <= 0 &&
            !Global.getSettings().isDevMode
        ) null
        else "Neutrino detector Mk.II activated"
    }

    override fun getDeactivationText(): String? = null

    override fun activateImpl() {}

    override fun showProgressIndicator(): Boolean = false

    override fun showActiveIndicator(): Boolean = isActive

    override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean) {
        val bad = Misc.getNegativeHighlightColor()
        val gray = Misc.getGrayColor()
        val highlight = Misc.getHighlightColor()
        var status = " (off)"
        if (turnedOn) {
            status = " (on)"
        }
        val title = tooltip.addTitle(spec.name + status)
        title.highlightLast(status)
        title.setHighlightColor(gray)
        val pad = 10f
        tooltip.addPara(
            "Reconfigures the fleet's drive field to act as a neutrino detector, " +
                    "allowing detection of human-made artifacts - and occasionally fleets - at extreme ranges. ", pad
        )
        tooltip.addPara(
            "High-emission sources such as stars, planets, jump-points, or space stations produce constant streams. " +
                    "Average sources produce periodic bursts. Low-emission sources produce occasional bursts.", pad
        )
        tooltip.addPara(
            "The Mk.II design improves upon the original by consuming marginally more power to detect and remove false positives.", pad
        )

        if (COMMODITY_ID != null) {
            var unit = "unit"

            if (COMMODITY_PER_DAY != 1f) unit = "units"
            val spec = commodity
            unit += " of " + spec.name.toLowerCase()
            tooltip.addPara(
                "Increases the range at which the fleet can be detected by %s and consumes %s $unit per day.",
                pad, highlight,
                "" + DETECTABILITY_PERCENT.toInt() + "%",
                "" + Misc.getRoundedValueMaxOneAfterDecimal(COMMODITY_PER_DAY)
            )
        } else {
            tooltip.addPara(
                "Increases the range at which the fleet can be detected by %s.",
                pad, highlight,
                "" + DETECTABILITY_PERCENT.toInt() + "%"
            )
        }

        if (fleet != null && fleet.isInHyperspace) {
            tooltip.addPara("Can not function in hyperspace.", bad, pad)
        } else {
            tooltip.addPara("Can not function in hyperspace.", pad)
        }

        //tooltip.addPara("Disables the transponder when activated.", pad);
        addIncompatibleToTooltip(tooltip, expanded)
    }

    override fun hasTooltip(): Boolean = true

    override fun getActiveLayers(): EnumSet<CampaignEngineLayers> = EnumSet.of(CampaignEngineLayers.ABOVE)

    override fun advance(amount: Float) {
        super.advance(amount)

        if (data != null && !isActive && progressFraction <= 0f) {
            data = null
        }
    }

    private var phaseAngle = 0f
    private var data: NeutrinoDetectorMkIIData? = null

    override fun applyEffect(amount: Float, level: Float) {
        val fleet = fleet ?: return

        //if (level < 1) level = 0;
        fleet.stats.detectedRangeMod.modifyPercent(modId, DETECTABILITY_PERCENT * level, "Gravimetric scan")
        val days = Global.getSector().clock.convertToDays(amount)
        phaseAngle += days * 360f * 10f
        phaseAngle = Misc.normalizeAngle(phaseAngle)

        if (data == null) {
            data = NeutrinoDetectorMkIIData(this)
        }

        data!!.advance(days)
        if (COMMODITY_ID != null) {
            val cost = days * COMMODITY_PER_DAY
            if (fleet.cargo.getCommodityQuantity(COMMODITY_ID) > 0 || Global.getSettings().isDevMode) {
                fleet.cargo.removeCommodity(COMMODITY_ID, cost)
            } else {
                fleet.addFloatingText(
                    "Out of " + commodity.name.toLowerCase(),
                    Misc.setAlpha(entity.indicatorColor, 255),
                    0.5f
                )
                deactivate()
            }
        }

        if (fleet.isInHyperspace) {
            deactivate()
        }
    }

    val commodity: CommoditySpecAPI
        get() = Global.getSettings().getCommoditySpec(COMMODITY_ID)

    override fun isUsable(): Boolean {
        val fleet = fleet ?: return false
        return isActive || !fleet.isInHyperspace
    }

    override fun deactivateImpl() {
        cleanupImpl()
    }

    override fun cleanupImpl() {
        val fleet = fleet ?: return
        fleet.stats.detectedRangeMod.unmodify(modId)
        //data = null;
    }

    //return getFleet().getRadius() + 25f;
    val ringRadius: Float
        get() = fleet.radius + 75f

    //return getFleet().getRadius() + 25f;
    @Transient
    private var texture: SpriteAPI? = null

    override fun render(layer: CampaignEngineLayers, viewport: ViewportAPI) {
        if (data == null) return
        val level = progressFraction
        if (level <= 0) return
        if (fleet == null) return
        if (!fleet.isPlayerFleet) return
        val alphaMult = viewport.alphaMult * level

//		float x = getFleet().getLocation().x;
//		float y = getFleet().getLocation().y;
//
//		GL11.glPushMatrix();
//		GL11.glTranslatef(x, y, 0);
//
//		GL11.glDisable(GL11.GL_TEXTURE_2D);
//		Misc.renderQuad(30, 30, 100, 100, Color.green, alphaMult * level);
//
//
//		GL11.glPopMatrix();


        //float noiseLevel = data.getNoiseLevel();
        val bandWidthInTexture = 256f
        var bandIndex: Float
        val radStart = ringRadius
        val radEnd = radStart + 75f
        val circ = (Math.PI * 2f * (radStart + radEnd) / 2f).toFloat()
        //float pixelsPerSegment = 10f;
        val pixelsPerSegment = circ / 360f
        //float pixelsPerSegment = circ / 720;
        val segments = Math.round(circ / pixelsPerSegment).toFloat()

//		segments = 360;
//		pixelsPerSegment = circ / segments;
        //pixelsPerSegment = 10f;
        val startRad = Math.toRadians(0.0).toFloat()
        val endRad = Math.toRadians(360.0).toFloat()
        val spanRad = Math.abs(endRad - startRad)
        val anglePerSegment = spanRad / segments
        val loc = fleet.location
        val x = loc.x
        val y = loc.y
        GL11.glPushMatrix()
        GL11.glTranslatef(x, y, 0f)

        //float zoom = viewport.getViewMult();
        //GL11.glScalef(zoom, zoom, 1);
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        if (texture == null) texture = Global.getSettings().getSprite("abilities", "neutrino_detector")
        texture!!.bindTexture()
        GL11.glEnable(GL11.GL_BLEND)
        //GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        val outlineMode = false
        //outlineMode = true;
        if (outlineMode) {
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            GL11.glDisable(GL11.GL_BLEND)
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE)
            //GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
        }
        val thickness = (radEnd - radStart) * 1f
        var texProgress = 0f
        val texHeight = texture!!.textureHeight
        val imageHeight = texture!!.height
        var texPerSegment = pixelsPerSegment * texHeight / imageHeight * bandWidthInTexture / thickness
        texPerSegment *= 1f
        val totalTex = Math.max(1f, Math.round(texPerSegment * segments).toFloat())
        texPerSegment = totalTex / segments
        val texWidth = texture!!.textureWidth
        val imageWidth = texture!!.width
        // Wisp: Purple color
        val color = Color(185, 12, 247, 255)
        //Color color = new Color(255,25,255,155);
        for (iter in 0..1) {
            if (iter == 0) {
                bandIndex = 1f
            } else {
                //color = new Color(255,215,25,255);
                //color = new Color(25,255,215,255);
                bandIndex = 0f
                texProgress = segments / 2f * texPerSegment
                //GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            }
            if (iter == 1) {
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE)
            }
            //bandIndex = 1;
            val leftTX = bandIndex * texWidth * bandWidthInTexture / imageWidth
            val rightTX = (bandIndex + 1f) * texWidth * bandWidthInTexture / imageWidth - 0.001f
            GL11.glBegin(GL11.GL_QUAD_STRIP)
            var i = 0f
            while (i < segments + 1) {
                val segIndex = i % segments.toInt()

                //float phaseAngleRad = (float) Math.toRadians(phaseAngle + segIndex * 10) + (segIndex * anglePerSegment * 10f);
                var phaseAngleRad: Float
                phaseAngleRad = if (iter == 0) {
                    Math.toRadians(phaseAngle.toDouble()).toFloat() + segIndex * anglePerSegment * 29f
                } else { //if (iter == 1) {
                    Math.toRadians(-phaseAngle.toDouble()).toFloat() + segIndex * anglePerSegment * 17f
                }
                val angle = Math.toDegrees((segIndex * anglePerSegment).toDouble()).toFloat()
                //if (iter == 1) angle += 180;
                val pulseSin = Math.sin(phaseAngleRad.toDouble()).toFloat()
                var pulseMax = thickness * 0.5f
                pulseMax = thickness * 0.2f
                pulseMax = 10f

                //pulseMax *= 0.25f + 0.75f * noiseLevel;
                val pulseAmount = pulseSin * pulseMax
                //float pulseInner = pulseAmount * 0.1f;
                val pulseInner = pulseAmount * 0.1f

//				float thicknessMult = delegate.getAuroraThicknessMult(angle);
//				float thicknessFlat = delegate.getAuroraThicknessFlat(angle);
                val theta = anglePerSegment * segIndex
                val cos = Math.cos(theta.toDouble()).toFloat()
                val sin = Math.sin(theta.toDouble()).toFloat()
                val rInner = radStart - pulseInner
                //if (rInner < r * 0.9f) rInner = r * 0.9f;

                //float rOuter = (r + thickness * thicknessMult - pulseAmount + thicknessFlat);
                var rOuter = radStart + thickness - pulseAmount


                //rOuter += noiseLevel * 25f;
                var grav = data!!.getDataAt(angle)
                //if (grav > 500) System.out.println(grav);
                //if (grav > 300) grav = 300;
                if (grav > 750) grav = 750f
                grav *= 250f / 750f
                grav *= level
                //grav *= 0.5f;
                //rInner -= grav * 0.25f;

                //rInner -= grav * 0.1f;
                rOuter += grav
                //				rInner -= grav * 3f;
//				rOuter -= grav * 3f;
                //System.out.println(grav);
                var alpha = alphaMult
                alpha *= 0.25f + Math.min(grav / 100, 0.75f)
                //alpha *= 0.75f;

//
//
//
//				phaseAngleWarp = (float) Math.toRadians(phaseAngle - 180 * iter) + (segIndex * anglePerSegment * 1f);
//				float warpSin = (float) Math.sin(phaseAngleWarp);
//				rInner += thickness * 0.5f * warpSin;
//				rOuter += thickness * 0.5f * warpSin;
                val x1 = cos * rInner
                val y1 = sin * rInner
                var x2 = cos * rOuter
                var y2 = sin * rOuter
                x2 += (Math.cos(phaseAngleRad.toDouble()) * pixelsPerSegment * 0.33f).toFloat()
                y2 += (Math.sin(phaseAngleRad.toDouble()) * pixelsPerSegment * 0.33f).toFloat()
                GL11.glColor4ub(
                    color.red.toByte(),
                    color.green.toByte(),
                    color.blue.toByte(),
                    (color.alpha.toFloat() * alphaMult * alpha).toByte()
                )
                GL11.glTexCoord2f(leftTX, texProgress)
                GL11.glVertex2f(x1, y1)
                GL11.glTexCoord2f(rightTX, texProgress)
                GL11.glVertex2f(x2, y2)
                texProgress += texPerSegment * 1f
                i++
            }
            GL11.glEnd()

            //GL11.glRotatef(180, 0, 0, 1);
        }
        GL11.glPopMatrix()
        if (outlineMode) {
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL)
        }
    }
}