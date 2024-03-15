package cn.archko.pdf.bahaviours

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.runtime.Composable
import io.iamjosephmj.flinger.configs.FlingConfiguration
import io.iamjosephmj.flinger.flings.flingBehavior

object CustomFlingBehaviours {

    @Composable
    fun smoothScroll(): FlingBehavior = flingBehavior(
        scrollConfiguration = FlingConfiguration.Builder()
            .decelerationFriction(0.049f)
            .splineStartTension(0.2f)
            .splineInflection(0.15f)
            .build()
    )
}