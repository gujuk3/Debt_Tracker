package com.example.debttracker.service;

import android.content.Context;
import android.util.Log;

import com.example.debttracker.util.PreferenceManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class CurrencyService {
    private static final String TAG = "CurrencyService";
    private static final String BASE_URL = "https://api.exchangerate-api.com/v4/";

    private static CurrencyService instance;
    private final ExchangeRateApi api;
    private final PreferenceManager preferenceManager;

    public interface ExchangeRateApi {
        @GET("latest/USD")
        Call<ExchangeRateResponse> getUsdRates();
    }

    public static class ExchangeRateResponse {
        @SerializedName("rates")
        public Map<String, Double> rates;

        @SerializedName("time_last_updated")
        public long timeLastUpdated;
    }

    public interface CurrencyCallback {
        void onSuccess(double usdToTryRate);
        void onError(String message);
    }

    private CurrencyService(Context context) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(ExchangeRateApi.class);
        preferenceManager = PreferenceManager.getInstance(context);
    }

    public static synchronized CurrencyService getInstance(Context context) {
        if (instance == null) {
            instance = new CurrencyService(context.getApplicationContext());
        }
        return instance;
    }

    public void fetchExchangeRate(CurrencyCallback callback) {
        // Önce cache'i kontrol et (1 saat geçerli)
        double cachedRate = preferenceManager.getCachedExchangeRate();
        long cacheTime = preferenceManager.getExchangeRateCacheTime();
        long oneHour = 60 * 60 * 1000; // 1 saat

        if (cachedRate > 0 && (System.currentTimeMillis() - cacheTime) < oneHour) {
            Log.d(TAG, "Cache'den kur kullanılıyor: " + cachedRate);
            callback.onSuccess(cachedRate);
            return;
        }

        // API'den yeni kur çek
        api.getUsdRates().enqueue(new Callback<ExchangeRateResponse>() {
            @Override
            public void onResponse(Call<ExchangeRateResponse> call, Response<ExchangeRateResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Double tryRate = response.body().rates.get("TRY");
                    if (tryRate != null) {
                        Log.d(TAG, "API'den kur alındı: " + tryRate);
                        preferenceManager.saveExchangeRate(tryRate);
                        callback.onSuccess(tryRate);
                    } else {
                        // Cache'deki kuru kullan veya hata ver
                        if (cachedRate > 0) {
                            callback.onSuccess(cachedRate);
                        } else {
                            callback.onError("TRY kuru bulunamadı");
                        }
                    }
                } else {
                    // Cache'deki kuru kullan veya hata ver
                    if (cachedRate > 0) {
                        callback.onSuccess(cachedRate);
                    } else {
                        callback.onError("API hatası: " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<ExchangeRateResponse> call, Throwable t) {
                Log.e(TAG, "API hatası", t);
                // İnternet yoksa cache'deki kuru kullan
                if (cachedRate > 0) {
                    callback.onSuccess(cachedRate);
                } else {
                    callback.onError("Bağlantı hatası: " + t.getMessage());
                }
            }
        });
    }

    // TL'yi USD'ye çevir
    public double convertTryToUsd(double tryAmount, double usdToTryRate) {
        if (usdToTryRate <= 0) return 0;
        return tryAmount / usdToTryRate;
    }

    // USD'yi TL'ye çevir
    public double convertUsdToTry(double usdAmount, double usdToTryRate) {
        return usdAmount * usdToTryRate;
    }
}