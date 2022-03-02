package com.google.ads.mediation.pangle;

import static com.google.ads.mediation.pangle.PangleConstants.ERROR_INVALID_SERVER_PARAMETERS;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import com.bytedance.sdk.openadsdk.TTAdConfig;
import com.bytedance.sdk.openadsdk.TTAdManager;
import com.bytedance.sdk.openadsdk.TTAdSdk;
import com.google.ads.mediation.pangle.rtb.PangleRtbBannerAd;
import com.google.ads.mediation.pangle.rtb.PangleRtbInterstitialAd;
import com.google.ads.mediation.pangle.rtb.PangleRtbRewardedAd;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdConfiguration;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.google.android.gms.ads.mediation.VersionInfo;
import com.google.android.gms.ads.mediation.rtb.RtbAdapter;
import com.google.android.gms.ads.mediation.rtb.RtbSignalData;
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks;
import java.util.HashSet;
import java.util.List;

public class PangleMediationAdapter extends RtbAdapter {

  final static String TAG = PangleMediationAdapter.class.getSimpleName();
  private PangleRtbBannerAd bannerAd;
  private PangleRtbInterstitialAd interstitialAd;
  private PangleRtbRewardedAd rewardedAd;
  private static int coppa = -1;
  private static int gdpr = -1;
  private static int ccpa = -1;

  @Override
  public void collectSignals(@NonNull RtbSignalData rtbSignalData,
      @NonNull SignalCallbacks signalCallbacks) {
    String biddingToken = getPangleSdkManager().getBiddingToken();
    signalCallbacks.onSuccess(biddingToken);
  }

  @Override
  public void initialize(@NonNull Context context,
      @NonNull final InitializationCompleteCallback initializationCompleteCallback,
      @NonNull List<MediationConfiguration> list) {
    HashSet<String> appIds = new HashSet<>();
    for (MediationConfiguration mediationConfiguration : list) {
      Bundle serverParameters = mediationConfiguration.getServerParameters();
      String appId = serverParameters.getString(PangleConstants.APP_ID);
      if (!TextUtils.isEmpty(appId)) {
        appIds.add(appId);
      }
    }

    int count = appIds.size();
    if (count <= 0) {
      AdError error = PangleConstants.createAdapterError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or invalid App ID.");
      Log.w(TAG, error.getMessage());
      initializationCompleteCallback.onInitializationFailed(error.getMessage());
      return;
    }

    String appId = appIds.iterator().next();

    if (count > 1) {
      String message = String.format("Found multiple app IDs in %s. " +
          ", using %s to initialize Pangle SDK", appIds.toString(), appId);
      Log.w(TAG, message);
    }

    TTAdSdk.init(context, new TTAdConfig.Builder()
        .appId(appId)
        .coppa(coppa)
        .setGDPR(gdpr)
        .setCCPA(ccpa)
        .build(), new TTAdSdk.InitCallback() {
      @Override
      public void success() {
        initializationCompleteCallback.onInitializationSucceeded();
      }

      @Override
      public void fail(int errorCode, String errorMessage) {
        Log.w(TAG, errorMessage);
        initializationCompleteCallback.onInitializationFailed(errorMessage);
      }
    });
  }

  @NonNull
  @Override
  public VersionInfo getVersionInfo() {
    String versionString = BuildConfig.ADAPTER_VERSION;
    String[] splits = versionString.split("\\.");

    if (splits.length >= 4) {
      int major = Integer.parseInt(splits[0]);
      int minor = Integer.parseInt(splits[1]);
      int micro = Integer.parseInt(splits[2]) * 100 + Integer.parseInt(splits[3]);
      if (splits.length >= 5) {
        micro = micro * 100 + Integer.parseInt(splits[4]);
      }
      return new VersionInfo(major, minor, micro);
    }

    String logMessage = String
        .format("Unexpected adapter version format: %s. Returning 0.0.0 for adapter version.",
            versionString);
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @NonNull
  @Override
  public VersionInfo getSDKVersionInfo() {
    String versionString = TTAdSdk.getAdManager().getSDKVersion();
    String[] splits = versionString.split("\\.");

    if (splits.length >= 3) {
      int major = Integer.parseInt(splits[0]);
      int minor = Integer.parseInt(splits[1]);
      int micro = Integer.parseInt(splits[2]);
      if (splits.length >= 4) {
        micro = micro * 100 + Integer.parseInt(splits[3]);
      }
      return new VersionInfo(major, minor, micro);
    }

    String logMessage = String
        .format("Unexpected SDK version format: %s. Returning 0.0.0 for SDK version.",
            versionString);
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @Override
  public void loadRtbBannerAd(@NonNull MediationBannerAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback) {
    bannerAd = new PangleRtbBannerAd(adConfiguration, callback);
    bannerAd.render();
  }

  @Override
  public void loadRtbInterstitialAd(@NonNull MediationInterstitialAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> callback) {
    interstitialAd = new PangleRtbInterstitialAd(adConfiguration, callback);
    interstitialAd.render();
  }

  @Override
  public void loadRtbNativeAd(@NonNull MediationNativeAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> callback) {
    AdError error =
        new AdError(
            0,
            "Native ad is not supported in Pangle.",
            PangleConstants.ERROR_DOMAIN);
    Log.w(TAG, error.getMessage());
    callback.onFailure(error);
  }

  @Override
  public void loadRtbRewardedAd(@NonNull MediationRewardedAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
    rewardedAd = new PangleRtbRewardedAd(adConfiguration, callback);
    rewardedAd.render();
  }

  public static TTAdManager getPangleSdkManager() {
    return TTAdSdk.getAdManager();
  }

  /**
   * Sets the Pangle coppa settings.
   */
  public static void setCoppa(@NonNull MediationAdConfiguration mediationAdConfiguration) {
    switch (mediationAdConfiguration.taggedForChildDirectedTreatment()) {
      case RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE:
        TTAdSdk.setCoppa(1);
        break;
      case RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE:
        TTAdSdk.setCoppa(0);
        break;
      default:
        TTAdSdk.setCoppa(-1);
    }
  }

  /**
   * Set this COPPA option before initializing mobile ads to ensure they are correctly forwarded to Pangle's SDK.
   */
  public static void setCoppaBeforeInitialize(int coppa) {
    PangleMediationAdapter.coppa = coppa;
  }

  /**
   * Set this GDPR option before initializing mobile ads to ensure they are correctly forwarded to Pangle's SDK.
   */
  public static void setGdprBeforeInitialize(int gdpr) {
    PangleMediationAdapter.gdpr = gdpr;
  }

  /**
   * Set this CCPA option before initializing mobile ads to ensure they are correctly forwarded to Pangle's SDK.
   */
  public static void setCcpaBeforeInitialize(int ccpa) {
    PangleMediationAdapter.ccpa = ccpa;
  }
}
