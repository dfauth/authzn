package com.github.dfauth.scrub.rfq;

import com.github.dfauth.authzn.Company;
import com.github.dfauth.scrub.uievents.NegotiationUIEvent;

public class CreateNegotiationEvent extends RfqVisibilityModel<NegotiationUIEvent> {

    private final long volume;
    private final long price;
    private final String instrument;

    public CreateNegotiationEvent(Company originator, Company broker, Company tradingCompany, long volume, long price, String instrument) {
        super(originator, broker, tradingCompany);
        this.volume = volume;
        this.price = price;
        this.instrument = instrument;
    }

    @Override
    public NegotiationUIEvent render(Company c) {
        return new NegotiationUIEvent(extract(originator, c),
                extract(broker, c),
                extract(tradingCompany, c),
                _render(volume, c),
                _render(price, c),
                _render(instrument, c)
        );
    }

}
