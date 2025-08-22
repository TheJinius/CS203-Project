package com.ubs.tariffapp.services;

import com.ubs.tariffapp.models.ForexRate;
import com.ubs.tariffapp.apis.ForexApiClient;

public class ForexService {

    public static double convert(String base, String target, double amount) throws Exception {
        ForexRate rates = ForexApiClient.getRates(base);
        double rate = rates.getRate(target);
        if (rate == -1) throw new Exception("Currency not found: " + target);
        return amount * rate;
    }
}
