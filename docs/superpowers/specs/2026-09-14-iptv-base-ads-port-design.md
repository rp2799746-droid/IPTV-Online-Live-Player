# IPTV Base Ads Port Design

## Goal

Port the TestSill advertising architecture into the IPTV app while preserving its existing layouts, placement IDs, package names, and IPTV-specific behavior.

## Constraints

- Keep every existing IPTV layout and screen visual treatment unchanged.
- Keep IPTV widget shortcuts, uninstall flow, mirroring flow, deep-link opening, consent, in-app update, OneSignal, Firebase initialization, and Settings back stack.
- Keep the current IPTV placement contract. Do not introduce TestSill placement keys that are absent from IPTV `ad_config.json`.
- Keep Language completion and Intro completion semantics. Replace static Language variant state with one persistent completion state and a per-Activity variant derived from it.
- Centralize native load eligibility and state in `AdsManager`; screens observe and render only.
- Do not retain screen-local ad retry sets, permanent requested-tag guards, test-ID fallbacks, or temporary global interstitial interval overrides.

## Architecture

`AdsManager` becomes the owner of native ad placement selection, config/purchase/network/UA gates, LiveData publication, and interstitial readiness. It exposes typed loaders for Language, Onboarding, and Welcome while retaining compatible wrappers for the IPTV-only placements that are outside this port.

`SplashActivity`, `LanguageActivity`, `IntroActivity`, and the onboarding page fragments retain their IPTV UI and navigation. They trigger the base-defined preload points, observe a placement-specific LiveData, and render an ad only after a non-null delivery. A skipped or failed placement delivers null, so the existing ad region can be hidden without screen-local retry logic.

## Placement Mapping

| Base responsibility | IPTV placement contract |
| --- | --- |
| Splash interstitial | `inter_splash` |
| Splash banner | `banner_splash` |
| Language initial native | `native_language_1`, `native_language_2` |
| Language click native | `native_language_1_click`, `native_language_2_click` |
| Onboarding page 1 | `native_onboarding_1_1`, `native_onboarding_2_1` |
| Onboarding fullscreen | `native_onboarding_fullscreen_1_2`, `native_onboarding_fullscreen_2_2` |
| Onboarding page 4 | `native_onboarding_1_4`, `native_onboarding_2_4` |
| Onboarding interstitial | `inter_onboarding` |
| Resume / Welcome interstitial | `open_resume`, `inter_welcome_back` |

The differing fullscreen suffix (`_2` in IPTV versus `_3` in TestSill) is a placement-name mapping only. Its behavior uses the matching TestSill fullscreen eligibility gate.

## Component Changes

### Application

Initialize Mobile Ads, DevConfig using BuildConfig values, bundled ad config, and ERain in base ordering. Preserve Hilt, ViewPump, Firebase, OneSignal, application-level Firebase callback ownership, IPTV tokens, and current App Open exclusions. Resume behavior selects App Open when `open_resume` is enabled; otherwise it selects IPTV Welcome.

### Splash

Preserve consent, network recovery dialog, dynamic shortcuts, deep links, app update, fullscreen presentation, and all IPTV destinations. Add a bounded Remote Config wait based on the base timing and fall back to bundled ad config. Determine the Language variant before every route to Language. Load the Language native only from `inter_splash.onAdLoaded`; failure, disabled, and timeout paths navigate without an extra native request.

### Language

Preserve locale persistence, language list, first-run versus Settings navigation, Done delay, swipe hint, and title behavior. A local `LanguageVariant` is read from Intent state, saved state, or the persistent Language-completed flag. The screen observes the initial native until a language choice, then swaps to the preloaded click-native observer. It does not make its own ad eligibility decision, retry request, or shimmer timeout decision.

### Onboarding

Preserve its ViewPager, current pages, `isIntroFlowCompleted` timing, final routing, and Back behavior. Move page-4, fullscreen, and onboarding-interstitial preloads to `IntroActivity` after the base 100 ms delay. Fragments render the already selected LiveData and hide the ad region for null. The fullscreen page stays in the pager only when its centralized eligibility result permits it.

### Welcome

Preserve current Welcome UI. Resume routing applies protected-screen, interstitial-showing, purchase, ad-click-return, `interwelcomeback`, and SDK eligibility checks. App Open and Welcome stay mutually exclusive.

## Failure Behavior

- Remote Config success, failure, or timeout each enter exactly one Splash continuation.
- Disabled config, offline, purchase, and UA ineligibility produce null native state.
- No screen can issue a fallback native retry after null or failure.
- All interstitial show paths call their continuation when no ready ad exists.

## Verification

- Compile the debug variant with JDK 21.
- Verify static placement call sites and no screen-local native retries.
- On a device, validate first/subsequent Language variants, Splash success/failure/timeout, click-ad browser return, Activity/process recreation, Settings Language, and Onboarding completion.
