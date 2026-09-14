package com.glazev.breathingtrainer.ui

import java.util.concurrent.atomic.AtomicBoolean

/** Allows only one asynchronous operation until its terminal callback releases the gate. */
internal class SingleFlightGate {
    private val active = AtomicBoolean(false)

    fun tryStart(): Boolean = active.compareAndSet(false, true)

    fun finish() {
        active.set(false)
    }
}
