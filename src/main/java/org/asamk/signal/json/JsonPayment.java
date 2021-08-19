package org.asamk.signal.json;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage.Payment;

public class JsonPayment {
    @JsonProperty
    final byte[] receipt;

    @JsonProperty
    final String note;
    JsonPayment(Payment payment) {
        this.receipt = payment.getPaymentNotification().get().getReceipt();
        this.note = payment.getPaymentNotification().get().getNote();
    }

}
