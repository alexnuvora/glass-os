# GlassOS 0.6 design batch

References: Apple design updates https://developer.apple.com/design/whats-new/ and Liquid Glass https://developer.apple.com/documentation/technologyoverviews/liquid-glass

## Delivered in this batch
- In-launcher glass folder overlay with a real grid of apps and spring-scale entrance animation, replacing basic list dialog
- Working in-folder application launch and editing
- Persistent status clock presentation control
- Simplified reference-inspired status text
- Existing customisable icons/dock, wallpaper-backed softened glass panels, battery/calendar widgets, searchable app library, preferences and icon long-press actions preserved

## Rendering scope
The glass backing samples a downscaled copy of GlassOS's own wallpaper, shifts the sampled coordinates slightly, and combines tinted gradients and optical edge highlights. It is not Apple's private Liquid Glass renderer; it does not dynamically refract all content or blur third-party app windows.

## Not yet pixel perfect
Matching the reference across densities requires screenshot capture and measured rendering on the target phone. System status bar, Android protected surfaces and third-party UI cannot be fully replaced by an ordinary launcher. Folder animations and glass visuals are approximations, not identical Apple's proprietary algorithms.
