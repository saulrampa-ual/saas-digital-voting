package com.saasvoting.ballotbox.application.port.out;

import com.saasvoting.ballotbox.domain.model.MinutesContent;

/** Turns the content of the minutes into a PDF (RF-16). Must give the same bytes for the same content. */
public interface MinutesRenderer {

    byte[] render(MinutesContent content);
}
