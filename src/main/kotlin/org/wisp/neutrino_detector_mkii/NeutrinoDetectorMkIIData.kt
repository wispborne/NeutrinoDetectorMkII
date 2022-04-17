package org.wisp.neutrino_detector_mkii

import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.impl.campaign.abilities.GraviticScanData.GSPing
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.velfield.SlipstreamTerrainPlugin2
import com.fs.starfarer.api.impl.campaign.velfield.SlipstreamTerrainPlugin2.SlipstreamSegment
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.lwjgl.util.vector.Vector2f
import org.wisp.neutrino_detector_mkii.NeutrinoDetectorMkIIAbility.Companion.SLIPSTREAM_DETECTION_RANGE
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor


/**
 * Source: com.fs.starfarer.api.impl.campaign.abilities.GraviticScanData
 */
class NeutrinoDetectorMkIIData     //private IntervalUtil specialInterval = new IntervalUtil(0.15f, 0.25f);
    (private val ability: NeutrinoDetectorMkIIAbility) {
    private val resolution = 360

    @Transient
    private var data: FloatArray? = null

    private val pings: MutableList<GSPing> = ArrayList()

    //private IntervalUtil noiseInterval = new IntervalUtil(0.01f, 0.02f);
    private val planetInterval = IntervalUtil(0.01f, 0.01f)
    private val specialInterval = IntervalUtil(0.075f, 0.125f)
    fun advance(days: Float) {
        if (ability.fleet == null || ability.fleet.containingLocation == null) return
//        if (ability.fleet.isInHyperspace) {
//            data = null
//            return
//        }
        val iter = pings.iterator()
        while (iter.hasNext()) {
            val ping = iter.next()
            ping.advance(days)
            if (ping.isDone) {
                iter.remove()
            }
        }


//		noiseInterval.advance(days);
//		if (noiseInterval.intervalElapsed() && false) {
//			float noiseLevel = getNoiseLevel();
//			int num = Math.round(noiseLevel * 10);
//			num = 1;
//			for (int i = 0; i < num; i++) {
//				float angle = (float) Math.random() * 360f;
////				float arc = 5f + 10f * (float) Math.random() + 10 * noiseLevel;
////				float grav = 5f + 50f * (float) Math.random() * noiseLevel;
//				float arc = 5f + 10f * (float) Math.random();
//				float grav = 30f + 80f * (float) Math.random();
//
////				float in = 0.02f + 0.02f * (float) Math.random();
////				float out = 0.02f + 0.02f * (float) Math.random();
//
//				float in = 0.05f + 0.1f * (float) Math.random();
//				in *= 0.25f;
//				float out = in;
//
//				GSPing ping = new GSPing(angle, arc, grav, in, out);
//				pings.add(ping);
//			}
//		}
        planetInterval.advance(days)
        if (planetInterval.intervalElapsed()) {
            maintainHighSourcePings()
        }
        specialInterval.advance(days)
        if (specialInterval.intervalElapsed()) {
            doSpecialPings()
        }
        updateData()

        //System.out.println("Pings: " + pings.size());
    }

    fun updateData() {
        data = FloatArray(resolution)
        val max = 0f
        val incr = 360f / resolution.toFloat()
        for (ping in pings) {
            val b = ping.fader.brightness
            if (b <= 0) continue

            //b = (float) Math.sqrt(b);
            //b *= b;
            val arc = ping.arc
            val mid = ping.angle
            val half = ceil((0.5f * arc / incr).toDouble()).toFloat()
            var i = -half
            while (i <= half) {
                val curr = mid + incr * i
                val index = getIndex(curr)
                var intensity = 1f - abs(i / half)
                intensity *= intensity
                val value = ping.grav * intensity * b
                data!![index] += value
                i++
            }
        }
    }

    fun getDataAt(angle: Float): Float {
        if (data == null) return 0f
        val index = getIndex(angle)
        return data!![index]
    }

    fun getIndex(angle: Float): Int {
        var angle = angle
        angle = Misc.normalizeAngle(angle)
        return floor((resolution * angle / 360f).toDouble()).toInt()
    }

    private var initialCount = 0
    private val special: MutableList<SectorEntityToken> = ArrayList()

    //private float totalForce;
    fun doSpecialPings() {
        val fleet = ability.fleet
        if (fleet.isInHyperspace) return;
        val loc = fleet.location
        val location = fleet.containingLocation
        val neutrinoLowSkipProb = 0.8f
        if (special.isEmpty()) {
//			for (SectorEntityToken entity : location.getAsteroids()) {
//				special.add(entity);
//			}
            for (`object` in location.getEntities(CustomCampaignEntityAPI::class.java)) {
                if (`object` is SectorEntityToken) {
                    val entity = `object`
                    val neutrinoHigh = entity.hasTag(Tags.NEUTRINO_HIGH)
                    if (neutrinoHigh) continue
                    val neutrino = entity.hasTag(Tags.NEUTRINO)
                    val neutrinoLow = entity.hasTag(Tags.NEUTRINO_LOW)
                    val station = entity.hasTag(Tags.STATION)
                    if (!neutrino && !neutrinoLow && !station) continue
                    if (neutrinoLow && Math.random() < neutrinoLowSkipProb) continue
                    special.add(entity)
                }
            }
            //			for (Object object : location.getEntities(OrbitalStationAPI.class)) {
//				if (object instanceof SectorEntityToken) {
//					SectorEntityToken entity = (SectorEntityToken) object;
//					special.add(entity);
//				}
//			}
            for (curr in location.fleets) {
                if (fleet === curr) continue
                val neutrinoHigh = curr.hasTag(Tags.NEUTRINO_HIGH)
                if (neutrinoHigh) continue
                if (Math.random() < neutrinoLowSkipProb) continue
                special.add(curr)
            }
            initialCount = special.size
        }
        val batch = ceil((initialCount / 1f).toDouble()).toInt()

        for (i in 0 until batch) {
            if (special.isEmpty()) break
            val curr = special.removeAt(0)
            val dist = Misc.getDistance(loc, curr.location)
            var arc = Misc.computeAngleSpan(curr.radius, dist)
            arc *= 2f
            if (arc < 15) arc = 15f
            if (arc > 150f) arc = 150f
            //arc += 30f;
            val angle = Misc.getAngleInDegrees(loc, curr.location)
            var g = getGravity(curr)
            g *= getRangeGMult(dist)
            var `in` = 0.05f + 0.1f * Math.random().toFloat()
            `in` *= 0.25f
            var out = `in`
            out *= 2f
            val ping = GSPing(angle, arc, g, `in`, out)
            ping.withSound = true
            pings.add(ping)
        }

        // Wisp: Remove the false positive pings.
//        long seed = (long) (location.getLocation().x * 1300000 + location.getLocation().y * 3700000 + 1213324234234L);
//        Random random = new Random(seed);
//
//        int numFalse = random.nextInt(5);
//        //System.out.println(numFalse);
//
//        for (int i = 0; i < numFalse; i++) {
//
//            boolean constant = random.nextFloat() > 0.25f;
//            if (!constant && (float) Math.random() < neutrinoLowSkipProb) {
//                random.nextFloat();
//                random.nextFloat();
//                continue;
//            }
//
//            float arc = 15;
//            float angle = random.nextFloat() * 360f;
//            float in = 0.05f + 0.1f * (float) Math.random();
//            in *= 0.25f;
//            float out = in;
//            out *= 2f;
//
//            float g = 80 + random.nextFloat() * 60;
//
//            GraviticScanData.GSPing ping = new GraviticScanData.GSPing(angle, arc, g, in, out);
//            ping.withSound = true;
//            pings.add(ping);
//        }
    }

    fun getRangeGMult(range: Float): Float {
        var range = range
        range -= 3000f
        if (range < 0) range = 0f
        val max = 15000f
        if (range > max) range = max
        return 1f - 0.85f * range / max
    }

    fun maintainSlipstreamPings() {
        val fleet = ability.fleet
        val loc = fleet.location
        val location = fleet.containingLocation
        val range = SLIPSTREAM_DETECTION_RANGE
        if (Misc.isInsideSlipstream(fleet)) return
        for (ter in location.terrainCopy) {
            if (ter.plugin is SlipstreamTerrainPlugin2) {
                val plugin = ter.plugin as SlipstreamTerrainPlugin2
                if (plugin.containsEntity(fleet)) continue
                val inRange: MutableList<SlipstreamSegment> = ArrayList<SlipstreamSegment>()
                val near: List<SlipstreamSegment> = plugin.getSegmentsNear(loc, range)
                var skip = 0
                for (curr in near) {
                    if (skip > 0) {
                        skip--
                        continue
                    }
                    if (curr.bMult <= 0) continue
                    val dist = Misc.getDistance(loc, curr.loc)
                    if (dist < range) {
                        inRange.add(curr)
                        skip = 5
                    }
                }
                if (!inRange.isEmpty()) {
                    for (curr in inRange) {
                        val dist = Misc.getDistance(loc, curr.loc)
                        var arc = Misc.computeAngleSpan(curr.width, dist)
                        arc *= 2f
                        if (arc > 150f) arc = 150f
                        if (arc < 20) arc = 20f
                        //arc += 30f;
                        val angle = Misc.getAngleInDegrees(loc, curr.loc)
                        var g = 500f
                        g *= .1f
                        g *= getRangeGMult(dist)
                        val `in` = planetInterval.intervalDuration * 5f
                        val ping = GSPing(angle, arc, g, `in`, `in`)
                        pings.add(ping)
                    }
                }
            }
        }
    }

    fun maintainHighSourcePings() {
        val fleet = ability.fleet
        val loc = fleet.location
        val location = fleet.containingLocation

        maintainSlipstreamPings();

        if (fleet.isInHyperspace) {
            return;
        }

//        val netForce = Vector2f()
        val all: MutableList<SectorEntityToken> = ArrayList(location.planets)
        for (`object` in location.getEntities(CustomCampaignEntityAPI::class.java)) {
            if (`object` is SectorEntityToken) {
                val entity = `object`
                val neutrinoHigh = entity.hasTag(Tags.NEUTRINO_HIGH)
                if (neutrinoHigh) {
                    all.add(entity)
                }
            }
        }
        for (curr in location.fleets) {
            if (fleet === curr) continue
            val neutrinoHigh = curr.hasTag(Tags.NEUTRINO_HIGH)
            if (neutrinoHigh) {
                all.add(curr)
            }
        }
        for (`object` in location.getEntities(OrbitalStationAPI::class.java)) {
            if (`object` is SectorEntityToken) {
                all.add(`object`)
            }
        }
        for (`object` in location.jumpPoints) {
            if (`object` is SectorEntityToken) {
                all.add(`object`)
            }
        }
        for (entity in all) {
            if (entity is PlanetAPI) {
                if (entity.spec.isNebulaCenter) continue
            }
            if (entity.radius <= 0) continue
            val dist = Misc.getDistance(loc, entity.location)
            var arc = Misc.computeAngleSpan(entity.radius, dist)
            arc *= 2f
            if (arc > 150f) arc = 150f
            if (arc < 20) arc = 20f
            //arc += 30f;
            val angle = Misc.getAngleInDegrees(loc, entity.location)
            var g = getGravity(entity)
            //g /= dist;
            g *= .1f
            if (entity.hasTag(Tags.NEUTRINO_HIGH) || entity is OrbitalStationAPI) {
                g *= 2f
            }
            g *= getRangeGMult(dist)
//            val dir = Misc.getUnitVectorAtDegreeAngle(angle)
//            dir.scale(g)
//            Vector2f.add(netForce, dir, netForce)
            //			if (Misc.isInArc(90, 30, angle)) {
//				System.out.println("fwefewf");
//			}
            val `in` = planetInterval.intervalDuration * 5f
            val ping = GSPing(angle, arc, g, `in`, `in`)
            pings.add(ping)
        }

//		for (String key : objectPings.keySet()) {
//			if (!seen.contains(key)) {
//				GSPing ping = objectPings.get(key);
//				ping.fader.setBounceDown(true);
//			}
//		}

        //totalForce = netForce.length();
        //totalForce = maxG;

        //System.out.println("Pings: " + pings.size());
        //System.out.println("Noise: " + getNoiseLevel());
        //System.out.println("Force: " + totalForce);
    }

    //	public float getTotalForce() {
    //		return totalForce;
    //	}
    //
    //	public float getNoiseLevel() {
    //		//if (true) return 0f;
    //
    //		float minForce = 20f;
    //		float noiseOneAt = 150;
    //
    //		if (totalForce <= minForce) return 0f;
    //		float noise = (totalForce - minForce) / (noiseOneAt - minForce);
    //		if (noise > 1) noise = 1;
    //		return noise;
    //	}
    fun getGravity(entity: SectorEntityToken): Float {
        var g = entity.radius
        if (entity is PlanetAPI) {
            //if (g < 200) g = 200;
            g *= 2f
            if (entity.spec.isBlackHole) {
                g *= 2f
            }
        }
        if (entity is OrbitalStationAPI) {
            g *= 4f
            if (g > 200) g = 200f
        }
        if (entity is CustomCampaignEntityAPI) {
            g *= 4f
            if (g > 200) g = 200f
        }
        if (entity is CampaignFleetAPI) {
            g *= 2f
            if (g > 200) g = 200f
        }

//		if (entity.getName().equals("Asteroid")) {
//			g *= 50f;
//		}
        return g
    }
}