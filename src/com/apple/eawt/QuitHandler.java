package com.apple.eawt;

public interface QuitHandler {
    public void handleQuitRequestWith(AppEvent.QuitEvent qe, QuitResponse qr);    
}
