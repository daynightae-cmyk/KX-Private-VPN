package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.launch.entryRoute
import org.junit.Assert.assertEquals
import org.junit.Test

/** The explicit orb-tap entry destination is pinned so first-run routing cannot drift. */
class EntryRouteTest {
    @Test fun firstLaunchEntersTheInAppAndroidWizard() {
        assertEquals("intro-guide", entryRoute(onboarded = false))
    }

    @Test fun returningUserEntersHome() {
        assertEquals("home", entryRoute(onboarded = true))
    }
}
