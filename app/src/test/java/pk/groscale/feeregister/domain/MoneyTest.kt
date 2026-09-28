package pk.groscale.feeregister.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyTest {

    @Test fun `formats with comma grouping and no decimals`() {
        assertEquals("Rs 2,500", formatRs(2500))
        assertEquals("Rs 0", formatRs(0))
        assertEquals("Rs 100", formatRs(100))
        assertEquals("Rs 42,000", formatRs(42000))
        assertEquals("Rs 1,200,000", formatRs(1200000))
    }

    @Test fun `groups three digits at a time`() {
        assertEquals("1,200,000", groupDigits(1200000))
        assertEquals("999", groupDigits(999))
        assertEquals("1,000", groupDigits(1000))
    }

    @Test fun `handles negatives for balance display`() {
        assertEquals("-1,500", groupDigits(-1500))
    }

    @Test fun `rounds half up to the step`() {
        assertEquals(2400, roundToStep(2386.36, 50))
        assertEquals(850, roundToStep(865.38, 50))
        assertEquals(600, roundToStep(608.70, 50))
        assertEquals(0, roundToStep(20.0, 50))
        assertEquals(50, roundToStep(25.0, 50))
    }
}
