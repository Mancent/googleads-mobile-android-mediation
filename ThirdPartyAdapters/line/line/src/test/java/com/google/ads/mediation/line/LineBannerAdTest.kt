package com.google.ads.mediation.line

import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.five_corp.ad.FiveAdConfig
import com.five_corp.ad.FiveAdCustomLayout
import com.five_corp.ad.FiveAdErrorCode
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class LineBannerAdTest {
  // Subject of tests
  private lateinit var lineBannerAd: LineBannerAd
  private lateinit var mediationAdConfiguration: MediationBannerAdConfiguration

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mockFiveAdConfig = mock<FiveAdConfig>()
  private val mockFiveAdCustomLayout =
    mock<FiveAdCustomLayout> {
      on { logicalWidth } doReturn AdSize.BANNER.width
      on { logicalHeight } doReturn AdSize.BANNER.height
    }
  private val mockMediationAdCallback = mock<MediationBannerAdCallback>()
  private val sdkFactory =
    mock<SdkFactory> {
      on { createFiveAdConfig(any()) } doReturn mockFiveAdConfig
      on { createFiveAdCustomLayout(context, TEST_SLOT_ID, AdSize.BANNER.width) } doReturn
        mockFiveAdCustomLayout
    }
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> =
    mock()

  @Before
  fun setup() {
    LineSdkFactory.delegate = sdkFactory

    // Properly initialize lineBannerAd
    mediationAdConfiguration = createMediationBannerAdConfiguration()
    LineBannerAd.newInstance(mediationAdConfiguration, mediationAdLoadCallback).onSuccess {
      lineBannerAd = it
    }
    whenever(mediationAdLoadCallback.onSuccess(lineBannerAd)) doReturn mockMediationAdCallback
  }

  @Test
  fun getView_returnsCreatedBannerAd() {
    val createdAdView = lineBannerAd.view

    assertThat(createdAdView).isEqualTo(mockFiveAdCustomLayout)
  }

  @Test
  fun onFiveAdLoad_withUnexpectedAdSize_invokesOnFailure() {
    val adErrorCaptor = argumentCaptor<AdError>()
    val differentBannerAd =
      mock<FiveAdCustomLayout> {
        on { logicalWidth } doReturn AdSize.LARGE_BANNER.width
        on { logicalHeight } doReturn AdSize.LARGE_BANNER.height
      }

    lineBannerAd.onFiveAdLoad(differentBannerAd)

    verify(mediationAdLoadCallback).onFailure(adErrorCaptor.capture())
    val capturedError = adErrorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(LineBannerAd.ERROR_CODE_MISMATCH_AD_SIZE)
    assertThat(capturedError.message).startsWith("Unexpected ad size loaded.")
    assertThat(capturedError.domain).isEqualTo(LineMediationAdapter.ADAPTER_ERROR_DOMAIN)
  }

  @Test
  fun onFiveAdLoad_invokesOnSuccess() {
    lineBannerAd.onFiveAdLoad(mockFiveAdCustomLayout)

    verify(mockFiveAdCustomLayout).setViewEventListener(lineBannerAd)
    verify(mediationAdLoadCallback).onSuccess(lineBannerAd)
  }

  @Test
  fun onFiveAdLoadError_invokesOnFailure() {
    val adErrorCaptor = argumentCaptor<AdError>()

    lineBannerAd.onFiveAdLoadError(mockFiveAdCustomLayout, FiveAdErrorCode.INTERNAL_ERROR)

    verify(mediationAdLoadCallback).onFailure(adErrorCaptor.capture())
    val capturedError = adErrorCaptor.firstValue
    assertThat(capturedError.code).isEqualTo(FiveAdErrorCode.INTERNAL_ERROR.value)
    assertThat(capturedError.message)
      .isEqualTo("FiveAd SDK returned a load error with code INTERNAL_ERROR.")
  }

  @Test
  fun onFiveAdClick_invokesReportAdClickedAndOnAdLeftApplication() {
    lineBannerAd.onFiveAdLoad(mockFiveAdCustomLayout)

    lineBannerAd.onFiveAdClick(mockFiveAdCustomLayout)

    verify(mockMediationAdCallback).reportAdClicked()
    verify(mockMediationAdCallback).onAdLeftApplication()
  }

  @Test
  fun onFiveAdImpression_invokesReportAdImpression() {
    lineBannerAd.onFiveAdLoad(mockFiveAdCustomLayout)

    lineBannerAd.onFiveAdImpression(mockFiveAdCustomLayout)

    verify(mockMediationAdCallback).reportAdImpression()
  }

  @Test
  fun onFiveAdClose_throwsNoException() {
    lineBannerAd.onFiveAdClose(mockFiveAdCustomLayout)
  }

  @Test
  fun onFiveAdViewError_throwsNoException() {
    val dummyErrorCode = FiveAdErrorCode.INTERNAL_ERROR

    lineBannerAd.onFiveAdViewError(mockFiveAdCustomLayout, dummyErrorCode)
  }

  @Test
  fun onFiveAdStart_throwsNoException() {
    lineBannerAd.onFiveAdStart(mockFiveAdCustomLayout)
  }

  @Test
  fun onFiveAdPause_throwsNoException() {
    lineBannerAd.onFiveAdPause(mockFiveAdCustomLayout)
  }

  @Test
  fun onFiveAdResume_throwsNoException() {
    lineBannerAd.onFiveAdResume(mockFiveAdCustomLayout)
  }

  @Test
  fun onFiveAdViewThrough_throwsNoException() {
    lineBannerAd.onFiveAdViewThrough(mockFiveAdCustomLayout)
  }

  @Test
  fun onFiveAdReplay_throwsNoException() {
    lineBannerAd.onFiveAdReplay(mockFiveAdCustomLayout)
  }

  @Test
  fun onFiveAdStall_throwsNoException() {
    lineBannerAd.onFiveAdStall(mockFiveAdCustomLayout)
  }

  @Test
  fun onFiveAdRecover_throwsNoException() {
    lineBannerAd.onFiveAdRecover(mockFiveAdCustomLayout)
  }

  private fun createMediationBannerAdConfiguration(): MediationBannerAdConfiguration {
    val serverParameters =
      bundleOf(
        LineMediationAdapter.KEY_SLOT_ID to TEST_SLOT_ID,
        LineMediationAdapter.KEY_APP_ID to TEST_APP_ID
      )
    return MediationBannerAdConfiguration(
      context,
      /*bidresponse=*/ "",
      serverParameters,
      /*mediationExtras=*/ Bundle(),
      /*isTesting=*/ true,
      /*location=*/ null,
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
      /*maxAdContentRating=*/ "",
      AdSize.BANNER,
      TEST_WATERMARK,
    )
  }

  companion object {
    private const val TEST_APP_ID = "testAppId"
    private const val TEST_SLOT_ID = "testSlotId"
    private const val TEST_WATERMARK = "testWatermark"
  }
}
