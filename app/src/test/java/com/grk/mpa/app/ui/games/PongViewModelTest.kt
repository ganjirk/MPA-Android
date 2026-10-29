package com.grk.mpa.app.ui.games

import com.grk.mpa.app.core.PongGame
import com.grk.mpa.app.core.PongSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PongViewModelTest {
    @Test
    fun framesAdvanceTheBall() {
        val vm = PongViewModel(PongGame())
        val startX = vm.state.ballX
        vm.onFrame(3)
        assertTrue(vm.state.ballX > startX)
    }

    @Test
    fun zeroStepsLeaveTheStateUntouched() {
        val vm = PongViewModel(PongGame())
        val before = vm.state
        vm.onFrame(0)
        assertEquals(before, vm.state)
    }

    @Test
    fun pausedGameIgnoresFrames() {
        val vm = PongViewModel(PongGame())
        vm.togglePause()
        val before = vm.state
        vm.onFrame(10)
        assertEquals(before, vm.state)
        vm.togglePause()
        vm.onFrame(10)
        assertTrue(vm.state.ballX > before.ballX)
    }

    @Test
    fun movingPaddlesUpdatesState() {
        val vm = PongViewModel(PongGame())
        vm.movePaddle(PongSide.LEFT, 200f)
        vm.movePaddle(PongSide.RIGHT, 400f)
        assertEquals(200f, vm.state.leftPaddleY, 0f)
        assertEquals(400f, vm.state.rightPaddleY, 0f)
    }

    @Test
    fun resetRestartsTheGameAndUnpauses() {
        val vm = PongViewModel(PongGame())
        vm.movePaddle(PongSide.RIGHT, 50f)
        vm.onFrame(130)
        assertEquals(1, vm.state.leftScore)
        vm.togglePause()
        vm.reset()
        assertEquals(0, vm.state.leftScore)
        assertFalse(vm.isPaused)
        assertEquals(vm.state.config.width / 2, vm.state.ballX, 0f)
    }
}
