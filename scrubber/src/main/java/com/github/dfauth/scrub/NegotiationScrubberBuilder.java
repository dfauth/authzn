package com.github.dfauth.scrub;

import com.github.dfauth.scrub.rfq.CreateNegotiationEvent;
import com.github.dfauth.scrub.uievents.NegotiationUIEvent;

public class NegotiationScrubberBuilder implements ScrubberBuilder<NegotiationUIEvent> {

    private CreateNegotiationEvent event;

    public NegotiationScrubberBuilder(CreateNegotiationEvent model) {
        this.event = model;
    }

    @Override
    public Scrubber<NegotiationUIEvent> build(UserModel u) {
        return e -> event.render(u);
    }
}
