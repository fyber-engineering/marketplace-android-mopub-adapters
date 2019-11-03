package com.fyber.mediation.mopub;

import android.content.Context;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import com.fyber.inneractive.sdk.external.*;
import com.fyber.inneractive.sdk.external.InneractiveUnitController.AdDisplayError;
import com.fyber.inneractive.sdk.external.InneractiveUserConfig.Gender;
import com.mopub.common.MoPub;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.CustomEventBanner;
import com.mopub.mobileads.MoPubErrorCode;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;

/**
 * Implements Fyber's banner Mopub's custom event class
 */
public class FyberBannerForMopub extends CustomEventBanner {
  // Mopub log tag definition
  private final static String LOG_TAG = "FyberBannerForMopub";

  // Members
  /**
   * Mopub's callback listener
   */
  CustomEventBannerListener customEventListener;
  /**
   * The Spot object for the banner
   */
  InneractiveAdSpot mBannerSpot;

  /**
   * Called by the Mopub infra-structure when Mopub requests a banner from Inneractive
   *
   * @param context Android context
   * @param customEventBannerListener callback interface
   * @param localExtras map of local parameters, which were passed by a call to setLocalExtras
   * @param serverExtras map of parameters, as defined in the Mopub console
   */
  @Override
  protected void loadBanner(final Context context,
                            final CustomEventBannerListener customEventBannerListener,
                            Map<String, Object> localExtras, Map<String, String> serverExtras) {

    log("load banner requested");

    setAutomaticImpressionAndClickTracking(false);
    customEventListener = customEventBannerListener;

    // Set variables from MoPub console.
    String appId = null;
    String spotId = null;

    if (serverExtras != null) {
      log("server extras: " + serverExtras);
      appId = serverExtras.get(FyberMopubMediationDefs.REMOTE_KEY_APP_ID);
      spotId = serverExtras.get(FyberMopubMediationDefs.REMOTE_KEY_SPOT_ID);
    }

    if (TextUtils.isEmpty(spotId)) {
      log("No spotID defined for ad unit. Cannot load banner");
      customEventListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
      return;
    }

    // If we've received an appId for this unit, try initializing the Fyber Marketplace SDK, if it was not already initialized
    if (!TextUtils.isEmpty(appId)) {
       FyberAdapterConfiguration.initializeFyberMarketplace(context, appId, serverExtras.containsKey(FyberMopubMediationDefs.REMOTE_KEY_DEBUG));
    }

    // We have a valid spot id. Read local extras
    String keywords = null;
    Gender gender = null;
    int age = 0;
    String zipCode = null;
    if (localExtras != null) {
      if (localExtras.containsKey(FyberMopubMediationDefs.KEY_KEYWORDS)) {
        keywords = (String) localExtras.get(FyberMopubMediationDefs.KEY_KEYWORDS);
      }

			/* Set the age variable as defined on IaMediationActivity class.   
			in case the variable is not initialized, the variable will not be in use*/

      if (localExtras.containsKey(FyberMopubMediationDefs.KEY_AGE)) {
        try {
          age = Integer.valueOf(localExtras.get(FyberMopubMediationDefs.KEY_AGE).toString());
        } catch (NumberFormatException e) {
          log("local extra contains Invalid Age");
        }
      }

			/* Set the gender variable as defined on IaMediationActivity class.   
			in case the variable is not initialized, the variable will not be in use*/

      if (localExtras.containsKey(FyberMopubMediationDefs.KEY_GENDER)) {
        String genderStr = localExtras.get(FyberMopubMediationDefs.KEY_GENDER).toString();
        if (FyberMopubMediationDefs.GENDER_MALE.equals(genderStr)) {
          gender = Gender.MALE;
        } else if (FyberMopubMediationDefs.GENDER_FEMALE.equals(genderStr)) {
          gender = Gender.FEMALE;
        }
      }

      /* Set zipCode variable as defined on IaMediationActivity class.
      in case the variable is not initialized, the variable will not be in use*/
      if (localExtras.containsKey(FyberMopubMediationDefs.KEY_ZIPCODE)) {
        zipCode = (String) localExtras.get(FyberMopubMediationDefs.KEY_ZIPCODE);
      }
    }

    // Destroy previous ad
    if (mBannerSpot != null) {
      mBannerSpot.destroy();
    }

    mBannerSpot = InneractiveAdSpotManager.get().createSpot();
    // Set your mediation name and version
    mBannerSpot.setMediationName(InneractiveMediationName.MOPUB);
    mBannerSpot.setMediationVersion(MoPub.SDK_VERSION);

    InneractiveAdViewUnitController controller = new InneractiveAdViewUnitController();
    mBannerSpot.addUnitController(controller);

    InneractiveAdRequest request = new InneractiveAdRequest(spotId);
    // Set optional parameters for better targeting.
    request.setUserParams(new InneractiveUserConfig()
        .setGender(gender)
        .setZipCode(zipCode)
        .setAge(age));
    if (!TextUtils.isEmpty(keywords)) {
      request.setKeywords(keywords);
    }

    // Load an Ad
    mBannerSpot.setRequestListener(new InneractiveAdSpot.RequestListener() {
      @Override
      public void onInneractiveSuccessfulAdRequest(InneractiveAdSpot adSpot) {
        if (adSpot != mBannerSpot) {
          log("Wrong Banner Spot: Received - " + adSpot + ", Actual - " + mBannerSpot);
          return;
        }

        log("on ad loaded successfully");

        // Create a parent layout for the Banner Ad
        ViewGroup layout = new RelativeLayout(context);
        InneractiveAdViewUnitController controller = (InneractiveAdViewUnitController) mBannerSpot
            .getSelectedUnitController();
        controller.setEventsListener(new InneractiveAdViewEventsListener() {
          @Override
          public void onAdImpression(InneractiveAdSpot adSpot) {
              customEventListener.onBannerImpression();
            log("onAdImpression");
          }

          @Override
          public void onAdClicked(InneractiveAdSpot adSpot) {
            log("onAdClicked");
            customEventListener.onBannerClicked();
          }

          @Override
          public void onAdWillCloseInternalBrowser(InneractiveAdSpot adSpot) {
            log("onAdWillCloseInternalBrowser");
          }

          @Override
          public void onAdWillOpenExternalApp(InneractiveAdSpot adSpot) {
            log("onAdWillOpenExternalApp");
            // customEventListener.onLeaveApplication();
            // Don't call the onLeaveApplication() API since it causes a false Click event on MoPub
          }

          @Override
          public void onAdEnteredErrorState(InneractiveAdSpot adSpot, AdDisplayError error) {
            log("onAdEnteredErrorState - " + error.getMessage());
          }

          @Override
          public void onAdExpanded(InneractiveAdSpot adSpot) {
            log("onAdExpanded");
            customEventListener.onBannerExpanded();
          }

          @Override
          public void onAdResized(InneractiveAdSpot adSpot) {
            log("onAdResized");
          }

          @Override
          public void onAdCollapsed(InneractiveAdSpot adSpot) {
            log("onAdCollapsed");
            customEventListener.onBannerCollapsed();
          }
        });

        controller.bindView(layout);
        customEventListener.onBannerLoaded(layout);
      }

      @Override
      public void onInneractiveFailedAdRequest(InneractiveAdSpot adSpot,
                                               InneractiveErrorCode errorCode) {
        log("on ad failed loading with Error: " + errorCode);
        if (errorCode == InneractiveErrorCode.CONNECTION_ERROR) {
          customEventListener.onBannerFailed(MoPubErrorCode.NO_CONNECTION);
        } else if (errorCode == InneractiveErrorCode.CONNECTION_TIMEOUT) {
          customEventListener.onBannerFailed(MoPubErrorCode.NETWORK_TIMEOUT);
        } else if (errorCode == InneractiveErrorCode.NO_FILL) {
          customEventListener.onBannerFailed(MoPubErrorCode.NO_FILL);
        } else {
          customEventListener.onBannerFailed(MoPubErrorCode.SERVER_ERROR);
        }
      }
    });

    mBannerSpot.requestAd(request);
  }

  /**
   * Called when an ad view should be cleared
   */
  @Override
  protected void onInvalidate() {
    log("onInvalidate called by Mopub");
    if (mBannerSpot != null) {
      mBannerSpot.destroy();
      mBannerSpot = null;
    }
  }

  /**
   * MopubLog helper
   * @param message
   */
  private void log(String message) {
    MoPubLog.log(CUSTOM, LOG_TAG, message);
  }
}
