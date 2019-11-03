package com.fyber.mediation.mopub;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import com.fyber.inneractive.sdk.external.*;
import com.fyber.inneractive.sdk.external.InneractiveUnitController.AdDisplayError;
import com.fyber.inneractive.sdk.external.InneractiveUserConfig.Gender;
import com.mopub.common.MoPub;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.CustomEventInterstitial;
import com.mopub.mobileads.MoPubErrorCode;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;

/**
 * Implements Fyber's interstitial Mopub's custom event class
 */
public class FyberInterstitialForMopub extends CustomEventInterstitial {
  // Mopub log tag definition
  private final static String LOG_TAG = "FyberInterstitialForMopub";

  // Members
  /**
   * Inneractived interstitial ad
   */
  InneractiveAdSpot mInterstitialSpot;
  /**
   * Context for showing the Ad
   */
  Context mContext;
  /**
   * The plugin fires events into Mopub's callback listener
   */
  private CustomEventInterstitialListener customEventListener;

  /**
   * Called by Mopub in order to start loading an interstitial ad
   *
   * @param context Android context
   * @param listener Mopub's external listener
   * @param localExtras map of local parameters, which were passed by a call to setLocalExtras
   * @param serverExtras map of parameters, as defined in the Mopub console
   */
  @Override
  protected void loadInterstitial(final Context context,
                                  final CustomEventInterstitialListener listener, Map<String, Object> localExtras,
                                  Map<String, String> serverExtras) {

    log("load interstitial requested");

    customEventListener = listener;

    setAutomaticImpressionAndClickTracking(false);

    // Set variables from MoPub console.
    String appId = null;
    String spotId = null;

    if (serverExtras != null) {
      log("server extras: " + serverExtras);
      appId = serverExtras.get(FyberMopubMediationDefs.REMOTE_KEY_APP_ID);
      spotId = serverExtras.get(InneractiveMediationDefs.REMOTE_KEY_SPOT_ID);
    }

    if (TextUtils.isEmpty(spotId)) {
      log("No spotID defined for ad unit. Cannot load interstitial");
      customEventListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
      return;
    }

    // If we've received an appId for this unit, try initializing the Fyber Marketplace SDK, if it was not already initialized
    if (!TextUtils.isEmpty(appId)) {
      FyberAdapterConfiguration.initializeFyberMarketplace(context, appId, serverExtras.containsKey(FyberMopubMediationDefs.REMOTE_KEY_DEBUG));
    }

    mContext = context;

    Gender gender = null;
    int age = 0;
    String zipCode = null;
    String keywords = null;
    if (localExtras != null) {

			/* Set keywords variable as defined on MoPub console, you can also define keywords with setlocalExtras in IaMediationActivity class.
      in case the variable is not initialized, the variable will not be in use */
      if (localExtras.containsKey(InneractiveMediationDefs.KEY_KEYWORDS)) {
        keywords = (String) localExtras.get(InneractiveMediationDefs.KEY_KEYWORDS);
      }
            
			/* Set the age variable as defined on IaMediationActivity class.   
			in case the variable is not initialized, the variable will not be in use */
      if (localExtras.containsKey(InneractiveMediationDefs.KEY_AGE)) {
        try {
          age = Integer.valueOf(localExtras.get(InneractiveMediationDefs.KEY_AGE).toString());
        } catch (NumberFormatException e) {
          log("local extra contains Invalid Age");
        }
      }

			/* Set the gender variable as defined on IaMediationActivity class.   
			in case the variable is not initialized, the variable will not be in use */
      if (localExtras.containsKey(InneractiveMediationDefs.KEY_GENDER)) {
        String genderStr = localExtras.get(InneractiveMediationDefs.KEY_GENDER).toString();
        if (genderStr.equals(InneractiveMediationDefs.GENDER_MALE)) {
          gender = Gender.MALE;
        } else if (genderStr.equals(InneractiveMediationDefs.GENDER_FEMALE)) {
          gender = (Gender.FEMALE);
        }
      }

			/* Set zipCode variable as defined on IaMediationActivity class.   
			in case the variable is not initialized, the variable will not be in use */
      if (localExtras.containsKey(InneractiveMediationDefs.KEY_ZIPCODE)) {
        zipCode = (String) localExtras.get(InneractiveMediationDefs.KEY_ZIPCODE);
      }
    }

    if (mInterstitialSpot != null) {
      mInterstitialSpot.destroy();
    }

    mInterstitialSpot = InneractiveAdSpotManager.get().createSpot();
    // Set your mediation name and version
    mInterstitialSpot.setMediationName(InneractiveMediationName.MOPUB);
    mInterstitialSpot.setMediationVersion(MoPub.SDK_VERSION);

    InneractiveFullscreenUnitController fullscreenUnitController = new InneractiveFullscreenUnitController();
    mInterstitialSpot.addUnitController(fullscreenUnitController);

    InneractiveAdRequest request = new InneractiveAdRequest(spotId);
    request.setUserParams(new InneractiveUserConfig()
        .setGender(gender)
        .setZipCode(zipCode)
        .setAge(age));
    if (!TextUtils.isEmpty(keywords)) {
      request.setKeywords(keywords);
    }

    // Load ad
    mInterstitialSpot.setRequestListener(new InneractiveAdSpot.RequestListener() {

      /**
       * Called by Inneractive when an interstitial is ready for display
       * @param adSpot Spot object
       */
      @Override
      public void onInneractiveSuccessfulAdRequest(InneractiveAdSpot adSpot) {
        log("on ad loaded successfully");
        customEventListener.onInterstitialLoaded();
      }

      /**
       * Called by Inneractive an interstitial fails loading
       * @param adSpot Spot object
       * @param errorCode the failure's error.
       */
      @Override
      public void onInneractiveFailedAdRequest(InneractiveAdSpot adSpot,
                                               InneractiveErrorCode errorCode) {
        log("Failed loading interstitial with error: " + errorCode);
        if (errorCode == InneractiveErrorCode.CONNECTION_ERROR) {
          customEventListener.onInterstitialFailed(MoPubErrorCode.NO_CONNECTION);
        } else if (errorCode == InneractiveErrorCode.CONNECTION_TIMEOUT) {
          customEventListener.onInterstitialFailed(MoPubErrorCode.NETWORK_TIMEOUT);
        } else if (errorCode == InneractiveErrorCode.NO_FILL) {
          customEventListener.onInterstitialFailed(MoPubErrorCode.NO_FILL);
        } else {
          customEventListener.onInterstitialFailed(MoPubErrorCode.SERVER_ERROR);
        }
      }
    });

    mInterstitialSpot.requestAd(request);
  }

  /**
   * Called by the Mopub infra-structure in order for the plugin to start showing Inneractive's
   * interstitial
   */
  @Override
  protected void showInterstitial() {
    log("show interstital called");
    // check if the ad is ready
    if (mInterstitialSpot != null && mInterstitialSpot.isReady()) {

      InneractiveFullscreenUnitController fullscreenUnitController = (InneractiveFullscreenUnitController) mInterstitialSpot
          .getSelectedUnitController();
      fullscreenUnitController.setEventsListener(new InneractiveFullscreenAdEventsListener() {

        /**
         * Called by Inneractive when an interstitial ad activity is closed
         * @param adSpot Spot object
         */
        @Override
        public void onAdDismissed(InneractiveAdSpot adSpot) {
          log("onAdDismissed");
          customEventListener.onInterstitialDismissed();
        }

        /**
         * Called by Inneractive when an interstitial ad activity is shown
         * @param adSpot Spot object
         */
        @Override
        public void onAdImpression(InneractiveAdSpot adSpot) {
          log("onAdImpression");
          customEventListener.onInterstitialShown();
          customEventListener.onInterstitialImpression();
        }

        /**
         * Called by Inneractive when an interstitial ad is clicked
         * @param adSpot Spot object
         */
        @Override
        public void onAdClicked(InneractiveAdSpot adSpot) {
          log("onAdClicked");
          customEventListener.onInterstitialClicked();
        }

        /**
         * Called by Inneractive when an interstitial ad opened an external application
         * @param adSpot Spot object
         */
        @Override
        public void onAdWillOpenExternalApp(InneractiveAdSpot adSpot) {
          log("onAdWillOpenExternalApp");
          // Don't call the onLeaveApplication() API since it causes a false Click event on MoPub
        }

        /**
         * Called when an ad has entered an error state, this will only happen when the ad is being shown
         * @param adSpot the relevant ad spot
         */
        @Override
        public void onAdEnteredErrorState(InneractiveAdSpot adSpot, AdDisplayError error) {
          log("onAdEnteredErrorState - " + error.getMessage());
        }

        /**
         * Called by Inneractive when Inneractive's internal browser, which was opened by this interstitial, was closed
         * @param adSpot Spot object
         */
        @Override
        public void onAdWillCloseInternalBrowser(InneractiveAdSpot adSpot) {
          log("onAdWillCloseInternalBrowser");
        }
      });

      // Add video content controller, for controlling video ads
      InneractiveFullscreenVideoContentController videoContentController = new InneractiveFullscreenVideoContentController();
      videoContentController.setEventsListener(new VideoContentListener() {
        @Override
        public void onProgress(int totalDurationInMsec, int positionInMsec) {
          log("Got video content progress: total time = " + totalDurationInMsec
                  + " position = " + positionInMsec);
        }

        /**
         * Called by inneractive when an Intersititial video ad was played to the end
         * <br>Can be used for incentive flow
         * <br>Note: This event does not indicate that the interstitial was closed
         */
        @Override
        public void onCompleted() {
          log("Got video content completed event");
        }

        @Override
        public void onPlayerError() {
          log("Got video content play error event");
        }
      });

      // Now add the content controller to the unit controller
      fullscreenUnitController.addContentController(videoContentController);

      fullscreenUnitController.show((Activity) mContext);
    } else {
      log("The Interstitial ad is not ready yet.");
    }
  }

  /**
   * Called by Mopub when an ad should be destroyed
   * <br>Destroy the underline Inneractive ad
   */
  @Override
  protected void onInvalidate() {
    log("onInvalidate called by Mopub");
    // We do the cleanup on the event of loadInterstitial.
    // TODO: What does this remark actually say?
  }

  /**
   * MopubLog helper
   * @param message
   */
  private void log(String message) {
    MoPubLog.log(CUSTOM, LOG_TAG, message);
  }
}
