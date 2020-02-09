package com.github.dfauth.scrub.uievents;

public class NegotiationUIEvent {

    private final String originator;
    private final String broker;
    private final String tradingCompany;
    private final Long volume;
    private final Long price;
    private final String instrument;

    public NegotiationUIEvent(String originator, String broker, String tradingCompany, Long volume, Long price, String instrument) {
        this.originator = originator;
        this.broker = broker;
        this.tradingCompany = tradingCompany;
        this.volume = volume;
        this.price = price;
        this.instrument = instrument;
    }

    public String getOriginator() {
        return originator;
    }

    public String getBroker() {
        return broker;
    }

    public String getTradingCompany() {
        return tradingCompany;
    }

    public Long getVolume() {
        return volume;
    }

    public Long getPrice() {
        return price;
    }

    public String getInstrument() {
        return instrument;
    }
}
