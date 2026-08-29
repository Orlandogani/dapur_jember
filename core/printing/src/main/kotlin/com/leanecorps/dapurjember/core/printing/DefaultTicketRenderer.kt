package com.leanecorps.dapurjember.core.printing

import com.leanecorps.dapurjember.core.domain.printing.KitchenTicketData
import com.leanecorps.dapurjember.core.domain.printing.ReceiptData
import com.leanecorps.dapurjember.core.domain.printing.TicketRenderer
import com.leanecorps.dapurjember.core.printing.template.renderCustomerReceipt
import com.leanecorps.dapurjember.core.printing.template.renderKitchenTicket
import javax.inject.Inject

/** [TicketRenderer] backed by the ESC/POS templates. Bound into the graph in [PrintingModule]. */
internal class DefaultTicketRenderer @Inject constructor() : TicketRenderer {

    override fun renderKitchenTicket(data: KitchenTicketData, paperWidthMm: Int): ByteArray =
        renderKitchenTicket(data, PaperWidth.ofMillimetres(paperWidthMm))

    override fun renderReceipt(data: ReceiptData, paperWidthMm: Int): ByteArray =
        renderCustomerReceipt(data, PaperWidth.ofMillimetres(paperWidthMm))
}
