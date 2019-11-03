package com.fyber.mediation.mopub;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.fyber.inneractive.sdk.external.InneractiveAdManager;
import com.mopub.common.BaseAdapterConfiguration;
import com.mopub.common.OnNetworkInitializationFinishedListener;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.MoPubErrorCode;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;

public class FyberAdapterConfiguration extends BaseAdapterConfiguration {
    // Definitions
    private static final String TAG = "FyberAdapterConfig";
    private static final String MOPUB_NETWORK_NAME = "fyber";

    // Configuration keys
    /**
     * The application id you have received from the Fyber marketplace console
     */
    public final static String KEY_FYBER_APP_ID = "appID";
    /**
     * Set to 1" or "true" in order to enable Fyber marketplace debug mode
     */
    public final static String KEY_FYBER_DEBUG = "debug";

    /** 4-digit versioning scheme, of which the leftmost 3 digits correspond to the network SDK version,
     * and the last digit denotes the minor version number referring to an adapter release */
    @NonNull
    @Override
    public String getAdapterVersion() {
        return InneractiveAdManager.getVersion() + ".0";
    }

    @Nullable
    @Override
    public String getBiddingToken(@NonNull Context context) {
        return null;
    }

    @NonNull
    @Override
    public String getMoPubNetworkName() {
        return MOPUB_NETWORK_NAME;
    }

    @NonNull
    @Override
    public String getNetworkSdkVersion() {
        return InneractiveAdManager.getVersion();
    }

    @Override
    public void initializeNetwork(@NonNull Context context, @Nullable Map<String, String> configuration, @NonNull OnNetworkInitializationFinishedListener listener) {
        Preconditions.checkNotNull(context);

        if (configuration != null) {
            String appId = configuration.get(KEY_FYBER_APP_ID);
            if (!TextUtils.isEmpty(appId)) {
                if (initializeFyberMarketplace(context, appId, configuration.containsKey(KEY_FYBER_DEBUG)) && listener != null) {
                    listener.onNetworkInitializationFinished(FyberAdapterConfiguration.class, MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS);
                    /** Note: Do not report {@link MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR} because the adapter may still be initialized from custom event classes */
                }
            } else {
                Log.d(TAG, "No Fyber app id given in configuration object. Initialization postponed. You can use FyberAdapterConfiguration.KEY_FYBER_APP_ID as your configuration key");

            }
        }
    }

    /**
     * This method initializes the Fyber marketplace SDK, and returns true if the initialization was successfull. It can either be called from the initializeNetwork method
     * or called by one of the custom adapters classes, if the appId is only defined in the Mopub console
     * @param context
     * @param appId Fyber's application id
     * @param debugMode if set to true, runs Fyber Marketplace with debug logs
     * @return true if initialized successfully. false otherwise
     */
    public static boolean initializeFyberMarketplace(Context context, String appId, boolean debugMode) {
        synchronized (FyberAdapterConfiguration.class) {
            // Just to be on the safe side, wrap initialize with exception handling
            try {
                if (debugMode) {
                    InneractiveAdManager.setLogLevel(Log.VERBOSE);
                }

                if (InneractiveAdManager.wasInitialized() == false) {
                    InneractiveAdManager.initialize(context, appId);
                } else if (appId.equals(InneractiveAdManager.getAppId()) == false) {
                    Log.w(TAG, "Fyber marketplace was initialized with appId " + InneractiveAdManager.getAppId() +
                            " and now requests initialization with another appId (" + appId + ") You may have configured the wrong appId on the Mopub console?\n" +
                            " you can only use a single appId and its related spots");

                    return false;
                }

                return true;
            } catch (Exception e) {
                MoPubLog.log(CUSTOM_WITH_THROWABLE, "Initializing Fyber has encountered " + "an exception.", e);
            }
        }

        return false;
    }
}