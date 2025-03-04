/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.utils.forge

import com.datadog.android.core.configuration.SecurityConfig
import com.datadog.android.security.NoOpEncryption
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.ForgeryFactory

class SecurityConfigForgeryFactory : ForgeryFactory<SecurityConfig> {
    override fun getForgery(forge: Forge): SecurityConfig {
        return SecurityConfig(
            localDataEncryption = forge.aNullable { NoOpEncryption() }
        )
    }
}
