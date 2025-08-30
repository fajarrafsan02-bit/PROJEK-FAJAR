package com.projek.tokweb.event;

import com.projek.tokweb.models.goldPrice.GoldPrice;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class GoldPriceUpdateEvent extends ApplicationEvent {
    private final GoldPrice newGoldPrice;
    
    public GoldPriceUpdateEvent(Object source, GoldPrice newGoldPrice) {
        super(source);
        this.newGoldPrice = newGoldPrice;
    }
}
