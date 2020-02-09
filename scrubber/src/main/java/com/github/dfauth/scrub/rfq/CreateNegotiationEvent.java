package com.github.dfauth.scrub.rfq;

import com.github.dfauth.scrub.Company;
import com.github.dfauth.scrub.UserModel;
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
    public NegotiationUIEvent render(UserModel u) {
        return new NegotiationUIEvent(extract(originator, u),
                extract(broker, u),
                extract(tradingCompany, u),
                _render(volume, u),
                _render(price, u),
                _render(instrument, u)
        );
    }

}
